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

import org.openmrs.EncounterRole;
import org.openmrs.Order;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.projectbuendia.webservices.rest.InvalidObjectDataException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    // ==== Basic types ====

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
    public static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    /** A SimpleDateFormat that formats as "yyyy-MM-dd" in UTC. */
    public static final DateFormat YYYYMMDD_UTC_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    /** A SimpleDateFormat that formats a date and time to be auto-parsed in a spreadsheet. */
    public static final DateFormat SPREADSHEET_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");
    static {
        FORMAT.setTimeZone(UTC);
        YYYYMMDD_UTC_FORMAT.setTimeZone(UTC);
        SPREADSHEET_FORMAT.setTimeZone(UTC);
    }

    /** Formats a {@link Date} as an ISO 8601 string in the UTC timezone. */
    public static String formatUtc8601(Date datetime) {
        return FORMAT.format(datetime);
    }

    /** Parses an ISO 8601-formatted date into a {@link Date}. */
    public static Date parse8601(String iso8601) {
        try {
            return FORMAT.parse(iso8601);
        } catch (ParseException e) {
            throw new InvalidObjectDataException(e.getMessage());
        }
    }

    public static String formatUtcDate(Date date) {
        return YYYYMMDD_UTC_FORMAT.format(date);
    }

    /** Parses a yyyy-MM-dd date, yielding a Date object at UTC midnight on the given date. */
    public static Date parseLocalDate(String text) {
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
     * - compare(s, t) == 0 if and only if s.equals(t).
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


    // ==== OpenMRS ====

    /** Returns the currently authenticated user. */
    public static User getAuthenticatedUser() {
        return Context.getUserContext().getAuthenticatedUser();
    }

    public static EncounterRole getUnknownEncounterRole() {
        EncounterService encounterService = Context.getEncounterService();
        for (EncounterRole role : encounterService.getAllEncounterRoles(true)) {
            if (!role.isRetired() && role.getName().equalsIgnoreCase("unknown")) {
                return role;
            }
        }
        EncounterRole unknownRole = new EncounterRole();
        unknownRole.setName("Unknown");
        encounterService.saveEncounterRole(unknownRole);
        return unknownRole;
    }

    /**
     * Adjusts an encounter datetime to ensure that OpenMRS will accept it.
     * The OpenMRS core is not designed for a client-server setup -- it will
     * summarily reject a submitted encounter if the encounter_datetime is in
     * the future, even if the client's clock is off by only one millisecond.
     * @param datetime The date and time of an encounter.
     * @return
     */
    public static Date fixEncounterDatetime(Date datetime) {
        Date now = new Date();
        if (datetime.after(now)) {
            datetime = now;
        }
        return datetime;
    }

    /** Iterates backwards through revision orders until it finds the root order. */
    public static Order getRootOrder(Order order) {
        while (order.getPreviousOrder() != null) {
            order = order.getPreviousOrder();
        }
        return order;
    }

    public static void requirePropertyAbsent(SimpleObject obj, String key) {
        if (obj.containsKey(key)) {
            throw new InvalidObjectDataException(String.format(
                "Property \"%s\" is not allowed", key));
        }
    }

    public static String getRequiredString(SimpleObject obj, String key) {
        Object value = obj.get(key);
        if (obj == null) {
            throw new InvalidObjectDataException(String.format(
                "Required property \"%s\" is missing", key));
        }
        try {
            return (String) value;
        } catch (ClassCastException e) {
            throw new InvalidObjectDataException(String.format(
                "Required property \"%s\" should be a String, not %s", key, value.getClass()));
        }
    }
}
