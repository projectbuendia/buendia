package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Provider;

import java.util.List;

/**
 * Interface for describing how to do certain automatic Xform things that may need to be changed.
 */
public interface XformCustomizer {
    /**
     * Generate the string representation for the given concept.
     */
    public String getLabel(Concept c);

    /**
     * @return the choice of locations to provide for the encounter.
     */
    public List<Location> getEncounterLocations();

    /**
     * @return the String label to use for the given encounter location.
     */
    public String getLabel(Location location);

    /**
     * @return the String label to use when displaying providers.
     */
    public String getLabel(Provider provider);

    /**
     * @return the label to attach to a group (section) in the form.
     */
    public String getGroupLabel(FormField field);

    /**
     * @return the contents of the appearance attribute, or null if none
     */
    public String getAppearanceAttribute(FormField field);
}
