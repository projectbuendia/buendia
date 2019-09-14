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
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.test.SkipBaseSetup;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** REST API tests for LocationResource. */
@SkipBaseSetup public class LocationResourceTest extends BaseApiRequestTest {

    // These constants should match the values in the BASE_TEST_DATA file.
    String RETIRED_UUID = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
    String XANADU_UUID = "9356400c-a5a2-4532-8f2b-2361b3446eb8";
    String NEVER_NEVER_UUID = "167ce20c-4785-4285-9119-d197268f7f4a";
    String NEVER_NEVER_NAME = "Never Never Land";

    // This value should not exist as a location UUID in the BASE_TEST_DATA file.
    String INVALID_LOCATION_UUID = "13572468-1357-2468-1357-12345678abcd";

    public String[] getInitialDataFiles() {
        return new String[] {
            "org/openmrs/projectbuendia/webservices/rest/base-test-data.xml"
        };
    }

    @Override public String getURI() {
        return "/locations";
    }

    @Override public String getUuid() {
        return XANADU_UUID;
    }

    @Override public long getAllCount() {
        // There are three locations in the BASE_TEST_DATA file, with ids "1", "2", and "3".
        // The location with id "1" is retired, so only two locations should be returned.
        return 2;
    }

    @Test public void testGetLocation() throws Exception {
        SimpleObject response = deserialize(handle(newGetRequest(getURI() + "/" + NEVER_NEVER_UUID)));
        assertNotNull(response);
        assertEquals(NEVER_NEVER_UUID, response.get("uuid"));
        assertEquals(XANADU_UUID, response.get("parent_uuid"));
        assertEquals(NEVER_NEVER_NAME, response.get("name"));
    }

    @Test public void testGetMissingLocation() throws Exception {
        assertExceptionOnRequest(newGetRequest(getURI() + "/" + INVALID_LOCATION_UUID), "nonexistent location UUID");
    }

    @Test public void testGetRetiredLocation() throws Exception {
        assertExceptionOnRequest(newGetRequest(getURI() + "/" + RETIRED_UUID), "retired location UUID");
    }
}