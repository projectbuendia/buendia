/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distrib-
 * uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
 * specific language governing permissions and limitations under the License.
 */

package org.projectbuendia.openmrs.api;

import java.io.InvalidObjectException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * When a Bookmark is passed to a method in ProjectBuendiaDAO, ProjectBuendiaDAO
 * will ensure that all records returned were either created, modified, or voided
 * on or after the specified minTime.  For records that were created, modified or
 * voided at the exact instant represented by minTime, minUuid is used as an
 * additional filter, and only records that have a UUID greater than or equal to
 * this value will be returned.  If minUuid is null, then only minTime is used.
 *
 * Conceptually, the semantics are identical to MySQL's greater-than operator on rows:
 * (geTimestamp, gtUuid) will return records where geTimestamp > record.timestamp OR
 * ((geTimestamp = record.timestamp) AND (gtUuid > record.uuid)). See the <a href=
 * "http://dev.mysql.com/doc/refman/5.7/en/comparison-operators.html#operator_greater-than">MySQL
 * manual</a> for more information.
 */
public class Bookmark {
    public final @Nonnull Date minTime;
    public final @Nullable String minUuid;

    private static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");
    private static final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    static { ISO8601_FORMAT.setTimeZone(UTC);}
    private static final long REQUEST_BUFFER_WINDOW = 2000;

    public Bookmark(Date minTime, @Nullable String minUuid) {
        if (minTime == null) {
            throw new IllegalArgumentException("minTime cannot be null");
        }
        this.minTime = minTime;
        this.minUuid = minUuid;
    }

    @Override public boolean equals(Object obj) {
        return obj instanceof Bookmark &&
            Objects.equals(minTime, ((Bookmark) obj).minTime) &&
            Objects.equals(minUuid, ((Bookmark) obj).minUuid);
    }

    public String serialize() {
        String result = ISO8601_FORMAT.format(minTime);
        if (minUuid != null) result += "/" + minUuid;
        return result;
    }

    public static Bookmark deserialize(String text) throws ParseException {
        String[] parts = text.split("/", 2);
        Date minTime = parse8601(parts[0]);
        String minUuid = null;
        if (parts.length == 2 && !parts[1].isEmpty()) {
            minUuid = parts[1];
        }
        return new Bookmark(minTime, minUuid);
    }

    /**
     * Bookmarks from the DAO aren't necessarily directly usable by the client - if the
     * most recently modified record is close enough to the current time, there's a chance that
     * records inserted by concurrent requests will never be synchronized. This method clamps a
     * DAO-provided Bookmark to a few seconds before the request time, to ensure that this edge
     * case can't occur. Note that in some circumstances, this could mean that a record is fetched
     * multiple times, which is ok under our synchronisation model.
     */
    public static Bookmark clampToBufferedRequestTime(@Nullable Bookmark bookmark, Date requestTime) {
        if (requestTime == null) {
            throw new IllegalArgumentException("requestTime cannot be null");
        }
        Date earliestAllowableTime = new Date(requestTime.getTime() - REQUEST_BUFFER_WINDOW);
        if (bookmark == null || earliestAllowableTime.before(bookmark.minTime)) {
            return new Bookmark(earliestAllowableTime, null);
        }
        return bookmark;
    }

    private static Date parse8601(String iso8601) throws ParseException {
        return ISO8601_FORMAT.parse(iso8601);
    }
}
