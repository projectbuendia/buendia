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

import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.openmrs.test.SkipBaseSetup;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** REST API tests for OrderResource */
@SkipBaseSetup public class OrderResourceTest extends BaseApiRequestTest {
    private static final String ENCOUNTERS_URL = "/projectbuendia/encounters";

    private static final long ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final long ONE_WEEK_IN_MILLIS = ONE_DAY_IN_MILLIS * 7;

    private static final String SAMPLE_PROVIDER_UUID = "c2299800-cca9-11e0-9572-0800200c9a66";
    private static final String SAMPLE_PATIENT_UUID = "5946f880-b197-400b-9caa-a3c661d23041";
    private static final String SAMPLE_INSTRUCTIONS = "Paracetamol 1000mg 4x daily";
    private static final long SAMPLE_START_DATE = 1420602264000L;
    private static final long SAMPLE_END_DATE = SAMPLE_START_DATE + ONE_WEEK_IN_MILLIS;

    @Override public String[] getInitialDataFiles() {
        return new String[] {
            "org/openmrs/projectbuendia/webservices/rest/base-test-data.xml",
            "org/openmrs/projectbuendia/webservices/rest/order-test-data.xml",
            "org/openmrs/projectbuendia/webservices/rest/single-order.xml"
        };
    };

    @Override public String getURI() {
        return "/projectbuendia/orders";
    }

    @Override public long getAllCount() {
        return 1; // orders in the dataset file
    }

    @Override public String getUuid() {
        return "aaaaa"; // from the dataset file
    }

    @Test public void testOrderCreationWithAllDataPopulated() throws Exception {
        SimpleObject input = newOrderJson(
            SAMPLE_PATIENT_UUID, SAMPLE_PROVIDER_UUID, SAMPLE_INSTRUCTIONS,
            SAMPLE_START_DATE, SAMPLE_END_DATE);
        SimpleObject response = deserialize(handle(newPostRequest(getURI(), input)));

        // Check that fields are correctly set in response
        assertEquals(SAMPLE_PATIENT_UUID, response.get("patient_uuid"));
        assertEquals(SAMPLE_INSTRUCTIONS, response.get("instructions"));
        assertEquals(SAMPLE_START_DATE, response.get("start_millis"));
        assertEquals(SAMPLE_END_DATE, response.get("stop_millis"));

        // Check that these fields match the object stored.
        String uuid = (String) response.get("uuid");
        Order stored = orderService.getOrderByUuid(uuid);
        assertEquals(SAMPLE_PATIENT_UUID, stored.getPatient().getUuid());
        assertEquals(SAMPLE_INSTRUCTIONS, stored.getInstructions());
        assertEquals(SAMPLE_START_DATE, stored.getScheduledDate().getTime());
        assertEquals(SAMPLE_END_DATE, stored.getAutoExpireDate().getTime());
    }

    @Test public void testOrderCreationWithoutPatientThrowsException() throws Exception {
        SimpleObject input = newOrderJson(
            null, SAMPLE_PROVIDER_UUID, SAMPLE_INSTRUCTIONS,
            SAMPLE_START_DATE, SAMPLE_END_DATE);
        assertExceptionOnRequest(newPostRequest(getURI(), input), "missing patient");
    }

    @Test public void testOrderCreationWithoutProviderThrowsException() throws Exception {
        SimpleObject input = newOrderJson(
            SAMPLE_PATIENT_UUID, null, SAMPLE_INSTRUCTIONS,
            SAMPLE_START_DATE, SAMPLE_END_DATE);
        assertExceptionOnRequest(newPostRequest(getURI(), input), "missing provider");
    }


    @Test public void testOrderCreationWithoutStartDateThrowsException() throws Exception {
        SimpleObject input = newOrderJson(
            SAMPLE_PATIENT_UUID, SAMPLE_PROVIDER_UUID, SAMPLE_INSTRUCTIONS,
            null, SAMPLE_END_DATE);
        assertExceptionOnRequest(newPostRequest(getURI(), input), "missing start date");
    }

