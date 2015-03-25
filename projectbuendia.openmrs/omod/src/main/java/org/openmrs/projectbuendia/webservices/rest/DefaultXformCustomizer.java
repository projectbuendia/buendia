// TODO/license: Confirm whether this file is licensed under MPL 2.0 like
// the rest of OpenMRS and add whatever license header is necessary.

package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;

import java.util.List;
import java.util.Locale;

/**
 * The default implementation of XformCustomizer taken from the original OpenMRS code.
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

    @Override
    public String getLabel(Provider provider) {
        String name = provider.getName();
        if (name == null) {
            Person person = provider.getPerson();
            name = person.getPersonName().toString();
        }
        String identifier = provider.getIdentifier();
        return name + " [" + identifier + "]";
    }

    @Override
    public String getGroupLabel(FormField formField) {
        String name = formField.getDescription();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        name = formField.getName();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        name = formField.getField().getDescription();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        name = formField.getField().getName();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        throw new IllegalArgumentException("No field name available");
    }

    @Override
    public String getAppearanceAttribute(FormField field) {
        return null;
    }

    @Override
    public Integer getRows(Concept concept) {
        return null;
    }
}
