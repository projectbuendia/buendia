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
    private static final String[][] EBOLA_STATUS_CONCEPT_NAMES_AND_UUIDS = {
            {"suspected", "041a7b80-6f13-11e4-b6d3-040ccecfdba4"},
            {"probable", "0ae8fd9c-6f13-11e4-9ad7-040ccecfdba4"},
            {"confirmed", "11a8a9c0-6f13-11e4-8c91-040ccecfdba4"},
            {"non-case", "b517037a-6f13-11e4-b5f2-040ccecfdba4"},
            {"convalescence", "c1349bd7-6f13-11e4-b315-040ccecfdba4"},
            {"can be discharged", "e45ef19e-6f13-11e4-b630-040ccecfdba4"},
            {"discharged", "e4a20c4a-6f13-11e4-b315-040ccecfdba4"},
            {"suspected dead", "e4c09b7d-6f13-11e4-b315-040ccecfdba4"},
            {"confirmed dead", "e4da31e1-6f13-11e4-b315-040ccecfdba4"},
    };

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

    private SimpleObject patientToJson(Patient patient) {
        SimpleObject jsonForm = new SimpleObject();
        if (patient != null) {
            PatientIdentifier patientIdentifier =
                    patient.getPatientIdentifier(getMsfIdentifierType());
            if (patientIdentifier != null) {
                jsonForm.add(ID, patientIdentifier.getIdentifier());
            }
            jsonForm.add(GIVEN_NAME, patient.getGivenName());
            jsonForm.add(FAMILY_NAME, patient.getFamilyName());
            PatientProgram patientProgram = getEbolaStatusPatientProgram(patient);
            PatientState patientState = patientProgram.getCurrentState(
                    getEbolaStatusProgramWorkflow());
            if (patientState != null) {
                jsonForm.add(STATUS, patientState.getState().getName());
            }
            jsonForm.add(GENDER, patient.getGender());
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
            ProgramWorkflowState workflowState =
                    getEbolaStatusWorkflowState((String) simpleObject.get(STATUS));
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

    private Concept getConcept(String name, String uuid, String typeName) {
        ConceptService conceptService = Context.getConceptService();
        Concept concept = conceptService.getConceptByUuid(uuid);
        if (concept == null) {
            concept = new Concept();
            concept.setUuid(uuid);
            concept.setShortName(new ConceptName(name, new Locale("en")));
            concept.setDatatype(conceptService.getConceptDatatypeByName(typeName));
            concept.setConceptClass(conceptService.getConceptClassByUuid(ConceptClass.MISC_UUID));
            conceptService.saveConcept(concept);
        }
        return concept;
    }

    /** Get the workflow state for the ebola status by name (e.g. "suspected"). */
    private ProgramWorkflowState getEbolaStatusWorkflowState(String name) {
        return getEbolaStatusProgramWorkflow().getStateByName(name);
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
            program.setConcept(getConcept(EBOLA_STATUS_PROGRAM_NAME, EBOLA_STATUS_PROGRAM_CONCEPT_UUID, "N/A"));
            workflowService.saveProgram(program);
        }
        Set<ProgramWorkflow> workflows = program.getWorkflows();
        if (workflows.isEmpty()) {
            ProgramWorkflow workflow = new ProgramWorkflow();
            workflow.setName(EBOLA_STATUS_WORKFLOW_NAME);
            workflow.setDescription(EBOLA_STATUS_WORKFLOW_NAME);
            workflow.setConcept(getConcept(EBOLA_STATUS_WORKFLOW_NAME, EBOLA_STATUS_WORKFLOW_CONCEPT_UUID, "N/A"));
            workflow.setProgram(program);
            for (String[] nameAndUuid : EBOLA_STATUS_CONCEPT_NAMES_AND_UUIDS) {
                ProgramWorkflowState state = new ProgramWorkflowState();
                state.setConcept(getConcept(nameAndUuid[0], nameAndUuid[1], "CODED"));
                state.setName(nameAndUuid[0]);
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
