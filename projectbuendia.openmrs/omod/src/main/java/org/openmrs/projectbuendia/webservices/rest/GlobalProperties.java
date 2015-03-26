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

/**
 * Project Buendia properties configured in "advanced settings", allowing
 * for values to be preconfigured, but still modifiable without a code change.
 * Every key in here should also be listed in config.xml with a description.
 * The two sets of properties must be kept properly in sync.
 */
public final class GlobalProperties {
    public static final String CHART_UUIDS = "projectbuendia.chartUuids";

    private GlobalProperties() {
    }
}
