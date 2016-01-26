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

import org.junit.Test;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncSourceJournal;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.api.context.Context;
import java.util.List;

/**
 * Tests various methods of the SyncSourceJournal class.
 */
public class SyncSourceJournalTest extends BaseModuleContextSensitiveTest {

	@Test
	public void getChanged_shouldReturnChangedRecordsByServer() throws Exception {
		executeDataSet("org/openmrs/module/sync/include/SyncRecords.xml");
		executeDataSet("org/openmrs/module/sync/include/SyncRemoteChildServer.xml");

		RemoteServer parent = Context.getService(SyncService.class).getParentServer();
		List<SyncRecord> recordList = new SyncSourceJournal().getChanged(parent, 1000);
		Assert.assertEquals(recordList.size(), 59);
	}
}