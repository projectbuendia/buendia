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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7InQueue;
import org.openmrs.module.xforms.formentry.FormEntryQueue;
import org.openmrs.module.xforms.formentry.FormEntryQueueProcessor;
import org.openmrs.module.xforms.formentry.FormEntryWrapper;
import org.openmrs.module.xforms.formentry.HL7InQueueProcessor;
import org.openmrs.module.xforms.model.PersonRepeatAttribute;
import org.openmrs.module.xforms.util.DOMUtil;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.util.OpenmrsConstants.PERSON_TYPE;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;


/**
 * Processes Xforms Queue entries.
 * When the processing is successful, the queue entry is submitted to the FormEntry Queue.
 * For unsuccessful processing, the queue entry is put in the Xforms error folder.
 * 
 * @author Daniel Kayiwa
 * @version 1.0
 */
@Transactional
public class XformsQueueProcessor {

	private static final Log log = LogFactory.getLog(XformsQueueProcessor.class);
	private static Boolean isRunning = false; // allow only one running
	private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

	private DocumentBuilder db;

	// Instance of form entry processor
	private FormEntryQueueProcessor formEntryProcessor = null;

	// Instance of hl7 processor
	private static HL7InQueueProcessor hl7Processor = null;


	public XformsQueueProcessor(){
		if (formEntryProcessor == null)
			formEntryProcessor = new FormEntryQueueProcessor();

		if (hl7Processor == null) 
			hl7Processor = new HL7InQueueProcessor();

		try{
			db = dbf.newDocumentBuilder();
		}
		catch(Exception e){
			log.error(Context.getMessageSourceService().getMessage("xforms.problemDocumentBuilder"), e);
		}
	}

	/**
	 * Starts up a thread to process all existing xforms queue entries
	 */
	public void processXformsQueue() throws APIException {
		synchronized (isRunning) {
			if (isRunning) {
				log.warn(Context.getMessageSourceService().getMessage("xforms.problemXformsQueue"));
				return;
			}
			isRunning = true;
		}
		try {			
			File queueDir = XformsUtil.getXformsQueueDir();
			for (File file : queueDir.listFiles()) {
				try{
					processXForm(XformsUtil.readFile(file.getAbsolutePath()), file.getAbsolutePath(), false, null);
				}
				catch(Exception e){
					log.error(Context.getMessageSourceService().getMessage("xforms.problemProcessingXform") + file.getAbsolutePath(), e); 
				}
			}
		}
		catch(Exception e){
			log.error(Context.getMessageSourceService().getMessage("xforms.problemProcessingQueue"), e); 
		}
		finally {
			isRunning = false;
		}
	}


	/**
	 * Saves obs entered during new patient registration (if any).
	 * 
	 * @param patient the patient whose obs to save.
	 * @param root
	 * @param pathName
	 * @param propagateErrors
	 * @throws Exception
	 */
	private void saveNewPatientEncounterIfAny(Patient patient, Element root, String pathName, boolean propagateErrors) throws Exception {
		NodeList elemList = root.getElementsByTagName("form");
		if (!(elemList != null && elemList.getLength() > 0)) 
			return;

		Element formNode = (Element)elemList.item(0);
		String id = formNode.getAttribute("id");
		String name = formNode.getAttribute("name");
		if(!(id != null && name != null && id.trim().length() > 0 && name.trim().length() > 0))
			return;

		setNodeValue(formNode, XformBuilder.NODE_PATIENT_PATIENT_ID, patient.getPatientId().toString());
		setNodeValue(formNode, XformBuilder.NODE_ENCOUNTER_LOCATION_ID, patient.getIdentifiers().iterator().next().getLocation().getLocationId().toString());

		String xml = XformsUtil.doc2String(formNode);

		if(isRemoteFormEntry()){
			FormEntryWrapper.createFormEntryQueue(xml);
		}
		else{
			processDoc(xml, pathName, propagateErrors);
		}
	}

