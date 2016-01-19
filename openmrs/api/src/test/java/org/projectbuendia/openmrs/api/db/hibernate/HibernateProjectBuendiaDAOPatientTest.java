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

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.SyncPage;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the patient-handling logic in {@link HibernateProjectBuendiaDAO}. Tests in this class test
 * the DAO logic, even though they interact with the service.
 */
public class HibernateProjectBuendiaDAOPatientTest extends HibernateProjectBuendiaDAOTest {

    private ProjectBuendiaService buendiaService;

    // Dataset 1: no voided patients, no duplicate timestamps.

    private static final String PATIENT_DATASET_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS =
            "org/projectbuendia/openmrs/include/patientDataSetNoDuplicateTimestamps.xml";

    private static final String[] EXPECTED_UUID_ORDER_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS =
            new String[] {
                "aaaaa",
                "eeeee",
                "bbbbb",
                "fffff",
                "ddddd",
                "ccccc",
                "gggggg"
            };

    private static final SyncToken SYNC_TOKEN_FOR_RECORD_eeeee =
            createSyncToken("2015-07-18 12:00:00.0", "eeeee");

    // END Dataset 1.

    // Dataset 2: voided patients, no duplicate timestamps
    private static final String PATIENT_DATASET_INCLUDES_VOIDED =
            "org/projectbuendia/openmrs/include/patientDataSetWithVoids.xml";

    private static final String[] EXPECTED_UUID_ORDER_VOIDS_EXCLUDE_VOIDED =
            new String[] {
                    "aaaaa",
                    "bbbbb",
                    "fffff",
                    "gggggg"
            };

    private static final String[] EXPECTED_UUID_ORDER_VOIDS_INCLUDE_VOIDED =
            EXPECTED_UUID_ORDER_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS;

    // END Dataset 2.

    // Dataset 3: voided patients, duplicate timestamps.
    private static final String PATIENT_DATASET_DUPLICATE_TIMESTAMPS =
            "org/projectbuendia/openmrs/include/patientDataSetWithDuplicateTimestamps.xml";

    private static final String[] EXPECTED_UUID_ORDER_DUPLICATE_TIMESTAMPS =
            new String[] {
                    "aaaaa",
                    "bbbbb",
                    "eeeee",
                    "fffff",
                    "ccccc",
                    "ddddd",
                    "gggggg"
            };

    private static final SyncToken SYNC_TOKEN_DATE_ONLY =
            createSyncToken("2015-07-21 00:00:00.0", null);

    // END Dataset 3.

    @Before
    public void setUpVariables() throws Exception {
        buendiaService = Context.getService(ProjectBuendiaService.class);
    }

    @Test
    public void testResultsInOrderWhenAllHaveDifferentUpdateTimes() throws Exception {
        executeDataSet(PATIENT_DATASET_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS);
        SyncPage<Patient> results =
                buendiaService.getPatientsModifiedAtOrAfter(null, false, 0);
        String[] actual = extractListOfUuids(results.results);
        assertArrayEquals(EXPECTED_UUID_ORDER_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS, actual);
    }

