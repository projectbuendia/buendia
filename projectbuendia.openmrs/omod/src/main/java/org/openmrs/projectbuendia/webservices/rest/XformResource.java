package org.openmrs.projectbuendia.webservices.rest;

import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.getElementOrThrowNS;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.removeNode;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.toElementIterable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.xforms.util.XformsUtil;
import org.projectbuendia.openmrs.webservices.rest.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Resource for xform templates (i.e. forms without data). Note: this is under
 * org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xform", supportedClass = Form.class, supportedOpenmrsVersions = "1.10.*")
public class XformResource implements Listable, Retrievable, Searchable {

    private static String HTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    private static String XFORMS_NAMESPACE = "http://www.w3.org/2002/xforms";

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(getClass());

    private final FormService formService;

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
                String xml = BuendiaXformBuilder.buildXform(form);
                xml = convertToOdkCollect(xml, form.getName());
                xml = removeRelationshipNodes(xml);
                jsonForm.add("xml", xml);
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
     *   <li>Changing the namespace of the the root element to http://www.w3.org/1999/xhtml
     *   <li>Wrapping the model element in an HTML head element, with a title child element
     *   <li>Wrapping remaining elements in an HTML body element
     * </ul>
     */
    static String convertToOdkCollect(String xml, String title) throws IOException, SAXException {
        // Change the namespace of the root element. I haven't figured out a way
        // to do
        // this within a document; removing the root element from the document
        // seems
        // to do odd things... so instead, we import it into a new document.
        Document oldDoc = XmlUtil.parse(xml);
        Document doc = XmlUtil.getDocumentBuilder().newDocument();
        Element root = (Element) doc.importNode(oldDoc.getDocumentElement(), true);
        root = (Element) doc.renameNode(root, HTML_NAMESPACE, "h:form");
        doc.appendChild(root);

        // Prepare the new wrapper elements
        Element head = doc.createElementNS(HTML_NAMESPACE, "h:head");
        Element titleElement = XmlUtil.appendElementNS(head, HTML_NAMESPACE, "h:title");
        titleElement.setTextContent(title);
        Element body = doc.createElementNS(HTML_NAMESPACE, "h:body");

        // Find the model element to go in the head, and all its following
        // siblings to go in the body.
        // We do this before moving any elements, for the sake of sanity.
        Element model = getElementOrThrowNS(root, XFORMS_NAMESPACE, "model");
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

        // Having removed the model and everything after it, we can now just
        // append the head
        // and body to the document element...
        root.appendChild(head);
        root.appendChild(body);

        return XformsUtil.doc2String(doc);
    }

    // VisibleForTesting
    /**
     * Removes the relationship nodes added (unconditionally) by xforms. If
     * XFRM-189 is fixed, this method can go away.
     */
    static String removeRelationshipNodes(String xml) throws IOException, SAXException {
        Document doc = XmlUtil.parse(xml);
        removeBinding(doc, "patient_relative");
        removeBinding(doc, "patient_relative.person");
        removeBinding(doc, "patient_relative.relationship");

        for (Element relative : toElementIterable(doc
                .getElementsByTagNameNS("", "patient_relative"))) {
            removeNode(relative);
        }

        // Remove every parent of a label element with a text of
        // "RELATIONSHIPS". (Easiest
        // way to find the ones added...)
        for (Element label : toElementIterable(doc
                .getElementsByTagNameNS(XFORMS_NAMESPACE, "label"))) {
            Element parent = (Element) label.getParentNode();
            if (XFORMS_NAMESPACE.equals(parent.getNamespaceURI())
                    && parent.getLocalName().equals("group")
                    && "RELATIONSHIPS".equals(label.getTextContent())) {

                removeNode(parent);
                // We don't need to find other labels now, especially if they
                // may already
                // have been removed.
                break;
            }
        }
        return XformsUtil.doc2String(doc);
    }

    private static void removeBinding(Document doc, String id) {
        for (Element binding : toElementIterable(doc.getElementsByTagNameNS(XFORMS_NAMESPACE,
                "bind"))) {
            if (binding.getAttribute("id").equals(id)) {
                removeNode(binding);
                return;
            }
        }
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT, Representation.FULL, Representation.REF);
    }

    private List<Hyperlink> getLinks(Form form) {
        Hyperlink self = new Hyperlink("self", getUri(form));
        self.setResourceAlias("xform");
        List<Hyperlink> links = new ArrayList<>();
        links.add(self);
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
}
