// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.resource.api.Deletable;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.resource.api.Updatable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * REST API for locations (places where patients can be located).
 * <p/>
 * <p>Expected behavior:
 * <ul>
 * <li>GET /location returns all locations ({@link #getAll(RequestContext)})
 * <li>GET /location/[UUID] returns a single location ({@link #retrieve(String, RequestContext)})
 * <li>POST /location adds a location ({@link #create(SimpleObject, RequestContext)}
 * <li>POST /location/[UUID] updates a location ({@link #update(String, SimpleObject, RequestContext)})
 * <li>DELETE /location/[UUID] deletes a location ({@link #delete(String, String, RequestContext)})
 * </ul>
 * <p/>
 * <p>Each operation accepts and returns locations in the following JSON form:
 * <p/>
 * <pre>
 * {
 *   "uuid": “12345678-1234-1234-1234-123456789abc",
 *   "names": {
 *     “en”: “Kailahun”,
 *   }
 *   "parent_uuid": “87654321-4321-4321-4321-cba9876543210"  // parent location
 * }
 * </pre>
 * <p/>
 * <p>If an error occurs, the response will be in the form:
 * <pre>
 * {
 *   "error": {
 *     "message": "[error message]",
 *     "code": "[breakpoint]",
 *     "detail": "[stack trace]"
 *   }
 * }
 * </pre>
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/locations",
    supportedClass = Location.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class LocationResource implements
    Listable, Searchable, Retrievable, Creatable, Updatable, Deletable {

    private static Log log = LogFactory.getLog(PatientResource.class);
    static final RequestLogger logger = RequestLogger.LOGGER;

    private final LocationService locationService;

    public LocationResource() {
        locationService = Context.getLocationService();
    }

    @Override
    public Object create(SimpleObject request, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "create", request);
            Object result = createInner(request);
            logger.reply(context, this, "create", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "create", e);
            throw e;
        }
    }

    private Object createInner(SimpleObject request) throws ResponseException {
        Utils.requirePropertyAbsent(request, "uuid");
        String parentUuid = Utils.getRequiredString(request, "parent_uuid");
        Location parent = locationService.getLocationByUuid(parentUuid);
        if (parent == null) {
            throw new InvalidObjectDataException("No parent location found with UUID " +
                parentUuid);
        }
        Location location = new Location();
        updateNames(request, location);
        location.setParentLocation(parent);
        return locationService.saveLocation(location);
    }

    private void updateNames(SimpleObject request, Location location) {
        Map names = (Map) request.get("names");
        if (names == null || names.isEmpty()) {
            throw new InvalidObjectDataException("No name specified for new location");
        }
        // The use of a "names" object with locale keys inside has never
        // allowed more than one key or used any key other than "en".
        // It now exists only for compatibility and is expected to always
        // contain exactly one key; it is not to be used for localization.
        String name = (String) names.values().iterator().next();
        if (name.isEmpty()) {
            throw new InvalidObjectDataException("Empty name specified for new location");
        }
        Location duplicate = locationService.getLocation(name);
        if (duplicate != null) {
            throw new InvalidObjectDataException(
                String.format("Another location already has the name \"%s\"", name));
        }
        location.setName(name);
        location.setDescription(name);
    }

    @Override public SimpleObject getAll(RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "getAll");
            SimpleObject result = getAllInner();
            logger.reply(context, this, "getAll", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "getAll", e);
            throw e;
        }
    }

    @Override public SimpleObject search(RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "search");
            SimpleObject result = searchInner(context);
            logger.reply(context, this, "search", null);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "search", e);
            throw e;
        }
    }

    @Override public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "retrieve");
            Object result = retrieveInner(uuid);
            logger.reply(context, this, "retrieve", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "retrieve", e);
            throw e;
        }
    }

    private Object retrieveInner(String uuid) throws ResponseException {
        Location location = locationService.getLocationByUuid(uuid);
        if (location == null || location.isRetired()) {
            throw new ObjectNotFoundException();
        }
        return locationToJson(location);
    }

    private SimpleObject locationToJson(Location location) {
        SimpleObject result = new SimpleObject();
        if (location == null) {
            throw new NullPointerException();
        }
        result.add("uuid", location.getUuid());
        Location parentLocation = location.getParentLocation();
        if (parentLocation != null) {
            result.add("parent_uuid", parentLocation.getUuid());
        }
        // The use of a "names" object with locale keys inside has never
        // allowed more than one key or used any key other than "en".
        // It now exists only for compatibility and is expected to always
        // contain exactly one key; it is not to be used for localization.
        SimpleObject names = new SimpleObject();
        names.add("en", location.getDisplayString());
        result.add("names", names);
        return result;
    }

    @Override public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }

    @Override public Object update(String uuid, SimpleObject request, RequestContext context)
        throws ResponseException {
        try {
            logger.request(context, this, "update", request);
            Object result = updateInner(uuid, request);
            logger.reply(context, this, "update", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "update", e);
            throw e;
        }
    }

    private Object updateInner(String uuid, SimpleObject request) throws ResponseException {
        Location existing = locationService.getLocationByUuid(uuid);
        if (existing == null) {
            throw new InvalidObjectDataException("No location found with UUID " + uuid);
        }
        updateNames(request, existing);
        Location location = locationService.saveLocation(existing);
        return locationToJson(location);
    }

    @Override public String getUri(Object instance) {
        Location location = (Location) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + location.getUuid();
    }

    @Override public void delete(String uuid, String reason, RequestContext context)
        throws ResponseException {
        try {
            logger.request(context, this, "delete");
            deleteInner(uuid);
            logger.reply(context, this, "delete", null);
        } catch (Exception e) {
            logger.error(context, this, "delete", e);
            throw e;
        }
    }

    private SimpleObject getAllInner() throws ResponseException {
        ArrayList<SimpleObject> results = new ArrayList<>();
        for (Location location : locationService.getAllLocations()) {
            if (location == null || location.isRetired()) continue;
            results.add(locationToJson(location));
        }
        SimpleObject list = new SimpleObject();
        list.add("results", results);
        return list;
    }

    private SimpleObject searchInner(RequestContext requestContext) throws ResponseException {
        return getAllInner();
    }

    private void deleteInner(String uuid) throws ResponseException {
        Location location = locationService.getLocationByUuid(uuid);
        if (location == null) {
            throw new InvalidObjectDataException("No location found with UUID " + uuid);
        }

        deleteLocationRecursively(location);
    }

    private void deleteLocationRecursively(Location location) {
        // This may delete locations that are assigned to existing patients.
        // The location is stored just as a string attribute on the patient,
        // so this will not break any database constraints; it just means
        // those patients will end up with an unknown location UUID.
        for (Location child : location.getChildLocations()) {
            deleteLocationRecursively(child);
        }
        locationService.purgeLocation(location);
    }
}
