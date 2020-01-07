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

import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.annotation.Authorized;
import org.openmrs.util.PrivilegeConstants;
import org.projectbuendia.openmrs.api.db.ProjectBuendiaDAO;
import org.projectbuendia.openmrs.api.db.SyncPage;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;

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

    void clearCache();

    /**
     * Returns all observations modified on or after the given {@code date}.
     * @param bookmark a token representing the first record to be excluded from the result set.
     *                  See {@link Bookmark} for more information.
     * @param includeVoided if {@code true}, results will include voided observations.
     * @param maxResults the maximum number of results to fetch. If {@code <= 0}, returns all
     */
    @Authorized(PrivilegeConstants.GET_OBS)
    SyncPage<Obs> getObservationsModifiedAtOrAfter(
        @Nullable Bookmark bookmark, boolean includeVoided, int maxResults)
        throws APIException;

    /**
     * Returns all patients modified on or after the given {@code date}.
     * @param bookmark a token representing the first record to be excluded from the result set.
     *                  See {@link Bookmark} for more information.
     * @param includeVoided if {@code true}, results will include voided patients.
     * @param maxResults the maximum number of results to fetch. If {@code <= 0}, returns all
     */
    @Authorized(PrivilegeConstants.GET_PATIENTS)
    SyncPage<Patient> getPatientsModifiedAtOrAfter(
        @Nullable Bookmark bookmark, boolean includeVoided, int maxResults)
        throws APIException;

    /**
     * Returns all orders modified on or after the given {@code date}.
     * @param bookmark a token representing the first record to be excluded from the result set.
     *                  See {@link Bookmark} for more information.
     * @param includeVoided if {@code true}, results will include voided orders.
     * @param maxResults the maximum number of results to fetch. If {@code <= 0}, returns all
     * @param allowedOrderTypes only order types specified in this whitelist will be fetched. If
     *                          null, all order types are permissible.
     */
    @Authorized(PrivilegeConstants.GET_ORDERS)
    SyncPage<Order> getOrdersModifiedAtOrAfter(
        @Nullable Bookmark bookmark, boolean includeVoided, int maxResults,
        @Nullable Order.Action[] allowedOrderTypes)
        throws APIException;
}
