package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Location;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.Collection;

/**
 * REST collection for locations.  In the JSON representation, every
 * location has a parent that can be null, and the "parent_uuid" key
 * is always present though its value may be null.  The parent can
 * be set to null during an update operation by including the
 * "parent_uuid" key with a value of null.
 */
@Resource(
    name = RestController.PATH + "/locations",
    supportedClass = Location.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class LocationResource extends BaseResource<Location> {
    public LocationResource() {
        super("locations", Representation.DEFAULT);
    }

    @Override protected Collection<Location> listItems(RequestContext context) {
        return locationService.getAllLocations(false /* includeRetired */);
    }

    @Override protected Location createItem(SimpleObject data, RequestContext context) {
        Location location = new Location();
        location.setName(Utils.getRequiredString(data, "name"));
        String parentUuid = Utils.getOptionalString(data, "parent_uuid");
        location.setParentLocation(DbUtils.locationsByUuid.get(parentUuid));
        return locationService.saveLocation(location);
    }

    @Override protected Location retrieveItem(String uuid) {
        return locationService.getLocationByUuid(uuid);
    }

    @Override protected Location updateItem(Location location, SimpleObject data, RequestContext context) {
        if (data.containsKey("name")) {
            location.setName(Utils.getRequiredString(data, "name"));
        }
        if (data.containsKey("parent_uuid")) {
            String parentUuid = Utils.getOptionalString(data, "parent_uuid");  // nullable
            location.setParentLocation(DbUtils.locationsByUuid.get(parentUuid));  // nullable
        }
        return locationService.saveLocation(location);
    }

    @Override protected void deleteItem(Location location, String reason, RequestContext context) {
        retireLocationRecursively(location, reason);
    }

    private void retireLocationRecursively(Location location, String reason) {
        // This may retire locations that are assigned to existing patients.
        // The location is stored just as a string attribute on the patient,
        // so this will not break any database constraints; it just means
        // those patients will end up with an unknown location UUID.
        for (Location child : location.getChildLocations()) {
            retireLocationRecursively(child, reason);
        }
        locationService.retireLocation(location, reason);
    }

    @Override protected void populateJson(SimpleObject json, Location location, RequestContext context) {
        json.add("name", location.getName());
        Location parent = location.getParentLocation();
        json.add("parent_uuid", parent != null ? parent.getUuid() : null);
    }
}
