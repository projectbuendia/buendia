/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.NotTransactional;

import java.util.Iterator;
import java.util.Set;

/**
 * Testing syncing of PersonAttributes via the Person object
 */
public class SyncPersonAttributeTest extends SyncBaseTest {

    @Autowired
    PersonService personService;

    @Autowired
    PatientService patientService;
	
	@Override
	public String getInitialDataset() {
        try {
            return "org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
	}
	
	@Test
	@NotTransactional
	public void shouldSavePersonAttributeTypeAndPersistForeignKeyPK() throws Exception {
		runSyncTest(new SyncTestHelper() {

			public void runOnChild() throws Exception {
				PersonAttributeType type = personService.getPersonAttributeType(6); // health district
				Patient patient = patientService.getPatient(4);
				PersonAttribute attr = new PersonAttribute(type, "Some Location");
				patient.addAttribute(attr);
				patientService.savePatient(patient);
				
			}
			
			public void runOnParent() throws Exception {
				Person person = personService.getPerson(4);
				PersonAttribute attr = person.getAttribute(6);
				Assert.assertEquals("Some Location", attr.getValue());
			}
		});
	}

    @Test
    @NotTransactional
    public void shouldUpdatePersonAttribute()throws Exception {

        runSyncTest(new SyncTestHelper() {

            public void runOnChild() throws Exception {
                PersonAttributeType type = personService.getPersonAttributeType(8); // favorite number

                Person person = personService.getPerson(5);  // get a patient that already has favorite number defined
                PersonAttribute attr = new PersonAttribute(type, "5");
                person.addAttribute(attr);
                personService.savePerson(person);

                Set<PersonAttribute> attrs = person.getAttributes();

                // patient should now have three attributes
                Assert.assertEquals(3, attrs.size());

                // as a sanity check, confirm that the patient has the right person attributes
                Iterator<PersonAttribute> i = person.getAttributes().iterator();

                while (i.hasNext()) {
                    attr = i.next();
                    int value = Integer.valueOf(attr.getValue());

                    if (value == 3 || value == 4  || value == 5) {

                        if (value == 3 || value == 4) {
                            Assert.assertTrue(attr.isVoided());
                        }
                        else {
                            Assert.assertFalse(attr.isVoided());
                        }
                        i.remove();
                    }
                }

                // list should now be empty
                Assert.assertEquals(0, attrs.size());
            }

            public void runOnParent() throws Exception {

                 Person person = personService.getPerson(5);
                Set<PersonAttribute> attrs = person.getAttributes();

                // confirm that parent service has proper person attributes
                Assert.assertEquals(3, attrs.size());

                Iterator<PersonAttribute> i = person.getAttributes().iterator();

                while (i.hasNext()) {
                    PersonAttribute attr = i.next();
                    int value = Integer.valueOf(attr.getValue());

                    if (value == 3 || value == 4  || value == 5) {

                        if (value == 3 || value == 4) {
                            Assert.assertTrue(attr.isVoided());
                        }
                        else {
                            Assert.assertFalse(attr.isVoided());
                        }
                        i.remove();
                    }
                }

                // list should now be empty
                Assert.assertEquals(0, attrs.size());
            }
        });

    }

    @Test
    @NotTransactional
    public void shouldSyncBooleanPersonAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {

            public void runOnChild() throws Exception {
                PersonAttributeType type = personService.getPersonAttributeTypeByName("Test Patient");
                Assert.assertEquals("java.lang.Boolean", type.getFormat());

                Patient patient = patientService.getPatient(4);
                PersonAttribute attr = new PersonAttribute(type, "true");
                patient.addAttribute(attr);
                patientService.savePatient(patient);
            }

            public void runOnParent() throws Exception {
                Person person = personService.getPerson(4);
                PersonAttribute attr = person.getAttribute("Test Patient");
                Assert.assertEquals("true", attr.getValue());
                Assert.assertEquals(Boolean.TRUE, attr.getHydratedObject());
            }
        });
    }
	
}
