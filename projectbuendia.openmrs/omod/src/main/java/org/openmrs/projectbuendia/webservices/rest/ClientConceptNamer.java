package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.springframework.util.ObjectUtils;

import java.util.Locale;

/**
 * A class to get a String representing a concept in the client. For a full document on why this is hard,
 * and why we decided to make these decisions see
 * https://docs.google.com/document/d/1ILDgcEDp_Bdm3q7IJa0gys4I-WfpJdATwuyI1pofuHE/edit?usp=sharing
 */
public class ClientConceptNamer {

    public static final String EXTENSION = "client";
    public static final Locale DEFAULT_CLIENT = new Locale.Builder()
            .setLanguage("en")
            .setExtension(Locale.PRIVATE_USE_EXTENSION, EXTENSION)
            .build();
    public static final Locale DEFAULT = new Locale.Builder()
            .setLanguage("en")
            .build();
    /**
     * Get the name for the concept to display in the client. Suppose we are given the locale es_419. The algorithm
     * used is as follows:
     * <ol>
     *     <li>es-419-x-client
     *     <li>es-419
     *     <li>es
     *     <li>en-x-client
     *     <li>en
     * </ol>
     *
     * So we try client localisation first, then the correct language, then if that language is not found we fallback
     * to english, client then default. For a given locale we will use the preferred String.
     *
     * @param concept the concept to get a name for
     * @param locale the locale the client wants
     * @return a String for the client with the best match we can get for that locale
     */
    public String getClientName(Concept concept, Locale locale) {
        String extension = locale.getExtension(Locale.PRIVATE_USE_EXTENSION);
        Locale.Builder builder;
        if (EXTENSION.equals(extension)) {
            builder = new Locale.Builder().setLocale(locale);
        } else {
            builder = new Locale.Builder().setLocale(locale)
                    .setExtension(Locale.PRIVATE_USE_EXTENSION, EXTENSION);
        }
        // If we already have a client extension, try it, before falling back to English.
        // Don't use the client fallback logic.
        String name = getPreferredStringInLocaleOrNull(concept, builder.build());
        if (name != null) {
            return name;
        }
        builder.clearExtensions();
        Locale noExtensions = builder.build();
        name = getPreferredStringInLocaleOrNull(concept, noExtensions);
        if (name != null) {
            return name;
        }
        Locale justLanguage = new Locale.Builder().setLanguage(noExtensions.getLanguage()).build();
        if (!justLanguage.equals(noExtensions)) {
            // If just language is different from no Extensions, fall back to just language.
            name = getPreferredStringInLocaleOrNull(concept, justLanguage);
            if (name != null) {
                return name;
            }
        }

        // By the time we reach here the locale asked for has failed. Try our two defaults.
        name = getPreferredStringInLocaleOrNull(concept, DEFAULT_CLIENT);
        if (name != null) {
            return name;
        }
        name = getPreferredStringInLocaleOrNull(concept, DEFAULT);
        if (name != null) {
            return name;
        }
        // fail over to anything we can get
        return concept.getName().getName();
    }

    private String getPreferredStringInLocaleOrNull(Concept concept, Locale locale) {
        for (ConceptName nameInLocale : concept.getNames(locale)) {
            if (ObjectUtils.nullSafeEquals(nameInLocale.isLocalePreferred(), true)) {
                return nameInLocale.getName();
            }
        }
        return null;
    }
}
