package org.openmrs.projectbuendia;

import org.openmrs.projectbuendia.webservices.rest.InvalidObjectDataException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper class for doing date/time work as we don't have Joda.
 */
public class DateTimeUtils {

    private static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");
    public static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    static {
        DateTimeUtils.FORMAT.setTimeZone(UTC);
    }

    /**
     * A SimpleDateFormat that formats as "yyyy-MM-dd".
     */
    public static final DateFormat YYYYMMDD_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * A SimpleDateFormat that formats a date and time so it can be auto-parsed by a spreadsheet .
     */
    public static final DateFormat SPREADSHEET_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static {
        SPREADSHEET_FORMAT.setTimeZone(UTC);
    }

    /**
     * Format a datetime as an ISO8601 String (UTC zone)
     */
    public static String toIso8601(Date dateTime) {
        return FORMAT.format(dateTime);
    }

    /** Parses a date in YYYY-MM-DD format, throwing appropriate exceptions */
    public static Date parseDate(String text, String fieldName) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateFormat.parse(text);
        } catch (ParseException e) {
            throw new InvalidObjectDataException(String.format(
                    "The %s field should be in YYYY-MM-DD format", fieldName));
        }
    }

}
