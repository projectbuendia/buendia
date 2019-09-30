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
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.projectbuendia.openmrs.api.Bookmark;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.api.db.ProjectBuendiaDAO;
import org.projectbuendia.openmrs.api.db.SyncPage;

import javax.annotation.Nullable;

/** It is a default implementation of {@link ProjectBuendiaService}. */
public class ProjectBuendiaServiceImpl extends BaseOpenmrsService implements ProjectBuendiaService {
    protected final Log log = LogFactory.getLog(this.getClass());

    private ProjectBuendiaDAO dao;

    @Override
    public void setDAO(ProjectBuendiaDAO dao) {
        this.dao = dao;
    }

    @Override
    public void clearCache() {
        if (dao != null) dao.clearCache();
    }

    @Override
    public SyncPage<Obs> getObservationsModifiedAtOrAfter(
        @Nullable Bookmark bookmark, boolean includeVoided, int maxResults) {
        return dao.getObservationsModifiedAfter(bookmark, includeVoided, maxResults);
    }

    @Override
    public SyncPage<Patient> getPatientsModifiedAtOrAfter(
        @Nullable Bookmark bookmark, boolean includeVoided, int maxResults) {
        return dao.getPatientsModifiedAfter(bookmark, includeVoided, maxResults);
    }

    @Override
    public SyncPage<Order> getOrdersModifiedAtOrAfter(
        @Nullable Bookmark bookmark, boolean includeVoided, int maxResults,
        @Nullable Order.Action[] allowedOrderTypes) {
        return dao.getOrdersModifiedAtOrAfter(
            bookmark, includeVoided, maxResults, allowedOrderTypes);
    }
}