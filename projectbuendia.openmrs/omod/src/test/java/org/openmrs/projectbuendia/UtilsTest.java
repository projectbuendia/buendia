package org.openmrs.projectbuendia;

import org.junit.Test;
import org.openmrs.Concept;

import java.util.Arrays;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void testFormatConceptUuid() throws Exception {
        for (String[] pair : new String[][] {
                {"", ""},
                {"123", "123AAAAAAAAAAAAAAAAAAAAAAAAAAAAA"},
                {"0123aaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        "0123AAAAAAAAAAAAAAAAAAAAAAAAAAAA"},
                {"123aaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0",
                        "123AAAAAAAAAAAAAAAAAAAAAAAAAAAA0"},
                {"01234567-89ab-cdef-0123-456789abcdef",
                        "0123456789abcdef0123456789abcdef"},
                {"01234567-89ab-cdef-0123-456789abcdef",
                        "0123-4567-89ab-cdef-0123-4567-89ab-cdef"},
        }) {
            String expected = pair[0], uuid = pair[1];
            Concept concept = new Concept();
            concept.setUuid(uuid);
            String result = Utils.formatConceptUuid(concept);
            assertEquals(expected, result);
        }
    }

    @Test
    public void testAlphanumericComparator() throws Exception {
        String[] elements = {"b1", "a11a", "a11", "a2", "a2b", "a02b", "a2a", "a1"};
        String[] sorted = elements.clone();
        Arrays.sort(sorted, Utils.alphanumericComparator);
        String[] expected = {"a1", "a2", "a2a", "a02b", "a2b", "a11", "a11a", "b1"};
        assertArrayEquals(expected, sorted);
    }
}