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

import java.util.Date;

/**
 * A {@code SyncToken} represents a bookmark into a dataset.
 * <p/>
 * When a {@code SyncToken} is passed to a method in {@link org.projectbuendia.openmrs.api.db
 * .ProjectBuendiaDAO}, {@code ProjectBuendiaDAO} will ensure that all records returned were either
 * created, modified, or voided on or after the {@code greaterThanOrEqualToTimestamp}.
 * For records that were created, modified or voided at the exact instant represented by
 * {@code greaterThanOrEqualToTimestamp}, {@code greaterThanUuid} is used as an additional filter,
 * and only records that have a UUID greater than or equal to this value will be returned.
 * <p>
 * Conceptually, the semantics are identical to MySQL's greater-than operator on rows:
 * (geTimestamp, gtUuid) will return records where {@code (geTimestamp > record.timestamp) OR
 * ((geTimestamp = record.timestamp) AND (gtUuid > record.uuid))}. See the <a href=
 * "http://dev.mysql.com/doc/refman/5.7/en/comparison-operators.html#operator_greater-than">MySQL
 * manual</a> for more information.
 */
public class SyncToken {
    public final Date greaterThanOrEqualToTimestamp;
    public final String greaterThanUuid;

    public SyncToken(Date greaterThanOrEqualToTimestamp, String greaterThanUuid) {
        if (greaterThanOrEqualToTimestamp == null || greaterThanUuid == null) {
            throw new IllegalArgumentException("Neither parameter can be null.");
        }
        this.greaterThanOrEqualToTimestamp = greaterThanOrEqualToTimestamp;
        this.greaterThanUuid = greaterThanUuid;
    }
}
