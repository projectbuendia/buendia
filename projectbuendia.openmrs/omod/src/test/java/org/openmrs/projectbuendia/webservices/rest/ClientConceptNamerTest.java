package org.openmrs.projectbuendia.webservices.rest;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;

import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for ClientConceptNamer.
 */
public class ClientConceptNamerTest {

    @Test
    public void testConceptNameFailoverToDefault() {
        Concept concept = new Concept();
        ConceptName en = makePreferred("name en", ClientConceptNamer.DEFAULT);
        concept.setNames(Arrays.asList(en));
        String name = new ClientConceptNamer(new Locale("fr")).getClientName(concept);
        assertEquals("name en", name);
    }

    @Test
    public void testConceptNameFailoverToDefaultClient() {
        Concept concept = new Concept();
        ConceptName en = makePreferred("name en", ClientConceptNamer.DEFAULT);
        ConceptName enClient = makePreferred("name en client", ClientConceptNamer.DEFAULT_CLIENT);
        concept.setNames(Arrays.asList(en, enClient));
        String name = new ClientConceptNamer(new Locale("fr")).getClientName(concept);
        assertEquals("name en client", name);
    }

    @Test
    public void testConceptNameFailoverToClientInLocale() {
        Concept concept = new Concept();
        ConceptName en = makePreferred("name en", ClientConceptNamer.DEFAULT);
        ConceptName enClient = makePreferred("name en client", ClientConceptNamer.DEFAULT_CLIENT);
        ConceptName frClient = makePreferred("name fr client", new Locale.Builder()
                .setLanguage("fr")
                .setRegion(ClientConceptNamer.CLIENT_REGION)
                .setVariant(ClientConceptNamer.VARIANT)
                .build());
        concept.setNames(Arrays.asList(en, enClient, frClient));
        String name = new ClientConceptNamer(new Locale("fr")).getClientName(concept);
        assertEquals("name fr client", name);
    }

    @Test
    public void testConceptNameFailoverToLocaleClientWhenLocaleDefaultPresent() {
        Concept concept = new Concept();
        ConceptName en = makePreferred("name en", ClientConceptNamer.DEFAULT);
        ConceptName enClient = makePreferred("name en client", ClientConceptNamer.DEFAULT_CLIENT);
        ConceptName fr = makePreferred("name fr", new Locale("fr"));
        ConceptName frClient = makePreferred("name fr client", new Locale.Builder()
                .setLanguage("fr")
                .setRegion(ClientConceptNamer.CLIENT_REGION)
                .setVariant(ClientConceptNamer.VARIANT)
                .build());
        concept.setNames(Arrays.asList(en, enClient, fr, frClient));
        String name = new ClientConceptNamer(new Locale("fr")).getClientName(concept);
        assertEquals("name fr client", name);
    }

    @Test
    public void testConceptNameFailoverToLocaleDefaultWhenNoLocaleDefault() {
        Concept concept = new Concept();
        ConceptName en = makePreferred("name en", ClientConceptNamer.DEFAULT);
        ConceptName enClient = makePreferred("name en client", ClientConceptNamer.DEFAULT_CLIENT);
        ConceptName fr = makePreferred("name fr", new Locale("fr"));
        concept.setNames(Arrays.asList(en, enClient, fr));
        String name = new ClientConceptNamer(new Locale("fr")).getClientName(concept);
        assertEquals("name fr", name);
    }

    private ConceptName makePreferred(String s, Locale l) {
        ConceptName enClient = new ConceptName(s, l);
        enClient.setLocalePreferred(true);
        return enClient;
    }
}
