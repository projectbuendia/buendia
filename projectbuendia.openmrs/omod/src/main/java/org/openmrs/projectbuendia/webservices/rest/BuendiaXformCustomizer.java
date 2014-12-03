package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Project Buendia choices about how to display elements in the XForms.
 */
public class BuendiaXformCustomizer implements XformCustomizer {

    final ClientConceptNamer namer = new ClientConceptNamer(Context.getLocale());

    @Override
    public String getLabel(Concept c) {
        return namer.getClientName(c);
    }

    @Override
    public List<Location> getEncounterLocations() {
        Location emc = Context.getLocationService().getLocationByUuid(LocationResource.EMC_UUID);
        return new ArrayList<>(emc.getChildLocations());
    }

    @Override
    public String getLabel(Location location) {
        return location.getName();
    }
}
