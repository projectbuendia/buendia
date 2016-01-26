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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.springframework.test.annotation.NotTransactional;

/**
 * Testing syncing of the {@link Role} object
 */
public class SyncRoleTest extends SyncBaseTest {
	
	@Override
	public String getInitialDataset() {
        try {
            return "org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
	}
	
	public String getParentDataset() {
		return "org/openmrs/module/sync/include/SyncPrivilegeTest.xml";
	}
	
	@Test
	@NotTransactional
	public void shouldAddRoleToUserWithDifferentUuid() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				UserService us = Context.getUserService();
				User u = us.getUser(1);
				Role r = us.getRole("Provider");
				
				// sanity check
				Assert.assertEquals(false, u.hasRole("Provider", true));
				u.addRole(r);
				us.saveUser(u, null);
			}
			
			public void runOnParent() throws Exception {
				UserService us = Context.getUserService();
				
				User u = us.getUser(1);
				Assert.assertEquals(true, u.hasRole("Provider", true));
			}
		});
	}
	
	
	
}
