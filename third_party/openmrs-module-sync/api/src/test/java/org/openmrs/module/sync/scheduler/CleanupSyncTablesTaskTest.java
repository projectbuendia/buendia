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
package org.openmrs.module.sync.scheduler;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Tests the {@link CleanupSyncTablesTask} to make sure it deletes sync records
 */
public class CleanupSyncTablesTaskTest extends BaseModuleContextSensitiveTest {
	
	@Test
	public void shouldDeleteOnlyTheStatesGiven() throws Exception {
		
		executeDataSet("org/openmrs/module/sync/include/SyncRecords.xml");
		
		SyncService syncService = Context.getService(SyncService.class);
		
		// sanity check
		List<SyncRecord> records = syncService.getSyncRecords();
		Assert.assertEquals(60, records.size());
		
		CleanupSyncTablesTask task = new CleanupSyncTablesTask();
		
		TaskDefinition td = new TaskDefinition();
		td.setProperty(CleanupSyncTablesTask.PROPERTY_STATES_TO_DELETE, "NOT_SUPPOSED_TO_SYNC");
		task.initialize(td);
		
		task.execute();
		
		// because task.execute closes the session again
		Context.clearSession();
		Context.openSession();
		records = syncService.getSyncRecords();
		
		// there are 60 records and only 1 of them are NOT_SUPPOSED_TO_SYNC, we are telling 
		// the task to only delete that 1
		Assert.assertEquals(59, records.size());
	}
	
}
