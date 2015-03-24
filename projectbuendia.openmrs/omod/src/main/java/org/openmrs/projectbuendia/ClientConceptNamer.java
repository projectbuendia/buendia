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

package org.openmrs.projectbuendia;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static Log log = LogFactory.getLog(ClientConceptNamer.class);

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
     * Gets the best available concept name string to display in the client.
     * The configured locale is checked for a preferred name string, first
     * with and then without the "_client" variant, and then without the
     * region if any.  If this does not succeed, then English is checked,
     * first with and then without the "_client" variant.  For example, if the
     * locale is "es_419", the following locales will be tried, in order:
     * <ol>
     *     <li>es_419_client
     *     <li>es_419
     *     <li>es
     *     <li>en_GB_client
     *     <li>en
     * </ol>
     *
     * If the configured locale is "fr", the sequence will be:
     * <ol>
     *     <li>fr_GB_client
     *     <li>fr
     *     <li>en_GB_client
     *     <li>en
     * </ol>
     *
     * @param concept the concept to get a name for
     * @return a String for the client with the best match we can get for that locale
     */
    public String getClientName(Concept concept) {
        String variant = locale.getVariant();
        Locale.Builder builder = new Locale.Builder().setLocale(locale);
        if (!VARIANT.equals(variant)) {
            builder.setVariant(VARIANT);
            // getCountry() and setRegion() refer to the same field.  Oy.
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
        ConceptName defaultName = concept.getName();
        if (defaultName == null) {
            log.error("tried to get a name for concept, uuid=" + concept.getUuid() + ", id=" + concept.getId()
                    + ", but none found");
            return "UNKNOWN Concept " + concept.getId();
        }
        return defaultName.getName();
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
