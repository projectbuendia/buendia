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
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.test.annotation.NotTransactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * This test is written in response to a production issue we have discovered, in which somehow an invalid sync record
 * is persisted and sent to the parent.  The problem manifests when the parent attempts to play back this record, finds
 * that the underlying object is invalid (eg. is missing a database required field), and fails to save, yet a sync record
 * for this item saves nonetheless (and saves in such a way that the original uuid is the same as the record uuid, so it
 * appears as a new record to subsequent children, thus causing an effect of bouncing back and forth forever and filling
 * up the sync record table with many invalid, duplicate sync records.
 */
public class SyncInvalidRecordTest extends SyncBaseTest {

	String syncRecordUuid = "e33761c7-25e0-40ca-b14a-58f99320c25e";

	@Override
    public String getInitialDataset() {
        try {
            return "org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	protected List<SyncRecord> getSyncRecords() throws Exception {
		List<SyncRecord> ret = new ArrayList<SyncRecord>();

		User u = Context.getAuthenticatedUser();

		SyncRecord sr = new SyncRecord();
		sr.setUuid(syncRecordUuid);
		sr.setCreator(u.getId().toString());
		sr.setDatabaseVersion(OpenmrsConstants.OPENMRS_VERSION_SHORT);
		sr.setTimestamp(new Date());
		sr.setRetryCount(0);
		sr.setState(SyncRecordState.NEW);
		sr.setContainedClasses("org.openmrs.PatientIdentifier");
		sr.setOriginalUuid(syncRecordUuid);

		SyncItem item = new SyncItem();
		item.setContainedType(PatientIdentifier.class);
		item.setKey(new SyncItemKey<String>(UUID.randomUUID().toString(), String.class));
		item.setState(SyncItemState.NEW);

		StringBuilder content = new StringBuilder();
		content.append("<org.openmrs.PatientIdentifier>");
		content.append("<patient type=\"org.openmrs.Patient\">").append(Context.getPatientService().getPatient(2).getUuid()).append("</patient>");
		content.append("<voided type=\"boolean\">false</voided>");
		content.append("<dateCreated type=\"timestamp\">2014-07-07T10:19:57.940-0400</dateCreated>");
		content.append("<uuid type=\"string\">54d3ca4a-d1ee-421c-a74e-5336c7519888</uuid>");
		content.append("<preferred type=\"boolean\">false</preferred>");
		content.append("<creator type=\"org.openmrs.User\">").append(u.getUuid()).append("</creator>");
		content.append("</org.openmrs.PatientIdentifier>");
		item.setContent(content.toString());
		sr.addItem(item);

		ret.add(sr);
		return ret;
	}

	@Override
	protected void repopulateDB(String xmlFileToExecute) throws Exception {
		super.repopulateDB(xmlFileToExecute);
		Context.addProxyPrivilege("SQL Level Access");
		Context.getAdministrationService().executeSQL("alter table patient_identifier alter column identifier set not null", false);
	}

	@Test
	@NotTransactional
	public void shouldAddInvalidIdentifierTest() throws Exception {
		runSyncTest(new SyncTestHelper() {

			@Override
			public void runOnChild() {
			}

			@Override
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				super.changedBeingApplied(syncRecords, record);

				// Assert that there are no sync records to start with
				assertNumSyncRecords(0);
			}

			@Override
			public void runOnParent() throws Exception {

				// Confirm that the identifier failed to save to the DB due to a validation error
				Patient p = Context.getPatientService().getPatient(2);
				Collection<PatientIdentifier> identifiers = p.getIdentifiers();
				Assert.assertEquals(2, identifiers.size());
				for (PatientIdentifier pi : p.getIdentifiers()) {
					Assert.assertNotSame("54d3ca4a-d1ee-421c-a74e-5336c7519888", pi.getUuid());
				}

				// Since the identifier failed to save, confirm that no sync record was saved either
				assertNumSyncRecords(0);
			}
		});
	}

	protected void assertNumSyncRecords(int num) throws Exception {
		int numFound = Context.getService(SyncService.class).getSyncRecords().size();
		if (num != numFound) {
			System.out.println("Expected " + num + " sync records but found: " + numFound);
			org.openmrs.test.TestUtil.printOutTableContents(getConnection(), "sync_record");
		}
		Assert.assertEquals(num, numFound);
	}
}
