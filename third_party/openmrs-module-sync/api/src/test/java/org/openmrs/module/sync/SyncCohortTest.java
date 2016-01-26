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

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openmrs.Cohort;
import org.openmrs.api.CohortService;
import org.openmrs.api.context.Context;
import org.springframework.test.annotation.NotTransactional;

/**
 * Tests sending cohorts across the wire
 */
public class SyncCohortTest extends SyncBaseTest {
	
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
	public String getParentDataset() {
		return "org/openmrs/module/sync/include/SyncCohortTest.xml";
	}
	
	@Override
	public String getChild2Dataset() {
		return getParentDataset();
	}
	
	@Test
	@NotTransactional
	public void shouldSaveMemberIdsInCohorts() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				CohortService cs = Context.getCohortService();
				
				Cohort cohort = new Cohort();
				cohort.addMember(2);
				cohort.addMember(3);
				cohort.setName("Dummy Name");
				cohort.setDescription("Dummy Desc");
				cs.saveCohort(cohort);
			}
			
			public void runOnParent() throws Exception {
				CohortService cs = Context.getCohortService();
				
				Cohort c = cs.getCohort("Dummy Name");
				assertNotNull("Failed to create the cohort", c);
				
				//TestUtil.printOutTableContents(getConnection(), "patient");
				assertTrue("Failed to transfer cohort members", c.getMemberIds().size() == 2);
				assertTrue("Failed to transfer cohort members with same patient id", c.getMemberIds().contains(3));
				assertFalse("Failed to convert patient id from #2 to #5", c.getMemberIds().contains(2));
				assertTrue("Failed to convert patient id from #2 to #5", c.getMemberIds().contains(5));
			}
		});
	}
	
}
