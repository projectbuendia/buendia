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
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.openmrs.projectbuendia.Utils.eq;

/**
 * A collection where each item corresponds to one patient and contains
 * the encounter and observation data for that patient as of a particular
 * point in time (referred to as the "snapshot time").
 * @see AbstractReadOnlyResource
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xencounters",
    supportedClass = Encounter.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class EncounterResource implements Creatable {
    private final PatientService patientService;
    private RequestLogger logger = RequestLogger.LOGGER;

    public EncounterResource() {
        patientService = Context.getPatientService();
    }

    /**
     * Create a new encounter for a patient. The expected JSON format is:
     * {
     * "uuid": "patient-uuid-xxxx",
     * "timestamp": seconds_since_epoch,
     * "observations": [
     * {
     * "question_uuid": "xxxx-...",
     * # then ONE of the following three answer_* fields:
     * "answer_date": "2013-01-30"
     * "answer_number": 40
     * "answer_uuid": "xxxx-...."
     * # and OPTIONALLY this field:
     * "order_uuid": "xxxx-..."
     * },
     * ]
     * }
     */
    @Override
    public Object create(SimpleObject obj, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "create", obj);
            Object result = createInner(obj, context);
            logger.reply(context, this, "create", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "create", e);
            throw e;
        }
    }

    public Object createInner(SimpleObject post, RequestContext context) throws ResponseException {
        // Warning! In order to re-use the observation creation code from PatientResource,
        // the JSON data format for this create method is different from the JSON data format
        // for get. This is terrible REST design. However, we are close to shipping, and
        // I don't want to introduce the server and client side changes needed if I changed
        // the wire format. So instead, there is this comment.
        // TODO: refactor the wire format for getEncounters so it matches the create format.

        String patientUuid = Utils.getRequiredString(post, "uuid");
        Patient patient = patientService.getPatientByUuid(patientUuid);
        if (patient == null) {
            throw new InvalidObjectDataException("Patient not found: " + post.get("uuid"));
        }
        Date encounterTime;
        Object timestamp = post.get("timestamp");
        try {
            if (timestamp != null) {
                encounterTime = new Date(Long.parseLong(timestamp.toString())*1000L);
            } else {
                // Allow clients to omit the timestamp to use the current server time.
                encounterTime = new Date();
            }
        } catch (NumberFormatException ex) {
            throw new InvalidObjectDataException(
                "Expected seconds since epoch for \"timestamp\" value: " + ex.getMessage());
        }
        Encounter encounter = ObservationUtils.addEncounter(
            (List) post.get("observations"), (List) post.get("order_uuids"),
            patient, encounterTime, "ADULTRETURN", (String) post.get("enterer_uuid"), null);
        SimpleObject simpleObject = new SimpleObject();
        populateJsonProperties(encounter, simpleObject);
        return simpleObject;
    }

    /**
     * Populates observation and order data for the given encounter, including:
     * <ul>
     *   <li>"timestamp": the encounter datetime in RFC 3339 === ISO 8601 format
     *   <li>"uuid": the encounter's UUID
     *   <li>"observations": {@link SimpleObject} that maps concept UUIDs to values
     *   <li>"order_uuids": unique identifiers of orders executed as part of this encounter.
     * </ul>
     */
    protected void populateJsonProperties(Encounter encounter, SimpleObject encounterJson) {
        encounterJson.put("patient_uuid", encounter.getPatient().getUuid());
        encounterJson.put("timestamp", Utils.formatUtc8601(encounter.getEncounterDatetime()));
        encounterJson.put("uuid", encounter.getUuid());

        SimpleObject observations = new SimpleObject();
        List<String> orderUuids = new ArrayList<>();
        for (Obs obs : encounter.getObs()) {
            Concept concept = obs.getConcept();
            if (concept != null && eq(concept.getUuid(), DbUtils.CONCEPT_ORDER_EXECUTED_UUID)) {
                orderUuids.add(obs.getOrder().getUuid());
                continue;
            }

            observations.put(obs.getConcept().getUuid(), ObservationUtils.obsValueToString(obs));
        }
        if (!observations.isEmpty()) {
            encounterJson.put("observations", observations);
        }
        if (!orderUuids.isEmpty()) {
            encounterJson.put("order_uuids", orderUuids);
        }
    }

    @Override
    public String getUri(Object instance) {
        // We don't actually use this, but return a relatively sensible value anyway.
        OpenmrsObject mrsObject = (OpenmrsObject) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + mrsObject.getUuid();
    }
}
