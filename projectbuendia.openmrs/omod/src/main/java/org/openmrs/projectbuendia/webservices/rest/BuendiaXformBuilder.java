package org.openmrs.projectbuendia.webservices.rest;

import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_BIND;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_CONCEPT_ID;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_CONSTRAINT;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_ID;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_MESSAGE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_NODESET;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_OPENMRS_CONCEPT;
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
import static org.openmrs.module.xforms.XformBuilder.NAMESPACE_XFORMS;
import static org.openmrs.module.xforms.XformBuilder.NAMESPACE_XML_INSTANCE;
import static org.openmrs.module.xforms.XformBuilder.NAMESPACE_XML_SCHEMA;
import static org.openmrs.module.xforms.XformBuilder.NODE_BIND;
import static org.openmrs.module.xforms.XformBuilder.NODE_GROUP;
import static org.openmrs.module.xforms.XformBuilder.NODE_HINT;
import static org.openmrs.module.xforms.XformBuilder.NODE_INSTANCE;
import static org.openmrs.module.xforms.XformBuilder.NODE_LABEL;
import static org.openmrs.module.xforms.XformBuilder.NODE_MODEL;
import static org.openmrs.module.xforms.XformBuilder.NODE_VALUE;
import static org.openmrs.module.xforms.XformBuilder.NODE_XFORMS;
import static org.openmrs.module.xforms.XformBuilder.PREFIX_XFORMS;
import static org.openmrs.module.xforms.XformBuilder.PREFIX_XML_INSTANCES;
import static org.openmrs.module.xforms.XformBuilder.PREFIX_XML_SCHEMA;
import static org.openmrs.module.xforms.XformBuilder.PREFIX_XML_SCHEMA2;

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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.module.xforms.RelativeBuilder;
import org.openmrs.module.xforms.XformBuilder;
import org.openmrs.module.xforms.XformConstants;
import org.openmrs.module.xforms.formentry.FormEntryWrapper;
import org.openmrs.module.xforms.formentry.FormSchemaFragment;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.util.FormConstants;
import org.openmrs.util.FormUtil;

/**
 * This is a clone of the Xforms module XformBuilderEx class, allowing us to tinker with the view
 * creation code separately from the module itself.
 */
public class BuendiaXformBuilder {

    // Note: not a Map because we need to call into Xforms module code which requires Hashtable :( 
    private final Hashtable<String, Element> bindings = new Hashtable<>();
    private final Map<FormField, Element> formFields = new HashMap<>();
    private final Map<FormField, String> fieldTokens = new HashMap<>();
    private final boolean useConceptIdAsHint;
    private Element bodyNode;

    private BuendiaXformBuilder() {
        useConceptIdAsHint = "true".equalsIgnoreCase(Context.getAdministrationService().getGlobalProperty("xforms.useConceptIdAsHint"));
    }
    
    /**
     * Builds an xform for an given an openmrs form. This is the only
     * public member in the class; it constructs an instance (to avoid
     * nasty statics) and then invokes private methods appropriately.
     */
    public static String buildXform(Form form) throws Exception {
        return new BuendiaXformBuilder().buildXformImpl(form); 
    }
    
