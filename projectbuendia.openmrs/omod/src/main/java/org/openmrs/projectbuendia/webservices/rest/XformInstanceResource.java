package org.openmrs.projectbuendia.webservices.rest;

import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.getElementOrThrow;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.xforms.XformsQueueProcessor;
import org.openmrs.module.xforms.util.XformsUtil;
import org.projectbuendia.openmrs.webservices.rest.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Resource for instances of xforms (i.e. filled in forms). Currently write-only
 */
// TODO(jonskeet): Still not really sure what supportedClass to use here... can
// we omit it?
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xforminstance", supportedClass = SimpleObject.class, supportedOpenmrsVersions = "1.10.*")
public class XformInstanceResource implements Creatable {

    static String XML_PROPERTY = "xml";
    static String PATIENT_ID_PROPERTY = "patient_id";
    static String ENTERER_ID_PROPERTY = "enterer_id";
    static String DATE_ENTERED_PROPERTY = "date_entered";

    private static final XformsQueueProcessor processor = new XformsQueueProcessor();

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(getClass());

    @Override
    public String getUri(Object instance) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object create(SimpleObject post, RequestContext context) throws ResponseException {
        try {
            String xml = completeXform(post);
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
        Document doc = XmlUtil.parse(xml);

        // Add patient element if we've been given a patient ID.
        // TODO(jonskeet): Is this okay if there's already a patient element?
        // Need to see how the Xforms module behaves.
        if (patientId != null) {
            Element patient = doc.createElement("patient");
            Element patientIdElement = doc.createElement("patient.patient_id");
            patientIdElement.setTextContent(String.valueOf(patientId));
            doc.getDocumentElement().appendChild(patient);
            patient.appendChild(patientIdElement);
        }

        // Modify header element
        Element header = getElementOrThrow(doc.getDocumentElement(), "header");
        getElementOrThrow(header, "enterer").setTextContent(entererId + "^");
        getElementOrThrow(header, "date_entered").setTextContent(dateEntered);

        return XformsUtil.doc2String(doc);
    }

}
