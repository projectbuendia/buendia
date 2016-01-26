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
package org.openmrs.module.sync.api.db.hibernate;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncBaseTest;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncTestHelper;
import org.openmrs.module.sync.TestUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.test.ExampleTransactionalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.NotTransactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Tests methods in HibernateSyncInterceptor
 */
public class HibernateSyncInterceptorTest extends SyncBaseTest {

	@Autowired
	ExampleTransactionalService testService;

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
		return Context.getService(SyncService.class).getSyncRecords();
	}

	@Test
	@NotTransactional
	public void shouldNotSaveAnySyncRecordsForAReadOnlyTransaction() throws Exception {
		runSyncTest(new SyncTestHelper() {
			public void runOnChild() throws Exception {
				Patient p = testService.getObject(Patient.class, 2);
				Assert.assertEquals(2, p.getPatientId().intValue());
			}
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				Assert.assertEquals(0, syncRecords.size());
			}
			public void runOnParent() throws Exception {

			}
		});
	}

	@Test
	@NotTransactional
	public void shouldSaveOneSyncRecordForOneTransaction() throws Exception {
		runSyncTest(new SyncTestHelper() {
			public void runOnChild() throws Exception {
				Patient p = testService.getObject(Patient.class, 2);
				p.getPersonName().setFamilyName("Smith");
				testService.saveObjectInTransaction(p);
			}
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				Assert.assertEquals(1, syncRecords.size());
				SyncRecord r = syncRecords.get(0);
				for (SyncItem item : r.getItems()) {
					if (PersonName.class.isAssignableFrom(item.getContainedType())) {
						Assert.assertTrue(item.getContent().contains("Smith"));
					}
				}
			}
			public void runOnParent() throws Exception {

			}
		});
	}

	@Test
	@NotTransactional
	public void shouldSaveTwoSyncRecordsForTwoSeparateTransactions() throws Exception {
		runSyncTest(new SyncTestHelper() {
			public void runOnChild() throws Exception {
				Patient p = testService.getObject(Patient.class, 2);
				p.setBirthdate(getDate("1978-08-29"));
				testService.saveObjectInTransaction(p);

				Encounter e = testService.getObject(Encounter.class, 1);
				e.setEncounterDatetime(new Date());
				testService.saveObjectInTransaction(e);
			}
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				Assert.assertEquals(2, syncRecords.size());
			}
			public void runOnParent() throws Exception {

			}
		});
	}

	@Test
	@NotTransactional
	public void shouldSaveOneSyncRecordForTwoNestedDefaultTransactions() throws Exception {
		runSyncTest(new SyncTestHelper() {
			public void runOnChild() throws Exception {
				Patient p = testService.getObject(Patient.class, 2);
				p.setGender("F");
				Encounter e = testService.getObject(Encounter.class, 1);
				e.setEncounterDatetime(new Date());
				testService.saveAllObjectsInSingleTransaction(p, e);
			}
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				Assert.assertEquals(1, syncRecords.size());
			}
			public void runOnParent() throws Exception {

			}
		});
	}

	@Test
	@NotTransactional
	@Ignore // This currently fails
	public void shouldSaveTwoSyncRecordsForTwoNestedNewTransactions() throws Exception {
		runSyncTest(new SyncTestHelper() {
			public void runOnChild() throws Exception {
				Patient p = testService.getObject(Patient.class, 3);
				p.setBirthdate(getDate("1980-02-05"));
				Encounter e = testService.getObject(Encounter.class, 1);
				e.setEncounterDatetime(getDate("2006-12-15"));
				testService.saveAllObjectsInNewTransactions(p, e);
			}
			public void runOnParent() throws Exception {
				Patient p = testService.getObject(Patient.class, 3);
				Assert.assertEquals("1980-02-05", formatDate(p.getBirthdate()));
				Encounter e = testService.getObject(Encounter.class, 1);
				Assert.assertEquals("2006-12-15", formatDate(e.getEncounterDatetime()));
			}
		});
	}

	private Date getDate(String ymd) {
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			return df.parse(ymd);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String formatDate(Date d) {
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			return df.format(d);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
