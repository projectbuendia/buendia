package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.EncounterProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.Bookmark;
import org.projectbuendia.openmrs.api.db.SyncPage;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Resource(
    name = RestController.PATH + "/observations",
    supportedClass = Obs.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*,1.12.*,2.0.*,2.1.*,2.2.*,2.3.*"
)
public class ObservationResource extends BaseResource<Obs> {
    private static final int MAX_OBSERVATIONS_PER_PAGE = 500;

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

    private SimpleObject abbreviateResult(SimpleObject result) {
        final int MAX_ITEMS = 10;
        List<SimpleObject> items = (List<SimpleObject>) result.get("results");
        List<Object> abbrevItems = new ArrayList<>();
        Object resultsValue = items;

        if (items.size() > MAX_ITEMS) {
            int omitted = items.size() - MAX_ITEMS;
            abbrevItems.add(String.format(
                "...only logging last %d of %d items (%d omitted)...",
                items.size(), MAX_ITEMS, omitted
            ));
            abbrevItems.addAll(items.subList(omitted, items.size()));
            resultsValue = abbrevItems;
        }

        SimpleObject abbrev = new SimpleObject()
            .add("results", resultsValue)
            .add("syncToken", result.get("syncToken"))
            .add("more", result.get("more"));
    }

    private SimpleObject obsToJson(Obs obs) {
        SimpleObject object = new SimpleObject()
            .add("uuid", obs.getUuid())
            .add("voided", obs.isVoided());

        if (obs.isVoided()) {
            return object;
        }

        object
            .add("patient_uuid", obs.getPerson().getUuid())
            .add("encounter_uuid", obs.getEncounter().getUuid())
            .add("concept_uuid", obs.getConcept().getUuid())
            .add("timestamp", Utils.toIso8601(obs.getObsDatetime()));

        Provider provider = Utils.getProviderFromUser(obs.getCreator());
        object.add("enterer_uuid", provider != null ? provider.getUuid() : null);

        boolean isExecutedOrder =
                DbUtil.getOrderExecutedConcept().equals(obs.getConcept()) && obs.getOrder() != null;
        if (isExecutedOrder) {
            // As far as the client knows, a chain of orders is represented by the root order's
            // UUID, so we have to work back through the chain or orders to get the root UUID.
            // Normally, the client will only ever supply observations for the root order ID, but
            // in the event that an order is marked as executed on the server (for example) we don't
            // want that to mean that an order execution gets missed.
            object.add("value", Utils.getRootOrder(obs.getOrder()).getUuid());
        } else {
            object.add("value", ObservationUtils.obsValueToString(obs));
        }

    @Override protected void deleteItem(Obs obs, String reason, RequestContext context) {
        obsService.voidObs(obs, reason + " (from Buendia client)");
    }

    @Override protected void populateJson(SimpleObject json, Obs obs, RequestContext context) {
        ObsUtils.putObsAsJson(json, obs);
    }
}
