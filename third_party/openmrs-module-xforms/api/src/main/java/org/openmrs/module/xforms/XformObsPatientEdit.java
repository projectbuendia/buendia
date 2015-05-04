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

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.openmrs.Patient;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.xforms.util.XformsUtil;

/**
 * Updates a patient object with demographic, address and attribute data from an encounter xform.
 */
public class XformObsPatientEdit {
	
	public static Patient updatePatientDemographics(String patientId, String xml) throws Exception {
		Patient patient = Context.getPatientService().getPatient(Integer.parseInt(patientId));
		Document doc = XformBuilder.getDocument(xml);
		
		updatePatientDemographics(patient, doc.getRootElement());
		
		Context.getPatientService().savePatient(patient);
		
		return patient;
	}
	
	public static void updatePatientDemographics(Patient patient, Element formNode) throws Exception {
		
		Element patientNode = XformBuilder.getElement(formNode, "patient");
		
		for (int i = 0; i < patientNode.getChildCount(); i++) {
			if (patientNode.getType(i) != Element.ELEMENT)
				continue;
			
			Element child = (Element) patientNode.getChild(i);
			String tableName = child.getAttributeValue(null, "openmrs_table");
			String attributeValue = child.getAttributeValue(null, "openmrs_attribute");
			String dataValue = XformBuilder.getTextValue(child);
			
			//We intentionally do not want to use the xforms module for clearing
			//patient demographic data. In such cases, they should use the official
			//OpenMRS pages for editing such data. That will prevent the module from
			//accidentally clearing data in case of a bug. We also do not update
			//patient identifier because we have not yet written code to tell which
			//identifier type we are dealing with.
			if (StringUtils.isBlank(dataValue)) {
				continue;
			}
			
			if ("person".equals(tableName) || "patient".equals(tableName)) {
				updatePerson(patient, attributeValue, dataValue, child);
			} else if ("person_name".equals(tableName) || "patient_name".equals(tableName)) {
				updatePersonName(patient, attributeValue, dataValue, child);
			} else if ("person_address".equals(tableName) || "patient_address".equals(tableName)) {
				updatePersonAddress(patient, attributeValue, dataValue, child);
			} else if ("person_attribute".equals(tableName) || "patient_attribute".equals(tableName)) {
				updatePersonAttribute(patient, attributeValue, dataValue, child);
			}
		}
	}
	
	private static void updatePerson(Patient patient, String attributeValue, String dataValue, Element node)
	    throws Exception {
		
		if (XformBuilder.NODE_GENDER.equals(attributeValue))
			patient.setGender(dataValue);
		else if (XformBuilder.NODE_BIRTHDATE_ESTIMATED.equals(attributeValue))
			patient.setBirthdateEstimated("true".equals(dataValue));
		else if (XformBuilder.NODE_BIRTHDATE.equals(attributeValue)) {
			patient.setBirthdate(XformsUtil.fromSubmitString2Date(dataValue));
		} else if (!"patient_id".equals(attributeValue))
			throw new APIException("Cannot find person field with name = " + attributeValue);
		
		//TODO do we need these two calls below?
		//patient.setDateChanged(new Date());
		//patient.setChangedBy(Context.getAuthenticatedUser());
	}
	
	private static void updatePersonName(Patient patient, String attributeValue, String dataValue, Element node)
	    throws Exception {
		
		PersonName personName = patient.getPersonName();
		
		if (XformBuilder.NODE_FAMILY_NAME.equals(attributeValue))
			personName.setFamilyName(dataValue);
		else if (XformBuilder.NODE_MIDDLE_NAME.equals(attributeValue))
			personName.setMiddleName(dataValue);
		else if (XformBuilder.NODE_GIVEN_NAME.equals(attributeValue))
			personName.setGivenName(dataValue);
		else if (XformBuilder.NODE_DEGREE.equals(attributeValue))
			personName.setDegree(dataValue);
		else if (XformBuilder.NODE_FAMILY_NAME2.equals(attributeValue))
			personName.setFamilyName2(dataValue);
		else if (XformBuilder.NODE_FAMILY_NAME_PREFIX.equals(attributeValue))
			personName.setFamilyNamePrefix(dataValue);
		else if (XformBuilder.NODE_FAMILY_NAME_SUFFIX.equals(attributeValue))
			personName.setFamilyNameSuffix(dataValue);
		else if (XformBuilder.NODE_PREFIX.equals(attributeValue))
			personName.setPrefix(dataValue);
		else
			throw new APIException("Cannot find person name called = " + attributeValue);
	}
	
