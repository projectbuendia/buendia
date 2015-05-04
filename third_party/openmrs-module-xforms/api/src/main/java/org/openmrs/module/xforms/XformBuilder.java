/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.xforms;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.CommonsLogLogChute;
import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptSource;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.Relationship;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.FormService;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.module.xforms.formentry.FormEntryWrapper;
import org.openmrs.module.xforms.util.ConceptUtil;
import org.openmrs.module.xforms.util.LocationUtil;
import org.openmrs.module.xforms.util.XformBuilderUtil;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.util.OpenmrsConstants.PERSON_TYPE;
import org.openmrs.util.OpenmrsUtil;
import org.xmlpull.v1.XmlPullParser;

//TODO This class is too big. May need breaking into smaller ones.

/**
 * Builds xforms from openmrs schema and template files. This class also builds the XForm for
 * creating new patients.
 * 
 * @author Daniel Kayiwa
 */
public final class XformBuilder implements GlobalPropertyListener {
	
	/** Namespace for XForms. */
	public static final String NAMESPACE_XFORMS = "http://www.w3.org/2002/xforms";
	
	/** Namespace for XML schema. */
	public static final String NAMESPACE_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	
	/** Namespace for XML schema instance. */
	public static final String NAMESPACE_XML_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
	
	/** Namespace for XHTML. */
	public static final String NAMESPACE_XHTML = "http://www.w3.org/1999/xhtml";
	
	/** Namespace prefix for openmrs custom types. */
	public static final String PREFIX_OPENMRS_TYPE = "openmrstype";
	
	/** Namespace prefix for XForms. */
	public static final String PREFIX_XFORMS = "xf";
	
	/** Namespace prefix for XML schema. */
	public static final String PREFIX_XML_SCHEMA = "xsd";
	
	/** The second Namespace prefix for XML schema. */
	public static final String PREFIX_XML_SCHEMA2 = "xs";
	
	/** Namespace prefix for XML schema instance. */
	public static final String PREFIX_XML_INSTANCES = "xsi";
	
	/** Namespace prefix for openmrs. */
	public static final String PREFIX_OPENMRS = "openmrs";
	
	/** The character separator for namespace prefix. */
	public static final String NAMESPACE_PREFIX_SEPARATOR = ":";
	
	public static final String CONTROL_INPUT = "input";
	
	public static final String CONTROL_SELECT = "select";
	
	public static final String CONTROL_SELECT1 = "select1";
	
	public static final String CONTROL_REPEAT = "repeat";
	
	public static final String NODE_LABEL = "label";
	
	public static final String NODE_HINT = "hint";
	
	public static final String NODE_VALUE = "value";
	
	public static final String NODE_ITEM = "item";
	
	public static final String NODE_HTML = "html";
	
	public static final String NODE_XFORMS = "xforms";
	
	public static final String NODE_SCHEMA = "schema";
	
	public static final String NODE_INSTANCE = "instance";
	
	public static final String NODE_MODEL = "model";
	
	public static final String NODE_BIND = "bind";
	
	public static final String NODE_ENUMERATION = "enumeration";
	
	public static final String NODE_DATE = "date";
	
	public static final String NODE_TIME = "time";
	
	public static final String NODE_SIMPLETYPE = "simpleType";
	
	public static final String NODE_COMPLEXTYPE = "complexType";
	
	public static final String NODE_SEQUENCE = "sequence";
	
	public static final String NODE_RESTRICTION = "restriction";
	
	public static final String NODE_ATTRIBUTE = "attribute";
	
	public static final String NODE_FORM = "form";
	
	public static final String NODE_PATIENT = "patient";
	
	public static final String NODE_XFORMS_VALUE = "xforms_value";
	
	public static final String NODE_OBS = "obs";
	
	public static final String NODE_PROBLEM_LIST = "problem_list";
	
	public static final String NODE_GROUP = "group";
	
	public static final String NODE_MININCLUSIVE = "minInclusive";
	
	public static final String NODE_MAXINCLUSIVE = "maxInclusive";
	
	public static final String ATTRIBUTE_ID = "id";
	
	public static final String ATTRIBUTE_NODESET = "nodeset";
	
	public static final String ATTRIBUTE_NAME = "name";
	
	public static final String ATTRIBUTE_BIND = "bind";
	
	public static final String ATTRIBUTE_REF = "ref";
	
	public static final String ATTRIBUTE_APPEARANCE = "appearance";
	
	public static final String ATTRIBUTE_NILLABLE = "nillable";
	
	public static final String ATTRIBUTE_MAXOCCURS = "maxOccurs";
	
	public static final String ATTRIBUTE_TYPE = "type";
	
	public static final String ATTRIBUTE_FIXED = "fixed";
	
	public static final String ATTRIBUTE_OPENMRS_DATATYPE = "openmrs_datatype";
	
	public static final String ATTRIBUTE_OPENMRS_CONCEPT = "openmrs_concept";
	
	public static final String ATTRIBUTE_OPENMRS_ATTRIBUTE = "openmrs_attribute";
	
	public static final String ATTRIBUTE_OPENMRS_TABLE = "openmrs_table";
	
	public static final String ATTRIBUTE_SUBMISSION = "submission";
	
	public static final String ATTRIBUTE_READONLY = "readonly";
	
	public static final String ATTRIBUTE_LOCKED = "locked";
	
	public static final String ATTRIBUTE_REQUIRED = "required";
	
	public static final String ATTRIBUTE_VISIBLE = "visible";
	
	public static final String ATTRIBUTE_DESCRIPTION_TEMPLATE = "description-template";
	
	public static final String ATTRIBUTE_BASE = "base";
	
	public static final String ATTRIBUTE_XSI_NILL = "xsi:nil";
	
	public static final String ATTRIBUTE_RESOURCE = "resource";
	
	public static final String ATTRIBUTE_MULTIPLE = "multiple";
	
	public static final String ATTRIBUTE_CONSTRAINT = "constraint";
	
	public static final String ATTRIBUTE_MESSAGE = "message";
	
	public static final String ATTRIBUTE_VALUE = "value";
	
	public static final String ATTRIBUTE_CONCEPT_ID = "concept_id";
	
	public static final String ATTRIBUTE_PROVIDER_ID_TYPE = "provider_id_type";
	
	public static final String ATTRIBUTE_UUID = "uuid";
	
	public static final String XPATH_VALUE_TRUE = "true()";
	
	public static final String XPATH_VALUE_FALSE = "false()";
	
	public static final String XPATH_VALUE_LAST = "last()";
	
	public static final String VALUE_TRUE = "true";
	
	public static final String VALUE_FALSE = "false";
	
	public static final String VALUE_PROVIDER_ID_TYPE_PROV_ID = "PROVIDER.ID";
	
	public static final String NODE_SEPARATOR = "/";
	
	public static final String NODE_ENCOUNTER_LOCATION_ID = "encounter.location_id";
	
	public static final String NODE_ENCOUNTER_PROVIDER_ID = "encounter.provider_id";
	
	public static final String NODE_ENCOUNTER_ENCOUNTER_ID = "encounter.encounter_id";
	
	public static final String NODE_ENCOUNTER_ENCOUNTER_DATETIME = "encounter.encounter_datetime";
	
	public static final String NODE_PATIENT_PATIENT_ID = "patient.patient_id";
	
	public static final String NODE_PATIENT_FAMILY_NAME = "patient.family_name";
	
	public static final String NODE_PATIENT_MIDDLE_NAME = "patient.middle_name";
	
	public static final String NODE_PATIENT_GIVEN_NAME = "patient.given_name";
	
	public static final String NODE_PATIENT_BIRTH_DATE = "patient.birthdate";
	
	public static final String NODE_PATIENT_BIRTH_DATE_ESTIMATED = "patient.birthdate_estimated";
	
	public static final String NODE_PATIENT_GENDER = "patient.sex";
	
	public static final String NODE_PATIENT_IDENTIFIER_TYPE = "patient_identifier.identifier_type";
	
	public static final String NODE_PATIENT_IDENTIFIER_TYPE_ID = "patient_identifier.identifier_type_id";
	
	public static final String NODE_PATIENT_ID = "patient_id";
	
	public static final String NODE_FAMILY_NAME = "family_name";
	
	public static final String NODE_MIDDLE_NAME = "middle_name";
	
	public static final String NODE_GIVEN_NAME = "given_name";
	
	public static final String NODE_GENDER = "gender";
	
	public static final String NODE_IDENTIFIER = "identifier";
	
	public static final String NODE_BIRTH_DATE = "birth_date";
	
	public static final String NODE_BIRTH_DATE_ESTIMATED = "birth_date_estimated";
	
	public static final String NODE_BIRTHDATE = "birthdate";
	
	public static final String NODE_BIRTHDATE_ESTIMATED = "birthdate_estimated";
	
	public static final String NODE_LOCATION_ID = "location_id";
	
	public static final String NODE_PROVIDER_ID = "provider_id";
	
	public static final String NODE_IDENTIFIER_TYPE_ID = "patient_identifier_type_id";
	
	public static final String NODE_DEGREE = "degree";
	
	public static final String NODE_FAMILY_NAME2 = "family_name2";
	
	public static final String NODE_FAMILY_NAME_PREFIX = "family_name_prefix";
	
	public static final String NODE_FAMILY_NAME_SUFFIX = "family_name_suffix";
	
	public static final String NODE_PREFIX = "prefix";
	
	public static final String NODE_LOAD = "load";
	
	public static final String DATA_TYPE_DATE = "xsd:date";
	
	public static final String DATA_TYPE_INT = "xsd:int";
	
	public static final String DATA_TYPE_TEXT = "xsd:string";
	
	public static final String DATA_TYPE_BOOLEAN = "xsd:boolean";
	
	public static final String DATA_TYPE_DECIMAL = "xsd:decimal";
	
	public static final String DATA_TYPE_BASE64BINARY = "xsd:base64Binary";
	
	public static final String DATA_TYPE_DATETIME = "xsd:dateTime";
	
	public static final String DATA_TYPE_TIME = "xsd:time";
	
	public static final String MULTIPLE_SELECT_VALUE_SEPARATOR = " ";
	
	/**
	 * The last five characters of an xml schema complex type name for a concept. e.g weight_kg_type
	 * where _type is appended to the concept weight_kg.
	 */
	public static final String COMPLEX_TYPE_NAME_POSTFIX = "_type";
	
	public static final String SIMPLE_TYPE_NAME_POSTFIX = "_type_restricted_type";
	
	/**
	 * The last eight characters of an xml schema complex type name for a concept. e.g
	 * problem_added_section where _section is appended to the concept problem_added.
	 */
	public static final String COMPLEX_SECTION_NAME_POSTFIX = "_section";
	
	/** The complex type node having a list of problems. e.g. Problem Added, Problem Resolved, etc. */
	public static final String COMPLEX_TYPE_NAME_PROBLEM_LIST = "problem_list_section";
	
	public static final String MODEL_ID = "openmrs_model";
	
	public static final String INSTANCE_ID = "openmrs_model_instance";
	
	public static final String BINDING_LOCATION_ID = "/form/encounter/encounter.location_id";
	
	public static final String BINDING_PATIENT_ID = "/form/patient/patient.patient_id";
	
	public static final String BINDING_FAMILY_NAME = "/form/patient/patient.family_name";
	
	public static final String BINDING_GIVEN_NAME = "/form/patient/patient.given_name";
	
	public static final String BINDING_MIDDLE_NAME = "/form/patient/patient.middle_name";
	
	public static final String BINDING_GENDER = "/form/patient/patient.sex";
	
	public static final String BINDING_BIRTH_DATE = "/form/patient/patient.birthdate";
	
	public static final String BINDING_BIRTH_DATE_ESTIMATED = "/form/patient/patient.birthdate_estimated";
	
	public static final String BINDING_IDENTIFIER_TYPE = "/form/patient/patient_identifier.identifier_type";
	
	public static final String NODE_NAME_PERSON_ADDRESSES = "person_addresses";
	
	public static final String NODE_NAME_PREFERRED = "preferred";
	
	public static final String NODE_NAME_ADDRESS1 = "address1";
	
	public static final String NODE_NAME_ADDRESS2 = "address2";
	
	public static final String NODE_NAME_CITY_VILLAGE = "city_village";
	
	public static final String NODE_NAME_STATE_PROVINCE = "state_province";
	
	public static final String NODE_NAME_POSTAL_CODE = "postal_code";
	
	public static final String NODE_NAME_COUNTRY = "country";
	
	public static final String NODE_NAME_LATITUDE = "latitude";
	
	public static final String NODE_NAME_LONGITUDE = "longitude";
	
	public static final String NODE_NAME_COUNTY_DISTRICT = "county_district";
	
	public static final String NODE_NAME_NEIGHBORHOOD_CELL = "neighborhood_cell";
	
	public static final String NODE_NAME_REGION = "region";
	
	public static final String NODE_NAME_SUBREGION = "subregion";
	
	public static final String NODE_NAME_TOWNSHIP_DIVISION = "township_division";
	
	public static final String NODE_NAME_PREFIX_PERSON_ADDRESS = "person_address_";
	
	public static final String NODE_NAME_OTHER_IDENTIFIERS = "other_identifiers";
	
	public static final String NODE_NAME_OTHER_IDENTIFIER = "other_identifier";
	
	public static final String NODE_NAME_OTHER_IDENTIFIER_TYPE_ID = "other_identifier_type_id";
	
	public static final String NODE_NAME_OTHER_IDENTIFIER_LOCATION_ID = "other_identifier_location_id";
	
	private static Hashtable<String, String> obsRepeatItems;
	
	private static Hashtable<String, String> nodesets;
	
	private static Hashtable<String, List<String>> repeatSharedKids;
	
	private static Hashtable<String, List<String>> sharedRestrictions;
	
	private static Hashtable<String, String> repeatChildTypes;
	
	private static String ATTRIBUTE_PRELOAD = "jr:preload";
	
	private static String ATTRIBUTE_PRELOAD_PARAMS = "jr:preloadParams";
	
	private static String PRELOAD_PATIENT = "patient";
	
	private static Log log = LogFactory.getLog(XformBuilder.class);
	
	private static Boolean useAutoCompleteForLocations;
	
	private static Boolean useAutoCompleteForProviders;
	
	/**
	 * Builds an Xform from an openmrs schema and template xml.
	 * 
	 * @param schemaXml - the schema xml.
	 * @param templateXml - the template xml.
	 * @return - the built xform's xml.
	 */
	public static String getXform4mStrings(String schemaXml, String templateXml) throws Exception {
		return getXform4mDocuments(getDocument(new StringReader(schemaXml)), getDocument(new StringReader(templateXml)));
	}
	
