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
package org.openmrs.module.sync.api;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.EncounterType;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncBaseTest;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncItemKey;
import org.openmrs.module.sync.SyncItemState;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.TestUtil;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.test.annotation.NotTransactional;

/**
 * Tests methods in the SyncIngestService
 */
public class SyncIngestServiceTest extends SyncBaseTest {

	@Override
	public String getInitialDataset() {
		return null;
	}

	@Before
	public void before() throws Exception {
		if (!Context.isSessionOpen()) {
			Context.openSession();
		}
		initializeInMemoryDatabase();
		executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
		executeDataSet("org/openmrs/module/sync/include/SyncRemoteChildServer.xml");
		authenticate();
		Context.clearSession();
	}

	/**
	 * @see {@link SyncService#getOpenmrsObjectByUuid(Class,String)}
	 */
	@Test
	@Verifies(value = "should create sync import record if successful", method = "processSyncRecord(SyncRecord,RemoteServer)")
	@NotTransactional
	public void processSyncRecord_shouldCreateSyncImportRecordIfSuccessful() throws Exception {

		RemoteServer parent = Context.getService(SyncService.class).getParentServer();
		Assert.assertNotNull(parent);

		SyncRecord record = createValidSyncRecord();
		Context.getService(SyncIngestService.class).processSyncRecord(record, parent);

		SyncImportRecord importRecord = Context.getService(SyncService.class).getSyncImportRecord(record.getOriginalUuid());
		Assert.assertNotNull(importRecord);
		Assert.assertEquals(SyncRecordState.COMMITTED, importRecord.getState());
		//org.openmrs.test.TestUtil.printOutTableContents(getConnection(), "sync_import");
	}

	/**
	 * @see {@link SyncService#getOpenmrsObjectByUuid(Class,String)}
	 */
	@Ignore("This test is written to demonstrate the issue reported in SYNC-310, and should be activated as that ticket is addressed")
	@Test
	@Verifies(value = "should create sync inmport records if error occurs", method = "processSyncRecord(SyncRecord,RemoteServer)")
	@NotTransactional
	public void processSyncRecord_shouldCreateSyncImportRecordIfErrorOccurs() throws Exception {

		RemoteServer parent = Context.getService(SyncService.class).getParentServer();
		Assert.assertNotNull(parent);

		SyncRecord record = createValidSyncRecord();

		// Setting containedType to null will lead to an exception.  This is what will currently
		// happen if you try to import an object from one server that another server doesn't know about
		// eg. if you have a module that saves it's own OpenmrsObjects installed on the parent but not the child
		record.getItems().iterator().next().setContainedType(null);

		boolean exceptionThrown = false;
		try {
			Context.getService(SyncIngestService.class).processSyncRecord(record, parent);
		}
		catch (Exception e) {
			exceptionThrown =  true;
		}
		Assert.assertTrue(exceptionThrown);

		SyncImportRecord importRecord = Context.getService(SyncService.class).getSyncImportRecord(record.getOriginalUuid());
		Assert.assertNotNull(importRecord);
		Assert.assertEquals(SyncRecordState.FAILED, importRecord.getState());
		//org.openmrs.test.TestUtil.printOutTableContents(getConnection(), "sync_import");
	}
	
	/**
	 * @see {@link SyncIngestService#processSyncRecord(SyncRecord,RemoteServer)}
	 */
	@Test
	@Verifies(value = "should log the full stacktrace when it fails", method = "processSyncRecord(SyncRecord,RemoteServer)")
	public void processSyncRecord_shouldLogTheFullStacktraceWhenItFails() throws Exception {
		final String recordUuid = "someRandomUuid";
		Throwable t = null;
		try {
			SyncIngestService sis = Context.getService(SyncIngestService.class);
			SyncRecord record = new SyncRecord();
			record.setOriginalUuid(recordUuid);
			//This should force a NPE since server is null
			sis.processSyncRecord(record, null);
		}
		catch (Exception e) {
			//since sync re throws the exception as a SyncIngestException, 
			//get the actual NPE exception that was thrown
			t = e.getCause();
		}
		
		Assert.assertNotNull(t);
		Assert.assertTrue(StringUtils.isBlank(t.getMessage()));
		Assert.assertTrue(t instanceof NullPointerException);
		
		SyncImportRecord importRecord = Context.getService(SyncService.class).getSyncImportRecord(recordUuid);
		
		Assert.assertNotNull(importRecord);
		Assert.assertFalse(StringUtils.isBlank(importRecord.getErrorMessage()));
	}

	protected SyncRecord createValidSyncRecord() {
		SyncRecord record = new SyncRecord();
		record.setUuid(UUID.randomUUID().toString());
		record.setOriginalUuid(UUID.randomUUID().toString());
		record.setState(SyncRecordState.NEW);
		record.setContainedClasses("org.openmrs.EncounterType");
		record.setDatabaseVersion(OpenmrsConstants.OPENMRS_VERSION_SHORT);
		record.setRetryCount(0);
		record.setTimestamp(new Date());

		SyncItem item = new SyncItem();
		item.setContainedType(EncounterType.class);
		item.setKey(new SyncItemKey<String>(UUID.randomUUID().toString(), String.class));
		item.setContent("<org.openmrs.EncounterType><description type=\"string\">Test Encounter Type</description><name type=\"string\">Test Encounter Type</name><retired type=\"boolean\">false</retired><dateCreated type=\"timestamp\">2013-03-22T18:29:26.249-0400</dateCreated><uuid type=\"string\">e5b4b20b-da7f-4e07-9201-5be196c13585</uuid><creator type=\"org.openmrs.User\">873786be-17b8-4284-8a1e-66c479dd119f</creator></org.openmrs.EncounterType>");
		item.setState(SyncItemState.NEW);
		record.addItem(item);

		return record;
	}
}