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
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.GlobalProperty;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.sync.SyncMailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for viewing, configuring, and testing server email settings
 */
@Controller
public class EmailConfigController {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	@Qualifier(value="adminService")
	AdministrationService administrationService;

	@RequestMapping("/module/sync/emailConfig")
	public void view(ModelMap model) {
		if (Context.isAuthenticated()) {
			Map<String, String> settings = SyncMailUtil.getCurrentlyConfiguredSettings();
			model.addAttribute("settings", settings);
		}
	}

	@RequestMapping("/module/sync/saveEmailConfiguration")
	public String saveSettings(HttpServletRequest request) {
		if (Context.isAuthenticated()) {
			for (String s : SyncMailUtil.getMailGlobalProperties()) {
				GlobalProperty p = administrationService.getGlobalPropertyObject(s);
				String propertyValue = ServletRequestUtils.getStringParameter(request, s, "");
				p.setPropertyValue(propertyValue);
				administrationService.saveGlobalProperty(p);
			}
		}
		return "redirect:/module/sync/emailConfig.form";
	}

	@RequestMapping("/module/sync/validateEmailSettings")
	public void validateSettings(ModelMap model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, String> settings = new HashMap<String, String>();
		for (String s : SyncMailUtil.getMailGlobalProperties()) {
			String propertyValue = ServletRequestUtils.getStringParameter(request, s, "");
			settings.put(s, propertyValue);
		}
		String validationMessage = SyncMailUtil.validateSettings(settings);
		response.setContentType("text/json");
		response.getWriter().write(toJson(validationMessage));
	}


	@RequestMapping("/module/sync/sendTestEmail")
	public void sendTestEmail(@RequestParam("recipients") String recipients,
							  @RequestParam("subject") String subject,
							  @RequestParam("emailBody") String emailBody,
							  HttpServletRequest request, HttpServletResponse response) throws Exception {

		MessageSourceService mss = Context.getMessageSourceService();
		String results = mss.getMessage("sync.emailConfig.messageSendSuccessful");
		try {
			String serverName = request.getServerName();
			SyncMailUtil.sendMessage(recipients, serverName + ": " + subject, emailBody);
		}
		catch (Exception e) {
			results = mss.getMessage("sync.emailConfig.messageSendFailed") + ": " + e.getMessage();
		}
		response.setContentType("text/json");
		response.getWriter().write(toJson(results));
	}

	/**
	 * Utility method to return the json representation of an object
	 */
	private static String toJson(Object obj) {
		ObjectMapper mapper = new ObjectMapper();
		StringWriter sw = new StringWriter();
		try {
			mapper.writeValue(sw, obj);
		}
		catch (Exception e) {
			throw new APIException("Error converting to JSON", e);
		}
		return sw.toString();
	}
}