	/**
	 * Creates an xform from schema and template files.
	 * 
	 * @param schemaPathName - the complete path and name of the schema file.
	 * @param templatePathName - the complete path and name of the template file
	 * @return the built xform's xml.
	 */
	public static String getXform4mFiles(String schemaPathName, String templatePathName, String xformAction) {
		try {
			Document schemaDoc = getDocument(new FileReader(schemaPathName));
			Document templateDoc = getDocument(new FileReader(templatePathName));
			return getXform4mDocuments(schemaDoc, templateDoc);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Converts xml to a documnt object.
	 * 
	 * @param xml - the xml to convert.
	 * @return - the Document object containing the xml.
	 */
	public static Document getDocument(String xml) {
		return getDocument(new StringReader(xml));
	}
	
	/**
	 * Sets the value of a node in a document.
	 * 
	 * @param doc - the document.
	 * @param name - the name of the node whose value to set.
	 * @param value - the value to set.
	 * @return - true if the node with the name was found, else false.
	 */
	public static boolean setNodeValue(Document doc, String name, String value) {
		return setNodeValue(doc.getRootElement(), name, value);
	}
	
	/**
	 * Sets the value of a child node in a parent node.
	 * 
	 * @param doc - the document.
	 * @param name - the name of the node whose value to set.
	 * @param value - the value to set.
	 * @return - true if the node with the name was found, else false.
	 */
	public static boolean setNodeValue(Element parentNode, String name, String value) {
		Element node = getElement(parentNode, name);
		if (node == null)
			return false;
		
		setNodeValue(node, value);
		return true;
	}
	
	public static String getNodeValue(Element parentNode, String name) {
		Element node = getElement(parentNode, name);
		if (node == null)
			return null;
		
		return getTextValue(node);
	}
	
	public static String getTextValue(Element node) {
		int numOfEntries = node.getChildCount();
		for (int i = 0; i < numOfEntries; i++) {
			if (node.isText(i))
				return node.getText(i);
			
			if (node.getType(i) == Element.ELEMENT) {
				String val = getTextValue(node.getElement(i));
				if (val != null)
					return val;
			}
		}
		
		return null;
	}
	
	/**
	 * Sets the text value of a node.
	 * 
	 * @param node - the node whose value to set.
	 * @param value - the value to set.
	 */
	public static void setNodeValue(Element node, String value) {
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
	 * Sets the value of an attribute of a node in a document.
	 * 
	 * @param doc - the document.
	 * @param nodeName - the name of the node.
	 * @param attributeName - the name of the attribute.
	 * @param value - the value to set.
	 * @return
	 */
	public static boolean setNodeAttributeValue(Document doc, String nodeName, String attributeName, String value) {
		Element node = getElement(doc.getRootElement(), nodeName);
		if (node == null)
			return false;
		
		node.setAttribute(null, attributeName, value);
		return true;
	}
	
	/**
	 * Gets a child element of a parent node with a given name.
	 * 
	 * @param parent - the parent element
	 * @param name - the name of the child.
	 * @return - the child element.
	 */
	public static Element getElement(Element parent, String name) {
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
	 * Gets a child element of a parent node with a given attribute prefix.
	 * 
	 * @param parent - the parent element
	 * @param attributeName - the name of the attribute.
	 * @param attributePrefix - the prefix of the attribute.
	 * @return - the child element.
	 */
	public static Element getElementByAttributePrefix(Element parent, String attributeName, String attributePrefix,
	                                                  boolean includeAttribute, String includeAttributeName,
	                                                  boolean copyIfNotExists, List<String> nonCopyAttributes) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			if (parent.getType(i) != Element.ELEMENT)
				continue;
			
			Element child = (Element) parent.getChild(i);
			
			//Node name may not match due to change of concept name
			//and hence we only check for attribute value of concept id
			String value = child.getAttributeValue(null, attributeName);
			if (value != null && value.startsWith(attributePrefix)) {
				String attributeValue = child.getAttributeValue(null, includeAttributeName); //if obs id is already filled, the just look for another node (Problem lists are normally more than one)
				if (attributeValue == null || includeAttribute)
					return child;
				else if (copyIfNotExists && attributeValue != null)
					return createCopy(child, nonCopyAttributes);
			}
			
			child = getElementByAttributePrefix(child, attributeName, attributePrefix, includeAttribute,
			    includeAttributeName, copyIfNotExists, nonCopyAttributes);
			if (child != null)
				return child;
		}
		
		return null;
	}
	
	/**
	 * Gets a child element of a parent node with a given attribute value.
	 * 
	 * @param parent - the parent element
	 * @param attributeName - the name of the attribute.
	 * @param attributeValue - the value of the attribute.
	 * @return - the child element.
	 */
	public static Element getElementByAttributeValue(Element parent, String attributeName, String attributeValue) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			if (parent.getType(i) != Element.ELEMENT)
				continue;
			
			Element child = (Element) parent.getChild(i);
			
			//Node name may not match due to change of concept name
			//and hence we only check for attribute value of concept id
			String value = child.getAttributeValue(null, attributeName);
			if (value != null && value.equalsIgnoreCase(attributeValue))
				return child;
			
			child = getElementByAttributeValue(child, attributeName, attributeValue);
			if (child != null)
				return child;
		}
		
		return null;
	}
	
	/**
	 * Gets a child element of a parent node with a given name.
	 * 
	 * @param parent - the parent element
	 * @param name - the name of the child.
	 * @return - the child element.
	 */
	public static Element getElementEx(Element parent, String name, String attributeName, String attributeValue) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			if (parent.getType(i) != Element.ELEMENT)
				continue;
			
			Element child = (Element) parent.getChild(i);
			if (child.getName().equalsIgnoreCase(name)
			        && attributeValue.equalsIgnoreCase(child.getAttributeValue(null, attributeName)))
				return child;
			