	/**
	 * Processes an xforms model.
	 * 
	 * @param xml the xml of the xforms model.
	 * @param pathName the full path and name of file form which this xform model has been read. null can be passed if the form does not come from a file.
	 */
	public void processXForm(String xml, String pathName, boolean propagateErrors,HttpServletRequest request) throws Exception {
		String xmlOriginal = xml;
		Patient patient = null;
		try{	
			Document doc = db.parse(IOUtils.toInputStream(xml,XformConstants.DEFAULT_CHARACTER_ENCODING));
			Element root = doc.getDocumentElement();

			//Check if new patient doc
			if(DOMUtil.isPatientDoc(doc)){
				patient = saveNewPatient(root,getCreator(doc),propagateErrors,request);
				if(patient == null)
					saveFormInError(xml,pathName, null);
				else{
					saveNewPatientEncounterIfAny(patient, root, pathName, propagateErrors);
					
					if(!isRemoteFormEntry()){
						saveFormInArchive(xml,pathName);
					}
				}
			} //Check if encounter doc
			else if(DOMUtil.isEncounterDoc(doc))
				submitXForm(doc,xml,pathName,true,propagateErrors);
			else{
				//Must be combined doc (new patient and encounter) where doc node is openmrs_data

				//<?xml version="1.0" encoding="UTF-8" ?>
				//<openmrs_data>
				//   <patient/>
				//   <form/>
				//   <form/>
				//  < etc.../>
				//</openmrs_data>

				NodeList list = doc.getDocumentElement().getChildNodes();
				for(int index = 0; index < list.getLength(); index++){
					Node node = list.item(index);
					if(node.getNodeType() != Node.ELEMENT_NODE)
						continue;

					//Assuming patient node is the first in the combined document, such that we get the patient id.
					if(DOMUtil.isPatientElementDoc((Element)node)){
						patient = saveNewPatient((Element)node,getCreator(doc),propagateErrors,request);
						if(patient == null){
							saveFormInError(xml,pathName, null);
							return;
						}	
					}
					else{
						setNewPatientId((Element)node,patient.getPatientId());
						Document encounterDoc = createNewDocFromNode(db,(Element)node);
						xml = XformsUtil.doc2String(encounterDoc);
						submitXForm(encounterDoc,xml,pathName,false,propagateErrors);
					}
				}

				saveFormInArchive(xmlOriginal,pathName);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);

			//If we created a new patient, remove them. XFRM-135
			if (patient != null)
				Context.getPatientService().purgePatient(patient);
			
			//TODO Joaquin had a problem where there were errors but form was not saved in error folder
			//so lets enforce this for now regardless of the error flag
			//if(!propagateErrors)
				saveFormInError(xmlOriginal,pathName, null);
			//else
				throw e;
		}
	}

	private Document createNewDocFromNode(DocumentBuilder db, Element element){
		Document doc = db.newDocument();
		doc.appendChild(doc.adoptNode(element));
		return doc;
	}

	/**
	 * Sets the patientid node to the value of the patient_id as got from the server.
	 * 
	 * @param root - the root element of the patientid node to set.
	 * @return - true if set, else false.
	 */
	private void setNewPatientId(Element root, Integer patientId){
		/*try{
			NodeList elemList = root.getElementsByTagName(XformBuilder.NODE_PATIENT_PATIENT_ID);
			if (!(elemList != null && elemList.getLength() > 0)) 
				return;

			elemList.item(0).setTextContent(patientId.toString());
		}
		catch(Exception e){
			log.error(e.getMessage(),e);
		}*/

		setNodeValue(root, XformBuilder.NODE_PATIENT_PATIENT_ID, patientId.toString());
	}

	private void setNodeValue(Element root, String name, String value){
		try{
			NodeList elemList = root.getElementsByTagName(name);
			if (!(elemList != null && elemList.getLength() > 0)) 
				return;

			elemList.item(0).setTextContent(value);
		}
		catch(Exception e){
			log.error(e.getMessage(),e);
		}
	}

	private boolean isRemoteFormEntry(){
		return Context.getAdministrationService().getGlobalProperty("xforms.isRemoteFormEntry","false").equals("true");
	}

	/**
	 * Submits a form to the form entry queue for further processing.
	 * 
	 * @param doc
	 * @param xml
	 * @param pathName
	 * @param archive
	 */
	private void submitXForm(Document doc, String xml, String pathName, boolean archive, boolean propagateErrors) throws Exception {
		String xmlOriginal = xml;
		try{
			fillPatientIdIfMissing(doc);
			saveComplexObs(doc,true);
			setMultipleSelectValues(doc.getDocumentElement());
			xml = XformsUtil.doc2String(doc);

			if(isRemoteFormEntry()){
				FormEntryWrapper.createFormEntryQueue(xml);
			}
			else{
				processDoc(xml, pathName, propagateErrors);
				
				String patientid = DOMUtil.getElementValue(doc, XformBuilder.NODE_PATIENT_PATIENT_ID);
				Patient patient = XformObsPatientEdit.updatePatientDemographics(patientid, xml);
				
				RelativeSubmission.submit(xml, patient);
				
				if(archive)
					saveFormInArchive(xmlOriginal, pathName);
			}

		} catch (Exception e) {

			log.error(e.getMessage(), e);

			//TODO Joaquin had a problem where there were errors but form was not saved in error folder
			//so lets enforce this for now regardless of the error flag
			//if(!propagateErrors)
				saveFormInError(xmlOriginal,pathName, e);
			//else
				throw e;
		}
	}

