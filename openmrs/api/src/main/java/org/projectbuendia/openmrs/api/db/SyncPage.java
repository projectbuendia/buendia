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

package org.projectbuendia.openmrs.api.db;

import org.projectbuendia.openmrs.api.SyncToken;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A result set that contains a list of results, and possibly a {@link SyncToken}
 * that can be used to fetch the next set of results.
 */
public class SyncPage<T> {
    public final List<T> results;
    @Nullable
    public final SyncToken syncToken;

    public SyncPage(List<T> results, @Nullable SyncToken syncToken) {
        this.results = results;
        this.syncToken = syncToken;
    }
}
