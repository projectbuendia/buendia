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
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.openmrs.util.PrivilegeConstants;

import java.util.Date;

@SkipBaseSetup
public class SyncLazyLoadingTest extends BaseModuleContextSensitiveTest {

	/**
	 * This test is meant to demonstrate a lazy loading exception in the Interceptor
	 * that was discovered while testing the use of the Options Form (eg. My Profile),
	 * and test against regressions to the fix put in place
	 */
	@Test
	public void testLazyLoading() throws Exception {
		PersonName pn;
		{
			startSession("bwayne");
			User user = Context.getAuthenticatedUser();
			pn = PersonName.newInstance(user.getPersonName());
			pn.setPersonNameId(null);
			stopSession();
		}
		{
			startSession("bwayne");
			Context.addProxyPrivilege(PrivilegeConstants.EDIT_USERS);
			Context.addProxyPrivilege(PrivilegeConstants.VIEW_USERS);

			User u = Context.getAuthenticatedUser();

			PersonName currentName = u.getPersonName();
			currentName.setPreferred(false);
			currentName.setVoided(true);
			currentName.setVoidedBy(Context.getAuthenticatedUser());
			currentName.setDateVoided(new Date());
			currentName.setVoidReason("Changed name on own options form");

			pn.setGivenName("Bruce");
			pn.setMiddleName("Batman");
			pn.setFamilyName("Wayne");
			pn.setPreferred(true);
			u.addName(pn);

			Context.getUserService().saveUser(u, null);

			Assert.assertEquals(u.getCreator(), Context.getUserService().getUser(1));

			stopSession();
		}
	}

	protected void startSession(String username) throws Exception {
		Context.openSession();
		initializeInMemoryDatabase();
		executeDataSet(EXAMPLE_XML_DATASET_PACKAGE_PATH);
		executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
		Context.authenticate(username, "test");
		Context.clearSession();
	}

	protected void stopSession() throws Exception {
		deleteAllData();
		Context.closeSession();
	}
}
