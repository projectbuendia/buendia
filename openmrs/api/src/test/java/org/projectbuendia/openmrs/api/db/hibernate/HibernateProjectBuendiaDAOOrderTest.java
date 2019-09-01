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
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.api.Bookmark;
import org.projectbuendia.openmrs.api.db.SyncPage;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

/**
 * Test the order-handling logic in {@link HibernateProjectBuendiaDAO}. Tests in this class test
 * the DAO logic, even though they interact with the service.
 */
public class HibernateProjectBuendiaDAOOrderTest extends HibernateProjectBuendiaDAOTest {

    private static final String TEST_DATASET =
            "org/projectbuendia/openmrs/include/orderDataSet.xml";

    private static final String[] EXPECTED_ORDER_INCLUDES_VOIDED = new String[] {
            "aaaaa",
            "ttttt",
            "uuuuu",
            "eeeee",
            "ppppp",
            "wwwww",
            "hhhhh",
            "kkkkk"
    };

    private static final String[] EXPECTED_ORDER_EXCLUDES_VOIDED = new String[] {
            "aaaaa",
            "ttttt",
            "uuuuu",
            "ppppp",
            "wwwww",
            "hhhhh"
    };

    private static final String[] EXPECTED_ORDER_ONLY_NEW_AND_RENEW_ACTIONS = new String[] {
            "aaaaa",
            "uuuuu",
            "ppppp",
            "wwwww",
            "kkkkk"
    };

    private ProjectBuendiaService buendiaService;

    @Before
    public void setUp() throws Exception {
        buendiaService = Context.getService(ProjectBuendiaService.class);
        executeDataSet(SAMPLE_PATIENT_DATA_SET);
        executeDataSet(TEST_DATASET);
    }

    @Test
    public void testIncludeVoidedFalseExcludesVoided() throws Exception {
        SyncPage<Order> actual = buendiaService.getOrdersModifiedAtOrAfter(
            CATCH_ALL, false, 0, null);
        assertArrayEquals(
                EXPECTED_ORDER_EXCLUDES_VOIDED,
                extractListOfUuids(actual.results));
    }

    @Test
    public void testIncludedVoidedTrueIncludesVoided() throws Exception {
        SyncPage<Order> actual = buendiaService.getOrdersModifiedAtOrAfter(
            CATCH_ALL, true, 0, null);
        assertArrayEquals(
                EXPECTED_ORDER_INCLUDES_VOIDED,
                extractListOfUuids(actual.results));
    }

    @Test
    public void testPaginatesCorrectly() throws Exception {
        SyncPage<Order> results =
                buendiaService.getOrdersModifiedAtOrAfter(null, true, 3, null);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_ORDER_INCLUDES_VOIDED, 0, 3),
                extractListOfUuids(results.results));
        Bookmark token = results.bookmark;
        results = buendiaService.getOrdersModifiedAtOrAfter(token, true, 3, null);
        assertArrayEquals(
                Arrays.copyOfRange(EXPECTED_ORDER_INCLUDES_VOIDED, 3, 6),
                extractListOfUuids(results.results));
        token = results.bookmark;
        results = buendiaService.getOrdersModifiedAtOrAfter(token, true, 3, null);
        assertArrayEquals(
                // There should only be two in the last page.
                Arrays.copyOfRange(EXPECTED_ORDER_INCLUDES_VOIDED, 6, 8),
                extractListOfUuids(results.results));
    }

    @Test
    public void testExcludesCorrectly() throws Exception {
        SyncPage<Order> results = buendiaService.getOrdersModifiedAtOrAfter(
                null,   // no sync token
                true,   // include voided
                0,      // all results
                // Only two actions - new and renew.
                new Order.Action[] {Order.Action.NEW, Order.Action.RENEW});
        assertArrayEquals(
                EXPECTED_ORDER_ONLY_NEW_AND_RENEW_ACTIONS,
                extractListOfUuids(results.results));
    }

    @Test
    public void testDatasetIsConsistent() throws Exception {
        testDataSetIsConsistent(
                TEST_DATASET, "orders", "order_id", "buendia_order_sync_map", "order_id");
    }
}
