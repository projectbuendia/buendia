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
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.projectbuendia.ObsValueVisitor;
import org.openmrs.projectbuendia.Utils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Utility methods for dealing with observations. */
public class ObservationUtils {
    /**
     * TODO(ping): Observations are (sadly) transmitted in three different
     * wire formats.  We should get this down to 1 XML and 1 JSON format.
     * See also: EncounterResource.createItem, XformInstanceResource.adjustXformDocument.
     *
     * 1.  Observations posted to /encounters and /patients are expected
     *     to be in an array containing one item per observation, in the form
     *     {"question_uuid": [uuid], "answer_[type]": [value]}
     *     where [uuid] is the UUID of the question concept and [type] is
     *     one of "date", "datetime", "number", "uuid", or "text",
     *     indicating the type of the [value].  Date and datetime values
     *     should be in yyyy-mm-dd or yyyy-mm-ddThh:mm:ss.sssZ format.
     *
     * 2.  Observations retrieved from /encounters are returned as one map
     *     containing one key-value pair per observation, where the concept
     *     UUID of the observation is the key and values of all types are
     *     converted to string values.  The type information is lost and
     *     the client does a feeble job of guessing the type.
     *
     * 3.  Observations posted to /xforminstances are expected to be in
     *     an XForm instance document, each one as an XML element inside
     *     a top-level XML element.  (The top-level elements represent
     *     the sections of the formm, and all observations are expected
     *     to be at the second level of the XML tree.)
     *
     * We should harmonize #1 and #2 into a single JSON format, probably
     * a map like #2 containing type information like #1, where the keys
;     * are the same but the values are single-element maps like so:
     * {[uuid]: {"answer_[type]": value}, ...}
     */

    private static Log log = LogFactory.getLog(ObservationUtils.class);
    private static final String OBSERVATIONS = "observations";
    private static final String QUESTION_UUID = "question_uuid";
    private static final String ANSWER_TEXT = "answer_text";
    private static final String ANSWER_DATE = "answer_date";
    private static final String ANSWER_DATETIME = "answer_datetime";
    private static final String ANSWER_NUMBER = "answer_number";
    private static final String ANSWER_UUID = "answer_uuid";
    private static final String ORDER_UUID = "order_uuid";

