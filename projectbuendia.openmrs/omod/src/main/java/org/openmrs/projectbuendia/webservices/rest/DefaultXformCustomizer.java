package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.api.context.Context;

import java.util.List;
import java.util.Locale;

/**
 * Created by nfortescue on 12/3/14.
 */
public class DefaultXformCustomizer implements XformCustomizer {

    private final Locale locale = Context.getLocale();

    @Override
    public String getLabel(Concept c) {
        return c.getName(locale).getName();
    }

    @Override
    public List<Location> getEncounterLocations() {
        return Context.getLocationService().getAllLocations(false);
    }

    @Override
    public String getLabel(Location loc) {
        return loc.getName() + " [" + loc.getLocationId() + "]";
    }
}
