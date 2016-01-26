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
package org.openmrs.module.sync.web.extension;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.web.extension.AdministrationSectionExt;
import org.openmrs.util.PrivilegeConstants;

/**
 * Displays the admin section on the admin index page
 */
public class AdminList extends AdministrationSectionExt {
	
	@Override
	public Map<String, String> getLinks() {
		
		Map<String, String> links = new LinkedHashMap<String, String>();
		
		if (Context.hasPrivilege("View Synchronization Records")) {
			links.put("module/sync/overview.htm", "sync.overview.title");
			links.put("module/sync/config.list", "sync.config.title");
			links.put("module/sync/statistics.list", "sync.statistics.title");
			links.put("module/sync/history.list", "sync.history.title");
			links.put("module/sync/maintenance.form", "sync.maintenance.title");
		}

		if (Context.hasPrivilege(PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES)) {
			links.put("module/sync/emailConfig.form", "sync.emailConfig.title");
		}
		
		if (Context.hasPrivilege("Manage Synchronization")) {
			links.put("module/sync/upgrade.form", "sync.upgrade.title");
		}
		
		links.put("module/sync/help.htm", "sync.help.title");
		
		return links;
	}
	
	@Override
	public String getTitle() {
		return "sync.header";
	}
	
}
