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
import org.openmrs.EncounterProvider;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.projectbuendia.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Utility methods for dealing with observations. */
public class ObsUtils {
    /**
     * Observations posted and returned by /encounters, /observations,
     * and /patients are in the form:
     *
     *     {
     *         "uuid": [obs_uuid],
     *         "encounter_uuid": [encounter_uuid],
     *         "patient_uuid": [patient_uuid],
     *         "provider_uuid": [provider_uuid],
     *         "concept_uuid": [concept_uuid],
     *         "type": [type],
     *         "time": "2019-01-01T12:34:56.789Z",
     *         "value_[type]": [value]
     *     }
     *
     * where [concept_uuid] is the UUID of the question concept and [type]
     * is one of "coded", "numeric", "text", "date", or "datetime",
     * indicating the type of the [value].  Date and datetime values
     * are in yyyy-mm-dd or yyyy-mm-ddThh:mm:ss.sssZ format.
     */

    private static Log log = LogFactory.getLog(ObsUtils.class);

    /** Adds a new encounter with the given observations. */
    public static Encounter addEncounter(List<Map> observations, Patient patient,
                                         Date encounterTime, String encounterTypeName,
                                         String providerUuid, String locationUuid) {
        // OpenMRS will reject the encounter if the time is in the past, even if
        // the client's clock is off by only one millisecond; work around this.
        encounterTime = DbUtils.fixEncounterDatetime(encounterTime);

        Location location = DbUtils.locationsByUuid.get(locationUuid);
        EncounterService encounterService = Context.getEncounterService();
        EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        if (encounterType == null) {
            throw new InvalidObjectDataException("Encounter type not found: " + encounterTypeName);
        }

        // Ensure the placement concept exists, so we can receive observations of it.
        DbUtils.getPlacementConcept();

        List<Obs> obsList = new ArrayList<>();
        if (observations != null) {
            for (Map observation : observations) {
                if (providerUuid == null) providerUuid = Utils.getOptionalString(observation, "provider_uuid");
                String conceptUuid = Utils.getRequiredString(observation, "concept_uuid");
                Concept concept = Context.getConceptService().getConceptByUuid(conceptUuid);
                Obs obs = new Obs(patient, concept, encounterTime, location);
                setObsValueFromJson(observation, obs);
                obsList.add(obs);
            }
        }

        // Write the encounter and all the observations to the database.
        Encounter encounter = new Encounter();
        encounter.setEncounterDatetime(encounterTime);
        encounter.setPatient(patient);
        encounter.setLocation(location);
        encounter.setEncounterType(encounterType);
        if (providerUuid != null) {
            Provider provider = Context.getProviderService().getProviderByUuid(providerUuid);
            if (provider != null) {
                encounter.addProvider(DbUtils.getUnknownEncounterRole(), provider);
            }
        }
        encounter = encounterService.saveEncounter(encounter);

        ObsService obsService = Context.getObsService();
        for (Obs obs : obsList) {
            if (obs != null) {
                encounter.addObs(obs);
                obsService.saveObs(obs, null);
            }
        }
        return encounter;
    }

    public static void setObsValueFromJson(Map json, Obs obs) {
        String conceptUuid = Utils.getRequiredString(json, "concept_uuid");
        Concept concept = Context.getConceptService().getConceptByUuid(conceptUuid);
        obs.setConcept(concept);
        if (json.containsKey("order_uuid")) {
            String orderUuid = Utils.getRequiredString(json, "order_uuid");
            obs.setOrder(Context.getOrderService().getOrderByUuid(orderUuid));
        }
        if (json.containsKey("value_coded")) {
            String coded = Utils.getRequiredString(json, "value_coded");
            obs.setValueCoded(Context.getConceptService().getConceptByUuid(coded));
        } else if (json.containsKey("value_numeric")) {
            obs.setValueNumeric(Utils.getRequiredNumber(json, "value_numeric"));
        } else if (json.containsKey("value_text")) {
            obs.setValueText(Utils.getRequiredString(json, "value_text"));
        } else if (json.containsKey("value_date")) {
            obs.setValueDate(Utils.getRequiredDate(json, "value_date"));
        } else if (json.containsKey("value_datetime")) {
            obs.setValueDate(Utils.getRequiredDatetime(json, "value_datetime"));
        } else {
            Utils.log("Warning! JSON observation contains no value: " + json.toString());
        }
    }

