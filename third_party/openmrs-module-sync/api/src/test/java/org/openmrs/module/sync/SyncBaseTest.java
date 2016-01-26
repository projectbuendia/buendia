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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.After;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.advice.GenerateSystemIdAdvisor;
import org.openmrs.module.sync.advice.SaveConceptAdvice;
import org.openmrs.module.sync.advice.SavePatientAdvice;
import org.openmrs.module.sync.api.SyncIngestService;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.FilePackage;
import org.openmrs.module.sync.serialization.IItem;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sets up common routines and initialization for all sync tests. Note for all sync tests: MUST MARK
 * AS NotTransctional so that Tx that is created in runOnChild() method is committed upon exit of
 * that method. Note: org.springframework.transaction.annotation.Propagation.REQUIRES_NEW doesn't
 * help since on most EDBMS systems it doesn't do what spec says
 */
public abstract class SyncBaseTest extends BaseModuleContextSensitiveTest {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	public DateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
	
	public abstract String getInitialDataset();

	/**
	 * This provides a mechanism for subclasses to indicate if they require a certain
	 * minimum version of OpenMRS to run, for example if a core bug was fixed that the test requires
	 * This is optional and by default the test will run for any supported OpenMRS version
	 */
	public String minimumRequiredOpenmrsVersion() {
		return null;
	}
	
	/**
	 * The dataset to run after {@link SyncTestHelper#runOnChild()} is called but before
	 * {@link SyncTestHelper#runOnParent()}
	 * 
	 * @return string or null if no dataset should execute
	 */
	public String getParentDataset() {
		return null;
	}
	
	/**
	 * The dataset to run after {@link SyncTestHelper#runOnParent()} is called, but before (the
	 * optional) {@link SyncTestHelper#runOnChild2()}
	 * 
	 * @return string or null if no dataset should execute
	 */
	public String getChild2Dataset() {
		return null;
	}
	
	@Override
	public void baseSetupWithStandardDataAndAuthentication() throws Exception {
		// Do nothing
	}
	
	@Transactional
	@Rollback(false)
	protected void runOnChild(SyncTestHelper testMethods) throws Exception {
		log.info("\n************************************* Running On Child *************************************");
		testMethods.runOnChild();
	}
	
	@Transactional
	@Rollback(false)
	protected void runOnParent(SyncTestHelper testMethods) throws Exception {
		authenticate();
		//now run parent
		log.info("\n************************************* Running on Parent *************************************");
		testMethods.runOnParent();
	}
	
	//this is final step so do rollback and let the test finish
	@Transactional
	protected void runOnChild2(SyncTestHelper testMethods) throws Exception {
		authenticate();
		
		//now run second child
		log.info("\n************************************* Running on Child2 *************************************");
		testMethods.runOnChild2();
	}
	
