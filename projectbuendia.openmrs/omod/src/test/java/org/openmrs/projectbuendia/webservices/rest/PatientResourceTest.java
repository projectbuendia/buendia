package org.openmrs.projectbuendia.webservices.rest;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/** Tests for PatientResource. */
@ContextConfiguration("/webModuleApplicationContext.xml")
public class PatientResourceTest extends BaseModuleContextSensitiveTest {
    protected void assertJsonEquals(String expected, String actual) {
        ObjectMapper om = new ObjectMapper();
        try {
            assertEquals(om.readTree(expected), om.readTree(actual));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPatientToJson() {
        PersonName name = new PersonName();
        name.setGivenName("Given");
        name.setFamilyName("Family");

        Patient patient = new Patient();
        patient.addName(name);
        patient.setGender("F");
        patient.setBirthdate(new Date(1987, 11, 27));
        patient.setDateCreated(new Date(2014, 12, 1));

        SimpleObject obj = PatientResource.patientToJson(patient);
        assertEquals("Given", obj.get("given_name"));
        assertEquals("Family", obj.get("family_name"));
        assertEquals("F", obj.get("gender"));
        assertEquals("1987-11-27", obj.get("birthdate"));

        long admission_timestamp = (long) obj.get("admission_timestamp");
        Date admission = new Date(admission_timestamp * 1000);
        assertEquals(new Date(2014, 12, 1), admission);
    }
}
