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
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.SyncPage;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Rest API for patients.
 * <p/>
 * <p>Expected behavior:
 * <ul>
 * <li>GET /patients returns all patients ({@link #getAll(RequestContext)})
 * <li>GET /patients/[UUID] returns a single patient ({@link #retrieve(String, RequestContext)})
 * <li>GET /patients?id=[id] returns a search for a patient with the specified MSF (i.e. not
 * OpenMRS) id.
 * <li>POST /patients creates a patient ({@link #create(SimpleObject, RequestContext)}
 * <li>POST /patients/[UUID] updates a patient ({@link #update(String, SimpleObject,
 * RequestContext)})
 * </ul>
 * <p/>
 * <p>Each operation handles Patient resources in the following JSON form:
 * <p/>
 * <pre>
 * {
 *   "uuid": "e5e755d4-f646-45b6-b9bc-20410e97c87c", // assigned by OpenMRS, not required for
 *   creation
 *   "id": "567", // required unique id specified by user
 *   "sex": "F", // required as "M" or "F", unfortunately
 *   "birthdate": "1990-02-17", // required, but can be estimated
 *   "given_name": "Jane", // required, "Unknown" suggested if not known
 *   "family_name": "Doe", // required, "Unknown" suggested if not known
 *   "assigned_location": { // optional, but highly encouraged
 *     "uuid": "0a49d383-7019-4f1f-bf4b-875f2cd58964", // UUID of the patient's assigned location
 *   }
 * },
 * </pre>
 * (Results may also contain deprecated fields other than those described above.)
 * <p/>
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
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patients",
    supportedClass = Patient.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class PatientResource implements Listable, Searchable, Retrievable, Creatable, Updatable {

    private static final SimpleDateFormat PATIENT_BIRTHDATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd");
    static {
        PATIENT_BIRTHDATE_FORMAT.setTimeZone(Utils.UTC);
    }

    private static final int MAX_PATIENTS_PER_PAGE = 500;

    // Fake values
    private static final User CREATOR = new User(1);
    static final RequestLogger logger = RequestLogger.LOGGER;

    // JSON property names
    private static final String ID = "id";
    private static final String UUID = "uuid";
    private static final String SEX = "sex";
    private static final String BIRTHDATE = "birthdate";
    private static final String GIVEN_NAME = "given_name";
    private static final String FAMILY_NAME = "family_name";
    private static final String ASSIGNED_LOCATION = "assigned_location";
    private static final String PARENT_UUID = "parent_uuid";
    private static final String VOIDED = "voided";

    private static Log log = LogFactory.getLog(PatientResource.class);
    private static final Object createPatientLock = new Object();
    private final PatientService patientService;
    private final ProjectBuendiaService buendiaService;

    public PatientResource() {
        patientService = Context.getPatientService();
        buendiaService = Context.getService(ProjectBuendiaService.class);
    }

    @Override public SimpleObject getAll(RequestContext context) throws ResponseException {
        // #search covers a more general case of of #getAll, so we just forward through.
        return search(context);
    }

    private SimpleObject handleSync(RequestContext context) throws ResponseException {
        SyncToken syncToken = RequestUtil.mustParseSyncToken(context);
        Date requestTime = new Date();

        SyncPage<Patient> patients = buendiaService.getPatientsModifiedAtOrAfter(
                syncToken,
                syncToken != null /* includeVoided */,
                MAX_PATIENTS_PER_PAGE /* maxResults */);

        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Patient patient : patients.results) {
            jsonResults.add(patientToJson(patient));
        }
        SyncToken newToken =
                SyncTokenUtils.clampSyncTokenToBufferedRequestTime(patients.syncToken, requestTime);
        // If we fetched a full page, there's probably more data available.
        boolean more = patients.results.size() == MAX_PATIENTS_PER_PAGE;
        return ResponseUtil.createIncrementalSyncResults(jsonResults, newToken, more);
    }

    // TODO: consolidate the incremental sync timestamping / wrapper logic for this and
    // EncountersResource into the same class.
    private SimpleObject getSimpleObjectWithResults(List<Patient> patients) {
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Patient patient : patients) {
            jsonResults.add(patientToJson(patient));
        }
        SimpleObject wrapper = new SimpleObject();
        wrapper.put("results", jsonResults);
        return wrapper;
    }

    protected static SimpleObject patientToJson(Patient patient) {
        SimpleObject jsonForm = new SimpleObject();

        jsonForm.add(UUID, patient.getUuid());
        jsonForm.add(VOIDED, patient.isPersonVoided());

        if (patient.isPersonVoided()) {
            // early return, we don't need the rest of the data.
            return jsonForm;
        }

        PatientIdentifier ident = patient.getPatientIdentifier();  // first preferred identifier

        // The client-side representation of an identifier is either:
        // "*" followed by an integer, where the integer is a local
        // (type "LOCAL") server-generated identifier; or otherwise
        // it is an MSF (type "MSF") client-provided identifier.
        if (ident.getIdentifierType().equals(DbUtil.getIdentifierTypeLocal())) {
            jsonForm.add(ID, "*" + ident.getIdentifier());
        } else {
            jsonForm.add(ID, ident.getIdentifier());
        }
        jsonForm.add(SEX, patient.getGender());
        if (patient.getBirthdate() != null) {
            jsonForm.add(BIRTHDATE, PATIENT_BIRTHDATE_FORMAT.format(patient.getBirthdate()));
        }
        String givenName = patient.getGivenName();
        if (!givenName.equals(MISSING_NAME)) {
            jsonForm.add(GIVEN_NAME, patient.getGivenName());
        }
        String familyName = patient.getFamilyName();
        if (!familyName.equals(MISSING_NAME)) {
            jsonForm.add(FAMILY_NAME, patient.getFamilyName());
        }

        // TODO: refactor so we have a single assigned location with a uuid,
        // and we walk up the tree to get extra information for the patient.
        String assignedLocation = DbUtil.getPersonAttributeValue(
            patient, DbUtil.getAssignedLocationAttributeType());
        if (assignedLocation != null) {
            LocationService locationService = Context.getLocationService();
            Location location = locationService.getLocation(
                Integer.valueOf(assignedLocation));
            if (location != null) {
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

    private String getFullName(Patient patient) {
        String given = patient.getGivenName();
        given = given.equals(MISSING_NAME) ? "" : given;
        String family = patient.getFamilyName();
        family = family.equals(MISSING_NAME) ? "" : family;
        return (given + " " + family).trim();
    }

    private Object createInner(SimpleObject json) throws ResponseException {
        // We really want this to use XForms, but let's have a simple default
        // implementation for early testing

        Patient patient;
        synchronized (createPatientLock) {
            String id = (String) json.get(ID);
            if (id != null) {
                requireValidUniqueMsfIdentifier(id);
            }
            String uuid = (String) json.get(UUID);
            if (uuid != null) {
                Patient uuidMatch = patientService.getPatientByUuid(uuid);
                if (uuidMatch != null) {
                    String name = getFullName(uuidMatch);
                    throw new InvalidObjectDataException(String.format(
                        "Another patient (%s) already has the UUID \"%s\"",
                        name.isEmpty() ? "with no name" : "named " + name, uuid));
                }
            }

            patient = jsonToPatient(json);
            patientService.savePatient(patient);

            // For LOCAL-type identifiers, we can only determine the identifier after
            // the Patient and PatientIdentifier objects have been saved.  At this
            // point the PatientIdentifier has a freshly-generated ID column, which
            // we use to construct the string identifier.
            PatientIdentifier ident = patient.getPatientIdentifier();
            if (ident.getIdentifierType().equals(DbUtil.getIdentifierTypeLocal())) {
                ident.setIdentifier("" + ident.getId());
                patientService.savePatientIdentifier(ident);
            }

        }
        // Store any initial observations that are included with the new patient.
        ObservationUtils.addEncounter(
            (List) json.get("observations"), null,
            patient, patient.getDateCreated(), "New patient", "ADULTINITIAL", null);
        return patientToJson(patient);
    }

    private static String normalizeSex(String sex) {
        if (sex == null) {
            return "U";
        } else if (sex.trim().matches("^[FfMmOoUu]")) {
            return sex.trim().substring(0, 1).toUpperCase();  // F, M, O, and U are valid
        } else {
            return "U";
        }
    }

    /** OpenMRS refuses to store empty names, so we use "." to represent a missing name. */
    private static final String MISSING_NAME = ".";

    /** Normalizes a name to something OpenMRS will accept. */
    private static String normalizeName(String name) {
        name = (name == null) ? "" : name.trim();
        return name.isEmpty() ? MISSING_NAME : name;
    }

    protected static Patient jsonToPatient(SimpleObject json) {
        Patient patient = new Patient();
        // TODO: do this properly from authentication
        patient.setCreator(CREATOR);
        patient.setDateCreated(new Date());

        if (json.containsKey(UUID)) {
            patient.setUuid((String) json.get(UUID));
        }

        String sex = (String) json.get(SEX);
        // OpenMRS calls it "gender"; we use it for physical sex (as other implementations do).
        patient.setGender(normalizeSex(sex));

        if (json.containsKey(BIRTHDATE)) {
            patient.setBirthdate(Utils.parseLocalDate((String) json.get(BIRTHDATE), BIRTHDATE));
        }

        PersonName pn = new PersonName();
        pn.setGivenName(normalizeName((String) json.get(GIVEN_NAME)));
        pn.setFamilyName(normalizeName((String) json.get(FAMILY_NAME)));
        pn.setCreator(patient.getCreator());
        pn.setDateCreated(patient.getDateCreated());
        patient.addName(pn);

        PatientIdentifier identifier = new PatientIdentifier();
        patient.addIdentifier(identifier);
        identifier.setCreator(patient.getCreator());
        identifier.setDateCreated(patient.getDateCreated());
        identifier.setLocation(DbUtil.getDefaultLocation());
        identifier.setPreferred(true);

        // OpenMRS requires that every patient have a preferred identifier.  If the
        // incoming "id" field is non-blank, it becomes the MSF identifier; otherwise,
        // we use our database to generate a numeric locally unique identifier.
        if (json.containsKey(ID) && !((String) json.get(ID)).isEmpty()) {
            identifier.setIdentifierType(DbUtil.getIdentifierTypeMsf());
            identifier.setIdentifier((String) json.get(ID));
        } else {
            identifier.setIdentifierType(DbUtil.getIdentifierTypeLocal());
            // To generate an integer ID, we need to save the patient identifier and
            // let the table fill in the ID AUTO_INCREMENT column.  But OpenMRS will
            // not let us save the patient identifier with a blank identifier string,
            // so let's generate a temporary unique identifier just for this purpose.
            // We also can't save the patient identifier until after the patient is
            // saved, so, after that's done, we'll fix up the identifier to use the
            // generated number in the ID column.
            identifier.setIdentifier("temp-" + new Date().getTime());
        }

        // Set assigned location last, as doing so saves the patient, which could fail
        // if performed in the middle of patient creation.
        if (json.containsKey(ASSIGNED_LOCATION)) {
            String assignedLocationUuid = null;
            Object assignedLocation = json.get(ASSIGNED_LOCATION);
            if (assignedLocation instanceof String) {
                assignedLocationUuid = (String) assignedLocation;
            }
            if (assignedLocation instanceof Map) {
                assignedLocationUuid = (String) ((Map) assignedLocation).get(UUID);
            }
            if (assignedLocationUuid != null) {
                setLocation(patient, assignedLocationUuid);
            }
        }

        return patient;
    }

    private static void setLocation(Patient patient, String locationUuid) {
        // Apply the given assigned location to a patient, if locationUuid is not null.
        if (locationUuid == null) return;

        Location location = Context.getLocationService().getLocationByUuid(locationUuid);
        if (location != null) {
            DbUtil.setPersonAttributeValue(patient,
                DbUtil.getAssignedLocationAttributeType(),
                Integer.toString(location.getId()));
        }
    }

    @Override public String getUri(Object instance) {
        Patient patient = (Patient) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + patient.getUuid();
    }

    @Override public SimpleObject search(RequestContext context) throws ResponseException {
        // If there's an ID, run an actual search, otherwise, run a sync.
        String patientId = context.getParameter("id");
        if (patientId != null) {
            try {
                logger.request(context, this, "search");
                SimpleObject result = searchInner(patientId);
                logger.reply(context, this, "search", result);
                return result;
            }
            catch (Exception e) {
                logger.error(context, this, "search", e);
                throw e;
            }
        } else {
            try {
                logger.request(context, this, "handleSync");
                SimpleObject result = handleSync(context);
                logger.reply(context, this, "handleSync", result);
                return result;
            } catch (Exception e) {
                logger.error(context, this, "handleSync", e);
                throw e;
            }
        }
    }

    /**
     * The patientId is the client-side representation of an identifier.
     * It is either "*" followed by an integer, where the integer is a
     * local (type "LOCAL") server-generated identifier; or otherwise it
     * is an MSF (type "MSF") client-provided identifier.
     */
    private SimpleObject searchInner(String patientId) throws ResponseException {
        List<PatientIdentifierType> identTypes = new ArrayList<>();
        String ident;
        if (patientId.startsWith("*")) {
            // The ID refers to a local server-generated identifier.
            identTypes.add(DbUtil.getIdentifierTypeLocal());
            ident = patientId.substring(1);
        } else {
            // The ID refers to an MSF client-provided identifier.
            identTypes.add(DbUtil.getIdentifierTypeMsf());
            ident = patientId;
        }
        List<Patient> patients = patientService.getPatients(null, ident, identTypes, true);
        return getSimpleObjectWithResults(patients);
    }

    @Override public Object retrieve(String uuid, RequestContext context) throws ResponseException {
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

    @Override public List<Representation> getAvailableRepresentations() {
        return Collections.singletonList(Representation.DEFAULT);
    }

    @Override
    public Object update(String uuid, SimpleObject simpleObject, RequestContext context) throws
        ResponseException {
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
     * <li>Any field set overwrites the current content
     * <li>Any field with a key but value == null deletes the current content
     * <li>Any field whose key is not present leaves the current content unchanged
     * <li>Subfields of location and age are not merged; instead the whole item is replaced
     * <li>If the client requests a change that is illegal, that is an error. Really the
     * whole call should fail, but for now there may be partial updates
     * </ul>
     */
    private Object updateInner(String uuid, SimpleObject simpleObject) throws ResponseException {
        Patient patient = patientService.getPatientByUuid(uuid);
        if (patient == null) {
            throw new ObjectNotFoundException();
        }

        applyEdits(patient, simpleObject);
        return patientToJson(patient);
    }

    /** Applies edits to a Patient.  Returns true if any changes were made. */
    protected void applyEdits(Patient patient, SimpleObject edits) {
        boolean changedPatient = false;
        String newGivenName = null;
        String newFamilyName = null;
        String newId = null;
        for (Map.Entry<String, Object> entry : edits.entrySet()) {
            switch (entry.getKey()) {
                // ==== JSON keys that update attributes of the Patient entity.

                case FAMILY_NAME:
                    newFamilyName = (String) entry.getValue();
                    break;
                case GIVEN_NAME:
                    newGivenName = (String) entry.getValue();
                    break;
                case ASSIGNED_LOCATION:
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        setLocation(patient, (String) value);
                    } else if (value instanceof Map) {
                        setLocation(patient, (String) ((Map) value).get(UUID));
                    }
                    break;
                case BIRTHDATE:
                    patient.setBirthdate(Utils.parseLocalDate((String) entry.getValue(), BIRTHDATE));
                    changedPatient = true;
                    break;
                case ID:
                    newId = (String) entry.getValue();
                    break;
                default:
                    log.warn("Property is nonexistent or not updatable; ignoring: " + entry);
                    break;
            }
        }

        PatientIdentifier identifier = patient.getPatientIdentifier(DbUtil.getIdentifierTypeMsf());
        if (newId != null && !newId.isEmpty() && (identifier == null || !newId.equals(identifier.getIdentifier()))) {
            synchronized (createPatientLock) {
                requireValidUniqueMsfIdentifier(newId);

                if (identifier != null) {
                    patient.removeIdentifier(identifier);
                }
                identifier = new PatientIdentifier();
                identifier.setCreator(patient.getCreator());
                identifier.setDateCreated(patient.getDateCreated());
                identifier.setLocation(DbUtil.getDefaultLocation());
                identifier.setIdentifier(newId);
                identifier.setIdentifierType(DbUtil.getIdentifierTypeMsf());
                identifier.setPreferred(true);
                patient.addIdentifier(identifier);
                patientService.savePatient(patient);
            }
            changedPatient = true;
        }
        if (newGivenName != null || newFamilyName != null) {
            PersonName oldName = patient.getPersonName();
            if (!normalizeName(newGivenName).equals(oldName.getGivenName())
                || !normalizeName(newFamilyName).equals(oldName.getFamilyName())) {
                PersonName newName = new PersonName();
                newName.setGivenName(
                    newGivenName != null ? normalizeName(newGivenName) : oldName.getGivenName());
                newName.setFamilyName(
                    newFamilyName != null ? normalizeName(newFamilyName) : oldName.getFamilyName());
                patient.addName(newName);
                oldName.setVoided(true);
                changedPatient = true;
            }
        }
        if (changedPatient) {
            patientService.savePatient(patient);
        }
    }

    /** Verifies that a string is acceptable as a unique, well-formed identifier. */
    private void requireValidUniqueMsfIdentifier(String ident) {
        // To prevent collision between identifier types, we don't permit the
        // client to try to create an MSF identifier that starts with "*".
        if (ident.startsWith("*")) {
            throw new InvalidObjectDataException(String.format(
                "\"%s\" is not a valid ID; the \"*\" prefix is reserved for server-generated IDs",
                ident
            ));
        }
        List<PatientIdentifierType> identifierTypes =
            Collections.singletonList(DbUtil.getIdentifierTypeMsf());
        List<Patient> existing = patientService.getPatients(
            null, ident, identifierTypes, true /* exact identifier match */);
        if (!existing.isEmpty()) {
            String name = getFullName(existing.get(0));
            throw new InvalidObjectDataException(String.format(
                "Another patient (%s) already has the ID \"%s\"",
                name.isEmpty() ? "with no name" : "named " + name, ident
            ));
        }
    }
}
