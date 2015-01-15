package org.openmrs.projectbuendia;

import java.text.DateFormat;
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
}