	/**
	 * Sets up initial data set before set of instructions simulating child changes is executed.
	 * 
	 * @see #runOnChild(SyncTestHelper)
	 * @see #runSyncTest(SyncTestHelper)
	 * @throws Exception
	 */
	@Transactional
	@Rollback(false)
	protected void beforeRunOnChild() throws Exception {
		Context.openSession();
		deleteAllData();
        try {
            executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
		executeDataSet("org/openmrs/module/sync/include/SyncRemoteChildServer.xml");
		executeDataSet(getInitialDataset());
		
		authenticate();
		
		// load the advice for the sync module.
		// this is kind of hacky, but loading up the module here has adverse effects (sqldiff tries to run)
		Context.addAdvisor(UserService.class, new GenerateSystemIdAdvisor());
		Context.addAdvice(PatientService.class, new SavePatientAdvice());
		Context.addAdvice(ConceptService.class, new SaveConceptAdvice());
	}


	/**
	 * Retrieves sync records currently stored in the sync_record.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Transactional
	@Rollback(false)
	protected List<SyncRecord> getSyncRecords() throws Exception {
		
		//get sync records created
		List<SyncRecord> syncRecords = Context.getService(SyncService.class).getSyncRecords();
		if (syncRecords == null || syncRecords.size() == 0) {
			assertFalse("No changes found (i.e. sync records size is 0)", true);
		}

		return syncRecords;
	}
	
	@Transactional
	@Rollback(false)
	protected void repopulateDB(String xmlFileToExecute) throws Exception {
		
		Context.clearSession();
		
		//reload db from scratch
		log.info("\n************************************* Reload Database *************************************");
		deleteAllData();
        try {
            executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
		
		if (xmlFileToExecute == null)
			xmlFileToExecute = "org/openmrs/module/sync/include/SyncRemoteChildServer.xml";
		executeDataSet(xmlFileToExecute);
				
		return;
	}

	
	@Transactional
	@Rollback(false)
	protected void applySyncChanges(List<SyncRecord> syncRecords, SyncTestHelper testMethods) throws Exception {
		//Context.openSession();
						
		authenticate();
		
		log.info("\n************************************* Sync Record(s) to Process *************************************");
		FilePackage pkg = new FilePackage();
		Record record = pkg.createRecordForWrite("SyncTest");
		Item top = record.getRootItem();
		for (SyncRecord syncRecord : syncRecords) {
			((IItem) syncRecord).save(record, top);
		}
		try {
			log.info("Sync record:\n" + record.toString());
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Serialization failed with an exception: " + e.getMessage());
		}
		
		// run some asserts on the records that are going to be applied
		testMethods.changedBeingApplied(syncRecords, record);
		
		log.info("\n************************************* Processing Sync Record(s) *************************************");
		RemoteServer origin = Context.getService(SyncService.class).getRemoteServer(1); //"46b16ac6-144e-102b-8d9c-e44ed545d86c");
		for (SyncRecord syncRecord : syncRecords) {
			Context.getService(SyncIngestService.class).processSyncRecord(syncRecord, origin);
		}
		
		return;
	}
	
	/**
	 * Executes the sync test workflow: <br/>
	 * 1. prepopulate DB <br/>
	 * 2. Execute set of instructions simulating sync child <br/>
	 * 3. Fetch sync records, re-initialize DB for parent and then apply the sync records <br/>
	 * 4. Execute set of instructions simulating sync parent; typically just asserts to ensure child
	 * changes came across. <br/>
	 * Note: The non-transactional vs. transactional behavior of helper methods: each step must be
	 * in its own Tx since sync flushes its sync record at Tx boundary. Consequently it is required
	 * for the overall test to run as non-transactional and each individual step to be
	 * transactional; as stated in class comments, true nested transactions are RDBMS fantasy, it
	 * mostly doesn't exist.
	 * 
	 * @param testMethods helper object holding methods for child and parent execution
	 * @throws Exception
	 */
	@NotTransactional
	public void runSyncTest(SyncTestHelper testMethods) throws Exception {

		if (!TestUtil.isOpenmrsVersionAtLeast(minimumRequiredOpenmrsVersion())) {
			System.out.println("Test: " + getClass() + " ignored as it is only applicable for OpenMRS versions >= " + minimumRequiredOpenmrsVersion());
			return;
		}

		this.beforeRunOnChild();
		
		this.runOnChild(testMethods);

		List<SyncRecord> firstChanges = this.getSyncRecords();
		
		this.repopulateDB(getParentDataset());

		try {
			this.applySyncChanges(firstChanges, testMethods);
		}
		catch (Exception e) {
			log.warn("An error occurred applying sync changes to parent: " + e.getMessage());
		}
		
		this.runOnParent(testMethods);

		//now that parent is committed; replay the parent's log against child #2
		//after that is done, the data should be the same again
		List<SyncRecord> secondChanges = this.getSyncRecords();
		this.repopulateDB(getChild2Dataset());

		try {
			this.applySyncChanges(secondChanges, testMethods);
		}
		catch (Exception e) {
			log.warn("An error occurred applying sync changes to child2: " + e.getMessage());
		}
		
		//now finish by checking the changes recorded on parent against the target state
		this.runOnChild2(testMethods);
	}
	
	/**
	 * Delete everything after each method so that non-sync tests work just fine off the normal
	 * database
	 * 
	 * @throws Exception
	 */
	@After
	public void cleanupDatabase() throws Exception {
		deleteAllData();
	}

	@Override
	public void executeDataSet(String datasetFilename) throws Exception {

		String xml = null;
		InputStream fileInInputStreamFormat = null;
		try {
			File file = new File(datasetFilename);
			if (file.exists())
				fileInInputStreamFormat = new FileInputStream(datasetFilename);
			else {
				fileInInputStreamFormat = getClass().getClassLoader().getResourceAsStream(datasetFilename);
				if (fileInInputStreamFormat == null)
					throw new FileNotFoundException("Unable to find '" + datasetFilename + "' in the classpath");
			}
			xml = IOUtils.toString(fileInInputStreamFormat, "UTF-8");
		}
		finally {
			IOUtils.closeQuietly(fileInInputStreamFormat);
		}

		if (OpenmrsConstants.OPENMRS_VERSION_SHORT.compareTo("1.9.2") < 0) {
			xml = xml.replace("urgency=\"STAT\" ", "");
		}

		StringReader reader = new StringReader(xml);
		ReplacementDataSet replacementDataSet = new ReplacementDataSet(new FlatXmlDataSet(reader, false, true, false));
		replacementDataSet.addReplacementObject("[NULL]", null);

		executeDataSet(replacementDataSet);
	}
}
