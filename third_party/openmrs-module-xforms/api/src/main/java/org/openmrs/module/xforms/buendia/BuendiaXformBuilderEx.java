/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p/>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.xforms.buendia;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptNumeric;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.module.xforms.RelativeBuilder;
import org.openmrs.module.xforms.XformBuilder;
import org.openmrs.module.xforms.XformConstants;
import org.openmrs.module.xforms.formentry.FormEntryWrapper;
import org.openmrs.module.xforms.formentry.FormSchemaFragment;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.projectbuendia.webservices.rest.BuendiaXformCustomizer;
import org.openmrs.util.FormConstants;
import org.openmrs.util.FormUtil;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_APPEARANCE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_BIND;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_CONCEPT_ID;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_CONSTRAINT;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_ID;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_MESSAGE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_NODESET;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_OPENMRS_CONCEPT;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_REQUIRED;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_TYPE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_UUID;
import static org.openmrs.module.xforms.XformBuilder.CONTROL_INPUT;
import static org.openmrs.module.xforms.XformBuilder.CONTROL_REPEAT;
import static org.openmrs.module.xforms.XformBuilder.CONTROL_SELECT;
import static org.openmrs.module.xforms.XformBuilder.CONTROL_SELECT1;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_BASE64BINARY;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_BOOLEAN;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_DATE;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_DATETIME;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_DECIMAL;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_TEXT;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_TIME;
import static org.openmrs.module.xforms.XformBuilder.INSTANCE_ID;
import static org.openmrs.module.xforms.XformBuilder.MODEL_ID;
import static org.openmrs.module.xforms.XformBuilder.NODE_BIND;
import static org.openmrs.module.xforms.XformBuilder.NODE_GROUP;
import static org.openmrs.module.xforms.XformBuilder.NODE_HINT;
import static org.openmrs.module.xforms.XformBuilder.NODE_INSTANCE;
import static org.openmrs.module.xforms.XformBuilder.NODE_ITEM;
import static org.openmrs.module.xforms.XformBuilder.NODE_LABEL;
import static org.openmrs.module.xforms.XformBuilder.NODE_MODEL;
import static org.openmrs.module.xforms.XformBuilder.NODE_VALUE;
import static org.openmrs.module.xforms.XformBuilder.NODE_XFORMS;
import static org.openmrs.module.xforms.XformBuilder.XPATH_VALUE_TRUE;
import static org.openmrs.projectbuendia.Utils.eq;

/**
 * This is a clone of the Xforms module XformBuilderEx class, allowing us to tinker with the view
 * creation code separately from the module itself.
 */
public class BuendiaXformBuilderEx {
    private static final int MALE_CONCEPT_ID = 1534;
    private static final int FEMALE_CONCEPT_ID = 1535;
    private static final String ATTRIBUTE_ROWS = "rows";

    private static final String NS_XFORMS = XformBuilder.NAMESPACE_XFORMS;
    private static final String NS_SCHEMA = XformBuilder.NAMESPACE_XML_SCHEMA;
    private static final String NS_INSTANCE = XformBuilder.NAMESPACE_XML_INSTANCE;

    private static final Log log = LogFactory.getLog(BuendiaXformBuilderEx.class);

    private final Map<String, Element> bindings = new HashMap<>();
    private final Map<FormField, String> bindNames = new HashMap<>();
    private final boolean useConceptIdAsHint;
    private final Locale locale;
    private final XformCustomizer customizer;
    private boolean includesLocations;
    private boolean includesProviders;

    /**
     * Builds an xform for an given an openmrs form. This is the only
     * public member in the class; it constructs an instance (to avoid
     * nasty statics) and then invokes private methods appropriately.
     */
    public static FormData buildXform(Form form, Locale locale) throws Exception {
        return new BuendiaXformBuilderEx(locale).buildXformImpl(form);
    }

    private BuendiaXformBuilderEx(Locale locale) {
        useConceptIdAsHint = "true".equalsIgnoreCase(
            Context.getAdministrationService().getGlobalProperty("xforms.useConceptIdAsHint"));
        this.locale = locale;
        this.customizer = new BuendiaXformCustomizer(locale);
    }

    private static Element append(Node parent, String ns, String localName) {
        Element child = parent.createElement(ns, localName);
        parent.addChild(Element.ELEMENT, child);
        return child;
    }

