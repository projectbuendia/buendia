package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Provider;
import org.openmrs.api.FormService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.util.FormConstants;
import org.projectbuendia.openmrs.webservices.rest.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.*;

/**
 * Resource for xform templates (i.e. forms without data). Note: this is under
 * org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xform", supportedClass = Form.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class XformResource extends AbstractReadOnlyResource<Form> {

    private static final String HTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    private static final String XFORMS_NAMESPACE = "http://www.w3.org/2002/xforms";

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(getClass());

    private final FormService formService;
    private final ProviderService providerService;

    public XformResource() {
        super("xform", Representation.DEFAULT, Representation.FULL, Representation.REF);
        this.formService = Context.getFormService();
        this.providerService = Context.getProviderService();
    }
    
    @Override
    protected void populateJsonProperties(Form form, RequestContext context, SimpleObject json) {
        json.add("name", form.getName());
        Date dateChanged = form.getDateChanged();
        json.add("date_created", form.getDateCreated());
        json.add("version", form.getVersion());
        boolean includesProviders = false;
        if (context.getRepresentation() == Representation.FULL) {
            try {
                // TODO(jonskeet): Use description instead of name?
                FormData formData = BuendiaXformBuilderEx.buildXform(form, new BuendiaXformCustomizer());
                String xml = convertToOdkCollect(formData.xml, form.getName());
                includesProviders = formData.includesProviders;
                xml = removeRelationshipNodes(xml);
                json.add("xml", xml);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            // Do a linear search, as otherwise it puts too many assumptions on comparison order.
            // Also FormField overrides compare to be based on lots of fields, but leaves .equals() based
            // on UUID unchanged, which is really dangerous.
            for (FormField formField : form.getFormFields()) {
                Field field = formField.getField();
                if (FormConstants.FIELD_TYPE_DATABASE.equals(field.getFieldType().getFieldTypeId()) &&
                        "encounter".equals(field.getTableName())) {
                    includesProviders = true;
                }
                dateChanged = maxDate(dateChanged, dateChanged(formField));
            }
        }
        if (includesProviders) {
            for (Provider provider : providerService.getAllProviders()) {
                dateChanged = maxDate(dateChanged, dateChanged(provider));
            }
        }
        json.add("date_changed", dateChanged);
    }

    private Date dateChanged(BaseOpenmrsMetadata d) {
        Date dateChanged = d.getDateChanged();
        if (dateChanged != null) {
            return dateChanged;
        }
        return d.getDateCreated();
    }

    private Date maxDate(Date d1, Date d2) {
        if (d1 == null) {
            return d2;
        }
        if (d2 == null) {
            return d1;
        }
        return d1.before(d2) ? d2 : d1;
    }

    @Override
    protected Form retrieveImpl(String uuid, RequestContext context) {
        return formService.getFormByUuid(uuid);
    }
    
    @Override
    protected Iterable<Form> searchImpl(RequestContext context) {
        // No query parameters supported - just give all the forms
        return formService.getAllForms(false);
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
}
