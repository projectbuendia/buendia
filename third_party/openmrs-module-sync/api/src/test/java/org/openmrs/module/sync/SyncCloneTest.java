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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.test.BaseContextSensitiveTest;

/**
 * 
 */
@Ignore
public class SyncCloneTest extends BaseContextSensitiveTest {
	
	@Override
	public Boolean useInMemoryDatabase() {
		return false;
	}
	
	/**
	 * @see org.openmrs.synchronization.engine.SyncBaseTest#getInitialDataset()
	 */
	@Before
	public void setupInitialDataset() throws Exception {
        try {
            executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
	}

	@Test
	public void testJDBCClone() throws Exception {
		SyncService syncService = Context.getService(SyncService.class);

		File dir = SyncUtil.getSyncApplicationDir();
		File fileOne = new File(dir, SyncConstants.CLONE_IMPORT_FILE_NAME
		        + SyncConstants.SYNC_FILENAME_MASK.format(new Date())
		        + "_one.sql");
		syncService.exportChildDB(null, new FileOutputStream(fileOne));
		long checksumOne = checksum(fileOne);
		
		deleteAllData();
		
		syncService.importParentDB(new FileInputStream(fileOne));
		File fileTwo = new File(dir, SyncConstants.CLONE_IMPORT_FILE_NAME
		        + SyncConstants.SYNC_FILENAME_MASK.format(new Date())
		        + "_two.sql");
		syncService.exportChildDB(null, new FileOutputStream(fileTwo));
		long checksumTwo = checksum(fileTwo);
		
		assertTrue("Failed to validate the checksum for the two sync clonedumps",
		           checksumOne == checksumTwo);
	}

	@Test
	public void testMySqlDump() throws Exception {
		SyncService syncService = Context.getService(SyncService.class);
		File fileOne = syncService.generateDataFile();
		long checksumOne = checksum(fileOne);
		
		deleteAllData();
		
		syncService.execGeneratedFile(fileOne);
		File fileTwo =syncService.generateDataFile();
		long checksumTwo = checksum(fileTwo);
		
		assertTrue("Failed to validate the checksum for the two sync clone dumps",
		           checksumOne == checksumTwo);
		
	}

	/*
	 * Calculate the checksum on a file located at <b>fileName</b>
	 * 
	 * @return checksum on that file
	 * 
	 * @param fileName the location of the file
	 * 
	 * @throws Exception
	 */
	public long checksum(File file) throws Exception {
		long checksum;
		byte[] buff = new byte[1024];
		CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file),
		                                                new Adler32());
		while (cis.read(buff) >= 0)
			;
		checksum = cis.getChecksum().getValue();
		return checksum;
	}
}
