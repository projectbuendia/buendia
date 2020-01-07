package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.xforms.XformsQueueProcessor;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.openmrs.projectbuendia.webservices.rest.XmlUtils.getChildren;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtils.removeNode;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtils.requirePath;

@Resource(
    name = RestController.PATH + "/xforminstances",
    supportedClass = Void.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*,1.12.*,2.0.*,2.1.*,2.2.*,2.3.*"
)
public class XformInstanceResource extends BaseResource<OpenmrsObject> {
    private static final Log LOG = LogFactory.getLog(XformInstanceResource.class);
    private static final String CLOB_XSLT_UUID = "buendia_clob_xform_instance_xslt";
    private static final XformsQueueProcessor processor = new XformsQueueProcessor();

    public XformInstanceResource() {
        super("XForm instances", Representation.DEFAULT);
    }
    @Override protected OpenmrsObject createItem(SimpleObject data, RequestContext context) {
        try {
            Document doc = getPreparedXformDocument(data);
            String xml = XformsUtil.doc2String(doc);
            File file = File.createTempFile("projectbuendia", null);
            processor.processXForm(xml, file.getAbsolutePath(), true, context.getRequest());
        } catch (IOException e) {
            throw new GenericRestException("Error storing xform data", e);
        } catch (ResponseException e) {
            // Just to avoid this being wrapped...
            throw e;
        } catch (Exception e) {
            throw new ConversionException("Error processing xform data", e);
        }
        return null;  // this resource cannot be retrieved, so better not to return anything
    }

    /** Given the received form instance, prepares an XML document that OpenMRS will accept. */
    public Document getPreparedXformDocument(SimpleObject post) throws IOException, SAXException {
        String xml = Utils.getRequiredString(post, "xml");

        String patientUuid = Utils.getRequiredString(post, "patient_uuid");
        Patient patient = DbUtils.patientsByUuid.get(patientUuid);

        Document doc = XmlUtils.parse(xml);
        Element formElement = XmlUtils.requireElementTagName(doc.getDocumentElement(), "form");
        ensureFormHasXsltResource(formElement.getAttribute("uuid"));

        User user = DbUtils.getAuthenticatedUser();
        adjustXformDocument(doc, patient.getId(), user.getId(), new Date());
        return doc;
    }

    /** Fixes up a received XForm instance document so that OpenMRS will accept it. */
    static void adjustXformDocument(Document doc, int patientId, int userId, Date dateEntered) throws SAXException, IOException {
        Element root = doc.getDocumentElement();

        // This element sets the encounter's patient_id column.
        XmlUtils.getOrCreatePath(doc, root, "patient", "patient.patient_id")
            .setTextContent("" + patientId);

        // The <enterer> element sets the encounter's creator column; without
        // this element, the encounter can end up with the wrong creator.
        XmlUtils.getOrCreatePath(doc, root, "header", "enterer")
            .setTextContent("" + userId);

        // Fill in the date that the form was entered; this becomes the encounter's date_created.
        // If this field is missing, saxon will crash with "Invalid dateTime value. too short".
        XmlUtils.getOrCreatePath(doc, root, "header", "date_entered")
            .setTextContent(Utils.formatUtc8601(dateEntered));

        // The encounter_datetime field should usually be omitted by the client,
        // which signals us to use the current server time.  When the client does
        // supply it, we need to reformat it.  See fixEncounterDatetime() below.
        setEncounterDatetime(doc, fixEncounterDatetime(getEncounterDatetime(doc)));

        // Make sure that all observations are under the obs element, with appropriate attributes.
        Element obs = XmlUtils.getOrCreateChild(doc, root, "obs");
        obs.setAttribute("openmrs_concept", "1238^MEDICAL RECORD OBSERVATIONS^99DCT");
        obs.setAttribute("openmrs_datatype", "ZZ");
        List<String> topLevelElements = Arrays.asList("header", "patient", "encounter", "obs");
        for (Element element : getChildren(root)) {
            if (!topLevelElements.contains(element.getTagName())) {
                for (Element observation : getChildren(element)) {
                    obs.appendChild(observation);
                }
                removeNode(element);
            }
        }
    }

