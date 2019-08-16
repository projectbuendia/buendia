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

package org.openmrs.projectbuendia.webservices.rest;

import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Test;
import org.projectbuendia.openmrs.api.SyncToken;

import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.openmrs.projectbuendia.webservices.rest.SyncTokenUtils.clampSyncTokenToBufferedRequestTime;
import static org.openmrs.projectbuendia.webservices.rest.SyncTokenUtils.jsonToSyncToken;
import static org.openmrs.projectbuendia.webservices.rest.SyncTokenUtils.syncTokenToJson;

public class SyncTokenUtilsTest {

    private static final int REQUEST_BUFFER_MILLIS = 2000;

    private final long requestTimestamp = 1448450728000L;
    private final Date requestTime = new Date(requestTimestamp);
    private final Date bufferedRequestTime = new Date(requestTimestamp - REQUEST_BUFFER_MILLIS);
    private final String uuid = "i-am-a-uuid";

    @Test
    public void testJsonToSyncTokenFullyPopulated() throws Exception {
        // Everything populated
        SyncToken expected = new SyncToken(new Date(1448450728000L), uuid);
        SyncToken result = jsonToSyncToken("{\"t\":\"2015-11-25T11:25:28.000Z\",\"u\":\"i-am-a-uuid\"}");
        assertSyncTokensEqual(expected, result);
    }

    @Test
    public void testJsonToSyncTokenNullUuid() throws Exception {
        // UUID = explicitly null
        SyncToken expected = new SyncToken(new Date(1448450728000L), null);
        SyncToken result = jsonToSyncToken("{\"t\":\"2015-11-25T11:25:28.000Z\",\"u\":null}");
        assertSyncTokensEqual(expected, result);
    }

    @Test
    public void testJsonToSyncTokenUuidAbsent() throws Exception {
        // No UUID
        SyncToken expected = new SyncToken(new Date(1448450728000L), null);
        SyncToken result = jsonToSyncToken("{\"t\":\"2015-11-25T11:25:28.000Z\"}");
        assertSyncTokensEqual(expected, result);
    }
    @Test
    public void testJsonToSyncTokenNoTimestamp() throws Exception {
        // No UUID
        try {
            jsonToSyncToken("{\"u\":\"12345\"}");
            fail("Expected an exception");
        } catch (JsonMappingException e) {
            //expected
        }
    }

    @Test
    public void testJsonToSyncTokenInvalidDate() throws Exception {
        // No UUID
        try {
            jsonToSyncToken("{\"t\":\"2015-11-25T11!25tuesday28.000Z\"}");
            fail("Expected an exception");
        } catch (InvalidObjectDataException e) {
            //expected
        }
    }

    @Test
    public void testSyncTokenToJsonWithUuid() throws Exception {
        String expected = "{\"t\":\"2015-11-25T11:25:28.000Z\",\"u\":\"i-am-a-uuid\"}";
        String result = syncTokenToJson(new SyncToken(new Date(1448450728000L), uuid));
        assertEquals(expected, result);
    }

    @Test
    public void testSyncTokenToJsonNullUuid() throws Exception {
        // Note: we'd also accept a missing "u" parameter here, see testJsonToSyncTokenUuidAbsent.
        String expected = "{\"t\":\"2015-11-25T11:25:28.000Z\",\"u\":null}";
        String result = syncTokenToJson(new SyncToken(new Date(1448450728000L), null));
        assertEquals(expected, result);
    }

    @Test
    public void testClampWithAcceptableDaoSyncToken() throws Exception {
        // Last record was modified 2.001 seconds before the request time.
        // This is ok.
        SyncToken daoToken = new SyncToken(new Date(requestTimestamp - 2001), uuid);

        SyncToken result = clampSyncTokenToBufferedRequestTime(daoToken, requestTime);
        assertSyncTokensEqual(daoToken, result);
    }

    @Test
    public void testClampWithDaoTokenTooRecent() throws Exception {
        // Last record was modified 1.5 seconds before the request time.
        // This is too recent; we should get a new SyncToken with the request timestamp and no UUID
        // instead.
        SyncToken daoToken = new SyncToken(new Date(requestTimestamp - 1500), uuid);

        SyncToken result = clampSyncTokenToBufferedRequestTime(daoToken, requestTime);
        assertNotNull(result);
        assertEquals(bufferedRequestTime, result.greaterThanOrEqualToTimestamp);
        assertNull(result.greaterThanUuid);
    }

    @Test
    public void testClampWithNullDaoToken() throws Exception {
        // No results retrieved from database. From an algorithmic perspective, we don't care what
        // the response is here - either a null sync token or a sync token clamped to the buffered
        // request time is fine - all that matters is that the timestamp is earlier than or equal
        // to the buffered request time. For programmer convenience, though, we've opted to make
        // sure the call never returns null.
        SyncToken result = clampSyncTokenToBufferedRequestTime(null, requestTime);
        assertNotNull(result);
        assertEquals(bufferedRequestTime, result.greaterThanOrEqualToTimestamp);
        assertNull(result.greaterThanUuid);
    }

    private void assertSyncTokensEqual(SyncToken expected, SyncToken actual) {
        assertEquals(expected.greaterThanOrEqualToTimestamp, actual.greaterThanOrEqualToTimestamp);
        assertEquals(expected.greaterThanUuid, actual.greaterThanUuid);
    }

}