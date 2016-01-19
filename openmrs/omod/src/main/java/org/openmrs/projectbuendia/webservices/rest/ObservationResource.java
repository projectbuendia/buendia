/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distrib-
 * uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
 * specific language governing permissions and limitations under the License.
 */

package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Obs;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.SyncPage;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A resource that allows observations to be incrementally synced.
 * Note: this resource is read-only. For creates, see {@link EncounterResource}.
 */
@Resource(
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/observations",
    supportedClass = Obs.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*")
public class ObservationResource implements Listable, Searchable {

    private static final int MAX_OBS_PER_PAGE = 500;

    private final ProjectBuendiaService buendiaService;

    public ObservationResource() {
        buendiaService = Context.getService(ProjectBuendiaService.class);
    }

    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        return handleSync(context);
    }

    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        return handleSync(context);
    }

    private SimpleObject handleSync(RequestContext context) {
        SyncToken syncFrom = RequestUtil.mustParseSyncToken(context);
        Date requestTime = new Date();

        SyncPage<Obs> observations =
                buendiaService.getObservationsModifiedAtOrAfter(
                        syncFrom, syncFrom != null, MAX_OBS_PER_PAGE);
        List<SimpleObject> jsonResults = new ArrayList<>(observations.results.size());
        for (Obs obs : observations.results) {
            jsonResults.add(obsToJson(obs));
        }
        SyncToken newToken = SyncTokenUtils.clampSyncTokenToBufferedRequestTime(
                observations.syncToken, requestTime);
        // If we fetched a full page, there's probably more data available.
        boolean more = observations.results.size() == MAX_OBS_PER_PAGE;
        return ResponseUtil.createIncrementalSyncResults(jsonResults, newToken, more);
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
            object.add("value", ObservationsHandler.obsValueToString(obs));
        }

        return object;
    }

    @Override
    public String getUri(Object instance) {
        Obs obs = (Obs) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + obs.getUuid();
    }
}
