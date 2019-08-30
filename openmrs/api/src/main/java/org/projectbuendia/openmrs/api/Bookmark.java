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
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@code Bookmark} represents a bookmark into a dataset.
 * <p/>
 * When a {@code Bookmark} is passed to a method in {@link org.projectbuendia.openmrs.api.db
 * .ProjectBuendiaDAO}, {@code ProjectBuendiaDAO} will ensure that all records returned were either
 * created, modified, or voided on or after the {@code minTime}.
 * For records that were created, modified or voided at the exact instant represented by
 * {@code minTime}, {@code minUuid} is used as an additional filter,
 * and only records that have a UUID greater than or equal to this value will be returned. If
 * {@code minUuid} is null, than only the {@code minTime} is used.
 * <p>
 * Conceptually, the semantics are identical to MySQL's greater-than operator on rows:
 * (geTimestamp, gtUuid) will return records where {@code (geTimestamp > record.timestamp) OR
 * ((geTimestamp = record.timestamp) AND (gtUuid > record.uuid))}. See the <a href=
 * "http://dev.mysql.com/doc/refman/5.7/en/comparison-operators.html#operator_greater-than">MySQL
 * manual</a> for more information.
 */
public class Bookmark {
    public final @Nonnull Date minTime;
    public final @Nullable String minUuid;

    public Bookmark(@Nonnull Date minTime, @Nullable String minUuid) {
        if (minTime == null) {
            throw new IllegalArgumentException("minTime cannot be null");
        }
        this.minTime = minTime;
        this.minUuid = minUuid;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Bookmark &&
            Objects.equals(minTime, ((Bookmark) obj).minTime) &&
            Objects.equals(minUuid, ((Bookmark) obj).minUuid);
    }
}