    private static void ensureFormHasXsltResource(String formUuid) {
        // NOTE(ping): We use a form_resource to customize the translation from XML to HL7
        // so that the encounter_datetime is recorded with both date and time (whereas the
        // default translation only records the date).  For details, see the instructions
        // under "How to include time for encounter date" in the XForms Module User Guide at:
        // https://wiki.openmrs.org/display/docs/XForms+Module+User+Guide
        //
        // The XForms module (see XformsServiceImpl.getXslt()) sadly requires this resource
        // to be named exactly form.getName() + ".xFormXslt", which means that if a Form is
        // renamed, its FormResource must be recreated or renamed, or it will be lost.
        // Here, we add the FormResource if it is missing, assuming that the XSLT file exists
        // in the database's clob_datatype_storage table with a known UUID.

        FormService formService = Context.getFormService();
        Form form = DbUtils.formsByUuid.get(formUuid);
        String resourceName = form.getName() + ".xFormXslt";
        FormResource resource = formService.getFormResource(form, resourceName);
        if (resource == null) {
            resource = new FormResource();
            resource.setForm(form);
            resource.setDatatypeClassname("org.openmrs.customdatatype.datatype.LongFreeTextDatatype");
            resource.setPreferredHandlerClassname("org.openmrs.web.attribute.handler.LongFreeTextTextareaHandler");
            resource.setName(resourceName);
            resource.setValueReferenceInternal(CLOB_XSLT_UUID);
            formService.saveFormResource(resource);
        }
    }

    private static String getEncounterDatetime(Document doc) {
        return requirePath(doc.getDocumentElement(),
            "encounter", "encounter.encounter_datetime").getTextContent();
    }

    private static void setEncounterDatetime(Document doc, String content) {
        XmlUtils.getOrCreatePath(doc, doc.getDocumentElement(),
            "encounter", "encounter.encounter_datetime").setTextContent(content);
    }

    /** Fixes up the content of the encounter_datetime element for OpenMRS. */
    private static String fixEncounterDatetime(String text) {
        Date datetime = parseTimestamp(text);
        if (datetime == null) {
            // If the encounter_datetime field is missing, we use server time.
            // The server's clock is the authoritative clock, so, unless the
            // user is explicitly backdating a set of observations, the client
            // should omit the encounter_datetime field.
            datetime = new Date();
        }

        // Work around OpenMRS's inherently client-antagonistic design.
        datetime = DbUtils.fixEncounterDatetime(datetime);

        // Saxon cannot parse timestamps with timezones that don't include a
        // a colon and minutes (e.g. "+0100"), so OpenMRS cannot handle them
        // either.  Details on the history of this workaround are here:
        // https://docs.google.com/document/d/1IT92y_YP7AnhpDfdelbS7huxNKswa4VSXYPzqbnkWik/edit
        // To avoid this problem entirely, we always format the timestamp in UTC.
        return Utils.formatUtc8601(datetime);
    }

    /** Parses a timestamp in a variety of formats, returning null on failure. */
    public static Date parseTimestamp(String timestamp) {
        // For safety, we accept a few reasonable date formats, including
        // non-standard formats without hyphens or colons as separators, and
        // we use the "X" pattern code for time zones, which understands both
        // time zone indicator formats ("+01:00" and "+0100") as well as "Z".
        List<String> acceptablePatterns = Arrays.asList(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyyMMdd'T'HHmmss.SSSX",
            "yyyyMMdd'T'HHmmssX",
            "yyyyMMdd"
        );
        for (String pattern : acceptablePatterns) {
            try {
                return new SimpleDateFormat(pattern).parse(timestamp);
            } catch (ParseException e) {
                LOG.warn("Unparseable encounter_datetime: " + timestamp);
            }
        }
        return null;
    }

    @Override protected void populateJson(SimpleObject json, OpenmrsObject unused, RequestContext context) { }
}
