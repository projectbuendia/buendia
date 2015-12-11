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

package org.openmrs.projectbuendia.webservices.rest;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.SyncToken;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.util.Date;
import java.util.Objects;

/**
 * Utilities for working with requests and request parameters.
 */
public class RequestUtil {
    /**
     * Obtains the "Sync From" date from a request. This is stored in the "since" HTTP parameter
     * in ISO 8601 format.
     * @param context the relevant request.
     * @return the instant that data should be provided from (inclusive), or {@code null} if the
     * "since" parameter was not present in the request.
     */
    @Nullable
    public static Date getSyncFromDate(RequestContext context) throws ParseException {
        String param = context.getParameter("since");
        if (param == null) {
            return null;
        }
        return Utils.fromIso8601(param);
    }

    /**
     * Identical to {@link #getSyncFromDate(RequestContext)}, but throws an
     * {@link IllegalPropertyException} with an appropriate error message instead of a
     * {@link ParseException} if the date fails to parse.
     */
    @Nullable
    public static Date mustParseSyncFromDate(RequestContext context)
            throws IllegalPropertyException {
        try {
            return getSyncFromDate(context);
        } catch (ParseException e) {
            throw new IllegalPropertyException("Date Format invalid, expected ISO 8601");
        }
    }

    /**
     * Obtains the {@link SyncToken} from a request. This is stored in the "since" HTTP parameter.
     * @param context the relevant request.
     * @return the instant that data should be provided from (inclusive), or {@code null} if the
     * "since" parameter was not present in the request.
     */
    @Nullable
    public static SyncToken getSyncToken(RequestContext context) throws
            ParseException, JsonParseException, JsonMappingException {
        String param = context.getParameter("since");
        if (param == null || "".equals(param)) {
            return null;
        }
        return SyncTokenUtils.jsonToSyncToken(param);
    }

    /**
     * Identical to {@link #getSyncToken(RequestContext)}, but throws an
     * {@link IllegalPropertyException} with an appropriate error message instead of a
     * {@link ParseException} if the date fails to parse.
     */
    @Nullable
    public static SyncToken mustParseSyncToken(RequestContext context)
            throws IllegalPropertyException {
        try {
            return getSyncToken(context);
        } catch (JsonMappingException | JsonParseException | ParseException e) {
            throw new IllegalPropertyException("Sync token invalid, " +
                    "ensure that the supplied sync token originated from the server");
        }
    }
}
