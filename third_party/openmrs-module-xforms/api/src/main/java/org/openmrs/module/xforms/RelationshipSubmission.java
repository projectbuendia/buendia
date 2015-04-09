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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kxml2.kdom.Element;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.APIException;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;

/**
 * Utility for building relationship submission in from data entry xforms.
 * 
 * @since 4.0.3
 */
public class RelationshipSubmission {
	
	private static final Log log = LogFactory.getLog(RelationshipSubmission.class);
	
	/**
	 * Saves relationships which have been edited, added, or deleted.
	 * 
	 * @param node the form root node.
	 * @param patient the patient that the form has been submitted for.
	 * @should create relationships
	 * @should edit an editing relationship
	 */
	public static void submit(Element rootElement, Patient patient) {
		if (rootElement == null)
			return;
		
		Element formNode = XformBuilder.getElement(rootElement, XformBuilder.NODE_FORM);
		if (formNode == null)
			return;
		
		PersonService ps = Context.getPersonService();
		List<Relationship> relationships = new ArrayList<Relationship>();
		for (int i = 0; i < formNode.getChildCount(); i++) {
			if (formNode.getType(i) != Element.ELEMENT
			        || !RelationshipBuilder.NODE_PATIENT_RELATIONSHIP.equals(formNode.getElement(i).getName())) {
				continue;
			}
			//System.out.println("Found element..");
			Element relationshipNode = formNode.getElement(i);
			String uuid = relationshipNode.getAttributeValue(null, XformBuilder.ATTRIBUTE_UUID);
			Person otherPerson = null;
			RelationshipType relationshipType = null;
			boolean isPersonA = false;
			Person personA;
			Person personB;
			
			//resolve the relationship type
			Element relationshipTypeNode = XformBuilder.getElement(relationshipNode,
			    RelationshipBuilder.BIND_PATIENT_RELATIONSHIP_TYPE_ID);
			if (relationshipTypeNode != null) {
				String relationTypeIdString = XformBuilder.getTextValue(relationshipTypeNode);
				if (StringUtils.isNotBlank(relationTypeIdString))
					relationshipType = ps.getRelationshipType(Integer.valueOf(relationTypeIdString));
				if (relationshipType == null)
					throw new APIException("Cannot find relation type with id:" + relationTypeIdString);
			}
			
			//determine is this patient is A or B in the relationship
			Element AorBNode = XformBuilder.getElement(relationshipNode,
			    RelationshipBuilder.BIND_PATIENT_RELATIONSHIP_A_OR_B);
			if (AorBNode == null) {
				throw new APIException("Cannot determine if the patient is A or B in the relationship:"
				        + relationshipType.getName());
			}
			String AorB = XformBuilder.getTextValue(AorBNode);
			if (AorB != null && ("A".equalsIgnoreCase(AorB) || "B".equalsIgnoreCase(AorB))) {
				isPersonA = "A".equalsIgnoreCase(AorB);
			} else {
				throw new APIException("Cannot determine if the patient is A or B in the relationship:"
				        + relationshipType.getName());
			}
			
			//get the relative
			Element relativeNode = XformBuilder.getElement(relationshipNode, RelationshipBuilder.BIND_RELATIVE);
			if (relativeNode != null) {
				Element relativeUuidNode = XformBuilder.getElement(relativeNode, RelationshipBuilder.BIND_RELATIVE_UUID);
				if (relativeUuidNode != null) {
					String relativeUuid = XformBuilder.getTextValue(relativeUuidNode);
					if (StringUtils.isNotBlank(relativeUuid))
						otherPerson = ps.getPersonByUuid(relativeUuid);
				}
			}
			if (otherPerson == null)
				throw new APIException("Cannot determine if the relattive in the relationship:" + relationshipType.getName());
			
			if (isPersonA) {
				personA = patient;
				personB = otherPerson;
			} else {
				personB = patient;
				personA = otherPerson;
			}
			
			Relationship relationship = null;
			if (StringUtils.isNotBlank(uuid)) {
				relationship = ps.getRelationshipByUuid(uuid);
				if (relationship == null)
					throw new APIException("Failed to find relationship with uuid:" + uuid);
			} else {
				relationship = new Relationship();
				relationship.setRelationshipType(relationshipType);
			}
			
			relationship.setPersonA(personA);
			relationship.setPersonB(personB);
			
			relationships.add(relationship);
		}
		
		for (Relationship relationship : relationships) {
			ps.saveRelationship(relationship);
		}
	}
}