	private void processDoc(String xml, String pathName, boolean propagateErrors) throws Exception {
		FormEntryQueue formEntryQueue = new FormEntryQueue();
		formEntryQueue.setCreator(Context.getAuthenticatedUser());
		formEntryQueue.setDateCreated(new Date());
		formEntryQueue.setFormData(xml);
		formEntryQueue.setFileSystemUrl(pathName);

		HL7InQueue hl7InQueue = formEntryProcessor.transformFormEntryQueue(formEntryQueue, propagateErrors);
		hl7Processor.processHL7InQueue(hl7InQueue,propagateErrors);
	}


	/**
	 * Archives a submitted form after processing.
	 * 
	 * @param xml - the form data.
	 * @param folder - the folder to save in.
	 * @param queuePathName - the path and name of this file in the queue. If you dont supply this,
	 * 						  a new radom file is created in this folder, else the a file with the
	 * 						  same name as the queued one is created in this folder.
	 */
	private String saveForm(String xml,File folder,String queuePathName){
		String pathName;// = folder.getAbsolutePath()+File.separatorChar+XformsUtil.getRandomFileName()+XformConstants.XML_FILE_EXTENSION;
		if(queuePathName == null)
			pathName = OpenmrsUtil.getOutFile(folder, new Date(), Context.getAuthenticatedUser()).getAbsolutePath();
		else
			pathName = folder.getAbsolutePath()+File.separatorChar+queuePathName.substring(queuePathName.lastIndexOf(File.separatorChar)+1);

		try{
			FileWriter writter = new FileWriter(pathName, false);
			writter.write(xml);
			writter.close();

			if(queuePathName != null){
				try{
					File file = new File(queuePathName);
					if(!file.delete())
						file.deleteOnExit();
				}catch(Exception e){
					log.error(e.getMessage(),e);
				}
			}
		}
		catch(Exception e){
			log.error(e.getMessage(),e);
		}

		return pathName;
	}

	/**
	 * Saves an xform in the xforms archive.
	 * 
	 * @param xml - the xml of the xform.
	 * @param queuePathName - the queue full path and file name of this xform.
	 * @return - the archive full path and file name.
	 */
	private String saveFormInArchive(String xml,String queuePathName){
		return saveForm(xml,XformsUtil.getXformsArchiveDir(new Date()),queuePathName);
	}

	/**
	 * Saves an xform in the errors folder.
	 * 
	 * @param xml - the xml of the xform.
	 * @param queuePathName - the queue full path and file name of this xform.
	 * @param exception TODO
	 * @return - the error full path and file name.
	 */
	private String saveFormInError(String xml,String queuePathName, Exception exception){
		String errorPath = saveForm(xml,XformsUtil.getXformsErrorDir(),queuePathName);
		
		Context.getService(XformsService.class).sendStacktraceToAdminByEmail(Context.getMessageSourceService().getMessage("xforms.problemFailedProcessForm") + errorPath, exception);
		
		return errorPath;
	}

