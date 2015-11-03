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

package org.projectbuendia.openmrs.api;

import org.openmrs.Encounter;
import org.openmrs.api.OpenmrsService;
import org.projectbuendia.openmrs.api.db.ProjectBuendiaDAO;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

/**
 * This service exposes module's core functionality. It is a Spring managed bean which is
 * configured in moduleApplicationContext.xml.
 * <p/>
 * It can be accessed only via Context:<br>
 * <code>
 * Context.getService(ProjectBuendiaService.class).someMethod();
 * </code>
 * @see org.openmrs.api.context.Context
 */
@Transactional
public interface ProjectBuendiaService extends OpenmrsService {

    /** Sets the DAO for this service. This is done by DI and Spring. */
    void setDAO(ProjectBuendiaDAO dao);

    /**
     * Returns all encounters modified on or after the given {code date}.
     * @param date if {@code null}, returns all encounters since the beginning of time.
     */
    List<Encounter> getEncountersCreatedAtOrAfter(@Nullable Date date);
}