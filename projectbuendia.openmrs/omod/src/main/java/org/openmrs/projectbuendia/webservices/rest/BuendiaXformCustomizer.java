package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        ArrayList<Location> result = new ArrayList<>();
        for (Location child : emc.getChildLocations()) {
            if (!child.isRetired()) {
                result.add(child);
            }
        }
        return result;
    }

    @Override
    public String getLabel(Location location) {
        return location.getName();
    }

    @Override
    public String getLabel(Provider provider) {
        String name = provider.getName();
        if (name == null) {
            Person person = provider.getPerson();
            name = person.getPersonName().toString();
        }
        return name;
    }
}
