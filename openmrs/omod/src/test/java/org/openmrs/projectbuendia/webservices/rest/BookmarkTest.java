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

import org.junit.Test;
import org.openmrs.module.webservices.rest.web.response.InvalidSearchException;
import org.projectbuendia.openmrs.api.Bookmark;

import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class BookmarkTest {

    private static final int REQUEST_BUFFER_MILLIS = 2000;

    private final long requestTimestamp = 1448450728000L;
    private final Date requestTime = new Date(requestTimestamp);
    private final Date bufferedRequestTime = new Date(requestTimestamp - REQUEST_BUFFER_MILLIS);
    private final String uuid = "i-am-a-uuid";

    @Test public void testDeserializeFullyPopulated() throws Exception {
        // Everything populated
        Bookmark expected = new Bookmark(new Date(1448450728000L), uuid);
        Bookmark result = Bookmark.deserialize("2015-11-25T11:25:28.000Z/i-am-a-uuid");
        assertEquals(expected, result);
    }

    @Test public void testDeserializeUuidEmpty() throws Exception {
        // UUID = empty string
        Bookmark expected = new Bookmark(new Date(1448450728000L), null);
        Bookmark result = Bookmark.deserialize("2015-11-25T11:25:28.000Z/");
        assertEquals(expected, result);
    }

    @Test public void testDeserializeUuidAbsent() throws Exception {
        // No UUID
        Bookmark expected = new Bookmark(new Date(1448450728000L), null);
        Bookmark result = Bookmark.deserialize("2015-11-25T11:25:28.000Z");
        assertEquals(expected, result);
    }
    @Test public void testDeserializeNoTimestamp() throws Exception {
        try {
            Bookmark.deserialize("/12345");
            fail("Expected an exception");
        } catch (ParseException e) {
            //expected
        }
    }

    @Test public void testDeserializeInvalidDate() throws Exception {
        try {
            Bookmark.deserialize("2015-11-25T11!25tuesday28.000Z");
            fail("Expected an exception");
        } catch (ParseException e) {
            //expected
        }
    }

    @Test public void testSerializeWithUuid() throws Exception {
        String expected = "2015-11-25T11:25:28.000Z/i-am-a-uuid";
        String result = new Bookmark(new Date(1448450728000L), uuid).serialize();
        assertEquals(expected, result);
    }

    @Test public void testSerializeNullUuid() throws Exception {
        String expected = "2015-11-25T11:25:28.000Z";
        String result = new Bookmark(new Date(1448450728000L), null).serialize();
        assertEquals(expected, result);
    }

    @Test
    public void testClampWithAcceptableDaoBookmark() throws Exception {
        // Last record was modified 2.001 seconds before the request time.
        // This is ok.
        Bookmark daoToken = new Bookmark(new Date(requestTimestamp - 2001), uuid);
        Bookmark result = Bookmark.clampToBufferedRequestTime(daoToken, requestTime);
        assertEquals(daoToken, result);
    }

    @Test
    public void testClampWithDaoTokenTooRecent() throws Exception {
        // Last record was modified 1.5 seconds before the request time.
        // This is too recent; we should get a new Bookmark with the request timestamp and no UUID
        // instead.
        Bookmark daoToken = new Bookmark(new Date(requestTimestamp - 1500), uuid);
        Bookmark result = Bookmark.clampToBufferedRequestTime(daoToken, requestTime);
        assertNotNull(result);
        assertEquals(bufferedRequestTime, result.minTime);
        assertNull(result.minUuid);
    }

    @Test public void testClampWithNullDaoToken() throws Exception {
        // No results retrieved from database. From an algorithmic perspective, we don't care what
        // the response is here - either a null sync token or a sync token clamped to the buffered
        // request time is fine - all that matters is that the timestamp is earlier than or equal
        // to the buffered request time. For programmer convenience, though, we've opted to make
        // sure the call never returns null.
        Bookmark result = Bookmark.clampToBufferedRequestTime(null, requestTime);
        assertNotNull(result);
        assertEquals(bufferedRequestTime, result.minTime);
        assertNull(result.minUuid);
    }
}