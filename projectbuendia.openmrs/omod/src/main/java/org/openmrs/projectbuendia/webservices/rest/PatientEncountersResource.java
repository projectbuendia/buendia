package org.openmrs.projectbuendia.webservices.rest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

/**
 * Read-only resource representing multiple observations for a single patient.
 */
// TODO(jonskeet): Ideally, this would be under patient/{uuid}/encounters; it's unclear whether
// that can be supported here...
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patientencounters", supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*")
public class PatientEncountersResource implements Searchable, Retrievable, Listable {
    
    private static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");
    
    // JSON property names
    private static final String UUID = "uuid";
    private static final String ENCOUNTERS = "encounters";
    private static final String OBSERVATIONS = "observations";
    private static final String TIMESTAMP = "timestamp";
    
    private final PatientService patientService;
    private final EncounterService encounterService;

    public PatientEncountersResource() {
        patientService = Context.getPatientService();
        encounterService = Context.getEncounterService();
    }
    
    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        Patient patient = patientService.getPatientByUuid(uuid);
        if (patient == null) {
            throw new ObjectNotFoundException();
        }
        return patientToJson(patient);
    }
    
    private SimpleObject patientToJson(Patient patient) {
        SimpleObject patientJson = new SimpleObject();
        patientJson.put(UUID, patient.getUuid());
        List<SimpleObject> encounters = new ArrayList<>();
        for (Encounter encounter : encounterService.getEncountersByPatient(patient)) {
            encounters.add(encounterToJson(encounter));
        }
        patientJson.put(ENCOUNTERS, encounters);
        return patientJson;
    }
    
    // TODO(jonskeet): Move out, or find somewhere else this is already done.
    private static String toIso8601(Date dateTime) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(UTC);
        return format.format(dateTime);
    }
    
    private SimpleObject encounterToJson(Encounter encounter) {
        SimpleObject encounterJson = new SimpleObject();
        // TODO: Check what format this ends up in.
        encounterJson.put(TIMESTAMP, toIso8601(encounter.getEncounterDatetime()));
        SimpleObject observations = new SimpleObject();
        for (Obs obs : encounter.getObs()) {
            encounterJson.put(UUID, encounter.getUuid());
            Concept concept = obs.getConcept();
            ConceptDatatype dataType = concept.getDatatype();
            String hl7Type = dataType.getHl7Abbreviation();
            String value;
            switch (hl7Type) {
                case HL7Constants.HL7_BOOLEAN:
                    value = String.valueOf(obs.getValueAsBoolean());
                    break;
                case HL7Constants.HL7_CODED:
                case HL7Constants.HL7_CODED_WITH_EXCEPTIONS:
                    value = obs.getValueCoded().getUuid();
                    break;
                case HL7Constants.HL7_NUMERIC:
                    value = String.valueOf(obs.getValueNumeric());
                    break;
                case HL7Constants.HL7_TEXT:
                    value = obs.getValueText();
                    break;
                default:
                    // TODO(jonskeet): Turn this into a warning log entry?
                    throw new IllegalArgumentException("Unexpected HL7 type: " + hl7Type);
            }
            observations.put(concept.getUuid(), value);
        }
        encounterJson.put(OBSERVATIONS, observations);        
        return encounterJson;
    }

    @Override
    public String getUri(Object instance) {
        Patient patient = (Patient) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + patient.getUuid();
    }
    
    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        // Searchable is only implemented as a workaround to RESTWS-471
        throw new UnsupportedOperationException("Searching not supported");
    }
    
    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.FULL);
    }

    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        List<Patient> patients = patientService.getAllPatients();
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Patient patient : patients) {
            jsonResults.add(patientToJson(patient));
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }
}
