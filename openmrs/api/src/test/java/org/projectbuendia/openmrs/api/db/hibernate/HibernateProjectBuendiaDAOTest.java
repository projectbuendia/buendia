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
import org.openmrs.BaseOpenmrsData;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.projectbuendia.openmrs.api.Bookmark;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Base class for tests which test {@link HibernateProjectBuendiaDAO}.
 */
@SkipBaseSetup
public abstract class HibernateProjectBuendiaDAOTest extends BaseModuleContextSensitiveTest {

    protected static final Bookmark CATCH_ALL = new Bookmark(new Date(0), null);
    protected static final DateFormat DB_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    protected static final String SAMPLE_PATIENT_DATA_SET =
            "org/projectbuendia/openmrs/include/samplePatientsDataSet.xml";

    private static final String BASE_DATASET =
            "org/projectbuendia/openmrs/include/baseMetaDataSet.xml";

    /**
     * {@link BaseModuleContextSensitiveTest} does this initialization, but also pre-loads the
     * database with a bunch of patient records. We don't want to load those patient records,
     * because we'd then have to augment them with `buendia_patient_sync_map` records, which would
     * couple our test integrity to the records in OpenMRS' test data. For this reason, we disable
     * {@link BaseModuleContextSensitiveTest}'s setup by putting the {@link SkipBaseSetup}
     * annotation on the class, but then we've got to explicitly init the database and authenticate
     * ourselves.
     */
    @Before
    public void setUpData() throws Exception {
        if (useInMemoryDatabase()) {
            initializeInMemoryDatabase();
            executeDataSet(BASE_DATASET);
            authenticate();
        }
    }

    /** Extracts a list of UUIDs from a list of OpenMRS data. */
    protected String[] extractListOfUuids(List<? extends BaseOpenmrsData> data) {
        String[] retVal = new String[data.size()];
        for (int i = 0; i < data.size(); i++) {
            retVal[i] = data.get(i).getUuid();
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
    protected static Bookmark createBookmark(String dateString, @Nullable String uuid) {
        try {
            return new Bookmark(DB_DATE_FORMAT.parse(dateString), uuid);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * It's possible to generate inconsistent test data that will cause the other tests to fail,
     * by using different UUIDs in the `person` table and the corresponding row in the
     * `buendia_patient_sync_map`. This test explicitly checks for that, and fails if it finds any
     * discrepancies. See `liquibase.xml` for why this inconsistency is possible in the data model.
     */
    protected void testDataSetIsConsistent(String dataset, String baseTableName, String baseIdField,
            String buendiaTableName, String buendiaIdField) throws Exception {
        executeDataSet(dataset);
        Statement statement = getConnection().createStatement();
        String query = String.format(
                "SELECT origin.%2$s, origin.uuid, buendia.uuid " +
                "FROM %1$s origin " +
                "LEFT JOIN %3$s buendia ON origin.%2$s = buendia.%4$s " +
                "WHERE origin.uuid <> buendia.uuid",
                baseTableName, baseIdField, buendiaTableName, buendiaIdField);
        ResultSet results  = statement.executeQuery(query);
        int failures = 0;
        while (results.next()) {
            failures++;
            System.out.printf("WARNING: Record with ID #%d has inconsistent entry in " +
                    "sync map.\nBase UUID: %s Sync Map UUID: %s\n",
                    results.getInt(1), results.getString(2), results.getString(3));
        }
        if (failures > 0) {
            fail(String.format("%d record(s) had inconsistent test data.", failures));
        }
    }
}
