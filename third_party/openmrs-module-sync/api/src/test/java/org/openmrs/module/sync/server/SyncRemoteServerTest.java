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
package org.openmrs.module.sync.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncServerClass;
import org.openmrs.test.SkipBaseSetup;
import org.springframework.test.annotation.NotTransactional;

/**
 * Tests remote server operations.
 */
public class SyncRemoteServerTest {

    /**
     * test RemoteServer class implementation
     * 
     * @throws Exception
     */
	@Test
    @NotTransactional
    @SkipBaseSetup
    public void shouldSendReceiveTest() throws Exception {

        // 'normal' state
		RemoteServer parent = new RemoteServer();
		
		Set<SyncServerClass> serverClasses = new HashSet<SyncServerClass>();
		SyncServerClass ssc = new SyncServerClass();

		//gp SyncClass
		SyncClass gp = new SyncClass();
		gp.setDefaultReceiveFrom(false);
		gp.setDefaultSendTo(false);
		gp.setName("org.openmrs.GlobalProperty");

		//sched SyncClass
		SyncClass sched = new SyncClass();
		sched.setDefaultReceiveFrom(false);
		sched.setDefaultSendTo(false);
		sched.setName("org.openmrs.scheduler");
		
		//add gp as sync server class for parent
		ssc.setReceiveFrom(false);
		ssc.setSendTo(false);
		ssc.setServerClassId(1);
		ssc.setSyncServer(parent);
		ssc.setSyncClass(gp);
				
		serverClasses.add(ssc);
		parent.setNickname("parent");
		parent.setServerClasses(serverClasses);

		//now make up a sync record with just enough data
		SyncRecord record = new SyncRecord();
		record.addContainedClass("org.openmrs.Patient");
		record.addContainedClass("org.openmrs.Person");
	
		assertTrue(parent.shouldBeSentSyncRecord(record));
		assertTrue(parent.shouldReceiveSyncRecordFrom(record));
		record.addContainedClass("org.openmrs.GlobalProperty");
		assertFalse(parent.shouldBeSentSyncRecord(record));
		assertFalse(parent.shouldReceiveSyncRecordFrom(record));
		
		ssc.setReceiveFrom(true);
		assertTrue(parent.shouldReceiveSyncRecordFrom(record));
		ssc.setReceiveFrom(false);
		
		//now do the test with sched and wild cards
		SyncServerClass ssc2 = new SyncServerClass();
		ssc2.setReceiveFrom(false);
		ssc2.setSendTo(false);
		ssc2.setServerClassId(1);
		ssc2.setSyncServer(parent);
		ssc2.setSyncClass(sched);
		serverClasses.add(ssc2);

		//un-map the gp and make sure it isn't matched any longer
		gp.setName("org.openmrs.Global.Bogus");
		assertTrue(parent.shouldBeSentSyncRecord(record));
		assertTrue(parent.shouldReceiveSyncRecordFrom(record));
		
		record.addContainedClass("org.openmrs.scheduler.TaskDefinition");
		assertFalse(parent.shouldBeSentSyncRecord(record));
		assertFalse(parent.shouldReceiveSyncRecordFrom(record));
		
        return;
    }
}
