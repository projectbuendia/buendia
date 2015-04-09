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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kxml2.kdom.Element;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.context.Context;

public class RelativeBuilder {
	
	public static final String NODE_RELATIVE = "patient_relative";
	
	public static final String NODE_PERSON = "patient_relative.person";
	
	public static final String NODE_RELATIONSHIP = "patient_relative.relationship";
	
	public static final String BIND_RELATIVE = "patient_relative";
	
	public static final String BIND_PERSON = "patient_relative.person";
	
	public static final String BIND_RELATIONSHIP = "patient_relative.relationship";
	
	private static final Log log = LogFactory.getLog(RelativeBuilder.class);
	
	public static void build(Element modelElement, Element bodyNode, Element dataNode) {
		buildBindNodes(modelElement);
		buildInputNodes(bodyNode);
	}
	
	private static void buildBindNodes(Element modelElement) {
		//Create bind node for relative.
		Element bindNode = modelElement.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		bindNode.setName(XformBuilder.NODE_BIND);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_ID, BIND_RELATIVE);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_NODESET, "/form/patient/patient_relative");
		modelElement.addChild(Element.ELEMENT, bindNode);
		
		//Create bind node for person.
		bindNode = modelElement.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		bindNode.setName(XformBuilder.NODE_BIND);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_ID, BIND_PERSON);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_NODESET, "/form/patient/patient_relative/patient_relative.person");
		modelElement.addChild(Element.ELEMENT, bindNode);
		
		//Create bind node for relationship.
		bindNode = modelElement.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		bindNode.setName(XformBuilder.NODE_BIND);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_ID, BIND_RELATIONSHIP);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_NODESET,
		    "/form/patient/patient_relative/patient_relative.relationship");
		modelElement.addChild(Element.ELEMENT, bindNode);
	}
	
	private static void buildInputNodes(Element bodyNode) {
		
		Element repeatNode = buildRepeatInputNode(bodyNode);
		
		buildPersonInputNode(repeatNode);
		buildRelationshipInputNode(repeatNode);
	}
	
	private static Element buildRepeatInputNode(Element bodyNode) {
		//Create the parent repeat ui node.
		Element groupNode = bodyNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		groupNode.setName(XformBuilder.NODE_GROUP);
		bodyNode.addChild(Element.ELEMENT, groupNode);
		
		Element labelNode = bodyNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		labelNode.setName(XformBuilder.NODE_LABEL);
		labelNode.addChild(Element.TEXT, "RELATIONSHIPS");
		groupNode.addChild(Element.ELEMENT, labelNode);
		
		Element hintNode = bodyNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		hintNode.setName(XformBuilder.NODE_HINT);
		hintNode.addChild(Element.TEXT, "Relationships that this patient has.");
		groupNode.addChild(Element.ELEMENT, hintNode);
		
		Element repeatNode = bodyNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		repeatNode.setName(XformBuilder.CONTROL_REPEAT);
		repeatNode.setAttribute(null, XformBuilder.ATTRIBUTE_BIND, BIND_RELATIVE);
		groupNode.addChild(Element.ELEMENT, repeatNode);
		
		return repeatNode;
	}
	
	private static void buildPersonInputNode(Element repeatNode) {
		//Create person input node.
		Element inputNode = repeatNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		inputNode.setName(XformBuilder.CONTROL_INPUT);
		inputNode.setAttribute(null, XformBuilder.ATTRIBUTE_BIND, BIND_PERSON);
		repeatNode.addChild(Element.ELEMENT, inputNode);
		
		//Create person label.
		Element labelNode = repeatNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		labelNode.setName(XformBuilder.NODE_LABEL);
		labelNode.addChild(Element.TEXT, "RELATIVE");
		inputNode.addChild(Element.ELEMENT, labelNode);
		
		repeatNode.addChild(Element.ELEMENT, inputNode);
	}
	
	private static void buildRelationshipInputNode(Element repeatNode) {
		//Create relationship input node.
		Element inputNode = repeatNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		inputNode.setName(XformBuilder.CONTROL_SELECT1);
		inputNode.setAttribute(null, XformBuilder.ATTRIBUTE_BIND, BIND_RELATIONSHIP);
		
		populateRelationshipTypes(inputNode);
		
		repeatNode.addChild(Element.ELEMENT, inputNode);
		
		//Create relationship label.
		Element labelNode = repeatNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		labelNode.setName(XformBuilder.NODE_LABEL);
		labelNode.addChild(Element.TEXT, "RELATIONSHIP");
		inputNode.addChild(Element.ELEMENT, labelNode);
		
		repeatNode.addChild(Element.ELEMENT, inputNode);
	}
	
	private static void populateRelationshipTypes(Element controlNode) {
		List<RelationshipType> relationshipTypes = Context.getPersonService().getAllRelationshipTypes(false);
		for (RelationshipType type : relationshipTypes) {
			Element itemNode;
			//The value is of the form relationTypeId:A
			itemNode = createRelationTypeOptionNode(type, controlNode, true);
			controlNode.addChild(Element.ELEMENT, itemNode);
			
			//For relationships like sibling/sibling just display one option. Otherwise, we need 2
			//items for each side of the relationship, one for each side of the relationship so that 
			//the user can select which side the of the relationship the relative is i.e A Vs B
			if (!type.getbIsToA().equalsIgnoreCase(type.getaIsToB())) {
				itemNode = createRelationTypeOptionNode(type, controlNode, false);
				controlNode.addChild(Element.ELEMENT, itemNode);
			}
		}
	}
	
	/**
	 * Creates a node for a select option for the specified relation type
	 * 
	 * @param relationshipType the relationshipType object.
	 * @param controlNode the select node
	 * @param isA specifies which side of the relationship we are adding the option for
	 * @return the Element for the select option
	 */
	private static Element createRelationTypeOptionNode(RelationshipType relationshipType, Element controlNode, boolean isA) {
		Element itemNode = controlNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		itemNode.setName(XformBuilder.NODE_ITEM);
		
		Element node = itemNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		node.setName(XformBuilder.NODE_LABEL);
		node.addChild(Element.TEXT, "is " + ((isA) ? relationshipType.getaIsToB() : relationshipType.getbIsToA()) + " ["
		        + relationshipType.getRelationshipTypeId() + "]");
		itemNode.addChild(Element.ELEMENT, node);
		
		node = itemNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		node.setName(XformBuilder.NODE_VALUE);
		node.addChild(Element.TEXT, relationshipType.getRelationshipTypeId() + ((isA) ? "A" : "B"));
		itemNode.addChild(Element.ELEMENT, node);
		return itemNode;
	}
	
	public static void fillRelationships(Patient patient, Element dataNode) throws Exception {
		Element relativeNode = XformBuilder.getElement(dataNode, NODE_RELATIVE);
		if (relativeNode == null)
			return;
		
		int index = 0;
		List<Relationship> relationships = Context.getPersonService().getRelationshipsByPerson(patient);
		for (Relationship relationship : relationships) {
			if (++index > 1)
				relativeNode = XformBuilder.createCopy(relativeNode, new ArrayList<String>());
			
			relativeNode.setAttribute(null, XformBuilder.ATTRIBUTE_UUID, relationship.getUuid());
			
			String personUuid;
			String relative;
			if (getPersonId(patient).equals(relationship.getPersonA().getPersonId())) {
				relative = relationship.getPersonB().getPersonName() + " - "
				        + getPatientIdentifier(relationship.getPersonB());
				
				personUuid = relationship.getPersonB().getUuid();
			} else {
				relative = relationship.getPersonA().getPersonName() + " - "
				        + getPatientIdentifier(relationship.getPersonA());
				
				personUuid = relationship.getPersonA().getUuid();
			}
			
			Element personNode = XformBuilder.getElement(relativeNode, NODE_PERSON);
			personNode.setAttribute(null, "displayValue", relative);
			personNode.setAttribute(null, "default", "false()");
			XformBuilder.setNodeValue(personNode, personUuid);
			
			Element relationshipNode = XformBuilder.getElement(relativeNode, NODE_RELATIONSHIP);
			relationshipNode.setAttribute(null, "default", "false()");
			XformBuilder.setNodeValue(relationshipNode, relationship.getRelationshipType().getRelationshipTypeId()
			        + ((relationship.getPersonA().getPersonId().equals(patient.getPersonId())) ? "B" : "A"));
		}
	}
	
	private static Integer getPersonId(Patient patient) throws Exception {
		try {
			return patient.getPersonId();
		}
		catch (NoSuchMethodError ex) {
			Method method = patient.getClass().getMethod("getPerson", null);
			return ((Person) method.invoke(patient, null)).getPersonId();
		}
	}
	
	private static String getPatientIdentifier(Person person) throws Exception {
		try {
			Patient patient = Context.getPatientService().getPatient(person.getPersonId());
			if (getPersonId(patient) == person.getPersonId()) {
				return patient.getPatientIdentifier().getIdentifier();
			}
		}
		catch(Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return "";
	}
}
