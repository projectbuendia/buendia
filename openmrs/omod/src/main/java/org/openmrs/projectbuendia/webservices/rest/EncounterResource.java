package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.Encounter;
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
        Object timestamp = data.get("timestamp");
        try {
            if (timestamp != null) {
                encounterTime = new Date(Long.parseLong(timestamp.toString())*1000L);
            } else {
                // Allow clients to omit the timestamp to use the current server time.
                encounterTime = new Date();
            }
        } catch (NumberFormatException ex) {
            throw new InvalidObjectDataException(
                "Expected seconds since epoch for \"timestamp\" value: " + ex.getMessage());
        }
        return ObservationUtils.addEncounter(
            (List) data.get("observations"), (List) data.get("order_uuids"),
            patient, encounterTime, "ADULTRETURN", (String) data.get("provider_uuid"), null);
    }

    @Override protected void populateJson(SimpleObject json, Encounter item, RequestContext context) {
        if (item.getPatient() != null) {
            json.put("patient_uuid", item.getPatient().getUuid());
        }
        if (item.getEncounterDatetime() != null) {
            json.put("timestamp", Utils.formatUtc8601(item.getEncounterDatetime()));
        }
        json.put("uuid", item.getUuid());

        SimpleObject observations = new SimpleObject();
        List<String> orderUuids = new ArrayList<>();
        for (Obs obs : item.getObs()) {
            Concept concept = obs.getConcept();
            if (concept != null && eq(concept.getUuid(), DbUtils.CONCEPT_ORDER_EXECUTED_UUID)) {
                orderUuids.add(obs.getOrder().getUuid());
                continue;
            }

            // When encounters are returned, the observations are in one big map,
            // which is a different format than when encounters are posted,
            // with an array of observations (see ObservationUtils).
            // TODO(ping): Include the concept type name in this observation dump.
            observations.put(obs.getConcept().getUuid(), ObservationUtils.obsValueToString(obs));
        }
        if (!observations.isEmpty()) {
            json.put("observations", observations);
        }
        if (!orderUuids.isEmpty()) {
            json.put("order_uuids", orderUuids);
        }
    }
}
