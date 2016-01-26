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
package org.openmrs.module.sync.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;

/**
 * Tests for the {@link ConfigCurrentServerFormController}
 */
@Controller
public class ConfigCurrentServerFormControllerTest extends BaseModuleContextSensitiveTest {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	@Test
	public void shouldSaveCurrentServerSettings() throws Exception {
		SyncService syncService = Context.getService(SyncService.class);
		AdministrationService as = Context.getAdministrationService();
		
		// sanity check
		Assert.assertNotSame("new server name", syncService.getServerName());
		Assert.assertNull(syncService.getServerUuid());
		Assert.assertNull(syncService.getAdminEmail());
		
		ConfigCurrentServerFormController controller = new ConfigCurrentServerFormController();
		
		controller.onSaveSettings("new server name", "some uuid", "the server email address", 97, 98, 99, 100, new MockHttpSession());
		
		Assert.assertNotNull(syncService.getServerName());
		Assert.assertNotNull(syncService.getServerUuid());
		Assert.assertNotNull(syncService.getAdminEmail());
		Assert.assertEquals("97", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS));
		Assert.assertEquals("98", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RECORDS_WEB));
		Assert.assertEquals("99", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RECORDS_FILE));
		Assert.assertEquals("100", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT));
	}
	
}
