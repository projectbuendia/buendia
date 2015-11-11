// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.VisitObsValue;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Utility for adding observations parsed from JSON. */
public class ObservationsHandler {
    /**
     * For consistency these should be accepted as XForm instances, but as a
     * short-term fix we allow observations to be expressed in this JSON format:
     * <pre>
     * "observations": [
     *   {
     *       "question_uuid": "xxxx-...',
     *       # then ONE of the following three answer_* fields:
     *       "answer_date": "2013-01-30"
     *       "answer_number": 40
     *       "answer_uuid": "xxxx-...."
     *   }, ...
     * ]
     * </pre>
     */

    private static Log log = LogFactory.getLog(ObservationsHandler.class);
    private static final String OBSERVATIONS = "observations";
    private static final String QUESTION_UUID = "question_uuid";
    private static final String ANSWER_DATE = "answer_date";
    private static final String ANSWER_NUMBER = "answer_number";
    private static final String ANSWER_UUID = "answer_uuid";
    private static final String ORDER_UUID = "order_uuid";

    /**
     * Adds a new encounter with the given observations and orders.
     * @param observations      a list of observations
     * @param orderUuids        a list of order UUIDs
     * @param patient           the patient for whom to add the encounter
     * @param encounterTime     the time of the encounter
     * @param changeMessage     a message to be recorded with the observation
     * @param encounterTypeName the OpenMRS name for the encounter type, configured in OpenMRS
     * @param locationUuid      the UUID of the location where the encounter happened
     */
    public static Encounter addEncounter(List observations, List orderUuids, Patient patient,
                                         Date encounterTime, String changeMessage, String
                                             encounterTypeName,
                                         String locationUuid) {
        // OpenMRS will reject the encounter if the time is in the past, even if
        // the client's clock is off by only one millisecond; work around this.
        encounterTime = Utils.fixEncounterDateTime(encounterTime);

        EncounterService encounterService = Context.getEncounterService();
        final Location location = Context.getLocationService().getLocationByUuid(locationUuid);
        if (location == null) {
            throw new InvalidObjectDataException("Location not found: " + locationUuid);
        }
        EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        if (encounterType == null) {
            throw new InvalidObjectDataException("Encounter type not found: " + encounterTypeName);
        }

        List<Obs> obsList = new ArrayList<>();
        if (observations != null) {
            for (Object observation : observations) {
                obsList.add(jsonObservationToObs(observation, patient, encounterTime, location));
            }
        }

        if (orderUuids != null) {
            for (Object item : orderUuids) {
                obsList.add(orderUuidToObs((String) item, patient, encounterTime, location));
            }
        }

        // Write the encounter and all the observations to the database.
        Encounter encounter = new Encounter();
        encounter.setEncounterDatetime(encounterTime);
        encounter.setPatient(patient);
        encounter.setLocation(location);
        encounter.setEncounterType(encounterType);
        encounter = encounterService.saveEncounter(encounter);

        ObsService obsService = Context.getObsService();
        for (Obs obs : obsList) {
            if (obs != null) {
                encounter.addObs(obs);
                obsService.saveObs(obs, changeMessage);
            }
        }
        return encounter;
    }

    static Obs jsonObservationToObs(Object jsonObservation, Patient patient,
                                    Date encounterTime, Location location) {
        Map observationObject = (Map) jsonObservation;
        String questionUuid = (String) observationObject.get(QUESTION_UUID);
        ConceptService conceptService = Context.getConceptService();
        Concept questionConcept = conceptService.getConceptByUuid(questionUuid);
        if (questionConcept == null) {
            log.warn("Question concept not found: " + questionUuid);
            return null;
        }
        Obs obs = new Obs(patient, questionConcept, encounterTime, location);
        String answerUuid = (String) observationObject.get(ANSWER_UUID);
        String answerDate = (String) observationObject.get(ANSWER_DATE);
        String answerNumber = (String) observationObject.get(ANSWER_NUMBER);
        if (answerUuid != null) {
            Concept answerConcept = conceptService.getConceptByUuid(answerUuid);
            if (answerConcept == null) {
                log.warn("Answer concept not found: " + answerUuid);
                return null;
            }
            obs.setValueCoded(answerConcept);
        } else if (answerDate != null) {
            try {
                obs.setValueDate(Utils.YYYYMMDD_UTC_FORMAT.parse(answerDate));
            } catch (ParseException e) {
                log.warn("Invalid date answer: " + answerDate);
                return null;
            }
        } else if (observationObject.containsKey(ANSWER_NUMBER)) {
            try {
                obs.setValueNumeric(Double.parseDouble(answerNumber));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid numeric answer: " + answerUuid);
                return null;
            }
        } else {
            log.warn("Invalid answer type: " + observationObject);
            return null;
        }
        return obs;
    }

    static Obs orderUuidToObs(String orderUuid, Patient patient, Date encounterTime, Location
        location) {
        Order order = Context.getOrderService().getOrderByUuid(orderUuid);
        if (order == null) {
            log.warn("Order not found: " + orderUuid);
            return null;
        }
        Obs obs = new Obs(patient, DbUtil.getOrderExecutedConcept(), encounterTime, location);
        obs.setOrder(order);
        obs.setValueNumeric(1d);
        return obs;
    }

    public static String obsValueToString(Obs obs) {
        return VisitObsValue.visit(
                obs, new VisitObsValue.ObsValueVisitor<String>() {
                    @Override public String visitCoded(Concept value) {
                        return value.getUuid();
                    }

                    @Override public String visitNumeric(Double value) {
                        return "" + value;
                    }

                    @Override public String visitBoolean(Boolean value) {
                        return "" + value;
                    }

                    @Override public String visitText(String value) {
                        return value;
                    }

                    @Override public String visitDate(Date value) {
                        return Utils.YYYYMMDD_UTC_FORMAT.format(value);
                    }

                    @Override public String visitDateTime(Date value) {
                        return Utils.toIso8601(value);
                    }
                });
    }
}
