package org.openmrs.projectbuendia.webservices.rest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.projectbuendia.openmrs.webservices.rest.RestController;

/**
 * Read-only resource representing multiple observations for a single patient.
 */
// TODO(jonskeet): Ideally, this would be under patient/{uuid}/encounters; it's unclear whether
// that can be supported here...
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patientencounters", supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class PatientEncountersResource extends AbstractReadOnlyResource<Patient> {
    
    private static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");
    
    // JSON property names
    private static final String UUID = "uuid";
    private static final String ENCOUNTERS = "encounters";
    private static final String OBSERVATIONS = "observations";
    private static final String TIMESTAMP = "timestamp";
    
    private final PatientService patientService;
    private final EncounterService encounterService;

    public PatientEncountersResource() {
        super("patient", Representation.DEFAULT);
        patientService = Context.getPatientService();
        encounterService = Context.getEncounterService();
    }
    
    @Override
    protected Patient retrieveImpl(String uuid, RequestContext context) {
        return patientService.getPatientByUuid(uuid);
    }
    
    @Override
    public List<Patient> searchImpl(RequestContext context) {
        return patientService.getAllPatients();
    }
    
    
    @Override
    protected void populateJsonProperties(Patient patient, RequestContext context, SimpleObject json) {
        List<SimpleObject> encounters = new ArrayList<>();
        for (Encounter encounter : encounterService.getEncountersByPatient(patient)) {
            encounters.add(encounterToJson(encounter));
        }
        json.put(ENCOUNTERS, encounters);
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
                    throw new IllegalArgumentException("Unexpected HL7 type: " + hl7Type + " for concept " + concept);
            }
            observations.put(concept.getUuid(), value);
        }
        encounterJson.put(OBSERVATIONS, observations);        
        return encounterJson;
    }
}
