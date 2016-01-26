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
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.springframework.test.annotation.NotTransactional;

/**
 * Testing syncing of the {@link PersonAttributeType} object
 */
public class SyncPersonAttributeTypeTest extends SyncBaseTest {
	
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
				PersonService ps = Context.getPersonService();
				
				PersonAttributeType type = ps.getPersonAttributeType(5);
				type.setName("CIVIL STATUS v2"); // change some arbitrary property so the obj gets saved
				ps.savePersonAttributeType(type);
				
				// make sure the foreignKey in the sync record is a uuid now
				SyncService ss = Context.getService(SyncService.class);
				List<SyncRecord> records = ss.getSyncRecords();
				SyncRecord record = records.get(records.size() - 1);
				SyncItem item = record.getItems().toArray(new SyncItem[] {})[0];
				// the uuid for concept #12
				Assert.assertTrue(item.getContent().contains("foreignKey type=\"org.openmrs.Concept\">333e833e-12c5-102b-119c-e43ed545d333<"));
			}
			
			public void runOnParent() throws Exception {
				PersonService ps = Context.getPersonService();
				
				// test to make sure the foreignKey is changed back to the right integer
				PersonAttributeType type = ps.getPersonAttributeType(5);
				Assert.assertEquals(12, type.getForeignKey().intValue());
			}
		});
	}
	
}