    private static Element appendText(Node parent, String ns, String localName, String text) {
        Element child = append(parent, ns, localName);
        child.addChild(Element.TEXT, text);
        return child;
    }

    private static Element appendOption(Element parent, String label, String value) {
        Element itemNode = append(parent, NS_XFORMS, NODE_ITEM);
        appendText(itemNode, NS_XFORMS, NODE_LABEL, label);
        appendText(itemNode, NS_XFORMS, NODE_VALUE, value);
        return itemNode;
    }

    private FormData buildXformImpl(Form form) throws Exception {
        boolean includeRelationshipNodes = false;
        /*
         * TODO(jonskeet): Reinstate this code when we're using a version of the
         * Xforms modules which has that property. (Or just don't reinstate...)
         *
         *      !"false".equals(Context.getAdministrationService()
         *  .getGlobalProperty(XformConstants.GLOBAL_PROP_KEY_INCLUDE_PATIENT_RELATIONSHIPS));
         */
        //String schemaXml = XformsUtil.getSchema(form);
        String templateXml = FormEntryWrapper.getFormTemplate(form);

        //Add relationship data node
        if (includeRelationshipNodes) {
            templateXml = templateXml.replace("</patient>", "  <patient_relative>\n      "
                + "<patient_relative.person/>\n      <patient_relative.relationship/>\n    "
                + "</patient_relative>\n  </patient>");
        }

        Document doc = new Document();
        doc.setEncoding(XformConstants.DEFAULT_CHARACTER_ENCODING);

        Element xformsNode = append(doc, NS_XFORMS, NODE_XFORMS);
        xformsNode.setPrefix("xf", NS_XFORMS);
        xformsNode.setPrefix("xs", NS_SCHEMA);
        xformsNode.setPrefix("xsd", NS_SCHEMA);
        xformsNode.setPrefix("xsi", NS_INSTANCE);
        xformsNode.setPrefix("jr", "http://openrosa.org/javarosa");

        Element modelNode = append(xformsNode, NS_XFORMS, NODE_MODEL);
        modelNode.setAttribute(null, ATTRIBUTE_ID, MODEL_ID);

        // All our UI nodes are appended directly into the xforms node.
        // Another alternative would be to create the HTML body node here, and append
        // everything under that.
        Element bodyNode = xformsNode;

        Element instanceNode = append(modelNode, NS_XFORMS, NODE_INSTANCE);
        instanceNode.setAttribute(null, ATTRIBUTE_ID, INSTANCE_ID);

        Element formNode = BuendiaXformBuilder.getDocument(new StringReader(templateXml))
            .getRootElement();
        formNode.setAttribute(null, ATTRIBUTE_UUID, form.getUuid());
        instanceNode.addChild(Element.ELEMENT, formNode);

        // (Note for comparison with XformBuilderEx: schema doc code removed here, as it wasn't
        // actually used.)

        //TODO This block should be replaced with using database field items instead of
        //     parsing the template document.
        Hashtable<String, String> problemList = new Hashtable<>();
        Hashtable<String, String> problemListItems = new Hashtable<>();
        BuendiaXformBuilder.parseTemplate(modelNode, formNode, formNode, bindings, problemList,
            problemListItems, 0);

        buildUiNodes(bodyNode, form);

        //find all conceptId attributes in the document and replace their value with a mapped
        // concept
        String prefSourceName = Context.getAdministrationService().getGlobalProperty(
            XformConstants.GLOBAL_PROP_KEY_PREFERRED_CONCEPT_SOURCE);
        //we only use the mappings if the global property is set
        if (StringUtils.isNotBlank(prefSourceName)) {
            for (int i = 0; i < formNode.getChildCount(); i++) {
                Element childElement = formNode.getElement(i);
                if (childElement != null) {
                    for (int j = 0; j < childElement.getChildCount(); j++) {
                        if (childElement.getElement(j) != null) {
                            Element grandChildElement = childElement.getElement(j);
                            String value = grandChildElement.getAttributeValue(null,
                                ATTRIBUTE_OPENMRS_CONCEPT);
                            if (StringUtils.isNotBlank(value)) {
                                BuendiaXformBuilder.addConceptMapAttributes(
                                    grandChildElement, value);
                            }
                        }
                    }
                }
            }
        }

        if (includeRelationshipNodes) {
            RelativeBuilder.build(modelNode, bodyNode, formNode);
        }

        String xml = BuendiaXformBuilder.fromDoc2String(doc);
        return new FormData(xml, includesProviders, includesLocations);
    }

