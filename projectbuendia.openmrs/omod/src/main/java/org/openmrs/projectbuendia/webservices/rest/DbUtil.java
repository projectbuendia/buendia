package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;

import java.util.*;

/** Static helper methods for handling OpenMRS database entities and UUIDs. */
public class DbUtil {
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

    private static Map<String, String> EBOLA_STATUS_KEYS_BY_UUID = new HashMap<>();
    private static Map<String, String> EBOLA_STATUS_UUIDS_BY_KEY = new HashMap<>();

    static {
        for (String[] keyNameUuid : EBOLA_STATUS_KEYS_NAMES_AND_UUIDS) {
            EBOLA_STATUS_KEYS_BY_UUID.put(keyNameUuid[2], keyNameUuid[0]);
            EBOLA_STATUS_UUIDS_BY_KEY.put(keyNameUuid[0], keyNameUuid[2]);
        }
    }

    public static PatientIdentifierType getMsfIdentifierType() {
        PatientService service = Context.getPatientService();
        PatientIdentifierType identifierType =
                service.getPatientIdentifierTypeByName(MSF_IDENTIFIER);
        if (identifierType == null) {
            identifierType = new PatientIdentifierType();
            identifierType.setName(MSF_IDENTIFIER);
            identifierType.setDescription("MSF patient identifier");
            identifierType.setFormatDescription("[facility code].[patient number]");
            service.savePatientIdentifierType(identifierType);
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
    public static String getKeyByState(ProgramWorkflowState state) {
        return EBOLA_STATUS_KEYS_BY_UUID.get(state.getConcept().getUuid());
    }

    /** Get the workflow state for an ebola status by key (e.g. "suspected"). */
    public static ProgramWorkflowState getStateByKey(String key) {
        ProgramWorkflow workflow = getEbolaStatusProgramWorkflow();
        ConceptService conceptService = Context.getConceptService();
        String uuid = EBOLA_STATUS_UUIDS_BY_KEY.get(key);
        return uuid == null ? null : workflow.getState(conceptService.getConceptByUuid(uuid));
    }

    public static ProgramWorkflow getEbolaStatusProgramWorkflow() {
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

    public static PersonAttributeType getAssignedLocationAttributeType() {
        return getPersonAttributeType(
                ASSIGNED_LOCATION_PERSON_ATTRIBUTE_TYPE_UUID, "assigned_location");
    }

    public static String getPersonAttributeValue(Person person, PersonAttributeType attrType) {
        PersonAttribute attribute = person.getAttribute(attrType);
        return attribute != null ? attribute.getValue() : null;
    }

    public static void setPersonAttributeValue(Patient patient, PersonAttributeType attrType, String value) {
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

    public static Location getLocationByName(String locationName, Location parent) {
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
}
