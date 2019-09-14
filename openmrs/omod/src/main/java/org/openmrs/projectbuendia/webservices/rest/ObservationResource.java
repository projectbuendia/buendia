package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.EncounterProvider;
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
        ObsUtils.putObsAsJson(json, obs);
    }
}
