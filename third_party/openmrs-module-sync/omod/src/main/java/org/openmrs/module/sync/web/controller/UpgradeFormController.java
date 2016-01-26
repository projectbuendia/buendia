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

import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.ModuleUtil;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller behind the upgrades form controller
 */
@Controller
public class UpgradeFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/module/sync/upgrade", method = RequestMethod.GET)
	public void showThePage(ModelMap modelMap) throws Exception {
		
		List<String> fromOptions = new Vector<String>();
		// only make 1.5.* an option if running at least 1.6.0
		if (ModuleUtil.compareVersion(OpenmrsConstants.OPENMRS_VERSION_SHORT, "1.6.0") >= 0) {
			fromOptions.add("1.5.*");
		}
		
		modelMap.put("fromOptions", fromOptions);
	}
	
	/**
	 * A user has hit the submit button and we are now printing out the sql to be used on the child db
	 * 
	 * @param response the http response
	 * @param session the current http session
	 * @param fromVersion the user chosen fromVersion (provided by the showThePage method in the
	 *            fromOptions list
	 * @return the page to redirect to or output is written directly to the HttpServletResponse
	 *         printWriter
	 * @throws Exception
	 */
	@RequestMapping(value = "/module/sync/upgrade", method = RequestMethod.POST)
	public String printUpgradeScript(HttpServletResponse response, HttpSession session, @RequestParam String fromVersion) throws Exception {
		
		// used to know if we need to redirect the user to the jsp again with a message
		boolean upgradePrinted = false;
		// only one call to response.getWriter is allowed
		PrintWriter writer = null;
		
		if ("1.5.*".equals(fromVersion) && ModuleUtil.compareVersion(OpenmrsConstants.OPENMRS_VERSION_SHORT, "1.6.0") >= 0) {
			upgradePrinted = true;
			if (writer == null)
				writer = response.getWriter();
			
			printUserUuidAdditions(writer);
		}
		
		// nothing was printed, so the user will be sent back to upgrade.form
		if (!upgradePrinted) {
			session.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.upgrade.notneeded");
			return "redirect:/module/sync/upgrade.form";
		}
		else
			return null;
	}
	
	/**
	 * 
	 * 
	 * @param writer
	 */
	private void printUserUuidAdditions(PrintWriter writer) {
		writer.println("-- Setting uuids on Users table for 1.5.* database upgrade to 1.6.0 database");
		writer.println("-- This should be run on the child database BEFORE upgrading it to 1.6.0");
		writer.println("-- The command to run this file is: mysql -u -p -e\"source thisfilename.sql\" openmrs  (Assuming openmrs is the name of your database)");
		writer.println("");
		writer.println("ALTER TABLE users ADD uuid CHAR(38);");
		
		for (User u : Context.getUserService().getAllUsers()) {
			writer.println("update users set uuid = '" + u.getUuid() + "' where user_id = " + u.getUserId() + ";");
		}
		
		writer.println("ALTER TABLE users MODIFY uuid char(38) NOT NULL;");
	}
	
}