			child = getElementEx(child, name, attributeName, attributeValue);
			if (child != null)
				return child;
		}
		
		return null;
	}
	
	/**
	 * Builds an Xfrom from an openmrs schema and template document.
	 * 
	 * @param schemaDoc - the schema document.
	 * @param templateDoc - the template document.
	 * @return - the built xform's xml.
	 */
	public static String getXform4mDocuments(Document schemaDoc, Document templateDoc) throws Exception {
		obsRepeatItems = new Hashtable<String, String>();
		nodesets = new Hashtable<String, String>();
		repeatSharedKids = new Hashtable<String, List<String>>();
		sharedRestrictions = new Hashtable<String, List<String>>();
		repeatChildTypes = new Hashtable<String, String>();
		
		Element formNode = (Element) templateDoc.getRootElement();
		
		Document doc = new Document();
		doc.setEncoding(XformConstants.DEFAULT_CHARACTER_ENCODING);
		
		Element xformsNode = doc.createElement(NAMESPACE_XFORMS, null);
		xformsNode.setName(NODE_XFORMS);
		xformsNode.setPrefix(PREFIX_XFORMS, NAMESPACE_XFORMS);
		xformsNode.setPrefix(PREFIX_XML_SCHEMA, NAMESPACE_XML_SCHEMA);
		xformsNode.setPrefix(PREFIX_XML_SCHEMA2, NAMESPACE_XML_SCHEMA);
		xformsNode.setPrefix(PREFIX_XML_INSTANCES, NAMESPACE_XML_INSTANCE);
		
		//if(XformsUtil.isJavaRosaSaveFormat())
		xformsNode.setPrefix("jr", "http://openrosa.org/javarosa");
		
		doc.addChild(org.kxml2.kdom.Element.ELEMENT, xformsNode);
		
		Element modelNode = doc.createElement(NAMESPACE_XFORMS, null);
		modelNode.setName(NODE_MODEL);
		modelNode.setAttribute(null, ATTRIBUTE_ID, MODEL_ID);
		xformsNode.addChild(Element.ELEMENT, modelNode);
		
		Element groupNode = doc.createElement(NAMESPACE_XFORMS, null);
		groupNode.setName(NODE_GROUP);
		Element labelNode = doc.createElement(NAMESPACE_XFORMS, null);
		labelNode.setName(NODE_LABEL);
		labelNode.addChild(Element.TEXT, "Page1");
		groupNode.addChild(Element.ELEMENT, labelNode);
		xformsNode.addChild(Element.ELEMENT, groupNode);
		
		Element instanceNode = doc.createElement(NAMESPACE_XFORMS, null);
		instanceNode.setName(NODE_INSTANCE);
		instanceNode.setAttribute(null, ATTRIBUTE_ID, INSTANCE_ID);
		modelNode.addChild(Element.ELEMENT, instanceNode);
		
		instanceNode.addChild(Element.ELEMENT, formNode);
		
		Document xformSchemaDoc = new Document();
		xformSchemaDoc.setEncoding(XformConstants.DEFAULT_CHARACTER_ENCODING);
		Element xformSchemaNode = doc.createElement(NAMESPACE_XML_SCHEMA, null);
		xformSchemaNode.setName(NODE_SCHEMA);
		xformSchemaDoc.addChild(org.kxml2.kdom.Element.ELEMENT, xformSchemaNode);
		
		Hashtable bindings = new Hashtable();
		Hashtable<String, String> problemList = new Hashtable<String, String>();
		Hashtable<String, String> problemListItems = new Hashtable<String, String>();
		parseTemplate(modelNode, formNode, formNode, bindings, groupNode, problemList, problemListItems, 0);
		parseSchema(schemaDoc.getRootElement(), groupNode, modelNode, xformSchemaNode, bindings, problemList,
		    problemListItems);
		
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
								addConceptMapAttributes(grandChildElement, value);
						}
					}
				}
			}
		}
		
		cleanUp();
		
		return fromDoc2String(doc);
	}
	
	private static void cleanUp() {
		obsRepeatItems.clear();
		nodesets.clear();
		repeatSharedKids.clear();
		sharedRestrictions.clear();
		repeatChildTypes.clear();
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
			ConceptSource preferredSource = null;
			String prefSourceName = Context.getAdministrationService().getGlobalProperty(
			    XformConstants.GLOBAL_PROP_KEY_PREFERRED_CONCEPT_SOURCE);
			if (StringUtils.isNotBlank(prefSourceName)) {
				preferredSource = cs.getConceptSourceByName(prefSourceName);
				if (concept.getConceptMappings().size() > 0) {
					if (preferredSource != null) {
						for (ConceptMap map : concept.getConceptMappings()) {
							if (OpenmrsUtil.nullSafeEquals(preferredSource, map.getSource())) {
								element.setAttribute(null, ATTRIBUTE_OPENMRS_CONCEPT,
								    map.getSource().getName() + ":" + map.getSourceCode());
								return;
							}
						}
					}
				}
			}
		}
		catch (NumberFormatException e) {
			log.warn(e.getMessage());
		}
	}
	
	/**
	 * Gets the label of an openmrs standard form node
	 * 
	 * @param name - the name of the node.
	 * @return - the label.
	 */
	public static String getDisplayText(String name) {
		/*if(name.equalsIgnoreCase(NODE_ENCOUNTER_ENCOUNTER_DATETIME))
			return "ENCOUNTER DATE";
		else if(name.equalsIgnoreCase(NODE_ENCOUNTER_LOCATION_ID))
			return "LOCATION";
		else if(name.equalsIgnoreCase(NODE_ENCOUNTER_PROVIDER_ID))
			return "PROVIDER";
		else if(name.equalsIgnoreCase(NODE_PATIENT_PATIENT_ID))
			return "PATIENT ID";
		else if(name.equalsIgnoreCase(NODE_PATIENT_MIDDLE_NAME))
			return "MIDDLE NAME";
		else if(name.equalsIgnoreCase(NODE_PATIENT_GIVEN_NAME))
			return "GIVEN NAME";
		else if(name.equalsIgnoreCase(NODE_PATIENT_FAMILY_NAME))
			return "FAMILY NAME";
		else
			return name.replace('_', ' ');*/

		name = name.replace('.', ' ');
		name = name.replace("patient ", "");
		name = name.replace("encounter ", "");
		name = name.replace("person_address ", "");
		name = name.replace("patient_address ", "");
		name = name.replace("person_name ", "");
		name = name.replace("person_attribute ", "");
		name = name.replace("patient_identifier ", "");
		
		name = name.replace('_', ' '); //This is done after the above in order not to make patient_id=id
		name = name.toUpperCase();
		
		return name;
	}
	
	/**
	 * Parses an openmrs template and builds the bindings in the model plus UI controls for openmrs
	 * table field questions.
	 * 
	 * @param modelElement - the model element to add bindings to.
	 * @param formNode the form node.
	 * @param bindings - a hash table to populate with the built bindings.
	 * @param bodyNode - the body node to add the UI control to.
	 */
	public static void parseTemplate(Element modelElement, Element formNode, Element formChild, Hashtable bindings,
	                                 Element bodyNode, Hashtable<String, String> problemList,
	                                 Hashtable<String, String> problemListItems, int level) {
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
			
			if (name.equals("patient_relationship")) {
				RelationshipBuilder.build(modelElement, bodyNode, child);
				continue;
			}
			
			//If the node has an openmrs_concept attribute but is not called obs,
			//Or has the openmrs_attribite and openmrs_table attributes. 
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
			
			parseTemplate(modelElement, formNode, child, bindings, bodyNode, problemList, problemListItems, level);
		}
	}
	
	/**
	 * Builds a UI control node for a table field.
	 * 
	 * @param node - the node whose UI control to build.
	 * @param bodyNode - the body node to add the UI control to.
	 * @return - the created UI control node.
	 */
	private static Element buildTableFieldUIControlNode(Element node, Element bodyNode) {
		
		Element controlNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
		String name = node.getName();
		
		//location and provider are not free text.
		if (name.equalsIgnoreCase(NODE_ENCOUNTER_LOCATION_ID) || name.equalsIgnoreCase(NODE_ENCOUNTER_PROVIDER_ID)) {
			if (useAutoCompleteForNode(name))
				controlNode.setName(CONTROL_INPUT);
			else
				controlNode.setName(CONTROL_SELECT1);
		} else {
			controlNode.setName(CONTROL_INPUT);
		}
		
		controlNode.setAttribute(null, ATTRIBUTE_BIND, name);
		
		//create the label
		Element labelNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
		labelNode.setName(NODE_LABEL);
		labelNode.addChild(Element.TEXT, getDisplayText(name) + "     ");
		controlNode.addChild(Element.ELEMENT, labelNode);
		
		addControl(bodyNode, controlNode);
		
		return controlNode;
	}
	
	/**
	 * Populates a UI control node with providers.
	 * 
	 * @param controlNode - the UI control node.
	 */
	private static void populateProviders(Element controlNode, Element formNode, Element modelNode, Element groupNode) {
		try {
			//If we are on 1.9 and above, try use the new provider API
			if (XformsUtil.isOnePointNineAndAbove()) {
				XformBuilderUtil.populateProviders(controlNode);
				return;
			}
			/*if(XformBuilderUtil.populateProviders19(formNode, modelNode, groupNode)){
				return;
			}*/

			List<User> providers = Context.getUserService().getUsersByRole(new Role("Provider"));
			for (User provider : providers) {
				
				Integer personId = XformsUtil.getPersonId(provider);
				
				Element itemNode = /*bodyNode*/controlNode.createElement(NAMESPACE_XFORMS, null);
				itemNode.setName(NODE_ITEM);
				
				Element node = itemNode.createElement(NAMESPACE_XFORMS, null);
				node.setName(NODE_LABEL);
				node.addChild(Element.TEXT, getProviderName(provider, personId));
				itemNode.addChild(Element.ELEMENT, node);
				
				node = itemNode.createElement(NAMESPACE_XFORMS, null);
				node.setName(NODE_VALUE);
				node.addChild(Element.TEXT, personId.toString());
				itemNode.addChild(Element.ELEMENT, node);
				
				controlNode.addChild(Element.ELEMENT, itemNode);
			}
		}
		catch (Exception ex) {
			log.error("Failed to populate providers into the xform", ex);
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
			Element itemNode = /*bodyNode*/controlNode.createElement(NAMESPACE_XFORMS, null);
			itemNode.setName(NODE_ITEM);
			
			Element node = itemNode.createElement(NAMESPACE_XFORMS, null);
			node.setName(NODE_LABEL);
			node.addChild(Element.TEXT, getLocationName(loc));
			itemNode.addChild(Element.ELEMENT, node);
			
			node = itemNode.createElement(NAMESPACE_XFORMS, null);
			node.setName(NODE_VALUE);
			node.addChild(Element.TEXT, loc.getLocationId().toString());
			itemNode.addChild(Element.ELEMENT, node);
			
			controlNode.addChild(Element.ELEMENT, itemNode);
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
	private static Element createBindNode(Element modelElement, Element node, Hashtable bindings,
	                                      Hashtable<String, String> problemList, Hashtable<String, String> problemListItems) {
		Element bindNode = modelElement.createElement(NAMESPACE_XFORMS, null);
		bindNode.setName(NODE_BIND);
		String parentName = ((Element) node.getParent()).getName();
		String binding = node.getName();
		
		if (bindings.containsKey(binding)) {
			String oldBinding = binding;
			binding = parentName + "_" + binding;
			
			if (sharedRestrictions != null) {
				List<String> bindingList = sharedRestrictions.get(oldBinding);
				if (bindingList == null) {
					bindingList = new ArrayList<String>();
					sharedRestrictions.put(oldBinding, bindingList);
				}
				
				bindingList.add(binding);
			}
			
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
			if (problemListItems.contains(name)) {
				List<String> repeats = repeatSharedKids.get(name);
				if (repeats == null) {
					repeats = new ArrayList<String>();
					repeatSharedKids.put(name, repeats);
				}
				repeats.add(problemListItems.get(name));
			}
			problemListItems.put(name, parentName);
		}
		
		bindNode.setAttribute(null, ATTRIBUTE_NODESET, nodeset);
		
		if (!((Element) ((Element) node.getParent()).getParent()).getName().equals(NODE_PROBLEM_LIST))
			modelElement.addChild(Element.ELEMENT, bindNode);
		
		//store the binding node with the key being its id attribute.
		bindings.put(binding, bindNode);
		
		if (nodesets != null)
			nodesets.put(binding, nodeset);
		
		return bindNode;
	}
	
	/**
	 * Adds a node to hold the xforms value for a multiple select node. The value is a space
	 * delimited list of selected answers, which will later on be used to fill the true or false
	 * values as expected by openmrs multiple select questions.
	 * 
	 * @param child - the multiple select node to add the value node to.
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
	 * @param bindingNode - the binding node whose type attribute we are to set.
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
	 * @param bindingNode - the binding node whose required and readonly attributes we are to set.
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
		XformsService xformsService = (XformsService) Context.getService(XformsService.class);
		String val = (String) xformsService.getFieldDefaultValue(formId, name);
		if (val == null) {
			val = (String) xformsService.getFieldDefaultValue(formId, name.replace('_', ' '));
			if (val == null)
				return null;
		}
		
		if (val.indexOf("$!{") == -1)
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
	 * Parses the openmrs schema document and builds UI conrols from openmrs concepts.
	 * 
	 * @param rootNode - the schema document root node.
	 * @param bodyNode - the xform document body node.
	 * @param modelNode - the xform model node.
	 * @param xformSchemaNode - the root node of the xml schema data types.
	 * @param bindings - a hashtable of node bindings.
	 */
	private static void parseSchema(Element rootNode, Element bodyNode, Element modelNode, Element xformSchemaNode,
	                                Hashtable bindings, Hashtable<String, String> problemList,
	                                Hashtable<String, String> problemListItems) {
		Hashtable<String, Element> repeatControls = new Hashtable<String, Element>();
		Hashtable<String, List<String>> duplicateFields = new Hashtable<String, List<String>>();
		
		int numOfEntries = rootNode.getChildCount();
		for (int i = 0; i < numOfEntries; i++) {
			if (rootNode.isText(i))
				continue;
			
			Element child = rootNode.getElement(i);
			String name = child.getName();
			if (name.equalsIgnoreCase(NODE_COMPLEXTYPE) && isUserDefinedSchemaElement(child)) {
				parseComplexType(child, child.getAttributeValue(null, ATTRIBUTE_NAME), bodyNode, xformSchemaNode, bindings,
				    problemList, problemListItems, repeatControls, modelNode);
				
				parseDuplicateFieldsComplexType(duplicateFields, repeatControls, child, bodyNode, modelNode,
				    xformSchemaNode, bindings, problemList, problemListItems);
				
				parseSharedRepeatKidsComplexType(child.getAttributeValue(null, ATTRIBUTE_NAME), duplicateFields,
				    repeatControls, child, bodyNode, modelNode, xformSchemaNode, bindings, problemList, problemListItems);
			} else {
				String nameAttribute = child.getAttributeValue(null, ATTRIBUTE_NAME);
				if (name.equalsIgnoreCase(NODE_SIMPLETYPE)
				        || (name.equalsIgnoreCase(NODE_COMPLEXTYPE) && nameAttribute != null
				                && nameAttribute.startsWith("_") && !nameAttribute.contains("_section"))/*&& isUserDefinedSchemaElement(child)*/)
					xformSchemaNode.addChild(0, Element.ELEMENT, child);
				
				if ("obs_section".equalsIgnoreCase(nameAttribute))
					parseObsSectionRepeats(child, bindings, problemList, duplicateFields);
			}
			
			if (name.equalsIgnoreCase(NODE_SIMPLETYPE)) {
				parseSimpleType(child, child.getAttributeValue(null, ATTRIBUTE_NAME), bindings);
				parseDuplicateFieldsSimpleType(duplicateFields, child, bindings);
			}
		}
	}
	
	private static void parseDuplicateFieldsComplexType(Hashtable<String, List<String>> duplicateFields,
	                                                    Hashtable<String, Element> repeatControls, Element child,
	                                                    Element bodyNode, Element modelNode, Element xformSchemaNode,
	                                                    Hashtable bindings, Hashtable<String, String> problemList,
	                                                    Hashtable<String, String> problemListItems) {
		Set<Entry<String, List<String>>> entries = duplicateFields.entrySet();
		for (Entry<String, List<String>> entry : entries) {
			String key = child.getAttributeValue(null, ATTRIBUTE_NAME);
			if (!key.equals(entry.getKey()))
				continue;
			
			String suffix = "_type";
			int pos = key.indexOf(suffix);
			if (pos < 1) {
				suffix = "_section";
				pos = key.indexOf(suffix);
			}
			
			String firstField = key.substring(0, pos);
			List<String> fields = entry.getValue();
			for (String field : fields) {
				if (field.equals(firstField))
					continue;
				
				parseComplexType(child, field + suffix, bodyNode, xformSchemaNode, bindings, problemList, problemListItems,
				    repeatControls, modelNode);
			}
		}
	}
	
	private static void parseSharedRepeatKidsComplexType(String name, Hashtable<String, List<String>> duplicateFields,
	                                                     Hashtable<String, Element> repeatControls, Element child,
	                                                     Element bodyNode, Element modelNode, Element xformSchemaNode,
	                                                     Hashtable bindings, Hashtable<String, String> problemList,
	                                                     Hashtable<String, String> problemListItems) {
		String orgName = name;
		name = getBindNodeName(name);
		List<String> repeats = repeatSharedKids.get(name);
		if (repeats == null)
			return;
		
		for (String repeat : repeats) {
			problemListItems.put(name, repeat);
			parseComplexType(child, orgName, bodyNode, xformSchemaNode, bindings, problemList, problemListItems,
			    repeatControls, modelNode);
		}
	}
	
	private static void parseDuplicateFieldsSimpleType(Hashtable<String, List<String>> duplicateFields, Element child,
	                                                   Hashtable bindings) {
		Set<Entry<String, List<String>>> entries = duplicateFields.entrySet();
		for (Entry<String, List<String>> entry : entries) {
			String key = child.getAttributeValue(null, ATTRIBUTE_NAME);
			if (!key.equals(entry.getKey() + "_restricted_type"))
				continue;
			
			String firstField = key.substring(0, key.indexOf("_type"));
			List<String> fields = entry.getValue();
			for (String field : fields) {
				if (field.equals(firstField))
					continue;
				
				parseSimpleType(child, field + "_type", bindings);
			}
		}
	}
	
	private static void parseObsSectionRepeats(Element complexTypeNode, Hashtable bindings,
	                                           Hashtable<String, String> problemList,
	                                           Hashtable<String, List<String>> duplicateFields) {
		for (int i = 0; i < complexTypeNode.getChildCount(); i++) {
			if (complexTypeNode.isText(i))
				continue; //ignore text.
				
			Element child = (Element) complexTypeNode.getElement(i);
			if (child.getName().equalsIgnoreCase(NODE_SEQUENCE)) {
				parseObsSectionSequenceRepeats(child, bindings, problemList, duplicateFields);
				return;
			}
		}
	}
	
	private static void parseObsSectionSequenceRepeats(Element sequenceNode, Hashtable bindings,
	                                                   Hashtable<String, String> problemList,
	                                                   Hashtable<String, List<String>> duplicateFields) {
		
		Hashtable<String, List<String>> fieldMap = new Hashtable<String, List<String>>();
		
		for (int i = 0; i < sequenceNode.getChildCount(); i++) {
			if (sequenceNode.isText(i))
				continue; //ignore text.
				
			Element child = (Element) sequenceNode.getElement(i);
			String name = child.getAttributeValue(null, ATTRIBUTE_NAME);
			if ("unbounded".equalsIgnoreCase(child.getAttributeValue(null, ATTRIBUTE_MAXOCCURS))) {
				problemList.put(name, name);
				
				/*obsRepeatItems.put(name, name);

				String nodeset = nodesets.get(name);
				Element bindNode = (Element)bindings.get(name);
				if(nodeset != null && bindNode != null){
					if(nodeset.endsWith("/value"))
						bindNode.setAttribute(null, ATTRIBUTE_NODESET, nodeset.substring(0, nodeset.length() - 6));
				}*/
			}
			
			String type = child.getAttributeValue(null, ATTRIBUTE_TYPE);
			List<String> fields = fieldMap.get(type);
			if (fields == null) {
				fields = new ArrayList<String>();
				fields.add(name);
				fieldMap.put(type, fields);
			} else {
				fields.add(name);
				duplicateFields.put(type, fields);
			}
		}
	}
	
	private static void parseSimpleType(Element simpleTypeNode, String name, Hashtable bindings) {
		
		if (name == null || name.trim().length() == 0)
			return;
		
		name = getBindNodeName(name);
		
		if (name == null || name.trim().length() == 0)
			return;
		
		for (int i = 0; i < simpleTypeNode.getChildCount(); i++) {
			if (simpleTypeNode.isText(i))
				continue; //ignore text.
				
			Element child = (Element) simpleTypeNode.getElement(i);
			if (child.getName().equalsIgnoreCase(NODE_RESTRICTION))
				parseRestriction(child, name, bindings);
		}
	}
	
	private static void parseRestriction(Element restrictionNode, String name, Hashtable bindings) {
		Element bindNode = (Element) bindings.get(name);
		if (bindNode != null) {
			String type = restrictionNode.getAttributeValue(null, ATTRIBUTE_BASE);
			if ("xs:int".equalsIgnoreCase(type) || "xs:float".equalsIgnoreCase(type))
				addValidationRuleRanges(name, bindings, bindNode, restrictionNode);
		}
	}
	
	/**
	 * Parses a complex type node in an openmrs schema document.
	 * 
	 * @param complexTypeNode - the complex type node.
	 * @param name - the name of the complex type node.
	 * @param bodyNode - the xform body node.
	 * @param xformSchemaNode - the top node of the xml schema data types.
	 * @param bindings - a hashtable of node bindings.
	 */
	private static void parseComplexType(Element complexTypeNode, String name, Element bodyNode, Element xformSchemaNode,
	                                     Hashtable bindings, Hashtable<String, String> problemList,
	                                     Hashtable<String, String> problemListItems,
	                                     Hashtable<String, Element> repeatControls, Element modelNode) {
		
		if (name.equals("patient_relationship_section") || name.equals("relative_section"))
			return;
		
		String orgName = name;
		name = getBindNodeName(name);
		if (name == null)
			return;
		
		Element labelNode = null, bindNode = (Element) bindings.get(name);
		if (bindNode == null) {
			//could be a section like problem_list_section
			if (name.equals("problem_list"))
				;//addProblemListItems(name, complexTypeNode, bodyNode, modelNode);
				
			String binding = repeatChildTypes.get(orgName);
			if (binding != null) {
				name = binding;
				bindNode = (Element) bindings.get(binding);
			} else
				return;
		}
		
		boolean repeatItem = false;
		Element lblNode = null;
		String nameAttributeValue = complexTypeNode.getAttributeValue(null, "name");
		/*if(nameAttributeValue != null && nameAttributeValue.endsWith("_section")){
			problemList.put(name, name);*/
		if (problemList.contains(name)) {
			lblNode = addProblemListSection(name, bodyNode, repeatControls, modelNode);
			repeatItem = true;
		}
		
		for (int i = 0; i < complexTypeNode.getChildCount(); i++) {
			if (complexTypeNode.isText(i))
				continue; //ignore text.
				
			Element node = (Element) complexTypeNode.getChild(i);
			if (node.getName().equalsIgnoreCase(NODE_SEQUENCE))
				labelNode = parseSequenceNode(name, node, bodyNode, xformSchemaNode, bindNode, problemList,
				    problemListItems, repeatControls, repeatItem, modelNode);
			
			if (repeatItem)
				labelNode = lblNode;
			
			if (labelNode != null && isNodeWithConceptNameAndId(node))
				addLabelTextAndHint(labelNode, node);
			else if (isNodeWithDataType(node)) {
				setDataType(bindNode, node);
				
				List<String> bindingList = sharedRestrictions.get(name);
				if (bindingList != null) {
					for (String binding : bindingList) {
						Element bindingNode = (Element) bindings.get(binding);
						if (bindingNode == null)
							continue;
						
						setDataType(bindingNode, node);
					}
				}
			}
		}
	}
	
	private static void addProblemListItems(String name, Element complexTypeNode, Element bodyNode, Element modelNode) {
		for (int i = 0; i < complexTypeNode.getChildCount(); i++) {
			if (complexTypeNode.isText(i))
				continue; //ignore text.
				
			Element node = (Element) complexTypeNode.getChild(i);
			if (node.getName().equalsIgnoreCase(NODE_SEQUENCE)) {
				for (int j = 0; j < node.getChildCount(); j++) {
					if (node.isText(j))
						continue; //ignore text.
						
					Element nd = (Element) node.getChild(j);
					String itemName = nd.getAttributeValue(null, ATTRIBUTE_NAME);
					String type = nd.getAttributeValue(null, ATTRIBUTE_TYPE);
					type = type.substring(0, type.length() - 5);
					if (type.equals(itemName))
						continue; //eg medication_orders and medication_orders_type. But if medication_orders_type is missing, then we shall never have a ui for medication_orders
						
					addProblemListItem(itemName, bodyNode);
				}
			}
		}
	}
	
	private static void addProblemListItem(String name, Element bodyNode) {
		String nodeset = "problem_list/" + name + "/value";
		String id = nodeset.replace('/', '_');
		
		Element select1Node = bodyNode.createElement(NAMESPACE_XFORMS, null);
		select1Node.setName(CONTROL_SELECT1);
		//select1Node.setAttribute(null, ATTRIBUTE_REF, nodeset);
		select1Node.setAttribute(null, ATTRIBUTE_BIND, id);
		bodyNode.addChild(Element.ELEMENT, select1Node);
		
		Element labelNode = select1Node.createElement(NAMESPACE_XFORMS, null);
		labelNode.setName(NODE_LABEL);
		labelNode.addChild(Element.TEXT, name);
		select1Node.addChild(Element.ELEMENT, labelNode);
		
		Element itemLabelNode = select1Node.createElement(NAMESPACE_XFORMS, null);
		itemLabelNode.setName(NODE_LABEL);
		itemLabelNode.addChild(Element.TEXT, name + " value");
		
		Element itemValNode = select1Node.createElement(NAMESPACE_XFORMS, null);
		itemValNode.setName(NODE_VALUE);
		itemValNode.addChild(Element.TEXT, "value");
		
		Element itemNode = select1Node.createElement(NAMESPACE_XFORMS, null);
		itemNode.setName(NODE_ITEM);
		itemNode.addChild(Element.ELEMENT, itemLabelNode);
		itemNode.addChild(Element.ELEMENT, itemValNode);
		
		select1Node.addChild(Element.ELEMENT, itemNode);
		
		//create bind node
		Element bindNode = select1Node.createElement(NAMESPACE_XFORMS, null);
		bindNode.setName(NODE_BIND);
		bindNode.setAttribute(null, ATTRIBUTE_ID, id);
		bindNode.setAttribute(null, ATTRIBUTE_NODESET, "/form/" + nodeset);
		bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
		select1Node.addChild(Element.ELEMENT, bindNode);
	}
	
	/**
	 * Checks if the xforms data type is set to any value other than text.
	 * 
	 * @param bindNode the xforms bind node
	 * @return
	 */
	private static boolean isDataTypeSetPrecisely(Element bindNode) {
		String type = bindNode.getAttributeValue(null, ATTRIBUTE_TYPE);
		if (type != null && !type.equalsIgnoreCase(DATA_TYPE_TEXT))
			return true;
		return false;
	}
	
	/**
	 * Sets the xforms data type from an openmrs data type
	 * 
	 * @param bindNode the bind xforms node whose data type to set.
	 * @param node the schema node having the openmrs data type
	 */
	public static void setDataType(Element bindNode, Element node) {
		
		//Some types may have been already set to the precise value
		//and hence should not be overwritten. eg NM could have been
		//set to either int or decimal.
		if (isDataTypeSetPrecisely(bindNode))
			return;
		
		String fixed = node.getAttributeValue(null, ATTRIBUTE_FIXED);
		if ("ED".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_BASE64BINARY);
		else if ("BIT".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_BOOLEAN);
		else if ("TS".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_DATETIME);
		else if ("TM".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TIME);
		else if ("DT".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_DATE);
		else if ("NM".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_DECIMAL);
		else if ("RP".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_BASE64BINARY);
		else if ("ST".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
		else if ("CWE".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
		else if ("N/A".equalsIgnoreCase(fixed))
			bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
		
		//TODO What of ZZ(Rule) and SM(Structured Numeric)?
	}
	
	private static void removeChildNode(Element node, String name) {
		for (int index = 0; index < node.getChildCount(); index++) {
			if (node.getType(index) == Element.ELEMENT) {
				Element n = node.getElement(index);
				if (name.equals(n.getAttributeValue(null, ATTRIBUTE_ID))) {
					node.removeChild(index);
					return;
				}
			}
		}
	}
	
	/**
	 * Gets the binding of a complex or simple type node. An example of such a node would be:
	 * complexType name="weight_kg_type" or <xs:simpleType name="weight_kg_type_restricted_type">
	 * 
	 * @param name - the name of the complex type node.
	 * @param bindings - a hashtable of bingings.
	 * @return - the binding node.
	 */
	/*private static Element getBindNode(String name, Hashtable bindings){
		if(name == null)
			return null;

		//We are only dealing with names ending with _type. e.g. education_level_type
		//Openmrs appends the _type to the name when creating xml types for each concept
		if(name.indexOf(COMPLEX_TYPE_NAME_POSTFIX) != -1){
			//remove the _type part. e.g from above the name is education_level
			name = name.substring(0, name.length() - COMPLEX_TYPE_NAME_POSTFIX.length());
			Element bindNode = (Element)bindings.get(name);
			return bindNode;
		}
		else if(name.indexOf(SIMPLE_TYPE_NAME_POSTFIX) != -1){
			//Now dealing with things like weight_kg_type_restricted_type
			//remove the _type_restricted_type part. e.g from above the name is weight_kg
			name = name.substring(0, name.length() - SIMPLE_TYPE_NAME_POSTFIX.length());
			Element bindNode = (Element)bindings.get(name);
			return bindNode;
		}
		else
			return null;
	}*/

	/**
	 * Gets the binding node name of a complex or simple type node name. An example of such a node
	 * would be weight_kg for: complexType name="weight_kg_type" or <xs:simpleType
	 * name="weight_kg_type_restricted_type">
	 * 
	 * @param name - the name of the complex or simple type node.
	 * @return - the binding node name.
	 */
	public static String getBindNodeName(String name) {
		if (name == null)
			return null;
		
		//Now dealing with things like weight_kg_type_restricted_type
		//remove the _type_restricted_type part. e.g from above the name is weight_kg
		if (name.indexOf(SIMPLE_TYPE_NAME_POSTFIX) != -1)
			return name.substring(0, name.length() - SIMPLE_TYPE_NAME_POSTFIX.length());
		else if (name.contains("_restricted_type") && name.contains("_type_")) {
			//eg weight_kg_type_1_restricted_type  for duplicate weight_kg
			String s = name;
			name = s.substring(0, s.indexOf("_type_"));
			return name + s.substring(name.length() + 5, s.indexOf("_restricted_type"));
		} else if (name.contains("_type_")) {
			//we are looking for something like weight_kg_type_1
			int pos = name.indexOf("_type_");
			String suffix = name.substring(pos + 5);
			if (isNumeric(suffix.substring(1)))
				return name.substring(0, pos) + suffix;
		}
		
		//Now we are only dealing with names ending with _type. e.g. education_level_type
		//Openmrs appends the _type to the name when creating xml types for each concept
		//To handle complicated problem lists that have more than one item, we also handle
		//names ending with section.
		if (name.indexOf(COMPLEX_TYPE_NAME_POSTFIX) == -1) {
			if (name.indexOf(COMPLEX_SECTION_NAME_POSTFIX) == -1)
				return null;
			return name.substring(0, name.length() - COMPLEX_SECTION_NAME_POSTFIX.length());
		}
		
		//remove the _type part. e.g from above the name is education_level
		name = name.substring(0, name.length() - COMPLEX_TYPE_NAME_POSTFIX.length());
		return name;
	}
	
	private static boolean isNumeric(String value) {
		try {
			Integer.parseInt(value);
			return true;
		}
		catch (Exception ex) {}
		
		return false;
	}
	
	/**
	 * Adds text and hint to a label node.
	 * 
	 * @param labelNode - the label node.
	 * @param node - the node having the concept name and id.
	 */
	private static void addLabelTextAndHint(Element labelNode, Element node) {
		String fixedAttributeValue = node.getAttributeValue(null, ATTRIBUTE_FIXED);
		labelNode.addChild(Element.TEXT, getConceptName(fixedAttributeValue));
		
		Element parentNode = (Element) labelNode.getParent();
		if (parentNode.getName().contains(XformBuilder.CONTROL_SELECT))
			parentNode.setAttribute(null, XformBuilder.ATTRIBUTE_CONCEPT_ID, getConceptId(fixedAttributeValue).toString());
		
		String hint = getConceptDescription(node);
		if (hint != null && hint.length() > 0) {
			Element hintNode = /*bodyNode*/labelNode.createElement(NAMESPACE_XFORMS, null);
			hintNode.setName(NODE_HINT);
			hintNode.addChild(Element.TEXT, getConceptDescription(node));
			labelNode.getParent().addChild(1, Element.ELEMENT, hintNode);
		}
	}
	
	/**
	 * Checks if this node has the concept name and id. An example of such a node would be as:
	 * <xs:attribute name="openmrs_concept" type="xs:string" use="required"
	 * fixed="5089^WEIGHT (KG)^99DCT" /> where the concept name and id combination we are refering
	 * to is: 5089^WEIGHT (KG)^99DCT
	 * 
	 * @param node - the node to check.
	 * @return true if so, else false.
	 */
	private static boolean isNodeWithConceptNameAndId(Element node) {
		return node.getName().equalsIgnoreCase(NODE_ATTRIBUTE) && node.getAttributeValue(null, ATTRIBUTE_NAME) != null
		        && node.getAttributeValue(null, ATTRIBUTE_NAME).equalsIgnoreCase(ATTRIBUTE_OPENMRS_CONCEPT);
	}
	
	/**
	 * Checks if this node has the openmrs data type. An example of such a node would be as:
	 * <xs:attribute name="openmrs_datatype" type="xs:string" use="required" fixed="NM" /> where the
	 * data type is pointed to by the fixed attribute
	 * 
	 * @param node - the node to check.
	 * @return true if so, else false.
	 */
	private static boolean isNodeWithDataType(Element node) {
		return node.getName().equalsIgnoreCase(NODE_ATTRIBUTE)
		        && ATTRIBUTE_OPENMRS_DATATYPE.equalsIgnoreCase(node.getAttributeValue(null, ATTRIBUTE_NAME));
	}
	
	/**
	 * Gets the concept id from a name and id combination.
	 * 
	 * @param conceptName - the concept name.
	 * @return - the id
	 */
	public static Integer getConceptId(String conceptName) {
		try {
			return Integer.parseInt(conceptName.substring(0, conceptName.indexOf("^")));
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Gets the description of a concept.
	 * 
	 * @param node - the node having the concept.
	 * @return - the concept description.
	 */
	private static String getConceptDescription(Element node) {
		try {
			String name = node.getAttributeValue(null, ATTRIBUTE_FIXED);
			Concept concept = Context.getConceptService().getConcept(getConceptId(name));
			ConceptName conceptName = concept.getName();
			return conceptName.getDescription();
		}
		catch (Exception ex) {
			//ex.printStackTrace();
		}
		return "";
	}
	
	/**
	 * Gets the name of a concept from the name and id combination value.
	 * 
	 * @param val - the name and id combination.
	 * @return - the cencept name.
	 */
	public static String getConceptName(String val) {
		val = val.substring(val.indexOf('^') + 1, val.lastIndexOf('^'));
		int pos = val.indexOf('^');
		if (pos > 0)
			val = val.substring(0, pos);
		return val;
	}
	
	/**
	 * Parses a sequence node from an openmrs schema document.
	 * 
	 * @param name
	 * @param sequenceNode
	 * @param bodyNode
	 * @param xformSchemaNode
	 * @param bindingNode
	 * @return the created label node.
	 */
	private static Element parseSequenceNode(String name, Element sequenceNode, Element bodyNode, Element xformSchemaNode,
	                                         Element bindingNode, Hashtable<String, String> problemList,
	                                         Hashtable<String, String> problemListItems,
	                                         Hashtable<String, Element> repeatControls, boolean repeatItem, Element modelNode) {
		Element labelNode = null, controlNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
		;
		
		for (int i = 0; i < sequenceNode.getChildCount(); i++) {
			if (sequenceNode.isText(i))
				continue; //ignore text.
				
			Element node = (Element) sequenceNode.getChild(i);
			String itemName = node.getAttributeValue(null, ATTRIBUTE_NAME);
			//???????
			if (repeatItem) {
				if (problemListItems.containsKey(itemName)) {
					List<String> repeats = repeatSharedKids.get(itemName);
					if (repeats == null) {
						repeats = new ArrayList<String>();
						repeatSharedKids.put(itemName, repeats);
					}
					repeats.add(problemListItems.get(itemName));
				}
				problemListItems.put(itemName, name);
				//removeChildNode(modelNode,itemName);
			}
			
			//Instead of the value node, multiple select questions have one node
			//for each possible select option.
			if (!itemName.equalsIgnoreCase(NODE_VALUE)) {
				//Assuming sections (those that end with _section) don't have this attribute.
				String type = node.getAttributeValue(null, "type"/*"minOccurs"*/);
				if (type != null && !repeatItem) {
					if (problemList.containsKey(name))
						//if(problemListItems.containsKey(name))
						return addProblemListSection(name, bodyNode, repeatControls, modelNode);
					else
						continue;
				}
				
				if (repeatItem) {
					String binding = name + "_" + itemName;
					if (name.equals(problemListItems.get(binding))) {
						repeatChildTypes.put(type, binding);
					}
				}
				
				//if(!(itemName.equalsIgnoreCase(NODE_DATE) || itemName.equalsIgnoreCase(NODE_TIME)) && node.getAttributeValue(null, ATTRIBUTE_OPENMRS_CONCEPT) == null)
				if (!itemName.equalsIgnoreCase(NODE_DATE) && !itemName.equalsIgnoreCase(NODE_TIME)
				        && node.getChildCount() > 0 /*&& node.getAttributeValue(null, ATTRIBUTE_OPENMRS_CONCEPT) == null*/)
					labelNode = parseMultiSelectNode(name, itemName, node, controlNode, bodyNode, labelNode, bindingNode,
					    problemList, problemListItems, repeatControls, modelNode);
				//else if(name.equalsIgnoreCase(COMPLEX_TYPE_NAME_PROBLEM_LIST))
				//	problemList.put(name, name);
				
				continue;
			}
			
			if (node.getAttributeValue(null, ATTRIBUTE_NILLABLE).equalsIgnoreCase("0"))
				bindingNode.setAttribute(null, ATTRIBUTE_REQUIRED, XPATH_VALUE_TRUE);
			
			//We are interested in the element whose name attribute is equal to value, 
			//for single select lists.
			labelNode = parseSequenceValueNode(name, node, labelNode, bodyNode, bindingNode, problemList, problemListItems,
			    repeatControls, modelNode);
		}
		
		return labelNode;
	}
	
	/**
	 * Creates a repeat control for problem lists with sections.
	 * 
	 * @param name
	 * @param bodyNode
	 * @param repeatControls
	 * @return the label node for the control.
	 */
	private static Element addProblemListSection(String name, Element bodyNode, Hashtable<String, Element> repeatControls,
	                                             Element modelNode) {
		Element groupNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
		groupNode.setName(NODE_GROUP);
		Element labelNode = groupNode.createElement(NAMESPACE_XFORMS, null);
		labelNode.setName(NODE_LABEL);
		groupNode.addChild(Element.ELEMENT, labelNode);
		bodyNode.addChild(Element.ELEMENT, groupNode);
		
		Element repeatControl = buildRepeatControl(groupNode, null, name, modelNode);
		repeatControls.put(name, repeatControl);
		
		//repeatControl.addChild(Element.ELEMENT, labelNode);
		addControl(groupNode, repeatControl);
		
		return labelNode;
	}
	
	/**
	 * Builds an xform input type control from a sequence value node.
	 * 
	 * @param name - the name of the complex type node we are dealing with.
	 * @param node - the value node.
	 * @param type - the type attribute value.
	 * @param labelNode - the label node.
	 * @param bindingNode - the binding node.
	 * @param bodyNode - the body node.
	 * @return returns the created label node.
	 */
	private static Element buildSequenceInputControlNode(String name, Element node, String type, Element labelNode,
	                                                     Element bindingNode, Element bodyNode,
	                                                     Hashtable<String, String> problemList,
	                                                     Hashtable<String, String> problemListItems,
	                                                     Hashtable<String, Element> repeatControls, Element modelNode) {
		type = getPrefixedDataType(type);
		
		if (!isDataTypeSetPrecisely(bindingNode))
			bindingNode.setAttribute(null, ATTRIBUTE_TYPE, type);
		
		Element inputNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
		inputNode.setName(CONTROL_INPUT);
		//inputNode.setAttribute(null, ATTRIBUTE_BIND, name);
		
		labelNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
		labelNode.setName(NODE_LABEL);
		inputNode.addChild(Element.ELEMENT, labelNode);
		
		addRepeatControlNode(name, inputNode, bodyNode, problemList, problemListItems, repeatControls, NODE_VALUE,
		    modelNode, bindingNode);
		
		return labelNode;
	}
	
	private static void addRepeatControlNode(String name, Element controlNode, Element bodyNode,
	                                         Hashtable<String, String> problemList,
	                                         Hashtable<String, String> problemListItems,
	                                         Hashtable<String, Element> repeatControls, String valueNodeName,
	                                         Element modelNode, Element bindingNode) {
		if (problemList.containsKey(name))
			controlNode.setAttribute(null, ATTRIBUTE_REF, valueNodeName);
		else if (problemListItems.containsKey(name))
			controlNode.setAttribute(null, ATTRIBUTE_BIND, name); //controlNode.setAttribute(null, ATTRIBUTE_REF, name+"/"+valueNodeName);
		else
			controlNode.setAttribute(null, ATTRIBUTE_BIND, name);
		
		if (problemList.contains(name))
			;//addControl(bodyNode,buildRepeatControl(bodyNode,controlNode,name, modelNode));
		else if (problemListItems.containsKey(name)) {
			String repeatControlName = problemListItems.get(name);
			String nodeset = bindingNode.getAttributeValue(null, XformBuilder.ATTRIBUTE_NODESET);
			if (nodeset != null && nodeset.contains(repeatControlName)) {
				Element repeatControl = repeatControls.get(repeatControlName);
				if (repeatControl != null) {
					repeatControl.addChild(Element.ELEMENT, controlNode);
					
					String binding = repeatControlName + "_" + name;
					String repeatBinding = problemListItems.get(binding);
					if (repeatBinding != null && repeatBinding.equals(problemListItems.get(name))) {
						controlNode.setAttribute(null, ATTRIBUTE_BIND, binding);
					}
				}
			} else
				addControl(bodyNode, controlNode);
		} else
			addControl(bodyNode, controlNode);
	}
	
	/**
	 * Parses a sequence value node and builds the corresponding xforms UI control.
	 * 
	 * @param name - the name of the complex type node whose sequence we are parsing.
	 * @param node - the sequence value node.
	 * @param labelNode - the label node to build.
	 * @param bodyNode - the body node.
	 * @param bindingNode - the binding node.
	 * @return the created label node.
	 */
	private static Element parseSequenceValueNode(String name, Element node, Element labelNode, Element bodyNode,
	                                              Element bindingNode, Hashtable<String, String> problemList,
	                                              Hashtable<String, String> problemListItems,
	                                              Hashtable<String, Element> repeatControls, Element modelNode) {
		String type = node.getAttributeValue(null, ATTRIBUTE_TYPE);
		
		if (type != null)
			labelNode = buildSequenceInputControlNode(name, node, type, labelNode, bindingNode, bodyNode, problemList,
			    problemListItems, repeatControls, modelNode);
		else {
			//This is a select1 or select control which don't have the type attribute.
			for (int j = 0; j < node.getChildCount(); j++) {
				if (node.isText(j))
					continue;
				
				Element simpleTypeNode = (Element) node.getChild(j);
				if (!simpleTypeNode.getName().equalsIgnoreCase(NODE_SIMPLETYPE))
					continue;
				
				return parseSimpleType(name, simpleTypeNode, bodyNode, bindingNode, problemList, problemListItems,
				    repeatControls, modelNode);
			}
		}
		return labelNode;
	}
	
	/**
	 * Checks if the data type has a namespace prefix, if it does not, it prepends it.
	 * 
	 * @param type - the data type.
	 */
	private static String getPrefixedDataType(String type) {
		if (type == null)
			return null;
		
		if (type.indexOf(NAMESPACE_PREFIX_SEPARATOR) == -1)
			type = PREFIX_OPENMRS_TYPE + NAMESPACE_PREFIX_SEPARATOR + type;
		else
			type = PREFIX_XML_SCHEMA + NAMESPACE_PREFIX_SEPARATOR
			        + type.substring(type.indexOf(NAMESPACE_PREFIX_SEPARATOR) + 1);
		//type = type.substring(type.indexOf(NAMESPACE_PREFIX_SEPARATOR)+1);
		
		if (type.contains("openmrs"))
			type = "xsd:string";
		
		if (type.contains("float"))
			type = DATA_TYPE_DECIMAL;
		else if (type.contains("int"))
			type = DATA_TYPE_INT;
		
		return type;
	}
	
	/**
	 * Parses a simple type node in an openmrs schema document to build select1 and select XForms
	 * items from xs:enumeration s.
	 * 
	 * @param name
	 * @param simpleTypeNode
	 * @param bodyNode
	 * @param bindingNode
	 * @return the xforms label node.
	 */
	private static Element parseSimpleType(String name, Element simpleTypeNode, Element bodyNode, Element bindingNode,
	                                       Hashtable<String, String> problemList,
	                                       Hashtable<String, String> problemListItems,
	                                       Hashtable<String, Element> repeatControls, Element modelNode) {
		for (int i = 0; i < simpleTypeNode.getChildCount(); i++) {
			if (simpleTypeNode.isText(i))
				continue; //ignore text.
				
			Element child = (Element) simpleTypeNode.getElement(i);
			if (child.getName().equalsIgnoreCase(NODE_RESTRICTION))
				return parseRestriction(name, (Element) simpleTypeNode.getParent(), child, bodyNode, bindingNode,
				    problemList, problemListItems, repeatControls, modelNode);
		}
		
		return null;
	}
	
	/**
	 * Gets the node having the concept name and id combination for a multiple select item node.
	 * Such a node would look like: xs:attribute name="openmrs_concept" type="xs:string"
	 * use="required" fixed="215^JAUNDICE^99DCT"
	 * 
	 * @param node - the multiple select item node.
	 * @return the concept node.
	 */
	private static Element getMultiSelectItemConceptNode(Element node) {
		Element retNode;
		for (int i = 0; i < node.getChildCount(); i++) {
			if (node.isText(i))
				continue; //ignore text.
				
			Element child = (Element) node.getChild(i);
			if (child.getName().equalsIgnoreCase(NODE_ATTRIBUTE))
				return child;
			
			retNode = getMultiSelectItemConceptNode(child);
			if (retNode != null)
				return retNode;
		}
		
		return null;
	}
	
	/**
	 * Parses a multi select node and builds its corresponding items. An example of such a node
	 * would be: xs:element name="jaundice" default="false" nillable="true" for a complex type whose
	 * name is eye_exam_findings_type
	 * 
	 * @param name - the name of the complex type node we are dealing with.
	 * @param itemName - the name attribute of the node we are dealing with.
	 * @param selectItemNode - the multiple select item node.
	 * @param controlNode - the xform UI control
	 * @param bodyNode - the body node.
	 * @param labelNode - the label node.
	 * @param bindingNode - the binding node.
	 * @return the label node we have created.
	 */
	private static Element parseMultiSelectNode(String name, String itemName, Element selectItemNode, Element controlNode,
	                                            Element bodyNode, Element labelNode, Element bindingNode,
	                                            Hashtable<String, String> problemList,
	                                            Hashtable<String, String> problemListItems,
	                                            Hashtable<String, Element> repeatControls, Element modelNode) {
		//If this is the first time we are looping through, create the input control.
		//Otherwise just add the items one by one as we get called for each.
		if (controlNode.getChildCount() == 0) {
			//controlNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
			controlNode.setName(CONTROL_SELECT);
			
			//For repeat kids, we set ref instead of bind
			if (!(problemList.containsKey(name) || problemListItems.containsKey(name)))
				controlNode.setAttribute(null, ATTRIBUTE_BIND, name);
			
			controlNode.setAttribute(null, ATTRIBUTE_APPEARANCE,
			    Context.getAdministrationService().getGlobalProperty("xforms.multiSelectAppearance"));
			
			labelNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
			labelNode.setName(NODE_LABEL);
			controlNode.addChild(Element.ELEMENT, labelNode);
			
			//addControl(bodyNode,controlNode); //bodyNode.addChild(Element.ELEMENT, controlNode);
			addRepeatControlNode(name, controlNode, bodyNode, problemList, problemListItems, repeatControls,
			    NODE_XFORMS_VALUE, modelNode, bindingNode);
			
			bindingNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
		}
		
		buildMultipleSelectItemNode(itemName, selectItemNode, controlNode);
		
		return labelNode;
		
	}
	
	/**
	 * Builds a multiple select item node.
	 * 
	 * @param itemName - the name attribute of the multiple select item whose item node we are
	 *            building.
	 * @param selectItemNode - the xml schema select item node.
	 * @param controlNode - the xforms control whose item we are building.
	 */
	private static void buildMultipleSelectItemNode(String itemName, Element selectItemNode, Element controlNode) {
		Element node = getMultiSelectItemConceptNode(selectItemNode);
		String value = node.getAttributeValue(null, ATTRIBUTE_FIXED);
		String label = getConceptName(value);
		
		Element itemLabelNode = /*bodyNode*/controlNode.createElement(NAMESPACE_XFORMS, null);
		itemLabelNode.setName(NODE_LABEL);
		itemLabelNode.addChild(Element.TEXT, label);
		
		Element itemValNode = /*bodyNode*/controlNode.createElement(NAMESPACE_XFORMS, null);
		itemValNode.setName(NODE_VALUE);
		itemValNode.addChild(Element.TEXT, itemName /*value*//*binding*/);
		
		Element itemNode = /*bodyNode*/controlNode.createElement(NAMESPACE_XFORMS, null);
		itemNode.setName(NODE_ITEM);
		itemNode.setAttribute(null, ATTRIBUTE_CONCEPT_ID, getConceptId(value).toString());
		itemNode.addChild(Element.ELEMENT, itemLabelNode);
		itemNode.addChild(Element.ELEMENT, itemValNode);
		
		controlNode.addChild(Element.ELEMENT, itemNode);
	}
	
	/**
	 * Adds a UI control to the document body.
	 * 
	 * @param bodyNode - the body node.
	 * @param controlNode - the UI control.
	 */
	public static void addControl(Element bodyNode, Element controlNode) {
		bodyNode.addChild(Element.ELEMENT, controlNode);
	}
	
	/**
	 * Parses a restriction which has the enumeration nodes. Such a node would look like:
	 * xs:restriction base="xs:string" It also sets the data type which is normally the base
	 * attribute.
	 * 
	 * @param name - the name of the complex type question node whose restriction we are parsing.
	 * @param valueNode - the node who name attribute is equal to value.
	 * @param restrictionNode - the restriction node.
	 * @param bodyNode - the xform body node.
	 * @param bindingNode - the binding node.
	 * @return the label node of the created control.
	 */
	private static Element parseRestriction(String name, Element valueNode, Element restrictionNode, Element bodyNode,
	                                        Element bindingNode, Hashtable<String, String> problemList,
	                                        Hashtable<String, String> problemListItems,
	                                        Hashtable<String, Element> repeatControls, Element modelNode) {
		
		//the base attribute of a restriction has the data type for this question.
		String type = restrictionNode.getAttributeValue(null, ATTRIBUTE_BASE);
		type = getPrefixedDataType(type);
		bindingNode.setAttribute(null, ATTRIBUTE_TYPE, type);
		
		String controlName = CONTROL_SELECT;
		String maxOccurs = valueNode.getAttributeValue(null, ATTRIBUTE_MAXOCCURS);
		if (maxOccurs != null && maxOccurs.equalsIgnoreCase("1"))
			controlName = CONTROL_SELECT1;
		if (!hasRestrictions(restrictionNode))
			controlName = CONTROL_INPUT;
		
		Element controlNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
		controlNode.setName(controlName);
		
		String valueNodeName = NODE_VALUE;
		if (controlName.equals(CONTROL_SELECT))
			valueNodeName = NODE_XFORMS_VALUE;
		
		if (!controlName.equalsIgnoreCase(CONTROL_INPUT))
			controlNode.setAttribute(null, ATTRIBUTE_APPEARANCE,
			    Context.getAdministrationService()
			            .getGlobalProperty(XformConstants.GLOBAL_PROP_KEY_SINGLE_SELECT_APPEARANCE));
		
		addRepeatControlNode(name, controlNode, bodyNode, problemList, problemListItems, repeatControls, valueNodeName,
		    modelNode, bindingNode);
		
		Element labelNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
		labelNode.setName(NODE_LABEL);
		controlNode.addChild(Element.ELEMENT, labelNode);
		
		addRestrictionEnumerations(restrictionNode, controlNode);
		
		return labelNode;
	}
	
	/**
	 * Checks if a restriction node has enumeration restrictions.
	 * 
	 * @param restrictionNode the restriction node.
	 * @return true if it has, else false;
	 */
	private static boolean hasRestrictions(Element restrictionNode) {
		for (int i = 0; i < restrictionNode.getChildCount(); i++) {
			if (restrictionNode.isText(i))
				continue;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Adds validation constraints to the xfrom as specified from the openmrs schema. For now these
	 * are for openmrs numeric concepts (int and float)
	 * 
	 * @param bindingNode the xforms node to contain the constraint
	 * @param restrictionNode the openmrs schema node having the allowed range values
	 */
	private static void addValidationRuleRanges(String name, Hashtable bindings, Element bindingNode, Element restrictionNode) {
		
		String lower = null, upper = null;
		
		for (int i = 0; i < restrictionNode.getChildCount(); i++) {
			if (restrictionNode.isText(i))
				continue; //ignore text.
				
			Element child = (Element) restrictionNode.getChild(i);
			if (child.getName().equalsIgnoreCase(NODE_MININCLUSIVE))
				lower = child.getAttributeValue(null, ATTRIBUTE_VALUE);
			else if (child.getName().equalsIgnoreCase(NODE_MAXINCLUSIVE))
				upper = child.getAttributeValue(null, ATTRIBUTE_VALUE);
		}
		
		if (upper != null && lower != null && upper.trim().length() > 0 && lower.trim().length() > 0) {
			setValidationRule(bindingNode, upper, lower);
			
			List<String> bindingList = sharedRestrictions.get(name);
			if (bindingList != null) {
				for (String binding : bindingList) {
					Element bindNode = (Element) bindings.get(binding);
					if (bindNode == null)
						continue;
					
					setValidationRule(bindNode, upper, lower);
				}
			}
		}
	}
	
	private static void setValidationRule(Element bindingNode, String upper, String lower) {
		bindingNode.setAttribute(null, ATTRIBUTE_CONSTRAINT, ". >= " + lower + " and . <= " + upper);
		bindingNode.setAttribute(null, (XformsUtil.isJavaRosaSaveFormat() ? "jr:constraintMsg" : ATTRIBUTE_MESSAGE),
		    "value should be between " + lower + " and " + upper + " inclusive");
	}
	
	/**
	 * Builds an XForms repeat control for problem list elemtnts.
	 * 
	 * @param bodyNode
	 * @param controlNode
	 * @param name
	 * @return
	 */
	private static Element buildRepeatControl(Element bodyNode, Element controlNode, String name, Element modelNode) {
		Element repeatControl = bodyNode.createElement(NAMESPACE_XFORMS, null);
		repeatControl.setName(CONTROL_REPEAT);
		repeatControl.setAttribute(null, ATTRIBUTE_BIND, name);
		
		if (controlNode != null)
			repeatControl.addChild(Element.ELEMENT, controlNode);
		else if (name.contains("problem_added") || name.contains("problem_resolved"))
			addDefaultProblemListChild(name, repeatControl, null, modelNode);
		else if (obsRepeatItems.contains(name))
			addDefaultProblemListChild(name, repeatControl, "value" /*nodesets.get(name)*/, modelNode);
		
		return repeatControl;
	}
	
	public static void addDefaultProblemListChild(String name, Element repeatControl, String nodeset, Element modelNode) {
		//add the input node.
		Element controlNode = repeatControl.createElement(NAMESPACE_XFORMS, null);
		controlNode.setName(CONTROL_INPUT);
		
		nodeset = (nodeset == null ? "problem_list/" + name + "/value" : nodeset);
		String id = nodeset.replace('/', '_');
		//controlNode.setAttribute(null, ATTRIBUTE_REF, nodeset);
		//controlNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
		controlNode.setAttribute(null, ATTRIBUTE_BIND, id);
		
		repeatControl.addChild(Element.ELEMENT, controlNode);
		
		//add the label.
		Element labelNode = controlNode.createElement(NAMESPACE_XFORMS, null);
		labelNode.setName(NODE_LABEL);
		labelNode.addChild(Element.TEXT, name + " value");
		controlNode.addChild(Element.ELEMENT, labelNode);
		
		//create bind node
		Element bindNode = controlNode.createElement(NAMESPACE_XFORMS, null);
		bindNode.setName(NODE_BIND);
		bindNode.setAttribute(null, ATTRIBUTE_ID, id);
		bindNode.setAttribute(null, ATTRIBUTE_NODESET, "/form/" + nodeset);
		bindNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
		modelNode.addChild(Element.ELEMENT, bindNode);
	}
	
	/**
	 * Adds enumerations items for a restriction node to an xform control. Such a node is a select
	 * or select1 kind, and the enumerations become the xform items.
	 * 
	 * @param restrictionNode - the restriction node.
	 * @param controlNode - the control node to add the enumerations to.
	 */
	private static void addRestrictionEnumerations(Element restrictionNode, Element controlNode) {
		Element itemValNode = null;
		Element itemLabelNode = null;
		String valueText = null;
		for (int i = 0; i < restrictionNode.getChildCount(); i++) {
			//element nodes have the values. e.g. <xs:enumeration value="1360^RE TREATMENT^99DCT" /> 
			if (restrictionNode.getType(i) == Element.ELEMENT) {
				Element child = restrictionNode.getElement(i);
				if (child.getName().equalsIgnoreCase(NODE_ENUMERATION)) {
					itemValNode = /*bodyNode*/controlNode.createElement(NAMESPACE_XFORMS, null);
					itemValNode.setName(NODE_VALUE);
					valueText = child.getAttributeValue(null, NODE_VALUE);
					itemValNode.addChild(Element.TEXT, valueText);
				}
			}
			
			//Comments have the labels. e.g. <!--  RE TREATMENT --> 
			if (restrictionNode.getType(i) == Element.COMMENT) {
				itemLabelNode = /*bodyNode*/controlNode.createElement(NAMESPACE_XFORMS, null);
				itemLabelNode.setName(NODE_LABEL);
				itemLabelNode.addChild(Element.TEXT, restrictionNode.getChild(i));
			}
			
			//Check if both the labal and value are set. First loop sets value and second label.
			if (itemLabelNode != null && itemValNode != null) {
				Element itemNode = controlNode.createElement(NAMESPACE_XFORMS, null);
				itemNode.setName(NODE_ITEM);
				itemNode.setAttribute(null, ATTRIBUTE_CONCEPT_ID, getConceptId(valueText).toString());
				controlNode.addChild(Element.ELEMENT, itemNode);
				
				itemNode.addChild(Element.ELEMENT, itemLabelNode);
				itemNode.addChild(Element.ELEMENT, itemValNode);
				itemLabelNode = null;
				itemValNode = null;
			}
		}
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
	public static String getNodePath(Element node) {
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
	public static String fromDoc2String(Document doc) throws Exception {
		
		KXmlSerializer serializer = new KXmlSerializer();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		
		try {
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			serializer.setOutput(dos, XformConstants.DEFAULT_CHARACTER_ENCODING);
			doc.write(serializer);
			serializer.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return new String(bos.toByteArray(), XformConstants.DEFAULT_CHARACTER_ENCODING);
		
		/*KXmlSerializer serializer = new KXmlSerializer();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);

		try{
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			serializer.setOutput(dos,null);
			doc.write(serializer);
			serializer.flush();
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}

		byte[] byteArr = bos.toByteArray();
		char[]charArray = new char[byteArr.length];
		for(int i=0; i<byteArr.length; i++)
			charArray[i] = (char)byteArr[i];

		return String.valueOf(charArray);*/
	}
	
	/**
	 * Gets a document from a stream reader.
	 * 
	 * @param reader - the reader.
	 * @return the document.
	 */
	public static Document getDocument(Reader reader) {
		Document doc = new Document();
		
		try {
			KXmlParser parser = new KXmlParser();
			parser.setInput(reader);
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
			
			doc.parse(parser);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return doc;
	}
	
	/**
	 * Sets the values of patient table fields in an xform. These are the ones with the attributes:
	 * openmrs_table and openmrs_attribute e.g. <patient_unique_number
	 * openmrs_table="PATIENT_IDENTIFIER" openmrs_attribute="IDENTIFIER" />
	 * 
	 * @param formId - the id of the form.
	 * @param parentNode - the root node of the xform.
	 * @param patientId - the patient id.
	 * @param xformsService - the xforms service.
	 */
	public static void setPatientTableFieldValues(Integer formId, Element parentNode, XformsService xformsService,
	                                              VelocityEngine velocityEngine, VelocityContext velocityContext)
	    throws Exception {
		int numOfEntries = parentNode.getChildCount();
		for (int i = 0; i < numOfEntries; i++) {
			if (parentNode.getType(i) != Element.ELEMENT)
				continue;
			
			Element child = (Element) parentNode.getChild(i);
			String tableName = child.getAttributeValue(null, ATTRIBUTE_OPENMRS_TABLE);
			String columnName = child.getAttributeValue(null, ATTRIBUTE_OPENMRS_ATTRIBUTE);
			/*if(tableName != null && columnName != null && isUserDefinedNode(child.getName())){
				String filterValue = getFieldDefaultValue(child.getName(), formId,false);
				Object value  = getPatientValue(xformsService,patientId,tableName,columnName,filterValue);
				if(value != null)
					setNodeValue(child, value.toString());
			}*/

			if (tableName != null && columnName != null) {
				String name = child.getName().toUpperCase();
				String value = xformsService.getFieldDefaultValue(formId, name);
				if (value == null && name.contains("_")) {
					name = name.replace('_', ' ');
					value = xformsService.getFieldDefaultValue(formId, name);
				}
				
				if (value != null && value.trim().length() > 0) {
					StringWriter w = new StringWriter();
					try {
						velocityEngine.evaluate(velocityContext, w, XformBuilder.class.getName(), value);
						value = w.toString();
						
						if (value != null && value.trim().length() > 0)
							setNodeValue(child, value.toString());
					}
					catch (Exception ex) {
						log.error("Failed to evaluate: " + value + " for field: " + name, ex);
					}
				}
			}
			
			setPatientTableFieldValues(formId, child, xformsService, velocityEngine, velocityContext);
		}
	}
	
	/**
	 * Gets the value of a patient database table field.
	 * 
	 * @param xformsService - the xforms service.
	 * @param patientId - the patient id
	 * @param tableName - the name of the table.
	 * @param columnName - the name of column in the table.
	 * @return - the value
	 */
	private static Object getPatientValue(XformsService xformsService, Integer patientId, String tableName,
	                                      String columnName, String filterValue) {
		
		Object value = null;
		
		try {
			value = xformsService.getPatientValue(patientId, tableName, columnName, filterValue);
		}
		catch (Exception e) {
			System.out.println("No column called: " + columnName + " in table: " + tableName);
		}
		
		return value;
	}
	
	/**
	 * Checks if a node is a user defined one. That is not one of the standard openmrs nodes.
	 * 
	 * @param name - the name of the node.
	 * @return - true if it is a user define one, else false.
	 */
	public static boolean isUserDefinedNode(String name) {
		return !(name.equalsIgnoreCase(NODE_ENCOUNTER_ENCOUNTER_DATETIME)
		        || name.equalsIgnoreCase(NODE_ENCOUNTER_LOCATION_ID) || name.equalsIgnoreCase(NODE_ENCOUNTER_PROVIDER_ID)
		        || name.equalsIgnoreCase(NODE_PATIENT_MIDDLE_NAME) || name.equalsIgnoreCase(NODE_PATIENT_GIVEN_NAME)
		        || name.equalsIgnoreCase(NODE_PATIENT_PATIENT_ID) || name.equalsIgnoreCase(NODE_PATIENT_FAMILY_NAME));
	}
	
	/**
	 * Checks if a schema node is a user defined one. User defines nodes are the ones whose names
	 * are not the standard openmrs names like form,_header_section,obs_section, etc.
	 * 
	 * @param node - the node to check.
	 * @return - true if it is a user defined one, else false.
	 */
	private static boolean isUserDefinedSchemaElement(Element node) {
		
		if (!(node.getName().equalsIgnoreCase(NODE_SIMPLETYPE) || node.getName().equalsIgnoreCase(NODE_COMPLEXTYPE)))
			return false;
		
		String name = node.getAttributeValue(null, ATTRIBUTE_NAME);
		
		return !(name.equalsIgnoreCase("form") || name.equalsIgnoreCase("_header_section")
		        || name.equalsIgnoreCase("_other_section") || name.equalsIgnoreCase("_requiredString")
		        || name.equalsIgnoreCase("_infopath_boolean") || name.equalsIgnoreCase("encounter_section")
		        || name.equalsIgnoreCase("obs_section") || name.equalsIgnoreCase("patient_section"));
	}
	
	/**
	 * Builds an xform for creating a new patient.
	 * 
	 * @param xformAction - the url to post the xform data to.
	 * @return - the xml of the new patient xform.
	 */
	public static String getNewPatientXform() throws Exception {
		obsRepeatItems = new Hashtable<String, String>();
		nodesets = new Hashtable<String, String>();
		repeatSharedKids = new Hashtable<String, List<String>>();
		sharedRestrictions = new Hashtable<String, List<String>>();
		repeatChildTypes = new Hashtable<String, String>();
		
		Document doc = new Document();
		doc.setEncoding(XformConstants.DEFAULT_CHARACTER_ENCODING);
		
		Element xformsNode = doc.createElement(NAMESPACE_XFORMS, null);
		xformsNode.setName(NODE_XFORMS);
		xformsNode.setPrefix(PREFIX_XFORMS, NAMESPACE_XFORMS);
		
		//if(XformsUtil.isJavaRosaSaveFormat())
		xformsNode.setPrefix("jr", "http://openrosa.org/javarosa");
		
		xformsNode.setPrefix(PREFIX_XML_SCHEMA, NAMESPACE_XML_SCHEMA);
		xformsNode.setPrefix(PREFIX_XML_SCHEMA2, NAMESPACE_XML_SCHEMA);
		xformsNode.setPrefix(PREFIX_XML_INSTANCES, NAMESPACE_XML_INSTANCE);
		doc.addChild(org.kxml2.kdom.Element.ELEMENT, xformsNode);
		
		Element modelNode = doc.createElement(NAMESPACE_XFORMS, null);
		modelNode.setName(NODE_MODEL);
		xformsNode.addChild(Element.ELEMENT, modelNode);
		
		Element instanceNode = doc.createElement(NAMESPACE_XFORMS, null);
		instanceNode.setName(NODE_INSTANCE);
		modelNode.addChild(Element.ELEMENT, instanceNode);
		
		Element formNode = doc.createElement(null, null);
		formNode.setName(NODE_PATIENT);
		formNode.setAttribute(null, ATTRIBUTE_NAME, "Patient");
		formNode.setAttribute(null, ATTRIBUTE_ID, String.valueOf(XformConstants.PATIENT_XFORM_FORM_ID));
		formNode.setAttribute(null, ATTRIBUTE_DESCRIPTION_TEMPLATE,
		    "${/patient/family_name}$ ${/patient/middle_name}$ ${/patient/given_name}$");
		
		instanceNode.addChild(Element.ELEMENT, formNode);
		
		//This will be the creator for patient record
		Element element = formNode.createElement(null, null);
		element.setName(XformConstants.NODE_ENTERER);
		formNode.addChild(Element.ELEMENT, element);
		
		Element groupNode = doc.createElement(NAMESPACE_XFORMS, null);
		groupNode.setName(NODE_GROUP);
		Element labelNode = doc.createElement(NAMESPACE_XFORMS, null);
		labelNode.setName(NODE_LABEL);
		labelNode.addChild(Element.TEXT, "Page1");
		groupNode.addChild(Element.ELEMENT, labelNode);
		xformsNode.addChild(Element.ELEMENT, groupNode);
		
		addPatientNode(formNode, modelNode, groupNode, NODE_FAMILY_NAME, DATA_TYPE_TEXT, "Family Name",
		    "The patient family name", true, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_MIDDLE_NAME, DATA_TYPE_TEXT, "Middle Name",
		    "The patient middle name", false, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_GIVEN_NAME, DATA_TYPE_TEXT, "Given Name",
		    "The patient given name", false, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_BIRTH_DATE, DATA_TYPE_DATE, "Birth Date",
		    "The patient birth date", false, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_BIRTH_DATE_ESTIMATED, DATA_TYPE_BOOLEAN, "Birth Date Estimated",
		    "Is the patient birth date estimated?", false, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_IDENTIFIER, DATA_TYPE_TEXT, "Identifier",
		    "The patient identifier", true, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_PATIENT_ID, DATA_TYPE_INT, "Patient ID", "The patient ID",
		    false, true, CONTROL_INPUT, null, null, false);
		
		addPatientNode(formNode, modelNode, groupNode, NODE_GENDER, DATA_TYPE_TEXT, "Gender", "The patient's sex", false,
		    false, CONTROL_SELECT1, new String[] { "Male", "Female" }, new String[] { "M", "F" }, true);
		
		addPatientNode(formNode, modelNode, groupNode, NODE_DEGREE, DATA_TYPE_TEXT, "Degree", "The patient name degree",
		    false, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_FAMILY_NAME2, DATA_TYPE_TEXT, "Family Name 2",
		    "The patient second family name", false, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_FAMILY_NAME_PREFIX, DATA_TYPE_TEXT, "Family Name Prefix",
		    "The patient family name prefix", false, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_FAMILY_NAME_SUFFIX, DATA_TYPE_TEXT, "Family Name Suffix",
		    "The patient family name suffix", false, false, CONTROL_INPUT, null, null, true);
		addPatientNode(formNode, modelNode, groupNode, NODE_PREFIX, DATA_TYPE_TEXT, "Prefix", "The patient name prefix",
		    false, false, CONTROL_INPUT, null, null, true);
		
		String[] items, itemValues;
		int i = 0;
		List<Location> locations = Context.getLocationService().getAllLocations(false);
		if (locations != null) {
			items = new String[locations.size()];
			itemValues = new String[locations.size()];
			for (Location loc : locations) {
				items[i] = loc.getName();
				itemValues[i++] = loc.getLocationId().toString();
			}
			addPatientNode(formNode, modelNode, groupNode, NODE_LOCATION_ID, DATA_TYPE_INT, "Location",
			    "The patient's location", true, false, CONTROL_SELECT1, items, itemValues, true);
		}
		
		List<PatientIdentifierType> identifierTypes = Context.getPatientService().getAllPatientIdentifierTypes();
		if (identifierTypes != null) {
			i = 0;
			items = new String[identifierTypes.size()];
			itemValues = new String[identifierTypes.size()];
			for (PatientIdentifierType identifierType : identifierTypes) {
				items[i] = identifierType.getName();
				itemValues[i++] = identifierType.getPatientIdentifierTypeId().toString();
			}
			addPatientNode(formNode, modelNode, groupNode, "patient_identifier_type_id", DATA_TYPE_INT, "Identifier Type",
			    "The patient's identifier type", true, false, CONTROL_SELECT1, items, itemValues, true);
		}
		
		addPersonAttributes(formNode, modelNode, groupNode);
		
		addPersonAddresses(formNode, modelNode, groupNode);
		
		addOtherPatientIdentifiers(formNode, modelNode, groupNode);
		
		addEncounterForm(doc, formNode, modelNode, groupNode);
		
		cleanUp();
		
		return XformBuilder.fromDoc2String(doc);
	}
	
	private static void addPatientNode(Element formNode, Element modelNode, Element bodyNode, String name, String type,
	                                   String label, String hint, boolean required, boolean readonly, String controlType,
	                                   String[] items, String[] itemValues, boolean visible) {
		
		addPatientNode(formNode, modelNode, bodyNode, name, type, label, hint, required, readonly, controlType, items,
		    itemValues, visible, "/" + NODE_PATIENT + "/" + name);
	}
	
	/**
	 * Adds a node to a new patient xform.
	 * 
	 * @param formNode
	 * @param modelNode
	 * @param bodyNode
	 * @param name
	 * @param type
	 * @param label
	 * @param hint
	 * @param required
	 * @param readonly
	 * @param controlType
	 * @param items
	 * @param itemValues
	 */
	public static void addPatientNode(Element formNode, Element modelNode, Element bodyNode, String name, String type,
	                                  String label, String hint, boolean required, boolean readonly, String controlType,
	                                  String[] items, String[] itemValues, boolean visible, String nodeset) {
		//add the model node
		Element element = formNode.createElement(null, null);
		element.setName(name);
		formNode.addChild(Element.ELEMENT, element);
		
		//if(name.equals("patient_identifier_type_id"))
		//	element.addChild(Element.TEXT, getDefaultIdentifierType());
		
		//add the model binding
		element = modelNode.createElement(NAMESPACE_XFORMS, null);
		element.setName(NODE_BIND);
		element.setAttribute(null, ATTRIBUTE_ID, name);
		element.setAttribute(null, ATTRIBUTE_NODESET, nodeset);
		element.setAttribute(null, ATTRIBUTE_TYPE, type);
		if (readonly)
			element.setAttribute(null, ATTRIBUTE_READONLY, XPATH_VALUE_TRUE);
		if (required)
			element.setAttribute(null, ATTRIBUTE_REQUIRED, XPATH_VALUE_TRUE);
		if (!visible)
			element.setAttribute(null, ATTRIBUTE_VISIBLE, XPATH_VALUE_FALSE);
		modelNode.addChild(Element.ELEMENT, element);
		
		//add the control
		element = bodyNode.createElement(NAMESPACE_XFORMS, null);
		element.setName(controlType);
		element.setAttribute(null, ATTRIBUTE_BIND, name);
		bodyNode.addChild(Element.ELEMENT, element);
		
		//add the label
		Element child = element.createElement(NAMESPACE_XFORMS, null);
		child.setName(NODE_LABEL);
		child.addChild(Element.TEXT, label);
		element.addChild(Element.ELEMENT, child);
		
		//add the hint
		child = element.createElement(NAMESPACE_XFORMS, null);
		child.setName(NODE_HINT);
		child.addChild(Element.TEXT, hint);
		element.addChild(Element.ELEMENT, child);
		
		//add control items
		if (items != null) {
			for (int i = 0; i < items.length; i++) {
				child = element.createElement(NAMESPACE_XFORMS, null);
				child.setName(NODE_ITEM);
				element.addChild(Element.ELEMENT, child);
				
				Element elem = element.createElement(NAMESPACE_XFORMS, null);
				elem.setName(NODE_LABEL);
				elem.addChild(Element.TEXT, items[i]);
				child.addChild(Element.ELEMENT, elem);
				
				elem = element.createElement(NAMESPACE_XFORMS, null);
				elem.setName(NODE_VALUE);
				elem.addChild(Element.TEXT, itemValues[i]);
				child.addChild(Element.ELEMENT, elem);
			}
		}
	}
	
	private static String getDefaultIdentifierType() {
		return null; //TODO Not high priority for now
	}
	
	/**
	 * Adds person attributes to the patient creator xform.
	 * 
	 * @param formDataNode the form submit data node.
	 * @param modelNode the xforms model node.
	 * @param xformsNode the xforms ui control node.
	 */
	private static void addPersonAttributes(Element formDataNode, Element modelNode, Element xformsNode) {
		
		List<PersonAttributeType> attributeTypes = Context.getPersonService().getPersonAttributeTypes(PERSON_TYPE.PERSON,
		    null);
		
		for (PersonAttributeType attribute : attributeTypes)
			addPatientAttributeNode(attribute, formDataNode, modelNode, xformsNode);
	}
	
	private static void addPersonAddresses(Element formNode, Element modelNode, Element groupNode) {
		
		/*Element dataNode = formNode.createElement(null, null);
		dataNode.setName(NODE_NAME_PERSON_ADDRESSES);
		formNode.addChild(Element.ELEMENT, dataNode);*/

		Element dataNode = formNode;
		
		//addPersonAddress(NODE_NAME_PREFERRED, "Preferred", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_ADDRESS1, "Address 1", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_ADDRESS2, "Address 2", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_CITY_VILLAGE, "City/Village", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_STATE_PROVINCE, "State/Province", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_POSTAL_CODE, "Postal Code", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_COUNTRY, "Country", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_LATITUDE, "Latitude", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_LONGITUDE, "Longitude", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_COUNTY_DISTRICT, "County/District", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_NEIGHBORHOOD_CELL, "Neighborhood Cell", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_REGION, "Region", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_SUBREGION, "Sub Region", dataNode, modelNode, groupNode);
		addPersonAddress(NODE_NAME_TOWNSHIP_DIVISION, "Township/Division", dataNode, modelNode, groupNode);
	}
	
	private static void addPersonAddress(String name, String text, Element formNode, Element modelNode, Element groupNode) {
		
		name = NODE_NAME_PREFIX_PERSON_ADDRESS + name;
		
		//add the model node
		Element dataNode = formNode.createElement(null, null);
		dataNode.setName(name);
		formNode.addChild(Element.ELEMENT, dataNode);
		
		//add the model binding
		Element bindingNode = modelNode.createElement(NAMESPACE_XFORMS, null);
		bindingNode.setName(NODE_BIND);
		bindingNode.setAttribute(null, ATTRIBUTE_ID, name);
		bindingNode.setAttribute(null, ATTRIBUTE_NODESET, "/" + NODE_PATIENT + "/" /*+NODE_NAME_PERSON_ADDRESSES+"/"*/
		        + name);
		bindingNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
		
		modelNode.addChild(Element.ELEMENT, bindingNode);
		
		//add the control
		Element element = groupNode.createElement(NAMESPACE_XFORMS, null);
		element.setName(CONTROL_INPUT);
		element.setAttribute(null, ATTRIBUTE_BIND, name);
		groupNode.addChild(Element.ELEMENT, element);
		
		//add the label
		Element child = element.createElement(NAMESPACE_XFORMS, null);
		child.setName(NODE_LABEL);
		child.addChild(Element.TEXT, text);
		element.addChild(Element.ELEMENT, child);
		
		//add the hint
		/*child = element.createElement(NAMESPACE_XFORMS, null);
		child.setName(NODE_HINT);
		child.addChild(Element.TEXT, attribute.getDescription());
		element.addChild(Element.ELEMENT, child);*/
	}
	
	private static void addPatientAttributeNode(PersonAttributeType attribute, Element formNode, Element modelNode,
	                                            Element bodyNode) {
		
		String controlName = getPersonAttributeControlType(attribute);
		
		if (controlName == null) {
			System.out.println("For attribute=" + attribute.getName() + " No concept found with id="
			        + attribute.getForeignKey());
			return;
		}
		
		String repeatSubName = "";
		String name = "person_attribute" + attribute.getPersonAttributeTypeId();
		if (controlName.equals(CONTROL_REPEAT)) {
			repeatSubName = name;
			name = "person_attribute_repeat_section" + attribute.getPersonAttributeTypeId();
		}
		
		String type = getPersonAttributeType(attribute);
		
		//add the model node
		Element dataNode = formNode.createElement(null, null);
		dataNode.setName(name);
		formNode.addChild(Element.ELEMENT, dataNode);
		
		if (controlName.equals(CONTROL_REPEAT)) {
			Element node = dataNode.createElement(null, null);
			node.setName(repeatSubName);
			dataNode.addChild(Element.ELEMENT, node);
			dataNode = node;
		}
		
		//add the model binding
		Element bindingNode = modelNode.createElement(NAMESPACE_XFORMS, null);
		bindingNode.setName(NODE_BIND);
		bindingNode.setAttribute(null, ATTRIBUTE_ID, name);
		if (repeatSubName.length() > 0)
			repeatSubName = "/" + repeatSubName;
		bindingNode.setAttribute(null, ATTRIBUTE_NODESET, "/" + NODE_PATIENT + "/" + name + repeatSubName);
		if (!controlName.equals(CONTROL_REPEAT))
			bindingNode.setAttribute(null, ATTRIBUTE_TYPE, type);
		
		modelNode.addChild(Element.ELEMENT, bindingNode);
		
		//add the control
		Element element = bodyNode.createElement(NAMESPACE_XFORMS, null);
		Element newBodyNode = bodyNode;
		
		if (controlName.equals(CONTROL_REPEAT)) {
			Element groupNode = bodyNode.createElement(NAMESPACE_XFORMS, null);
			groupNode.setName(NODE_GROUP);
			Element labelNode = element.createElement(NAMESPACE_XFORMS, null);
			labelNode.setName(NODE_LABEL);
			labelNode.addChild(Element.TEXT, name);
			groupNode.addChild(Element.ELEMENT, labelNode);
			bodyNode.addChild(Element.ELEMENT, groupNode);
			
			Element repeatNode = groupNode.createElement(NAMESPACE_XFORMS, null);
			repeatNode.setName(CONTROL_REPEAT);
			repeatNode.setAttribute(null, ATTRIBUTE_BIND, name);
			groupNode.addChild(Element.ELEMENT, repeatNode);
			
			element = repeatNode;
			newBodyNode = repeatNode;
		} else {
			element.setName(controlName);
			element.setAttribute(null, ATTRIBUTE_BIND, name);
			bodyNode.addChild(Element.ELEMENT, element);
			dataNode = formNode;
		}
		
		//add the label
		Element child = element.createElement(NAMESPACE_XFORMS, null);
		child.setName(NODE_LABEL);
		child.addChild(Element.TEXT, attribute.getName());
		element.addChild(Element.ELEMENT, child);
		
		//add the hint
		child = element.createElement(NAMESPACE_XFORMS, null);
		child.setName(NODE_HINT);
		child.addChild(Element.TEXT, attribute.getDescription());
		element.addChild(Element.ELEMENT, child);
		
		if (attribute.getFormat() != null) {
			if ("org.openmrs.Location".equals(attribute.getFormat().trim()))
				populateLocations(element);
			else if ("org.openmrs.Concept".equals(attribute.getFormat().trim()))
				pupulateConceptOptions(element, attribute.getForeignKey(), dataNode, modelNode, newBodyNode);
		}
	}
	
	private static void pupulateConceptOptions(Element controlNode, Integer conceptId, Element formNode, Element modelNode,
	                                           Element bodyNode) {
		if (conceptId == null)
			return;
		
		Concept concept = Context.getConceptService().getConcept(conceptId);
		if (concept == null)
			return;
		
		pupulateConceptOptions(controlNode, concept, formNode, modelNode, bodyNode);
	}
	
	private static void pupulateConceptOptions(Element controlNode, Concept concept, Element formNode, Element modelNode,
	                                           Element bodyNode) {
		if (concept == null)
			return;
		
		if (concept.isSet())
			populateConceptSet(controlNode, concept, formNode, modelNode, bodyNode);
		else {
			Collection<ConceptAnswer> conceptAnswers = concept.getAnswers();
			
			for (ConceptAnswer conceptAnswer : conceptAnswers) {
				Concept answerConcept = conceptAnswer.getAnswerConcept();
				if (answerConcept == null)
					continue;
				
				Element itemNode = controlNode.createElement(NAMESPACE_XFORMS, null);
				itemNode.setName(NODE_ITEM);
				itemNode.setAttribute(null, ATTRIBUTE_CONCEPT_ID, answerConcept.getConceptId().toString());
				
				Element node = itemNode.createElement(NAMESPACE_XFORMS, null);
				node.setName(NODE_LABEL);
				
				node.addChild(Element.TEXT, answerConcept.getName().getName());
				itemNode.addChild(Element.ELEMENT, node);
				
				node = itemNode.createElement(NAMESPACE_XFORMS, null);
				node.setName(NODE_VALUE);
				node.addChild(Element.TEXT, answerConcept.getConceptId().toString());
				
				itemNode.addChild(Element.ELEMENT, node);
				
				controlNode.addChild(Element.ELEMENT, itemNode);
			}
		}
	}
	
	private static void populateConceptSet(Element controlNode, Concept concept, Element formNode, Element modelNode,
	                                       Element bodyNode) {
		
		List<Concept> conceptSet = Context.getConceptService().getConceptsInSet(concept);
		for (Concept c : conceptSet) {
			
			String name = "person_attribute_concept" + c.getConceptId();
			String type = getConceptDataType(c);
			
			//add the model node
			Element element = formNode.createElement(null, null);
			element.setName(name);
			formNode.addChild(Element.ELEMENT, element);
			
			//add the model binding
			/*element = modelNode.createElement(NAMESPACE_XFORMS, null);
			element.setName(NODE_BIND);
			element.setAttribute(null, ATTRIBUTE_ID, name);
			element.setAttribute(null, ATTRIBUTE_NODESET, "/"+NODE_PATIENT+"/"+name);
			element.setAttribute(null, ATTRIBUTE_TYPE, type);

			modelNode.addChild(Element.ELEMENT, element);*/

			//add the control
			element = bodyNode.createElement(NAMESPACE_XFORMS, null);
			element.setName(getConceptControlType(c));
			element.setAttribute(null, ATTRIBUTE_BIND, name);
			element.setAttribute(null, ATTRIBUTE_TYPE, type);
			bodyNode.addChild(Element.ELEMENT, element);
			
			//add the label
			Element child = element.createElement(NAMESPACE_XFORMS, null);
			child.setName(NODE_LABEL);
			child.addChild(Element.TEXT, c.getName().getName());
			element.addChild(Element.ELEMENT, child);
			
			//add the hint
			child = element.createElement(NAMESPACE_XFORMS, null);
			child.setName(NODE_HINT);
			child.addChild(Element.TEXT, c.getName().getDescription());
			element.addChild(Element.ELEMENT, child);
			
			pupulateConceptOptions(element, c, formNode, modelNode, bodyNode);
		}
	}
	
	private static String getPersonAttributeType(PersonAttributeType attribute) {
		
		String type = attribute.getFormat();
		
		if (type.equals("java.lang.Integer"))
			return DATA_TYPE_INT;
		else if (type.equals("java.lang.Double") || type.equals("java.lang.Float"))
			return DATA_TYPE_DECIMAL;
		else if (type.equals("java.lang.Boolean"))
			return DATA_TYPE_BOOLEAN;
		else if (type.equals("java.util.Date") || type.equals("java.sql.Date"))
			return DATA_TYPE_DATE;
		
		return DATA_TYPE_TEXT;
	}
	
	private static String getConceptDataType(Concept concept) {
		
		ConceptDatatype datatype = concept.getDatatype();
		
		if (datatype.isNumeric())
			return DATA_TYPE_DECIMAL;
		else if (datatype.isBoolean())
			return DATA_TYPE_BOOLEAN;
		else if (datatype.isDate())
			return DATA_TYPE_DATE;
		else if (datatype.getHl7Abbreviation().equals("ED"))
			return DATA_TYPE_BASE64BINARY;
		
		return DATA_TYPE_TEXT;
	}
	
	private static String getPersonAttributeControlType(PersonAttributeType attribute) {
		String type = attribute.getFormat();
		if (type == null)
			type = "";
		else
			type = type.trim();
		
		if (type.equals("org.openmrs.Location"))
			return CONTROL_SELECT1;
		else if ((type.equals("org.openmrs.Concept") && attribute.getForeignKey() != null)) {
			Concept concept = Context.getConceptService().getConcept(attribute.getForeignKey());
			if (concept == null)
				return null;
			
			if (concept.isSet())
				return CONTROL_REPEAT;
			return CONTROL_SELECT1;
		}
		return CONTROL_INPUT;
	}
	
	private static String getConceptControlType(Concept concept) {
		if (concept.isSet())
			return CONTROL_REPEAT;
		else if (concept.getAnswers() != null && concept.getAnswers().size() > 0)
			return CONTROL_SELECT1;
		return CONTROL_INPUT;
	}
	
	public static Document setPatientFieldValues(Patient patient, Form form, Document doc, XformsService xformsService)
	    throws Exception {
		//EasyFactoryConfiguration config = new EasyFactoryConfiguration();
		
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
		    "org.apache.velocity.runtime.log.CommonsLogLogChute");
		velocityEngine.setProperty(CommonsLogLogChute.LOGCHUTE_COMMONS_LOG_NAME, "xforms_velocity");
		velocityEngine.init();
		VelocityContext velocityContext = new VelocityContext();
		velocityContext.put("calendar", Calendar.getInstance());
		velocityContext.put("patient", patient);
		velocityContext.put("form", form);
		velocityContext.put("obs", new ObsHistory(patient));
		velocityContext.put("concept", new ConceptUtil());
		velocityContext.put("location", new LocationUtil());
		velocityContext.put(
		    "timestamp",
		    new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
		        XformConstants.GLOBAL_PROP_KEY_DATE_TIME_SUBMIT_FORMAT, XformConstants.DEFAULT_DATE_TIME_SUBMIT_FORMAT)));
		velocityContext.put(
		    "date",
		    new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
		        XformConstants.GLOBAL_PROP_KEY_DATE_SUBMIT_FORMAT, XformConstants.DEFAULT_DATE_SUBMIT_FORMAT)));
		velocityContext.put(
		    "time",
		    new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
		        XformConstants.GLOBAL_PROP_KEY_TIME_SUBMIT_FORMAT, XformConstants.DEFAULT_TIME_SUBMIT_FORMAT)));
		
		velocityContext.put(
		    "displayTimestamp",
		    new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
		        XformConstants.GLOBAL_PROP_KEY_DATE_TIME_DISPLAY_FORMAT, XformConstants.DEFAULT_DATE_TIME_DISPLAY_FORMAT)));
		velocityContext.put(
		    "displayDate",
		    new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
		        XformConstants.GLOBAL_PROP_KEY_DATE_DISPLAY_FORMAT, XformConstants.DEFAULT_DATE_DISPLAY_FORMAT)));
		velocityContext.put(
		    "displayTime",
		    new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
		        XformConstants.GLOBAL_PROP_KEY_TIME_DISPLAY_FORMAT, XformConstants.DEFAULT_TIME_DISPLAY_FORMAT)));
		
		List<Encounter> encounters = Context.getEncounterService().getEncountersByPatientId(patient.getPatientId(), false);
		velocityContext.put("patientEncounters", encounters);
		
		List<Relationship> relationships = Context.getPersonService().getRelationshipsByPerson(patient);
		// change Person objects to Patient objects if applicable
		for (Relationship rel : relationships) {
			Person otherPerson = null;
			if (rel.getPersonA().equals(patient)) {
				otherPerson = rel.getPersonB();
				if (otherPerson.isPatient())
					rel.setPersonB(Context.getPatientService().getPatient(otherPerson.getPersonId()));
			}
			else {
				otherPerson = rel.getPersonA();
				if (otherPerson.isPatient())
					rel.setPersonA(Context.getPatientService().getPatient(otherPerson.getPersonId()));
			}
		}
		// we need at least one empty relationship in InfoPath
		if (relationships.isEmpty()) {
			relationships = new ArrayList<Relationship>();
			relationships.add(new Relationship());
		}
		velocityContext.put("relationships", relationships);
		
		//TODO Uncomment this after relation ship widget ticket is done.
		//and fully test to see if forms still submit all the way from OpenMRS 1.6
		/*EventCartridge eventCartridge = new EventCartridge();
		eventCartridge.addEventHandler(new VelocityExceptionHandler());
		velocityContext.attachEventCartridge(eventCartridge);
		
		String xml = fromDoc2String(doc);
		try {
			StringWriter w = new StringWriter();
			velocityEngine.evaluate(velocityContext, w, XformBuilder.class.getName(), xml);
			doc = getDocument(w.toString());
		}
		catch (Exception e) {
			log.error("Error evaluating default values for form " + form.getName() + "["
			    + form.getFormId() + "]", e);
		}*/
		
		setPatientTableFieldValues(form.getFormId(), doc.getRootElement(), xformsService, velocityEngine, velocityContext);
	
		return doc;
	}
	
	public static Element createCopy(Element element, List<String> nonCopyAttributes) {
		Element copy = element.getParent().createElement(null, null);
		copy.setName(element.getName());
		element.getParent().addChild(Element.ELEMENT, copy);
		
		copyAttributes(element, copy, nonCopyAttributes);
		copyChildren(element, copy, nonCopyAttributes);
		
		return copy;
	}
	
	private static void copyChildren(Element srcNode, Element dstNode, List<String> nonCopyAttributes) {
		
		for (int index = 0; index < srcNode.getChildCount(); index++) {
			if (srcNode.getType(index) != Element.ELEMENT)
				continue;
			
			Element child = (Element) srcNode.getChild(index);
			
			Element newChild = dstNode.createElement(null, null);
			newChild.setName(child.getName());
			dstNode.addChild(Element.ELEMENT, newChild);
			
			copyAttributes(child, newChild, nonCopyAttributes);
			copyChildren(child, newChild, nonCopyAttributes);
		}
	}
	
	private static void copyAttributes(Element srcNode, Element dstNode, List<String> nonCopyAttributes) {
		for (int index = 0; index < srcNode.getAttributeCount(); index++) {
			String name = srcNode.getAttributeName(index);
			if (!nonCopyAttributes.contains(name))
				dstNode.setAttribute(null, name, srcNode.getAttributeValue(null, name));
		}
	}
	
	/**
	 * Adds and encounter section to the patient registration form for entering obs during new
	 * patient registration.
	 * 
	 * @param doc
	 * @param formNode
	 * @param modelNode
	 * @param groupNode
	 */
	private static void addEncounterForm(Document doc, Element formNode, Element modelNode, Element groupNode) {
		String formId = Context.getAdministrationService().getGlobalProperty("xforms.patientRegEncounterFormId", "0");
		if ("0".equals(formId))
			return;
		
		FormService formService = (FormService) Context.getService(FormService.class);
		Form form = formService.getForm(Integer.parseInt(formId));
		if (form == null)
			return;
		
		String templateXml = FormEntryWrapper.getFormTemplate(form);
		Document templateDoc = XformBuilder.getDocument(templateXml);
		Element rootNode = templateDoc.getRootElement();
		for (int index = 0; index < rootNode.getChildCount(); index++) {
			if (rootNode.getType(index) != Element.ELEMENT)
				continue;
			
			Element child = (Element) rootNode.getChild(index);
			if (child.getName().equalsIgnoreCase("patient")) {
				removeNonUsedPatientChildNodes(child);
				break;
			}
		}
		formNode.addChild(org.kxml2.kdom.Element.ELEMENT, rootNode);
		
		Document xformSchemaDoc = new Document();
		xformSchemaDoc.setEncoding(XformConstants.DEFAULT_CHARACTER_ENCODING);
		Element xformSchemaNode = doc.createElement(NAMESPACE_XML_SCHEMA, null);
		xformSchemaNode.setName(NODE_SCHEMA);
		xformSchemaDoc.addChild(org.kxml2.kdom.Element.ELEMENT, xformSchemaNode);
		
		Hashtable bindings = new Hashtable();
		Hashtable<String, String> problemList = new Hashtable<String, String>();
		Hashtable<String, String> problemListItems = new Hashtable<String, String>();
		parseTemplate(modelNode, formNode, formNode, bindings, groupNode, problemList, problemListItems, 0);
		
		Document schemaDoc = XformBuilder.getDocument(XformsUtil.getSchema(form));
		parseSchema(schemaDoc.getRootElement(), groupNode, modelNode, xformSchemaNode, bindings, problemList,
		    problemListItems);
		
		removeNonUsedUINodes(groupNode);
	}
	
	/**
	 * Removes data nodes that are kids of the patient node for obs entry on a patient registration
	 * form.
	 * 
	 * @param patientNode the patient node.
	 */
	private static void removeNonUsedPatientChildNodes(Element patientNode) {
		for (int index = 0; index < patientNode.getChildCount(); index++) {
			if (patientNode.getType(index) != Element.ELEMENT)
				continue;
			
			Element child = (Element) patientNode.getChild(index);
			if (!child.getName().equalsIgnoreCase("patient.patient_id")) {
				patientNode.removeChild(index);
				index -= 1;
			}
		}
	}
	
	/**
	 * Removes ui nodes that are not necessary for obs entry on a patient registration form.
	 * 
	 * @param groupNode the group node having the ui nodes.
	 */
	private static void removeNonUsedUINodes(Element groupNode) {
		for (int index = 0; index < groupNode.getChildCount(); index++) {
			if (groupNode.getType(index) != Element.ELEMENT)
				continue;
			
			Element child = (Element) groupNode.getChild(index);
			String value = child.getAttributeValue(null, "bind");
			if ("encounter.location_id".equalsIgnoreCase(value) || "patient.patient_id".equalsIgnoreCase(value)) {
				groupNode.removeChild(index);
				index -= 1;
			}
		}
	}
	
	/**
	 * Gets the name of the location as displayed in xforms.
	 * 
	 * @param location the location object.
	 * @return the location display text.
	 */
	public static String getLocationName(Location location) {
		return location.getName() + " [" + location.getLocationId() + "]";
	}
	
	/**
	 * Gets the name of the provider as it will be displayed in xforms.
	 * 
	 * @param provider the provider.
	 * @param personId the id of the person this provider represents.
	 * @return the display formatted provider name.
	 */
	public static String getProviderName(User provider, Integer personId) {
		PersonName personName = provider.getPersonName(); //This may be null for some users that have not last and first names.
		return (personName != null ? personName.toString() : provider.getUsername()) + " [" + personId + "]";
	}
	
	private static void addOtherPatientIdentifiers(Element formNode, Element modelNode, Element groupNode) {
		
		String name = NODE_NAME_OTHER_IDENTIFIERS;
		
		//add the model node
		Element dataNode = formNode.createElement(null, null);
		dataNode.setName(name);
		formNode.addChild(Element.ELEMENT, dataNode);
		
		//add the model binding
		Element bindingNode = modelNode.createElement(NAMESPACE_XFORMS, null);
		bindingNode.setName(NODE_BIND);
		bindingNode.setAttribute(null, ATTRIBUTE_ID, name);
		bindingNode.setAttribute(null, ATTRIBUTE_NODESET, "/" + NODE_PATIENT + "/" /*+NODE_NAME_PERSON_ADDRESSES+"/"*/
		        + name);
		bindingNode.setAttribute(null, ATTRIBUTE_TYPE, DATA_TYPE_TEXT);
		
		modelNode.addChild(Element.ELEMENT, bindingNode);
		
		//Create repeat group node
		Element repeatGroupNode = groupNode.createElement(NAMESPACE_XFORMS, null);
		repeatGroupNode.setName(NODE_GROUP);
		groupNode.addChild(Element.ELEMENT, repeatGroupNode);
		
		//add the repeat group label
		Element labelNode = repeatGroupNode.createElement(NAMESPACE_XFORMS, null);
		labelNode.setName(NODE_LABEL);
		labelNode.addChild(Element.TEXT, "Other dentifiers");
		repeatGroupNode.addChild(Element.ELEMENT, labelNode);
		
		//add repeat node.
		Element repeatNode = repeatGroupNode.createElement(NAMESPACE_XFORMS, null);
		repeatNode.setName(CONTROL_REPEAT);
		repeatNode.setAttribute(null, ATTRIBUTE_BIND, name);
		repeatGroupNode.addChild(Element.ELEMENT, repeatNode);
		
		addPatientNode(dataNode, modelNode, repeatNode, NODE_NAME_OTHER_IDENTIFIER, DATA_TYPE_TEXT, "Identifier",
		    "The patient's other identifier value", false, false, CONTROL_INPUT, null, null, true, "/" + NODE_PATIENT + "/"
		            + name + "/other_identifier");
		
		String[] items, itemValues;
		List<PatientIdentifierType> identifierTypes = Context.getPatientService().getAllPatientIdentifierTypes();
		if (identifierTypes != null) {
			int i = 0;
			items = new String[identifierTypes.size()];
			itemValues = new String[identifierTypes.size()];
			for (PatientIdentifierType identifierType : identifierTypes) {
				items[i] = identifierType.getName();
				itemValues[i++] = identifierType.getPatientIdentifierTypeId().toString();
			}
			addPatientNode(dataNode, modelNode, repeatNode, NODE_NAME_OTHER_IDENTIFIER_TYPE_ID, DATA_TYPE_INT,
			    "Identifier Type", "The patient's other identifier type", false, false, CONTROL_SELECT1, items, itemValues,
			    true, "/" + NODE_PATIENT + "/" + name + "/other_identifier_type_id");
		}
		
		List<Location> locations = Context.getLocationService().getAllLocations();
		if (locations != null) {
			int i = 0;
			items = new String[locations.size()];
			itemValues = new String[locations.size()];
			for (Location location : locations) {
				items[i] = getLocationName(location);
				itemValues[i++] = location.getLocationId().toString();
			}
			
			addPatientNode(dataNode, modelNode, repeatNode, NODE_NAME_OTHER_IDENTIFIER_LOCATION_ID, DATA_TYPE_INT,
			    "Identifier Location", "The patient's other identifier location", false, false, CONTROL_SELECT1, items,
			    itemValues, true, "/" + NODE_PATIENT + "/" + name + "/other_identifier_location_id");
		}
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyChanged(org.openmrs.GlobalProperty)
	 */
	@Override
	public void globalPropertyChanged(GlobalProperty gp) {
		if (XformConstants.XFORM_GP_USE_AUTOCOMPLETE_FOR_LOCATIONS.equals(gp.getProperty()))
			useAutoCompleteForLocations = null;
		else
			useAutoCompleteForProviders = null;
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyDeleted(java.lang.String)
	 */
	@Override
	public void globalPropertyDeleted(String gpName) {
		if (XformConstants.XFORM_GP_USE_AUTOCOMPLETE_FOR_LOCATIONS.equals(gpName))
			useAutoCompleteForLocations = null;
		else
			useAutoCompleteForProviders = null;
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#supportsPropertyName(java.lang.String)
	 */
	@Override
	public boolean supportsPropertyName(String gpName) {
		return XformConstants.XFORM_GP_USE_AUTOCOMPLETE_FOR_LOCATIONS.equals(gpName)
		        || XformConstants.XFORM_GP_USE_AUTOCOMPLETE_FOR_PROVIDERS.equals(gpName);
	}
	
	/**
	 * Determines whether to use an autocomplete field or a select for an xforms UI node
	 * 
	 * @param nodeName the name of the xforms node
	 * @return true if it should be an autocomplete otherwise false
	 */
	private static boolean useAutoCompleteForNode(String nodeName) {
		if (nodeName.equalsIgnoreCase(NODE_ENCOUNTER_LOCATION_ID) && XformsUtil.usesJquery()) {
			if (useAutoCompleteForLocations == null) {
				useAutoCompleteForLocations = Boolean.valueOf(Context.getAdministrationService().getGlobalProperty(
				    XformConstants.XFORM_GP_USE_AUTOCOMPLETE_FOR_LOCATIONS));
			}
			return useAutoCompleteForLocations;
		} else if (nodeName.equalsIgnoreCase(NODE_ENCOUNTER_PROVIDER_ID) && XformsUtil.isOnePointNineAndAbove()) {
			if (useAutoCompleteForProviders == null) {
				useAutoCompleteForProviders = Boolean.valueOf(Context.getAdministrationService().getGlobalProperty(
				    XformConstants.XFORM_GP_USE_AUTOCOMPLETE_FOR_PROVIDERS));
			}
			return useAutoCompleteForProviders;
		}
		
		//This is not a location or provider element
		return false;
	}
}
