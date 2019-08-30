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
import org.projectbuendia.openmrs.api.Bookmark;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.openmrs.projectbuendia.webservices.rest.BookmarkUtils.clampBookmarkToBufferedRequestTime;
import static org.openmrs.projectbuendia.webservices.rest.BookmarkUtils.parseJson;
import static org.openmrs.projectbuendia.webservices.rest.BookmarkUtils.toJson;

public class BookmarkUtilsTest {

    private static final int REQUEST_BUFFER_MILLIS = 2000;

    private final long requestTimestamp = 1448450728000L;
    private final Date requestTime = new Date(requestTimestamp);
    private final Date bufferedRequestTime = new Date(requestTimestamp - REQUEST_BUFFER_MILLIS);
    private final String uuid = "i-am-a-uuid";

    @Test
    public void testJsonToBookmarkFullyPopulated() throws Exception {
        // Everything populated
        Bookmark expected = new Bookmark(new Date(1448450728000L), uuid);
        Bookmark result = parseJson("{\"t\":\"2015-11-25T11:25:28.000Z\",\"u\":\"i-am-a-uuid\"}");
        assertBookmarksEqual(expected, result);
    }

    @Test
    public void testJsonToBookmarkNullUuid() throws Exception {
        // UUID = explicitly null
        Bookmark expected = new Bookmark(new Date(1448450728000L), null);
        Bookmark result = parseJson("{\"t\":\"2015-11-25T11:25:28.000Z\",\"u\":null}");
        assertBookmarksEqual(expected, result);
    }

    @Test
    public void testJsonToBookmarkUuidAbsent() throws Exception {
        // No UUID
        Bookmark expected = new Bookmark(new Date(1448450728000L), null);
        Bookmark result = parseJson("{\"t\":\"2015-11-25T11:25:28.000Z\"}");
        assertBookmarksEqual(expected, result);
    }
    @Test
    public void testJsonToBookmarkNoTimestamp() throws Exception {
        // No UUID
        try {
            parseJson("{\"u\":\"12345\"}");
            fail("Expected an exception");
        } catch (JsonMappingException e) {
            //expected
        }
    }

    @Test
    public void testJsonToBookmarkInvalidDate() throws Exception {
        // No UUID
        try {
            parseJson("{\"t\":\"2015-11-25T11!25tuesday28.000Z\"}");
            fail("Expected an exception");
        } catch (InvalidObjectDataException e) {
            //expected
        }
    }

    @Test
    public void testBookmarkToJsonWithUuid() throws Exception {
        String expected = "{\"t\":\"2015-11-25T11:25:28.000Z\",\"u\":\"i-am-a-uuid\"}";
        String result = toJson(new Bookmark(new Date(1448450728000L), uuid));
        assertEquals(expected, result);
    }

    @Test
    public void testBookmarkToJsonNullUuid() throws Exception {
        // Note: we'd also accept a missing "u" parameter here, see testJsonToBookmarkUuidAbsent.
        String expected = "{\"t\":\"2015-11-25T11:25:28.000Z\",\"u\":null}";
        String result = toJson(new Bookmark(new Date(1448450728000L), null));
        assertEquals(expected, result);
    }

    @Test
    public void testClampWithAcceptableDaoBookmark() throws Exception {
        // Last record was modified 2.001 seconds before the request time.
        // This is ok.
        Bookmark daoToken = new Bookmark(new Date(requestTimestamp - 2001), uuid);

        Bookmark result = clampBookmarkToBufferedRequestTime(daoToken, requestTime);
        assertBookmarksEqual(daoToken, result);
    }

    @Test
    public void testClampWithDaoTokenTooRecent() throws Exception {
        // Last record was modified 1.5 seconds before the request time.
        // This is too recent; we should get a new Bookmark with the request timestamp and no UUID
        // instead.
        Bookmark daoToken = new Bookmark(new Date(requestTimestamp - 1500), uuid);

        Bookmark result = clampBookmarkToBufferedRequestTime(daoToken, requestTime);
        assertNotNull(result);
        assertEquals(bufferedRequestTime, result.minTime);
        assertNull(result.minUuid);
    }

    @Test
    public void testClampWithNullDaoToken() throws Exception {
        // No results retrieved from database. From an algorithmic perspective, we don't care what
        // the response is here - either a null sync token or a sync token clamped to the buffered
        // request time is fine - all that matters is that the timestamp is earlier than or equal
        // to the buffered request time. For programmer convenience, though, we've opted to make
        // sure the call never returns null.
        Bookmark result = clampBookmarkToBufferedRequestTime(null, requestTime);
        assertNotNull(result);
        assertEquals(bufferedRequestTime, result.minTime);
        assertNull(result.minUuid);
    }

    private void assertBookmarksEqual(Bookmark expected, Bookmark actual) {
        assertEquals(expected.minTime, actual.minTime);
        assertEquals(expected.minUuid, actual.minUuid);
    }

}