    @Test
    public void testOrderCreationWithoutEndDateIsAccepted() throws Exception {
        SimpleObject input = newOrderJson(
            SAMPLE_PATIENT_UUID, SAMPLE_PROVIDER_UUID, SAMPLE_INSTRUCTIONS,
            SAMPLE_START_DATE, null);
        SimpleObject response = deserialize(handle(newPostRequest(getURI(), input)));

        String uuid = (String) response.get("uuid");

        // Check that fields are correctly set in response
        assertEquals(SAMPLE_PATIENT_UUID, response.get("patient_uuid"));
        assertEquals(SAMPLE_INSTRUCTIONS, response.get("instructions"));
        assertEquals(SAMPLE_START_DATE, response.get("start_millis"));
        assertNull(response.get("stop_millis"));

        // Check that these fields match the object stored.
        Order stored = orderService.getOrderByUuid(uuid);
        assertEquals(SAMPLE_PATIENT_UUID, stored.getPatient().getUuid());
        assertEquals(SAMPLE_INSTRUCTIONS, stored.getInstructions());
        assertEquals(SAMPLE_START_DATE, stored.getScheduledDate().getTime());
        assertNull(stored.getAutoExpireDate());
    }

    @Test public void testOrderUpdateWithoutProviderThrowsException() throws Exception {
        Order baseOrder = createOrderStartingNow();

        long startTime = baseOrder.getScheduledDate().getTime();
        long newEndTime = System.currentTimeMillis() + 2 * ONE_WEEK_IN_MILLIS;

        SimpleObject newDetails = newOrderJson(
            null, null, null, null, newEndTime);
        assertExceptionOnRequest(newPostRequest(getURI() + "/" + baseOrder.getUuid(), newDetails), "missing provider");
    }

    @Test public void testUpdateForOrderOlderThan24HrsIsARevision() throws Exception {
        String newInstructions = "Some instructions?";
        Order baseOrder = createExpiredOrderOlderThan24Hrs();
        SimpleObject newDetails = newOrderJson(
            null, SAMPLE_PROVIDER_UUID, newInstructions, null, null);
        MockHttpServletRequest request =
            newPostRequest(getURI() + "/" + baseOrder.getUuid(), newDetails);
        SimpleObject response = deserialize(handle(request));

        String uuid = (String) response.get("uuid");

        // Check that it's returning the same UUID
        assertEquals(baseOrder.getUuid(), uuid);

        // Check that fields are correctly set in response
        assertEquals(SAMPLE_PATIENT_UUID, response.get("patient_uuid"));
        assertEquals(newInstructions, response.get("instructions"));
        assertEquals(SAMPLE_START_DATE, response.get("start_millis"));
        assertEquals(SAMPLE_END_DATE, response.get("stop_millis"));

        // Check that the underlying order is a different one and has the correct values.
        Order stored = OrderRestResource.getLastRevision(orderService.getOrderByUuid(uuid));
        assertEquals(SAMPLE_PATIENT_UUID, stored.getPatient().getUuid());
        assertEquals(newInstructions, stored.getInstructions());
        assertEquals(SAMPLE_START_DATE, stored.getScheduledDate().getTime());
        assertEquals(SAMPLE_END_DATE, stored.getAutoExpireDate().getTime());
    }

    @Test public void testUpdateForExecutedOrderIsARevision() throws Exception {
        Order baseOrder = createOrderStartingNow();
        executeOrder(baseOrder);
        long startTime = baseOrder.getScheduledDate().getTime();
        long newEndTime = System.currentTimeMillis() + 2 * ONE_WEEK_IN_MILLIS;

        SimpleObject newDetails = newOrderJson(
            null, SAMPLE_PROVIDER_UUID, null, null, newEndTime);
        MockHttpServletRequest request =
            newPostRequest(getURI() + "/" + baseOrder.getUuid(), newDetails);
        SimpleObject response = deserialize(handle(request));

        // The client should get the same UUID, but in storage, it should be a different order.
        String uuid = (String) response.get("uuid");
        assertEquals(baseOrder.getUuid(), uuid);

        // Check that fields are correctly set in response
        assertEquals(SAMPLE_PATIENT_UUID, response.get("patient_uuid"));
        assertEquals(SAMPLE_INSTRUCTIONS, response.get("instructions"));
        assertEquals(startTime, response.get("start_millis"));
        assertEquals(newEndTime, response.get("stop_millis"));

        // Check that these fields match the object stored.
        Order stored = OrderRestResource.getLastRevision(baseOrder);
        assertEquals(SAMPLE_PATIENT_UUID, stored.getPatient().getUuid());
        assertEquals(SAMPLE_INSTRUCTIONS, stored.getInstructions());
        assertEquals(startTime, stored.getScheduledDate().getTime());
        assertEquals(newEndTime, stored.getAutoExpireDate().getTime());

        // Verify that updating the updated order also works correctly.
        newEndTime += ONE_WEEK_IN_MILLIS;
        newDetails = newOrderJson(
            null, SAMPLE_PROVIDER_UUID, null, null, newEndTime);
        request = newPostRequest(getURI() + "/" + baseOrder.getUuid(), newDetails);
        response = deserialize(handle(request));
        uuid = (String) response.get("uuid");
        assertEquals(baseOrder.getUuid(), uuid);
        assertEquals(newEndTime, response.get("stop_millis"));

        stored = OrderRestResource.getLastRevision(baseOrder);
        assertEquals(newEndTime, stored.getAutoExpireDate().getTime());

        // Verify that retrieving the order gets the last revision.
        SimpleObject retrieved = deserialize(handle(newGetRequest(getURI() + "/" + uuid)));
        assertEquals(uuid, retrieved.get("uuid"));
        assertEquals(newEndTime, retrieved.get("stop_millis"));
    }

