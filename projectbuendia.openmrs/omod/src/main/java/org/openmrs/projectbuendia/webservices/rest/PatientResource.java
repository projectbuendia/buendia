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
import org.openmrs.projectbuendia.DateTimeUtils;
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
 * GET /patient returns all patients ({@link #getAll(RequestContext)})
 * GET /patient?q=[query] returns patients with a name or id that contains the query ({@link #search(RequestContext)})
 * GET /patient/[UUID] returns information on a single patient ({@link #retrieve(String, RequestContext)})
 * POST /patient creates a patient ({@link #create(SimpleObject, RequestContext)}
 * POST /patient/[UUID] updates a patient ({@link #update(String, SimpleObject, RequestContext)})
 *
 * <p>Each operation handles Patient resources, with the following syntax:
 *
 * <pre>
 * {
 *   "uuid": "e5e755d4-f646-45b6-b9bc-20410e97c87c", // unique id assigned by OpenMRS, not required for creation
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
 * Note that results may contain other fields not listed above, but these fields are deprecated and no longer supported.
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
    private static final String ADMISSION_TIMESTAMP = "admission_timestamp";

    @Deprecated
    private static final String ZONE = "zone";
    @Deprecated
    private static final String ZONE_UUID = "zone_uuid";
    @Deprecated
    private static final String TENT = "tent";
    @Deprecated
    private static final String TENT_UUID = "tent_uuid";
    @Deprecated
    private static final String BED = "bed";
    @Deprecated
    private static final String BED_UUID = "bed_uuid";

    private static Log log = LogFactory.getLog(PatientResource.class);
    private final PatientService patientService;
    private final ObservationsHandler observationsHandler = new ObservationsHandler();
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
        // We really want this to use XForms, but lets have a simple default implementation for early testing

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
        if (observationsHandler.hasObservations(json)) {
            observationsHandler.addObservations(json, patient, patient.getDateCreated(), "Initial triage",
                    "ADULTINITIAL", LocationResource.TRIAGE_UUID);
        }

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
                DateTimeUtils.parseDate((String) json.get(BIRTHDATE), BIRTHDATE));
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

    private List<Patient> filterPatients(String query, boolean searchUuid, List<Patient> allPatients) {
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
     * The SimpleObject arriving is a Gson serialization of a client Patient Bean. It has the following semantics:
     * <ul>
     *     <li>Any field set overwrites the current content
     *     <li>Any field with a key but value == null deletes the current content
     *     <li>Any field not contained leaves the current content unchanged
     *     <li>Recursive merge for location and age should not be done, instead the whole item is written
     *     <li>If the client requests a change which is illegal that is an error. Really the whole call should fail,
     *         but for now there may be partial updates
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
                    patient.setBirthdate(DateTimeUtils.parseDate((String) entry.getValue(), BIRTHDATE));
                    changedPatient = true;
                    break;
                case GENDER:
                    patient.setGender(validateGender((String) entry.getValue()));
                    changedPatient = true;
                    break;

                // ==== JSON keys that change data OTHER than patient attributes.
                case ADMISSION_TIMESTAMP:
                    // This is really evil and maybe shouldn't even be done. Instead we should have an admission event.
                    // TODO: switch to an admission event
                    Integer seconds = (Integer) entry.getValue();
                    if (seconds != null) {
                        patient.setDateCreated(new Date(seconds * 1000L));
                        changedPatient = true;
                    }
                    break;

                default:
                    log.warn("Patient has no such property or property is not updatable (ignoring) Change: " + entry);
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

                // last entry in branch is zone, second last is tent, third is bed, ignore the rest.
                SimpleObject locationJson = new SimpleObject();
                locationJson.add(UUID, location.getUuid());
                if (location.getParentLocation() != null) {
                    locationJson.add(PARENT_UUID, location.getParentLocation().getUuid());
                }

                // Walk up the tree and then count back.
                // TODO: remove this code when the client does it's own tree traversal.
                ArrayList<Location> branch = new ArrayList<>();
                while(!location.getUuid().equals(LocationResource.ROOT_UUID)) {
                    branch.add(location);
                    location = location.getParentLocation();
                    if (location == null) {
                        // If we never end up at the camp root, then it is an illegal location, ignore.
                        branch.clear();
                        break;
                    }
                }

                if (branch.size() > 0) {
                    Location zone = branch.get(branch.size()-1);
                    locationJson.add(ZONE, zone.getDisplayString());
                    locationJson.add(ZONE_UUID, zone.getUuid());
                }
                if (branch.size() > 1) {
                    Location tent = branch.get(branch.size()-2);
                    locationJson.add(TENT, tent.getDisplayString());
                    locationJson.add(TENT_UUID, tent.getUuid());
                }
                if (branch.size() > 2) {
                    Location bed = branch.get(branch.size()-2);
                    locationJson.add(BED, bed.getDisplayString());
                    locationJson.add(BED_UUID, bed.getUuid());
                }
                jsonForm.add(ASSIGNED_LOCATION, locationJson);
            }

            // TODO: This value was a stopgap before we had a better way to store
            // the admission date.  This JSON property is no longer used in the client;
            // instead, the admission date is stored as an observation and returned by
            // PatientEncountersResource.
            jsonForm.add(ADMISSION_TIMESTAMP,
                patient.getDateCreated().getTime() / 1000);
        }
        return jsonForm;
    }
}