    private void buildUiNodes(Element parentNode, Form form) {
        buildUiNodes(parentNode, FormUtil.getFormStructure(form), 0);
    }

    private void buildUiNodes(
        Element parentNode, TreeMap<Integer, TreeSet<FormField>> structure, Integer sectionId) {
        TreeSet<FormField> section = structure.get(sectionId);
        if (section == null) return;

        ArrayList<String> tagList = new ArrayList<>();

        for (FormField formField : section) {
            String fieldToken = FormUtil.getXmlToken(formField.getField().getName());
            String bindName = FormUtil.getNewTag(fieldToken, tagList);
            bindNames.put(formField, bindName);

            Field field = formField.getField();
            boolean required = formField.isRequired();
            Element fieldUiNode;

            int fieldTypeId = field.getFieldType().getFieldTypeId();
            if (fieldTypeId == FormConstants.FIELD_TYPE_CONCEPT) {
                Concept concept = field.getConcept();
                ConceptDatatype datatype = concept.getDatatype();

                // TODO(jonskeet): Don't rely on names here? (Do we even need problem lists?)
                if ((bindName.contains("problem_added") || bindName.contains("problem_resolved")) &&
                    formField.getParent() != null &&
                    (formField.getParent().getField().getName().contains("PROBLEM LIST"))) {
                    fieldUiNode = addProblemList(parentNode, formField, concept);
                } else if (eq(bindName, "problem_list")) {
                    // TODO(jonskeet): Work out what we should do here. There won't be any
                    // bindings for this.
                    // The child nodes will be covered by the case above, when we recurse down.
                    fieldUiNode = parentNode;
                } else {
                    String abbr = datatype.getHl7Abbreviation();
                    switch (abbr) {
                        case HL7Constants.HL7_BOOLEAN:
                            fieldUiNode = addUiNode(parentNode, formField, DATA_TYPE_BOOLEAN, CONTROL_INPUT, required);
                            break;
                        case HL7Constants.HL7_DATE:
                            fieldUiNode = addUiNode(parentNode, formField, DATA_TYPE_DATE, CONTROL_INPUT, required);
                            break;
                        case HL7Constants.HL7_DATETIME:
                            fieldUiNode = addUiNode(parentNode, formField, DATA_TYPE_DATETIME, CONTROL_INPUT, required);
                            break;
                        case HL7Constants.HL7_TIME:
                            fieldUiNode = addUiNode(parentNode, formField, DATA_TYPE_TIME, CONTROL_INPUT, required);
                            break;
                        case HL7Constants.HL7_TEXT:
                            fieldUiNode = addUiNode(parentNode, formField, DATA_TYPE_TEXT, CONTROL_INPUT, required);
                            break;
                        case HL7Constants.HL7_NUMERIC:
                            fieldUiNode = addUiNode(parentNode, formField, DATA_TYPE_DECIMAL, CONTROL_INPUT, required);
                            break;
                        case HL7Constants.HL7_CODED:
                        case HL7Constants.HL7_CODED_WITH_EXCEPTIONS:
                            fieldUiNode = addCodedField(parentNode, formField, required);
                            break;
                        case "ED": // This isn't in HL7Constants as far as I can tell.
                            fieldUiNode = addUiNode(parentNode, formField, DATA_TYPE_BASE64BINARY, CONTROL_INPUT, required);
                            break;
                        default:
                            // TODO(jonskeet): Remove this hack when we understand better...
                            if (eq(field.getName(), "OBS")) {
                                fieldUiNode = createGroupNode(parentNode, formField);
                            } else {
                                log.warn("Unhandled HL7 abbreviation " + abbr + " for field " + field.getName());
                                continue; // Skip recursion, go to next field
                            }
                    }
                }
            } else if (fieldTypeId == FormConstants.FIELD_TYPE_SECTION) {
                // TODO(jonskeet): Use the description for a hint?
                fieldUiNode = append(parentNode, NS_XFORMS, NODE_GROUP);
                Element label = append(fieldUiNode, NS_XFORMS, NODE_LABEL);
                label.addChild(Node.TEXT, getDisplayName(formField));
                String appearanceAttribute = customizer.getAppearanceAttribute(formField);
                if (appearanceAttribute != null) {
                    fieldUiNode.setAttribute(null, ATTRIBUTE_APPEARANCE, appearanceAttribute);
                }
            } else if (fieldTypeId == FormConstants.FIELD_TYPE_DATABASE) {
                fieldUiNode = addDatabaseElementUiNode(bindName, formField, parentNode);
            } else {
                // Don't understand this field type
                log.warn("Unhandled field type " + field.getFieldType().getName() + " for field " + field.getName());
                continue; // Skip recursion, go to next field
            }

            // Recurse down to subnodes.
            buildUiNodes(fieldUiNode, structure, formField.getFormFieldId());
        }
    }