    @Test public void testDeleteForNewNonExecutedOrderVoids() throws Exception {
        String uuid = createOrderStartingNow().getUuid();
        handle(newDeleteRequest(getURI() + "/" + uuid));
        assertTrue("Order is voided", orderService.getOrderByUuid(uuid).isVoided());
    }

    @Test public void testDeleteForExecutedOrderVoids() throws Exception {
        Order baseOrder = createOrderStartingNow();
        executeOrder(baseOrder);
        String uuid = baseOrder.getUuid();
        handle(newDeleteRequest(getURI() + "/" + uuid));
        Order order = orderService.getOrderByUuid(uuid);
        assertTrue("Order is voided", order.isVoided());
    }

    @Test public void testDeleteForRevisedOrderVoidsAll() throws Exception {
        Order baseOrder = createOrderStartingNow();
        String baseUuid = baseOrder.getUuid();

        // Make the update.
        SimpleObject newDetails = newOrderJson(
            null, SAMPLE_PROVIDER_UUID, "New instructions!", null, null);
        handle(newPostRequest(getURI() + "/" + baseUuid, newDetails));
        handle(newDeleteRequest(getURI() + "/" + baseUuid));

        // Reload the base order from storage
        baseOrder = orderService.getOrderByUuid(baseUuid);
        assertTrue("Base order is voided", baseOrder.isVoided());
        Order revisionOrder = OrderRestResource.getLastRevision(baseOrder);
        assertNotNull("Expected a non-null revision order", revisionOrder);
        assertTrue("Revision order is voided", revisionOrder.isVoided());
    }


    private Order createExpiredOrderOlderThan24Hrs() throws Exception {
        SimpleObject input = newOrderJson(
            SAMPLE_PATIENT_UUID, SAMPLE_PROVIDER_UUID, SAMPLE_INSTRUCTIONS,
            SAMPLE_START_DATE, SAMPLE_END_DATE);
        SimpleObject response = deserialize(handle(newPostRequest(getURI(), input)));
        String uuid = (String) response.get("uuid");
        return orderService.getOrderByUuid(uuid);
    }

    private Order createOrderStartingNow() throws Exception {
        long now = System.currentTimeMillis();
        SimpleObject input = newOrderJson(
            SAMPLE_PATIENT_UUID,SAMPLE_PROVIDER_UUID, SAMPLE_INSTRUCTIONS,
            now, now + ONE_WEEK_IN_MILLIS);
        SimpleObject response = deserialize(handle(newPostRequest(getURI(), input)));
        String uuid = (String) response.get("uuid");
        return orderService.getOrderByUuid(uuid);
    }

    private void executeOrder(Order order) throws Exception {
        handle(newPostRequest(ENCOUNTERS_URL, new SimpleObject()
            .add("uuid", order.getPatient().getUuid())
            .add("order_uuids", new String[] {order.getUuid()})
        ));
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
        @Nullable String patientUuid, @Nullable String providerUuid,
        @Nullable String instructions, @Nullable Long startMillis, @Nullable Long stopMillis) {
        SimpleObject order = new SimpleObject();
        if (patientUuid != null) {
            order.add("patient_uuid", patientUuid);
        }
        if (providerUuid != null) {
            order.add("orderer_uuid", providerUuid);
        }
        if (instructions != null) {
            order.add("instructions", instructions);
        }
        if (startMillis != null) {
            order.add("start_millis", startMillis);
        }
        if (stopMillis != null) {
            order.add("stop_millis", stopMillis);
        }
        return order;
    }
}
