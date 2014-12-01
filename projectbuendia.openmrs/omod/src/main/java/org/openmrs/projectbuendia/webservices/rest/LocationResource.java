package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.resource.api.Updatable;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.List;

/**
 * Rest API for getting locations in an EMC (Ebola Management Centre). Example JSON for a location looks like:
 *
 * <pre>
 * {
 *   uuid: “1234-5”
 *   names {
 *     “en”: “Kailahun” // Or suspect zone, or tent 3, or bed2, tent 3
 *     “fr”: “Kailahun” // we won’t localize at first, but this gives us the ability to later without code changes
 *   }
 *   parent_uuid: “4567-3” // uuid of parent location
 * }
 * </pre>
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/location", supportedClass = Location.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class LocationResource implements Listable, Searchable, Retrievable, Creatable, Updatable {

    // JSON Constants.
    private static final String UUID = "uuid";
    private static final String PARENT_UUID = "parent_uuid";
    private static final String NAMES = "names";

    // Known locations.
    // The root location.
    private static final String EMC_UUID = "3449f5fe-8e6b-4250-bcaa-fca5df28ddbf";
    private static final String EMC_NAME = "Facility Kailahun";
    // The hard-coded zones. These are (name, UUID) pairs, and are children of the EMC.
    private static final String[][] ZONE_NAMES_AND_UUIDS = {
        {"Triage Zone", "3f75ca61-ec1a-4739-af09-25a84e3dd237"},
        {"Suspected Zone", "2f1e2418-ede6-481a-ad80-b9939a7fde8e"},
        {"Probable Zone", "3b11e7c8-a68a-4a5f-afb3-a4a053592d0e"},
        {"Confirmed Zone", "b9038895-9c9d-4908-9e0d-51fd535ddd3c"},
        {"Morgue", "4ef642b9-9843-4d0d-9b2b-84fe1984801f"},
        {"Discharged", "d7ca63c3-6ea0-4357-82fd-0910cc17a2cb"},
    };

    private static Log log = LogFactory.getLog(PatientResource.class);

    private final LocationService locationService;
    private final Location emcLocation;

    public LocationResource() {
        locationService = Context.getLocationService();
        emcLocation = getEmcLocation(locationService);
    }

    private static Location getEmcLocation(LocationService service) {
        Location location = service.getLocationByUuid(EMC_UUID);
        if (location == null) {
            log.info("Creating root EMC location");
            location = new Location();
            location.setName(EMC_NAME);
            location.setUuid(EMC_UUID);
            location.setDescription(EMC_NAME);
            service.saveLocation(location);
        }
        return location;
    }

    @Override
    public Object create(SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
        return null;
    }

    @Override
    public SimpleObject getAll(RequestContext requestContext) throws ResponseException {
        return null;
    }

    @Override
    public SimpleObject search(RequestContext requestContext) throws ResponseException {
        return null;
    }

    @Override
    public Object retrieve(String s, RequestContext requestContext) throws ResponseException {
        return null;
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return null;
    }

    @Override
    public Object update(String s, SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
        return null;
    }

    @Override
    public String getUri(Object o) {
        return null;
    }
}
