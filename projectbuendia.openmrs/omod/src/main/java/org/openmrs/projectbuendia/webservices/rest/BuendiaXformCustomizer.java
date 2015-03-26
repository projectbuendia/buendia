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

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Field;
import org.openmrs.FieldType;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.projectbuendia.ClientConceptNamer;
import org.openmrs.util.FormConstants;

import java.util.ArrayList;
import java.util.List;

/** Project Buendia choices about how to display elements in the XForms. */
public class BuendiaXformCustomizer implements XformCustomizer {

    final ClientConceptNamer namer = new ClientConceptNamer(Context.getLocale());

    @Override
    public String getLabel(Concept c) {
        return namer.getClientName(c);
    }

    @Override
    public List<Location> getEncounterLocations() {
        Location root = Context.getLocationService().getLocationByUuid(LocationResource.ROOT_UUID);

        ArrayList<Location> result = new ArrayList<>();
        for (Location child : root.getChildLocations()) {
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
    public String getAppearanceAttribute(FormField formField) {
        Field field = formField.getField();
        FieldType fieldType = field.getFieldType();
        if (fieldType.getFieldTypeId().equals(FormConstants.FIELD_TYPE_SECTION)) {
            String extras = "";
            // use binary anywhere in the section to add binary select 1
            String name = formField.getName();
            if (name == null) {
                name = field.getName();
            }
            if (name != null && name.contains("binary")) {
                extras += "|binary-select-one";
            }
            if (name != null && name.contains("invisible")) {
                extras += "|invisible";
            }
            if (!extras.isEmpty()) {
                // Prefix with "full", "compact", or "minimal", per xforms spec.
                return "full" + extras;
            }
        }
        return null;
    }

    @Override
    public Integer getRows(Concept concept) {
        // Once you have 2, the carriage return is shown, so it will expand to more line.
        return 2;
    }
}
