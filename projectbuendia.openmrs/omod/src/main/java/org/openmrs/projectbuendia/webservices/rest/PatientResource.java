package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.PersonName;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.User;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
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
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource for xform templates (i.e. forms without data).
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patient",
    supportedClass = Patient.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class PatientResource implements Listable, Searchable, Retrievable, Creatable, Updatable {
    // Fake values
    private static final User CREATOR = new User(1);
    private static final String FACILITY_NAME = "Kailahun";  // TODO(kpy): Use a real facility name.

    // JSON property names
    private static final String ID = "id";
    private static final String UUID = "uuid";
    private static final String GENDER = "gender";
    private static final String BIRTHDATE = "birthdate";
    private static final String GIVEN_NAME = "given_name";
    private static final String FAMILY_NAME = "family_name";
    private static final String ASSIGNED_LOCATION = "assigned_location";
    private static final String PARENT_UUID = "parent_uuid";
    private static final String STATUS = "status";
    private static final String ADMISSION_TIMESTAMP = "admission_timestamp";

    @Deprecated
    private static final String AGE = "age";
    @Deprecated
    private static final String AGE_TYPE = "type";
    @Deprecated
    private static final String YEARS = "years";
    @Deprecated
    private static final String MONTHS = "months";
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

    public PatientResource() {
        patientService = Context.getPatientService();
    }

    @Override
    public SimpleObject getAll(RequestContext requestContext) throws ResponseException {
        List<Patient> patients = patientService.getAllPatients();
        return getSimpleObjectWithResults(patients);
    }

    private String validateGender(String value) {
        if (value.equals("F") || value.equals("M")) return value;
        throw new InvalidObjectDataException(
                "Gender should be specified as \"F\" or \"M\"");
    }

    /** Parses a date in YYYY-MM-DD format. */
    private static Date parseDate(String text, String fieldName) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateFormat.parse(text);
        } catch (ParseException e) {
            throw new InvalidObjectDataException(String.format(
                    "The %s field should be in YYYY-MM-DD format", fieldName));
        }
    }

    @Override
    public Object create(SimpleObject json, RequestContext requestContext) throws ResponseException {
        // We really want this to use XForms, but lets have a simple default implementation for early testing

        if (!json.containsKey(ID)) {
            throw new InvalidObjectDataException("Patient ID is required but not specified");
        }
        String id = (String) json.get(ID);
        List<PatientIdentifierType> identifierTypes =
                Arrays.asList(DbUtil.getMsfIdentifierType());
        List<Patient> existing = patientService.getPatients(
            null, id, identifierTypes, true /* exact identifier match */);
        if (!existing.isEmpty()) {
            Patient patient = existing.get(0);
            String given = patient.getGivenName();
            given = (given == null) ? "" : given;
            String family = patient.getFamilyName();
            family = (family == null) ? "" : family;
            String name = (given + " " + family).trim();
            throw new InvalidObjectDataException(String.format(
                "Another patient (%s) already has the ID \"%s\"",
                name.isEmpty() ? "with no name" : "named " + name, id));
        }

        Patient patient = jsonToPatient(json);
        patientService.savePatient(patient);

        // Status
        if (json.containsKey(STATUS)) {
            ProgramWorkflowState workflowState =
                    DbUtil.getStateByKey((String) json.get(STATUS));
            if (workflowState != null) {
                statusChange(patient, workflowState);
                patientService.savePatient(patient);
            }
        }

        return patientToJson(patient);
    }

    protected boolean isPatientIdInUse(String id) {
        List<PatientIdentifierType> identifierTypes =
            Arrays.asList(DbUtil.getMsfIdentifierType());
        List<Patient> existing = Context.getPatientService().getPatients(
            null, id, identifierTypes, true /* exact identifier match */);
        return !existing.isEmpty();
    }

    protected static Patient jsonToPatient(SimpleObject json) {
        Patient patient = new Patient();
        // TODO(nfortescue): do this properly from authentication
        patient.setCreator(CREATOR);
        patient.setDateCreated(new Date());

        if (json.containsKey(GENDER)) {
            patient.setGender((String) json.get(GENDER));
        }
        if (json.containsKey(BIRTHDATE)) {
            patient.setBirthdate(
                parseDate((String) json.get(BIRTHDATE), BIRTHDATE));
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
    public SimpleObject search(RequestContext requestContext) throws ResponseException {
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
    public Object retrieve(String uuid, RequestContext requestContext) throws ResponseException {
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
    @Override
    public Object update(String uuid, SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
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
                    patient.setBirthdate(parseDate((String) entry.getValue(), BIRTHDATE));
                    changedPatient = true;
                    break;
                case GENDER:
                    patient.setGender(validateGender((String) entry.getValue()));
                    changedPatient = true;
                    break;
                case ADMISSION_TIMESTAMP:
                    // This is really evil and maybe shouldn't even be done. Instead we should have an admission event.
                    // TODO(nfortescue): switch to an admission event
                    Integer seconds = (Integer) entry.getValue();
                    if (seconds != null) {
                        patient.setDateCreated(new Date(seconds * 1000L));
                        changedPatient = true;
                    }
                    break;

                // ==== JSON keys that change data OTHER than patient attributes.

                case STATUS:
                    ProgramWorkflowState workflowState =
                        DbUtil.getStateByKey((String) entry.getValue());
                    if (workflowState != null) {
                        statusChange(patient, workflowState);
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

    private void statusChange(Patient patient, ProgramWorkflowState newWorkflowState) {
        // OpenMRS has validators (see PatientProgramValidator) that check among other things that you can't
        // have a patient twice on the same state on the same day.

        // In general, it would be good enough to make all changes within 10 minutes count as edits (rather than
        // changes). However, for demos, you will probably change the same status many times on the same day.
        // So we will just go through and delete all changes with the same start date and status as the change
        // we are trying to add. I will leave as an open question the merge duplicates question.
        // TODO(nfortescue): decide on whether to merge changes similar in time.

        ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
        PatientProgram patientProgram = DbUtil.getEbolaStatusPatientProgram(patient);
        // If this method of set manipulation seems strange it is because hibernate is fussy about how you modify
        // collections in hibernate created objects.
        Set<PatientState> allStates = patientProgram.getStates();
        HashSet<PatientState> toRemove = new HashSet<>();
        Date ourDate = new Date();
        // Compare day, month, year. This is dangerous without joda time, and when you think about timezones.
        // TODO(nfortescue): make this safe.
        Date ourJustDay = new Date(ourDate.getYear(), ourDate.getMonth(), ourDate.getDate());

        for (PatientState oldState : allStates) {
            if (newWorkflowState.equals(oldState.getState()) && ourJustDay.equals(oldState.getStartDate())) {
                toRemove.add(oldState);
                // we could break; here, but for extra safety make sure we check them all.
            }
        }

        if (!toRemove.isEmpty()) {
            for (PatientState bad : toRemove) {
                allStates.remove(bad);
            }
        }
        // If we don't save here we get optimistic locking failures from hibernate.
        workflowService.savePatientProgram(patientProgram);
        // Important to reuse date here over midnight.
        patientProgram.transitionToState(newWorkflowState, ourDate);
        workflowService.savePatientProgram(patientProgram);
    }

    /**
     * Converts a date to a year with a fractional part, e.g. Jan 1, 1970
     * becomes 1970.0; Jul 1, 1970 becomes approximately 1970.5.
     */
    // TODO(kpy): Remove this after client v0.2 is no longer in use.
    private static double dateToFractionalYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        double daysInYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
        return calendar.get(Calendar.YEAR) +
            calendar.get(Calendar.DAY_OF_YEAR) / daysInYear;
    }

    /** Estimate the age of a person in years, given their birthdate. */
    // TODO(kpy): Remove this after client v0.2 is no longer in use.
    private static double birthDateToAge(Date birthDate) {
        return dateToFractionalYear(new Date()) -
            dateToFractionalYear(birthDate);
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

                // For backward compatibility.  Later clients ignore the "age"
                // key and calculate the displayed age from the birthdate.
                // TODO(kpy): Remove this after client v0.2 is no longer in use.
                SimpleObject ageObject = new SimpleObject();
                double age = birthDateToAge(patient.getBirthdate());
                if (age < 1.0) {
                    ageObject.add(MONTHS, (int) Math.floor(age * 12));
                    ageObject.add(AGE_TYPE, MONTHS);
                } else {
                    ageObject.add(YEARS, (int) Math.floor(age));
                    ageObject.add(AGE_TYPE, YEARS);
                }
                jsonForm.add(AGE, ageObject);
            }
            jsonForm.add(GIVEN_NAME, patient.getGivenName());
            jsonForm.add(FAMILY_NAME, patient.getFamilyName());

            // TODO(nfortescue): refactor so we have a single assigned location with a uuid,
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
                // TODO(nfortescue): remove this code when the client does it's own tree traversal.
                ArrayList<Location> branch = new ArrayList<>();
                while(!location.getUuid().equals(LocationResource.EMC_UUID)) {
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

            // TODO(kpy): Store the admission date/time in a patient attribute.
            // Assuming that admission time is equal to database creation time
            // prevents retroactive entry of existing patients or editing of
            // incorrect admission times.
            // NOTE(kpy): Client expects an integer value, not double.
            jsonForm.add(ADMISSION_TIMESTAMP,
                patient.getDateCreated().getTime() / 1000);
        }
        return jsonForm;
    }
}
