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

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

public class UtilsTest {
    @Test
    public void testAlphanumericComparator() throws Exception {
        String[] elements = {"b1", "a11a", "a11", "a2", "a2b", "a02b", "a2a", "a1"};
        String[] sorted = elements.clone();
        Arrays.sort(sorted, Utils.ALPHANUMERIC_COMPARATOR);
        String[] expected = {"a1", "a2", "a2a", "a02b", "a2b", "a11", "a11a", "b1"};
        assertArrayEquals(expected, sorted);
    }
}