package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.Location;

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
}
