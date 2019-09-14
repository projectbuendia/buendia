package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Resource(
    name = RestController.PATH + "/encounters",
    supportedClass = Location.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class EncounterResource extends BaseResource<Encounter> {
    public EncounterResource() {
        super("encounters", Representation.DEFAULT);
    }

    @Override protected Encounter createItem(SimpleObject data, RequestContext context) {
        String patientUuid = Utils.getRequiredString(data, "patient_uuid");
        Patient patient = patientService.getPatientByUuid(patientUuid);
        if (patient == null) {
            throw new InvalidObjectDataException("Patient not found: " + data.get("uuid"));
        }
        Date encounterTime = Utils.getOptionalDatetime(data, "time");
        return ObsUtils.addEncounter((List<Map>) data.get("observations"), patient,
            encounterTime, "ADULTRETURN", (String) data.get("provider_uuid"), null);
    }

    @Override protected void populateJson(SimpleObject json, Encounter item, RequestContext context) {
        if (item.getPatient() != null) {
            json.put("patient_uuid", item.getPatient().getUuid());
        }
        if (item.getEncounterDatetime() != null) {
            json.put("time", Utils.formatUtc8601(item.getEncounterDatetime()));
        }
        for (EncounterProvider ep : item.getEncounterProviders()) {
            if (ep.getProvider() != null) {
                json.put("provider_uuid", ep.getProvider().getUuid());
                break;
            }
        }
        ObsUtils.putObservationsAsJson(json, item.getObs());
    }
}
