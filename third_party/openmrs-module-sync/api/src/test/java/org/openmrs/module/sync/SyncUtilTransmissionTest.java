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

import junit.framework.Assert;
import org.junit.Ignore;

import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncTransmission;
import org.openmrs.module.sync.SyncTransmissionState;
import org.openmrs.module.sync.SyncUtilTransmission;
import org.openmrs.module.sync.SyncUtilTransmission.ReceivingSize;
import org.openmrs.module.sync.ingest.SyncTransmissionResponse;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Tests methods on the SyncUtilTransmission class.
 */
public class SyncUtilTransmissionTest extends BaseModuleContextSensitiveTest implements Runnable {
	
	//Ignoring this test up to when a correct one will be implemented.
	@Ignore
	@Test
	public void doFullSynchronize_shouldRunOneSyncTaskAtATime() throws Exception {
		executeDataSet("org/openmrs/module/sync/include/SyncRemoteChildServer.xml");
		
		new Thread(this).start();
		
		//This test works on the assumption that the code below will execute 
		//before the above thread's run method is executed.
		try {
			Context.openSession();
			SyncTransmissionResponse response = SyncUtilTransmission.doFullSynchronize(new ReceivingSize(), null);
			Assert.assertEquals(SyncTransmissionState.OK_NOTHING_TO_DO, response.getState());
		}
		finally {
			Context.closeSession();
		}
	}
	
	public void run() {
		try {
			Context.openSession();
			SyncTransmissionResponse response = SyncUtilTransmission.doFullSynchronize(new ReceivingSize(), null);
			Assert.assertEquals(SyncTransmissionState.ERROR_CANNOT_RUN_PARALLEL, response.getState());
		}
		finally {
			Context.closeSession();
		}
	}
	
	@Test
	public void processSyncTransmission_shouldNotThrowNPEWhenGivenInvalidRemoteServerUuid() {
		SyncTransmissionResponse response = SyncUtilTransmission.processSyncTransmission(new SyncTransmission("11111111111",
		        true), null);
		Assert.assertEquals(SyncTransmissionState.CANNOT_FIND_SERVER_WITH_UUID, response.getState());
	}
}