	/** 
	 * Creates a new patient from an xform create new patient document.
	 * 
	 * @param doc - the document.
	 * @param creator - the logged on user.
	 * @return - true if the patient is created successfully, else false.
	 */
	private Patient saveNewPatient(Element root, User creator, boolean propagateErrors, HttpServletRequest request) throws Exception{		
		PatientService patientService = Context.getPatientService();
		XformsService xformsService = (XformsService)Context.getService(XformsService.class);

		Patient pt = new Patient();
		pt.setCreator(creator);
		pt.setDateCreated(new Date());	

		PersonName pn = new PersonName();
		pn.setGivenName(DOMUtil.getElementValue(root,XformBuilder.NODE_GIVEN_NAME));
		pn.setFamilyName(DOMUtil.getElementValue(root,XformBuilder.NODE_FAMILY_NAME));
		pn.setMiddleName(DOMUtil.getElementValue(root,XformBuilder.NODE_MIDDLE_NAME));
		
		pn.setDegree(DOMUtil.getElementValue(root, XformBuilder.NODE_DEGREE));
		pn.setFamilyName2(DOMUtil.getElementValue(root, XformBuilder.NODE_FAMILY_NAME2));
		pn.setFamilyNamePrefix(DOMUtil.getElementValue(root, XformBuilder.NODE_FAMILY_NAME_PREFIX));
		pn.setFamilyNameSuffix(DOMUtil.getElementValue(root, XformBuilder.NODE_FAMILY_NAME_SUFFIX));
		pn.setPrefix(DOMUtil.getElementValue(root, XformBuilder.NODE_PREFIX));
		
		pn.setPreferred(true);
		pn.setCreator(creator);
		pn.setDateCreated(pt.getDateCreated());
		pt.addName(pn);

		pt.setBirthdateEstimated("true".equals(DOMUtil.getElementValue(root, XformBuilder.NODE_BIRTH_DATE_ESTIMATED)));

		String val = DOMUtil.getElementValue(root,XformBuilder.NODE_BIRTH_DATE);
		if(val != null && val.length() > 0)
			try{ pt.setBirthdate(XformsUtil.fromSubmitString2Date(val)); } catch(Exception e){log.error(val,e); }

			pt.setGender(DOMUtil.getElementValue(root,XformBuilder.NODE_GENDER));	

			PatientIdentifier identifier = new PatientIdentifier();
			identifier.setCreator(creator);
			identifier.setDateCreated(pt.getDateCreated());
			identifier.setIdentifier(DOMUtil.getElementValue(root,XformBuilder.NODE_IDENTIFIER));
			int id = Integer.parseInt(DOMUtil.getElementValue(root,XformBuilder.NODE_IDENTIFIER_TYPE_ID));
			PatientIdentifierType identifierType = patientService.getPatientIdentifierType(id);
			identifier.setIdentifierType(identifierType);
			identifier.setLocation(getLocation(DOMUtil.getElementValue(root,XformBuilder.NODE_LOCATION_ID)));
			identifier.setPreferred(true);
			pt.addIdentifier(identifier);

			addPersonAttributes(pt, root, xformsService, creator);

			addPersonAddresses(pt, root, creator);
			
			addOtherIdentifiers(pt, root, creator, patientService, propagateErrors, request);

			Patient pt2 = patientService.identifierInUse(identifier.getIdentifier(),identifier.getIdentifierType(),pt);
			if(pt2 == null){
				pt = patientService.savePatient(pt);
				addPersonRepeatAttributes(pt,root,xformsService);

				if(request != null)
					request.setAttribute(XformConstants.REQUEST_ATTRIBUTE_ID_PATIENT_ID, pt.getPatientId().toString());

				return pt;
			}
			else if(rejectExistingPatientCreation()){
				String message = Context.getMessageSourceService().getMessage("xforms.problemPatientExists")+identifier.getIdentifier()+Context.getMessageSourceService().getMessage("xforms.accepted");
				log.error(message);

				if(request != null)
					request.setAttribute(XformConstants.REQUEST_ATTRIBUTE_ID_ERROR_MESSAGE, message);

				if(propagateErrors)
					throw new Exception(message);

				return null;
			}
			else{
				String message = Context.getMessageSourceService().getMessage("xforms.problemPatientExists")+identifier.getIdentifier()+Context.getMessageSourceService().getMessage("xforms.accepted");
				log.warn(message);

				if(request != null)
					request.setAttribute(XformConstants.REQUEST_ATTRIBUTE_ID_ERROR_MESSAGE, message);

				if(propagateErrors)
					throw new Exception(message);

				return pt;
			}
	}

	private void addPersonAttributes(Patient pt, Element root,XformsService xformsService, User creator) throws Exception{
		//First translate complex obs to file pointers;
		saveComplexObs(root.getOwnerDocument(),false);

		// look for person attributes in the xml doc and save to person
		List<PersonAttributeType> personAttributeTypes = Context.getPersonService().getPersonAttributeTypes(PERSON_TYPE.PERSON, null);
		for (PersonAttributeType type : personAttributeTypes) {
			NodeList nodes = root.getElementsByTagName("person_attribute"+type.getPersonAttributeTypeId());

			if(nodes == null || nodes.getLength() == 0)
				continue;

			String value = ((Element)nodes.item(0)).getTextContent();
			if(value == null || value.length() == 0)
				continue;

			PersonAttribute pa = new PersonAttribute(type, value);
			pa.setCreator(creator);
			pa.setDateCreated(pt.getDateCreated());
			pt.addAttribute(pa);
		}
	}

	private void addPersonAddresses(Patient pt, Element root, User creator) throws Exception{
		PersonAddress pa = new PersonAddress();
		pa.setCreator(creator);
		pa.setDateCreated(pt.getDateCreated());
		pa.setPreferred(true);

		addPersonAddressValue(XformBuilder.NODE_NAME_ADDRESS1, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_ADDRESS2, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_CITY_VILLAGE, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_STATE_PROVINCE, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_POSTAL_CODE, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_COUNTRY, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_LATITUDE, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_LONGITUDE, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_COUNTY_DISTRICT, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_NEIGHBORHOOD_CELL, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_REGION, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_SUBREGION, pa, root);
		addPersonAddressValue(XformBuilder.NODE_NAME_TOWNSHIP_DIVISION, pa, root);

		pt.addAddress(pa);
	}

