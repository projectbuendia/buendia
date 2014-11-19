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
import static org.openmrs.module.xforms.XformBuilder.NODE_ITEM;
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
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.module.xforms.RelativeBuilder;
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
public class BuendiaXformBuilderEx {

    private final Map<String, Element> bindings = new HashMap<>();
    private final Map<FormField, String> fieldTokens = new HashMap<>();
    private final boolean useConceptIdAsHint;

    private BuendiaXformBuilderEx() {
        useConceptIdAsHint = "true".equalsIgnoreCase(Context.getAdministrationService().getGlobalProperty("xforms.useConceptIdAsHint"));
    }
    
    /**
     * Builds an xform for an given an openmrs form. This is the only
     * public member in the class; it constructs an instance (to avoid
     * nasty statics) and then invokes private methods appropriately.
     */
    public static String buildXform(Form form) throws Exception {
        return new BuendiaXformBuilderEx().buildXformImpl(form); 
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
        Element bodyNode = xformsNode;
        
        Element instanceNode = appendElement(modelNode, NAMESPACE_XFORMS, NODE_INSTANCE);
        instanceNode.setAttribute(null, ATTRIBUTE_ID, INSTANCE_ID);
        
        Element formNode = (Element) BuendiaXformBuilder.getDocument(new StringReader(templateXml)).getRootElement();
        formNode.setAttribute(null, ATTRIBUTE_UUID, form.getUuid());
        instanceNode.addChild(Element.ELEMENT, formNode);
        
        // (Note for comparison with XformBuilderEx: schema doc code removed here, as it wasn't actually used.) 
        
        //TODO This block should be replaced with using database field items instead of
        //     parsing the template document.
        Hashtable<String, String> problemList = new Hashtable<String, String>();
        Hashtable<String, String> problemListItems = new Hashtable<String, String>();
        BuendiaXformBuilder.parseTemplate(modelNode, formNode, formNode, bindings, problemList, problemListItems, 0);
        
        buildUInodes(form, bodyNode);
        
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
                                BuendiaXformBuilder.addConceptMapAttributes(grandChildElement, value);
                        }
                    }
                }
            }
        }
        
        if (includeRelationshipNodes) {
            RelativeBuilder.build(modelNode, bodyNode, formNode);
        }
        return BuendiaXformBuilder.fromDoc2String(doc);
    }     
    
    private void buildUInodes(Form form, Element bodyNode) {   
        Locale locale = Context.getLocale();
        TreeMap<Integer, TreeSet<FormField>> formStructure = FormUtil.getFormStructure(form);
        buildUInodes(form, formStructure, 0, locale, bodyNode);
    }
    
    private void buildUInodes(Form form, TreeMap<Integer, TreeSet<FormField>> formStructure,
            Integer sectionId, Locale locale, Element parentUiNode) {         
        TreeSet<FormField> section = formStructure.get(sectionId);
        if (section == null) {
            return;
        }
        
        // Note: FormUtil.getTagList needs a Vector<String>. Urgh.
        Vector<String> tagList = new Vector<>();
        
        for(FormField formField : section) {
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
            
            Element fieldUiNode;
            
            int fieldTypeId = field.getFieldType().getFieldTypeId();
            
            if (fieldTypeId == FormConstants.FIELD_TYPE_CONCEPT) {
                Concept concept = field.getConcept();
                ConceptDatatype datatype = concept.getDatatype();
                
                // TODO(jonskeet): Don't rely on names here? (Do we even need problem lists?)
                if ( (name.contains("problem_added") || name.contains("problem_resolved")) &&
                        formField.getParent() != null &&
                        (formField.getParent().getField().getName().contains("PROBLEM LIST")) ){
                    
                    fieldUiNode = addProblemList(name, concept, required, locale, formField, parentUiNode);
                } else if (name.equals("problem_list")) {
                    // TODO(jonskeet): Work out what we should do here. There won't be any bindings for this.
                    // The child nodes will be covered by the case above, when we recurse down.
                    fieldUiNode = parentUiNode;
                }
                else {
                    switch (datatype.getHl7Abbreviation()) {
                        case HL7Constants.HL7_BOOLEAN:
                            fieldUiNode = addUiNode(name, concept, DATA_TYPE_BOOLEAN, CONTROL_INPUT, locale, parentUiNode);
                            break;
                        case HL7Constants.HL7_DATE:
                            fieldUiNode = addUiNode(name, concept, DATA_TYPE_DATE, CONTROL_INPUT, locale, parentUiNode);
                            break;
                        case HL7Constants.HL7_DATETIME:
                            fieldUiNode = addUiNode(name, concept, DATA_TYPE_DATETIME, CONTROL_INPUT, locale, parentUiNode);
                            break;
                        case HL7Constants.HL7_TIME:
                            fieldUiNode = addUiNode(name, concept, DATA_TYPE_TIME, CONTROL_INPUT, locale, parentUiNode);
                            break;
                        case HL7Constants.HL7_TEXT:
                            fieldUiNode = addUiNode(name, concept, DATA_TYPE_TEXT, CONTROL_INPUT, locale, parentUiNode);
                            break;
                        case HL7Constants.HL7_NUMERIC:
                            ConceptNumeric conceptNumeric = Context.getConceptService().getConceptNumeric(concept.getConceptId());
                            fieldUiNode = addUiNode(name, conceptNumeric, DATA_TYPE_DECIMAL, CONTROL_INPUT, locale, parentUiNode);
                            break;
                        case HL7Constants.HL7_CODED:
                        case HL7Constants.HL7_CODED_WITH_EXCEPTIONS:
                            fieldUiNode = addCodedField(name, formField, field, required, concept, locale, parentUiNode);
                            break;
                        case "ED": // This isn't in HL7Constants as far as I can tell.
                            fieldUiNode = addUiNode(name, concept, DATA_TYPE_BASE64BINARY, CONTROL_INPUT, locale, parentUiNode);
                            break;
                        default:
                            // Concept effectively being used as a section.
                            fieldUiNode = createGroupNode(formField, locale, parentUiNode);
                            break;
                    }
                }
            }            
            else if (fieldTypeId == FormConstants.FIELD_TYPE_SECTION) {
                fieldUiNode = appendElement(parentUiNode, NAMESPACE_XFORMS, NODE_GROUP);
                Element label = appendElement(fieldUiNode, NAMESPACE_XFORMS, NODE_LABEL);
                label.addChild(Node.TEXT, getDisplayName(formField));
            } else if (fieldTypeId == FormConstants.FIELD_TYPE_DATABASE) {
                fieldUiNode = addDatabaseElementUiNode(name, formField, parentUiNode);
            }
            else {
                // TODO(jonskeet): What do we do with this?
                fieldUiNode = appendElement(parentUiNode, NAMESPACE_XFORMS, NODE_GROUP);
                Element label = appendElement(fieldUiNode, NAMESPACE_XFORMS, NODE_LABEL);
                label.addChild(Node.TEXT, getDisplayName(formField));
            }
            
            // Recurse down to subnodes.
            buildUInodes(form, formStructure, formField.getFormFieldId(), locale, fieldUiNode);
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
            
            Element itemNode = appendElement(controlNode, NAMESPACE_XFORMS, NODE_ITEM);
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
    
    private Element addProblemList(String token, Concept concept, boolean required,
        Locale locale, FormField formField, Node parentUiNode) {
        
        Element groupNode = appendElement(parentUiNode, NAMESPACE_XFORMS, NODE_GROUP);
        
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
        return groupNode;
    }
    
    private Element createGroupNode(FormField formField, Locale locale, Element parentUiNode) {
        String token = fieldTokens.get(formField);
        
        Element groupNode = appendElement(parentUiNode, NAMESPACE_XFORMS, NODE_GROUP);            
        Element labelNode = appendElement(groupNode, NAMESPACE_XFORMS, NODE_LABEL);
        labelNode.addChild(Element.TEXT, formField.getField().getConcept().getName(locale, false).getName());
        
        addHintNode(labelNode, formField.getField().getConcept());
        
        if (formField.getMaxOccurs() != null && formField.getMaxOccurs() == -1) {
            Element repeatControl = appendElement(groupNode, NAMESPACE_XFORMS, CONTROL_REPEAT);
            repeatControl.setAttribute(null, ATTRIBUTE_BIND, token);
            return repeatControl;
        }
        else {
            groupNode.setAttribute(null, ATTRIBUTE_ID, token);
            return groupNode;
        }
    }
    
    private String getDisplayName(FormField formField) {
        String name = formField.getDescription(); 
        if (name != null) {
            return name;
        }
        name = formField.getName();
        if (name != null) {
            return name;
        }
        name = formField.getField().getDescription();
        if (name != null) {
            return name;
        }
        name = formField.getField().getName();
        if (name != null) {
            return name;
        }
        throw new IllegalArgumentException("No field name available");
    }
    
    private Element addCodedField(String name, FormField formField, Field field,
            boolean required, Concept concept, Locale locale, Element parentUiNode) {
        if (formField.getMaxOccurs() != null && formField.getMaxOccurs().intValue() == -1) {
            return addProblemList(name, concept, required, locale, formField, parentUiNode);
        }
        else {
            //Collection<ConceptAnswer> answers = concept.getAnswers(false);
            List<ConceptAnswer> answers = new ArrayList<ConceptAnswer>(concept.getAnswers(false));
            Collections.sort(answers);
            
            String controlName = field.getSelectMultiple() ? CONTROL_SELECT : CONTROL_SELECT1;
            Element controlNode = addUiNode(name, concept, DATA_TYPE_TEXT, controlName, locale, parentUiNode);
            addCodedUiNodes(true, controlNode, answers, concept, DATA_TYPE_TEXT, CONTROL_SELECT, locale);
            return controlNode;
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
    
    ///////////////////////////////////////////////////////////////////////////
    // Code which was in XformBuilder, but is UI-based
    
    /**
     * Builds a UI control node for a table field.
     * 
     * @param node - the node whose UI control to build.
     * @param bodyNode - the body node to add the UI control to.
     * @return - the created UI control node.
     */
    private Element addDatabaseElementUiNode(String bindName, FormField formField, Element parentUiNode) {
        Element controlNode = appendElement(parentUiNode, NAMESPACE_XFORMS, CONTROL_INPUT);        
        controlNode.setAttribute(null, ATTRIBUTE_BIND, bindName);
        
        // TODO: Set the data type on the bind node? It may already be done.
        
        // Handle encounter provider / location: these are multiple choice questions, and we populate
        // the options.
        Field field = formField.getField();
        if ("encounter".equals(field.getTableName())) {
            if ("location_id".equals(field.getAttributeName())) {
                controlNode.setName(CONTROL_SELECT1);
                populateLocations(controlNode);
            } else if ("provider_id".equals(field.getAttributeName())) {
                controlNode.setName(CONTROL_SELECT1);
                populateProviders(controlNode);
            }
        }
        
        //create the label
        Element labelNode = appendElement(controlNode, NAMESPACE_XFORMS, NODE_LABEL);
        labelNode.addChild(Element.TEXT, getDisplayName(formField));        
        return controlNode;
    }
    
    /**
     * Populates a UI control node with providers.
     * 
     * @param controlNode - the UI control node.
     */
    private static void populateProviders(Element controlNode) {
        for (Provider provider : Context.getProviderService().getAllProviders()) {
            String name = provider.getName();
            if (name == null) {
                Person person = provider.getPerson();
                name = person.getPersonName().toString();
            }
            
            String identifier = provider.getIdentifier();
            Integer providerId = provider.getId();
            
            Element itemNode = appendElement(controlNode, NAMESPACE_XFORMS, NODE_ITEM);
            
            Element node = appendElement(itemNode, NAMESPACE_XFORMS, NODE_LABEL);
            node.addChild(Element.TEXT, name + " [" + identifier + "]");
            
            node = appendElement(itemNode, NAMESPACE_XFORMS, NODE_VALUE);
            node.addChild(Element.TEXT, providerId.toString());
        }
    }
    
    /**
     * Populates a UI control node with locations.
     * 
     * @param controlNode - the UI control node.
     */
    private static void populateLocations(Element controlNode) {
        List<Location> locations = Context.getLocationService().getAllLocations(false);
        for (Location loc : locations) {
            Element itemNode = appendElement(controlNode, NAMESPACE_XFORMS, NODE_ITEM);
            
            Element node = appendElement(itemNode, NAMESPACE_XFORMS, NODE_LABEL);
            node.addChild(Element.TEXT, loc.getName() + " [" + loc.getLocationId() + "]");
            
            node = appendElement(itemNode, NAMESPACE_XFORMS, NODE_VALUE);
            node.addChild(Element.TEXT, loc.getLocationId().toString());
        }
    }
}
