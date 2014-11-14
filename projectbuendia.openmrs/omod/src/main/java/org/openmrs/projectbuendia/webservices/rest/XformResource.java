package org.openmrs.projectbuendia.webservices.rest;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.Hyperlink;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.xforms.XformBuilderEx;
import org.openmrs.module.xforms.util.XformsUtil;
import org.projectbuendia.openmrs.webservices.rest.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Resource for xform templates (i.e. forms without data).
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up. 
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xform", supportedClass = Form.class, supportedOpenmrsVersions = "1.10.*")
public class XformResource implements Listable, Retrievable, Searchable {
    private static String HTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    private static String XFORMS_NAMESPACE = "http://www.w3.org/2002/xforms";
    private static final DocumentBuilder documentBuilder;

    static {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
	private final Log log = LogFactory.getLog(getClass());

	private FormService formService;
	
	public XformResource() {
		this.formService = Context.getFormService();
	}
	
    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        List<SimpleObject> jsonResults = new ArrayList<>();
        List<Form> forms = formService.getAllForms(false);
        for (Form form : forms) {
        	SimpleObject jsonForm = toSimpleObject(form, context.getRepresentation());
        	jsonResults.add(jsonForm);
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }

	private SimpleObject toSimpleObject(Form form, Representation representation) {
		SimpleObject jsonForm = new SimpleObject();
		jsonForm.add("name", form.getName());
		jsonForm.add("date_changed", form.getDateChanged());
		jsonForm.add("date_created", form.getDateCreated());
		jsonForm.add("version", form.getVersion());
		jsonForm.add("uuid", form.getUuid());
		jsonForm.add("links", getLinks(form));
		if (representation == Representation.FULL) {
			try {
			    // TODO(jonskeet): Use description instead of name?
				jsonForm.add("xml", convertToOdkCollect(XformBuilderEx.buildXform(form), form.getName()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return jsonForm;
	}
	
	// Visible for testing
	/**
	 * Converts a vanilla Xform into one that ODK Collect is happy to work with.
	 * This requires:
	 * <ul>
	 *  <li>Changing the namespace of the the root element to http://www.w3.org/1999/xhtml
	 *  <li>Wrapping the model element in an HTML head element, with a title child element
	 *  <li>Wrapping remaining elements in an HTML body element
	 * </ul>
	 */
	static String convertToOdkCollect(String xml, String title) throws IOException, SAXException {
        // Change the namespace of the root element. I haven't figured out a way to do
        // this within a document; removing the root element from the document seems
        // to do odd things... so instead, we import it into a new document.
	    Document oldDoc = documentBuilder.parse(new InputSource(new StringReader(xml)));
	    Document doc = documentBuilder.newDocument();
	    Element root = (Element) doc.importNode(oldDoc.getDocumentElement(), true);
	    root = (Element) doc.renameNode(root, HTML_NAMESPACE, "h:form");
	    doc.appendChild(root);

	    // Prepare the new wrapper elements
	    Element head = doc.createElementNS(HTML_NAMESPACE, "h:head");
	    Element titleElement = doc.createElementNS(HTML_NAMESPACE, "h:title");
	    titleElement.setTextContent(title);
	    head.appendChild(titleElement);
        Element body = doc.createElementNS(HTML_NAMESPACE, "h:body");

        // Find the model element to go in the head, and all its following siblings to go in the body.
        // We do this before moving any elements, for the sake of sanity.
	    Element model = getElementOrThrow(root, XFORMS_NAMESPACE, "model");
        List<Node> nodesAfterModel = new ArrayList<>();
        Node nextSibling = model.getNextSibling();
        while (nextSibling != null) {
            nodesAfterModel.add(nextSibling);
            nextSibling = nextSibling.getNextSibling();
        }

        // Now we're done with the preparation, we can move everything.
        head.appendChild(model);
        for (Node node : nodesAfterModel) {
            body.appendChild(node);
        }
        
        // Having removed the model and everything after it, we can now just append the head
        // and body to the document element...
        root.appendChild(head);
        root.appendChild(body);
	
	    return XformsUtil.doc2String(doc);
	}
    
    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT, Representation.FULL, Representation.REF);
    }

    private List<Hyperlink> getLinks(Form form) {
        List<Hyperlink> links = new ArrayList<>();        
        links.add(new Hyperlink("self", getUri(form)));
        return links;
    }

    @Override
    public String getUri(Object instance) {
    	Form form = (Form) instance;
		Resource res = getClass().getAnnotation(Resource.class);
		return RestConstants.URI_PREFIX + res.name() + "/" + form.getUuid();
    }

    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
    	Form form = formService.getFormByUuid(uuid);
    	if (form == null) {
    		throw new ObjectNotFoundException();
    	}
    	return toSimpleObject(form, context.getRepresentation());
    }

    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        throw new UnsupportedOperationException();
    }

    private static Element getElementOrThrow(Element element, String namespaceURI, String localName) {
        NodeList elements = element.getElementsByTagNameNS(namespaceURI, localName);
        if (elements.getLength() != 1) {
            throw new IllegalPropertyException("Element "
                    + element.getNodeName() + " must have exactly one " + localName
                    + " element");
        }
        return (Element) elements.item(0);
    }
}
