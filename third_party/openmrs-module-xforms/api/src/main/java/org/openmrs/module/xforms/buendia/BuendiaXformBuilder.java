/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.xforms.buendia;

import org.apache.commons.lang.StringUtils;
import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.xforms.XformConstants;
import org.openmrs.module.xforms.XformsService;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.util.OpenmrsUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_CONSTRAINT;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_ID;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_LOCKED;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_MESSAGE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_MULTIPLE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_NODESET;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_OPENMRS_ATTRIBUTE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_OPENMRS_CONCEPT;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_OPENMRS_DATATYPE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_OPENMRS_TABLE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_REQUIRED;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_TYPE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_VISIBLE;
import static org.openmrs.module.xforms.XformBuilder.ATTRIBUTE_XSI_NILL;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_BOOLEAN;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_DATE;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_DATETIME;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_INT;
import static org.openmrs.module.xforms.XformBuilder.DATA_TYPE_TEXT;
import static org.openmrs.module.xforms.XformBuilder.NAMESPACE_XFORMS;
import static org.openmrs.module.xforms.XformBuilder.NODE_BIND;
import static org.openmrs.module.xforms.XformBuilder.NODE_ENCOUNTER_ENCOUNTER_DATETIME;
import static org.openmrs.module.xforms.XformBuilder.NODE_ENCOUNTER_LOCATION_ID;
import static org.openmrs.module.xforms.XformBuilder.NODE_ENCOUNTER_PROVIDER_ID;
import static org.openmrs.module.xforms.XformBuilder.NODE_INSTANCE;
import static org.openmrs.module.xforms.XformBuilder.NODE_PATIENT_BIRTH_DATE;
import static org.openmrs.module.xforms.XformBuilder.NODE_PATIENT_BIRTH_DATE_ESTIMATED;
import static org.openmrs.module.xforms.XformBuilder.NODE_PATIENT_FAMILY_NAME;
import static org.openmrs.module.xforms.XformBuilder.NODE_PATIENT_GENDER;
import static org.openmrs.module.xforms.XformBuilder.NODE_PATIENT_GIVEN_NAME;
import static org.openmrs.module.xforms.XformBuilder.NODE_PATIENT_MIDDLE_NAME;
import static org.openmrs.module.xforms.XformBuilder.NODE_PATIENT_PATIENT_ID;
import static org.openmrs.module.xforms.XformBuilder.NODE_PROBLEM_LIST;
import static org.openmrs.module.xforms.XformBuilder.NODE_SEPARATOR;
import static org.openmrs.module.xforms.XformBuilder.NODE_VALUE;
import static org.openmrs.module.xforms.XformBuilder.NODE_XFORMS_VALUE;
import static org.openmrs.module.xforms.XformBuilder.VALUE_TRUE;
import static org.openmrs.module.xforms.XformBuilder.XPATH_VALUE_FALSE;
import static org.openmrs.module.xforms.XformBuilder.XPATH_VALUE_TRUE;

//TODO This class is too big. May need breaking into smaller ones.

/**
 * This is a clone of the Xforms module XformBuilder class, allowing us to tinker with the view
 * creation code separately from the module itself. Methods we don't need have been removed,
 * and the constants are imported from XformBuilder.
 */
public final class BuendiaXformBuilder {
    private static final String ATTRIBUTE_PRELOAD = "jr:preload";
    
    private static final String ATTRIBUTE_PRELOAD_PARAMS = "jr:preloadParams";
    
    private static final String PRELOAD_PATIENT = "patient";
    
    /**
     * Sets the value of a child node in a parent node.
     * 
     * @param parentNode - the node to add a child to.
     * @param name - the name of the node whose value to set.
     * @param value - the value to set.
     * @return - true if the node with the name was found, else false.
     */
    private static boolean setNodeValue(Element parentNode, String name, String value) {
        Element node = getElement(parentNode, name);
        if (node == null) {
            return false;
        }
        setNodeValue(node, value);
        return true;
    }
    