	private void addPersonAddressValue(String name, PersonAddress pa, Element root) throws Exception{
		NodeList nodes = root.getElementsByTagName(XformBuilder.NODE_NAME_PREFIX_PERSON_ADDRESS + name);
		if(nodes == null || nodes.getLength() == 0)
			return;

		String value = ((Element)nodes.item(0)).getTextContent();
		if(value == null || value.length() == 0)
			return;

		if(name.equals(XformBuilder.NODE_NAME_ADDRESS1))
			pa.setAddress1(value);
		else if(name.equals(XformBuilder.NODE_NAME_ADDRESS2))
			pa.setAddress2(value);
		else if(name.equals(XformBuilder.NODE_NAME_CITY_VILLAGE))
			pa.setCityVillage(value);
		else if(name.equals(XformBuilder.NODE_NAME_STATE_PROVINCE))
			pa.setStateProvince(value);
		else if(name.equals(XformBuilder.NODE_NAME_POSTAL_CODE))
			pa.setPostalCode(value);
		else if(name.equals(XformBuilder.NODE_NAME_COUNTRY))
			pa.setCountry(value);
		else if(name.equals(XformBuilder.NODE_NAME_LATITUDE))
			pa.setLatitude(value);
		else if(name.equals(XformBuilder.NODE_NAME_LONGITUDE))
			pa.setLongitude(value);
		else if(name.equals(XformBuilder.NODE_NAME_COUNTY_DISTRICT))
			pa.setCountyDistrict(value);
		else if(name.equals(XformBuilder.NODE_NAME_NEIGHBORHOOD_CELL))
			pa.setNeighborhoodCell(value);
		else if(name.equals(XformBuilder.NODE_NAME_REGION))
			pa.setRegion(value);
		else if(name.equals(XformBuilder.NODE_NAME_SUBREGION))
			pa.setSubregion(value);
		else if(name.equals(XformBuilder.NODE_NAME_TOWNSHIP_DIVISION))
			pa.setTownshipDivision(value);
	}

