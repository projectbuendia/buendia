package org.openmrs.projectbuendia;

import org.openmrs.Concept;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    /**
     * Compares two objects that may each be null, Integer, or String.  null sorts
     * before everything; all Integers sort before all Strings; Integers sort
     * according to numeric value; Strings sort according to string value.
     */
    public static Comparator<Object> nullIntStrComparator = new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
            if (a instanceof Integer && b instanceof Integer) {
                return (Integer) a - (Integer) b;
            }
            if (a instanceof String && b instanceof String) {
                return ((String) a).compareTo((String) b);
            }
            return (a == null ? 0 : a instanceof Integer ? 1 : 2)
                    - (b == null ? 0 : b instanceof Integer ? 1 : 2);
        }
    };

    /**
     * Compares two lists, each of whose elements is a null, Integer, or String,
     * lexicographically by element, just like Python does.
     */
    public static Comparator<List<Object>> nullIntStrListComparator = new Comparator<List<Object>>() {
        @Override
        public int compare(List<Object> a, List<Object> b) {
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                int result = nullIntStrComparator.compare(a.get(i), b.get(i));
                if (result != 0) {
                    return result;
                }
            }
            return a.size() - b.size();
        }
    };

    // Note: Use of \L here assumes a string that is already NFC-normalized.
    private static final Pattern NUMBER_OR_WORD_PATTERN = Pattern.compile("([0-9]+)|\\p{L}+");

    /**
     * Compares two strings in a way that sorts alphabetic parts in alphabetic
     * order and numeric parts in numeric order, while guaranteeing that:
     *   - compare(s, t) == 0 if and only if s.equals(t).
     *   - compare(s, s + t) < 0 for any strings s and t.
     *   - compare(s + x, s + y) == Integer.compare(x, y) for all integers x, y
     *     and strings s that do not end in a digit.
     *   - compare(s + t, s + u) == compare(s, t) for all strings s and strings
     *     t, u that consist entirely of Unicode letters.
     * For example, the strings ["b1", "a11a", "a11", "a2", "a2b", "a2a", "a1"]
     * have the sort order ["a1", "a2", "a2a", "a2b", "a11", "a11a", "b1"].
     */
    public static Comparator<String> alphanumericComparator = new Comparator<String>() {
        /**
         * Breaks a string into a list of Integers (from sequences of ASCII digits)
         * and Strings (from sequences of letters).  Other characters are ignored.
         */
        private List<Object> getParts(String str) {
            Matcher matcher = NUMBER_OR_WORD_PATTERN.matcher(str);
            List<Object> parts = new ArrayList<>();
            while (matcher.find()) {
                String part = matcher.group();
                String intPart = matcher.group(1);
                parts.add(intPart != null ? Integer.valueOf(intPart) : part);
            }
            return parts;
        }

        @Override
        public int compare(String a, String b) {
            String aNormalized = Normalizer.normalize(a == null ? "" : a, Normalizer.Form.NFC);
            String bNormalized = Normalizer.normalize(b == null ? "" : b, Normalizer.Form.NFC);
            List<Object> aParts = getParts(aNormalized);
            List<Object> bParts = getParts(bNormalized);
            // Add a separator to ensure that the tiebreakers added below are never
            // compared against the actual numeric or alphabetic parts.
            aParts.add(null);
            bParts.add(null);
            // Break ties between strings that yield the same parts (e.g. "a04b"
            // and "a4b") using the normalized original string as a tiebreaker.
            aParts.add(aNormalized);
            bParts.add(bNormalized);
            // Break ties between strings that become the same after normalization
            // using the non-normalized string as a further tiebreaker.
            aParts.add(a);
            bParts.add(b);
            return nullIntStrListComparator.compare(aParts, bParts);
        }
    };
}
