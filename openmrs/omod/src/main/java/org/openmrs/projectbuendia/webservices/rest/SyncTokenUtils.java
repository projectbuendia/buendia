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

package org.openmrs.projectbuendia.webservices.rest;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.SyncToken;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * Utilities for working with {@link SyncToken}s in HTTP requests and responses.
 */
public class SyncTokenUtils {

    private static final String JSON_FIELD_TIMESTAMP = "t";
    private static final String JSON_FIELD_UUID = "u";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Creates a {@link SyncToken} from its corresponding JSON representation. */
    public static SyncToken jsonToSyncToken(String param) throws
            ParseException, JsonParseException, JsonMappingException {
        SimpleObject object;
        try {
            object = SimpleObject.parseJson(param);
        } catch (JsonParseException | JsonMappingException e) {
            // These are subclasses of IOException, so we need to specifically add a catch clause
            // that rethrows.
            throw e;
        } catch (IOException e) {
            // Should never occur, we're reading from a string.
            throw new RuntimeException(e);
        }
        if (object.get(JSON_FIELD_TIMESTAMP) == null) {
            throw new JsonMappingException("Didn't find a valid timestamp field in the sync token");
        }
        return new SyncToken(
                Utils.parse8601(object.get(JSON_FIELD_TIMESTAMP).toString()),
                // This could be null, cast instead of calling toString().
                (String) object.get(JSON_FIELD_UUID));
    }

    /** Converts a {@link SyncToken} into a corresponding JSON representation. */
    public static String syncTokenToJson(SyncToken token) {
        Object object = new SimpleObject()
                .add(JSON_FIELD_TIMESTAMP, Utils.formatUtc8601(token.greaterThanOrEqualToTimestamp))
                .add(JSON_FIELD_UUID, token.greaterThanUuid);
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            // Shouldn't occur.
            throw new RuntimeException(e);
        }
    }

    private static final long REQUEST_BUFFER_WINDOW = 2000;

    /**
     * {@link SyncToken}s from the DAO aren't necessarily directly usable by the client - if the
     * most recently modified record is close enough to the current time, there's a chance that
     * records inserted by concurrent requests will never be synchronized. This method clamps a
     * DAO-provided SyncToken to a few seconds before the request time, to ensure that this edge
     * case can't occur. Note that in some circumstances, this could mean that a record is fetched
     * multiple times, which is ok under our synchronisation model.
     */
    public static SyncToken clampSyncTokenToBufferedRequestTime(
            @Nullable SyncToken token, Date requestTime) {
        if (requestTime == null) {
            throw new IllegalArgumentException("requestTime cannot be null");
        }
        Date earliestAllowableTime = new Date(requestTime.getTime() - REQUEST_BUFFER_WINDOW);
        if (token == null || earliestAllowableTime.before(token.greaterThanOrEqualToTimestamp)) {
            return new SyncToken(earliestAllowableTime, null);
        }
        return token;
    }
}
