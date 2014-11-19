package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
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
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.*;

/**
 * Resource for xform templates (i.e. forms without data).
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patient", supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*")
public class PatientResource implements Listable, Searchable, Retrievable, Creatable {

    private static final String GENDER = "gender";
    private static final String AGE = "age";
    private static final String AGE_UNIT = "age_unit";  // "years" or "months"
    private static final String GIVEN_NAME = "given_name";
    private static final String FAMILY_NAME = "family_name";
    private static final String STATUS = "status";
    private static final User CREATOR = new User(1);
    private static final String ID = "id";
    private static final String MSF_IDENTIFIER = "MSF";
    private static final String EBOLA_STATUS_PROGRAM_NAME = "Ebola status program";
    private static final String EBOLA_STATUS_PROGRAM_UUID = "849c86fa-6f3d-11e4-b2f4-040ccecfdba4";
    private static final String EBOLA_STATUS_PROGRAM_CONCEPT_UUID = "8c00e1b5-6f35-11e4-a3fa-040ccecfdba4";
    private static final String EBOLA_STATUS_WORKFLOW_CONCEPT_UUID = "107f9c7a-6f3b-11e4-ba22-040ccecfdba4";
    private static final String EBOLA_STATUS_WORKFLOW_NAME = "Ebola status workflow";

    // The elements of each triple are:
    // 1. The key, which is how the status is represented in JSON.
    // 2. The concept name, which is a short phrase to avoid collision with other concepts.
    // 3. The UUID of the ProgramWorkflowState that represents the status.
    private static final String[][] EBOLA_STATUS_KEYS_NAMES_AND_UUIDS = {
            {"suspected", "suspected ebola case", "041a7b80-6f13-11e4-b6d3-040ccecfdba4"},
            {"probable", "probable ebola case", "0ae8fd9c-6f13-11e4-9ad7-040ccecfdba4"},
            {"confirmed", "confirmed ebola case", "11a8a9c0-6f13-11e4-8c91-040ccecfdba4"},
            {"non-case", "not an ebola case", "b517037a-6f13-11e4-b5f2-040ccecfdba4"},
            {"convalescent", "convalescing at ebola facility", "c1349bd7-6f13-11e4-b315-040ccecfdba4"},
            {"can be discharged", "ready for discharge from ebola facility", "e45ef19e-6f13-11e4-b630-040ccecfdba4"},
            {"discharged", "discharged from ebola facility", "e4a20c4a-6f13-11e4-b315-040ccecfdba4"},
            {"suspected dead", "suspected death at ebola facility", "e4c09b7d-6f13-11e4-b315-040ccecfdba4"},
            {"confirmed dead", "confirmed death at ebola facility", "e4da31e1-6f13-11e4-b315-040ccecfdba4"},
    };
    private static Map<String, String> EBOLA_STATUS_KEYS_BY_UUID = new HashMap<>();
    private static Map<String, String> EBOLA_STATUS_UUIDS_BY_KEY = new HashMap<>();
    static {
        for (String[] keyNameUuid : EBOLA_STATUS_KEYS_NAMES_AND_UUIDS) {
            EBOLA_STATUS_KEYS_BY_UUID.put(keyNameUuid[2], keyNameUuid[0]);
            EBOLA_STATUS_UUIDS_BY_KEY.put(keyNameUuid[0], keyNameUuid[2]);
        }
    }

    public static final Location LOCATION = new Location(1);
    private final PatientService patientService;

    public PatientResource() {
        patientService = Context.getPatientService();
    }

    @Override
    public SimpleObject getAll(RequestContext requestContext) throws ResponseException {
        List<Patient> patients = patientService.getAllPatients();
        return getSimpleObjectFromPatientList(patients);
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

    private SimpleObject patientToJson(Patient patient) {
        SimpleObject jsonForm = new SimpleObject();
        if (patient != null) {
            PatientIdentifier patientIdentifier =
                    patient.getPatientIdentifier(getMsfIdentifierType());
            if (patientIdentifier != null) {
                jsonForm.add(ID, patientIdentifier.getIdentifier());
            }

            jsonForm.add(GENDER, patient.getGender());
            double age = birthDateToAge(patient.getBirthdate());
            if (age < 1.0) {
                jsonForm.add(AGE, (int) Math.floor(age * 12));
                jsonForm.add(AGE_UNIT, "months");
            } else {
                jsonForm.add(AGE, (int) Math.floor(age));
                jsonForm.add(AGE_UNIT, "years");
            }

            jsonForm.add(GIVEN_NAME, patient.getGivenName());
            jsonForm.add(FAMILY_NAME, patient.getFamilyName());

            PatientProgram patientProgram = getEbolaStatusPatientProgram(patient);
            PatientState patientState = patientProgram.getCurrentState(
                    getEbolaStatusProgramWorkflow());
            if (patientState != null) {
                jsonForm.add(STATUS, getKeyByState(patientState.getState()));
            }

            jsonForm.add("created_timestamp_utc", patient.getDateCreated().getTime());
        }
        return jsonForm;
    }

    @Override
    public Object create(SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
        // We really want this to use XForms, but lets have a simple default implementation for early testing

        if (!simpleObject.containsKey(ID)) {
            throw new ConversionException("No id set in create request");
        }
        PatientIdentifierType identifierType = getMsfIdentifierType();
        ArrayList<PatientIdentifierType> identifierTypes = new ArrayList<>();
        identifierTypes.add(identifierType);
        String id = (String)simpleObject.get(ID);
        List<Patient> existing =
                patientService.getPatients(null, id, identifierTypes, true /* exact identifier match */);
        if (!existing.isEmpty()) {
            throw new ConversionException("Creating an object that already exists " + id);
        }

        Patient patient = new Patient();
        // TODO(nfortescue): do this properly from authentication
        patient.setCreator(CREATOR);
        patient.setDateCreated(new Date());

        if (simpleObject.containsKey(GENDER)) {
            patient.setGender((String)simpleObject.get(GENDER));
        }
        if (simpleObject.containsKey(AGE)) {
            double number = ((Number) simpleObject.get(AGE)).doubleValue();
            if ("months".equals(simpleObject.get(AGE_UNIT))) {
                long millis = (long) Math.floor(
                        new Date().getTime() - number * 365.24 / 12 * 24 * 3600 * 1000);
                patient.setBirthdate(new Date(millis));
            } else {  // default to years
                patient.setBirthdate(fractionalYearToDate(
                        dateToFractionalYear(new Date()) - number));
            }
        }

        PersonName pn = new PersonName();
        if (simpleObject.containsKey(GIVEN_NAME)) {
            pn.setGivenName((String)simpleObject.get(GIVEN_NAME));
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
        identifier.setLocation(LOCATION);
        identifier.setIdentifierType(identifierType);
        identifier.setPreferred(true);
        patient.addIdentifier(identifier);
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

    private Concept getConcept(String name, String uuid, String typeUuid) {
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
    private Program getEbolaStatusProgram() {
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
    private PatientProgram getEbolaStatusPatientProgram(Patient patient) {
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

        return getSimpleObjectFromPatientList(filteredPatients);
    }

    @Override
    public Object retrieve(String uuid, RequestContext requestContext) throws ResponseException {
        Patient patient = patientService.getPatientByUuid(uuid);
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
                if(StringUtils.containsIgnoreCase(name.getFullName(), query)) {
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

    private SimpleObject getSimpleObjectFromPatientList(List<Patient> patients) {
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Patient patient : patients) {
            SimpleObject jsonForm = patientToJson(patient);
            jsonResults.add(jsonForm);
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }
}