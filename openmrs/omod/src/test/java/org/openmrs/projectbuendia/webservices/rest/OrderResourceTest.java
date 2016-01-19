/*
 * Copyright 2016 The Project Buendia Authors
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

package org.openmrs.projectbuendia.webservices.rest;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link OrderResource}.
 *
 * This test class is unusual in that it has two purposes:
 * - it incorporates some smoke tests for querying as a result of inheriting from
 *   {@link MainResourceControllerTest}. We don't bother testing any further here; our main query
 *   tests are in {@code HibernateProjectBuendiaDAOTest} and subclasses.
 * - Testing that updates, deletions and revisions work correctly.
 */
@SkipBaseSetup
public class OrderResourceTest extends MainResourceControllerTest {

    private static final String BASE_DATASET =
            "org/openmrs/projectbuendia/webservices/rest/baseMetaDataSet.xml";
    private static final String BASE_ORDER_DATASET =
            "org/openmrs/projectbuendia/webservices/rest/order-test-base-data.xml";
    private static final String TEST_DATASET =
            "org/openmrs/projectbuendia/webservices/rest/single-order.xml";

    private static final String BASE_URL = "/projectbuendia/orders";
    private static final String ENCOUNTERS_URL = "/projectbuendia/encounters";

    private static final long ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final long ONE_WEEK_IN_MILLIS = ONE_DAY_IN_MILLIS * 7;

    private static final String SAMPLE_PATIENT_UUID = "5946f880-b197-400b-9caa-a3c661d23041";
    private static final String SAMPLE_INSTRUCTIONS = "Paracetamol 1000mg 4x daily";
    private static final long SAMPLE_START_DATE = 1420602264000L;
    private static final long SAMPLE_END_DATE = SAMPLE_START_DATE + ONE_WEEK_IN_MILLIS;
    private OrderService orderService;

    /**
     * {@link BaseModuleContextSensitiveTest} does this initialization, but also pre-loads the
     * database with a bunch of records. We don't want to load those records,
     * because we'd then have to augment them with `buendia_[type]_sync_map` records, which would
     * couple our test integrity to the records in OpenMRS' test data. For this reason, we disable
     * {@link BaseModuleContextSensitiveTest}'s setup by putting the {@link SkipBaseSetup}
     * annotation on the class, but then we've got to explicitly init the database and authenticate
     * ourselves.
     */
    @Before
    public void setUp() throws Exception {
        orderService = Context.getOrderService();
        if (useInMemoryDatabase()) {
            initializeInMemoryDatabase();
            authenticate();
        }
        executeDataSet(BASE_DATASET);
        executeDataSet(BASE_ORDER_DATASET);
        executeDataSet(TEST_DATASET);
    }

    @Override
    public String getURI() {
        return "projectbuendia/orders";
    }

    @Override
    public String getUuid() {
        // From the dataset file.
        return "aaaaa";
    }

    @Override
    public long getAllCount() {
        // From the dataset file.
        return 1;
    }

    @Test
    public void testOrderCreationWithAllDataPopulated() throws Exception {
        SimpleObject input = newOrderJson(
                SAMPLE_PATIENT_UUID,
                SAMPLE_INSTRUCTIONS,
                SAMPLE_START_DATE,
                SAMPLE_END_DATE);
        MockHttpServletRequest request = newPostRequest(BASE_URL, input);
        SimpleObject response = deserialize(handle(request));

        String uuid = (String) response.get(OrderResource.UUID);

        // Check that fields are correctly set in response
        assertEquals(SAMPLE_PATIENT_UUID, response.get(OrderResource.PATIENT_UUID));
        assertEquals(SAMPLE_INSTRUCTIONS, response.get(OrderResource.INSTRUCTIONS));
        assertEquals(SAMPLE_START_DATE, response.get(OrderResource.START_MILLIS));
        assertEquals(SAMPLE_END_DATE, response.get(OrderResource.STOP_MILLIS));

        // Check that these fields match the object stored.
        Order stored = orderService.getOrderByUuid(uuid);
        assertEquals(SAMPLE_PATIENT_UUID, stored.getPatient().getUuid());
        assertEquals(SAMPLE_INSTRUCTIONS, stored.getInstructions());
        assertEquals(SAMPLE_START_DATE, stored.getScheduledDate().getTime());
        assertEquals(SAMPLE_END_DATE, stored.getAutoExpireDate().getTime());
    }

