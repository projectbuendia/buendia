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
import org.openmrs.util.FormConstants;

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
            // use binary anywhere in the section to add binary select 1
            String name = formField.getName();
            if (name != null && name.contains("binary")) {
                return "binary-select-one";
            }
            if (name != null && name.contains("invisible")) {
                return "invisible";
            }
        }
        return null;
    }
}
