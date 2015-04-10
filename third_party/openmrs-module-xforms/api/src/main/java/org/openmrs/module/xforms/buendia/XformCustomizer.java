/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.xforms.buendia;

import org.openmrs.Concept;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;

import java.util.List;
import java.util.Locale;

/** A class that provides customizable values used for rendering an XForm. */
public class XformCustomizer {

    private final Locale locale = Context.getLocale();

    public String getLabel(Concept c) {
        return c.getName(locale).getName();
    }

    public List<Location> getEncounterLocations() {
        return Context.getLocationService().getAllLocations(false);
    }

    public String getLabel(Location loc) {
        return loc.getName() + " [" + loc.getLocationId() + "]";
    }

    public String getLabel(Provider provider) {
        String name = provider.getName();
        if (name == null) {
            Person person = provider.getPerson();
            name = person.getPersonName().toString();
        }
        String identifier = provider.getIdentifier();
        return name + " [" + identifier + "]";
    }

    public String getGroupLabel(FormField formField) {
        String name = formField.getDescription();
        if (name != null && name.length() > 0) {
            return name;
        }
        name = formField.getName();
        if (name != null && name.length() > 0) {
            return name;
        }
        name = formField.getField().getDescription();
        if (name != null && name.length() > 0) {
            return name;
        }
        name = formField.getField().getName();
        if (name != null && name.length() > 0) {
            return name;
        }
        throw new IllegalArgumentException("No field name available");
    }

    public String getAppearanceAttribute(FormField field) {
        return null;
    }

    public Integer getRows(Concept concept) {
        return null;
    }
}
