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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests for LocationResource. */
@SkipBaseSetup
public class LocationResourceTest extends MainResourceControllerTest {

    private static final String BASE_TEST_DATA =
        "org/openmrs/projectbuendia/webservices/rest/base-test-data.xml";

    // These constants should match the values in the BASE_TEST_DATA file.
    String XANADU_UUID = "9356400c-a5a2-4532-8f2b-2361b3446eb8";
    String NEVER_NEVER_UUID = "167ce20c-4785-4285-9119-d197268f7f4a";
    String NEVER_NEVER_NAME = "Never Never Land";

    // This value should not exist as a location UUID in the BASE_TEST_DATA file.
    String INVALID_LOCATION_UUID = "13572468-1357-2468-1357-12345678abcd";

    private LocationService locationService;

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
        locationService = Context.getLocationService();
        if (useInMemoryDatabase()) {
            initializeInMemoryDatabase();
            authenticate();
        }
        executeDataSet(BASE_TEST_DATA);
    }

    @Override
    public String getURI() {
        return "projectbuendia/locations";
    }

    @Override
    public String getUuid() {  // for the common tests defined in MainResourceControllerTest
        return XANADU_UUID;
    }

    @Override
    public long getAllCount() {
        // There are three locations in the BASE_TEST_DATA file, with ids "1", "2", and "3".
        // The location with id "1" is retired, so only two locations should be returned.
        return 2;
    }

    @Test
    public void testGetLocation() throws Exception {
        MockHttpServletRequest request = this.request(RequestMethod.GET, this.getURI() + "/" + NEVER_NEVER_UUID);
        SimpleObject response = this.deserialize(this.handle(request));
        Assert.assertNotNull(response);

        assertEquals(NEVER_NEVER_UUID, response.get("uuid"));
        assertEquals(XANADU_UUID, response.get("parent_uuid"));
        assertEquals(NEVER_NEVER_NAME, ((Map<String, Object>) response.get("names")).get("en"));
    }

    @Test
    public void testGetMissingLocation() throws Exception {
        MockHttpServletRequest request = this.request(RequestMethod.GET, this.getURI() + "/" + INVALID_LOCATION_UUID);
        try {
            MockHttpServletResponse response = this.handle(request);
        } catch (ObjectNotFoundException e) {
            // success
            return;
        }
        fail("ObjectNotFoundException was expected but not thrown");
    }
}