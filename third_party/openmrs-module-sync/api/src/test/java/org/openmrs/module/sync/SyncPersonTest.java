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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.springframework.test.annotation.NotTransactional;

/**
 * Testing syncing of the {@link Person} object
 */
public class SyncPersonTest extends SyncBaseTest {
	
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
	public void shouldSavePersonAttributeAndPersistForeignKeyPK() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				PersonService ps = Context.getPersonService();
				
				PersonAttributeType type = ps.getPersonAttributeType(5);
				
				Person person = ps.getPerson(4);
				person.addAttribute(new PersonAttribute(type, "13")); // make this person MARRIED
				ps.savePerson(person);
				
				// make sure the PersonAttribute.value in the sync record is a uuid now
				SyncService ss = Context.getService(SyncService.class);
				List<SyncRecord> records = ss.getSyncRecords();
				SyncRecord record = records.get(records.size() - 1);
				SyncItem item = record.getItems().toArray(new SyncItem[] {})[0];
				// the uuid for concept #13, "MARRIED"
				Assert.assertTrue(item.getContent().contains("value type=\"org.openmrs.Concept\">111e833e-12c5-102b-119c-e43ed545d111<"));
			}
			
			public void runOnParent() throws Exception {
				PersonService ps = Context.getPersonService();
				
				// test to make sure the value is changed back to the right integer
				Person person = ps.getPerson(4);
				PersonAttribute attribute = person.getAttribute(5);
				Assert.assertEquals("13", attribute.getValue());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSaveSimpleStringPersonAttribute() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				PersonService ps = Context.getPersonService();
				
				PersonAttributeType type = ps.getPersonAttributeType(1);
				
				Person person = ps.getPerson(4);
				person.addAttribute(new PersonAttribute(type, "name")); // assign an arbitrary string value to this attribute 
				ps.savePerson(person);
				
			}
			
			public void runOnParent() throws Exception {
				PersonService ps = Context.getPersonService();
				
				// test to make sure that the value has been synced
				Person person = ps.getPerson(4);
				PersonAttribute attribute = person.getAttribute(1);
				Assert.assertEquals("name", attribute.getValue());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSaveSimpleStringPersonAttributeWithNumericalValue() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				PersonService ps = Context.getPersonService();
				
				PersonAttributeType type = ps.getPersonAttributeType(1);
				
				Person person = ps.getPerson(4);
				person.addAttribute(new PersonAttribute(type, "123")); // set the string to a number, since this is what was giving us the problem 
				ps.savePerson(person);
				
			}
			
			public void runOnParent() throws Exception {
				PersonService ps = Context.getPersonService();
				
				// test to make sure that the value has been synced
				Person person = ps.getPerson(4);
				PersonAttribute attribute = person.getAttribute(1);
				Assert.assertEquals("123", attribute.getValue());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSaveSimpleIntegerPersonAttribute() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				PersonService ps = Context.getPersonService();
				
				PersonAttributeType type = ps.getPersonAttributeType(8);
				
				Person person = ps.getPerson(4);
				person.addAttribute(new PersonAttribute(type, "368")); // assign an arbitrary Integer value to this attribute 
				ps.savePerson(person);
				
			}
			
			public void runOnParent() throws Exception {
				PersonService ps = Context.getPersonService();
				
				// test to make sure that the value has been synced
				Person person = ps.getPerson(4);
				PersonAttribute attribute = person.getAttribute(8);
				Assert.assertEquals("368", attribute.getValue());
			}
		});
	}
	
	
}
