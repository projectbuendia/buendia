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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.resource.api.Updatable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Rest API for patients.
 *
 * <p>Expected behavior:
 * <ul>
 * <li>GET /patient returns all patients ({@link #getAll(RequestContext)})
 * <li>GET /patient?q=[query] returns patients whose name or ID contains the query string
 *     ({@link #search(RequestContext)})
 * <li>GET /patient/[UUID] returns a single patient ({@link #retrieve(String, RequestContext)})
 * <li>POST /patient creates a patient ({@link #create(SimpleObject, RequestContext)}
 * <li>POST /patient/[UUID] updates a patient ({@link #update(String, SimpleObject, RequestContext)})
 * </ul>
 *
 * <p>Each operation handles Patient resources in the following JSON form:
 *
 * <pre>
 * {
 *   "uuid": "e5e755d4-f646-45b6-b9bc-20410e97c87c", // assigned by OpenMRS, not required for creation
 *   "id": "567", // required unique id specified by user
 *   "gender": "F", // required as "M" or "F", unfortunately
 *   "birthdate": "1990-02-17", // required, but can be estimated
 *   "given_name": "Jane", // required, "Unknown" suggested if not known
 *   "family_name": "Doe", // required, "Unknown" suggested if not known
 *   "assigned_location": { // optional, but highly encouraged
 *     "uuid": "0a49d383-7019-4f1f-bf4b-875f2cd58964", // UUID of the patient's assigned location
 *   }
 * },
 * </pre>
 * (Results may also contain deprecated fields other than those described above.)
 *
 * <p>If an error occurs, the response will contain the following:
 * <pre>
 * {
 *   "error": {
 *     "message": "[error message]",
 *     "code": "[breakpoint]",
 *     "detail": "[stack trace]"
 *   }
 * }
 * </pre>
 */
@Resource(
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patient",
    supportedClass = Patient.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class PatientResource implements Listable, Searchable, Retrievable, Creatable, Updatable {
    // Fake values
    private static final User CREATOR = new User(1);
    private static final String FACILITY_NAME = "Kailahun";  // TODO: Use a real facility name.
    static final RequestLogger logger = RequestLogger.LOGGER;

    // JSON property names
    private static final String ID = "id";
    private static final String UUID = "uuid";
    private static final String GENDER = "gender";
    private static final String BIRTHDATE = "birthdate";
    private static final String GIVEN_NAME = "given_name";
    private static final String FAMILY_NAME = "family_name";
    private static final String ASSIGNED_LOCATION = "assigned_location";
    private static final String PARENT_UUID = "parent_uuid";

    private static Log log = LogFactory.getLog(PatientResource.class);
    private final PatientService patientService;
    private static final Object createPatientLock = new Object();

    public PatientResource() {
        patientService = Context.getPatientService();
    }

    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "getAll");
            SimpleObject result = getAllInner();
            logger.reply(context, this, "getAll", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "getAll", e);
            throw e;
        }
    }

    private SimpleObject getAllInner() throws ResponseException {
        List<Patient> patients = patientService.getAllPatients();
        return getSimpleObjectWithResults(patients);
    }

    private String validateGender(String value) {
        if (value.equals("F") || value.equals("M")) return value;
        throw new InvalidObjectDataException(
                "Gender should be specified as \"F\" or \"M\"");
    }

    public Object create(SimpleObject json, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "create", json);
            Object result = createInner(json);
            logger.reply(context, this, "create", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "create", e);
            throw e;
        }
    }

    private Object createInner(SimpleObject json) throws ResponseException {
        // We really want this to use XForms, but let's have a simple default
        // implementation for early testing

        if (!json.containsKey(ID)) {
            throw new InvalidObjectDataException("Patient ID is required but not specified");
        }
        String id = (String) json.get(ID);
        List<PatientIdentifierType> identifierTypes =
                Arrays.asList(DbUtil.getMsfIdentifierType());
        Patient patient = null;
        synchronized (createPatientLock) {
            List<Patient> existing = patientService.getPatients(
                    null, id, identifierTypes, true /* exact identifier match */);
            if (!existing.isEmpty()) {
                patient = existing.get(0);
                String given = patient.getGivenName();
                given = (given == null) ? "" : given;
                String family = patient.getFamilyName();
                family = (family == null) ? "" : family;
                String name = (given + " " + family).trim();
                throw new InvalidObjectDataException(String.format(
                        "Another patient (%s) already has the ID \"%s\"",
                        name.isEmpty() ? "with no name" : "named " + name, id));
            }

            patient = jsonToPatient(json);
            patientService.savePatient(patient);
        }
        // Observation for first symptom date
        ObservationsHandler.addEncounter(
                (List) json.get("observations"), null,
                patient, patient.getDateCreated(), "Initial triage",
                "ADULTINITIAL", LocationResource.TRIAGE_UUID);
        return patientToJson(patient);
    }

    protected static Patient jsonToPatient(SimpleObject json) {
        Patient patient = new Patient();
        // TODO: do this properly from authentication
        patient.setCreator(CREATOR);
        patient.setDateCreated(new Date());

        if (json.containsKey(GENDER)) {
            patient.setGender((String) json.get(GENDER));
        }
        if (json.containsKey(BIRTHDATE)) {
            patient.setBirthdate(
                Utils.parseDate((String) json.get(BIRTHDATE), BIRTHDATE));
        }

        PersonName pn = new PersonName();
        if (json.containsKey(GIVEN_NAME)) {
            pn.setGivenName((String) json.get(GIVEN_NAME));
        }
        if (json.containsKey(FAMILY_NAME)) {
            pn.setFamilyName((String) json.get(FAMILY_NAME));
        }

        pn.setCreator(patient.getCreator());
        pn.setDateCreated(patient.getDateCreated());
        patient.addName(pn);

        if (json.containsKey(ID)) {
            PatientIdentifier identifier = new PatientIdentifier();
            identifier.setCreator(patient.getCreator());
            identifier.setDateCreated(patient.getDateCreated());
            identifier.setIdentifier((String) json.get(ID));
            // TODO/generalize: Instead of getting the root location by a hardcoded
            // name (almost certainly an inappropriate name), change the helper
            // function to DbUtil.getRootLocation().
            identifier.setLocation(DbUtil.getLocationByName(FACILITY_NAME, null));
            identifier.setIdentifierType(DbUtil.getMsfIdentifierType());
            identifier.setPreferred(true);
            patient.addIdentifier(identifier);
        }

        // Set assigned location last, as doing so saves the patient, which could fail
        // if performed in the middle of patient creation.
        if (json.containsKey(ASSIGNED_LOCATION)) {
            Map assignedLocation = (Map) json.get(ASSIGNED_LOCATION);
            if (assignedLocation != null) {
                setLocation(patient, (String) assignedLocation.get(UUID));
            }
        }

        return patient;
    }

    @Override
    public String getUri(Object instance) {
        Patient patient = (Patient) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + patient.getUuid();
    }

    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "search");
            SimpleObject result = searchInner(context);
            logger.reply(context, this, "search", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "search", e);
            throw e;
        }
    }

    private SimpleObject searchInner(RequestContext requestContext) throws ResponseException {
        // Partial string query for searches.
        String query = requestContext.getParameter("q");

        // If set, also search on uuid. By default uuid is skipped.
        boolean searchUuid = (requestContext.getParameter("searchUuid") != null);

        // Retrieve all patients and filter the list based on the query.
        List<Patient> filteredPatients = filterPatients(
            query, searchUuid, patientService.getAllPatients());

        return getSimpleObjectWithResults(filteredPatients);
    }

    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "retrieve", uuid);
            Object result = retrieveInner(uuid);
            logger.reply(context, this, "retrieve", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "retrieve", e);
            throw e;
        }
    }

    private Object retrieveInner(String uuid) throws ResponseException {
        Patient patient = patientService.getPatientByUuid(uuid);
        if (patient == null) {
            throw new ObjectNotFoundException();
        }
        return patientToJson(patient);
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }

    private List<Patient> filterPatients(
            String query, boolean searchUuid, List<Patient> allPatients) {
        List<Patient> filteredPatients = new ArrayList<>();

        // Filter patients by id, name, and MSF id. Don't use patientService.getPatients() for
        // this, as the behavior does not match the expected behavior from the API docs.
        PatientIdentifierType msfIdentifierType = DbUtil.getMsfIdentifierType();
        for (Patient patient : allPatients) {
            boolean match = false;

            // First check the patient's full name.
            for (PersonName name : patient.getNames()) {
                if (StringUtils.containsIgnoreCase(name.getFullName(), query)) {
                    match = true;
                    break;
                }
            }

            // Short-circuit on name match.
            if (match) {
                filteredPatients.add(patient);
                continue;
            }

            // Next check the patient's MSF id.
            for (PatientIdentifier identifier : patient.getPatientIdentifiers(msfIdentifierType)) {
                if (StringUtils.containsIgnoreCase(identifier.getIdentifier(), query)) {
                    match = true;
                    break;
                }
            }

            if (match || (searchUuid && StringUtils.containsIgnoreCase(patient.getUuid(), query))) {
                filteredPatients.add(patient);
            }
        }

        return filteredPatients;
    }

    private SimpleObject getSimpleObjectWithResults(List<Patient> patients) {
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Patient patient : patients) {
            jsonResults.add(patientToJson(patient));
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }

    @Override
    public Object update(String uuid, SimpleObject simpleObject, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "update", uuid + ", " + simpleObject);
            Object result = updateInner(uuid, simpleObject);
            logger.reply(context, this, "update", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "update", e);
            throw e;
        }
    }

    /**
     * Receives a SimpleObject that is parsed from the Gson serialization of a client-side
     * Patient bean.  It has the following semantics:
     * <ul>
     *     <li>Any field set overwrites the current content
     *     <li>Any field with a key but value == null deletes the current content
     *     <li>Any field whose key is not present leaves the current content unchanged
     *     <li>Subfields of location and age are not merged; instead the whole item is replaced
     *     <li>If the client requests a change that is illegal, that is an error. Really the
     *         whole call should fail, but for now there may be partial updates
     * </ul>
     */
    private Object updateInner(String uuid, SimpleObject simpleObject) throws ResponseException {
        Patient patient = patientService.getPatientByUuid(uuid);
        if (patient == null) {
            throw new ObjectNotFoundException();
        }

        if (applyEdits(patient, simpleObject)) {
            patientService.savePatient(patient);
        }
        return patientToJson(patient);
    }

    /** Applies edits to a Patient.  Returns true if any changes were made. */
    protected boolean applyEdits(Patient patient, SimpleObject edits) {
        boolean changedPatient = false;
        for (Map.Entry<String, Object> entry : edits.entrySet()) {
            PersonName oldName, newName;
            switch (entry.getKey()) {

                // ==== JSON keys that update attributes of the Patient entity.

                case FAMILY_NAME:
                    oldName = patient.getPersonName();
                    newName = new PersonName();
                    newName.setFamilyName((String) entry.getValue());
                    newName.setGivenName(oldName.getGivenName());
                    patient.addName(newName);
                    oldName.setVoided(true);
                    changedPatient = true;
                    break;
                case GIVEN_NAME:
                    oldName = patient.getPersonName();
                    newName = new PersonName();
                    newName.setGivenName((String) entry.getValue());
                    newName.setFamilyName(oldName.getFamilyName());
                    patient.addName(newName);
                    oldName.setVoided(true);
                    changedPatient = true;
                    break;
                case ASSIGNED_LOCATION:
                    Map assignedLocation = (Map) entry.getValue();
                    setLocation(patient, (String) assignedLocation.get(UUID));
                    break;
                case BIRTHDATE:
                    patient.setBirthdate(Utils.parseDate((String) entry.getValue(), BIRTHDATE));
                    changedPatient = true;
                    break;
                case GENDER:
                    patient.setGender(validateGender((String) entry.getValue()));
                    changedPatient = true;
                    break;
                default:
                    log.warn("Property is nonexistent or not updatable; ignoring: " + entry);
                    break;
            }
        }
        return changedPatient;
    }

    private static void setLocation(Patient patient, String locationUuid) {
        // Apply the given assigned location to a patient, if locationUuid is not null.
        if (locationUuid == null) {
            return;
        }

        Location location = Context.getLocationService().getLocationByUuid(locationUuid);
        if (location != null) {
            DbUtil.setPersonAttributeValue(patient,
                    DbUtil.getAssignedLocationAttributeType(),
                    Integer.toString(location.getId()));
        }
    }

    protected static SimpleObject patientToJson(Patient patient) {
        SimpleObject jsonForm = new SimpleObject();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (patient != null) {
            jsonForm.add(UUID, patient.getUuid());
            PatientIdentifier patientIdentifier =
                    patient.getPatientIdentifier(DbUtil.getMsfIdentifierType());
            if (patientIdentifier != null) {
                jsonForm.add(ID, patientIdentifier.getIdentifier());
            }
            jsonForm.add(GENDER, patient.getGender());
            if (patient.getBirthdate() != null) {
                jsonForm.add(BIRTHDATE, dateFormat.format(patient.getBirthdate()));
            }
            jsonForm.add(GIVEN_NAME, patient.getGivenName());
            jsonForm.add(FAMILY_NAME, patient.getFamilyName());

            // TODO: refactor so we have a single assigned location with a uuid,
            // and we walk up the tree to get extra information for the patient.
            String assignedLocation = DbUtil.getPersonAttributeValue(
                    patient, DbUtil.getAssignedLocationAttributeType());
            if (assignedLocation != null) {
                LocationService locationService = Context.getLocationService();
                Location location = locationService.getLocation(
                    Integer.valueOf(assignedLocation));

                SimpleObject locationJson = new SimpleObject();
                locationJson.add(UUID, location.getUuid());
                if (location.getParentLocation() != null) {
                    locationJson.add(PARENT_UUID, location.getParentLocation().getUuid());
                }
                jsonForm.add(ASSIGNED_LOCATION, locationJson);
            }
        }
        return jsonForm;
    }
}
