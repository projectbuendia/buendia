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

package org.projectbuendia.openmrs.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.api.db.ProjectBuendiaDAO;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

/** It is a default implementation of {@link ProjectBuendiaService}. */
public class ProjectBuendiaServiceImpl extends BaseOpenmrsService implements ProjectBuendiaService {
    protected final Log log = LogFactory.getLog(this.getClass());

    private ProjectBuendiaDAO dao;

    @Override
    public void setDAO(ProjectBuendiaDAO dao) {
        this.dao = dao;
    }

    @Override
    public List<Encounter> getEncountersModifiedOnOrAfter(@Nullable Date date) {
        return dao.getEncountersModifiedOnOrAfter(date);
    }
}