    public static void putObservationsAsJson(SimpleObject json, Collection<Obs> observations) {
        List<Map> obsArrayJson = new ArrayList<>();
        for (Obs obs : observations) {
            obsArrayJson.add(ObsUtils.putObsAsJson(new SimpleObject(), obs));
        }
        json.put("observations", obsArrayJson);
    }

    public static SimpleObject putObsAsJson(SimpleObject json, Obs obs) {
        json.put("uuid", obs.getUuid());
        if (obs.getEncounter() != null) {
            json.add("encounter_uuid", obs.getEncounter().getUuid());
        }
        if (obs.getPerson() != null) {
            json.add("patient_uuid", obs.getPerson().getUuid());
        }
        for (EncounterProvider ep : obs.getEncounter().getEncounterProviders()) {
            if (ep.getProvider() != null) {
                json.add("provider_uuid", ep.getProvider().getUuid());
                break;
            }
        }
        if (obs.getConcept() != null) {
            json.add("concept_uuid", obs.getConcept().getUuid());
        }
        return ObsUtils.putObsValueAsJson(json, obs);
    }

    public static SimpleObject putObsValueAsJson(SimpleObject json, Obs obs) {
        json.add("type", DbUtils.getConceptTypeName(obs.getConcept()));
        if (obs.getObsDatetime() != null) {
            json.add("time", Utils.formatUtc8601(obs.getObsDatetime()));
        }
        if (obs.getOrder() != null) {
            // As far as the client knows, a chain of orders is represented by the root order's
            // UUID, so we have to work back through the chain or orders to get the root UUID.
            // Normally, the client will only ever supply observations for the root order ID, but
            // in the event that an order is marked as executed on the server (for example) we don't
            // want that to mean that an order execution gets missed.
            json.add("order_uuid", DbUtils.getRootOrder(obs.getOrder()).getUuid());
        }

        String hl7Type = obs.getConcept().getDatatype().getHl7Abbreviation();
        switch (hl7Type) {
            case HL7Constants.HL7_BOOLEAN:
                Boolean bool = obs.getValueAsBoolean();
                return json.add("value_coded", bool == null ? null : (boolean) bool
                    ? Context.getConceptService().getTrueConcept().getUuid()
                    : Context.getConceptService().getFalseConcept().getUuid());
            case HL7Constants.HL7_CODED: // deliberate fall through
            case HL7Constants.HL7_CODED_WITH_EXCEPTIONS:
                Concept coded = obs.getValueCoded();
                return json.add("value_coded", coded == null ? null : coded.getUuid());
            case HL7Constants.HL7_NUMERIC:
                Double numeric = obs.getValueNumeric();
                return json.add("value_numeric", numeric == null ? null : (double) numeric);
            case HL7Constants.HL7_TEXT:
                return json.add("value_text", obs.getValueText());
            case HL7Constants.HL7_DATE:
                Date date = obs.getValueDate();
                return json.add(
                    "value_date", date == null ? null : Utils.YYYYMMDD_UTC_FORMAT.format(date));
            case HL7Constants.HL7_DATETIME:
                Date datetime = obs.getValueDatetime();
                return json.add(
                    "value_datetime", datetime == null ? null : Utils.formatUtc8601(datetime));
            default:
                Utils.log("Warning! Obs has unknown HL7 type " + hl7Type + " for concept " + obs.getConcept());
                return json;
        }
    }
}
