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
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.test.SkipBaseSetup;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.assertEquals;

/** REST API tests for ConceptResource. */
@SkipBaseSetup
public class ConceptResourceTest extends BaseApiRequestTest {
    private static final String YES_UUID = "b055abd8-a420-4a11-8b98-02ee170a7b54";

    public String[] getInitialDataFiles() {
        return new String[] {
            "org/openmrs/projectbuendia/webservices/rest/base-test-data.xml",
        };
    };

    public String getURI() {
        return "/projectbuendia/concepts";
    }

    public long getAllCount() {
        return 0;  // no concepts are returned because there are no forms or charts
    }

    public String getUuid() {
        return YES_UUID;
    }

    @Test public void testGetConcept() throws Exception {
        MockHttpServletRequest request = request(RequestMethod.GET, this.getURI() + "/" + YES_UUID);
        SimpleObject response = this.deserialize(this.handle(request));
        Assert.assertNotNull(response);

        assertEquals(YES_UUID, response.get("uuid"));
        assertEquals("none", response.get("type"));
        assertEquals("Yes", response.get("name"));
    }

    @Test public void testGetConceptInExactLocale() throws Exception {
        MockHttpServletRequest request = newRequest(
            RequestMethod.GET, this.getURI() + "/" + YES_UUID, new Parameter("locale", "fr"));
        SimpleObject response = this.deserialize(this.handle(request));
        Assert.assertNotNull(response);

        assertEquals(YES_UUID, response.get("uuid"));
        assertEquals("none", response.get("type"));
        assertEquals("Oui", response.get("name"));  // exact match for a name with language tag "fr"
    }

    @Test public void testGetConceptInMoreSpecificLocale() throws Exception {
        MockHttpServletRequest request = newRequest(
            RequestMethod.GET, this.getURI() + "/" + YES_UUID, new Parameter("locale", "fr-CA"));
        SimpleObject response = this.deserialize(this.handle(request));
        Assert.assertNotNull(response);

        assertEquals(YES_UUID, response.get("uuid"));
        assertEquals("none", response.get("type"));
        assertEquals("Oui", response.get("name"));  // "fr-CA" retrieves a name with language tag "fr"
    }

    @Test public void testGetConceptInLessSpecificLocale() throws Exception {
        MockHttpServletRequest request = newRequest(
            RequestMethod.GET, this.getURI() + "/" + YES_UUID, new Parameter("locale", "es"));
        SimpleObject response = this.deserialize(this.handle(request));
        Assert.assertNotNull(response);

        assertEquals(YES_UUID, response.get("uuid"));
        assertEquals("none", response.get("type"));
        assertEquals("Yes", response.get("name"));  // "es" does not retrieve a name with language tag "es-419"
    }

    @Test public void testGetConceptInUnknownLocale() throws Exception {
        MockHttpServletRequest request = newRequest(
            RequestMethod.GET, this.getURI() + "/" + YES_UUID, new Parameter("locale", "ru"));
        SimpleObject response = this.deserialize(this.handle(request));
        Assert.assertNotNull(response);

        assertEquals(YES_UUID, response.get("uuid"));
        assertEquals("none", response.get("type"));
        assertEquals("Yes", response.get("name"));  // no "ru" name is defined; we should get the "en" name
    }
}
