package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.projectbuendia.DateTimeUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Common class for adding observations both from the patient create, and from the encounters resource.
 */
public class ObservationsHandler {

    /**
     * Really these should come through as xforms, but as an expedient short term fix we will put an observations
     * section into the JSON RPC. JSON should be of the form:
     * observations : [
     *   {
     *       'question_uuid': 'xxxx-...',
     *       'answer_date': '2013-01-30', OR
     *       'answer_number': 40, OR
     *       'answer_uuid': 'xxxx-....'
     *   }, ...
     * ]
     */
    private static final String OBSERVATIONS = "observations";
    private static final String QUESTION_UUID = "question_uuid";
    private static final String ANSWER_DATE = "answer_date";
    private static final String ANSWER_NUMBER = "answer_number";
    private static final String ANSWER_UUID = "answer_uuid";

    /**
     * @return if a higher level JSON object has a list of observations using the standard parameter name
     */
    public boolean hasObservations(SimpleObject json) {
        // Observation for first symptom date
        return json.containsKey(OBSERVATIONS);
    }

    /**
     * Add a new encounter and a list of observations to a patient at a particular point in time.
     *
     * @param json the JSON encapsulating the observations, which must have a field "observations"
     * @param patient the patient to add the encounter to
     * @param encounterTime the time of the encounter
     * @param changeMessage a message to be recorded in the database with the observation
     * @param encounterTypeName the OpenMRS name for the encounter type, configured in OpenMRS
     * @param locationUuid the UUID to record for the location the encounter happened
     */
    public Encounter addObservations(SimpleObject json, Patient patient, Date encounterTime, String changeMessage,
                                String encounterTypeName, String locationUuid) {
        List observations = (List)json.get(OBSERVATIONS);
        if (observations.isEmpty()) {
            return null;
        }
        EncounterService encounterService = Context.getEncounterService();
        ConceptService conceptService = Context.getConceptService();
        final Location location = Context.getLocationService().getLocationByUuid(locationUuid);
        if (location == null) {
            throw new InvalidObjectDataException("Could not get location " + locationUuid);
        }
        ObsService obsService = Context.getObsService();
        Encounter encounter = new Encounter();
        encounter.setEncounterDatetime(encounterTime);
        encounter.setPatient(patient);
        encounter.setLocation(location);
        EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        if (encounterType == null) {
            throw new InvalidObjectDataException("Could not get " + encounterTypeName
                    + " encounter type, DB in bad state");
        }
        encounter.setEncounterType(encounterType);
        encounter = encounterService.saveEncounter(encounter);
        for (Object observation : observations) {
            Map observationObject = (Map) observation;
            String questionUuid = (String) observationObject.get(QUESTION_UUID);
            Concept questionConcept =
                    conceptService.getConceptByUuid(questionUuid);
            if (questionConcept == null) {
                throw new InvalidObjectDataException("Bad concept for question " + questionUuid);
            }
            Obs obs = new Obs(patient, questionConcept, encounterTime, location);
            obs.setEncounter(encounter);
            // For now assume all answers are coded or date, we can deal with numerical etc later.
            if (observationObject.containsKey(ANSWER_UUID)) {
                String answerUuid = (String) observationObject.get(ANSWER_UUID);
                Concept answerConcept = conceptService.getConceptByUuid(answerUuid);
                if (answerConcept == null) {
                    throw new InvalidObjectDataException("Bad concept for answer " + answerUuid);
                }
                obs.setValueCoded(answerConcept);
            } else if (observationObject.containsKey(ANSWER_DATE)) {
                obs.setValueDate(DateTimeUtils.parseDate((String) observationObject.get(ANSWER_DATE),
                        "answer for " + questionUuid));
            } else if (observationObject.containsKey(ANSWER_NUMBER)) {
                obs.setValueNumeric(Double.parseDouble(observationObject.get(ANSWER_NUMBER).toString()));
            } else {
                throw new InvalidObjectDataException("Unknown answer type " + observationObject);
            }
            encounter.addObs(obs);
            obsService.saveObs(obs, changeMessage);
        }
        encounter = encounterService.saveEncounter(encounter);
        return encounter;
    }
}
