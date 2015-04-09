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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kxml2.kdom.Element;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.xforms.util.DOMUtil;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.util.OpenmrsConstants.PERSON_TYPE;



/**
 * 
 * @author daniel
 *
 */
public class XformPatientEdit {

	private static final Log log = LogFactory.getLog(XformPatientEdit.class);


	public static boolean isPatientElement(Element element){
		return (element.getName().equalsIgnoreCase(XformBuilder.NODE_PATIENT) && 
				String.valueOf(XformConstants.PATIENT_XFORM_FORM_ID).equals(element.getAttributeValue(null,(XformBuilder.ATTRIBUTE_ID))));
	}


	public static Patient getEditedPatient(HttpServletRequest request, Element rootNode) throws Exception {

		XformObsEdit.retrieveSessionValues(request);
		XformObsEdit.clearSessionData(request, XformConstants.PATIENT_XFORM_FORM_ID);
		
		String patientId = XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_PATIENT_ID);
		Patient patient = Context.getPatientService().getPatient(Integer.parseInt(patientId));

		PersonName personName = patient.getPersonName();
		personName.setFamilyName(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_FAMILY_NAME));
		personName.setMiddleName(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_MIDDLE_NAME));
		personName.setGivenName(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_GIVEN_NAME));		
		personName.setDateChanged(new Date());
		personName.setChangedBy(Context.getAuthenticatedUser());
		
		personName.setDegree(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_DEGREE));
		personName.setFamilyName2(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_FAMILY_NAME2));
		personName.setFamilyNamePrefix(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_FAMILY_NAME_PREFIX));
		personName.setFamilyNameSuffix(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_FAMILY_NAME_SUFFIX));
		personName.setPrefix(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_PREFIX));

		String val = XformBuilder.getNodeValue(rootNode,XformBuilder.NODE_BIRTH_DATE);
		try{ 
			patient.setBirthdate(XformsUtil.fromSubmitString2Date(val)); 
		} catch(Exception e){log.error(val,e); }

		patient.setBirthdateEstimated("true".equals(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_BIRTH_DATE_ESTIMATED)));

		patient.setGender(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_GENDER));
		patient.setDateChanged(new Date());
		patient.setChangedBy(Context.getAuthenticatedUser());

		PatientIdentifier patientIdentifier = patient.getPatientIdentifier();
		patientIdentifier.setIdentifier(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_IDENTIFIER));
		Location location =  Context.getLocationService().getLocation(Integer.parseInt(
				XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_LOCATION_ID)));
		patientIdentifier.setLocation(location);

		PatientIdentifierType identifierType = Context.getPatientService().getPatientIdentifierType(
				Integer.parseInt(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_IDENTIFIER_TYPE_ID)));
		patientIdentifier.setIdentifierType(identifierType);

		savePersonAttributes(patient,rootNode);

		savePersonAddresses(patient,rootNode);

		return patient;
	}

	private static void savePersonAddresses(Patient patient, Element rootNode) throws Exception {

		boolean addressIsNew = false;
		PersonAddress pa = patient.getPersonAddress();
		if(pa == null){
			pa = new PersonAddress();
			pa.setDateCreated(new Date());
			pa.setCreator(Context.getAuthenticatedUser());
			pa.setPreferred(true);
			addressIsNew = true;
		}

		pa.setAddress1(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_ADDRESS1));
		pa.setAddress2(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_ADDRESS2));
		pa.setCityVillage(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_CITY_VILLAGE));
		pa.setStateProvince(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_STATE_PROVINCE));
		pa.setPostalCode(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_POSTAL_CODE));
		pa.setCountry(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_COUNTRY));
		pa.setLatitude(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_LATITUDE));
		pa.setLongitude(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_LONGITUDE));
		pa.setCountyDistrict(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_COUNTY_DISTRICT));
		pa.setNeighborhoodCell(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_NEIGHBORHOOD_CELL));
		pa.setRegion(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_REGION));
		pa.setSubregion(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_SUBREGION));
		pa.setTownshipDivision(XformBuilder.getNodeValue(rootNode, XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + XformBuilder.NODE_NAME_TOWNSHIP_DIVISION));
				
		if(addressIsNew){
			if(pa.getAddress1() == null || pa.getAddress1().trim().length() == 0)
				return;
			
			patient.addAddress(pa);
		}
	}

	private static void savePersonAttributes(Patient patient, Element rootNode) throws Exception {

		List<String> complexObs = DOMUtil.getModelComplexObsNodeNames(XformConstants.PATIENT_XFORM_FORM_ID);
		List<String> dirtyComplexObs = XformObsEdit.getEditedComplexObsNames();

		PersonService personService = Context.getPersonService();
		for (PersonAttributeType type : personService.getPersonAttributeTypes(PERSON_TYPE.PERSON, null)) {
			String name = "person_attribute"+type.getPersonAttributeTypeId();
			Element element = XformBuilder.getElement(rootNode,name);

			if(element == null)
				continue;

			String value = XformBuilder.getTextValue(element);

			if(complexObs.contains(name)){				
				if(!dirtyComplexObs.contains(name))
					continue;

				value = XformObsEdit.saveComplexObs(name,value,rootNode);
			}

			PersonAttribute personAttribute = patient.getAttribute(type.getPersonAttributeTypeId());

			if(personAttribute == null){
				personAttribute = new PersonAttribute();
				personAttribute.setDateCreated(new Date());
				personAttribute.setCreator(Context.getAuthenticatedUser());
				personAttribute.setAttributeType(type);
				
				//addAttribute will not add if value is not set.
				personAttribute.setValue(value);
				patient.addAttribute(personAttribute);
			}
			
			personAttribute.setValue(value);
			
			if(value == null || value.length() == 0)
				patient.removeAttribute(personAttribute);
		}
	}
}