    private Element addUiNode(
        Element parentNode, FormField formField, String dataType, String controlName, boolean required) {
        Concept concept = formField.getField().getConcept();
        if (concept.getDatatype().getHl7Abbreviation().equals(HL7Constants.HL7_NUMERIC)) {
            concept = Context.getConceptService().getConceptNumeric(concept.getConceptId());
            if (concept == null) {
                throw new IllegalStateException("Numeric concept could not be fetched for " + formField.getField().getConcept());

            }
        }

        String bindName = bindNames.get(formField);
        Element bindNode = bindings.get(bindName);
        if (bindNode == null) {
            throw new IllegalArgumentException("No bind node for bindName " + bindName);
        }
        bindNode.setAttribute(null, ATTRIBUTE_TYPE, dataType);
        if (required) {
            bindNode.setAttribute(null, ATTRIBUTE_REQUIRED, XPATH_VALUE_TRUE);
        }

        Element controlNode = append(parentNode, NS_XFORMS, controlName);
        controlNode.setAttribute(null, ATTRIBUTE_BIND, bindName);
        if (eq(DATA_TYPE_TEXT, dataType)) {
            Integer rows = customizer.getRows(concept);
            if (rows != null) {
                controlNode.setAttribute(null, ATTRIBUTE_ROWS, rows.toString());
            }
        }
        addHintNode(appendText(
            controlNode, NS_XFORMS, NODE_LABEL, getLabel(concept)
        ), concept);

        if (concept instanceof ConceptNumeric) {
<<<<<<< HEAD
            ConceptNumeric numeric = (ConceptNumeric) concept;
            Double minValue = numeric.getLowAbsolute();
            Double maxValue = numeric.getHiAbsolute();
            boolean precise = numeric.isPrecise();
            String min = minValue != null ? FormSchemaFragment.numericToString(minValue, precise) : null;
            String max = maxValue != null ? FormSchemaFragment.numericToString(maxValue, precise) : null;
            if (min != null) bindNode.setAttribute(null, "constraint-min", min);
            if (max != null) bindNode.setAttribute(null, "constraint-max", max);

            String msgAttr = XformsUtil.isJavaRosaSaveFormat() ? "jr:constraintMsg" : ATTRIBUTE_MESSAGE;
            if (min != null && max != null) {
                bindNode.setAttribute(null, ATTRIBUTE_CONSTRAINT, ". >= " + min + " and . <= " + max);
                bindNode.setAttribute(null, msgAttr, "Value should be between " + min + " and " + max);
            } else if (min != null) {
                bindNode.setAttribute(null, ATTRIBUTE_CONSTRAINT, ". >= " + min);
                bindNode.setAttribute(null, msgAttr, "Value should be at least " + min);
            } else if (max != null) {
                bindNode.setAttribute(null, ATTRIBUTE_CONSTRAINT, ". <= " + max);
                bindNode.setAttribute(null, msgAttr, "Value should be at most " + max);
=======
            ConceptNumeric numericConcept = (ConceptNumeric) concept;
            Double minInclusive = numericConcept.getLowAbsolute();
            Double maxInclusive = numericConcept.getHiAbsolute();

            if (minInclusive != null) {
                String lower = (minInclusive == null ? "" :
                    FormSchemaFragment.numericToString(minInclusive, true));
                if (maxInclusive != null) {
                    String upper = (maxInclusive == null ? "" :
                        FormSchemaFragment.numericToString(maxInclusive, true));
                    bindNode.setAttribute(null, ATTRIBUTE_CONSTRAINT, ". >= " + lower + " and . "
                        + "<= " + upper);
                    bindNode.setAttribute(null,
                        (XformsUtil.isJavaRosaSaveFormat() ? "jr:constraintMsg" :
                            ATTRIBUTE_MESSAGE),
                        "value should be between " + lower + " and " + upper + " inclusive");
                } else {
                    bindNode.setAttribute(null, ATTRIBUTE_CONSTRAINT, ". >= " + lower);
                    bindNode.setAttribute(null,
                        (XformsUtil.isJavaRosaSaveFormat() ? "jr:constraintMsg" :
                            ATTRIBUTE_MESSAGE),
                        "value should be greater than or equal to " + lower);
                }
            } else if (maxInclusive != null) {
                String upper = (maxInclusive == null ? "" :
                    FormSchemaFragment.numericToString(maxInclusive, true));
                bindNode.setAttribute(null, ATTRIBUTE_CONSTRAINT, " . <= " + upper);
                bindNode.setAttribute(null,
                    (XformsUtil.isJavaRosaSaveFormat() ? "jr:constraintMsg" : ATTRIBUTE_MESSAGE),
                    "value should be less than or equal to " + upper);
>>>>>>> Make small changes to types and columns for OpenMRS 2.
            }
        }

        return controlNode;
    }

    private void addCodedUiNodes(
        Element controlNode, Concept concept, Collection<ConceptAnswer> answers, boolean selectMultiple) {
        for (ConceptAnswer answer : answers) {
            Concept answerConcept = answer.getAnswerConcept();
            String conceptName = customizer.getLabel(answerConcept);
            String conceptValue;

            if (answerConcept.getConceptClass().getConceptClassId().equals(HL7Constants.CLASS_DRUG)
                && answer.getAnswerDrug() != null) {
                conceptName = answer.getAnswerDrug().getName();
                if (selectMultiple) {
                    conceptValue = FormUtil.getXmlToken(conceptName);
                } else {
                    conceptValue = FormUtil.conceptToString(answerConcept, locale) +
                        "^" + FormUtil.drugToString(answer.getAnswerDrug());
                }
            } else {
                if (selectMultiple) {
                    conceptValue = FormUtil.getXmlToken(conceptName);
                } else {
                    conceptValue = FormUtil.conceptToString(answerConcept, locale);
                }
            }

            Element itemNode = appendOption(controlNode, conceptName, conceptValue);
            itemNode.setAttribute(null, ATTRIBUTE_CONCEPT_ID, concept.getConceptId().toString());
        }
    }

    private Element addProblemList(Element parentNode, FormField formField, Concept concept) {
        String token = bindNames.get(formField);
        Element groupNode = append(parentNode, NS_XFORMS, NODE_GROUP);
        addHintNode(appendText(groupNode, NS_XFORMS, NODE_LABEL,
            customizer.getLabel(formField.getField().getConcept())), concept);

        Element repeatControl = append(groupNode, NS_XFORMS, CONTROL_REPEAT);
        repeatControl.setAttribute(null, ATTRIBUTE_BIND, token);

        Element controlNode = append(repeatControl, NS_XFORMS, CONTROL_INPUT);
        String nodeset = "problem_list/" + token + "/value";
        String id = nodeset.replace('/', '_');
        controlNode.setAttribute(null, ATTRIBUTE_BIND, id);
        addHintNode(appendText(controlNode, NS_XFORMS, NODE_LABEL, token + " value"), concept);

        Element bindNode = append(bindings.get(token).getParent(), NS_XFORMS, NODE_BIND);
        bindNode.setAttribute(null, ATTRIBUTE_ID, id);
        bindNode.setAttribute(null, ATTRIBUTE_NODESET, "/form/" + nodeset);
        bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
        return groupNode;
    }

    private Element createGroupNode(Element parentNode, FormField formField) {
        String token = bindNames.get(formField);

        Element groupNode = append(parentNode, NS_XFORMS, NODE_GROUP);
        addHintNode(appendText(
            groupNode, NS_XFORMS, NODE_LABEL, getDisplayName(formField)
        ), formField.getField().getConcept());

        if (formField.getMaxOccurs() != null && formField.getMaxOccurs() == -1) {
            Element repeatControl = append(groupNode, NS_XFORMS, CONTROL_REPEAT);
            repeatControl.setAttribute(null, ATTRIBUTE_BIND, token);
            return repeatControl;
        } else {
            groupNode.setAttribute(null, ATTRIBUTE_ID, token);
            return groupNode;
        }
    }

    private String getDisplayName(FormField formField) {
        String name = formField.getDescription();
        if (StringUtils.isNotEmpty(name)) return name;
        name = formField.getName();
        if (StringUtils.isNotEmpty(name)) return name;
        name = formField.getField().getDescription();
        if (StringUtils.isNotEmpty(name)) return name;
        name = formField.getField().getName();
        if (StringUtils.isNotEmpty(name)) return name;
        throw new IllegalArgumentException("No field name available");
    }

    private Element addCodedField(Element parentNode, FormField formField, boolean required) {
        Concept concept = formField.getField().getConcept();
        if (formField.getMaxOccurs() != null && formField.getMaxOccurs().intValue() == -1) {
            return addProblemList(parentNode, formField, concept);
        } else {
            boolean selectMultiple = formField.getField().getSelectMultiple();
            List<ConceptAnswer> answers = new ArrayList<>(concept.getAnswers(false));
            Collections.sort(answers);

            String controlName = selectMultiple ? CONTROL_SELECT : CONTROL_SELECT1;
            Element controlNode = addUiNode(parentNode, formField, DATA_TYPE_TEXT, controlName, required);
            addCodedUiNodes(controlNode, concept, answers, selectMultiple);
            return controlNode;
        }
    }

    private void addHintNode(Element labelNode, Concept concept) {
        String hint = null;
        if (concept.getDescription() != null) {
            hint = concept.getDescription().getDescription();
        }
        if (useConceptIdAsHint) {
            Integer id = concept.getConceptId();
            hint = hint != null ? hint + " [" + id + "]" : "" + id;
        }
        if (hint != null) {
            appendText(labelNode.getParent(), NS_XFORMS, NODE_HINT, hint);
        }
    }

    private String getLabel(Concept concept) {
        return customizer.getLabel(concept);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Code which was in XformBuilder, but is UI-based

    /**
     * Builds a UI control node for a table field.
     * @return - the created UI control node.
     */
    private Element addDatabaseElementUiNode(String bindName, FormField formField, Element
        parentUiNode) {
        Element controlNode = append(parentUiNode, NS_XFORMS, CONTROL_INPUT);
        controlNode.setAttribute(null, ATTRIBUTE_BIND, bindName);

        // TODO: Set the data type on the bind node? It may already be done.

        // Handle encounter provider / location: these are multiple choice questions,
        // and we populate the options.
        Field field = formField.getField();
        if (eq(field.getTableName(), "patient")) {
            if (eq(field.getAttributeName(), "gender")) {
                controlNode.setName(CONTROL_SELECT1);
                populateGenders(controlNode);
            } else if (eq(field.getAttributeName(), "birthdate")) {
                controlNode.setAttribute(null, ATTRIBUTE_APPEARANCE,
                    "minimal|show_years|show_months");
            }
        } else if (eq(field.getTableName(), "encounter")) {
            if (eq(field.getAttributeName(), "location_id")) {
                controlNode.setName(CONTROL_SELECT1);
                populateLocations(controlNode);
            } else if (eq(field.getAttributeName(), "provider_id")) {
                controlNode.setName(CONTROL_SELECT1);
                populateProviders(controlNode);
            }
        }

        //create the label
        appendText(controlNode, NS_XFORMS, NODE_LABEL, getDisplayName(formField));
        return controlNode;
    }

    private void populateGenders(Element controlNode) {
        ConceptService conceptService = Context.getConceptService();
        appendOption(controlNode, getLabel(conceptService.getConcept(MALE_CONCEPT_ID)), "M");
        appendOption(controlNode, getLabel(conceptService.getConcept(FEMALE_CONCEPT_ID)), "F");
    }

    /** Populates a selection node with provider options. The labels are UUIDs, understood by the client. */
    private void populateProviders(Element controlNode) {
        includesProviders = true;
        for (Provider provider : Context.getProviderService().getAllProviders()) {
            appendOption(controlNode, customizer.getLabel(provider), "" + provider.getId());
        }
    }

    /** Populates a selection node with location options. The labels are UUIDs, understood by the client. */
    private void populateLocations(Element controlNode) {
        includesLocations = true;
        List<Location> locations = customizer.getEncounterLocations();
        for (Location location : locations) {
            appendOption(controlNode, customizer.getLabel(location), "" + location.getId());
        }
    }
}
