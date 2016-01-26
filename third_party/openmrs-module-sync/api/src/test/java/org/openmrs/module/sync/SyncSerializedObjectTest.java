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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.SerializedObject;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.serialization.OpenmrsSerializer;
import org.springframework.test.annotation.NotTransactional;

/**
 * This class tests the {@link SerializedObjectDAO} linked to from the Context. Currently that file
 * is the {@link HibernateSerializedObjectDAO}.
 */
public class SyncSerializedObjectTest extends SyncBaseTest {
	
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
	public void shouldSyncSerializedOBject() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				
				//just random data
				SerializedObject serializedObject = new SerializedObject();
				serializedObject.setName("blah");
				serializedObject.setDescription("This is to test saving a report");
				serializedObject.setCreator(new User(1));
				serializedObject.setDateCreated(new Date());
				serializedObject.setUuid("d4b94c0a-03cb-11e0-a36c-a923b2165773");
				serializedObject.setType(User.class.getName());
				serializedObject.setSubtype(User.class.getName());
				serializedObject.setSerializationClass(OpenmrsSerializer.class);
				serializedObject.setSerializedData("gook");
				
				SyncService ss = Context.getService(SyncService.class);
				ss.saveOrUpdate(serializedObject);
			}
			
			public void runOnParent() throws Exception {
				SerializedObject o = null;
				SyncService ss = Context.getService(SyncService.class);
				o = ss.getOpenmrsObjectByUuid(SerializedObject.class, "d4b94c0a-03cb-11e0-a36c-a923b2165773");

				assertNotNull(o);
				assertEquals("blah",o.getName());
				assertEquals(OpenmrsSerializer.class,o.getSerializationClass());
			}
		});
	}	

}
