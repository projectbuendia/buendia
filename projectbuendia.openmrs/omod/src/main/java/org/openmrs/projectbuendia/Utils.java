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
     * Compares two objects that may each be Integer or String.  All Integers
     * sort before all Strings; Integers compare according to their numeric
     * value and Strings compare according to their string value.
     */
    public static Comparator<Object> integerOrStringComparator = new Comparator<Object>() {
        public int compare(Object a, Object b) {
            if (a instanceof Integer && b instanceof Integer) {
                return (Integer) a - (Integer) b;
            }
            if (a instanceof String && b instanceof String) {
                return ((String) a).compareTo((String) b);
            }
            return (a instanceof String ? 1 : 0) - (b instanceof String ? 1 : 0);
        }
    };

    /**
     * Compares two lists, whose elements may be Integers or Strings,
     * lexicographically by element, just like Python does.
     */
    public static Comparator<List<Object>> integerOrStringListComparator = new Comparator<List<Object>>() {
        public int compare(List<Object> a, List<Object> b) {
            int result = 0;
            for (int i = 0; result == 0; i++) {
                if (i >= a.size() || i >= b.size()) {
                    return a.size() - b.size();
                }
                result = integerOrStringComparator.compare(a.get(i), b.get(i));
            }
            return result;
        }
    };

    /**
     * Compares two strings in a way that sorts alphabetic parts in alphabetic
     * order and numeric parts in numeric order, returning -1, 0, or 1.
     * For example, the strings ["b1", "a11a", "a11", "a2", "a2b", "a2a", "a1"]
     * have the sort order ["a1", "a2", "a2a", "a2b", "a11", "a11a", "b1"].
     */
    public static Comparator<String> alphanumericComparator = new Comparator<String>() {
        /**
         * Breaks a string into an array of Numbers (from sequences of ASCII digits)
         * and Strings (from sequences of Unicode letters after NFC normalization).
         * Other characters are ignored.
         */
        private List<Object> getParts(String str) {
            Pattern numberOrWord = Pattern.compile("[0-9]+|\\p{L}+");
            Matcher matcher = numberOrWord.matcher(
                    str == null ? "" : Normalizer.normalize(str, Normalizer.Form.NFC));
            List<Object> parts = new ArrayList<>();
            while (matcher.find()) {
                String part = matcher.group();
                parts.add(part.matches("[0-9]+") ? Integer.valueOf(part) : part);
            }
            return parts;
        }

        public int compare(String a, String b) {
            List<Object> aParts = getParts(a);
            List<Object> bParts = getParts(b);
            // for breaking ties, e.g. "a04b" < "a4b"
            aParts.add("");
            aParts.add(a);
            bParts.add("");
            bParts.add(b);
            return integerOrStringListComparator.compare(aParts, bParts);
        }
    };

    /**
     * Formats a Concept UUID for spreadsheet export.  Null UUIDs become "".
     * UUIDs consisting only of a decimal integer followed by "A"s,
     * where the integer contains at most 8 digits and has no leading zeroes,
     * are displayed as just the integer.  All other UUIDs are formatted in
     * blocks of 8, 4, 4, 4, and 12 lowercase hex digits separated by hyphens.
     */
    public static String formatConceptUuid(Concept concept) {
        String uuid = concept.getUuid();
        if (uuid == null || uuid.isEmpty()) {
            return "";
        }
        String hexDigits = uuid.replace("-", "").toLowerCase();
        if (!hexDigits.matches("^[0-9a-f]{32}$")) {
            throw new IllegalArgumentException("\"" + uuid + "\" is not a valid UUID");
        }
        Matcher matcher = Pattern.compile("^([1-9][0-9]{0,7})a+$").matcher(hexDigits);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return hexDigits.substring(0, 8) + "-" +
                hexDigits.substring(8, 12) + "-" +
                hexDigits.substring(12, 16) + "-" +
                hexDigits.substring(16, 20) + "-" +
                hexDigits.substring(20, 32);
    }
}
