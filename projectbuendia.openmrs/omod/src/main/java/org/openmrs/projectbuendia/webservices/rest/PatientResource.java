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
import org.openmrs.module.webservices.rest.web.resource.api.*;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.*;

/**
 * Resource for xform templates (i.e. forms without data).
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patient", supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*")
public class PatientResource implements Listable, Searchable, Retrievable, Creatable, Updatable {
    // Fake values
    private static final User CREATOR = new User(1);
    private static final String FACILITY_NAME = "Kailahun";  // TODO(kpy): Use a real facility name.

    // JSON property names
    private static final String ID = "id";
    private static final String UUID = "uuid";
    private static final String GENDER = "gender";
    private static final String AGE = "age";
    private static final String AGE_UNIT = "age_unit";  // "years" or "months"
    private static final String MONTHS_VALUE = "months";
    private static final String AGE_TYPE = "type";
    private static final String YEARS_VALUE = "years";
    private static final String MONTHS_TYPE = "months";
    private static final String YEARS_TYPE = "years";
    private static final String GIVEN_NAME = "given_name";
    private static final String FAMILY_NAME = "family_name";
    private static final String ASSIGNED_LOCATION = "assigned_location";
    private static final String ZONE = "zone";
    private static final String TENT = "tent";
    private static final String BED = "bed";
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
    private static final String ZONE_LOCATION_TAG_UUID = "1c22989d-3b87-47d3-9459-b54aafbd1169";
    private static final String TENT_LOCATION_TAG_UUID = "4c92578f-cde9-4b99-b641-f3b9e0cc268d";
    private static final String BED_LOCATION_TAG_UUID = "f2cf9e4e-a197-4c44-9290-0d3dd963838e";
    private static final String ASSIGNED_ZONE_PERSON_ATTRIBUTE_TYPE_UUID = "1c22989d-3b87-47d3-9459-b54aafbd1169";
    private static final String ASSIGNED_TENT_PERSON_ATTRIBUTE_TYPE_UUID = "4c92578f-cde9-4b99-b641-f3b9e0cc268d";
    private static final String ASSIGNED_BED_PERSON_ATTRIBUTE_TYPE_UUID = "f2cf9e4e-a197-4c44-9290-0d3dd963838e";

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
    private final PersonAttributeType assignedZoneAttrType;
    private final PersonAttributeType assignedTentAttrType;
    private final PersonAttributeType assignedBedAttrType;

    public PatientResource() {
        patientService = Context.getPatientService();
        assignedZoneAttrType = getPersonAttributeType(
                ASSIGNED_ZONE_PERSON_ATTRIBUTE_TYPE_UUID, "assigned_zone");
        assignedTentAttrType = getPersonAttributeType(
                ASSIGNED_TENT_PERSON_ATTRIBUTE_TYPE_UUID, "assigned_tent");
        assignedBedAttrType = getPersonAttributeType(
                ASSIGNED_BED_PERSON_ATTRIBUTE_TYPE_UUID, "assigned_bed");
    }

    @Override
    public SimpleObject getAll(RequestContext requestContext) throws ResponseException {
        List<Patient> patients = patientService.getAllPatients();
        return getSimpleObjectWithResults(patients);
    }

    /**
     * Converts a date to a year with a fractional part, e.g. Jan 1, 1970
     * becomes 1970.0; Jul 1, 1970 becomes approximately 1970.5.
     */
    private double dateToFractionalYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        double daysInYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
        return calendar.get(Calendar.YEAR) +
                calendar.get(Calendar.DAY_OF_YEAR) / daysInYear;
    }

    private Date fractionalYearToDate(double year) {
        int yearInt = (int) Math.floor(year);
        double yearFrac = year - yearInt;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, yearInt);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long millis = calendar.getTimeInMillis();
        int daysInYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
        millis += yearFrac * daysInYear * 24 * 3600 * 1000;
        return new Date(millis);
    }

    /** Estimate the age of a person in years, given their birthdate. */
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
        PatientIdentifierType identifierType = getMsfIdentifierType();
        ArrayList<PatientIdentifierType> identifierTypes = new ArrayList<>();
        identifierTypes.add(identifierType);
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
        if (simpleObject.containsKey(AGE)) {
            patient.setBirthdate(calculateNewBirthdate(((Number) simpleObject.get(AGE)).doubleValue(),
                    (String)simpleObject.get(AGE_UNIT)));
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
        identifier.setLocation(getLocationByName(FACILITY_NAME, null, null));
        identifier.setIdentifierType(identifierType);
        identifier.setPreferred(true);
        patient.addIdentifier(identifier);

        // Assigned zone, tent, and bed (convert integer 2 to string "2")
        setPatientAssignedLocation(patient, FACILITY_NAME,
                "" + simpleObject.get("assigned_zone"),
                "" + simpleObject.get("assigned_tent"),
                "" + simpleObject.get("assigned_bed"));
        patientService.savePatient(patient);

        // Status
        if (simpleObject.containsKey(STATUS)) {
            ProgramWorkflowState workflowState = getStateByKey((String) simpleObject.get(STATUS));
            if (workflowState != null) {
                ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
                PatientProgram patientProgram = getEbolaStatusPatientProgram(patient);
                patientProgram.transitionToState(workflowState, new Date());
                workflowService.savePatientProgram(patientProgram);
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

    public static PatientIdentifierType getMsfIdentifierType() {
        PatientService patientService = Context.getPatientService();
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

    private static LocationTag getLocationTag(String uuid, String name) {
        LocationService locationService = Context.getLocationService();
        LocationTag tag = locationService.getLocationTagByUuid(uuid);
        if (tag == null) {
            tag = new LocationTag();
            tag.setUuid(uuid);
            tag.setName(name);
            tag.setDescription(name);
            locationService.saveLocationTag(tag);
        }
        return tag;
    }

    private static PersonAttributeType getPersonAttributeType(String uuid, String name) {
        PersonService personService = Context.getPersonService();
        PersonAttributeType personAttributeType = personService.getPersonAttributeTypeByUuid(uuid);
        if (personAttributeType == null) {
            personAttributeType = new PersonAttributeType();
            personAttributeType.setUuid(uuid);
            personAttributeType.setName(name);
            personAttributeType.setDescription(name);
            personService.savePersonAttributeType(personAttributeType);
        }
        return personAttributeType;
    }

    private static Location getLocationByName(String locationName, LocationTag tag, Location parent) {
        LocationService locationService = Context.getLocationService();
        Location location = locationService.getLocation(locationName);
        if (location == null) {
            location = new Location();
            location.setName(locationName);
            location.setDescription(locationName);
            if (tag != null) {
                Set<LocationTag> tags = new HashSet<>();
                tags.add(tag);
                location.setTags(tags);
            }
            if (parent != null) {
                location.setParentLocation(parent);
            }
            locationService.saveLocation(location);
        }
        return location;
    }

    private static String getLocationLeafName(String locationId) {
        LocationService locationService = Context.getLocationService();
        Location location = locationService.getLocation(Integer.valueOf(locationId));
        if (location != null) {
            // The location name is a path consisting of comma-separated components,
            // with each component prefixed by the tag name for that level.
            String locationName = location.getName();
            String[] components = locationName.split(",");
            String leafName = components[components.length - 1].trim();
            for (LocationTag tag : location.getTags()) {
                String tagUuid = tag.getUuid();
                String tagName = tag.getName();
                if (ZONE_LOCATION_TAG_UUID.equals(tagUuid) ||
                        TENT_LOCATION_TAG_UUID.equals(tagUuid) ||
                        BED_LOCATION_TAG_UUID.equals(tagUuid)) {
                    if (leafName.startsWith(tagName)) {
                        leafName = leafName.substring(tagName.length()).trim();
                    }
                }
            }
            return leafName;
        }
        return null;
    }

    private static Location getLocationByPath(String facilityName, String zoneName, String tentName, String bedName) {
        LocationTag zoneTag = getLocationTag(ZONE_LOCATION_TAG_UUID, "Zone");
        LocationTag tentTag = getLocationTag(TENT_LOCATION_TAG_UUID, "Tent");
        LocationTag bedTag = getLocationTag(BED_LOCATION_TAG_UUID, "Bed");

        // To ensure that each Location has a unique name, construct a fully qualified
        // location name consisting of comma-separated components, with each component
        // prefixed by the tag name for that level, e.g. "Facility Kailahun, Zone 2, Tent 1"
        // as distinct from "Tent 1" in any other zone or facility.
        String facilityLocationName;
        String zoneLocationName;
        String tentLocationName;
        String bedLocationName;
        Location result = null;
        if (facilityName != null) {
            facilityLocationName = "Facility " + facilityName;
            Location facility = result = getLocationByName(facilityLocationName, null, null);
            if (zoneName != null) {
                zoneLocationName = facilityLocationName + ", Zone " + zoneName;
                Location zone = result = getLocationByName(zoneLocationName, zoneTag, facility);
                if (tentName != null) {
                    tentLocationName = zoneLocationName + ", Tent " + tentName;
                    Location tent = result = getLocationByName(tentLocationName, tentTag, zone);
                    if (bedName != null) {
                        bedLocationName = tentLocationName + ", Bed " + bedName;
                        Location bed = result = getLocationByName(bedLocationName, bedTag, tent);
                    }
                }
            }
        }
        return result;
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

        String facilityName = FACILITY_NAME;
        String zoneName = getPersonAttributeValue(patient, assignedZoneAttrType);
        String tentName = getPersonAttributeValue(patient, assignedTentAttrType);
        String bedName = getPersonAttributeValue(patient, assignedBedAttrType);
        boolean changedPatient = false;
        for (Map.Entry<String, Object> entry : simpleObject.entrySet()) {
            Date newBirthday = null;
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
                    zoneName = (String) assignedLocation.get(ZONE);
                    tentName = (String) assignedLocation.get(TENT);
                    bedName = (String) assignedLocation.get(BED);
                    break;
                case AGE:
                    Map age = (Map) entry.getValue();
                    switch((String) age.get(AGE_TYPE)) {
                        case MONTHS_TYPE:
                            newBirthday = calculateNewBirthdate((Integer) age.get(MONTHS_VALUE), MONTHS_TYPE);
                            break;
                        case YEARS_TYPE:
                            newBirthday = calculateNewBirthdate((Integer) age.get(YEARS_VALUE), YEARS_TYPE);
                            break;
                        default:
                            continue;
                    }
                    patient.setBirthdate(newBirthday);
                    patient.setBirthdateEstimated(true);
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
                        ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
                        PatientProgram patientProgram = getEbolaStatusPatientProgram(patient);
                        patientProgram.transitionToState(workflowState, new Date());
                        workflowService.savePatientProgram(patientProgram);
                        changedPatient = true;
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
        setPatientAssignedLocation(patient, facilityName, zoneName, tentName, bedName);
        return patientToJson(patient);
    }

    private void setPatientAssignedLocation(
            Patient patient, String facilityName, String zoneName, String tentName, String bedName) {
        if (zoneName != null) {
            setPersonAttributeValue(patient, assignedZoneAttrType,
                    "" + getLocationByPath(facilityName, zoneName, null, null).getLocationId());
            if (tentName != null) {
                setPersonAttributeValue(patient, assignedTentAttrType,
                        "" + getLocationByPath(facilityName, zoneName, tentName, null).getLocationId());
                if (bedName != null) {
                    setPersonAttributeValue(patient, assignedBedAttrType,
                            "" + getLocationByPath(facilityName, zoneName, tentName, bedName).getLocationId());
                }
            }
        }
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
        if (patient != null) {
            jsonForm.add(UUID, patient.getUuid());
            PatientIdentifier patientIdentifier =
                    patient.getPatientIdentifier(getMsfIdentifierType());
            if (patientIdentifier != null) {
                jsonForm.add(ID, patientIdentifier.getIdentifier());
            }

            jsonForm.add(GENDER, patient.getGender());
            double age = birthDateToAge(patient.getBirthdate());
            SimpleObject ageObject = new SimpleObject();
            if (age < 1.0) {
                ageObject.add(MONTHS_VALUE, (int) Math.floor(age * 12));
                ageObject.add(AGE_TYPE, MONTHS_TYPE);
            } else {
                ageObject.add(YEARS_VALUE, (int) Math.floor(age));
                ageObject.add(AGE_TYPE, YEARS_TYPE);
            }
            jsonForm.add(AGE, ageObject);

            jsonForm.add(GIVEN_NAME, patient.getGivenName());
            jsonForm.add(FAMILY_NAME, patient.getFamilyName());

            String assignedZoneId = getPersonAttributeValue(patient, assignedZoneAttrType);
            String assignedTentId = getPersonAttributeValue(patient, assignedTentAttrType);
            String assignedBedId = getPersonAttributeValue(patient, assignedBedAttrType);
            if (assignedZoneId != null || assignedTentId != null || assignedBedId != null) {
                SimpleObject location = new SimpleObject();
                if (assignedZoneId != null) {
                    location.add(ZONE, getLocationLeafName(assignedZoneId));
                }
                if (assignedTentId != null) {
                    location.add(TENT, getLocationLeafName(assignedTentId));
                }
                if (assignedBedId != null) {
                    location.add(BED, getLocationLeafName(assignedBedId));
                }
                jsonForm.add(ASSIGNED_LOCATION, location);
            }

            PatientProgram patientProgram = getEbolaStatusPatientProgram(patient);
            PatientState patientState = patientProgram.getCurrentState(
                    getEbolaStatusProgramWorkflow());
            if (patientState != null) {
                jsonForm.add(STATUS, getKeyByState(patientState.getState()));
            }

            jsonForm.add(CREATED_TIMESTAMP_MILLIS, patient.getDateCreated().getTime());
            jsonForm.add(ADMISSION_TIMESTAMP_SECS, patient.getDateCreated().getTime() / 1000);
        }
        return jsonForm;
    }
}
