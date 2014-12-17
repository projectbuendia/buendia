package org.openmrs.projectbuendia.webservices.rest;

/**
 * Simple bean for giving information about an XForm form after it has been built.
 */
public class FormData {

    /**
     * The Xform XML representation on the form.
     */
    public final String xml;
    /**
     * True if the generated representation includes data about the current available providers;
     */
    public final boolean includesProviders;
    /**
     * True if the generated representation includes data about the current available locations;
     */
    public final boolean includesLocations;

    public FormData(String xml, boolean includesProviders, boolean includesLocations) {
        this.xml = xml;
        this.includesProviders = includesProviders;
        this.includesLocations = includesLocations;
    }
}
