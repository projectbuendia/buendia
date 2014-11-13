package org.openmrs.projectbuendia.webservices.rest;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Resource for instances of xforms (i.e. filled in forms). Currently write-only
 */
// TODO(jonskeet): Still not really sure what supportedClass to use here... can
// we omit it?
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xforminstance", supportedClass = SimpleObject.class, supportedOpenmrsVersions = "1.10.*")
public class XformInstanceResource implements Creatable {

    private static final DocumentBuilder documentBuilder;

    static {
        try {
            documentBuilder = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final XformsQueueProcessor processor = new XformsQueueProcessor();

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public String getUri(Object instance) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object create(SimpleObject post, RequestContext context)
            throws ResponseException {
        String xml = (String) post.get("xml");
        int patientId = (Integer) post.get("patient_id");
        int entererId = (Integer) post.get("enterer_id");
        String dateEntered = (String) post.get("date_entered");

        try {
            Document doc = documentBuilder.parse(new InputSource(
                    new StringReader(xml)));

            // Add patient element
            Element patient = doc.createElement("patient");
            Element patientIdElement = doc.createElement("patient.patient_id");
            patientIdElement.setTextContent(String.valueOf(patientId));
            doc.getDocumentElement().appendChild(patient);
            patient.appendChild(patientIdElement);

            // Modify header element
            Element header = getElementOrThrow(doc.getDocumentElement(),
                    "header");
            getElementOrThrow(header, "enterer")
                    .setTextContent(entererId + "^");
            getElementOrThrow(header, "date_entered").setTextContent(
                    dateEntered);

            xml = XformsUtil.doc2String(doc);

            File file = File.createTempFile("projectbuendia", null);
            processor.processXForm(xml, file.getAbsolutePath(), true,
                    context.getRequest());
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

    private static Element getElementOrThrow(Element element, String name) {
        NodeList elements = element.getElementsByTagName(name);
        if (elements.getLength() != 1) {
            throw new IllegalPropertyException("Element "
                    + element.getNodeName() + " must have exactly one " + name
                    + " element");
        }
        return (Element) elements.item(0);
    }
}
