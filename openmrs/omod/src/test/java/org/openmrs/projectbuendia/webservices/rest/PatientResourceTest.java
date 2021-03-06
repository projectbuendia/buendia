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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** REST API tests for PatientResource. */
@SkipBaseSetup
public class PatientResourceTest extends BaseApiRequestTest {
    // These constants should match the values in the data files.
    String PETER_PAN_UUID = "f6f74ed9-5681-482a-9aa5-c3192579fa59";
    String XANADU_UUID = "9356400c-a5a2-4532-8f2b-2361b3446eb8";

    public String[] getInitialDataFiles() {
        return new String[] {
            "org/openmrs/projectbuendia/webservices/rest/base-test-data.xml",
            "org/openmrs/projectbuendia/webservices/rest/patient-test-data.xml"
        };
    };

    public String getURI() {
        return "/patients";
    }

    public long getAllCount() {
        return 2;  // number of patients in the test data file
    }

    public String getUuid() {
        return PETER_PAN_UUID;
    }

    @Test public void testGetPatient() throws Exception {
        MockHttpServletRequest request = this.request(RequestMethod.GET, this.getURI() + "/" + PETER_PAN_UUID);
        SimpleObject response = this.deserialize(this.handle(request));
        Assert.assertNotNull(response);

        assertEquals(PETER_PAN_UUID, response.get("uuid"));
        assertEquals("Peter", response.get("given_name"));
        assertEquals("Pan", response.get("family_name"));
        assertEquals("M", response.get("sex"));
        assertEquals("ABC123", response.get("id"));
    }

    @Test public void testCreatePatient() throws Exception {
        SimpleObject input = new SimpleObject();
        input.add("id", "XYZ");
        input.add("given_name", "Mary");
        input.add("family_name", "Poppins");
        input.add("sex", "F");
        input.add("birthdate", "1970-01-01");

        MockHttpServletRequest request = newPostRequest(getURI(), input);
        SimpleObject response = deserialize(handle(request));

        String uuid = (String) response.get("uuid");
        assertEquals("XYZ", response.get("id"));
        assertEquals("Mary", response.get("given_name"));
        assertEquals("Poppins", response.get("family_name"));
        assertEquals("F", response.get("sex"));
        assertEquals("1970-01-01", response.get("birthdate"));

        Patient patient = patientService.getPatientByUuid(uuid);
        assertEquals("XYZ", patient.getPatientIdentifier(DbUtils.getMsfIdType()).getIdentifier());
    }

    @Test public void testCreatePatientWithObservation() throws Exception {
        SimpleObject input = new SimpleObject();
        input.add("id", "XYZ");
        input.add("given_name", "Mary");
        input.add("family_name", "Poppins");
        input.add("sex", "F");
        input.add("birthdate", "1970-01-01");

        List<Map> observations = new ArrayList<>();
        SimpleObject obs = new SimpleObject();
        obs.add("provider_uuid", "0a828bd6-d63c-4acc-b987-2400f2ab8c4c");
        obs.add("concept_uuid", "96408258-000b-424e-af1a-403919332938");
        obs.add("value_text", "abc");
        observations.add(obs);
        input.add("observations", observations);

        MockHttpServletRequest request = newPostRequest(getURI(), input);
        SimpleObject response = deserialize(handle(request));

        String uuid = (String) response.get("uuid");
        assertEquals("XYZ", response.get("id"));
        assertEquals("Mary", response.get("given_name"));
        assertEquals("Poppins", response.get("family_name"));
        assertEquals("F", response.get("sex"));
        assertEquals("1970-01-01", response.get("birthdate"));

        List<Map> respList = (List<Map>) response.get("observations");
        assertEquals(1, respList.size());
        Map respObs = respList.get(0);
        assertEquals("0a828bd6-d63c-4acc-b987-2400f2ab8c4c", respObs.get("provider_uuid"));
        assertEquals("96408258-000b-424e-af1a-403919332938", respObs.get("concept_uuid"));
        assertEquals("abc", respObs.get("value_text"));
    }
}
