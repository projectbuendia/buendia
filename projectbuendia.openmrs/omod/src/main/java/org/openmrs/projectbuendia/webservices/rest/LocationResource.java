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
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
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
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API for locations (places where patients can be located).
 *
 * <p>Expected behavior:
 * <ul>
 * <li>GET /location returns all locations ({@link #getAll(RequestContext)})
 * <li>GET /location/[UUID] returns a single location ({@link #retrieve(String, RequestContext)})
 * <li>POST /location adds a location ({@link #create(SimpleObject, RequestContext)}
 * <li>POST /location/[UUID] updates a location ({@link #update(String, SimpleObject, RequestContext)})
 * <li>DELETE /location/[UUID] deletes a location ({@link #delete(String, String, RequestContext)})
 * </ul>
 *
 * <p>Each operation accepts and returns locations in the following JSON form:
 *
 * <pre>
 * {
 *   "uuid": “12345678-1234-1234-1234-123456789abc",
 *   "names": {
 *     “en”: “Kailahun”,
 *     “fr”: “Kailahun”  // (if other locales are available in the future)
 *   }
 *   "parent_uuid": “87654321-4321-4321-4321-cba9876543210"  // parent location
 * }
 * </pre>
 *
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
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/location",
    supportedClass = Location.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class LocationResource implements
        Listable, Searchable, Retrievable, Creatable, Updatable, Deletable {

    // Known locations.
    // The root location.
    public static final String ROOT_UUID = "3449f5fe-8e6b-4250-bcaa-fca5df28ddbf";
    // TODO/generalize: The facility name should not be hardcoded here.
    private static final String ROOT_NAME = "Facility Kailahun";
    public static final String TRIAGE_UUID = "3f75ca61-ec1a-4739-af09-25a84e3dd237";
    // The hard-coded zones. These are (name, UUID) pairs, and are children of the root location.
    private static final String[][] ZONE_NAMES_AND_UUIDS = {
        {"Triage Zone", TRIAGE_UUID},
        {"Suspected Zone", "2f1e2418-ede6-481a-ad80-b9939a7fde8e"},
        {"Probable Zone", "3b11e7c8-a68a-4a5f-afb3-a4a053592d0e"},
        {"Confirmed Zone", "b9038895-9c9d-4908-9e0d-51fd535ddd3c"},
        {"Morgue", "4ef642b9-9843-4d0d-9b2b-84fe1984801f"},
        {"Discharged", "d7ca63c3-6ea0-4357-82fd-0910cc17a2cb"},
    };

    private static Log log = LogFactory.getLog(PatientResource.class);
    static final RequestLogger logger = RequestLogger.LOGGER;

    private final LocationService locationService;

    public LocationResource() {
        locationService = Context.getLocationService();
        Location emcLocation = getEmcLocation(locationService);
        ensureZonesExist(locationService, emcLocation);
    }

    private static Location getEmcLocation(LocationService service) {
        Location location = service.getLocationByUuid(ROOT_UUID);
        if (location == null) {
            log.info("Creating root location");
            location = new Location();
            location.setName(ROOT_NAME);
            location.setUuid(ROOT_UUID);
            location.setDescription(ROOT_NAME);
            service.saveLocation(location);
        }
        return location;
    }

    private static void ensureZonesExist(LocationService service, Location emc) {
        for (String[] nameAndUuid : ZONE_NAMES_AND_UUIDS) {
            String name = nameAndUuid[0];
            String uuid = nameAndUuid[1];
            Location zone = service.getLocationByUuid(uuid);
            if (zone == null) {
                log.info("Creating zone location " + name);
                zone = new Location();
                zone.setName(name);
                zone.setUuid(uuid);
                zone.setDescription(name);
                zone.setParentLocation(emc);
                service.saveLocation(zone);
            }
        }
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
        if (request.containsKey("uuid")) {
            throw new InvalidObjectDataException("\"uuid\" key is specified but not allowed");
        }
        String parentUuid = (String) request.get("parent_uuid");
        if (parentUuid == null) {
            throw new InvalidObjectDataException("Required \"parent_uuid\" key is missing");
        }
        Location parent = locationService.getLocationByUuid(parentUuid);
        if (parent == null) {
            throw new InvalidObjectDataException("No parent location found with UUID " + parentUuid);
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
        // TODO(nfortescue): work out if locations can be localized.
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

    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
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

    private SimpleObject getAllInner() throws ResponseException {
        ArrayList<SimpleObject> jsonResults = new ArrayList<>();
        // A new fetch is needed to sort out the hibernate cache.
        Location root = locationService.getLocationByUuid(ROOT_UUID);
        if (root == null) {
            throw new IllegalStateException(
                "Top-level location not found, expected UUID: " + ROOT_UUID);
        }
        addRecursively(root, jsonResults);
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }

    private void addRecursively(Location location, ArrayList<SimpleObject> results) {
        if (location.isRetired()) {
            return;
        }
        results.add(locationToJson(location));
        for (Location child : location.getChildLocations()) {
            addRecursively(child, results);
        }
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
        SimpleObject names = new SimpleObject();
        names.add("en", location.getDisplayString());
        result.add("names", names);
        return result;
    }

    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
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


    private SimpleObject searchInner(RequestContext requestContext) throws ResponseException {
        return getAll(requestContext);
    }
    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "retrieve", uuid);
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
        return location == null ? null : locationToJson(location);
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }


    @Override
    public Object update(String uuid, SimpleObject request, RequestContext context)
            throws ResponseException {
        try {
            logger.request(context, this, "update", uuid + ", " + request);
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

    @Override
    public String getUri(Object instance) {
        Location location = (Location) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + location.getUuid();
    }

    @Override
    public void delete(String uuid, String reason, RequestContext context)
            throws ResponseException {
        try {
            logger.request(context, this, "update", uuid + ", " + reason);
            deleteInner(uuid);
            logger.reply(context, this, "update", null);
        } catch (Exception e) {
            logger.error(context, this, "update", e);
            throw e;
        }
    }

    private void deleteInner(String uuid) throws ResponseException {
        if (ROOT_UUID.equals(uuid)) {
            throw new InvalidObjectDataException("Cannot delete the root location");
        }
        for (String[] nameAndUuid : ZONE_NAMES_AND_UUIDS) {
            if (nameAndUuid[1].equals(uuid)) {
                throw new InvalidObjectDataException(
                        "Cannot delete the zone \"" + nameAndUuid[0] + "\"");
            }
        }
        Location location = locationService.getLocationByUuid(uuid);
        if (location == null) {
            throw new InvalidObjectDataException("No location found with UUID " + uuid);
        }

        deleteLocationRecursively(location);
    }

    private void deleteLocationRecursively(Location location) {
        // We can't rely on database constraints to fail when deleting a
        // location, as locations are only stored as strings.  Checking all
        // the patient attributes and child locations is really slow, and
        // slower than it need be, but deleting locations should be rare.
        PatientService patientService = Context.getPatientService();
        for (Patient patient : patientService.getAllPatients()) {
            Set<PersonAttribute> attributes = patient.getAttributes();
            for (PersonAttribute attribute : attributes) {
                if (attribute.getValue().equals(location.getUuid())) {
                    throw new InvalidObjectDataException(
                            String.format("Cannot delete the location \"%s\""
                                    + " because it has patients assigned to it",
                                    location.getDisplayString()));
                }
            }
        }
        for (Location child : location.getChildLocations()) {
            deleteLocationRecursively(child);
        }
        locationService.purgeLocation(location);
    }
}