	private static void updatePersonAddress(Patient patient, String attributeValue, String dataValue, Element node) {
		boolean addressIsNew = false;
		PersonAddress pa = patient.getPersonAddress();
		if (pa == null) {
			pa = new PersonAddress();
			pa.setDateCreated(new Date());
			pa.setCreator(Context.getAuthenticatedUser());
			pa.setPreferred(true);
			addressIsNew = true;
		}
		
		if (XformBuilder.NODE_NAME_ADDRESS1.equals(attributeValue))
			pa.setAddress1(dataValue);
		else if (XformBuilder.NODE_NAME_ADDRESS2.equals(attributeValue))
			pa.setAddress2(dataValue);
		else if (XformBuilder.NODE_NAME_CITY_VILLAGE.equals(attributeValue))
			pa.setCityVillage(dataValue);
		else if (XformBuilder.NODE_NAME_STATE_PROVINCE.equals(attributeValue))
			pa.setStateProvince(dataValue);
		else if (XformBuilder.NODE_NAME_POSTAL_CODE.equals(attributeValue))
			pa.setPostalCode(dataValue);
		else if (XformBuilder.NODE_NAME_COUNTRY.equals(attributeValue))
			pa.setCountry(dataValue);
		else if (XformBuilder.NODE_NAME_LATITUDE.equals(attributeValue))
			pa.setLatitude(dataValue);
		else if (XformBuilder.NODE_NAME_LONGITUDE.equals(attributeValue))
			pa.setLongitude(dataValue);
		else if (XformBuilder.NODE_NAME_COUNTY_DISTRICT.equals(attributeValue))
			pa.setCountyDistrict(dataValue);
		else if (XformBuilder.NODE_NAME_NEIGHBORHOOD_CELL.equals(attributeValue))
			pa.setNeighborhoodCell(dataValue);
		else if (XformBuilder.NODE_NAME_REGION.equals(attributeValue))
			pa.setRegion(dataValue);
		else if (XformBuilder.NODE_NAME_SUBREGION.equals(attributeValue))
			pa.setSubregion(dataValue);
		else if (XformBuilder.NODE_NAME_TOWNSHIP_DIVISION.equals(attributeValue))
			pa.setTownshipDivision(dataValue);
		else
			throw new APIException("Cannot find person address with name = " + attributeValue);
		
		if (addressIsNew) {
			if (pa.getAddress1() == null || pa.getAddress1().trim().length() == 0)
				return;
			
			patient.addAddress(pa);
		}
	}
	
	private static void updatePersonAttribute(Patient patient, String attributeValue, String dataValue, Element node) {
		PersonAttributeType personAttributeType = Context.getPersonService().getPersonAttributeType(
		    Integer.parseInt(attributeValue));
		
		if (personAttributeType == null)
			throw new APIException("Cannot find person attribute type with id = " + attributeValue);
		
		PersonAttribute personAttribute = patient.getAttribute(personAttributeType);
		if (personAttribute == null) {
			personAttribute = new PersonAttribute();
			personAttribute.setDateCreated(new Date());
			personAttribute.setCreator(Context.getAuthenticatedUser());
			personAttribute.setAttributeType(personAttributeType);
			
			//addAttribute will not add if value is not set.
			setPersonAttributeValue(dataValue, personAttribute, personAttributeType);
			patient.addAttribute(personAttribute);
		}
		
		setPersonAttributeValue(dataValue, personAttribute, personAttributeType);
		
		if (StringUtils.isBlank(dataValue)) {
			patient.removeAttribute(personAttribute);
		}
	}
	
	private static void setPersonAttributeValue(String dataValue, PersonAttribute personAttribute, PersonAttributeType personAttributeType) {
		if ("org.openmrs.Concept".equals(personAttributeType.getFormat())) {
			
			if (!dataValue.contains("^")) {
				return; //not edited
			}
			
			dataValue = XformBuilder.getConceptId(dataValue).toString();
		}
		
		personAttribute.setValue(dataValue);
	}
}
