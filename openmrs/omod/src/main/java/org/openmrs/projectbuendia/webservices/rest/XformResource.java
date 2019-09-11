package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Provider;
import org.openmrs.api.FormService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.xforms.buendia.BuendiaXformBuilderEx;
import org.openmrs.module.xforms.buendia.FormData;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.util.FormConstants;
import org.projectbuendia.openmrs.webservices.rest.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import static org.openmrs.projectbuendia.Utils.eq;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtils.elementsIn;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtils.removeNode;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtils.requireDescendant;

@Resource(
    name = RestController.PATH + "/xforms",
    supportedClass = Form.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class XformResource extends BaseResource<Form> {
    private static final String HTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    private static final String XFORMS_NAMESPACE = "http://www.w3.org/2002/xforms";

    public XformResource() {
        super("XForms", Representation.DEFAULT);
    }

    @Override protected Collection<Form> listItems(RequestContext context) {
        return getXformForms(formService);
    }

    public static Collection<Form> getXformForms(FormService formService) {
        List<Form> results = new ArrayList<>();
        for (Form form : formService.getAllForms()) {
            if (DbUtils.isPublishedXform(form)) results.add(form);
        }
        return results;
    }

    @Override protected Form retrieveItem(String uuid) {
        Form form = formService.getFormByUuid(uuid);
        return DbUtils.isPublishedXform(form) ? form : null;
    }

    /**
     * Adds the following fields to the {@link SimpleObject}:
     *   - name: display type of the form
     *   - date_created: the date the form was created, as ms since epoch
     *   - version: the version number of the form (e.g. 0.2.1)
     *   - date_changed: the date the form was last modified, as ms since epoch;
     *     for forms that contain a provider field, this date will also be
     *     updated whenever the set of providers on the server changes
     * If the query parameter "?v=full" is present, also adds the "xml" field
     * containing the XML of the form model definition.
     */
    @Override protected void populateJson(SimpleObject json, Form form, RequestContext context) {
        json.add("name", form.getName());
        json.add("id", form.getFormId());
        json.add("version", form.getVersion());
        Date dateChanged = form.getDateChanged();
        json.add("date_created", form.getDateCreated());
        json.add("version", form.getVersion());
        boolean includesProviders = false;
        if (context.getRepresentation() == Representation.FULL) {
            try {
                FormData formData = BuendiaXformBuilderEx.buildXform(
                    form, new BuendiaXformCustomizer(DbUtils.getLocaleForTag(context.getParameter("locale"))));
                String xml = convertToOdkCollect(formData.xml, form.getName());
                includesProviders = formData.includesProviders;
                xml = removeRelationshipNodes(xml);
                json.add("xml", xml);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            // Do a linear search, as otherwise it puts too many assumptions on
            // comparison order.  Also FormField overrides compare to be based
            // on lots of fields, but leaves .equals() based on UUID unchanged,
            // which is really dangerous.
            for (FormField formField : form.getFormFields()) {
                Field field = formField.getField();
                if (eq(field.getFieldType().getFieldTypeId(),
                    FormConstants.FIELD_TYPE_DATABASE)
                    && eq(field.getTableName(), "encounter")) {
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

    /**
     * Converts a vanilla Xform into one that ODK Collect is happy to work with, by:
     *   - Changing the namespace of the the root element to http://www.w3.org/1999/xhtml
     *   - Wrapping the model element in an HTML head element with a title element
     *   - Wrapping the remaining elements in an HTML body element
     */
    static String convertToOdkCollect(String xml, String title) throws IOException, SAXException {
        // Change the namespace of the root element. I haven't figured out a way
        // to do
        // this within a document; removing the root element from the document
        // seems
        // to do odd things... so instead, we import it into a new document.
        Document oldDoc = XmlUtils.parse(xml);
        Document doc = XmlUtils.createDocumentBuilder().newDocument();
        Element root = (Element) doc.importNode(oldDoc.getDocumentElement(), true);
        root = (Element) doc.renameNode(root, HTML_NAMESPACE, "h:form");
        doc.appendChild(root);

        // Prepare the new wrapper elements
        Element head = doc.createElementNS(HTML_NAMESPACE, "h:head");
        Element titleElement = XmlUtils.appendChild(head, HTML_NAMESPACE, "h:title");
        titleElement.setTextContent(title);
        Element body = doc.createElementNS(HTML_NAMESPACE, "h:body");

        // Find the model element to go in the head, and all its following
        // siblings to go in the body.
        // We do this before moving any elements, for the sake of sanity.
        Element model = requireDescendant(root, XFORMS_NAMESPACE, "model");
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
        // append the head and body to the document element...
        root.appendChild(head);
        root.appendChild(body);

        return XformsUtil.doc2String(doc);
    }

    /**
     * Removes the relationship nodes added (unconditionally) by xforms. If
     * XFRM-189 is fixed, this method can go away.
     */
    static String removeRelationshipNodes(String xml) throws IOException, SAXException {
        Document doc = XmlUtils.parse(xml);
        removeBinding(doc, "patient_relative");
        removeBinding(doc, "patient_relative.person");
        removeBinding(doc, "patient_relative.relationship");

        for (Element relative : elementsIn(doc
            .getElementsByTagNameNS("", "patient_relative"))) {
            removeNode(relative);
        }

        // Remove every parent of a label element with a text of
        // "RELATIONSHIPS". (Easiest way to find the ones added...)
        for (Element label : elementsIn(doc
            .getElementsByTagNameNS(XFORMS_NAMESPACE, "label"))) {
            Element parent = (Element) label.getParentNode();
            if (eq(XFORMS_NAMESPACE, parent.getNamespaceURI())
                && eq(parent.getLocalName(), "group")
                && eq(label.getTextContent(), "RELATIONSHIPS")) {
                removeNode(parent);
                // We don't need to find other labels now, especially if they
                // may already have been removed.
                break;
            }
        }
        return XformsUtil.doc2String(doc);
    }

    /** Returns the later of two nullable dates. */
    private Date maxDate(@Nullable Date d1, @Nullable Date d2) {
        if (d1 == null) return d2;
        if (d2 == null) return d1;
        return d1.before(d2) ? d2 : d1;
    }

    /**
     * Returns the actual last modification time of an OpenMRS object.
     * Because OpenMRS doesn't set the modification time upon initial
     * creation (sigh) we have to check both dateChanged and dateCreated.
     */
    private Date dateChanged(BaseOpenmrsMetadata d) {
        Date dateChanged = d.getDateChanged();
        if (dateChanged != null) return dateChanged;
        return d.getDateCreated();
    }

    // Visible for testing

    private static void removeBinding(Document doc, String id) {
        for (Element binding : elementsIn(
            doc.getElementsByTagNameNS(XFORMS_NAMESPACE, "bind"))) {
            if (eq(binding.getAttribute("id"), id)) {
                removeNode(binding);
                return;
            }
        }
    }
}