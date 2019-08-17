package org.openmrs.projectbuendia.webservices.rest;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.openmrs.EncounterProvider;
import org.openmrs.Obs;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.SyncPage;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.openmrs.projectbuendia.Utils.eq;

@Resource(
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/observations",
    supportedClass = Obs.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class ObservationRestResource extends BaseRestResource<Obs> {
    private static final int MAX_OBSERVATIONS_PER_PAGE = 100;

    public ObservationRestResource() {
        super("observations", Representation.DEFAULT);
    }

    @Override protected SimpleObject syncItems(String tokenJson, List<Obs> items) {
        SyncToken token;
        try {
            token = SyncTokenUtils.jsonToSyncToken(tokenJson);
        } catch (ParseException | JsonParseException | JsonMappingException e) {
            throw new IllegalPropertyException(String.format(
                "Invalid sync token \"%s\"", tokenJson));
        }
        SyncPage<Obs> observations = buendiaService.getObservationsModifiedAtOrAfter(
            token, true /* include voided */, MAX_OBSERVATIONS_PER_PAGE);
        items.addAll(observations.results);
        SyncToken newToken = SyncTokenUtils.clampSyncTokenToBufferedRequestTime(
            observations.syncToken, new Date());
        // If we fetched a full page, there's probably more data available.
        boolean more = observations.results.size() == MAX_OBSERVATIONS_PER_PAGE;
        return new SimpleObject()
            .add("syncToken", SyncTokenUtils.syncTokenToJson(newToken))
            .add("more", more);
    }

    @Override protected void populateJson(SimpleObject json, Obs obs, RequestContext context) {
        json.add("patient_uuid", obs.getPerson().getUuid());
        json.add("encounter_uuid", obs.getEncounter().getUuid());
        json.add("concept_uuid", obs.getConcept().getUuid());
        json.add("timestamp", Utils.formatUtc8601(obs.getObsDatetime()));

        for (EncounterProvider ep : obs.getEncounter().getEncounterProviders()) {
            json.add("enterer_uuid", ep.getProvider().getUuid());
            break;
        }

        boolean isExecutedOrder = obs.getOrder() != null &&
            eq(DbUtils.getOrderExecutedConcept(), obs.getConcept());
        if (isExecutedOrder) {
            // As far as the client knows, a chain of orders is represented by the root order's
            // UUID, so we have to work back through the chain or orders to get the root UUID.
            // Normally, the client will only ever supply observations for the root order ID, but
            // in the event that an order is marked as executed on the server (for example) we don't
            // want that to mean that an order execution gets missed.
            json.add("value", DbUtils.getRootOrder(obs.getOrder()).getUuid());
        } else {
            json.add("value", ObservationUtils.obsValueToString(obs));
        }
    }
}