    @Test
    public void testOrderCreationWithoutPatientThrowsException() throws Exception {
        SimpleObject input = newOrderJson(
                null,
                SAMPLE_INSTRUCTIONS,
                SAMPLE_START_DATE,
                SAMPLE_END_DATE);
        MockHttpServletRequest request = newPostRequest(BASE_URL, input);
        try {
            handle(request);
            fail("Expected handling this request to throw an exception");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testOrderCreationWithoutStartDateReturnsThrowsException() throws Exception {
        SimpleObject input = newOrderJson(
                SAMPLE_PATIENT_UUID,
                SAMPLE_INSTRUCTIONS,
                null,
                SAMPLE_END_DATE);
        MockHttpServletRequest request = newPostRequest(BASE_URL, input);
        try {
            handle(request);
            fail("Expected handling this request to throw an exception");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testOrderCreationWithoutEndDateIsAccepted() throws Exception {
        SimpleObject input = newOrderJson(
                SAMPLE_PATIENT_UUID,
                SAMPLE_INSTRUCTIONS,
                SAMPLE_START_DATE,
                null);
        MockHttpServletRequest request = newPostRequest(BASE_URL, input);
        SimpleObject response = deserialize(handle(request));

        String uuid = (String) response.get(OrderResource.UUID);

        // Check that fields are correctly set in response
        assertEquals(SAMPLE_PATIENT_UUID, response.get(OrderResource.PATIENT_UUID));
        assertEquals(SAMPLE_INSTRUCTIONS, response.get(OrderResource.INSTRUCTIONS));
        assertEquals(SAMPLE_START_DATE, response.get(OrderResource.START_MILLIS));
        assertTrue("Response contains stop_millis, even if it's not set",
                response.containsKey(OrderResource.STOP_MILLIS));
        assertNull(response.get(OrderResource.STOP_MILLIS));

        // Check that these fields match the object stored.
        Order stored = orderService.getOrderByUuid(uuid);
        assertEquals(SAMPLE_PATIENT_UUID, stored.getPatient().getUuid());
        assertEquals(SAMPLE_INSTRUCTIONS, stored.getInstructions());
        assertEquals(SAMPLE_START_DATE, stored.getScheduledDate().getTime());
        assertNull(stored.getAutoExpireDate());
    }

    @Test
    public void testUpdateForOrderOlderThan24HrsIsARevision() throws Exception {
        String newInstructions = "Some instructions?";
        Order baseOrder = createExpiredOrderOlderThan24Hrs();
        SimpleObject newDetails = newOrderJson(null, newInstructions, null, null);
        MockHttpServletRequest request =
                newPostRequest(BASE_URL + "/" + baseOrder.getUuid(), newDetails);
        SimpleObject response = deserialize(handle(request));

        String uuid = (String) response.get(OrderResource.UUID);

        // Check that it's returning the same UUID
        assertEquals(baseOrder.getUuid(), uuid);

        // Check that fields are correctly set in response
        assertEquals(SAMPLE_PATIENT_UUID, response.get(OrderResource.PATIENT_UUID));
        assertEquals(newInstructions, response.get(OrderResource.INSTRUCTIONS));
        assertEquals(SAMPLE_START_DATE, response.get(OrderResource.START_MILLIS));
        assertEquals(SAMPLE_END_DATE, response.get(OrderResource.STOP_MILLIS));

        // Check that the underlying order is a different one and has the correct values.
        Order stored = OrderResource.getLatestVersion(orderService.getOrderByUuid(uuid));
        assertEquals(SAMPLE_PATIENT_UUID, stored.getPatient().getUuid());
        assertEquals(newInstructions, stored.getInstructions());
        assertEquals(SAMPLE_START_DATE, stored.getScheduledDate().getTime());
        assertEquals(SAMPLE_END_DATE, stored.getAutoExpireDate().getTime());
    }

    @Test
    public void testUpdateForExecutedOrderIsARevision() throws Exception {
        Order baseOrder = createOrderStartingNow();
        executeOrder(baseOrder);
        long startTime = baseOrder.getScheduledDate().getTime();

        long newEndTime = System.currentTimeMillis() + 2 * ONE_WEEK_IN_MILLIS;

        SimpleObject newDetails = newOrderJson(null, null, null, newEndTime);
        MockHttpServletRequest request =
                newPostRequest(BASE_URL + "/" + baseOrder.getUuid(), newDetails);

        SimpleObject response = deserialize(handle(request));

        String uuid = (String) response.get(OrderResource.UUID);

        // The client should get the same UUID, but in storage, it should be a different order.
        assertEquals(baseOrder.getUuid(), uuid);

        // Check that fields are correctly set in response
        assertEquals(SAMPLE_PATIENT_UUID, response.get(OrderResource.PATIENT_UUID));
        assertEquals(SAMPLE_INSTRUCTIONS, response.get(OrderResource.INSTRUCTIONS));
        assertEquals(startTime, response.get(OrderResource.START_MILLIS));
        assertEquals(newEndTime, response.get(OrderResource.STOP_MILLIS));

        // Check that these fields match the object stored.
        Order stored = orderService.getRevisionOrder(orderService.getOrderByUuid(uuid));
        assertEquals(SAMPLE_PATIENT_UUID, stored.getPatient().getUuid());
        assertEquals(SAMPLE_INSTRUCTIONS, stored.getInstructions());
        assertEquals(startTime, stored.getScheduledDate().getTime());
        assertEquals(newEndTime, stored.getAutoExpireDate().getTime());
    }

    @Test
    public void testDeleteForNewNonExecutedOrderVoids() throws Exception {
        Order order = createOrderStartingNow();
        String uuid = order.getUuid();
        MockHttpServletRequest request =
                newDeleteRequest(BASE_URL + "/" + uuid);
        handle(request);
        assertTrue("Order is voided", orderService.getOrderByUuid(uuid).isVoided());
    }

    @Test
    public void testDeleteForExecutedOrderVoids() throws Exception {
        Order baseOrder = createOrderStartingNow();
        executeOrder(baseOrder);
        String uuid = baseOrder.getUuid();
        MockHttpServletRequest request =
                newDeleteRequest(BASE_URL + "/" + uuid);
        handle(request);
        OrderService service = orderService;
        Order order = service.getOrderByUuid(uuid);
        assertTrue("Order is voided", order.isVoided());
    }

    @Test
    public void testDeleteForRevisedOrderVoidsAll() throws Exception {
        Order baseOrder = createOrderStartingNow();
        String baseUuid = baseOrder.getUuid();

        SimpleObject newDetails = newOrderJson(null, "New instructions!", null, null);
        MockHttpServletRequest request =
                newPostRequest(BASE_URL + "/" + baseUuid, newDetails);

        // Make the update.
        handle(request);

        request = newDeleteRequest(BASE_URL + "/" + baseUuid);
        handle(request);
        // Reload the base order from storage
        baseOrder = orderService.getOrderByUuid(baseUuid);
        assertTrue("Base order is voided", baseOrder.isVoided());
        Order revisionOrder = OrderResource.getLatestVersion(baseOrder);
        assertNotNull("Expected a non-null revision order", revisionOrder);
        assertTrue("Revision order is voided", revisionOrder.isVoided());
    }


    private Order createExpiredOrderOlderThan24Hrs() throws Exception {
        SimpleObject input = newOrderJson(
                SAMPLE_PATIENT_UUID, SAMPLE_INSTRUCTIONS, SAMPLE_START_DATE, SAMPLE_END_DATE);
        SimpleObject response = deserialize(handle(newPostRequest(BASE_URL, input)));
        String uuid = (String) response.get(OrderResource.UUID);
        return orderService.getOrderByUuid(uuid);
    }

    private Order createOrderStartingNow() throws Exception {
        long now = System.currentTimeMillis();
        SimpleObject input = newOrderJson(
                SAMPLE_PATIENT_UUID, SAMPLE_INSTRUCTIONS, now, now + ONE_WEEK_IN_MILLIS);
        SimpleObject response = deserialize(handle(newPostRequest(BASE_URL, input)));
        String uuid = (String) response.get(OrderResource.UUID);
        return orderService.getOrderByUuid(uuid);
    }

    private void executeOrder(Order order) throws Exception {
        SimpleObject input = new SimpleObject()
                .add("uuid", order.getPatient().getUuid())
                .add("order_uuids", new String[]{order.getUuid()});
        handle(newPostRequest(ENCOUNTERS_URL, input));
    }

    // Other test cases:
    // - Delete, more than 24 hrs old
    // - Delete, executed
    // DONE
    // - Update, more than 24 hrs old
    // - Update, executed
    // - Update, less than 24 hrs, not executed
    // - Delete less than 24 hrs, not executed
    // - Delete, order has expired

    private static SimpleObject newOrderJson(
            @Nullable String patientUuid, @Nullable String instructions,
            @Nullable Long startMillis, @Nullable Long stopMillis) {
        SimpleObject order = new SimpleObject();
        if (patientUuid != null) {
            order.add(OrderResource.PATIENT_UUID, patientUuid);
        }
        if (instructions != null) {
            order.add(OrderResource.INSTRUCTIONS, instructions);
        }
        if (startMillis != null) {
            order.add(OrderResource.START_MILLIS, startMillis);
        }
        if (stopMillis != null) {
            order.add(OrderResource.STOP_MILLIS, stopMillis);
        }
        return order;
    }
}
