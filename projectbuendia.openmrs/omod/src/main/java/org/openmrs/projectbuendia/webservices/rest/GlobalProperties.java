package org.openmrs.projectbuendia.webservices.rest;

/**
 * Project Buendia properties configured in "advanced settings", allowing
 * for values to be preconfigured, but still modifiable without a code change.
 * Every key in here should also be listed in config.xml, along with a description.
 * The two sets of properties must be kept properly in sync.  
 */
public final class GlobalProperties {
    public static final String CHART_UUIDS = "projectbuendia.chartUuids";
    
    private GlobalProperties() {
    }
}