    /**
     * Sets the text value of a node.
     * 
     * @param node - the node whose value to set.
     * @param value - the value to set.
     */
    private static void setNodeValue(Element node, String value) {
        if (value == null)
            value = "";
        
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.isText(i)) {
                node.removeChild(i);
                node.addChild(Element.TEXT, value);
                return;
            }
        }
        
        node.addChild(Element.TEXT, value);
    }
    
    /**
     * Gets a child element of a parent node with a given name.
     * 
     * @param parent - the parent element
     * @param name - the name of the child.
     * @return - the child element.
     */
    private static Element getElement(Element parent, String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getType(i) != Element.ELEMENT)
                continue;
            
            Element child = (Element) parent.getChild(i);
            if (child.getName().equalsIgnoreCase(name))
                return child;
            
            child = getElement(child, name);
            if (child != null)
                return child;
        }
        
        return null;
    }
    
    /**
     * Methods replaces the conceptId with a concept source name and source code.
     * 
     * @param element the element with the openmrs_concept attribute
     * @param conceptValueString the value of the openmrs_concept attribute
     */
    public static void addConceptMapAttributes(Element element, String conceptValueString) {
        String[] tokens = StringUtils.split(conceptValueString, "^");
        ConceptService cs = Context.getConceptService();
        try {
            Concept concept = cs.getConcept(Integer.valueOf(tokens[0].trim()));
            String prefSourceName = Context.getAdministrationService().getGlobalProperty(
                XformConstants.GLOBAL_PROP_KEY_PREFERRED_CONCEPT_SOURCE);
            if (StringUtils.isNotBlank(prefSourceName)) {
                ConceptSource preferredSource = cs.getConceptSourceByName(prefSourceName);
                if (concept.getConceptMappings().size() > 0) {
                    if (preferredSource != null) {
                        for (ConceptMap map : concept.getConceptMappings()) {
                            ConceptReferenceTerm term = map.getConceptReferenceTerm();
                            if (OpenmrsUtil.nullSafeEquals(preferredSource, term.getConceptSource())) {
                                element.setAttribute(null, ATTRIBUTE_OPENMRS_CONCEPT,
                                    term.getConceptSource().getName() + ":" + term.getCode());
                                return;
                            }
                        }
                    }
                }
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid concept value: " + conceptValueString, e);
        }
    }    
    
    /**
     * Parses an openmrs template and builds the bindings in the model plus UI controls for openmrs
     * table field questions.
     * 
     * @param modelElement - the model element to add bindings to.
     * @param formNode the form node.
     * @param bindings - a hash table to populate with the built bindings.
     */
    public static void parseTemplate(Element modelElement, Element formNode, Element formChild, Map<String, Element> bindings,
                                     Map<String, String> problemList,
                                     Map<String, String> problemListItems, int level) {
        level++;
        int numOfEntries = formChild.getChildCount();
        for (int i = 0; i < numOfEntries; i++) {
            if (formChild.isText(i))
                continue; //Ignore all text.
                
            Element child = formChild.getElement(i);
            //These two attributes are a must for all nodes to be filled with values.
            //eg openmrs_concept="1740^ARV REGIMEN^99DCT" openmrs_datatype="CWE" 
            if (child.getAttributeValue(null, ATTRIBUTE_OPENMRS_DATATYPE) == null
                    && child.getAttributeValue(null, ATTRIBUTE_OPENMRS_CONCEPT) != null)
                continue; //These could be like options for multiple select, which take true or false value.
                
            String name = child.getName();
            
            /* TODO(jonskeet): If we care, move this into buildUiNode.
            if (name.equals("patient_relationship")) {
                RelationshipBuilder.build(modelElement, bodyNode, child);
                continue;
            }
            */
            
            //If the node has an openmrs_concept attribute but is not a top-level node,
            //Or has the openmrs_attribute and openmrs_table attributes. 
            if ((child.getAttributeValue(null, ATTRIBUTE_OPENMRS_CONCEPT) != null && level > 1 /*!child.getName().equals(NODE_OBS)*/)
                    || (child.getAttributeValue(null, ATTRIBUTE_OPENMRS_ATTRIBUTE) != null && child.getAttributeValue(null,
                        ATTRIBUTE_OPENMRS_TABLE) != null)) {
                
                if (!name.equalsIgnoreCase(NODE_PROBLEM_LIST)) {
                    Element bindNode = createBindNode(modelElement, child, bindings, problemList, problemListItems);
                    
                    if (isMultSelectNode(child))
                        addMultipleSelectXformValueNode(child);
                    
                    if (isTableFieldNode(child)) {
                        setTableFieldDataType(name, bindNode);
                        setTableFieldBindingAttributes(name, bindNode);
                        setTableFieldDefaultValue(name, formNode);
                        
                        if ("identifier".equalsIgnoreCase(child.getAttributeValue(null, ATTRIBUTE_OPENMRS_ATTRIBUTE))
                                && "patient_identifier".equalsIgnoreCase(child.getAttributeValue(null,
                                    ATTRIBUTE_OPENMRS_TABLE))) {
                            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD, PRELOAD_PATIENT);
                            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD_PARAMS, "patientIdentifier");
                        }
                    }
                }
            }
            
            //if(child.getAttributeValue(null, ATTRIBUTE_OPENMRS_ATTRIBUTE) != null && child.getAttributeValue(null, ATTRIBUTE_OPENMRS_TABLE) != null){
            //Build UI controls for the openmrs fixed table fields. The rest of the controls are built from
            /* TODO(jonskeet): Move this into buildUiNodes
            if (isTableFieldNode(child)) {
                Element controlNode = buildTableFieldUIControlNode(child, bodyNode);
                
                if (name.equalsIgnoreCase(NODE_ENCOUNTER_LOCATION_ID) && CONTROL_SELECT1.equals(controlNode.getName()))
                    populateLocations(controlNode);
                else if (name.equalsIgnoreCase(NODE_ENCOUNTER_PROVIDER_ID)) {
                    populateProviders(controlNode, formNode, modelElement, bodyNode);
                    
                    //if this is 1.9, we need to add the provider_id_type attribute and set its value, this 
                    //will be used by xml to hl7 xslt to determine if it should include the assigning
                    //authority so that ORUR01 handler in core considers the id to be a providerId 
                    if (XformsUtil.isOnePointNineAndAbove()) {
                        ((Element) child).setAttribute(null, XformBuilder.ATTRIBUTE_PROVIDER_ID_TYPE,
                            XformBuilder.VALUE_PROVIDER_ID_TYPE_PROV_ID);
                    }
                } else if (name.equalsIgnoreCase(NODE_ENCOUNTER_ENCOUNTER_DATETIME))
                    setNodeValue(child, "'today()'"); //Set encounter date defaulting to today
            }
            */
            parseTemplate(modelElement, formNode, child, bindings, problemList, problemListItems, level);
        }
    }
    
    /**
     * Creates a model binding node.
     * 
     * @param modelElement - the model node to add the binding to.
     * @param node - the node whose binding to create.
     * @param bindings - a hashtable of node bindings keyed by their names.
     * @return - the created binding node.
     */
    private static Element createBindNode(Element modelElement, Element node, Map<String, Element> bindings,
                                          Map<String, String> problemList, Map<String, String> problemListItems) {
        Element bindNode = modelElement.createElement(NAMESPACE_XFORMS, null);
        bindNode.setName(NODE_BIND);
        String parentName = ((Element) node.getParent()).getName();
        String binding = node.getName();

        if (bindings.containsKey(binding)) {
            binding = parentName + "_" + binding;            
            problemListItems.put(binding, parentName);
        } else {
            if (!(parentName.equalsIgnoreCase("obs") || parentName.equalsIgnoreCase("patient")
                    || parentName.equalsIgnoreCase("encounter") || parentName.equalsIgnoreCase("problem_list") || parentName
                    .equalsIgnoreCase("orders"))) {
                //binding = parentName + "_" + binding;
                //TODO Need to investigate why the above commented out code brings the no data node found error in the form designer
            }
        }
        
        bindNode.setAttribute(null, ATTRIBUTE_ID, binding);
        
        String name = node.getName();
        String nodeset = getNodesetAttValue(node);
        
        //For problem list element bindings, we do not add the value part.
        if (parentName.equalsIgnoreCase(NODE_PROBLEM_LIST)) {
            problemList.put(name, name);
            nodeset = getNodePath(node);
        }
        
        //Check if this is an item of a problem list.
        if (problemList.containsKey(parentName)) {
            if (problemListItems.containsValue(name)) {
                throw new IllegalStateException("Original code would use repeatSharedKids here, despite it being null");
            }
            problemListItems.put(name, parentName);
        }
        
        bindNode.setAttribute(null, ATTRIBUTE_NODESET, nodeset);
        
        if (!((Element) ((Element) node.getParent()).getParent()).getName().equals(NODE_PROBLEM_LIST))
            modelElement.addChild(Element.ELEMENT, bindNode);
        
        //store the binding node with the key being its id attribute.
        bindings.put(binding, bindNode);
        
        return bindNode;
    }
    
    /**
     * Adds a node to hold the xforms value for a multiple select node. The value is a space
     * delimited list of selected answers, which will later on be used to fill the true or false
     * values as expected by openmrs multiple select questions.
     * 
     * @param node - the multiple select node to add the value node to.
     */
    private static void addMultipleSelectXformValueNode(Element node) {
        //Element xformsValueNode = modelElement.createElement(null, null);
        Element xformsValueNode = node.createElement(null, null);
        xformsValueNode.setName(NODE_XFORMS_VALUE);
        xformsValueNode.setAttribute(null, ATTRIBUTE_XSI_NILL, VALUE_TRUE);
        node.addChild(Element.ELEMENT, xformsValueNode);
    }
    
    /**
     * Set data types for the openmrs fixed table fields.
     * 
     * @param name - the name of the question node.
     * @param bindNode - the binding node whose type attribute we are to set.
     */
    private static void setTableFieldDataType(String name, Element bindNode) {
        if (name.equalsIgnoreCase(NODE_ENCOUNTER_ENCOUNTER_DATETIME)) {
            bindNode.setAttribute(null, ATTRIBUTE_TYPE, XformsUtil.encounterDateIncludesTime() ? DATA_TYPE_DATETIME
                    : DATA_TYPE_DATE);
            bindNode.setAttribute(null, ATTRIBUTE_CONSTRAINT, ". <= today()");
            bindNode.setAttribute(null, (XformsUtil.isJavaRosaSaveFormat() ? "jr:constraintMsg" : ATTRIBUTE_MESSAGE),
                "Encounter date cannot be after today");
        } else if (name.equalsIgnoreCase(NODE_ENCOUNTER_LOCATION_ID))
            bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_INT);
        else if (name.equalsIgnoreCase(NODE_ENCOUNTER_PROVIDER_ID))
            bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_INT);
        else if (name.equalsIgnoreCase(NODE_PATIENT_PATIENT_ID))
            bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_INT);
        else
            bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
        
    }
    
    /**
     * Set required and readonly attributes for the openmrs fixed table fields.
     * 
     * @param name - the name of the question node.
     * @param bindNode - the binding node whose required and readonly attributes we are to set.
     */
    private static void setTableFieldBindingAttributes(String name, Element bindNode) {
        
        if (name.equalsIgnoreCase(NODE_ENCOUNTER_ENCOUNTER_DATETIME))
            bindNode.setAttribute(null, ATTRIBUTE_REQUIRED, XPATH_VALUE_TRUE);
        else if (name.equalsIgnoreCase(NODE_ENCOUNTER_LOCATION_ID))
            bindNode.setAttribute(null, ATTRIBUTE_REQUIRED, XPATH_VALUE_TRUE);
        else if (name.equalsIgnoreCase(NODE_ENCOUNTER_PROVIDER_ID))
            bindNode.setAttribute(null, ATTRIBUTE_REQUIRED, XPATH_VALUE_TRUE);
        else if (name.equalsIgnoreCase(NODE_PATIENT_PATIENT_ID)) {
            bindNode.setAttribute(null, ATTRIBUTE_REQUIRED, XPATH_VALUE_TRUE);
            //bindNode.setAttribute(null, ATTRIBUTE_READONLY, XPATH_VALUE_TRUE);
            //bindNode.setAttribute(null, ATTRIBUTE_LOCKED, XPATH_VALUE_TRUE);
            bindNode.setAttribute(null, ATTRIBUTE_VISIBLE, XPATH_VALUE_FALSE);
            
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD, PRELOAD_PATIENT);
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD_PARAMS, "patientId");
        } else {
            //all table field are readonly on forms since they cant be populated in their tables 
            //form encounter forms. This population only happens when creating or editing patient.
            bindNode.setAttribute(null, ATTRIBUTE_LOCKED, XPATH_VALUE_TRUE);
            
            //The ATTRIBUTE_READONLY prevents firefox from displaying values in the disabled
            //widgets. So this is why we are using locked which will still be readonly
            //but values can be seen in the widgets.
        }
        /*else if(name.equalsIgnoreCase(NODE_PATIENT_FAMILY_NAME))
            bindNode.setAttribute(null, ATTRIBUTE_READONLY, XPATH_VALUE_TRUE);
        else if(name.equalsIgnoreCase(NODE_PATIENT_MIDDLE_NAME))
            bindNode.setAttribute(null, ATTRIBUTE_READONLY, XPATH_VALUE_TRUE);
        else if(name.equalsIgnoreCase(NODE_PATIENT_GIVEN_NAME))
            bindNode.setAttribute(null, ATTRIBUTE_READONLY, XPATH_VALUE_TRUE);
        else{
            bindNode.setAttribute(null, ATTRIBUTE_READONLY, XPATH_VALUE_TRUE);
            bindNode.setAttribute(null, ATTRIBUTE_LOCKED, XPATH_VALUE_TRUE);
        }*/

        //jr:preload="patient" jr:preloadParams="ID"
        
        if (name.equalsIgnoreCase(NODE_PATIENT_BIRTH_DATE)) {
            bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_DATE);
            
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD, PRELOAD_PATIENT);
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD_PARAMS, "birthDate");
        } else if (name.equalsIgnoreCase(NODE_PATIENT_BIRTH_DATE_ESTIMATED))
            bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_BOOLEAN);
        
        //peloaders
        if (name.equalsIgnoreCase(NODE_PATIENT_FAMILY_NAME)) {
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD, PRELOAD_PATIENT);
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD_PARAMS, "familyName");
        } else if (name.equalsIgnoreCase(NODE_PATIENT_MIDDLE_NAME)) {
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD, PRELOAD_PATIENT);
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD_PARAMS, "middleName");
        } else if (name.equalsIgnoreCase(NODE_PATIENT_GIVEN_NAME)) {
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD, PRELOAD_PATIENT);
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD_PARAMS, "givenName");
        } else if (name.equalsIgnoreCase(NODE_PATIENT_GENDER)) {
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD, PRELOAD_PATIENT);
            bindNode.setAttribute(null, ATTRIBUTE_PRELOAD_PARAMS, "sex");
        }
    }
    
    private static void setTableFieldDefaultValue(String name, Element formElement) {
        Integer formId = Integer.valueOf(formElement.getAttributeValue(null, ATTRIBUTE_ID));
        String val = getFieldDefaultValue(name, formId, true);
        if (val != null)
            setNodeValue(formElement, name, val);
    }
    
    private static String getFieldDefaultValue(String name, Integer formId, boolean forAllPatients) {
        XformsService xformsService = Context.getService(XformsService.class);
        String val = xformsService.getFieldDefaultValue(formId, name);
        if (val == null) {
            val = xformsService.getFieldDefaultValue(formId, name.replace('_', ' '));
            if (val == null)
                return null;
        }
        
        if (!val.contains("$!{"))
            return val;
        else if (!forAllPatients) {
            Integer id = getDefaultValueId(val);
            if (id != null)
                return id.toString();
        }
        
        return null;
    }
    
    private static Integer getDefaultValueId(String val) {
        int pos1 = val.indexOf('(');
        if (pos1 == -1)
            return null;
        int pos2 = val.indexOf(')');
        if (pos2 == -1)
            return null;
        if ((pos2 - pos1) < 2)
            return null;
        
        String id = val.substring(pos1 + 1, pos2);
        try {
            return Integer.valueOf(id);
        }
        catch (Exception e) {}
        
        return null;
    }
    
    /**
     * Check whether a node is an openmrs table field node These are the ones with the attributes:
     * openmrs_table and openmrs_attribute e.g. patient_unique_number
     * openmrs_table="PATIENT_IDENTIFIER" openmrs_attribute="IDENTIFIER"
     * 
     * @param node - the node to check.
     * @return - true if it is, else false.
     */
    private static boolean isTableFieldNode(Element node) {
        return (node.getAttributeValue(null, ATTRIBUTE_OPENMRS_ATTRIBUTE) != null && node.getAttributeValue(null,
            ATTRIBUTE_OPENMRS_TABLE) != null);
        
    }
    
    /**
     * Checks whether a node is multiple select or not.
     * 
     * @param child - the node to check.k
     * @return - true if multiple select, else false.
     */
    private static boolean isMultSelectNode(Element child) {
        return (child.getAttributeValue(null, ATTRIBUTE_MULTIPLE) != null && child.getAttributeValue(null,
            ATTRIBUTE_MULTIPLE).equals("1"));
    }
    
    /**
     * Check if a given node as an openmrs value node.
     * 
     * @param node - the node to check.
     * @return - true if it has, else false.
     */
    private static boolean hasValueNode(Element node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.isText(i))
                continue;
            
            Element child = node.getElement(i);
            if (child.getName().equalsIgnoreCase(NODE_VALUE))
                return true;
        }
        
        return false;
    }
    
    /**
     * Gets the value of the nodeset attribute, for a given node, used for xform bindings.
     * 
     * @param node - the node.
     * @return - the value of the nodeset attribite.
     */
    private static String getNodesetAttValue(Element node) {
        if (hasValueNode(node))
            return getNodePath(node) + "/value";
        else if (isMultSelectNode(node))
            return getNodePath(node) + "/xforms_value";
        else
            return getNodePath(node);
    }
    
    /**
     * Gets the path of a node from the instance node.
     * 
     * @param node - the node whose path to get.
     * @return - the complete path from the instance node.
     */
    private static String getNodePath(Element node) {
        String path = node.getName();
        Element parent = (Element) node.getParent();
        while (parent != null && !parent.getName().equalsIgnoreCase(NODE_INSTANCE)) {
            path = parent.getName() + NODE_SEPARATOR + path;
            if (parent.getParent() != null && parent.getParent() instanceof Element)
                parent = (Element) parent.getParent();
            else
                parent = null;
        }
        return NODE_SEPARATOR + path;
    }
    
    /**
     * Converts an xml document to a string.
     * 
     * @param doc - the document.
     * @return the xml string in in the document.
     */
    public static String fromDoc2String(Document doc) throws IOException {        
        KXmlSerializer serializer = new KXmlSerializer();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.setOutput(bos, XformConstants.DEFAULT_CHARACTER_ENCODING);
        doc.write(serializer);
        serializer.flush();
        
        return new String(bos.toByteArray(), XformConstants.DEFAULT_CHARACTER_ENCODING);
    }
    
    /**
     * Gets a document from a stream reader.
     */
    public static Document getDocument(Reader reader) throws XmlPullParserException, IOException {
        Document doc = new Document();
        
        KXmlParser parser = new KXmlParser();
        parser.setInput(reader);
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        
        doc.parse(parser);
        return doc;
    }
}
