package org.openmrs.projectbuendia.webservices.rest;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.openmrs.EncounterProvider;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.Bookmark;
import org.projectbuendia.openmrs.api.db.SyncPage;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.openmrs.projectbuendia.Utils.eq;

@Resource(
    name = RestController.PATH + "/observations",
    supportedClass = Obs.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class ObservationResource extends BaseResource<Obs> {
    private static final int MAX_OBSERVATIONS_PER_PAGE = 100;

    public ObservationResource() {
        super("observations", Representation.DEFAULT);
    }

    @Override protected Collection<Obs> listItems(RequestContext context) {
        return buendiaService.getObservationsModifiedAtOrAfter(
            null, false /* include voided */, MAX_OBSERVATIONS_PER_PAGE).results;
    }

    @Override protected SimpleObject syncItems(Bookmark bookmark, List<Obs> items) {
        SyncPage<Obs> observations = buendiaService.getObservationsModifiedAtOrAfter(
            bookmark, true /* include voided */, MAX_OBSERVATIONS_PER_PAGE);
        items.addAll(observations.results);
        Bookmark newBookmark = Bookmark.clampToBufferedRequestTime(
            observations.bookmark, new Date());
        // If we fetched a full page, there's probably more data available.
        boolean more = observations.results.size() == MAX_OBSERVATIONS_PER_PAGE;
        return new SimpleObject()
            .add("bookmark", newBookmark.serialize())
            .add("more", more);
    }

    @Override protected Obs retrieveItem(String uuid) {
        return obsService.getObsByUuid(uuid);
    }

    @Override protected void deleteItem(Obs obs, String reason, RequestContext context) {
        obsService.voidObs(obs, reason + " (from Buendia client)");
    }

    @Override protected void populateJson(SimpleObject json, Obs obs, RequestContext context) {
        if (obs.getPerson() != null) {
            json.add("patient_uuid", obs.getPerson().getUuid());
        }
        if (obs.getEncounter() != null) {
            json.add("encounter_uuid", obs.getEncounter().getUuid());
        }
        if (obs.getConcept() != null) {
            json.add("concept_uuid", obs.getConcept().getUuid());
            json.add("concept_type", DbUtils.getConceptTypeName(obs.getConcept()));
        }
        if (obs.getObsDatetime() != null) {
            json.add("timestamp", Utils.formatUtc8601(obs.getObsDatetime()));
        }

        for (EncounterProvider ep : obs.getEncounter().getEncounterProviders()) {
            if (ep.getProvider() != null) {
                json.add("enterer_uuid", ep.getProvider().getUuid());
                break;
            }
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