	private void addPersonRepeatAttributes(Patient pt, Element root,XformsService xformsService){
		NodeList nodes = root.getChildNodes();
		if(nodes == null)
			return;

		for(int index = 0; index < nodes.getLength(); index++){
			Node node = nodes.item(index);
			if(node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String name = node.getNodeName();
			if(name.startsWith("person_attribute_repeat_section")){
				String attributeId = name.substring("person_attribute_repeat_section".length());
				addPersonRepeatAttribute(pt,node,attributeId,xformsService);
			}
		}
	}

	private void addPersonRepeatAttribute(Patient pt,Node repeatNode,String attributeId,XformsService xformsService){
		NodeList nodes = repeatNode.getChildNodes();
		if(repeatNode == null)
			return;

		for(int index = 0; index < nodes.getLength(); index++){
			Node node = nodes.item(index);
			if(node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String name = node.getNodeName();
			if(name.startsWith("person_attribute"))		
				addPersonRepeatAttributeValues(pt,node,attributeId,xformsService,index+1);
		}
	}

	private void addPersonRepeatAttributeValues(Patient pt,Node repeatNode,String attributeId,XformsService xformsService, int displayOrder){
		if(repeatNode == null)
			return;
		
		NodeList nodes = repeatNode.getChildNodes();

		for(int index = 0; index < nodes.getLength(); index++){
			Node node = nodes.item(index);
			if(node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String name = node.getNodeName();
			if(name.startsWith("person_attribute_concept")){
				String valueId = name.substring("person_attribute_concept".length());

				PersonRepeatAttribute personRepeatAttribute = new PersonRepeatAttribute();
				personRepeatAttribute.setPersonId(pt.getPersonId());
				personRepeatAttribute.setCreator(Context.getAuthenticatedUser().getUserId());
				personRepeatAttribute.setDateCreated(pt.getDateCreated());
				personRepeatAttribute.setValue(node.getTextContent());
				personRepeatAttribute.setValueId(Integer.parseInt(valueId));
				personRepeatAttribute.setValueIdType(PersonRepeatAttribute.VALUE_ID_TYPE_CONCEPT);
				personRepeatAttribute.setValueDisplayOrder(displayOrder);
				personRepeatAttribute.setAttributeTypeId(Integer.parseInt(attributeId));

				xformsService.savePersonRepeatAttribute(personRepeatAttribute);
			}
		}
	}

	/**
	 * Check if we are to reject forms for patients considered new when they already exist, 
	 * by virture of patient identifier.
	 * @return true if we are to reject, else false.
	 */
	private boolean rejectExistingPatientCreation(){
		String reject = Context.getAdministrationService().getGlobalProperty(XformConstants.GLOBAL_PROP_KEY_REJECT_EXIST_PATIENT_CREATE,XformConstants.DEFAULT_REJECT_EXIST_PATIENT_CREATE);
		return !("false".equalsIgnoreCase(reject));
	}

	/**
	 * Gets a location object given a locaton id
	 * 
	 * @param locationId - the id.
	 * @return
	 */
	private Location getLocation(String locationId){
		return Context.getLocationService().getLocation(Integer.parseInt(locationId));
	}

	private User getCreator(Document doc){
		//return Context.getAuthenticatedUser();
		NodeList elemList = doc.getElementsByTagName(XformConstants.NODE_ENTERER);
		if (elemList != null && elemList.getLength() > 0) {
			String s = ((Element)elemList.item(0)).getTextContent();
			User user = Context.getUserService().getUser(Integer.valueOf(s.substring(0,s.indexOf('^'))));
			return user;
		}
		return null;
	}

	/**
	 * Converts xforms multiple select answer values to the format expected by
	 * the openmrs form model.
	 * 
	 * @param parentNode - the parent node of the document.
	 */
	private void setMultipleSelectValues(Node parentNode){
		NodeList nodes = parentNode.getChildNodes();
		for(int i=0; i<nodes.getLength(); i++){
			Node node = nodes.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			if(isMultipleSelectNode(node))
				setMultipleSelectNodeValues(node);
			setMultipleSelectValues(node);
		}
	}

	/** 
	 * Gets the values of a multiple select node.
	 * 
	 * @param parentNode- the node
	 * @return - a sting with values separated by space.
	 */
	private String getMultipleSelectNodeValue(Node parentNode){
		String value = null;

		NodeList nodes = parentNode.getChildNodes();
		for(int i=0; i<nodes.getLength(); i++){
			Node node = nodes.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			String name = node.getNodeName();
			if(name != null && name.equalsIgnoreCase(XformBuilder.NODE_XFORMS_VALUE)){
				value = node.getTextContent();
				parentNode.removeChild(node);
				break;
			}
		}

		return value;
	}

	/**
	 * Sets the values of an openmrs multiple select node.
	 * 
	 * @param parentNode - the node.
	 */
	private void setMultipleSelectNodeValues(Node parentNode){
		String values = getMultipleSelectNodeValue(parentNode);
		if(values == null || values.length() == 0)
			return;

		String[] valueArray = values.split(XformBuilder.MULTIPLE_SELECT_VALUE_SEPARATOR);

		NodeList nodes = parentNode.getChildNodes();
		for(int i=0; i<nodes.getLength(); i++){
			Node node = nodes.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String name = node.getNodeName();
			if(name.equalsIgnoreCase(XformBuilder.NODE_DATE) || name.equalsIgnoreCase(XformBuilder.NODE_TIME) || 
					name.equalsIgnoreCase(XformBuilder.NODE_VALUE) || name.equalsIgnoreCase(XformBuilder.NODE_XFORMS_VALUE))
				continue;
			setMultipleSelectNodeValue(node,valueArray);
		}
	}

	/**
	 * Sets the value of an openmrs multiple select node.
	 * 
	 * @param node - the multiple select node.
	 * @param valueArray - an array of selected values.
	 */
	private void setMultipleSelectNodeValue(Node node,String[] valueArray){
		for(String value : valueArray){
			if(!value.equalsIgnoreCase(node.getNodeName()))
				continue;
			node.setTextContent(XformBuilder.VALUE_TRUE);
			return;
		}

		node.setTextContent(XformBuilder.VALUE_FALSE);
	}

	/**
	 * Checks if a node is multiple select.
	 * 
	 * @param node - the node to check.
	 * @return - true if it is a multiple select node, else false.
	 */
	private boolean isMultipleSelectNode(Node node){
		boolean multipSelect = false;

		NamedNodeMap attributes = node.getAttributes();
		if(attributes != null){
			Node multipleValue = attributes.getNamedItem(XformBuilder.ATTRIBUTE_MULTIPLE);
			if(attributes.getNamedItem(XformBuilder.ATTRIBUTE_OPENMRS_CONCEPT) != null &&  multipleValue != null && multipleValue.getNodeValue().equals("1"))
				multipSelect = true;
		}

		return multipSelect;
	}


	private void fillPatientIdIfMissing(Document doc) throws Exception{
		String patientid = DOMUtil.getElementValue(doc,XformBuilder.NODE_PATIENT_PATIENT_ID);
		if(patientid != null && patientid.trim().length() > 0)
			return; //patient id is properly filled. may need to check if the patient exists

		//Check if patient identifier is filled.
		String patientIdentifier = getPatientIdentifier(doc);;
		if(patientIdentifier == null || patientIdentifier.trim().length() == 0)
			throw new Exception(Context.getMessageSourceService().getMessage(".expectedPatientID"));

		List<Patient> patients = Context.getPatientService().getPatients(null, patientIdentifier, null);
		if(patients != null && patients.size() > 1)
			throw new Exception(Context.getMessageSourceService().getMessage("xformsmoreThanOneID") + patientIdentifier); 

		if(patients != null && patients.size() == 1){
			DOMUtil.setElementValue(doc.getDocumentElement(), XformBuilder.NODE_PATIENT_PATIENT_ID, patients.get(0).getPatientId().toString());
			return;
		}

		//Check if patient identifier type is filled
		String identifierType = DOMUtil.getElementValue(doc, XformBuilder.NODE_PATIENT_IDENTIFIER_TYPE);
		if(identifierType == null || identifierType.trim().length() == 0){
			identifierType = DOMUtil.getElementValue(doc, XformBuilder.NODE_PATIENT_IDENTIFIER_TYPE_ID);
			if(identifierType == null || identifierType.trim().length() == 0){
				identifierType = Context.getAdministrationService().getGlobalProperty("xforms.new_patient_identifier_type_id", null);
				if(identifierType == null || identifierType.trim().length() == 0)
					throw new Exception(Context.getMessageSourceService().getMessage("xforms.expectedPatientIDType"));
			}
		}

		//Check if family name is filled.
		String familyName = DOMUtil.getElementValue(doc, XformBuilder.NODE_PATIENT_FAMILY_NAME);
		if(familyName == null || familyName.trim().length() == 0)
			throw new Exception(Context.getMessageSourceService().getMessage("xforms.expectedFamilyName"));

		//Check if gender is filled
		String gender = DOMUtil.getElementValue(doc, XformBuilder.NODE_PATIENT_GENDER);
		if(gender == null || gender.trim().length() == 0)
			throw new Exception(Context.getMessageSourceService().getMessage("xforms.expectedGender"));

		//Check if birth date is filled
		String birthDate = DOMUtil.getElementValue(doc, XformBuilder.NODE_PATIENT_BIRTH_DATE);
		if(birthDate == null || birthDate.trim().length() == 0)
			throw new Exception(Context.getMessageSourceService().getMessage("xforms.expectedBirthdate"));

		Patient patient = new Patient();
		patient.setCreator(getCreator(doc));
		patient.setDateCreated(new Date());	
		patient.setGender(gender);

		PersonName pn = new PersonName();

		pn.setFamilyName(familyName);
		pn.setGivenName(DOMUtil.getElementValue(doc, XformBuilder.NODE_PATIENT_GIVEN_NAME));
		pn.setMiddleName(DOMUtil.getElementValue(doc, XformBuilder.NODE_PATIENT_MIDDLE_NAME));

		pn.setCreator(patient.getCreator());
		pn.setDateCreated(patient.getDateCreated());
		patient.addName(pn);

		PatientIdentifier identifier = new PatientIdentifier();
		identifier.setCreator(patient.getCreator());
		identifier.setDateCreated(patient.getDateCreated());
		identifier.setIdentifier(patientIdentifier.toString());

		int id = Integer.parseInt(identifierType);
		PatientIdentifierType idtfType = Context.getPatientService().getPatientIdentifierType(id);
		if(idtfType == null)
			throw new Exception(Context.getMessageSourceService().getMessage("xforms.expectedIdentifier"));
		
		identifier.setIdentifierType(idtfType);
		identifier.setLocation(getLocation(DOMUtil.getElementValue(doc, XformBuilder.NODE_ENCOUNTER_LOCATION_ID)));
		identifier.setPreferred(true);
		patient.addIdentifier(identifier);

		patient.setBirthdate(XformsUtil.fromSubmitString2Date(birthDate.toString()));
		patient.setBirthdateEstimated("true".equals(DOMUtil.getElementValue(doc, XformBuilder.NODE_PATIENT_BIRTH_DATE_ESTIMATED)));

		addPersonAttributes(patient, doc.getDocumentElement(), Context.getService(XformsService.class), patient.getCreator());
		addPersonAddresses(patient, doc.getDocumentElement(), patient.getCreator());
		
		Context.getPatientService().savePatient(patient);
		DOMUtil.setElementValue(doc.getDocumentElement(), XformBuilder.NODE_PATIENT_PATIENT_ID, patient.getPatientId().toString());
	
		//TODO May need to call addPersonRepeatAttributes(pt,root,xformsService);
	}

	private String getPatientIdentifier(Document doc){
		NodeList elemList = doc.getDocumentElement().getElementsByTagName("patient");
		if (!(elemList != null && elemList.getLength() > 0))
			return null;

		Element patientNode = (Element)elemList.item(0);

		NodeList children = patientNode.getChildNodes();
		int len = patientNode.getChildNodes().getLength();
		for(int index=0; index<len; index++){
			Node child = children.item(index);
			if(child.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if("patient_identifier".equalsIgnoreCase(((Element)child).getAttribute("openmrs_table")) &&
					"identifier".equalsIgnoreCase(((Element)child).getAttribute("openmrs_attribute")))
				return child.getTextContent();
		}

		return null;
	}

	private void saveComplexObs(Document doc, boolean useValueNode) throws Exception {
		List<String> names = DOMUtil.getModelComplexObsNodeNames(doc.getDocumentElement().getAttribute("id"));
		for(String name : names)
			saveComplexObsValue(DOMUtil.getElement(doc, name),useValueNode);
	}

	private void saveComplexObsValue(Element element, boolean useValueNode) throws Exception {
		String value = null;
		if(useValueNode)
			value = DOMUtil.getElementValue(element, "value");
		else
			value = element.getTextContent();

		if(value == null || value.trim().length() == 0)
			return;

		byte[] bytes = Base64.decode(value);

		String path = element.getOwnerDocument().getDocumentElement().getAttribute("name");

		path += File.separatorChar + element.getNodeName();

		File file = OpenmrsUtil.getOutFile(XformsUtil.getXformsComplexObsDir(path), new Date(), Context.getAuthenticatedUser());
		FileOutputStream writter = new FileOutputStream(file);
		writter.write(bytes);
		writter.close();

		if(useValueNode)
			DOMUtil.setElementValue(element, "value", file.getAbsolutePath());
		else
			element.setTextContent(file.getAbsolutePath());

		//System.out.println("complex obs value = " + file.getAbsolutePath());
	}
	
	private void addOtherIdentifiers(Patient pt, Element root, User creator, PatientService patientService, boolean propagateErrors, HttpServletRequest request) throws Exception{
		NodeList nodes = root.getElementsByTagName(XformBuilder.NODE_NAME_OTHER_IDENTIFIERS);
		if(nodes == null || nodes.getLength() == 0)
			return;
		
		for(int index = 0; index < nodes.getLength(); index++){
			addOtherIdentifier((Element)nodes.item(index), creator, pt, patientService, propagateErrors, request);
		}
	}
	
	private void addOtherIdentifier(Element root, User creator, Patient pt, PatientService patientService, boolean propagateErrors, HttpServletRequest request) throws Exception {
		//Look for identifier value.
		NodeList nodes = root.getElementsByTagName(XformBuilder.NODE_NAME_OTHER_IDENTIFIER);
		if(nodes == null || nodes.getLength() == 0)
			return; //no identifier node found, possibly deleted.
		
		String identifierValue = nodes.item(0).getTextContent();
		if(identifierValue == null || identifierValue.trim().length() == 0)
			return; //no identifier value found.
		
		//Look for identifier type id
		nodes = root.getElementsByTagName(XformBuilder.NODE_NAME_OTHER_IDENTIFIER_TYPE_ID);
		if(nodes == null || nodes.getLength() == 0)
			reportError(Context.getMessageSourceService().getMessage("xforms.formShouldHaveID") + identifierValue,  propagateErrors, request); 
		
		String identifierTypeId = nodes.item(0).getTextContent();
		if(identifierTypeId == null || identifierTypeId.trim().length() == 0)
			reportError(Context.getMessageSourceService().getMessage("xforms.selectIdentifierType") + identifierValue,  propagateErrors, request);
		
		//Look for identifier location id
		nodes = root.getElementsByTagName(XformBuilder.NODE_NAME_OTHER_IDENTIFIER_LOCATION_ID);
		if(nodes == null || nodes.getLength() == 0)
			reportError(Context.getMessageSourceService().getMessage("xforms.shouldHaveLocationType") + identifierValue,  propagateErrors, request); 
		
		String identifierLocationId = nodes.item(0).getTextContent();
		if(identifierLocationId == null || identifierLocationId.trim().length() == 0)
			reportError(Context.getMessageSourceService().getMessage("xforms.selectLocationType") + identifierValue,  propagateErrors, request); 
		
		//Now try add the identifier.
		PatientIdentifier identifier = new PatientIdentifier();
		identifier.setCreator(creator);
		identifier.setDateCreated(pt.getDateCreated());
		identifier.setIdentifier(identifierValue);
		PatientIdentifierType identifierType = patientService.getPatientIdentifierType(Integer.parseInt(identifierTypeId));
		identifier.setIdentifierType(identifierType);
		identifier.setLocation(getLocation(identifierLocationId));
		identifier.setPreferred(false);
		
		for(PatientIdentifier existingIdentifier : pt.getIdentifiers()){
			if(existingIdentifier.getIdentifierType() == identifierType){
				reportError(Context.getMessageSourceService().getMessage("xforms.patientHasIdentifier") + identifierType.getName(), propagateErrors, request);
			}
		}
		
		pt.addIdentifier(identifier);
		
		Patient pt2 = patientService.identifierInUse(identifier.getIdentifier(),identifier.getIdentifierType(), pt);
		if(pt2 != null){
			reportError(Context.getMessageSourceService().getMessage("xforms.triedCreatePatientAlreadyExists") + identifier.getIdentifier(), propagateErrors, request);
		}
	}
	
	private void reportError(String message, boolean propagateErrors, HttpServletRequest request) throws Exception {
		log.error(message);

		if(request != null)
			request.setAttribute(XformConstants.REQUEST_ATTRIBUTE_ID_ERROR_MESSAGE, message);

		if(propagateErrors)
			throw new Exception(message);
	}
}
