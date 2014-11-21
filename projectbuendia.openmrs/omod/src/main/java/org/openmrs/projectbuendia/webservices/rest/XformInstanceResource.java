package org.openmrs.projectbuendia.webservices.rest;

import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.getElementOrThrow;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.getElements;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.removeNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
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
import org.openmrs.module.xforms.util.DOMUtil;
import org.openmrs.module.xforms.util.XformsUtil;
import org.projectbuendia.openmrs.webservices.rest.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Resource for instances of xforms (i.e. filled in forms). Currently write-only
 */
// TODO(jonskeet): Still not really sure what supportedClass to use here... can
// we omit it?
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xforminstance", supportedClass = SimpleObject.class, supportedOpenmrsVersions = "1.10.*")
public class XformInstanceResource implements Creatable {

    // Everything not in this set is assumed to be a group of observations.
    private static final Set<String> KNOWN_CHILD_ELEMENTS = new HashSet<String>();

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
    private final Log log = LogFactory.getLog(getClass());

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
    public Object create(SimpleObject post, RequestContext context) throws ResponseException {
        String xml = "";
        xml = "";
        try {
            xml = completeXform(convertIdIfNecessary(post));
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

        // We want to return the UUID, MSF patient ID, given name, and family
        // name of the new Patient to the client as well, to support the
        // Unfortunately, XformsQueueProcessor.processXForm doesn't return the
        // new Patient object, so we have to get it in the roundabout way of
        // looking it up by the provided MSF patient ID.
        Patient patient = null;
        String msfPatientId = getMsfPatientIdFromFormInstanceXml(xml);
        if (msfPatientId != null) {
            PatientService service = Context.getPatientService();
            List<PatientIdentifierType> idTypes = new ArrayList<>();
            idTypes.add(PatientResource.getMsfIdentifierType());
            List<Patient> patients = service.getPatients("", msfPatientId, idTypes, true);
            if (!patients.isEmpty()) {
                patient = patients.get(0);
                post = (SimpleObject) post.clone();
                post.add("uuid", patient.getUuid());
                post.add("id", msfPatientId);
                post.add("given_name", patient.getGivenName());
                post.add("family_name", patient.getFamilyName());
            }
        }
        return post;
    }

    /** Finds and returns the content of the msf_patient_id element in an XML string. */
    public String getMsfPatientIdFromFormInstanceXml(String xml) {
        Document doc = null;
        try {
            doc = XmlUtil.parse(xml);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String msf_patient_id = "";
        Element root = doc.getDocumentElement();
        NodeList msf_patient_ids = root.getElementsByTagName("msf_patient_id");
        if (msf_patient_ids.getLength() == 1) {
            return msf_patient_ids.item(0).getTextContent();
        }
        return null;
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
