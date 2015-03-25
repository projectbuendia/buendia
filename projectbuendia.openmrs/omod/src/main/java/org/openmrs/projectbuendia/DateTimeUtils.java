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

import org.openmrs.projectbuendia.webservices.rest.InvalidObjectDataException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Helper class for date/time functions, as we don't have Joda. */
public class DateTimeUtils {

    private static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");

    /** ISO 8601 format for a complete date and time in UTC. */
    public static final DateFormat FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    static {
        DateTimeUtils.FORMAT.setTimeZone(UTC);
    }

    /** A SimpleDateFormat that formats as "yyyy-MM-dd". */
    public static final DateFormat YYYYMMDD_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /** A SimpleDateFormat that formats a date and time so it is auto-parsed in a spreadsheet. */
    public static final DateFormat SPREADSHEET_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static {
        SPREADSHEET_FORMAT.setTimeZone(UTC);
    }

    /** Formats a datetime as an ISO 8601 string in the UTC timezone. */
    public static String toIso8601(Date dateTime) {
        return FORMAT.format(dateTime);
    }

    /** Parses a yyyy-MM-dd date or throws InvalidObjectDataException. */
    public static Date parseDate(String text, String fieldName) {
        try {
            return YYYYMMDD_FORMAT.parse(text);
        } catch (ParseException e) {
            throw new InvalidObjectDataException(String.format(
                    "The %s field should be in yyyy-MM-dd format", fieldName));
        }
    }
}
