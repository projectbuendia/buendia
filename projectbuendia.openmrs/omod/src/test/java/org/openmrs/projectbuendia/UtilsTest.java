package org.openmrs.projectbuendia;

import org.junit.Test;
import org.openmrs.Concept;

import java.util.Arrays;

import static org.junit.Assert.*;

public class UtilsTest {
    @Test
    public void testAlphanumericComparator() throws Exception {
        String[] elements = {"b1", "a11a", "a11", "a2", "a2b", "a02b", "a2a", "a1"};
        String[] sorted = elements.clone();
        Arrays.sort(sorted, Utils.alphanumericComparator);
        String[] expected = {"a1", "a2", "a2a", "a02b", "a2b", "a11", "a11a", "b1"};
        assertArrayEquals(expected, sorted);
    }
}