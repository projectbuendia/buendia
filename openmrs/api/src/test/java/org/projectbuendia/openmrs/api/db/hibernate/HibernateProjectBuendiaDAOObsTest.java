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

package org.projectbuendia.openmrs.api.db.hibernate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.SyncPage;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

/**
 * Test the observation-handling logic in {@link HibernateProjectBuendiaDAO}. Tests in this class
 * test the DAO logic, even though they interact with the service.
 */
public class HibernateProjectBuendiaDAOObsTest extends HibernateProjectBuendiaDAOTest {

    // Dataset 1: no duplicate update times. Includes voided observations

    private static final String DATASET_NO_DUPLICATE_TIMESTAMPS =
            "org/projectbuendia/openmrs/include/obsDataSetNoDuplicateTimestamps.xml";

    private static final String[] EXPECTED_ORDER_NO_DUPLICATES_INCLUDE_VOIDED = new String[] {
            "fffffff",
            "aaaaaa",
            "wwwwww",
            "bbbbbb",
            "cccccc",
            "tttttt",
            "yyyyyy"
    };

    private static final String[] EXPECTED_ORDER_NO_DUPLICATES_EXCLUDE_VOIDED = new String[] {
            "fffffff",
            "aaaaaa",
            "wwwwww",
            "tttttt",
            "yyyyyy"
    };

    // End Dataset 1.

    // Dataset 2: duplicate update times. Includes voided observations.
    private static final String DATASET_DUPLICATE_TIMESTAMPS =
            "org/projectbuendia/openmrs/include/obsDataSetWithDuplicateTimestamps.xml";

    private static final String[] EXPECTED_ORDER_DUPLICATES = new String[] {
            "fffffff",
            "aaaaaa",
            "bbbbbb",
            "wwwwww",
            "cccccc",
            "tttttt",
            "yyyyyy"
    };

    private static final String[] EXPECTED_ORDER_DUPLICATES_EXCLUDE_VOIDED_AFTER_bbbbbb =
            new String[] {
                "wwwwww",
                "tttttt",
                "yyyyyy"
            };


    private ProjectBuendiaService buendiaService;

    @Before
    public void setUpVariables() throws Exception {
        buendiaService = Context.getService(ProjectBuendiaService.class);
    }

    @Before
    public void setUpBaseData() throws Exception {
        executeDataSet(SAMPLE_PATIENT_DATA_SET);
    }

    // Dataset 1

    @Test
    public void testNoDuplicatesPaginatesCorrectly() throws Exception {
        executeDataSet(DATASET_NO_DUPLICATE_TIMESTAMPS);
        SyncPage<Obs> results =
                buendiaService.getObservationsModifiedAtOrAfter(null, true, 3);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_ORDER_NO_DUPLICATES_INCLUDE_VOIDED, 0, 3),
                extractListOfUuids(results.results));
        SyncToken token = results.syncToken;
        results = buendiaService.getObservationsModifiedAtOrAfter(token, true, 3);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_ORDER_NO_DUPLICATES_INCLUDE_VOIDED, 3, 6),
                extractListOfUuids(results.results));
        token = results.syncToken;
        results = buendiaService.getObservationsModifiedAtOrAfter(token, true, 3);
        assertArrayEquals(
                // There should only be one in the last page.
                Arrays.copyOfRange(EXPECTED_ORDER_NO_DUPLICATES_INCLUDE_VOIDED, 6, 7),
                extractListOfUuids(results.results));
    }

    @Test
    public void testIncludeVoidedFalseExcludesVoided() throws Exception {
        executeDataSet(DATASET_NO_DUPLICATE_TIMESTAMPS);
        SyncPage<Obs> actual =
                buendiaService.getObservationsModifiedAtOrAfter(CATCH_ALL_SYNCTOKEN, false, 0);
        Assert.assertArrayEquals(
                EXPECTED_ORDER_NO_DUPLICATES_EXCLUDE_VOIDED,
                extractListOfUuids(actual.results));
    }

    @Test
    public void testIncludedVoidedTrueIncludesVoided() throws Exception {
        executeDataSet(DATASET_NO_DUPLICATE_TIMESTAMPS);
        SyncPage<Obs> actual =
                buendiaService.getObservationsModifiedAtOrAfter(CATCH_ALL_SYNCTOKEN, true, 0);
        Assert.assertArrayEquals(
                EXPECTED_ORDER_NO_DUPLICATES_INCLUDE_VOIDED,
                extractListOfUuids(actual.results));
    }

    // End Dataset 1

    // Dataset 2

    @Test
    public void testPaginatesCorrectlyWithDuplicateTimestamps() throws Exception {
        executeDataSet(DATASET_DUPLICATE_TIMESTAMPS);
        SyncPage<Obs> results =
                buendiaService.getObservationsModifiedAtOrAfter(null, true, 3);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_ORDER_DUPLICATES, 0, 3),
                extractListOfUuids(results.results));
        SyncToken token = results.syncToken;
        results = buendiaService.getObservationsModifiedAtOrAfter(token, true, 3);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_ORDER_DUPLICATES, 3, 6),
                extractListOfUuids(results.results));
        token = results.syncToken;
        results = buendiaService.getObservationsModifiedAtOrAfter(token, true, 3);
        assertArrayEquals(
                // There should only be one in the last page.
                Arrays.copyOfRange(EXPECTED_ORDER_DUPLICATES, 6, 7),
                extractListOfUuids(results.results));
    }

    @Test
    public void testCorrectSyncTokenWhenSwitchingBetweenIncludeAndExcludedVoided()
            throws Exception {
        executeDataSet(DATASET_DUPLICATE_TIMESTAMPS);
        SyncPage<Obs> results =
                buendiaService.getObservationsModifiedAtOrAfter(null, true, 3);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_ORDER_DUPLICATES, 0, 3),
                extractListOfUuids(results.results));
        // We've now gone past one voided observations. There's one more voided. We test that if
        // we request with includeVoided = false, the correct results are still fetched - i.e. the
        // bookmark doesn't "shift" because of a parameter change.
        SyncToken token = results.syncToken;
        results = buendiaService.getObservationsModifiedAtOrAfter(token, false, 3);
        assertArrayEquals(
                EXPECTED_ORDER_DUPLICATES_EXCLUDE_VOIDED_AFTER_bbbbbb,
                extractListOfUuids(results.results));

    }

    // End Dataset 2

    // Dataset consistency tests

    @Test
    public void testDatasetNoDuplicatesIsConsistent() throws Exception {
        testDataSetIsConsistent(DATASET_NO_DUPLICATE_TIMESTAMPS);
    }

    @Test
    public void testDatasetWithDuplicatesIsConsistent() throws Exception {
        testDataSetIsConsistent(DATASET_DUPLICATE_TIMESTAMPS);
    }

    private void testDataSetIsConsistent(String dataset) throws Exception {
        testDataSetIsConsistent(dataset, "obs", "obs_id", "buendia_obs_sync_map", "obs_id");
    }
}