    /**
     * Adds a new encounter with the given observations and orders.
     * @param observations      a list of observations
     * @param orderUuids        a list of order UUIDs
     * @param patient           the patient for whom to add the encounter
     * @param encounterTime     the time of the encounter
     * @param encounterTypeName the OpenMRS name for the encounter type, configured in OpenMRS
     * @param providerUuid      the provider who entered the observations
     * @param locationUuid      the UUID of the location where the encounter happened
     */
    public static Encounter addEncounter(List observations, List orderUuids, Patient patient,
                                         Date encounterTime, String encounterTypeName,
                                         String providerUuid, String locationUuid) {
        // OpenMRS will reject the encounter if the time is in the past, even if
        // the client's clock is off by only one millisecond; work around this.
        encounterTime = DbUtils.fixEncounterDatetime(encounterTime);

        EncounterService encounterService = Context.getEncounterService();
        Location location = null;
        if (locationUuid != null) {
            location = Context.getLocationService().getLocationByUuid(locationUuid);
            if (location == null) {
                throw new InvalidObjectDataException("Location not found: " + locationUuid);
            }
        }
        EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        if (encounterType == null) {
            throw new InvalidObjectDataException("Encounter type not found: " + encounterTypeName);
        }

        // Ensure the placement concept exists, so we can receive observations of it.
        DbUtils.getPlacementConcept();

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
                obsService.saveObs(obs, null);
            }
        }
        if (providerUuid != null) {
            Provider provider = Context.getProviderService().getProviderByUuid(providerUuid);
            if (provider != null) {
                encounter.addProvider(DbUtils.getUnknownEncounterRole(), provider);
            }
        }
        return encounter;
    }

    static Obs jsonObservationToObs(Object jsonObservation, Patient patient,
                                    Date encounterTime, Location location) {
        Map jsonObs = (Map) jsonObservation;
        String questionUuid = (String) jsonObs.get(QUESTION_UUID);
        ConceptService conceptService = Context.getConceptService();
        Concept questionConcept = conceptService.getConceptByUuid(questionUuid);
        if (questionConcept == null) {
            log.warn("Question concept not found: " + questionUuid);
            return null;
        }
        Obs obs = new Obs(patient, questionConcept, encounterTime, location);
        if (jsonObs.containsKey(ANSWER_UUID)) {
            String answerUuid = (String) jsonObs.get(ANSWER_UUID);
            Concept answerConcept = conceptService.getConceptByUuid(answerUuid);
            if (answerConcept == null) {
                log.warn("Answer concept not found: " + answerUuid);
                return null;
            }
            obs.setValueCoded(answerConcept);
        } else if (jsonObs.containsKey(ANSWER_DATE)) {
            String answerDate = (String) jsonObs.get(ANSWER_DATE);
            try {
                obs.setValueDate(Utils.YYYYMMDD_UTC_FORMAT.parse(answerDate));
            } catch (ParseException e) {
                log.warn("Invalid answer_date: " + answerDate);
                return null;
            }
        } else if (jsonObs.containsKey(ANSWER_DATETIME)) {
            String answerDate = (String) jsonObs.get(ANSWER_DATETIME);
            try {
                obs.setValueDatetime(Utils.ISO8601_FORMAT.parse(answerDate));
            } catch (ParseException e) {
                log.warn("Invalid answer_datetime: " + answerDate);
                return null;
            }
        } else if (jsonObs.containsKey(ANSWER_NUMBER)) {
            // Accept a number or a string containing a number.
            String answerNumber = jsonObs.get(ANSWER_NUMBER).toString();
            try {
                obs.setValueNumeric(Double.parseDouble(answerNumber));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid answer_number: " + answerNumber);
                return null;
            }
        } else if (jsonObs.containsKey(ANSWER_TEXT)) {
            String answerText = (String) jsonObs.get(ANSWER_TEXT);
            obs.setValueText(answerText);
        } else {
            log.warn("Invalid answer type: " + jsonObs);
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
        Obs obs = new Obs(patient, DbUtils.getOrderExecutedConcept(), encounterTime, location);
        obs.setOrder(order);
        obs.setValueNumeric(1d);
        return obs;
    }

    public static String obsValueToString(Obs obs) {
        return visit(obs, new ObsValueVisitor<String>() {
            @Override public String visitCoded(Concept value) {
                return value.getUuid();
            }

            @Override public String visitNumeric(Double value) {
                return "" + value;
            }

            @Override public String visitBoolean(Boolean value) {
                return value ? Context.getConceptService().getTrueConcept().getUuid()
                    : Context.getConceptService().getFalseConcept().getUuid();
            }

            @Override public String visitText(String value) {
                return value;
            }

            @Override public String visitDate(Date value) {
                return Utils.YYYYMMDD_UTC_FORMAT.format(value);
            }

            @Override public String visitDatetime(Date value) {
                return Utils.formatUtc8601(value);
            }
        });
    }

    /** Applies a visitor to an observation (we can't retrofit to Obs). */
    public static <T> T visit(Obs obs, ObsValueVisitor<T> visitor) {
        Concept concept = obs.getConcept();
        ConceptDatatype dataType = concept.getDatatype();
        String hl7Type = dataType.getHl7Abbreviation();
        switch (hl7Type) {
            case HL7Constants.HL7_BOOLEAN:
                return visitor.visitBoolean(obs.getValueAsBoolean());
            case HL7Constants.HL7_CODED: // deliberate fall through
            case HL7Constants.HL7_CODED_WITH_EXCEPTIONS:
                return visitor.visitCoded(obs.getValueCoded());
            case HL7Constants.HL7_NUMERIC:
                return visitor.visitNumeric(obs.getValueNumeric());
            case HL7Constants.HL7_TEXT:
                return visitor.visitText(obs.getValueText());
            case HL7Constants.HL7_DATE:
                return visitor.visitDate(obs.getValueDate());
            case HL7Constants.HL7_DATETIME:
                return visitor.visitDatetime(obs.getValueDatetime());
            default:
                throw new IllegalArgumentException(
                    "Unexpected HL7 type: " + hl7Type + " for concept " + concept);
        }
    }
}
