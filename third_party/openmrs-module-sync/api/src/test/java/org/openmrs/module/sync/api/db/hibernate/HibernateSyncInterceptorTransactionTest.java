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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.EncounterType;
import org.openmrs.VisitType;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.TestUtil;
import org.openmrs.module.sync.test.ExampleTransactionalService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;

/**
 * Tests transaction handling of the HibernateSyncInterceptor
 */
public class HibernateSyncInterceptorTransactionTest extends BaseModuleContextSensitiveTest {

	protected final Log log = LogFactory.getLog(getClass());

	private static final String ENCOUNTER_TYPE = "Test Encounter Type";
	private static final String VISIT_TYPE = "Visit Type";
	private static final String EXCEPTION_MESSAGE = "Test Exception";

	@Autowired
	ExampleTransactionalService testService;

	@Before
	@Override
	public void baseSetupWithStandardDataAndAuthentication() throws Exception {
	}

	@After
	@Override
	public void deleteAllData() throws Exception {
	}

	@BeforeTransaction
	public void beforeTx() throws Exception {
		Context.openSession();
		initializeInMemoryDatabase();
		executeDataSet(EXAMPLE_XML_DATASET_PACKAGE_PATH);
		executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
		authenticate();
	}

	@AfterTransaction
	public void afterTx() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("====== ENCOUNTER_TYPE =========");
			log.debug(getTableContents("encounter_type"));
			log.debug("====== VISIT_TYPE =========");
			log.debug(getTableContents("visit_type"));
			log.debug("====== SYNC_RECORD =========");
			log.debug(getTableContents("sync_record"));
		}
		if (checkTableContains("encounter_type", ENCOUNTER_TYPE)) {
			Assert.assertTrue("Encounter Type saved, but Sync Record was not", checkTableContains("sync_record", ENCOUNTER_TYPE));
		}
		else {
			Assert.assertFalse("Encounter Type not saved, but Sync Record was", checkTableContains("sync_record", ENCOUNTER_TYPE));
		}
		if (checkTableContains("visit_type", VISIT_TYPE)) {
			Assert.assertTrue("Visit Type saved, but Sync Record was not", checkTableContains("sync_record", VISIT_TYPE));
		}
		else {
			Assert.assertFalse("Visit Type not saved, but Sync Record was", checkTableContains("sync_record", VISIT_TYPE));
		}
		super.deleteAllData();
	}

	@Test
	@Transactional
	@Rollback(false)
	public void shouldTestAllInSameTx() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName(ENCOUNTER_TYPE);
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectInTransaction(encounterType);
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName(VISIT_TYPE);
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInTransaction(visitType);
		}
	}

	@Test
	@Transactional
	@Rollback(false)
	public void shouldTestFirstInNewTx() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName(ENCOUNTER_TYPE);
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectInNewTransaction(encounterType);
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName(VISIT_TYPE);
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInTransaction(visitType);
		}
	}

	@Test
	@Transactional
	@Rollback(false)
	public void shouldTestSecondInNewTx() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName(ENCOUNTER_TYPE);
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectInTransaction(encounterType);
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName(VISIT_TYPE);
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInNewTransaction(visitType);
		}
	}

	@Test
	@Transactional
	@Rollback(false)
	public void shouldTestBothInNewTx() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName(ENCOUNTER_TYPE);
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectInNewTransaction(encounterType);
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName(VISIT_TYPE);
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInNewTransaction(visitType);
		}
	}

	@Test
	@Transactional
	@Rollback(false)
	public void shouldTestFirstInNoTransaction() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName(ENCOUNTER_TYPE);
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectNoTransaction(encounterType);
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName(VISIT_TYPE);
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInTransaction(visitType);
		}
	}

	@Test
	@NotTransactional
	public void shouldTestFirstThrowsException() throws Exception {
		beforeTx();
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName(ENCOUNTER_TYPE);
			encounterType.setDescription("Encounter Type Description");
			String exceptionMessage = "";
			try {
				testService.saveObjectInTransactionWithException(encounterType);
			}
			catch (IllegalArgumentException e) {
				exceptionMessage = e.getMessage();
			}
			Assert.assertEquals(EXCEPTION_MESSAGE, exceptionMessage);
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName(VISIT_TYPE);
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInTransaction(visitType);
		}
		afterTx();
	}

	@Test
	@NotTransactional
	public void shouldTestSecondThrowsException() throws Exception {
		beforeTx();
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName(ENCOUNTER_TYPE);
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectInTransaction(encounterType);
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName(VISIT_TYPE);
			visitType.setDescription("Visit Type Description");
			String exceptionMessage = "";
			try {
				testService.saveObjectInTransactionWithException(visitType);
			}
			catch (IllegalArgumentException e) {
				exceptionMessage = e.getMessage();
			}
			Assert.assertEquals(EXCEPTION_MESSAGE, exceptionMessage);
		}
		afterTx();
	}

	public String getTableContents(String tableName) throws Exception {
		StringWriter writer = new StringWriter();
		IDatabaseConnection connection = new DatabaseConnection(getConnection());
		DatabaseConfig config = connection.getConfig();
		config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
		QueryDataSet outputSet = new QueryDataSet(connection);
		outputSet.addTable(tableName);
		FlatXmlDataSet.write(outputSet, writer);
		return writer.toString();
	}

	public boolean checkTableContains(String tableName, String testValue) throws Exception {
		return getTableContents(tableName).contains(testValue);
	}
}
