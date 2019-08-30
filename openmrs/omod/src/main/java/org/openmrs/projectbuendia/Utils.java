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

import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.projectbuendia.webservices.rest.InvalidObjectDataException;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

public class Utils {
    // ==== Basic types ====

    /** A sane eq operator, to replace Java's broken == and broken equals(). */
    public static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /** A safe check for null or empty strings. */
    public static boolean isEmpty(Object s) {
        return s == null || eq(s, "");
    }

    /** A safe check for null or whitespace strings. */
    public static boolean isBlank(Object s) {
        return s == null || (s instanceof String && ((String) s).trim().isEmpty());
    }

    /** Converts a JSON-parsed number (sometimes Integer, sometimes Long) to a nullable Long. */
    public static Long asLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer) {
            return Long.valueOf((Integer) obj);
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        throw new ClassCastException("Expected value of type Long or Integer");
    }

    /** Converts objects of integer types to BigIntegers. */
    public static BigInteger toBigInteger(Object obj) {
        if (obj instanceof Integer) {
            return BigInteger.valueOf(((Integer) obj).longValue());
        }
        if (obj instanceof Long) {
            return BigInteger.valueOf((Long) obj);
        }
        if (obj instanceof BigInteger) {
            return (BigInteger) obj;
        }
        return null;
    }

    /** Converts nulls to empty strings. */
    public static @Nonnull String toNonnull(String str) {
        return str == null ? "" : str;
    }

    // ==== Dates and times ====

    /** ISO 8601 format for a complete date and time in UTC. */
    public static final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    /** A SimpleDateFormat that formats as "yyyy-MM-dd" in UTC. */
    public static final DateFormat YYYYMMDD_UTC_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    /** A SimpleDateFormat that formats a date and time to be auto-parsed in a spreadsheet. */
    public static final DateFormat SPREADSHEET_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");
    static {
        ISO8601_FORMAT.setTimeZone(UTC);
        YYYYMMDD_UTC_FORMAT.setTimeZone(UTC);
        SPREADSHEET_FORMAT.setTimeZone(UTC);
    }

    /** Formats a {@link Date} as an ISO 8601 string in the UTC timezone. */
    public static String formatUtc8601(Date datetime) {
        return ISO8601_FORMAT.format(datetime);
    }

    /** Parses an ISO 8601-formatted date into a {@link Date}. */
    public static Date parse8601(String iso8601) {
        try {
            return ISO8601_FORMAT.parse(iso8601);
        } catch (ParseException e) {
            throw new InvalidObjectDataException(e.getMessage());
        }
    }

    public static String formatUtcDate(Date date) {
        return YYYYMMDD_UTC_FORMAT.format(date);
    }

    /** Parses a yyyy-MM-dd date, yielding a Date object at UTC midnight on the given date. */
    public static @Nullable Date parseLocalDate(String text) {
        if (text == null) return null;
        try {
            return YYYYMMDD_UTC_FORMAT.parse(text);
        } catch (ParseException e) {
            throw new InvalidObjectDataException(e.getMessage());
        }
    }


    // ==== Ordering ====

    /**
     * Compares two objects that may be null, Integer, Long, BigInteger, or String.
     * null sorts before everything; all integers sort before all strings; integers
     * sort according to numeric value; strings sort according to string value.
     */
    public static final Comparator<Object> NULL_INT_STR_COMPARATOR = new Comparator<Object>() {
        @Override public int compare(Object a, Object b) {
            BigInteger intA = toBigInteger(a);
            BigInteger intB = toBigInteger(b);
            if (intA != null && intB != null) {
                return intA.compareTo(intB);
            }
            if (a instanceof String && b instanceof String) {
                return ((String) a).compareTo((String) b);
            }
            return (a == null ? 0 : intA != null ? 1 : 2)
                - (b == null ? 0 : intB != null ? 1 : 2);
        }
    };

    /**
     * Compares two lists, each of whose elements is a null, Integer, Long,
     * BigInteger, or String, lexicographically by element, just like Python.
     */
    public static final Comparator<List<Object>> NULL_INT_STR_LIST_COMPARATOR = new Comparator<List<Object>>() {
        @Override public int compare(List<Object> a, List<Object> b) {
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                int result = NULL_INT_STR_COMPARATOR.compare(a.get(i), b.get(i));
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
     * Compares two strings in a manner that sorts alphabetic parts in alphabetic
     * order and numeric parts in numeric order, while guaranteeing that:
     * - compare(s, t) == 0 if and only if eq(s, t).
     * - compare(s, s + t) < 0 for any strings s and t.
     * - compare(s + x, s + y) == Integer.compare(x, y) for all integers x, y
     * and strings s that do not end in a digit.
     * - compare(s + t, s + u) == compare(t, u) for all strings s and strings
     * t, u that consist entirely of Unicode letters.
     * For example, the strings ["b1", "a11a", "a11", "a2", "a2b", "a2a", "a1"]
     * have the sort order ["a1", "a2", "a2a", "a2b", "a11", "a11a", "b1"].
     */
    public static final Comparator<String> ALPHANUMERIC_COMPARATOR = new Comparator<String>() {
        @Override public int compare(String a, String b) {
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
            return NULL_INT_STR_LIST_COMPARATOR.compare(aParts, bParts);
        }

        /**
         * Breaks a string into a list of Integers (from sequences of ASCII digits)
         * and Strings (from sequences of letters).  Other characters are ignored.
         */
        private List<Object> getParts(String str) {
            Matcher matcher = NUMBER_OR_WORD_PATTERN.matcher(str);
            List<Object> parts = new ArrayList<>();
            while (matcher.find()) {
                try {
                    String part = matcher.group();
                    String intPart = matcher.group(1);
                    parts.add(intPart != null ? new BigInteger(intPart) : part);
                } catch (Exception e) {  // shouldn't happen, but just in case
                    parts.add(null);
                }
            }
            return parts;
        }
    };


    // === JSON SimpleObjects ===

    public static void requirePropertyAbsent(SimpleObject obj, String key) {
        if (obj.containsKey(key)) {
            throw new InvalidObjectDataException(String.format(
                "Property \"%s\" is not allowed", key));
        }
    }

    public static @Nonnull String getRequiredString(SimpleObject obj, String key) {
        Object value = obj.get(key);
        if (value == null) {
            throw new InvalidObjectDataException(String.format(
                "Required property \"%s\" is missing or null", key));
        }
        if (!(value instanceof String)) {
            throw new InvalidObjectDataException(String.format(
                "Property \"%s\" should be a String, not %s", key, value.getClass()));
        }
        return (String) value;
    }

    public static @Nullable String getOptionalString(SimpleObject obj, String key) {
        return obj.get(key) != null ? getRequiredString(obj, key) : null;
    }

    public static @Nonnull Date getRequiredDateMillis(SimpleObject obj, String key) {
        Object value = obj.get(key);
        if (value == null) {
            throw new InvalidObjectDataException(String.format(
                "Required property \"%s\" is missing or null", key));
        }
        long millis;
        try {
            millis = asLong(value);
        } catch (ClassCastException e) {
            throw new InvalidObjectDataException(String.format(
                "Property \"%s\" should be a number, not %s", key, value.getClass()));
        }
        return new Date(millis);
    }

    public static @Nullable Date getOptionalDateMillis(SimpleObject obj, String key) {
        return obj.get(key) != null ? getRequiredDateMillis(obj, key) : null;
    }


    // ==== HTTP responses ====

    public static void addVersionHeaders(RequestContext context) {
        HttpServletResponse response = context.getResponse();
        response.addHeader("Buendia-Server-Version", "0.13");
        response.addHeader("Buendia-Client-Minimum-Version", "0.17");
    }
}
