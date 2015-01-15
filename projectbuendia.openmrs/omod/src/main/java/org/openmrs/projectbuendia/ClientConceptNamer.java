package org.openmrs.projectbuendia;

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

    public static final String VARIANT = "client";
    public static final String CLIENT_REGION = "GB";
    public static final Locale DEFAULT_CLIENT = new Locale.Builder()
            .setLanguage("en")
            .setRegion(CLIENT_REGION)
            .setVariant(VARIANT)
            .build();
    public static final Locale DEFAULT = new Locale.Builder()
            .setLanguage("en")
            .build();

    private final Locale locale;

    public ClientConceptNamer(Locale locale) {
        this.locale = locale;
    }

    /**
     * Get the name for the concept to display in the client. Suppose we are given the locale es_419. The algorithm
     * used is as follows:
     * <ol>
     *     <li>es_419_client
     *     <li>es_419
     *     <li>es
     *     <li>en_GB_client
     *     <li>en
     * </ol>
     *
     * If we ask for fr the sequence will be
     * <ol>
     *     <li>fr_GB_client
     *     <li>fr
     *     <li>en_GB_client
     *     <li>en
     * </ol>
     *
     * So we try client localisation first, then the correct language, then if that language is not found we fallback
     * to english, client then default. For a given locale we will use the preferred String.
     *
     * @param concept the concept to get a name for
     * @return a String for the client with the best match we can get for that locale
     */
    public String getClientName(Concept concept) {
        String variant = locale.getVariant();
        Locale.Builder builder;
        if (VARIANT.equals(variant)) {
            builder = new Locale.Builder().setLocale(locale);
        } else {
            builder = new Locale.Builder().setLocale(locale)
                    .setVariant(VARIANT);
            if ("".equals(locale.getCountry())) {
                builder.setRegion(CLIENT_REGION);
            }
        }
        // If we already have a client extension, try it, before falling back to English.
        // Don't use the client fallback logic.
        String name = getPreferredStringInLocaleOrNull(concept, builder.build());
        if (name != null) {
            return name;
        }

        // try specifically what was requested. This might try the client variant again, never mind.
        name = getPreferredStringInLocaleOrNull(concept, locale);
        if (name != null) {
            return name;
        }

        // If the requested had a country/region, try it without the region
        if ("".equals(locale.getCountry())) {
            name = getPreferredStringInLocaleOrNull(concept, new Locale(locale.getLanguage()));
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
