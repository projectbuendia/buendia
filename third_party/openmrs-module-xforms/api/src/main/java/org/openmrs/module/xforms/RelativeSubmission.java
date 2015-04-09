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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kxml2.kdom.Element;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.APIException;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;

public class RelativeSubmission {
	
	/**
	 * Saves relationships which have been edited, added, or deleted.
	 * 
	 * @param node the form root node.
	 * @param patient the patient that the form has been submitted for.
	 * @should create relationships
	 * @should edit an editing relationship
	 */
	public static void submit(String xml, Patient patient) {
		Element patientNode = XformBuilder.getElement(XformBuilder.getDocument(xml).getRootElement(),
		    XformBuilder.NODE_PATIENT);
		
		if (patientNode == null)
			return;
		
		if (XformBuilder.getElement(patientNode, RelativeBuilder.NODE_RELATIVE) == null) {
			return; //user deleted the relationships node XFRM-176
		}
		
		PersonService ps = Context.getPersonService();
		
		//we start by assuming all relationships are deleted until found in the submission.
		List<Relationship> deletedRelationships = ps.getRelationshipsByPerson(patient);
		List<Relationship> newRelationships = new ArrayList<Relationship>();
		
		for (int i = 0; i < patientNode.getChildCount(); i++) {
			if (patientNode.getType(i) != Element.ELEMENT
			        || !RelativeBuilder.NODE_RELATIVE.equals(patientNode.getElement(i).getName())) {
				continue;
			}
			
			Element relativeNode = patientNode.getElement(i);
			
			String relationshipUuid = relativeNode.getAttributeValue(null, XformBuilder.ATTRIBUTE_UUID);
			if (!("true()".equals(relativeNode.getAttributeValue(null, "new"))) && StringUtils.isNotBlank(relationshipUuid)) {
				updateRelationship(relationshipUuid, relativeNode, patient, deletedRelationships, ps);
				continue;
			}
			
			Relationship relationship = getRelationship(relativeNode, patient, ps);
			if (relationship != null) {
				newRelationships.add(relationship);
			}
		}
		
		for (Relationship reltnp : newRelationships) {
			ps.saveRelationship(reltnp);
		}
		
		for (Relationship reltnp : deletedRelationships) {
			ps.purgeRelationship(reltnp);
		}
	}
	
	private static Relationship getRelationship(Element relativeNode, Patient patient, PersonService ps) {
		Element personNode = XformBuilder.getElement(relativeNode, RelativeBuilder.BIND_PERSON);
		Element relationshipNode = XformBuilder.getElement(relativeNode, RelativeBuilder.NODE_RELATIONSHIP);
		
		String personUuid = XformBuilder.getTextValue(personNode);
		String relationshipStr = XformBuilder.getTextValue(relationshipNode);
		
		if (StringUtils.isBlank(personUuid) || StringUtils.isBlank(relationshipStr)) {
			if (StringUtils.isNotBlank(personUuid) || StringUtils.isNotBlank(relationshipStr)) {
				throw new APIException("Both person and relationship should be null or not null");
			}
			return null;
		}
		
		String AorB = relationshipStr.substring(relationshipStr.length() - 1);
		String relationshipTypeId = relationshipStr.substring(0, relationshipStr.length() - 1);
		
		RelationshipType relationshipType = null;
		if (StringUtils.isNotBlank(relationshipTypeId))
			relationshipType = ps.getRelationshipType(Integer.valueOf(relationshipTypeId));
		if (relationshipType == null)
			throw new APIException("Cannot find relation type with id:" + relationshipType);
		
		boolean isPersonA = false;
		if (AorB != null && ("A".equalsIgnoreCase(AorB) || "B".equalsIgnoreCase(AorB))) {
			isPersonA = "A".equalsIgnoreCase(AorB);
		} else {
			throw new APIException("Cannot determine if the patient is A or B in the relationship:"
			        + relationshipType.getName());
		}
		
		Person otherPerson = ps.getPersonByUuid(personUuid);
		if (otherPerson == null)
			throw new APIException("Cannot determine if the relattive in the relationship:" + relationshipType.getName());
		
		Person personA;
		Person personB;
		
		if (isPersonA) {
			personB = patient;
			personA = otherPerson;
		} else {
			personA = patient;
			personB = otherPerson;
		}
		
		Relationship relationship = new Relationship();
		relationship.setRelationshipType(relationshipType);
		
		relationship.setPersonA(personA);
		relationship.setPersonB(personB);
		
		return relationship;
	}
	
	private static void updateRelationship(String uuid, Element relativeNode, Patient patient,
	                                       List<Relationship> deletedRelationships, PersonService ps) {
		
		Relationship relationship = Context.getPersonService().getRelationshipByUuid(uuid);
		if (relationship == null)
			throw new APIException("Cannot find relationship with uuid: " + uuid);
		
		//relationship no longer considered deleted since it is found in the submission
		deletedRelationships.remove(relationship);
		
		Relationship editedRelationship = getRelationship(relativeNode, patient, ps);
		
		if (editedRelationship.getPersonA().getPersonId() == relationship.getPersonA().getPersonId()
		        && editedRelationship.getPersonB().getPersonId() == relationship.getPersonB().getPersonId()
		        && editedRelationship.getRelationshipType().getRelationshipTypeId() == relationship.getRelationshipType()
		                .getRelationshipTypeId()) {
			//not changed.
			return;
		}
		
		relationship.setPersonA(editedRelationship.getPersonA());
		relationship.setPersonB(editedRelationship.getPersonB());
		relationship.setRelationshipType(editedRelationship.getRelationshipType());
		
		relationship.setChangedBy(Context.getAuthenticatedUser());
		relationship.setDateChanged(new Date());
		
		ps.saveRelationship(relationship);
	}
}
