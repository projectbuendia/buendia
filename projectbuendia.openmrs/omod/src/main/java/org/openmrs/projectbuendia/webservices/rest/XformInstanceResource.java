package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.xforms.XformsQueueProcessor;
import org.openmrs.module.xforms.util.XformsUtil;
import org.projectbuendia.openmrs.webservices.rest.RestController;
import org.springframework.format.datetime.joda.JodaDateTimeFormatAnnotationFormatterFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.*;

/**
 * Resource for instances of xforms (i.e. filled in forms). Currently write-only
 */
// TODO(jonskeet): Still not really sure what supportedClass to use here... can
// we omit it?
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xforminstance", supportedClass = SimpleObject.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class XformInstanceResource implements Creatable {
    static final RequestLogger logger = RequestLogger.LOGGER;

    // Everything not in this set is assumed to be a group of observations.
    private static final Set<String> KNOWN_CHILD_ELEMENTS = new HashSet<>();

    static {
        KNOWN_CHILD_ELEMENTS.add("header");
        KNOWN_CHILD_ELEMENTS.add("patient");
        KNOWN_CHILD_ELEMENTS.add("patient.patient_id");
        KNOWN_CHILD_ELEMENTS.add("encounter");
        KNOWN_CHILD_ELEMENTS.add("obs");
    }

    static String XML_PROPERTY = "xml";
    static String PATIENT_ID_PROPERTY = "patient_id";
    static String PATIENT_UUID_PROPERTY = "patient_uuid";
    static String ENTERER_ID_PROPERTY = "enterer_id";
    static String DATE_ENTERED_PROPERTY = "date_entered";

    private static final XformsQueueProcessor processor = new XformsQueueProcessor();

    @SuppressWarnings("unused")
    private static final Log getLog() {
        // TODO(kpy): Figure out why getLog(XformInstanceResource.class) gives
        // no log output.  Using "org.openmrs.api" works, though.
        return LogFactory.getLog("org.openmrs.api");
    }

    private final PatientService patientService;

    public XformInstanceResource() {
        patientService = Context.getPatientService();
    }

    @Override
    public String getUri(Object instance) {
        // TODO Auto-generated method stub
        return null;
    }

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

    private Object createInner(SimpleObject post, RequestContext context) throws ResponseException {
        try {
            String xml = completeXform(convertIdIfNecessary(post));
            File file = File.createTempFile("projectbuendia", null);
            adjustSystemClock(xml);
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

    /** Extracts the encounter date from a submitted encounter. */
    private Date getEncounterDate(Document doc) {
        Element root = doc.getDocumentElement();
        Element encounterDatetime = getElementOrThrow(
                getElementOrThrow(root, "encounter"),
                "encounter.encounter_datetime");

        // The code in completeXform converts the encounter_datetime using
        // ISO_DATETIME_TIME_ZONE_FORMAT.format() to ensure that the time zone
        // indicator contains a colon ("+01:00" instead of "+0100"); without
        // this colon, OpenMRS fails to parse the date.  Surprisingly, a new
        // SimpleDateFormat(ISO_DATETIME_TIME_ZONE_FORMAT.getPattern() cannot
        // parse the string produced by ISO_DATETIME_TIME_ZONE_FORMAT.format().
        // For safety we accept a few reasonable date formats, including
        // "yyyy-MM-dd'T'HH:mm:ss.SSSX", which can parse both kinds of time
        // zone indicator ("+01:00" and "+0100").
        List<String> acceptablePatterns = Arrays.asList(
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd HH:mm:ss"
        );

        String datetimeText = encounterDatetime.getTextContent();
        for (String pattern : acceptablePatterns) {
            try {
                return new SimpleDateFormat(pattern).parse(datetimeText);
            } catch (ParseException e) { }
        }
        return new Date();
    }

    // TODO(kpy): We're switching to having the server be the authoritative
    // time source via NTP, so instead of pushing the server's clock forward,
    // we should just adjust the date to ensure it's in the past.

    /**
     * Adjusts the system clock to ensure that the incoming encounter date
     * is not in the future.  <b>This is a temporary hack</b> intended to work
     * around the fact that the Edison system clock does not stay running
     * while power is off; when it falls behind, a validation constraint in
     * OpenMRS starts rejecting all incoming encounters because they have
     * dates in the future.  To work around this, we attempt to push the
     * system clock forward whenever we receive an encounter that appears to
     * be in the future.  The system clock is set by a setuid executable
     * program "/usr/bin/buendia-pushclock".
     * @param xml
     */
    private void adjustSystemClock(String xml) {
        final String PUSHCLOCK = "/usr/bin/buendia-pushclock";

        if (!new File(PUSHCLOCK).exists()) {
            getLog().warn(PUSHCLOCK + " is missing; not adjusting the clock");
            return;
        }

        try {
            Document doc = XmlUtil.parse(xml);
            Date date = getEncounterDate(doc);
            getLog().info("encounter_datetime parsed as " + date);

            // Convert to seconds.  Allow up to 60 sec for truncation to
            // minutes and up to 60 sec for network and server latency.
            long timeSecs = (date.getTime() / 1000) + 60 + 60;
            Process pushClock = Runtime.getRuntime().exec(
                    new String[] {PUSHCLOCK, "" + timeSecs});
            int code = pushClock.waitFor();
            getLog().info("buendia-pushclock " + timeSecs + " -> exit code " + code);
        } catch (SAXException | IOException | InterruptedException e) {
            getLog().error("adjustSystemClock failed:", e);
        }
    }

    private SimpleObject convertIdIfNecessary(SimpleObject post) {
        Object patientId = post.get(PATIENT_ID_PROPERTY);

        if (patientId != null) {
            return post;
        }

        String uuid = (String) post.get(PATIENT_UUID_PROPERTY);
        if (uuid != null) {

            Patient patient = patientService.getPatientByUuid(uuid);
            if (patient == null) {
                throw new IllegalPropertyException("Patient UUID did not exist: " + uuid);
            }
            post.add(PATIENT_ID_PROPERTY, patient.getPatientId());
            return post;
        }

        return post;
    }

    // VisibleForTesting
    /**
     * Add appropriate sections to the XForm we receive, to include patient ID
     * (where present), etc.
     */
    static String completeXform(SimpleObject post) throws SAXException, IOException {
        String xml = (String) post.get(XML_PROPERTY);
        Integer patientId = (Integer) post.get(PATIENT_ID_PROPERTY);

        int entererId = (Integer) post.get(ENTERER_ID_PROPERTY);
        String dateEntered = (String) post.get(DATE_ENTERED_PROPERTY);
        dateEntered = workAroundClientIssue(dateEntered);
        Document doc = XmlUtil.parse(xml);

        // If we haven't been given a patient id, then the XForms processor will create a patient
        // then fill in the patient.patient_id in the DOM. However, it won't actually create the node,
        // just fill it in. So whatever the case, make sure a patient.patient_id node exists.

        Element root = doc.getDocumentElement();
        Element patient = getFirstElementOrCreate(doc, root, "patient");
        Element patientIdElement = getFirstElementOrCreate(doc, patient, "patient.patient_id");

        // Add patient element if we've been given a patient ID.
        // TODO(jonskeet): Is this okay if there's already a patient element?
        // Need to see how the Xforms module behaves.
        if (patientId != null) {
            patientIdElement.setTextContent(String.valueOf(patientId));
        }

        // Modify header element
        Element header = getElementOrThrow(root, "header");
        getElementOrThrow(header, "enterer").setTextContent(entererId + "^");
        getElementOrThrow(header, "date_entered").setTextContent(dateEntered);

        // NOTE(kpy): We use a form_resource named <form-name>.xFormXslt to alter the translation
        // from XML to HL7 so that the encounter_datetime is recorded with a date and time.
        // (The default XSLT transform records only the date, not the time.)  This means that
        // IF THE FORM IS RENAMED, THE FORM RESOURCE MUST ALSO BE RENAMED, or the encounter
        // datetime will be recorded with only a date and the time will always be 00:00.

        // Modify encounter.encounter_datetime to make sure the timezone format has a minute section
        // See https://docs.google.com/document/d/1IT92y_YP7AnhpDfdelbS7huxNKswa4VSXYPzqbnkWik/edit
        // for an explanation why. Saxon datetime parsing can't cope with timezones without minutes
        Element encounterDatetimeElement =
                getElementOrThrow(getElementOrThrow(root, "encounter"), "encounter.encounter_datetime");
        String datetime = encounterDatetimeElement.getTextContent();
        if (datetime != null) {
            try {
                // SimpleDateFormat to handle ISO8601, being lenient on timezone.
                Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse(datetime);
               // Reformat with the stricter apache class
                String corrected = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(date);
                encounterDatetimeElement.setTextContent(corrected);
            } catch (ParseException e) {
                getLog().warn("failed to do date correction on " + datetime);
            }
        }
        //TODO(nfortescue); we should also have some code here to ensure that the correct XLST exists for every form
        // otherwise we lose it on form rename.

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

    // VisibleForTesting
    // Before a fix, the Android client posted a date of yyyyMMddTHHmmss.SSSZ 
    static String workAroundClientIssue(String fromClient) {
        // Just detect it by the lack of hyphens...
        if (fromClient.indexOf('-') == -1) {
            // Convert to yyyy-MM-ddTHH:mm:ss.SSS
            fromClient = new StringBuilder(fromClient)
                .insert(4, '-')
                .insert(7, '-')
                .insert(13, ':')
                .insert(16, ':')
                .toString();
        }
        return fromClient;
    }

    private static Element getFirstElementOrCreate(Document doc, Element parent, String elementName) {
        NodeList patientElements = parent.getElementsByTagName(elementName);
        Element patient;
        if (patientElements == null || patientElements.getLength() == 0) {
            patient = doc.createElementNS(null, elementName);
            parent.appendChild(patient);
        } else {
            patient = (Element)patientElements.item(0);
        }
        return patient;
    }
}
