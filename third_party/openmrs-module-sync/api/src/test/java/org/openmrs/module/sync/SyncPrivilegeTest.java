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
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.springframework.test.annotation.NotTransactional;

/**
 * Testing syncing of the {@link Privilege} object
 */
public class SyncPrivilegeTest extends SyncBaseTest {
	
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
	public void shouldTransferPrivilege() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				UserService us = Context.getUserService();
				
				Privilege p = new Privilege("a priv", "a desc");
				us.savePrivilege(p);
			}
			
			public void runOnParent() throws Exception {
				UserService us = Context.getUserService();
				
				Assert.assertEquals("a desc", us.getPrivilege("a priv").getDescription());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldIgnoreModifiedPrivilegeWithDifferentUuidOnParent() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				UserService us = Context.getUserService();
				
				Privilege p = us.getPrivilege("Add Concepts");
				p.setDescription("a new description");
				us.savePrivilege(p);
			}
			
			public void runOnParent() throws Exception {
				UserService us = Context.getUserService();
				
				// THESE SHOULD NOT be the same because the parent one has a different uuid and so no updates are applied
				Assert.assertNotSame("a new description", us.getPrivilege("Add Concepts").getDescription());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldAddPrivilegeWithDifferentUuidToRole() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				UserService us = Context.getUserService();
				Role r = us.getRole("Provider");
				
				// sanity check
				Assert.assertEquals(false, r.hasPrivilege("Add Concepts"));
				
				Privilege p = us.getPrivilege("Add Concepts");
				r.addPrivilege(p);
				us.saveRole(r);
			}
			
			public void runOnParent() throws Exception {
				UserService us = Context.getUserService();
				
				Role r = us.getRole("Provider");
				Assert.assertEquals(true, r.hasPrivilege("Add Concepts"));
			}
		});
	}
	
	
	
}
