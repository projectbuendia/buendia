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

import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.VisitObsValue;

import javax.annotation.Nullable;
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

    private static final String KEY_QUESTION_UUID = "question_uuid";
    private static final String KEY_ANSWER = "answer_value";

    /**
     * Adds a new encounter with the given observations and orders.
     * @param observations      a list of observations
     * @param orderUuids        a list of order UUIDs
     * @param patient           the patient for whom to add the encounter
     * @param encounterTime     the time of the encounter
     * @param changeMessage     a message to be recorded with the observation
     * @param encounterTypeName the OpenMRS name for the encounter type, configured in OpenMRS
     * @param locationUuid      the UUID of the location where the encounter happened
     * @param entererUuid      optional. The UUID of the provider who entered the encounter.
     */
    public static Encounter addEncounter(List observations, List orderUuids, Patient patient,
                                         Date encounterTime, String changeMessage,
                                         String encounterTypeName, String locationUuid,
                                         @Nullable String entererUuid) {
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

        @Nullable User enterer = Utils.getUserFromProviderUuid(entererUuid);

        List<Obs> obsList = new ArrayList<>();
        if (observations != null) {
            for (Object observation : observations) {
                Obs obs = jsonObservationToObs(observation, patient, encounterTime, location);
                obs.setCreator(enterer);
                obsList.add(obs);
            }
        }

        if (orderUuids != null) {
            for (Object item : orderUuids) {
                Obs obs = orderUuidToObs((String) item, patient, encounterTime, location);
                obs.setCreator(enterer);
                obsList.add(obs);
            }
        }

        // Write the encounter and all the observations to the database.
        Encounter encounter = new Encounter();
        encounter.setEncounterDatetime(encounterTime);
        encounter.setPatient(patient);
        encounter.setLocation(location);
        encounter.setEncounterType(encounterType);

        // Maybe set provider
        Provider provider = Utils.getProviderFromUser(enterer);
        if (provider != null) {
            EncounterRole encounterRole = Context.getEncounterService()
                    .getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID);
            encounter.setProvider(encounterRole, provider);
        }

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

    public static Obs jsonObservationToObs(Object jsonObservation, Patient patient,
                                    Date encounterTime, Location location) {
        Map observationObject = (Map) jsonObservation;
        String questionUuid = (String) observationObject.get(KEY_QUESTION_UUID);
        ConceptService conceptService = Context.getConceptService();
        Concept questionConcept = conceptService.getConceptByUuid(questionUuid);
        if (questionConcept == null) {
            throw new InvalidObjectDataException("Question concept not found: " + questionUuid);
        }
        Obs obs = new Obs(patient, questionConcept, encounterTime, location);
        String answer = (String) observationObject.get(KEY_ANSWER);
        ConceptDatatype conceptDatatype = questionConcept.getDatatype();
        try {
            // OpenMRS's setValueAsString method parses strings using the local server timezone,
            // instead of GMT. This can result in date values getting shifted. We work around this
            // by intercepting datatypes that are dates, formatting using a UTC formatter, and
            // storing the value directly.
            if (conceptDatatype.isDate()) {
                obs.setValueDate(Utils.YYYYMMDD_UTC_FORMAT.parse(answer));
            } else if (conceptDatatype.isTime() || conceptDatatype.isDateTime()) {
                throw new RuntimeException("Timestamps aren't supported yet.");
            } else {
                obs.setValueAsString(answer);
            }
        } catch (ParseException e) {
            throw new InvalidObjectDataException("Couldn't parse value '" + answer + "'.");
        }
        return obs;
    }

    private static Obs orderUuidToObs(String orderUuid, Patient patient, Date encounterTime,
                                      Location location) {
        Order order = Context.getOrderService().getOrderByUuid(orderUuid);
        if (order == null) {
            throw new InvalidObjectDataException("Order not found: " + orderUuid);
        }
        Obs obs = new Obs(patient, DbUtil.getOrderExecutedConcept(), encounterTime, location);
        obs.setOrder(order);
        obs.setValueNumeric(1d);
        return obs;
    }

    public static SimpleObject obsToJson(Obs obs) {
        SimpleObject object = new SimpleObject()
            .add("uuid", obs.getUuid())
            .add("voided", obs.isVoided());

        if (obs.isVoided()) {
            return object;
        }

        object
            .add("patient_uuid", obs.getPerson().getUuid())
            .add("encounter_uuid", obs.getEncounter().getUuid())
            .add("concept_uuid", obs.getConcept().getUuid())
            .add("timestamp", Utils.toIso8601(obs.getObsDatetime()));

        Provider provider = Utils.getProviderFromUser(obs.getCreator());
        object.add("enterer_uuid", provider != null ? provider.getUuid() : null);

        boolean isExecutedOrder =
                DbUtil.getOrderExecutedConcept().equals(obs.getConcept()) && obs.getOrder() != null;
        if (isExecutedOrder) {
            // As far as the client knows, a chain of orders is represented by the root order's
            // UUID, so we have to work back through the chain or orders to get the root UUID.
            // Normally, the client will only ever supply observations for the root order ID, but
            // in the event that an order is marked as executed on the server (for example) we don't
            // want that to mean that an order execution gets missed.
            object.add("value", Utils.getRootOrder(obs.getOrder()).getUuid());
        } else {
            object.add("value", ObservationsHandler.obsValueToString(obs));
        }

        return object;
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
