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
import org.openmrs.api.EncounterService;
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
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A collection where each item corresponds to one patient and contains
 * the encounter and observation data for that patient as of a particular
 * point in time (referred to as the "snapshot time").
 * @see AbstractReadOnlyResource
 */
// TODO: Merge with PatientResource and let clients use a query parameter to
// indicate whether encounter data should be included with each returned patient.
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patientencounters",
    supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class PatientEncountersResource
    extends AbstractReadOnlyResource<Patient> implements Creatable {
    private final PatientService patientService;
    private final EncounterService encounterService;

    public PatientEncountersResource() {
        super("patient", Representation.DEFAULT);
        patientService = Context.getPatientService();
        encounterService = Context.getEncounterService();
    }

    /**
     * Returns all patients.  The retrieved records will be filled in with
     * each patient's encounter and observation data by
     * {@link #populateJsonProperties(Patient, RequestContext, SimpleObject, long)}
     * on its way to becoming JSON that is sent to the client.
     * @param context      unused here; see populateJsonProperties() for details
     * @param snapshotTime unused here; see populateJsonProperties() for details
     * @see AbstractReadOnlyResource#search(RequestContext)
     */
    @Override public List<Patient> searchImpl(RequestContext context, long snapshotTime) {
        return patientService.getAllPatients();
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
        return encounterToJson(encounter);
    }

    /**
     * Converts an encounter to its JSON representation, filling in observation data.
     * @param encounter an encounter instance
     * @return a SimpleObject representing the encounter, ready to be serialized to JSON
     */
    private SimpleObject encounterToJson(Encounter encounter) {
        SimpleObject encounterJson = new SimpleObject();
        // TODO: Check what format this ends up in.
        encounterJson.put("timestamp", Utils.toIso8601(encounter.getEncounterDatetime()));
        SimpleObject observations = new SimpleObject();
        List<String> orderUuids = new ArrayList<>();
        for (Obs obs : encounter.getObs()) {
            // TODO/simplify: Move this .put() call outside the loop.
            encounterJson.put("uuid", encounter.getUuid());
            Concept concept = obs.getConcept();
            if (concept != null &&
                concept.getUuid().equals(DbUtil.getOrderExecutedConcept().getUuid())) {
                orderUuids.add(obs.getOrder().getUuid());
                continue;
            }
            observations.put(obs.getConcept().getUuid(), VisitObsValue.visit(
                obs, new VisitObsValue.ObsValueVisitor<String>() {
                    @Override public String visitCoded(Concept value) {
                        return value.getUuid();
                    }

                    @Override public String visitNumeric(Double value) {
                        return String.valueOf(value);
                    }

                    @Override public String visitBoolean(Boolean value) {
                        return String.valueOf(value);
                    }

                    @Override public String visitText(String value) {
                        return value;
                    }

                    @Override public String visitDate(Date value) {
                        return Utils.YYYYMMDD_FORMAT.format(value);
                    }

                    @Override public String visitDateTime(Date value) {
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
        return encounterJson;
    }

    /**
     * Retrieves the patient with a given UUID.  The retrieved record will
     * be filled in with the patient's encounter and observation data by
     * {@link #populateJsonProperties(Patient, RequestContext, SimpleObject, long)}
     * on its way to becoming JSON that is sent to the client.
     * @param context      unused here; see populateJsonProperties() for details
     * @param snapshotTime unused here; see populateJsonProperties() for details
     * @see AbstractReadOnlyResource#retrieve(String, RequestContext)
     */
    @Override
    protected Patient retrieveImpl(String uuid, RequestContext context, long snapshotTime) {
        return patientService.getPatientByUuid(uuid);
    }

    /**
     * Fetches the encounter and observation data for a patient as of a given
     * snapshotTime, optionally also omitting data that was fetched previously,
     * and adds the following fields to the given {@link SimpleObject}:
     * <ul>
     * <li>"encounters": {@link List} of {@link SimpleObject}s, each containing:
     * <ul>
     * <li>"timestamp": the encounter datetime in RFC 3339 format
     * <li>"uuid": the encounter's UUID
     * <li>"observations": {@link SimpleObject} that maps concept UUIDs to values
     * </ul>
     * </ul>
     * @param context      the request context; supports the optional "sm" query
     *                     parameter, which lets a client fetch only the data that is new since
     *                     a previous fetch (the results will contain only the encounter and
     *                     observation data with creation or modification times at or after the
     *                     specified time).  To fetch just the new data since a previous fetch,
     *                     set "sm" to the snapshotTime that was returned in that previous fetch.
     * @param snapshotTime a server clock time in epoch milliseconds; only
     *                     encounters and observations that existed as of this snapshot time
     *                     (i.e. created strictly before snapshotTime) will be returned.
     */
    @Override protected void populateJsonProperties(
        Patient patient, RequestContext context, SimpleObject json, long snapshotTime) {
        String parameter = context.getParameter("sm");
        Long startMillisecondsInclusive = null;
        if (parameter != null) {
            // Fail fast throwing number format exception to aid debugging.
            startMillisecondsInclusive = Long.parseLong(parameter);
        }
        List<Encounter> encountersByPatient;
        if (startMillisecondsInclusive == null) {
            encountersByPatient = encounterService.getEncountersByPatient(patient);
        } else {
            // It would be nice to be able to use the getEncounters() method here, which has some
            // filtering parameters such as fromDate and toDate.  Unfortunately, these restrict
            // according to the encounter date, whereas we need to filter by creation/modification
            // date in order to support incremental fetching correctly.  (Filtering by encounter
            // date would miss encounters dated in the past that are added later.)  Filtering by
            // creation/modification date been added as a feature request to OpenMRS at
            // https://issues.openmrs.org/browse/TRUNK-4571
            //
            // Until this feature is available, we have two options:
            // 1. Use the DAO directly, hooking in to the spring injection code to get it.
            // 2. Load the encounters, and then filter in RAM before getting the observations.
            // For now, we are going with 2 and hoping it is fast enough.
            encountersByPatient = filterEncountersByModificationTime(startMillisecondsInclusive,
                encounterService.getEncountersByPatient(patient));
        }
        List<SimpleObject> encounters = new ArrayList<>();
        for (Encounter encounter : filterBeforeSnapshotTime(snapshotTime, encountersByPatient)) {
            encounters.add(encounterToJson(encounter));
        }
        json.put("encounters", encounters);
    }

    /**
     * Given a list of encounters, selects those that were created or modified
     * at or after a specified time.
     * @param startMillisecondsInclusive a timestamp in epoch milliseconds
     * @param encountersByPatient        a list of encounters or observations
     * @return a new list of the items with creation or modification times &gt;=
     * startMillisecondsInclusive
     */
    private List<Encounter> filterEncountersByModificationTime(
        long startMillisecondsInclusive, List<Encounter> encountersByPatient) {
        ArrayList<Encounter> filtered = new ArrayList<>();
        for (Encounter encounter : encountersByPatient) {
            // Sigh.  OpenMRS does not set modification time on initial create,
            // so we have to check both the creation and modification times.
            if (encounter.getDateCreated().getTime() >= startMillisecondsInclusive || (
                encounter.getDateChanged() != null
                    && encounter.getDateChanged().getTime() >= startMillisecondsInclusive)) {
                filtered.add(encounter);
            }
        }
        return filtered;
    }

    /**
     * Given a list of encounters, selects those that existed at a given time.
     * @param snapshotTime        a timestamp in epoch milliseconds
     * @param encountersByPatient a list of encounters
     * @return a new list of encounters with creation times strictly less than snapshotTime
     */
    private List<Encounter> filterBeforeSnapshotTime(
        long snapshotTime, List<Encounter> encountersByPatient) {
        List<Encounter> filtered = new ArrayList<>();
        for (Encounter encounter : encountersByPatient) {
            if (encounter.getDateCreated().getTime() < snapshotTime) {
                filtered.add(encounter);
            }
        }
        return filtered;
    }
}
