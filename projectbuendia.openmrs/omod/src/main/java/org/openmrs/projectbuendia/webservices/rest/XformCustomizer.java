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

import org.openmrs.Concept;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Provider;

import java.util.List;

/**
 * Interface for describing how to do certain automatic Xform things that may need to be changed.
 */
public interface XformCustomizer {
    /** Generate the string representation for the given concept. */
    public String getLabel(Concept c);

    /** Returns the choice of locations to provide for the encounter. */
    public List<Location> getEncounterLocations();

    /** Returns the String label to use for the given encounter location. */
    public String getLabel(Location location);

    /** Returns the String label to use when displaying providers. */
    public String getLabel(Provider provider);

    /** Returns the label to attach to a group (section) in the form.  */
    public String getGroupLabel(FormField field);

    /**
     * The xform spec says that appearance should be full, compact, or minimal.
     * In order to add extra qualifiers, ODK separates them with pipes |.
     * So you could add for example "full|binary-select-one".
     *
     * @return the contents of the appearance attribute, or null if none
     */
    public String getAppearanceAttribute(FormField field);

    /**
     * Get the number of rows for querying a particular text question.
     *
     * @param concept the concept used for the question
     * @return null for default (single line) or an integer number of rows
     */
    public Integer getRows(Concept concept);
}
