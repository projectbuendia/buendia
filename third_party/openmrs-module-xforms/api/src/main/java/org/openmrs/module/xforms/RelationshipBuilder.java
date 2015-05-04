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

import org.kxml2.kdom.Element;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.context.Context;

/**
 * Utility for building relationship nodes in xforms.
 * 
 * @since 4.0.3
 */
public class RelationshipBuilder {
	
	public static final String NODE_PATIENT_RELATIONSHIP = "patient_relationship";
	
	public static final String BIND_PATIENT_RELATIONSHIP = "patient." + NODE_PATIENT_RELATIONSHIP;
	
	public static final String BIND_RELATIVE = NODE_PATIENT_RELATIONSHIP + ".relative";
	
	public static final String BIND_RELATIONSHIP_TYPE_ID = NODE_PATIENT_RELATIONSHIP + ".relationship_type_id";
	
	public static final String NODE_RELATIVE = "relative";
	
	public static final String BIND_PATIENT_RELATIONSHIP_TYPE_ID = "patient_relationship.relationship_type_id";
	
	public static final String BIND_PATIENT_RELATIONSHIP_A_OR_B = "patient_relationship.a_or_b";
	
	public static final String BIND_RELATIVE_UUID = "relative.uuid";
	
	public static void build(Element modelElement, Element bodyNode, Element dataNode) {
		//Create the parent repeat ui node.
		Element groupNode = bodyNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		groupNode.setName(XformBuilder.NODE_GROUP);
		//groupNode.setAttribute(null, XformBuilder.ATTRIBUTE_BIND, BIND_RELATIVE);
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
		repeatNode.setAttribute(null, XformBuilder.ATTRIBUTE_BIND, BIND_PATIENT_RELATIONSHIP);
		groupNode.addChild(Element.ELEMENT, repeatNode);
		
		//Create relative input node.
		Element inputNode = bodyNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		inputNode.setName(XformBuilder.CONTROL_INPUT);
		inputNode.setAttribute(null, XformBuilder.ATTRIBUTE_BIND, NODE_RELATIVE);
		repeatNode.addChild(Element.ELEMENT, inputNode);
		
		//TODO, Fix the input node for the relative to support person search widget
		
		//Create relative label.
		labelNode = bodyNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		labelNode.setName(XformBuilder.NODE_LABEL);
		labelNode.addChild(Element.TEXT, "RELATIVE");
		inputNode.addChild(Element.ELEMENT, labelNode);
		
		//Create relationship type input node.
		inputNode = bodyNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		inputNode.setName(XformBuilder.CONTROL_SELECT1);
		inputNode.setAttribute(null, XformBuilder.ATTRIBUTE_BIND, BIND_RELATIONSHIP_TYPE_ID);
		
		populateRelationshipTypes(inputNode);
		
		repeatNode.addChild(Element.ELEMENT, inputNode);
		
		//Create relationship label.
		labelNode = bodyNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		labelNode.setName(XformBuilder.NODE_LABEL);
		labelNode.addChild(Element.TEXT, "RELATIONSHIP");
		inputNode.addChild(Element.ELEMENT, labelNode);
		
		//Create bind node for patient relationship.
		Element bindNode = modelElement.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		bindNode.setName(XformBuilder.NODE_BIND);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_ID, BIND_PATIENT_RELATIONSHIP);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_NODESET, "/form/patient/patient_relationship");
		modelElement.addChild(Element.ELEMENT, bindNode);
		
		//Create bind node for patient relationship type.
		bindNode = modelElement.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		bindNode.setName(XformBuilder.NODE_BIND);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_ID, BIND_RELATIONSHIP_TYPE_ID);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_NODESET,
		    "/form/patient/patient_relationship/patient_relationship.relationship_type_id");
		modelElement.addChild(Element.ELEMENT, bindNode);
		
		//Create bind node for relative.
		bindNode = modelElement.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		bindNode.setName(XformBuilder.NODE_BIND);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_ID, NODE_RELATIVE);
		bindNode.setAttribute(null, XformBuilder.ATTRIBUTE_NODESET, "/form/patient/patient_relationship/relative/relative.uuid");
		modelElement.addChild(Element.ELEMENT, bindNode);
	}
	
	public static void fillRelationships(Patient patient, Element dataNode) throws Exception {
		Element patientRelationShipNode = XformBuilder.getElement(dataNode, NODE_PATIENT_RELATIONSHIP);
		if (patientRelationShipNode == null)
			return; //For does not need relationships.
			
		Element emptyPatientRelationShipNode = XformBuilder.createCopy(patientRelationShipNode, new ArrayList<String>());
		
		int index = 0;
		List<Relationship> relationships = Context.getPersonService().getRelationshipsByPerson(patient);
		for (Relationship relationship : relationships) {
			if (++index > 1)
				patientRelationShipNode = XformBuilder.createCopy(emptyPatientRelationShipNode, new ArrayList<String>());
			
			String relative;
			
			if (getPersonId(patient).equals(relationship.getPersonA().getPersonId())) {
				relative = relationship.getPersonB().getPersonName().toString() + " - "
				        + getPatientIdentifier(relationship.getPersonB());
			} else {
				relative = relationship.getPersonA().getPersonName().toString() + " - "
				        + getPatientIdentifier(relationship.getPersonA());
			}
			
			patientRelationShipNode.setAttribute(null, XformBuilder.ATTRIBUTE_UUID, relationship.getUuid());
			
			XformBuilder.getElement(patientRelationShipNode, "relative.uuid").setAttribute(null, "displayValue", relative);
			
			XformBuilder.setNodeValue(patientRelationShipNode, BIND_RELATIONSHIP_TYPE_ID, relationship.getRelationshipType()
			        .getRelationshipTypeId()
			        + ((relationship.getPersonA().getPersonId().equals(patient.getPersonId())) ? "B" : "A"));
			
			XformBuilder.setNodeValue(patientRelationShipNode, "patient_relationship.exists", "1");
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
	
	private static String getShortName(Patient patient) {
		if (patient.getGivenName() != null)
			return patient.getGivenName();
		
		return patient.getFamilyName();
	}
	
	private static String getPatientIdentifier(Person person) throws Exception {
		Patient patient = Context.getPatientService().getPatient(person.getPersonId());
		if (getPersonId(patient) == person.getPersonId())
			return patient.getPatientIdentifier().getIdentifier();
		
		return "";
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
		node.addChild(Element.TEXT, "is the " + ((isA) ? relationshipType.getaIsToB() : relationshipType.getbIsToA()) + " ["
		        + relationshipType.getRelationshipTypeId() + "]");
		itemNode.addChild(Element.ELEMENT, node);
		
		node = itemNode.createElement(XformBuilder.NAMESPACE_XFORMS, null);
		node.setName(XformBuilder.NODE_VALUE);
		node.addChild(Element.TEXT, relationshipType.getRelationshipTypeId() + ((isA) ? "A" : "B"));
		itemNode.addChild(Element.ELEMENT, node);
		return itemNode;
	}
}
