// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.xforms.XformsQueueProcessor;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.getElementOrThrow;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.getElements;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.removeNode;

/**
 * Resource for submitted "form instances" (filled-in forms).  Write-only.
 * <p/>
 * <p>Accepts POST requests to [API root]/xforminstance with JSON data of the form:
 * <pre>
 * {
 *   patient_id: "123", // patient ID assigned by medical center
 *   patient_uuid: "24ae3-5", // patient UUID in OpenMRS
 *   enterer_id: "1234-5", // person ID of the provider entering the data
 *   date_entered: "2015-03-14T09:26:53.589Z", // date that the encounter was
 *           // *entered* (not necessarily when observations were taken)
 *   xml: "..." // XML contents of the form instance, as provided by ODK
 * }
 * </pre>
 * <p/>
 * <p>When creation is successful, the created XformInstance JSON is returned.
 * If an error occurs, the response will be in the form:
 * <pre>
 * {
 *   "error": {
 *     "message": "[error message]",
 *     "code": "[breakpoint]",
 *     "detail": "[stack trace]"
 *   }
 * }
 * </pre>
 */
// TODO: Still not really sure what supportedClass to use here... can we omit it?
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xforminstances",
    supportedClass = SimpleObject.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class XformInstanceResource implements Creatable {
    static final RequestLogger logger = RequestLogger.LOGGER;
    private static final Log LOG = LogFactory.getLog(XformInstanceResource.class);

    // Everything not in this set is assumed to be a group of observations.
    private static final Set<String> KNOWN_CHILD_ELEMENTS = new HashSet<>();
    private static final XformsQueueProcessor processor = new XformsQueueProcessor();

    static {
        KNOWN_CHILD_ELEMENTS.add("header");
        KNOWN_CHILD_ELEMENTS.add("patient");
        KNOWN_CHILD_ELEMENTS.add("patient.patient_id");
        KNOWN_CHILD_ELEMENTS.add("encounter");
        KNOWN_CHILD_ELEMENTS.add("obs");
    }

    private final PatientService patientService;
    private final ProviderService providerService;
    private final UserService userService;

    public XformInstanceResource() {
        patientService = Context.getPatientService();
        providerService = Context.getProviderService();
        userService = Context.getUserService();
    }

    @Override public String getUri(Object instance) {
        // TODO Auto-generated method stub
        return null;
    }

    /** Accepts a submitted form instance. */
    @Override
    public Object create(SimpleObject obj, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "create", obj);
            Object result = createInner(obj, context);
            logger.reply(context, this, "create", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "create", e);
            throw e;
        }
    }

    /** Accepts a submitted form instance. */
    private Object createInner(SimpleObject post, RequestContext context) throws ResponseException {
        try {
            String xml = getPreparedXml(post);
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
        // FIXME
        return post;
    }

    /** Fills in and makes adjustments to the form instance XML so that OpenMRS will accept it. */
    public String getPreparedXml(SimpleObject post) throws IOException, SAXException {
        String xml = Utils.getRequiredString(post, "xml");

        String patientUuid = Utils.getRequiredString(post, "patient_uuid");
        Patient patient = patientService.getPatientByUuid(patientUuid);
        if (patient == null) throw new ObjectNotFoundException();

        return completeXform(xml, patient.getId());
    }

    /**
     * Fixes up the received XForm instance with various adjustments and additions
     * needed to get the observations into OpenMRS, e.g. include Patient ID, adjust
     * datetime formats, etc.
     */
    static String completeXform(String xml, Integer patientId) throws SAXException, IOException {
        Document doc = XmlUtil.parse(xml);

        // If we haven't been given a patient id, then the XForms processor will
        // create a patient and fill in the patient.patient_id in the DOM.
        // However, it won't actually create the node, just fill it in.
        // So whatever the case, make sure a patient.patient_id node exists.
        Element root = doc.getDocumentElement();
        Element patient = getFirstElementOrCreate(doc, root, "patient");
        Element patientIdElement = getFirstElementOrCreate(doc, patient, "patient.patient_id");

        // Add patient element if we've been given a patient ID.
        // TODO: Is this okay if there's already a patient element?
        // Need to see how the Xforms module behaves.
        if (patientId != null) {
            patientIdElement.setTextContent(String.valueOf(patientId));
        }

        // NOTE(ping): We use a form_resource named <form-name>.xFormXslt to alter the translation
        // from XML to HL7 so that the encounter_datetime is recorded with a date and time.
        // (The default XSLT transform records only the date, not the time.)  This means that
        // IF THE FORM IS RENAMED, THE FORM_RESOURCE MUST ALSO BE RENAMED, or the encounter
        // datetime will be recorded with only a date and the time will always be 00:00.

        // TODO(ping): We should have some code here to ensure that the correct XSLT exists
        // for every form; otherwise, we will lose it when a form is renamed.

        // OpenMRS can't handle the encounter_datetime in the format we receive.
        fixEncounterDatetimeElement(doc);

        // Make sure that all observations are under the obs element, with appropriate attributes
        Element obs = getFirstElementOrCreate(doc, root, "obs");
        obs.setAttribute("openmrs_concept", "1238^MEDICAL RECORD OBSERVATIONS^99DCT");
        obs.setAttribute("openmrs_datatype", "ZZ");
        for (Element element : getElements(root)) {
            if (!KNOWN_CHILD_ELEMENTS.contains(element.getLocalName())) {
                for (Element observation : getElements(element)) {
                    obs.appendChild(observation);
                }
                removeNode(element);
            }
        }
        return XformsUtil.doc2String(doc);
    }

    /**
     * Searches for an element among the descendants of a given root element,
     * or creates it as an immediate child of the given element.
     */
    private static Element getFirstElementOrCreate(
        Document doc, Element parent, String elementName) {
        NodeList patientElements = parent.getElementsByTagName(elementName);
        Element patient;
        if (patientElements == null || patientElements.getLength() == 0) {
            patient = doc.createElementNS(null, elementName);
            parent.appendChild(patient);
        } else {
            patient = (Element) patientElements.item(0);
        }
        return patient;
    }

    /** Fixes up the encounter_datetime element to make it acceptable to OpenMRS. */
    private static void fixEncounterDatetimeElement(Document doc) {
        Date datetime = getEncounterDatetime(doc);

        // Work around OpenMRS's inherently client-antagonistic design.
        datetime = Utils.fixEncounterDatetime(datetime);

        // The time zone indicator must be formatted with a colon and minutes;
        // (e.g. "+01:00" instead of "+0100"); otherwise OpenMRS fails to parse
        // it, as Saxon datetime parsing can't handle timezones without minutes.
        // For more details on this workaround, see
        // https://docs.google.com/document/d/1IT92y_YP7AnhpDfdelbS7huxNKswa4VSXYPzqbnkWik/edit
        String formattedDatetime = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(datetime);
        getElementOrThrow(
            getElementOrThrow(doc.getDocumentElement(), "encounter"),
            "encounter.encounter_datetime"
        ).setTextContent(formattedDatetime);
    }

    /** Extracts and parses the encounter date from a submitted encounter. */
    private static Date getEncounterDatetime(Document doc) {
        Element element = getElementOrThrow(
            getElementOrThrow(doc.getDocumentElement(), "encounter"),
            "encounter.encounter_datetime");

        Date encounterDate = parseTimestamp(element.getTextContent());
        if (encounterDate == null) {
            LOG.warn("No encounter_datetime found; using the current time");
            encounterDate = new Date();
        }
        return encounterDate;
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
            "yyyyMMdd"
        );
        for (String pattern : acceptablePatterns) {
            try {
                return new SimpleDateFormat(pattern).parse(timestamp);
            } catch (ParseException e) { /* ignore and continue */ }
        }
        return null;
    }
}
