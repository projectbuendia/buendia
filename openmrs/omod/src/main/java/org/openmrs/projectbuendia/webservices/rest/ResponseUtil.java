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

import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.SyncToken;

import java.util.Date;
import java.util.List;

/**
 * Utility methods for creating responses.
 */
public class ResponseUtil {
    private ResponseUtil() {}

    public static SimpleObject createIncrementalSyncResults(
            List<SimpleObject> results, Date newSyncToken) {
        return new SimpleObject()
                .add("results", results)
                .add("snapshotTime", Utils.toIso8601(newSyncToken));
    }

    public static SimpleObject createIncrementalSyncResults(
            List<SimpleObject> results, SyncToken syncToken, boolean more) {
        return new SimpleObject()
                .add("results", results)
                .add("syncToken", SyncTokenUtils.syncTokenToJson(syncToken))
                .add("more", more);
    }
}
