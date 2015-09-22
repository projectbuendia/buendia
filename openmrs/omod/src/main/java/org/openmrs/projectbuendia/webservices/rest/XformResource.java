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
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.getElementOrThrowNS;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.removeNode;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.toElementIterable;

/**
 * Resource for "form models" (not-yet-filled-in forms).   Note: this is under
 * org.openmrs as otherwise the resource annotation isn't picked up.
 * @see AbstractReadOnlyResource
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xforms",
    supportedClass = Form.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
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

    /**
     * Adds the following fields to the {@link SimpleObject}:
     * <ul>
     * <li>name: display name of the form
     * <li>date_created: the date the form was created, as ms since epoch
     * <li>version: the version number of the form (e.g. 0.2.1)
     * <li>date_changed: the date the form was last modified, as ms since epoch;
     * for forms that contain a provider field, this date will also be
     * updated whenever the set of providers on the server changes
     * </ul>
     * <p/>
     * If the query parameter "?v=full" is present, also adds the "xml" field
     * containing the XML of the form model definition.
     * @param context      the request context; specify "v=full" in the URL params
     *                     to include the XML for the form model in the response
     * @param snapshotTime ignored
     */
    @Override protected void populateJsonProperties(
        Form form, RequestContext context, SimpleObject json, long snapshotTime) {
        json.add("name", form.getName());
        json.add("id", form.getFormId());
        json.add("version", form.getVersion());
        Date dateChanged = form.getDateChanged();
        json.add("date_created", form.getDateCreated());
        json.add("version", form.getVersion());
        boolean includesProviders = false;
        if (context.getRepresentation() == Representation.FULL) {
            try {
                // TODO: Use description instead of name?
                FormData formData = BuendiaXformBuilderEx.buildXform(
                    form, new BuendiaXformCustomizer());
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
                if (FormConstants.FIELD_TYPE_DATABASE.equals(
                    field.getFieldType().getFieldTypeId())
                    && "encounter".equals(field.getTableName())) {
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
     * Converts a vanilla Xform into one that ODK Collect is happy to work with.
     * This requires:
     * <ul>
     * <li>Changing the namespace of the the root element to http://www.w3.org/1999/xhtml
     * <li>Wrapping the model element in an HTML head element, with a title child element
     * <li>Wrapping remaining elements in an HTML body element
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
        Document doc = XmlUtil.parse(xml);
        removeBinding(doc, "patient_relative");
        removeBinding(doc, "patient_relative.person");
        removeBinding(doc, "patient_relative.relationship");

        for (Element relative : toElementIterable(doc
            .getElementsByTagNameNS("", "patient_relative"))) {
            removeNode(relative);
        }

        // Remove every parent of a label element with a text of
        // "RELATIONSHIPS". (Easiest way to find the ones added...)
        for (Element label : toElementIterable(doc
            .getElementsByTagNameNS(XFORMS_NAMESPACE, "label"))) {
            Element parent = (Element) label.getParentNode();
            if (XFORMS_NAMESPACE.equals(parent.getNamespaceURI())
                && parent.getLocalName().equals("group")
                && "RELATIONSHIPS".equals(label.getTextContent())) {
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
        for (Element binding : toElementIterable(
            doc.getElementsByTagNameNS(XFORMS_NAMESPACE, "bind"))) {
            if (binding.getAttribute("id").equals(id)) {
                removeNode(binding);
                return;
            }
        }
    }

    // VisibleForTesting

    /**
     * Retrieves a single xform with the given UUID.  See
     * {@link #populateJsonProperties(Form, RequestContext, SimpleObject, long)}
     * for details on the context and snapshotTime arguments.
     * @param context      unused here
     * @param snapshotTime unused here
     * @see AbstractReadOnlyResource#retrieve(String, RequestContext)
     */
    @Override protected Form retrieveImpl(String uuid, RequestContext context, long snapshotTime) {
        return formService.getFormByUuid(uuid);
    }

    /**
     * Returns all xforms (there is no support for query parameters).  See
     * {@link #populateJsonProperties(Form, RequestContext, SimpleObject, long)}
     * for details on the context and snapshotTime arguments.
     * @param context      unused here
     * @param snapshotTime unused here
     *                     Note: because of a bug in parsing form definitions, "v=full" is currently
     *                     broken for this function
     * @see AbstractReadOnlyResource#search(RequestContext)
     */
    @Override protected Iterable<Form> searchImpl(RequestContext context, long snapshotTime) {
        // TODO/bug: Fix verbose mode. Currently produces the error:
        // "No bind node for bindName _3._bleeding_sites".
        // No query parameters supported - just give all the forms
        List<Form> forms = new ArrayList<>();
        for (Form form : formService.getAllForms()) {
            if (form.getPublished()) {
                forms.add(form);
            }
        }
        return forms;
    }
}
