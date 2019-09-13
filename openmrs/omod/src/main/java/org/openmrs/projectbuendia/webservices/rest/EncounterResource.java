package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.openmrs.projectbuendia.Utils.eq;

@Resource(
    name = RestController.PATH + "/encounters",
    supportedClass = Location.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class EncounterResource extends BaseResource<Encounter> {
    public EncounterResource() {
        super("encounters", Representation.DEFAULT);
    }

    /** Creates a new item from the posted data. */
    @Override public Object create(SimpleObject data, RequestContext context) throws ResponseException {
        // super.create forbids the "uuid" property, as it should, and we plan
        // to more accurately name this field "patient_uuid", so let's move it
        // to that name for now until the wire format changes and we can delete
        // this method override.
        String uuid = Utils.getRequiredString(data, "uuid");
        data.removeProperty("uuid").add("patient_uuid", uuid);
        return super.create(data, context);
    }

    @Override protected Encounter createItem(SimpleObject data, RequestContext context) {
        // Warning! In order to re-use the observation creation code from PatientResource,
        // the JSON data format for this create method is different from the JSON data format
        // for get. This is terrible REST design. However, we are close to shipping, and
        // I don't want to introduce the server and client side changes needed if I changed
        // the wire format. So instead, there is this comment.
        // TODO: Change the wire format for observations to match the create format.

        String patientUuid = Utils.getRequiredString(data, "patient_uuid");
        Patient patient = patientService.getPatientByUuid(patientUuid);
        if (patient == null) {
            throw new InvalidObjectDataException("Patient not found: " + data.get("uuid"));
        }
        Date encounterTime;
        Object time = data.get("time");
        try {
            if (time != null) {
                encounterTime = Utils.parse8601(time.toString());
            } else {
                // Allow clients to omit the timestamp to use the current server time.
                encounterTime = new Date();
            }
        } catch (NumberFormatException ex) {
            throw new InvalidObjectDataException(
                "Expected seconds since epoch for \"timestamp\" value: " + ex.getMessage());
        }
        return ObsUtils.addEncounter(
            (List<Map>) data.get("observations"), (List<String>) data.get("order_uuids"),
            patient, encounterTime, "ADULTRETURN", (String) data.get("provider_uuid"), null);
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
        ObsUtils.putObservationsAndOrders(json, item.getObs());
    }
}