    private String buildXformImpl(Form form) throws Exception {
        boolean includeRelationshipNodes = !"false".equals(Context.getAdministrationService()
            .getGlobalProperty(XformConstants.GLOBAL_PROP_KEY_INCLUDE_PATIENT_RELATIONSHIPS));
        
        //String schemaXml = XformsUtil.getSchema(form);
        String templateXml = FormEntryWrapper.getFormTemplate(form);
        
        //Add relationship data node
        if (includeRelationshipNodes) {
            templateXml = templateXml.replace("</patient>", "  <patient_relative>\n      <patient_relative.person/>\n      <patient_relative.relationship/>\n    </patient_relative>\n  </patient>");
        }
                
        Document doc = new Document();
        doc.setEncoding(XformConstants.DEFAULT_CHARACTER_ENCODING);
        
        Element xformsNode = appendElement(doc, NAMESPACE_XFORMS, NODE_XFORMS);
        xformsNode.setPrefix(PREFIX_XFORMS, NAMESPACE_XFORMS);
        xformsNode.setPrefix(PREFIX_XML_SCHEMA, NAMESPACE_XML_SCHEMA);
        xformsNode.setPrefix(PREFIX_XML_SCHEMA2, NAMESPACE_XML_SCHEMA);
        xformsNode.setPrefix(PREFIX_XML_INSTANCES, NAMESPACE_XML_INSTANCE);
        xformsNode.setPrefix("jr", "http://openrosa.org/javarosa");
        
        Element modelNode = appendElement(xformsNode, NAMESPACE_XFORMS, NODE_MODEL);
        modelNode.setAttribute(null, ATTRIBUTE_ID, MODEL_ID);

        // All our UI nodes are appended directly into the xforms node.
        // Another alternative would be to create the HTML body node here, and append
        // everything under that.
        bodyNode = xformsNode;
        
        Element instanceNode = appendElement(modelNode, NAMESPACE_XFORMS, NODE_INSTANCE);
        instanceNode.setAttribute(null, ATTRIBUTE_ID, INSTANCE_ID);
        
        Element formNode = (Element) XformBuilder.getDocument(new StringReader(templateXml)).getRootElement();
        formNode.setAttribute(null, ATTRIBUTE_UUID, form.getUuid());
        instanceNode.addChild(Element.ELEMENT, formNode);
        
        // (Note for comparison with XformBuilderEx: schema doc code removed here, as it wasn't actually used.) 
        
        //TODO This block should be replaced with using database field items instead of
        //     parsing the template document.
        Hashtable<String, String> problemList = new Hashtable<String, String>();
        Hashtable<String, String> problemListItems = new Hashtable<String, String>();
        XformBuilder.parseTemplate(modelNode, formNode, formNode, bindings, bodyNode, problemList, problemListItems, 0);
        
        buildUInodes(form);
        
        //find all conceptId attributes in the document and replace their value with a mapped concept
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
                            String value = grandChildElement.getAttributeValue(null, ATTRIBUTE_OPENMRS_CONCEPT);
                            if (StringUtils.isNotBlank(value))
                                XformBuilder.addConceptMapAttributes(grandChildElement, value);
                        }
                    }
                }
            }
        }
        
        if (includeRelationshipNodes) {
            RelativeBuilder.build(modelNode, bodyNode, formNode);
        }
        return XformBuilder.fromDoc2String(doc);
    }     
    
    private void buildUInodes(Form form) {   
        Locale locale = Context.getLocale();
        TreeMap<Integer, TreeSet<FormField>> formStructure = FormUtil.getFormStructure(form);
        buildUInodes(form, formStructure, 0, locale);
    }
    
    private void buildUInodes(Form form, TreeMap<Integer, TreeSet<FormField>> formStructure, Integer sectionId, Locale locale) {         
        if (!formStructure.containsKey(sectionId))
            return;
        
        TreeSet<FormField> section = formStructure.get(sectionId);
        if (section == null || section.size() < 1)
            return;
        
        // Note: FormUtil.getTagList needs a Vector<String>. Urgh.
        Vector<String> tagList = new Vector<>();
        
        for(FormField formField : section) { 
            Integer subSectionId = formField.getFormFieldId();
            String sectionName = FormUtil.getXmlToken(formField.getField().getName());
            String name = FormUtil.getNewTag(sectionName, tagList);

            if(formField.getParent() != null && fieldTokens.values().contains(name)){
                String parentName = fieldTokens.get(formField.getParent());
                String token = parentName + "_" + name;
                
                if(!bindings.containsKey(token)) {
                    token = FormUtil.getNewTag(FormUtil.getXmlToken(formField.getParent().getField().getName()),  new Vector<String>());
                    token = token + "_" + name;
                }
                
                name = token;
            }
            
            fieldTokens.put(formField, name);

            Field field = formField.getField();
            boolean required = formField.isRequired();
            
            if (field.getFieldType().getFieldTypeId().equals(FormConstants.FIELD_TYPE_CONCEPT)) {
                Concept concept = field.getConcept();
                ConceptDatatype datatype = concept.getDatatype();
                
                // TODO(jonskeet): Don't rely on names here? (Do we even need problem lists?)
                if ( (name.contains("problem_added") || name.contains("problem_resolved")) &&
                        formField.getParent() != null &&
                        (formField.getParent().getField().getName().contains("PROBLEM LIST")) ){
                    
                    addProblemList(name, concept, required, locale, formField);
                }
                else {
                    switch (datatype.getHl7Abbreviation()) {
                        case HL7Constants.HL7_BOOLEAN:
                            addUiNode(name, concept, DATA_TYPE_BOOLEAN, CONTROL_INPUT, locale, getParentNode(formField, locale));
                            break;
                        case HL7Constants.HL7_DATE:
                            addUiNode(name, concept, DATA_TYPE_DATE, CONTROL_INPUT, locale, getParentNode(formField, locale));
                            break;
                        case HL7Constants.HL7_DATETIME:
                            addUiNode(name, concept, DATA_TYPE_DATETIME, CONTROL_INPUT, locale, getParentNode(formField, locale));
                            break;
                        case HL7Constants.HL7_TIME:
                            addUiNode(name, concept, DATA_TYPE_TIME, CONTROL_INPUT, locale, getParentNode(formField, locale));
                            break;
                        case HL7Constants.HL7_TEXT:
                            addUiNode(name, concept, DATA_TYPE_TEXT, CONTROL_INPUT, locale, getParentNode(formField, locale));
                            break;
                        case HL7Constants.HL7_NUMERIC:
                            ConceptNumeric conceptNumeric = Context.getConceptService().getConceptNumeric(concept.getConceptId());
                            addUiNode(name, conceptNumeric, DATA_TYPE_DECIMAL, CONTROL_INPUT, locale, getParentNode(formField, locale));
                            break;
                        case HL7Constants.HL7_CODED:
                        case HL7Constants.HL7_CODED_WITH_EXCEPTIONS:
                            addCodedField(name, formField, field, required, concept, locale);
                            break;
                        case "ED": // This isn't in HL7Constants as far as I can tell.
                            addUiNode(name, concept, DATA_TYPE_BASE64BINARY, CONTROL_INPUT, locale, getParentNode(formField, locale));
                            break;
                    }
                }
            }
            
            if (formStructure.containsKey(subSectionId)) {
                buildUInodes(form, formStructure, subSectionId, locale);
            }
        }
    }

    private Element addUiNode(String token, Concept concept, String dataType, String controlName, Locale locale, Element bodyNode) {
        String bindName = token;
        
        Element controlNode = appendElement(bodyNode, NAMESPACE_XFORMS, controlName);
        controlNode.setAttribute(null, ATTRIBUTE_BIND, bindName);
        
        Element bindNode = bindings.get(bindName);
        if (bindNode == null) {
            throw new IllegalArgumentException("No bind node for bindName " + bindName);
        }
        
        bindNode.setAttribute(null, ATTRIBUTE_TYPE, dataType);
        
        //create the label
        Element labelNode = appendElement(controlNode, NAMESPACE_XFORMS, NODE_LABEL);
        ConceptName name = concept.getName(locale, false);
        if (name == null) {
            name = concept.getName();
        }        
        labelNode.addChild(Element.TEXT, name.getName());
        
        addHintNode(labelNode, concept);
        
        if(concept instanceof ConceptNumeric) {
            ConceptNumeric numericConcept = (ConceptNumeric)concept;
            if(numericConcept.isPrecise()){
                Double minInclusive = numericConcept.getLowAbsolute();
                Double maxInclusive = numericConcept.getHiAbsolute();
                
                if(!(minInclusive == null && maxInclusive == null)){
                    String lower = (minInclusive == null ? "" : FormSchemaFragment.numericToString(minInclusive, numericConcept.isPrecise()));
                    String upper = (maxInclusive == null ? "" : FormSchemaFragment.numericToString(maxInclusive, numericConcept.isPrecise()));
                    bindNode.setAttribute(null, ATTRIBUTE_CONSTRAINT, ". >= " + lower + " and . <= " + upper);
                    bindNode.setAttribute(null, (XformsUtil.isJavaRosaSaveFormat() ? "jr:constraintMsg" : ATTRIBUTE_MESSAGE),
                        "value should be between " + lower + " and " + upper + " inclusive");
                }
            }
        }
        
        return controlNode;
    }
    
    private void addCodedUiNodes(boolean multiplSel, Element controlNode, Collection<ConceptAnswer> answerList,
            Concept concept, String dataType, String controlName, Locale locale){
        for (ConceptAnswer answer : answerList) {
            String conceptName = answer.getAnswerConcept().getName(locale).getName();
            String conceptValue;
            
            if (answer.getAnswerConcept().getConceptClass().getConceptClassId().equals(HL7Constants.CLASS_DRUG)
                    && answer.getAnswerDrug() != null) {
                
                conceptName = answer.getAnswerDrug().getName();
                
                if(multiplSel)
                    conceptValue = FormUtil.getXmlToken(conceptName);
                else {
                    conceptValue = StringEscapeUtils.escapeXml(FormUtil.conceptToString(answer.getAnswerConcept(),
                                    locale)) + "^" + FormUtil.drugToString(answer.getAnswerDrug());
                }
            } else {
                if(multiplSel)
                    conceptValue = FormUtil.getXmlToken(conceptName);
                else
                    conceptValue = StringEscapeUtils.escapeXml(FormUtil.conceptToString(answer.getAnswerConcept(), locale));
            }
            
            Element itemNode = appendElement(controlNode, NAMESPACE_XFORMS, XformBuilder.NODE_ITEM);
            itemNode.setAttribute(null, ATTRIBUTE_CONCEPT_ID, concept.getConceptId().toString());
            
            Element itemLabelNode = appendElement(itemNode, NAMESPACE_XFORMS, NODE_LABEL);
            itemLabelNode.addChild(Element.TEXT, conceptName);
            
            //TODO This will make sense after the form designer's OptionDef implements
            //the xforms hint.
            //addHintNode(itemLabelNode, answer.getAnswerConcept());
            
            Element itemValNode = appendElement(itemNode, NAMESPACE_XFORMS, NODE_VALUE);
            itemValNode.addChild(Element.TEXT, conceptValue);
        }
    }
    
    private void addProblemList(String token, Concept concept, boolean required,
        Locale locale, FormField formField) {
        
        Element groupNode = appendElement(bodyNode, NAMESPACE_XFORMS, NODE_GROUP);
        
        Element labelNode = appendElement(groupNode, NAMESPACE_XFORMS, NODE_LABEL);
        labelNode.addChild(Element.TEXT, formField.getField().getConcept().getName(locale, false).getName());
        
        addHintNode(labelNode, concept);
        
        Element repeatControl = appendElement(groupNode, NAMESPACE_XFORMS, CONTROL_REPEAT);
        repeatControl.setAttribute(null, ATTRIBUTE_BIND, token);

        //add the input node.
        Element controlNode = appendElement(repeatControl, NAMESPACE_XFORMS, CONTROL_INPUT);        
        String nodeset = "problem_list/" + token + "/value";
        String id = nodeset.replace('/', '_');
        controlNode.setAttribute(null, ATTRIBUTE_BIND, id);
        
        //add the label.
        labelNode = appendElement(controlNode, NAMESPACE_XFORMS, NODE_LABEL);
        labelNode.addChild(Element.TEXT, token + " value");
        
        addHintNode(labelNode, concept);
        
        //create bind node
        Element bindNode = appendElement(bindings.get(token).getParent(), NAMESPACE_XFORMS, NODE_BIND);
        bindNode.setAttribute(null, ATTRIBUTE_ID, id);
        bindNode.setAttribute(null, ATTRIBUTE_NODESET, "/form/" + nodeset);
        bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
    }
    
    /**
     * Returns the element representing the UI control for the parent of the given
     * form field - i.e. the element under which the UI control for the form field
     * itself should be added.
     */
    private Element getParentNode(FormField formField, Locale locale){
        formField = formField.getParent();
        if (formField == null || formField.getParent() == null) {
            return bodyNode; // is this problem list?
        }
        Element node = formFields.get(formField);
        if (node != null) {
            return node;
        }
        
        String token = fieldTokens.get(formField);
        
        Element groupNode = appendElement(bodyNode, NAMESPACE_XFORMS, NODE_GROUP);            
        Element labelNode = appendElement(groupNode, NAMESPACE_XFORMS, NODE_LABEL);
        labelNode.addChild(Element.TEXT, formField.getField().getConcept().getName(locale, false).getName());
        
        addHintNode(labelNode, formField.getField().getConcept());
        
        if (formField.getMaxOccurs() != null && formField.getMaxOccurs() == -1) {
            Element repeatControl = appendElement(groupNode, NAMESPACE_XFORMS, CONTROL_REPEAT);
            repeatControl.setAttribute(null, ATTRIBUTE_BIND, token);
            
            formFields.put(formField, repeatControl);
            return repeatControl;
        }
        else {
            groupNode.setAttribute(null, ATTRIBUTE_ID, token);
            formFields.put(formField, groupNode);
            return groupNode;
        }
    }
    
    private void addCodedField(String name, FormField formField, Field field,
            boolean required, Concept concept, Locale locale) {
        if (formField.getMaxOccurs() != null && formField.getMaxOccurs().intValue() == -1) {
            addProblemList(name, concept, required, locale, formField);
        }
        else {
            //Collection<ConceptAnswer> answers = concept.getAnswers(false);
            List<ConceptAnswer> answers = new ArrayList<ConceptAnswer>(concept.getAnswers(false));
            Collections.sort(answers);
            
            String controlName = field.getSelectMultiple() ? CONTROL_SELECT : CONTROL_SELECT1;
            Element controlNode = addUiNode(name, concept, DATA_TYPE_TEXT, controlName, locale, getParentNode(formField, locale));
            if (controlNode != null) {
                addCodedUiNodes(true, controlNode, answers, concept, DATA_TYPE_TEXT, CONTROL_SELECT, locale);
            }
        }
    }
    
    private void addHintNode(Element labelNode, Concept concept) {
        String hint = null;
        if(concept.getDescription() != null) {
            hint = concept.getDescription().getDescription();
        }
        
        if(useConceptIdAsHint) {
            hint = (hint != null ? hint + " [" + concept.getConceptId() + "]" : concept.getConceptId().toString());
        }
        
        if(hint != null) {
            Element hintNode = appendElement(labelNode.getParent(), NAMESPACE_XFORMS, NODE_HINT);
            hintNode.addChild(Element.TEXT, hint);
        }
    }
    
    private static Element appendElement(Node parent, String namespaceURI, String localName) {
        Element child = parent.createElement(namespaceURI, localName);
        parent.addChild(Element.ELEMENT, child);
        return child;
    }
}
