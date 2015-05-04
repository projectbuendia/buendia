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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.xforms.util.DOMUtil;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.propertyeditor.LocationEditor;
import org.openmrs.propertyeditor.UserEditor;
import org.openmrs.util.FormConstants;
import org.openmrs.util.FormUtil;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.FileCopyUtils;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;


/**
 * Utility functions and variables for editing for obs using xforms.
 * 
 * @author daniel
 *
 */
public class XformObsEdit {

	private static final Log log = LogFactory.getLog(XformObsEdit.class);

	/** A mapping of xpath expressions to their complex data. */
	private static HashMap<String,byte[]> complexData;

	/** A mapping of xpath expressions and their corresponding node names. */
	private static HashMap<String,String> complexDataNodeNames;

	/** A list of xpath expressions for complex data which has beed edited. */
	private static List<String> dirtyComplexData;


	public static String getMultSelObsNodeName(Element parent, Obs obs, List<String> nonCopyAttributes){
		return XformBuilder.getElementByAttributePrefix(parent, "openmrs_concept",obs.getValueCoded().getConceptId()+"^", false, "obsId", true, nonCopyAttributes).getName();
	}

	public static void fillObs(HttpServletRequest request,Document doc, Integer encounterId, String xml)throws Exception{

		Element formNode = XformBuilder.getElement(doc.getRootElement(),"form");
		if(formNode == null)
			return;

		Encounter encounter = Context.getEncounterService().getEncounter(encounterId);
		if(encounter == null)
			return;		

		retrieveSessionValues(request);
		clearSessionData(request,encounter.getForm().getFormId());

		formNode.setAttribute(null, "encounterId", encounter.getEncounterId().toString());

		if (encounter.getLocation() != null) {
			XformBuilder.setNodeValue(doc, XformBuilder.NODE_ENCOUNTER_LOCATION_ID, encounter.getLocation().getLocationId().toString());
			XformBuilder.setNodeAttributeValue(doc, XformBuilder.NODE_ENCOUNTER_LOCATION_ID, "displayValue", encounter.getLocation().getName());
		}
		
		XformBuilder.setNodeValue(doc, XformBuilder.NODE_ENCOUNTER_ENCOUNTER_DATETIME, XformsUtil.encounterDateIncludesTime() ? XformsUtil.fromDateTime2SubmitString(encounter.getEncounterDatetime()) : XformsUtil.fromDate2SubmitString(encounter.getEncounterDatetime()));
		Integer providerId = XformsUtil.getProviderId(encounter);
		if (providerId != null) {
			XformBuilder.setNodeValue(doc, XformBuilder.NODE_ENCOUNTER_PROVIDER_ID, providerId.toString());
			String name = XformsUtil.getProviderName(encounter);
			if (name != null) {
				XformBuilder.setNodeAttributeValue(doc, XformBuilder.NODE_ENCOUNTER_PROVIDER_ID, "displayValue", name);
			}
		}

		List<String> complexObs = DOMUtil.getXformComplexObsNodeNames(xml);

		HashMap<String,Element> obsGroupNodes = new HashMap<String,Element>();
		List<String> nonCopyAttributes = new ArrayList<String>();
		nonCopyAttributes.add("obsId");
		nonCopyAttributes.add("obsGroupId");
		
		DecimalFormat decimalFormat = new DecimalFormat("#");
		
		Set<Obs> observations = encounter.getObs();
		for(Obs obs : observations){
			Concept concept = obs.getConcept();

			//Obs obsGroup = obs.getObsGroup();
			String obsGroupId = obs.getObsGroup() == null ? null : obs.getObsGroup().getObsId().toString();
			
			//TODO This needs to do a better job by searching for an attribute that starts with
			//a concept id of the concept and hence remove the name dependency as the concept name
			//could change or may be different in a different locale.
			Element node = XformBuilder.getElementByAttributePrefix(formNode,"openmrs_concept", (obsGroupId == null ? concept.getConceptId() : obs.getObsGroup().getConcept()) +"^", (obsGroupId == null ? false : true), (obsGroupId == null ? "obsId" : "obsGroupId"), true, nonCopyAttributes);
			if(node == null)
				continue;

			//check if this is a repeating obs.
			if(obsGroupId != null){
				String nodeGroupId = node.getAttributeValue(null, "obsGroupId");

				//if this is first time to get a node of this parent.
				if(nodeGroupId == null || nodeGroupId.trim().length() == 0){
					node.setAttribute(null, "obsGroupId", obsGroupId); //new group processing
					obsGroupNodes.put(obsGroupId, node);
				}
				else{
					if(!nodeGroupId.equals(obsGroupId)){
						Element groupNode = obsGroupNodes.get(obsGroupId);
						if(groupNode == null){//new group
							node = XformBuilder.createCopy(node, nonCopyAttributes);
							node.setAttribute(null, "obsGroupId", obsGroupId);
							obsGroupNodes.put(obsGroupId, node);
						}//else a kid of some other group which we processed but not necessarily being the first one.
						else
							node = groupNode;
					}//else another kid of an already processed group.
				}
				
				node = XformBuilder.getElementByAttributePrefix(node,"openmrs_concept",concept.getConceptId()+"^", false, "obsId", false, nonCopyAttributes);
				if(node == null)
					continue;
			}

			String value = obs.getValueAsString(Context.getLocale());
			if(concept.getDatatype().isCoded() && obs.getValueCoded() != null)
				value = FormUtil.conceptToString(obs.getValueCoded(), Context.getLocale());
			else if(concept.getDatatype().getHl7Abbreviation().equals(ConceptDatatype.DATE) && obs.getValueDatetime() != null)
				value = XformsUtil.fromDate2SubmitString(obs.getValueDatetime());
			else if(concept.getDatatype().getHl7Abbreviation().equals(ConceptDatatype.DATETIME) && obs.getValueDatetime() != null)
				value = XformsUtil.fromDateTime2SubmitString(obs.getValueDatetime());
			else if(concept.getDatatype().getHl7Abbreviation().equals(ConceptDatatype.TIME) && obs.getValueDatetime() != null)
				value = XformsUtil.fromTime2SubmitString(obs.getValueDatetime());
			else if(concept.getDatatype().getHl7Abbreviation().equals(ConceptDatatype.NUMERIC) && obs.getValueNumeric() != null){
				if(value != null && value.contains("E")) //check if number is in scientific format.
					value = decimalFormat.format(obs.getValueNumeric());
			}

			if("1".equals(node.getAttributeValue(null,"multiple"))){
				Element multNode = XformBuilder.getElement(node, "xforms_value");
				if(multNode != null){
					value = XformBuilder.getTextValue(multNode);
					if(value != null && value.trim().length() > 0)
						value += " ";
					else
						value = "";

					//TODO This may fail when locale changes from the one in which the form was designed.
					//String xmlToken = FormUtil.getXmlToken(obs.getValueAsString(Context.getLocale()));
					try{
						String xmlToken = getMultSelObsNodeName((Element)multNode.getParent(), obs, nonCopyAttributes);
						XformBuilder.setNodeValue(node, "xforms_value", value + xmlToken);

						//TODO May need to use getElementWithAttributePrefix()
						Element valueNode = XformBuilder.getElement(node, xmlToken);
						if(valueNode != null){
							XformBuilder.setNodeValue(valueNode,"true");
							valueNode.setAttribute(null, "obsId", obs.getObsId().toString());
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
			else{
				Element valueNode = XformBuilder.getElement(node, "value");
				if(valueNode != null){
					String nodeName = node.getName();
					if(complexObs.contains(nodeName)){
						String key = getComplexDataKey(formNode.getAttributeValue(null, "id"),"/form/obs/" + node.getName() + "/value");
						value = getComplexObsValue(node.getName(),valueNode,value,key);
					}

					XformBuilder.setNodeValue(valueNode,value);
					valueNode.setAttribute(null, "obsId", obs.getObsId().toString());
					
					if(concept.getDatatype().isCoded() && obs.getValueCoded() != null)
						valueNode.setAttribute(null, "displayValue", obs.getValueCoded().getName().getName());

					if(obsGroupId != null || (nodeName.equals("problem_added") || nodeName.equals("problem_resolved")))
						valueNode.setAttribute(null, "default", "false()");
				}

				node.setAttribute(null, "obsId", obs.getObsId().toString());
				//XformBuilder.setNodeValue(node, "value", value);
			}
			
			//If obs date is not the same as for that of the encounter, it means the user explicitly entered it.
			if (obs.getObsDatetime() != null && !encounter.getEncounterDatetime().equals(obs.getObsDatetime())) {
				Element dateNode = XformBuilder.getElement(node, "date");
				if (dateNode != null) {
					value = XformsUtil.fromDate2SubmitString(obs.getObsDatetime());
					XformBuilder.setNodeValue(dateNode, value);
				}
			}
		}

		//System.out.println(XformBuilder.fromDoc2String(doc));
	}

	private static String getComplexObsValue(String nodeName, Element valueNode, String value, String key){
		complexDataNodeNames.put(key, nodeName);

		if(value == null || value.trim().length() == 0)
			return value;

		try{
			byte[] bytes = FileCopyUtils.copyToByteArray(new FileInputStream(value));
			complexData.put(key, bytes);
			return Base64.encode(bytes);
		}
		catch(Exception ex){
			log.error(ex); //File may be deleted, but that should not prevent loading of the form.
		}

		return value;
	}

	public static Encounter getEditedEncounter(HttpServletRequest request,Document doc,Set<Obs> obs2Void, String xml) throws Exception{
		return getEditedEncounter(request,XformBuilder.getElement(doc.getRootElement(),"form"),obs2Void);
	}

	public static Encounter getEditedEncounter(HttpServletRequest request,Element formNode,Set<Obs> obs2Void) throws Exception{
		if(formNode == null || !"form".equals(formNode.getName()))
			return null;

		String formId = formNode.getAttributeValue(null, "id");

		retrieveSessionValues(request);
		clearSessionData(request,Integer.parseInt(formId));

		List<String> complexObs = DOMUtil.getModelComplexObsNodeNames(formId);
		List<String> dirtyComplexObs = getEditedComplexObsNames();

		Date datetime = new Date();

		Integer encounterId = Integer.parseInt(formNode.getAttributeValue(null, "encounterId"));
		Encounter encounter = Context.getEncounterService().getEncounter(encounterId);
		setEncounterHeader(encounter,formNode);

		Hashtable<String,String[]> multipleSelValues = new Hashtable<String,String[]>();

		Set<Obs> observations = encounter.getObs();

		for(Obs obs : observations){
			Concept concept = obs.getConcept();

			String nodeName = FormUtil.getXmlToken(concept.getDisplayString());
			Element node = XformBuilder.getElementByAttributeValue(formNode,"obsId",obs.getObsId().toString());
			
			if(node == null){
				if(obs.getObsGroup() != null)
					voidObs(obs.getObsGroup(),datetime,obs2Void);

				voidObs(obs,datetime,obs2Void);
				continue;
			}

			if(isMultipleSelNode((Element)node.getParent()))
				node = (Element)node.getParent();

			if(isMultipleSelNode(node)){
				String xmlToken = FormUtil.getXmlToken(obs.getValueAsString(Context.getLocale()));
				if(multipleSelValueContains(nodeName,xmlToken,node,multipleSelValues))
					continue;

				voidObs(obs,datetime,obs2Void);
			}
			else{
				Element valueNode = XformBuilder.getElement(node, "value");

				if(valueNode != null){
					String newValue = XformBuilder.getTextValue(valueNode);
					String oldValue = obs.getValueAsString(Context.getLocale());

					if(concept.getDatatype().getHl7Abbreviation().equals(ConceptDatatype.DATETIME) && obs.getValueDatetime() != null)
						oldValue = XformsUtil.fromDateTime2SubmitString(obs.getValueDatetime());
					else if(concept.getDatatype().getHl7Abbreviation().equals(ConceptDatatype.TIME) && obs.getValueDatetime() != null)
						oldValue = XformsUtil.fromTime2SubmitString(obs.getValueDatetime());

					//Deal with complex obs first
					if(complexObs.contains(nodeName)){
						//Continue if complex obs has neither been replaced 
						//with a another one nor cleared.
						if(!dirtyComplexObs.contains(nodeName) && 
								!( (newValue == null || newValue.trim().length() == 0) && 
										(oldValue != null && oldValue.trim().length() > 0 ) ))
							continue; //complex obs not modified.

						voidObs(obs,datetime,obs2Void);

						if(newValue == null || newValue.trim().length() == 0)
							continue; //complex obs just cleared without a replacement.

						newValue = saveComplexObs(nodeName,newValue,formNode);
					}
					else{
						if(concept.getDatatype().isCoded() && obs.getValueCoded() != null)
							oldValue = conceptToString(obs.getValueCoded(), Context.getLocale());
						
						if(oldValue.equals(newValue))
							continue; //obs has not changed
						
						voidObs(obs,datetime,obs2Void);
						
						if(newValue == null || newValue.trim().length() == 0)
							continue;
					}

					//setObsValue(obs,newValue);

					//obs.setDateChanged(datetime);
					//obs.setChangedBy(Context.getAuthenticatedUser());

					encounter.addObs(createObs(concept,obs.getObsGroup(),newValue,datetime));
				}
				else
					throw new IllegalArgumentException("cannot locate node for concept: " + concept.getDisplayString());

			}
		}

		//addNewObs(formNode,complexObs,encounter,XformBuilder.getElement(formNode,"obs"),datetime,null);
		//addNewObs(formNode,complexObs,encounter,XformBuilder.getElement(formNode,"problem_list"),datetime,null);
		
		//Add new obs for all form sections
		for(int i=0; i<formNode.getChildCount(); i++){
			if(formNode.getType(i) != Element.ELEMENT)
				continue;

			Element child = (Element)formNode.getChild(i);
			
			String conceptStr = child.getAttributeValue(null, "openmrs_concept");
			if(conceptStr == null || conceptStr.trim().length() == 0)
				;//continue; //TODO Can't we have sections which are not concepts?
			
			addNewObs(formNode, complexObs, encounter, child, datetime, null);
		}
		
		XformObsPatientEdit.updatePatientDemographics(encounter.getPatient(), formNode);
		
		return encounter;
	}

	private static void voidObs(Obs obs, Date datetime, Set<Obs> obs2Void){
		obs.setVoided(true);
		obs.setVoidedBy(Context.getAuthenticatedUser());
		obs.setDateVoided(datetime);
		obs.setVoidReason("xformsmodule"); //TODO Need to set this from user.

		obs2Void.add(obs);
	}

	private static void setEncounterHeader(Encounter encounter, Element formNode) throws Exception{
		encounter.setLocation(Context.getLocationService().getLocation(Integer.valueOf(XformBuilder.getNodeValue(formNode, XformBuilder.NODE_ENCOUNTER_LOCATION_ID))));
		
		Integer id = Integer.valueOf(XformBuilder.getNodeValue(formNode, XformBuilder.NODE_ENCOUNTER_PROVIDER_ID));
		if (XformsUtil.isOnePointNineAndAbove()) {
			XformsUtil.setProvider(encounter, id);
		}
		else {
			Person person = Context.getPersonService().getPerson(id);
			try{
				Method method = encounter.getClass().getMethod("setProvider", new Class[]{Person.class});
				method.invoke(encounter, new Object[]{person});
			}
			catch(NoSuchMethodError ex){
				encounter.setProvider(Context.getUserService().getUser(id));
			}
		}
		
		String submitString = XformBuilder.getNodeValue(formNode, XformBuilder.NODE_ENCOUNTER_ENCOUNTER_DATETIME);
		Date date = null;
		if(XformsUtil.encounterDateIncludesTime())
			date = XformsUtil.fromSubmitString2DateTime(submitString);
		else
			date = XformsUtil.fromSubmitString2Date(submitString);
		
		encounter.setEncounterDatetime(date);
	}

	private static void addNewObs(Element formNode, List<String> complexObs,Encounter encounter, Element obsNode, Date datetime, Obs obsGroup) throws Exception{
		if(obsNode == null)
			return;

		for(int i=0; i<obsNode.getChildCount(); i++){
			if(obsNode.getType(i) != Element.ELEMENT)
				continue;

			Element node = (Element)obsNode.getChild(i);

			String conceptStr = node.getAttributeValue(null, "openmrs_concept");
			if(conceptStr == null || conceptStr.trim().length() == 0)
				continue;

			Concept concept = Context.getConceptService().getConcept(Integer.parseInt(getConceptId(conceptStr)));

			if(isMultipleSelNode(node))
				addMultipleSelObs(encounter,concept,node,datetime);
			else{
				Element valueNode = XformBuilder.getElement(node, "value");
				String value = XformBuilder.getTextValue(valueNode);

				String obsId = node.getAttributeValue(null, "obsId");
				String obsGroupId = node.getAttributeValue(null, "obsGroupId");

				if((obsId != null && obsId.trim().length() > 0) ||
						obsGroupId != null && obsGroupId.trim().length() > 0){

					if(!"true()".equals(valueNode.getAttributeValue(null, "new")))
						continue; //new obs cant have an obs id
				}

				if(obsGroupId != null && obsGroupId.trim().length() > 0){		
					Obs group = createObs(concept,obsGroup,value,datetime);
					encounter.addObs(group);
					addNewObs(formNode,complexObs,encounter,node,datetime,group);
					continue; //new obs cant have an obsGroupId
				}

				if(valueNode == null)
					continue;

				if(value == null || value.trim().length() == 0)
					continue;

				String nodeName = node.getName();

				if(complexObs.contains(nodeName))
					value = saveComplexObs(nodeName,value,formNode);

				Obs obs = createObs(concept,obsGroup,value,datetime);
				encounter.addObs(obs);

				if(concept.isSet())
					addNewObs(node,complexObs,encounter,node,datetime,obs);
			}
		}
	}

	private static void addMultipleSelObs(Encounter encounter, Concept concept,Element node, Date datetime) throws Exception{
		Element multValueNode = XformBuilder.getElement(node, "xforms_value");
		if(multValueNode == null)
			return;

		String	value = XformBuilder.getTextValue(multValueNode);
		if(value == null || value.trim().length() == 0)
			return;

		String[] valueArray = value.split(XformBuilder.MULTIPLE_SELECT_VALUE_SEPARATOR);

		for(int i=0; i<node.getChildCount(); i++){
			if(node.getType(i) != Element.ELEMENT)
				continue;

			Element valueNode = (Element)node.getChild(i);

			String obsId = valueNode.getAttributeValue(null, "obsId");
			if(obsId != null && obsId.trim().length() > 0){
				if(!"true()".equals(valueNode.getAttributeValue(null, "new")))
					continue; //new obs cant have an obs id
			}

			String obsGroupId = valueNode.getAttributeValue(null, "obsGroupId");
			if(obsGroupId != null && obsGroupId.trim().length() > 0)
				continue; //new obs cant have an obsGroupId

			String conceptStr = valueNode.getAttributeValue(null, "openmrs_concept");
			if(conceptStr == null || conceptStr.trim().length() == 0)
				continue; //must have an openmrs_concept attribute hence nothing like date or value nodes

			if(!contains(valueNode.getName(),valueArray))
				continue; //name must be in the xforms_value

			Obs obs = createObs(concept,null,conceptStr,datetime);
			encounter.addObs(obs);
		}
	}

	private static boolean multipleSelValueContains(String nodeName, String valueName,Element node,Hashtable<String,String[]> multipleSelValues){
		String[] values = multipleSelValues.get(nodeName);
		if(values == null){
			Element multNode = XformBuilder.getElement(node, "xforms_value");
			if(multNode != null){
				String value = XformBuilder.getTextValue(multNode);
				if(value == null)
					value = "";
				values = value.split(XformBuilder.MULTIPLE_SELECT_VALUE_SEPARATOR);

				multipleSelValues.put(nodeName, values);
			}
		}

		return contains(valueName,values);
	}

	/*private static void setObsValue(Obs obs,Element valueNode, boolean isNew) throws Exception{
		setObsValue(obs,XformBuilder.getTextValue(valueNode),isNew);
	}*/

	private static boolean setObsValue(Obs obs,String value) throws Exception{
		ConceptDatatype dt = obs.getConcept().getDatatype();

		if (dt.isNumeric())
			obs.setValueNumeric(Double.parseDouble(value.toString()));
		else if (dt.isText())
			obs.setValueText(value);
		else if (dt.isCoded())
			obs.setValueCoded((Concept) convertToType(getConceptId(value), Concept.class));
		else if (dt.isBoolean()){
			boolean booleanValue = value != null && !Boolean.FALSE.equals(value) && !"false".equals(value);
			obs.setValueNumeric(booleanValue ? 1.0 : 0.0);
		}
		else if(dt.TIME.equals(dt.getHl7Abbreviation()))
			obs.setValueDatetime(XformsUtil.fromSubmitString2Time(value));
		else if(dt.DATETIME.equals(dt.getHl7Abbreviation()))
			obs.setValueDatetime(XformsUtil.fromSubmitString2DateTime(value));
		else if (dt.isDate())
			obs.setValueDatetime(XformsUtil.fromSubmitString2Date(value));
		else if ("ZZ".equals(dt.getHl7Abbreviation())) {
			// don't set a value. eg  could be a set
		}else
			throw new IllegalArgumentException("concept datatype not yet implemented: " + dt.getName() + " with Hl7 Abbreviation: " + dt.getHl7Abbreviation());

		return false;
	}

	private static String getConceptId(String conceptStr){
		int pos = conceptStr.indexOf('^');
		if(pos < 1)
			return conceptStr; //must be a number already.
		
		return conceptStr.substring(0, pos);
	}

	private static Object convertToType(String val, Class<?> clazz) {
		if (val == null)
			return null;
		if ("".equals(val) && !String.class.equals(clazz))
			return null;
		if (Location.class.isAssignableFrom(clazz)) {
			LocationEditor ed = new LocationEditor();
			ed.setAsText(val);
			return ed.getValue();
		} else if (User.class.isAssignableFrom(clazz)) {
			UserEditor ed = new UserEditor();
			ed.setAsText(val);
			return ed.getValue();
		} else if (Date.class.isAssignableFrom(clazz)) {
			try {
				DateFormat df = Context.getDateFormat();
				df.setLenient(false);
				return df.parse(val);
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		} else if (Double.class.isAssignableFrom(clazz)) {
			return Double.valueOf(val);
		} else if (Concept.class.isAssignableFrom(clazz)) {
			ConceptEditor ed = new ConceptEditor();
			ed.setAsText(val);
			return ed.getValue();
		} else {
			return val;
		}
	}

	private static Obs createObs(Concept concept,Obs obsGroup, String value, Date datetime) throws Exception{
		Obs obs = new Obs();
		obs.setConcept(concept);
		obs.setObsGroup(obsGroup);
		setObsValue(obs,value);

		if (datetime != null)
			obs.setObsDatetime(datetime);

		obs.setCreator(Context.getAuthenticatedUser());

		return obs;
	}

	private static boolean isMultipleSelNode(Element node){
		return "1".equals(node.getAttributeValue(null,"multiple"));
	}

	private static boolean contains(String name,String[] valueArray){
		for(String value : valueArray){
			if(!value.equalsIgnoreCase(name))
				continue;
			return true;
		}

		return false;
	}

	public static String conceptToString(Concept concept, Locale locale) {
		ConceptName localizedName = concept.getName(locale);
		return conceptToString(concept, localizedName);
	}

	/**
	 * Turn the given concept/concept-name pair into a string acceptable for hl7 and forms
	 * 
	 * @param concept Concept to convert to a string
	 * @param localizedName specific localized concept-name
	 * @return String representation of the given concept
	 */
	public static String conceptToString(Concept concept, ConceptName localizedName) {
		return concept.getConceptId() + "^" + localizedName.getName() + "^" + FormConstants.HL7_LOCAL_CONCEPT; // + "^"
		// + localizedName.getConceptNameId() + "^" + localizedName.getName() + "^" + FormConstants.HL7_LOCAL_CONCEPT_NAME;
	}

	public static String getComplexDataKey(String formid, String xpath){
		return formid + xpath;
	}

	public static List<String> getEditedComplexObsNames(){
		List<String> names = new ArrayList<String>();

		for(String xpath : dirtyComplexData){
			String name = complexDataNodeNames.get(xpath);
			if(name != null)
				names.add(name);
		}

		return names;
	}

	public static String saveComplexObs(String nodeName, String value, Element formNode) throws Exception {
		byte[] bytes = Base64.decode(value);

		String path = formNode.getAttributeValue(null,"name");
		path += File.separatorChar + nodeName;

		File file = OpenmrsUtil.getOutFile(XformsUtil.getXformsComplexObsDir(path), new Date(), Context.getAuthenticatedUser());
		FileOutputStream writter = new FileOutputStream(file);
		writter.write(bytes);
		writter.close();

		return file.getAbsolutePath();
	}

	public static void retrieveSessionValues(HttpServletRequest request){
		HttpSession session = request.getSession();

		complexData = (HashMap<String,byte[]>)session.getAttribute("XformObsEdit.complexData");
		complexDataNodeNames = (HashMap<String,String>)session.getAttribute("XformObsEdit.complexDataNodeNames");
		dirtyComplexData = (List<String>)session.getAttribute("XformObsEdit.dirtyComplexData");

		if(complexData == null){
			complexData = new HashMap<String,byte[]>();
			session.setAttribute("XformObsEdit.complexData", complexData);
		}

		if(complexDataNodeNames == null){
			complexDataNodeNames = new HashMap<String,String>();
			session.setAttribute("XformObsEdit.complexDataNodeNames", complexDataNodeNames);
		}

		if(dirtyComplexData == null){
			dirtyComplexData = new ArrayList<String>();
			session.setAttribute("XformObsEdit.dirtyComplexData", dirtyComplexData);
		}
	}

	public static void clearSessionData(HttpServletRequest request,Integer formId){
		complexData.clear();
		complexDataNodeNames.clear();
		dirtyComplexData.clear();

		clearFormSessionData(request, formId.toString());
	}


	public static byte[] getComplexData(HttpServletRequest request,String formId, String xpath){
		retrieveSessionValues(request);
		return complexData.get(getComplexDataKey(formId, xpath));
	}

	public static void setComplexDataDirty(HttpServletRequest request,String formId, String xpath){
		retrieveSessionValues(request);

		String key = getComplexDataKey(formId, xpath);
		if(!dirtyComplexData.contains(key))
			dirtyComplexData.add(key);
	}

	public static void loadAndClearSessionData(HttpServletRequest request,Integer formId){
		HttpSession session = request.getSession();

		complexData = (HashMap<String,byte[]>)session.getAttribute("XformObsEdit.complexData");
		complexDataNodeNames = (HashMap<String,String>)session.getAttribute("XformObsEdit.complexDataNodeNames");
		dirtyComplexData = (List<String>)session.getAttribute("XformObsEdit.dirtyComplexData");

		if(complexData != null)
			complexData.clear();

		if(complexDataNodeNames != null)
			complexDataNodeNames.clear();

		if(dirtyComplexData != null)
			dirtyComplexData.clear();

		clearFormSessionData(request, formId.toString());
	}

	public static void clearFormSessionData(HttpServletRequest request,String formId){
		HttpSession session = request.getSession();
		session.setAttribute(getFormKey(formId), null);
	}

	public static String getFormKey(String formId){
		return "MultidemiaData"+formId;
	}

	public static void fillPatientComplexObs(HttpServletRequest request,Document doc, String xml) throws Exception{
		retrieveSessionValues(request);
		clearSessionData(request, XformConstants.PATIENT_XFORM_FORM_ID);

		Element patientNode = XformBuilder.getElement(doc.getRootElement(), "patient");
		if(patientNode== null)
			return;

		List<String> complexObs = DOMUtil.getXformComplexObsNodeNames(xml);

		for(int index = 0; index < patientNode.getChildCount(); index++){
			if(patientNode.getType(index) != Element.ELEMENT)
				continue;
			//Syste
			Element node = (Element)patientNode.getChild(index);
			if(complexObs.contains(node.getName())){
				String value = XformBuilder.getTextValue(node);
				if(value != null && value.trim().length() > 0){
					String key = getComplexDataKey(patientNode.getAttributeValue(null, "id"),"/patient/" + node.getName());
					value = getComplexObsValue(node.getName(),node,value,key);
					XformBuilder.setNodeValue(node,value);
				}
			}
		}
	}
}
