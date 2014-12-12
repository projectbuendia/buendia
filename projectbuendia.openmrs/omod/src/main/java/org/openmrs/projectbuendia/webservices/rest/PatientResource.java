package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.*;
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

import java.util.*;

/**
 * Resource for xform templates (i.e. forms without data).
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patient", supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
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
    private static final String STATUS = "status";
    private static final String ADMISSION_TIMESTAMP_SECS = "admission_timestamp";

    // OpenMRS object names
    public static final String MSF_IDENTIFIER = "MSF";
    private static final String EBOLA_STATUS_PROGRAM_NAME = "Ebola status program";
    private static final String EBOLA_STATUS_WORKFLOW_NAME = "Ebola status workflow";

    // OpenMRS object UUIDs
    private static final String EBOLA_STATUS_PROGRAM_UUID = "849c86fa-6f3d-11e4-b2f4-040ccecfdba4";
    private static final String EBOLA_STATUS_PROGRAM_CONCEPT_UUID = "8c00e1b5-6f35-11e4-a3fa-040ccecfdba4";
    private static final String EBOLA_STATUS_WORKFLOW_CONCEPT_UUID = "107f9c7a-6f3b-11e4-ba22-040ccecfdba4";
    public static final String ASSIGNED_LOCATION_PERSON_ATTRIBUTE_TYPE_UUID = "0dd66a70-5d0a-4665-90be-67e2fe01b3fc";

    // The elements of each triple are:
    // 1. The key, which is how the status is represented in JSON.
    // 2. The concept name, which is a short phrase to avoid collision with other concepts.
    // 3. The UUID of the ProgramWorkflowState that represents the status.
    private static final String[][] EBOLA_STATUS_KEYS_NAMES_AND_UUIDS = {
            {"SUSPECTED_CASE", "suspected ebola case", "041a7b80-6f13-11e4-b6d3-040ccecfdba4"},
            {"PROBABLE_CASE", "probable ebola case", "0ae8fd9c-6f13-11e4-9ad7-040ccecfdba4"},
            {"CONFIRMED_CASE", "confirmed ebola case", "11a8a9c0-6f13-11e4-8c91-040ccecfdba4"},
            {"NON_CASE", "not an ebola case", "b517037a-6f13-11e4-b5f2-040ccecfdba4"},
            {"CONVALESCENT", "convalescing at ebola facility", "c1349bd7-6f13-11e4-b315-040ccecfdba4"},
            {"READY_FOR_DISCHARGE", "ready for discharge from ebola facility", "e45ef19e-6f13-11e4-b630-040ccecfdba4"},
            {"DISCHARGED", "discharged from ebola facility", "e4a20c4a-6f13-11e4-b315-040ccecfdba4"},
            {"SUSPECTED_DEATH", "suspected death at ebola facility", "e4c09b7d-6f13-11e4-b315-040ccecfdba4"},
            {"CONFIRMED_DEATH", "confirmed death at ebola facility", "e4da31e1-6f13-11e4-b315-040ccecfdba4"},
    };
    public static final String CREATED_TIMESTAMP_MILLIS = "created_timestamp_utc";
    private static Map<String, String> EBOLA_STATUS_KEYS_BY_UUID = new HashMap<>();
    private static Map<String, String> EBOLA_STATUS_UUIDS_BY_KEY = new HashMap<>();

    static {
        for (String[] keyNameUuid : EBOLA_STATUS_KEYS_NAMES_AND_UUIDS) {
            EBOLA_STATUS_KEYS_BY_UUID.put(keyNameUuid[2], keyNameUuid[0]);
            EBOLA_STATUS_UUIDS_BY_KEY.put(keyNameUuid[0], keyNameUuid[2]);
        }
    }

    private static Log log = LogFactory.getLog(PatientResource.class);

    private final PatientService patientService;

    private final PersonAttributeType assignedLocationAttrType;
    private final PatientIdentifierType msfPatientIdentifierType;
    private final List<PatientIdentifierType> identifierTypes;

    public PatientResource() {
        patientService = Context.getPatientService();
        assignedLocationAttrType = getPersonAttributeType(
                ASSIGNED_LOCATION_PERSON_ATTRIBUTE_TYPE_UUID, "assigned_location");
        msfPatientIdentifierType = getMsfIdentifierType();
        identifierTypes = new ArrayList<>();
        identifierTypes.add(msfPatientIdentifierType);
    }

    @Override
    public SimpleObject getAll(RequestContext requestContext) throws ResponseException {
        List<Patient> patients = patientService.getAllPatients();
        return getSimpleObjectWithResults(patients);
    }

    /** Parses a date in YYYY-MM-DD format. */
    private static Date parseDate(String text, String fieldName) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateFormat.parse(text);
        } catch (ParseException e) {
            throw new InvalidObjectDataException("\"" + fieldName + "\" field is not in yyyy-mm-dd format");
        }

    /**
     * Converts a date to a year with a fractional part, e.g. Jan 1, 1970
     * becomes 1970.0; Jul 1, 1970 becomes approximately 1970.5.
     */
    // TODO(kpy): Remove this after client v0.2 is no longer in use.
    private double dateToFractionalYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        double daysInYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
        return calendar.get(Calendar.YEAR) +
                calendar.get(Calendar.DAY_OF_YEAR) / daysInYear;
    }

    /** Estimate the age of a person in years, given their birthdate. */
    // TODO(kpy): Remove this after client v0.2 is no longer in use.
    private double birthDateToAge(Date birthDate) {
        return dateToFractionalYear(new Date()) -
                dateToFractionalYear(birthDate);
    }

    @Override
    public Object create(SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
        // We really want this to use XForms, but lets have a simple default implementation for early testing

        if (!simpleObject.containsKey(ID)) {
            throw new InvalidObjectDataException("JSON object lacks required \"id\" field");
        }
        String id = (String) simpleObject.get(ID);
        List<Patient> existing =
                patientService.getPatients(null, id, identifierTypes, true /* exact identifier match */);
        if (!existing.isEmpty()) {
            throw new InvalidObjectDataException("Patient with this ID already exists: " + id);
        }

        Patient patient = new Patient();
        // TODO(nfortescue): do this properly from authentication
        patient.setCreator(CREATOR);
        patient.setDateCreated(new Date());

        if (simpleObject.containsKey(GENDER)) {
            patient.setGender((String) simpleObject.get(GENDER));
        }
        if (simpleObject.containsKey(BIRTHDATE)) {
            patient.setBirthdate(parseDate((String) json.get(BIRTHDATE), BIRTHDATE));
        }

        PersonName pn = new PersonName();
        if (simpleObject.containsKey(GIVEN_NAME)) {
            pn.setGivenName((String) simpleObject.get(GIVEN_NAME));
        }
        if (simpleObject.containsKey(FAMILY_NAME)) {
            pn.setFamilyName((String) simpleObject.get(FAMILY_NAME));
        }

        pn.setCreator(patient.getCreator());
        pn.setDateCreated(patient.getDateCreated());
        patient.addName(pn);

        // Identifier with fake location
        PatientIdentifier identifier = new PatientIdentifier();
        identifier.setCreator(patient.getCreator());
        identifier.setDateCreated(patient.getDateCreated());
        identifier.setIdentifier(id);
        identifier.setLocation(getLocationByName(FACILITY_NAME, null));
        identifier.setIdentifierType(msfPatientIdentifierType);
        identifier.setPreferred(true);
        patient.addIdentifier(identifier);

        patientService.savePatient(patient);

        // Status
        if (simpleObject.containsKey(STATUS)) {
            ProgramWorkflowState workflowState = getStateByKey((String) simpleObject.get(STATUS));
            if (workflowState != null) {
                statusChange(patient, workflowState);
                patientService.savePatient(patient);
            }
        }

        return patientToJson(patient);
    }

    private Date calculateNewBirthdate(double ageValue, String ageType) {
        Date newBirthdate;
        if ("months".equals(ageType)) {
            long millis = (long) Math.floor(
                    new Date().getTime() - (ageValue + 0.5) * 365.24 / 12 * 24 * 3600 * 1000);
            newBirthdate = new Date(millis);
        } else {  // default to years
            newBirthdate = fractionalYearToDate(
                    dateToFractionalYear(new Date()) - (ageValue + 0.5));
        }
        return newBirthdate;
    }

    private PatientIdentifierType getMsfIdentifierType() {
        PatientIdentifierType identifierType = patientService.getPatientIdentifierTypeByName(MSF_IDENTIFIER);
        if (identifierType == null) {
            identifierType = new PatientIdentifierType();
            identifierType.setName(MSF_IDENTIFIER);
            identifierType.setDescription("MSF patient identifier");
            identifierType.setFormatDescription("[facility code].[patient number]");
            patientService.savePatientIdentifierType(identifierType);
        }
        return identifierType;
    }

    public static Concept getConcept(String name, String uuid, String typeUuid) {
        ConceptService conceptService = Context.getConceptService();
        Concept concept = conceptService.getConceptByUuid(uuid);
        if (concept == null) {
            concept = new Concept();
            concept.setUuid(uuid);
            concept.setShortName(new ConceptName(name, new Locale("en")));
            concept.setDatatype(conceptService.getConceptDatatypeByUuid(typeUuid));
            concept.setConceptClass(conceptService.getConceptClassByUuid(ConceptClass.MISC_UUID));
            conceptService.saveConcept(concept);
        }
        return concept;
    }

    /** Get the key (e.g. "confirmed") for an ebola status by workflow state. */
    private String getKeyByState(ProgramWorkflowState state) {
        return EBOLA_STATUS_KEYS_BY_UUID.get(state.getConcept().getUuid());
    }

    /** Get the workflow state for an ebola status by key (e.g. "suspected"). */
    private ProgramWorkflowState getStateByKey(String key) {
        ProgramWorkflow workflow = getEbolaStatusProgramWorkflow();
        ConceptService conceptService = Context.getConceptService();
        String uuid = EBOLA_STATUS_UUIDS_BY_KEY.get(key);
        return uuid == null ? null : workflow.getState(conceptService.getConceptByUuid(uuid));
    }

    private ProgramWorkflow getEbolaStatusProgramWorkflow() {
        ProgramWorkflow workflow = null;
        for (ProgramWorkflow w : getEbolaStatusProgram().getWorkflows()) {
            workflow = w;
        }
        return workflow;
    }

    /**
     * Get the "Ebola status" Program, creating the Program if it doesn't exist yet
     * (including its ProgramWorkflow, the workflow's ProgramWorkflowStates, and the
     * Concepts corresponding to those states).
     */
    public static Program getEbolaStatusProgram() {
        ProgramWorkflowService workflowService = Context.getProgramWorkflowService();

        Program program = workflowService.getProgramByUuid(EBOLA_STATUS_PROGRAM_UUID);
        if (program == null) {
            program = new Program();
            program.setName(EBOLA_STATUS_PROGRAM_NAME);
            program.setUuid(EBOLA_STATUS_PROGRAM_UUID);
            program.setDescription(EBOLA_STATUS_PROGRAM_NAME);
            program.setConcept(getConcept(EBOLA_STATUS_PROGRAM_NAME,
                    EBOLA_STATUS_PROGRAM_CONCEPT_UUID, ConceptDatatype.N_A_UUID));
            workflowService.saveProgram(program);
        }
        Set<ProgramWorkflow> workflows = program.getWorkflows();
        if (workflows.isEmpty()) {
            ProgramWorkflow workflow = new ProgramWorkflow();
            workflow.setName(EBOLA_STATUS_WORKFLOW_NAME);
            workflow.setDescription(EBOLA_STATUS_WORKFLOW_NAME);
            workflow.setConcept(getConcept(EBOLA_STATUS_WORKFLOW_NAME,
                    EBOLA_STATUS_WORKFLOW_CONCEPT_UUID, ConceptDatatype.N_A_UUID));
            workflow.setProgram(program);
            for (String[] keyNameUuid : EBOLA_STATUS_KEYS_NAMES_AND_UUIDS) {
                ProgramWorkflowState state = new ProgramWorkflowState();
                state.setConcept(getConcept(keyNameUuid[1], keyNameUuid[2],
                        ConceptDatatype.CODED_UUID));
                state.setName(keyNameUuid[1]);
                state.setInitial(false);
                state.setTerminal(false);
                workflow.addState(state);
            }
            program.addWorkflow(workflow);
            workflowService.saveProgram(program);
        }
        return program;
    }

    /**
     * Get the PatientProgram associating this Patient with the "Ebola status" Program,
     * creating the PatientProgram if it doesn't exist yet.
     */
    public static PatientProgram getEbolaStatusPatientProgram(Patient patient) {
        ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
        Program program = getEbolaStatusProgram();
        List<PatientProgram> patientPrograms =
                workflowService.getPatientPrograms(patient, program, null, null, null, null, false);
        PatientProgram patientProgram;
        if (patientPrograms.isEmpty()) {
            patientProgram = new PatientProgram();
            patientProgram.setPatient(patient);
            patientProgram.setProgram(program);
            patientProgram.setDateEnrolled(new Date());
            workflowService.savePatientProgram(patientProgram);
        } else {
            patientProgram = patientPrograms.get(0);
        }
        return patientProgram;
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
        List<Patient> filteredPatients = filterPatients(query, searchUuid, patientService.getAllPatients());

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
        PatientIdentifierType msfIdentifierType = patientService.getPatientIdentifierTypeByName(MSF_IDENTIFIER);
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

    private static PersonAttributeType getPersonAttributeType(String uuid, String name) {
        PersonService personService = Context.getPersonService();
        PersonAttributeType personAttributeType = personService.getPersonAttributeTypeByUuid(uuid);
        if (personAttributeType == null) {
            personAttributeType = new PersonAttributeType();
            personAttributeType.setUuid(uuid);
            personAttributeType.setName(name);
            personAttributeType.setDescription(name);
            personAttributeType.setForeignKey(0);
            personAttributeType.setFormat("org.openmrs.Location");
            personService.savePersonAttributeType(personAttributeType);
        }
        return personAttributeType;
    }

    private static Location getLocationByName(String locationName, Location parent) {
        LocationService locationService = Context.getLocationService();
        Location location = locationService.getLocation(locationName);
        if (location == null) {
            location = new Location();
            location.setName(locationName);
            location.setDescription(locationName);
            if (parent != null) {
                location.setParentLocation(parent);
            }
            locationService.saveLocation(location);
        }
        return location;
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

        boolean changedPatient = false;
        for (Map.Entry<String, Object> entry : simpleObject.entrySet()) {
            Date newBirthday;
            PersonName oldName;
            PersonName newName;
            switch (entry.getKey()) {
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
                    String locationUuid = (String) assignedLocation.get(UUID);
                    if (locationUuid != null) {
                        Location location = Context.getLocationService().getLocationByUuid(locationUuid);
                        if (location != null) {
                            setPersonAttributeValue(patient, assignedLocationAttrType,
                                    Integer.toString(location.getId()));
                        }
                    }
                    break;
                case BIRTHDATE:
                    patient.setBirthdate(parseDate((String) entry.getValue(), BIRTHDATE));
                    changedPatient = true;
                    break;
                case GENDER:
                    String gender = (String) entry.getValue();
                    if ("M".equalsIgnoreCase(gender) || "F".equalsIgnoreCase(gender)) {
                        patient.setGender(gender);
                        changedPatient = true;
                    }
                    break;
                case STATUS:
                    ProgramWorkflowState workflowState = getStateByKey((String) entry.getValue());
                    if (workflowState != null) {
                        statusChange(patient, workflowState);
                    }
                    break;
                case ADMISSION_TIMESTAMP_SECS:
                    // This is really evil and maybe shouldn't even be done. Instead we should have an admission event.
                    // TODO(nfortescue): switch to an admission event
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

        if (changedPatient) {
            patientService.savePatient(patient);
        }
        return patientToJson(patient);
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
        PatientProgram patientProgram = getEbolaStatusPatientProgram(patient);
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

    private static String getPersonAttributeValue(Person person, PersonAttributeType attrType) {
        PersonAttribute attribute = person.getAttribute(attrType);
        return attribute != null ? attribute.getValue() : null;
    }

    private static void setPersonAttributeValue(Patient patient, PersonAttributeType attrType, String value) {
        PersonService personService = Context.getPersonService();
        PersonAttribute attribute = patient.getAttribute(attrType);
        if (attribute == null) {
            attribute = new PersonAttribute();
            attribute.setAttributeType(attrType);
            attribute.setValue(value);
            patient.addAttribute(attribute);
        } else {
            attribute.setValue(value);
        }
        personService.savePerson(patient);
    }

    private SimpleObject patientToJson(Patient patient) {
        SimpleObject jsonForm = new SimpleObject();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (patient != null) {
            jsonForm.add(UUID, patient.getUuid());
            PatientIdentifier patientIdentifier =
                    patient.getPatientIdentifier(msfPatientIdentifierType);
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
            String assignedLocation = getPersonAttributeValue(patient, assignedLocationAttrType);
            if (assignedLocation != null) {
                LocationService locationService = Context.getLocationService();
                Location location = locationService.getLocation(Integer.valueOf(assignedLocation));

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

            jsonForm.add(CREATED_TIMESTAMP_MILLIS, patient.getDateCreated().getTime());
            jsonForm.add(ADMISSION_TIMESTAMP_SECS, patient.getDateCreated().getTime() / 1000);
        }
        return jsonForm;
    }
}
