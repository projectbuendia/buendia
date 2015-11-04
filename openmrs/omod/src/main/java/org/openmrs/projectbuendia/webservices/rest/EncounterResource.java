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
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.VisitObsValue;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A collection where each item corresponds to one patient and contains
 * the encounter and observation data for that patient as of a particular
 * point in time (referred to as the "snapshot time").
 * @see AbstractReadOnlyResource
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/encounters",
    supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class EncounterResource
    extends AbstractReadOnlyResource<Encounter> implements Creatable {
    private final PatientService patientService;
    private final ProjectBuendiaService buendiaService;

    public EncounterResource() {
        super("encounter", Representation.DEFAULT);
        patientService = Context.getPatientService();
        buendiaService = Context.getService(ProjectBuendiaService.class);
    }

    /**
     * Returns all encounters observed after the "since" parameter, or since the beginning of time
     * if the "since" parameter is not set.
     * @param context      used to obtain the "since" parameter for incremental sync.
     * @param snapshotTime unused here; see populateJsonProperties() for details
     * @see AbstractReadOnlyResource#search(RequestContext)
     */
    @Override public List<Encounter> searchImpl(RequestContext context, long snapshotTime) {
        Date syncFrom;
        try {
            syncFrom = RequestUtil.getSyncFromDate(context);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date Format invalid, expected ISO 8601");
        }
        return buendiaService.getEncountersCreatedAtOrAfter(syncFrom);
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

        if (!post.containsKey("uuid")) {
            throw new InvalidObjectDataException("Missing \"uuid\" key for patient");
        }
        Patient patient = patientService.getPatientByUuid(post.get("uuid").toString());
        if (patient == null) {
            throw new InvalidObjectDataException("Patient not found: " + post.get("uuid"));
        }
        Date encounterTime;
        String timestamp = post.get("timestamp").toString();
        try {
            if (timestamp != null) {
                encounterTime = new Date(Long.parseLong(timestamp)*1000L);
            } else {
                // Allow clients to omit the timestamp to use the current server time.
                encounterTime = new Date();
            }
        } catch (NumberFormatException ex) {
            throw new InvalidObjectDataException(
                "Expected seconds since epoch for \"timestamp\" value: " + ex.getMessage());
        }
        Encounter encounter = ObservationsHandler.addEncounter(
            (List) post.get("observations"), (List) post.get("order_uuids"),
            patient, encounterTime, "new observation", "ADULTRETURN",
            // TODO: Consider using patient's location instead of the root location.
            LocationResource.ROOT_UUID);
        if (encounter == null) {
            throw new InvalidObjectDataException("No observations specified");
        }
        SimpleObject simpleObject = new SimpleObject();
        populateJsonProperties(encounter, context, simpleObject, 0);
        return simpleObject;
    }

    /**
     * Retrieves the encounter with a given UUID. Currently not implemented.
     */
    @Override
    protected Encounter retrieveImpl(String uuid, RequestContext context, long snapshotTime) {
        // Not implemented
        return null;
    }

    /**
     * Populates observation and order data for the given encounter, including:
     * <ul>
     *   <li>"timestamp": the encounter datetime in RFC 3339 === ISO 8601 format
     *   <li>"uuid": the encounter's UUID
     *   <li>"observations": {@link SimpleObject} that maps concept UUIDs to values
     *   <li>"order_uuids": unique identifiers of orders executed as part of this encounter.
     * </ul>
     * @param context      unused.
     * @param snapshotTime unused.
     */
    @Override protected void populateJsonProperties(
        Encounter encounter, RequestContext context, SimpleObject encounterJson, long
            snapshotTime) {
        encounterJson.put("patient_uuid", encounter.getPatient().getUuid());
        encounterJson.put("timestamp", Utils.toIso8601(encounter.getEncounterDatetime()));
        encounterJson.put("uuid", encounter.getUuid());

        SimpleObject observations = new SimpleObject();
        List<String> orderUuids = new ArrayList<>();
        for (Obs obs : encounter.getObs()) {
            Concept concept = obs.getConcept();
            if (concept != null &&
                concept.getUuid().equals(DbUtil.getOrderExecutedConcept().getUuid())) {
                orderUuids.add(obs.getOrder().getUuid());
                continue;
            }

            observations.put(obs.getConcept().getUuid(), VisitObsValue.visit(
                obs, new VisitObsValue.ObsValueVisitor<Object>() {
                    @Override public Object visitCoded(Concept value) {
                        return value.getUuid();
                    }

                    @Override public Object visitNumeric(Double value) {
                        return "" + value;
                    }

                    @Override public Object visitBoolean(Boolean value) {
                        return "" + value;
                    }

                    @Override public Object visitText(String value) {
                        return value;
                    }

                    @Override public Object visitDate(Date value) {
                        return Utils.YYYYMMDD_UTC_FORMAT.format(value);
                    }

                    @Override public Object visitDateTime(Date value) {
                        return Utils.toIso8601(value);
                    }
                }));
        }
        if (!observations.isEmpty()) {
            encounterJson.put("observations", observations);
        }
        if (!orderUuids.isEmpty()) {
            encounterJson.put("order_uuids", orderUuids);
        }
    }
}
