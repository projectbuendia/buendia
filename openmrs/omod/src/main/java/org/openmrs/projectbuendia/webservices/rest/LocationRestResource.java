package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Location;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.Collection;

@Resource(
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/locations",
    supportedClass = Location.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class LocationRestResource extends BaseRestResource<Location> {
    public LocationRestResource() {
        super("locations", Representation.DEFAULT);
    }

    @Override protected Collection<Location> listItems(RequestContext context) {
        return locationService.getAllLocations(false /* includeRetired */);
    }

    @Override protected Location createItem(SimpleObject data, RequestContext context) {
        Location location = new Location();
        String parentUuid = Utils.getOptionalString(data, "parent_uuid");
        if (parentUuid != null) {
            Location parent = locationService.getLocationByUuid(parentUuid);
            if (parent == null) {
                throw new InvalidObjectDataException(String.format(
                    "No parent location found with UUID %s", parentUuid));
            }
            location.setParentLocation(parent);
        }
        location.setName(Utils.getRequiredString(data, "name"));
        return locationService.saveLocation(location);
    }

    @Override protected Location retrieveItem(String uuid) {
        return locationService.getLocationByUuid(uuid);
    }

    @Override protected Location updateItem(Location location, SimpleObject data, RequestContext context) {
        if (data.get("name") != null) {
            location.setName(Utils.getRequiredString(data, "name"));
        }
        return locationService.saveLocation(location);
    }

    @Override protected void deleteItem(Location location, String reason, RequestContext context) {
        deleteLocationRecursively(location, reason);
    }

    private void deleteLocationRecursively(Location location, String reason) {
        // This may delete locations that are assigned to existing patients.
        // The location is stored just as a string attribute on the patient,
        // so this will not break any database constraints; it just means
        // those patients will end up with an unknown location UUID.
        for (Location child : location.getChildLocations()) {
            deleteLocationRecursively(child, reason);
        }
        locationService.retireLocation(location, reason);
    }

    @Override protected void populateJson(SimpleObject json, Location location, RequestContext context) {
        Location parent = location.getParentLocation();
        if (parent != null) json.add("parent_uuid", parent.getUuid());
        json.add("name", location.getName());
        json.add("names", new SimpleObject().add("en", location.getName())); // backward compatibility
    }
}
