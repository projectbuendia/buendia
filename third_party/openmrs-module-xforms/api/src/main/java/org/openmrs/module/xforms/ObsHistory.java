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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.context.Context;


/**
 * Retrieves observations, for a given patient, as used in form field default value velocity templates.
 * 
 * @author danielkayiwa
 *
 */
public class ObsHistory {

	/** The patient whose observations we are fetching. */
	private Patient patient;

	/**
	 * Create s a new obs history object for a given patient.
	 * 
	 * @param patient the patient whose observations we are going to fetch.
	 */
	public ObsHistory(Patient patient){
		this.patient = patient;
	}

	/**
	 * Gets the most recent observation for a given concept.
	 * 
	 * @param conceptId is the concept id.
	 * @return an {@link Obs} object.
	 */
	public Obs getObs(int conceptId){
		return getObs(1, conceptId);
	}
	
	/**
	 * Gets the observation at a given index for a given concept.
	 * 
	 * @param encounterIndex is the index of the observation starting with 1 for the most recent,
	 * 							2 for the second most recent, 3 for the third most recent, and more.
	 * 
	 * @param conceptId is the concept id.
	 * @return an {@link Obs} object.
	 */
	public Obs getObs(int encounterIndex, int conceptId){
		if(encounterIndex == 0)
			encounterIndex = 1;

		Concept concept = Context.getConceptService().getConcept(conceptId);
		if(concept == null)
			return null;

		int index = 1;
		
		List<Person> whom = new Vector<Person>();
		whom.add(patient);
		
		List<Concept> questions = new Vector<Concept>();
		questions.add(concept);
		
		List<String> sort = new Vector<String>();
		sort.add("encounter");
		
		List<Obs> observations = Context.getObsService().getObservations(whom, null, questions, null, null, null, sort, null, null, null, null, false);
		//List<Obs> observations = Context.getObsService().getObservationsByPersonAndConcept(patient, concept);
		for(Obs obs : observations){
			if(encounterIndex == index){
				return obs;
			}
			index++;
		}

		return null;
	}

	/**
	 * Gets the value of the most recent observation for a given concept.
	 * 
	 * @param conceptId is the concept id.
	 * @return the value.
	 */
	public Object getValue(int conceptId){
		return getValue(1, conceptId);
	}
	
	
	/**
	 * Gets the value of an observation at a given index for a given concept.
	 * 
	 * @param encounterIndex is the index of the observation starting with 1 for the most recent,
	 * 							2 for the second most recent, 3 for the third most recent, and more.
	 * 
	 * @param conceptId is the concept id.
	 * @return the value.
	 */
	public Object getValue(int encounterIndex, int conceptId){
		return getValue(getObs(encounterIndex, conceptId));
	}

	/**
	 * Gets the value of a given {@link Obs}
	 * 
	 * @param obs the {@link Obs} object.
	 * @return the value.
	 */
	private Object getValue(Obs obs){
		if(obs != null){
			ConceptDatatype dt = obs.getConcept().getDatatype();
			if(dt.isNumeric())
				return obs.getValueNumeric();
			else if(dt.isBoolean())
				return obs.getValueAsBoolean();
			else if(dt.isText())
				return obs.getValueText();
			else if(dt.isDate()){
				if(obs.getValueDatetime() != null)
					return org.openmrs.api.context.Context.getDateFormat().format(obs.getValueDatetime());
				return obs.getValueDatetime();  
			}
			else if(dt.isCoded())
				return obs.getValueCoded();
		}

		return null;
	}
	
	public Date getObsDatetime(int conceptId){
		Obs obs = getObs(1, conceptId);
		if (obs != null)
			return obs.getObsDatetime();
		else
			return null;
	}
	
	public Date getObsDatetime(int encounterIndex, int conceptId){
		Obs obs = getObs(encounterIndex, conceptId);
		if (obs != null)
			return obs.getObsDatetime();
		else
			return null;
	}
	
	public String getObsDatetime(int conceptId, String format){
		Obs obs = getObs(1, conceptId);
		if (obs != null)
			return new SimpleDateFormat(format).format(obs.getObsDatetime());
		else
			return null;
	}
	
	public String getObsDatetime(int encounterIndex, int conceptId, String format){
		Obs obs = getObs(encounterIndex, conceptId);
		if (obs != null)
			return new SimpleDateFormat(format).format(obs.getObsDatetime());
		else
			return null;
	}
}