    @Test
    public void testPagesContainCorrectRecordsWhenAllHaveDifferentUpdateTimes() throws Exception {
        executeDataSet(PATIENT_DATASET_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS);
        SyncPage<Patient> results =
                buendiaService.getPatientsModifiedAtOrAfter(null, false, 3);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_UUID_ORDER_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS, 0, 3),
                extractListOfUuids(results.results));
        SyncToken token = results.syncToken;
        results = buendiaService.getPatientsModifiedAtOrAfter(token, false, 3);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_UUID_ORDER_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS, 3, 6),
                extractListOfUuids(results.results));
        token = results.syncToken;
        results = buendiaService.getPatientsModifiedAtOrAfter(token, false, 3);
        assertArrayEquals(
                // There should only be one in the last page.
                Arrays.copyOfRange(EXPECTED_UUID_ORDER_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS, 6, 7),
                extractListOfUuids(results.results));
    }

    @Test
    public void testSyncTokenGeneratedFromLastResultInPage() throws Exception {
        executeDataSet(PATIENT_DATASET_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS);
        SyncToken token = buendiaService.getPatientsModifiedAtOrAfter(null, false, 2).syncToken;
        assertEquals(SYNC_TOKEN_FOR_RECORD_eeeee, token);
    }

    @Test
    public void testFetchPastEndOfResultSet() throws Exception {
        executeDataSet(PATIENT_DATASET_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS);
        // First, fetch all the data.
        SyncToken token = buendiaService.getPatientsModifiedAtOrAfter(null, false, 0).syncToken;
        // The sync token should be generated from the last result, so using it should return
        // an empty result set.
        SyncPage<Patient> result = buendiaService.getPatientsModifiedAtOrAfter(token, false, 0);
        assertTrue("Expected empty results", result.results.isEmpty());
        assertNull(result.syncToken);
    }

    @Test
    public void testZeroMaxResultsReturnsLotsOfResults() throws Exception {
        executeDataSet(PATIENT_DATASET_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS);
        SyncPage<Patient> results = buendiaService.getPatientsModifiedAtOrAfter(null, false, 0);
        assertNotEquals(0, results.results.size());
        results = buendiaService.getPatientsModifiedAtOrAfter(CATCH_ALL_SYNCTOKEN, false, 0);
        assertNotEquals(0, results.results.size());
    }

    @Test
    public void testVoidedPatientsIncludedWhenParameterTrue() throws Exception {
        executeDataSet(PATIENT_DATASET_INCLUDES_VOIDED);
        SyncPage<Patient> results = buendiaService.getPatientsModifiedAtOrAfter(null, true, 0);
        assertArrayEquals(
                EXPECTED_UUID_ORDER_VOIDS_INCLUDE_VOIDED,
                extractListOfUuids(results.results));
    }

    @Test
    public void testVoidedPatientsExcludedWhenParameterFalse() throws Exception {
        executeDataSet(PATIENT_DATASET_INCLUDES_VOIDED);
        SyncPage<Patient> results = buendiaService.getPatientsModifiedAtOrAfter(null, false, 0);
        assertArrayEquals(
                EXPECTED_UUID_ORDER_VOIDS_EXCLUDE_VOIDED,
                extractListOfUuids(results.results));
    }


    @Test
    public void testPagesContainCorrectRecordsWhenSomeHaveSameUpdateTime() throws Exception {
        executeDataSet(PATIENT_DATASET_DUPLICATE_TIMESTAMPS);
        SyncPage<Patient> results =
                buendiaService.getPatientsModifiedAtOrAfter(null, true, 3);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_UUID_ORDER_DUPLICATE_TIMESTAMPS, 0, 3),
                extractListOfUuids(results.results));
        SyncToken token = results.syncToken;
        results = buendiaService.getPatientsModifiedAtOrAfter(token, true, 3);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_UUID_ORDER_DUPLICATE_TIMESTAMPS, 3, 6),
                extractListOfUuids(results.results));
        token = results.syncToken;
        results = buendiaService.getPatientsModifiedAtOrAfter(token, true, 3);
        assertArrayEquals(
                // There should only be one in the last page.
                Arrays.copyOfRange(EXPECTED_UUID_ORDER_DUPLICATE_TIMESTAMPS, 6, 7),
                extractListOfUuids(results.results));
    }

    @Test
    public void testNullUuidInSyncTokenReturnsAllResultsNewerThanTimestamp() throws Exception {
        executeDataSet(PATIENT_DATASET_DUPLICATE_TIMESTAMPS);
        List<Patient> results =
                buendiaService.getPatientsModifiedAtOrAfter(SYNC_TOKEN_DATE_ONLY, true, 0).results;
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_UUID_ORDER_DUPLICATE_TIMESTAMPS, 4, 7),
                extractListOfUuids(results));
    }

    @Test
    public void testFetchWithNoResults() throws Exception {
        // Don't add any data.
        SyncPage<Patient> result = buendiaService.getPatientsModifiedAtOrAfter(null, false, 0);
        assertTrue("Expected empty results", result.results.isEmpty());
        assertNull(result.syncToken);
    }

    // DATASET CONSISTENCY TESTS. See note on {@link #testDataSetIsConsistent}.

    @Test
    public void testDataSetNoVoidedNoDuplicateTimestampsIsConsistent() throws Exception {
        testDataSetIsConsistent(PATIENT_DATASET_NO_VOIDED_NO_DUPLICATE_TIMESTAMPS);
    }

    @Test
    public void testDataSetWithVoidsIsConsistent() throws Exception {
        testDataSetIsConsistent(PATIENT_DATASET_INCLUDES_VOIDED);
    }

    @Test
    public void testDataSetWithDuplicateTimestampsisConsistent() throws Exception {
        testDataSetIsConsistent(PATIENT_DATASET_DUPLICATE_TIMESTAMPS);
    }

    private void testDataSetIsConsistent(String dataset) throws Exception {
        testDataSetIsConsistent(
                dataset, "person", "person_id", "buendia_patient_sync_map", "patient_id");
    }
}