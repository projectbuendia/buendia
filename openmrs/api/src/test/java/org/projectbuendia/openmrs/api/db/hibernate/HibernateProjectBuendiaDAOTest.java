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
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.SyncPage;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests in this class test the DAO logic, even though they interact with the service.
 */
@SkipBaseSetup
public class HibernateProjectBuendiaDAOTest extends BaseModuleContextSensitiveTest {

    private ProjectBuendiaService buendiaService;

    private static final SyncToken CATCH_ALL_SYNCTOKEN = new SyncToken(new Date(0), null);
    private static final DateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

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

    /**
     * {@link BaseModuleContextSensitiveTest} does this initialization, but also pre-loads the
     * database with a bunch of patient records. We don't want to load those patient records,
     * because we'd then have to augment them with `buendia_patient_sync_map` records, which would couple
     * our test integrity to the records in OpenMRS' test data. For this reason, we disable
     * {@link BaseModuleContextSensitiveTest}'s setup by putting the {@link SkipBaseSetup}
     * annotation on the class, but then we've got to explicitly init the database and authenticate
     * ourselves.
     */
    @Before
    public void setUpData() throws Exception {
        if (useInMemoryDatabase()) {
            initializeInMemoryDatabase();
            authenticate();
        }
    }

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

    /**
     * It's possible to generate inconsistent test data that will cause the other tests to fail,
     * by using different UUIDs in the `person` table and the corresponding row in the
     * `buendia_patient_sync_map`. This test explicitly checks for that, and fails if it finds any
     * discrepancies. See `liquibase.xml` for why this inconsistency is possible in the data model.
     */
    private void testDataSetIsConsistent(String dataset) throws Exception {
        executeDataSet(dataset);
        Statement statement = getConnection().createStatement();
        String query =
                "SELECT p.person_id, p.uuid, sm.uuid " +
                "FROM person p LEFT JOIN buendia_patient_sync_map sm ON p.person_id = sm.patient_id " +
                "WHERE p.uuid <> sm.uuid";
        ResultSet results  = statement.executeQuery(query);
        int failures = 0;
        while (results.next()) {
            failures++;
            System.out.printf(
                    "WARNING: Person with ID #%d has inconsistent entry in buendia_patient_sync_map.\n" +
                            "Person UUID: %s Sync Map UUID: %s\n",
                    results.getInt(1), results.getString(2), results.getString(3));
        }
        if (failures > 0) {
            fail(String.format("%d patient(s) had inconsistent test data.", failures));
        }
    }

    /** Extracts a list of UUIDs from a list of patients. */
    private String[] extractListOfUuids(List<Patient> patients) {
        String[] retVal = new String[patients.size()];
        for (int i = 0; i < patients.size(); i++) {
            retVal[i] = patients.get(i).getUuid();
        }
        return retVal;
    }

    /**
     * Creates a sync token, and converts any thrown exceptions to RuntimeExceptions so it can be
     * used for static fields.
     *
     * @param dateString Specified in the same format as in the dataset XML files, for readability.
     *                   e.g. "2015-07-18 12:00:00.0"
     */
    private static SyncToken createSyncToken(String dateString, @Nullable String uuid) {
        try {
            return new SyncToken(DB_DATE_FORMAT.parse(dateString), uuid);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}