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
import org.openmrs.GlobalProperty;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.scheduler.web.controller.SchedulerFormController;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 
 */
public class MaintenanceController extends SimpleFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	public Integer maxPageRecords = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT);
	
	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		super.initBinder(request, binder);
		binder.registerCustomEditor(java.lang.Long.class, new CustomNumberEditor(java.lang.Long.class, true));
		binder.registerCustomEditor(java.util.Date.class, new CustomDateEditor(SchedulerFormController.DEFAULT_DATE_FORMAT,
		        true));
	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView processFormSubmission(HttpServletRequest request, HttpServletResponse response, Object command,
	                                             BindException errors) throws Exception {
		TaskDefinition task = (TaskDefinition) command;
		Map<String, String> properties = new HashMap<String, String>();
		String[] names = ServletRequestUtils.getStringParameters(request, "propertyName");
		String[] values = ServletRequestUtils.getStringParameters(request, "propertyValue");
		if (names != null) {
			for (int x = 0; x < names.length; x++) {
				if (names[x].length() > 0)
					properties.put(names[x], values[x]);
			}
		}
		task.setProperties(properties);
		
		task.setStartTimePattern(SchedulerFormController.DEFAULT_DATE_PATTERN);
		// if the user selected a different repeat interval unit, fix repeatInterval
		String units = request.getParameter("repeatIntervalUnits");
		Long interval = task.getRepeatInterval();
		
		if ("minutes".equals(units)) {
			interval = interval * 60;
		} else if ("hours".equals(units)) {
			interval = interval * 60 * 60;
		} else if ("days".equals(units)) {
			interval = interval * 60 * 60 * 24;
		}
		
		task.setRepeatInterval(interval);
		
		return super.processFormSubmission(request, response, command, errors);
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		TaskDefinition taskModel = null;
		Collection<TaskDefinition> tasks = Context.getSchedulerService().getRegisteredTasks();
		if (tasks != null) {
			for (TaskDefinition task : tasks) {
				if (task.getTaskClass().equals(SyncConstants.CLEAN_UP_OLD_RECORDS_TASK_CLASS_NAME)) {
					taskModel = task;
				}
			}
		}
		if (taskModel == null)
			taskModel = new TaskDefinition();
		
		return taskModel;
	}
	
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request, Object obj, Errors errors) throws Exception {
		
		Map<String, Object> ret = new HashMap<String, Object>();
		
		List<SyncRecord> returnList = new ArrayList<SyncRecord>();
		List<SyncRecord> matchesList = new ArrayList<SyncRecord>();
		String keyword = ServletRequestUtils.getStringParameter(request, "keyword", "");
		Integer page = ServletRequestUtils.getIntParameter(request, "page", 1);
		
		Integer maxPages = 1;
		Integer totalRecords = 0;
		
		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			SyncService syncService = Context.getService(SyncService.class);
			
			// if ("".equals(keyword) || keyword == null)
			// return new ArrayList<SyncRecord>();
			
			if (StringUtils.hasText(keyword))
				matchesList = syncService.getSyncRecords(keyword);
			
			String maxPageRecordsString = Context.getAdministrationService().getGlobalProperty(
			    SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS, SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT);
			
			try {
				maxPageRecords = Integer.parseInt(maxPageRecordsString);
			}
			catch (NumberFormatException e) {
				log.warn("Unable to format gp: " + SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS + " into an integer", e);
			}
			
			if (maxPageRecords < 1) {
				maxPageRecords = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT);
			}
			
			// Adding paging
			totalRecords = matchesList.size();
			if (matchesList.size() % maxPageRecords == 0)
				maxPages = (int) (totalRecords / maxPageRecords);
			else
				maxPages = (int) (totalRecords / maxPageRecords) + 1;
			
			if (page > maxPages)
				page = 1;
			
			returnList.clear();
			int start = (page - 1) * maxPageRecords;
			for (int i = 0; start + i < totalRecords && i < maxPageRecords; i++) {
				returnList.add(matchesList.get(start + i));
			}
			
		}
		
		List<GlobalProperty> globalPropList = new ArrayList<GlobalProperty>();
		List<GlobalProperty> syncPropList = new ArrayList<GlobalProperty>();
		Map<String, String> recordTypes = new HashMap<String, String>();
		Map<Object, String> itemTypes = new HashMap<Object, String>();
		Map<Object, String> itemUuids = new HashMap<Object, String>();
		Map<String, String> recordText = new HashMap<String, String>();
		Map<String, String> recordChangeType = new HashMap<String, String>();
		
		// warning: right now we are assuming there is only 1 item per record
		for (SyncRecord record : returnList) {
			
			String mainClassName = null;
			String mainUuid = null;
			String mainState = null;
			
			for (SyncItem item : record.getItems()) {
				String syncItem = item.getContent();
				mainState = item.getState().toString();
				Record xml = Record.create(syncItem);
				Item root = xml.getRootItem();
				String className = root.getNode().getNodeName().substring("org.openmrs.".length());
				itemTypes.put(item.getKey().getKeyValue(), className);
				if (mainClassName == null)
					mainClassName = className;
				
				// String itemInfoKey = itemInfoKeys.get(className);
				
				// now we have to go through the item child nodes to find the
				// real UUID that we want
				NodeList nodes = root.getNode().getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
					Node n = nodes.item(i);
					String propName = n.getNodeName();
					if (propName.equalsIgnoreCase("uuid")) {
						String uuid = n.getTextContent();
						itemUuids.put(item.getKey().getKeyValue(), uuid);
						if (mainUuid == null)
							mainUuid = uuid;
					}
				}
			}
			
			// persistent sets should show something other than their
			// mainClassName (persistedSet)
			if (mainClassName.indexOf("Persistent") >= 0)
				mainClassName = record.getContainedClasses();
			
			recordTypes.put(record.getUuid(), mainClassName);
			recordChangeType.put(record.getUuid(), mainState);
			
			// refactored - CA 21 Jan 2008
			String displayName = "";
			try {
				displayName = SyncUtil.displayName(mainClassName, mainUuid);
			}
			catch (Exception e) {
				// some methods like Concept.getName() throw Exception s all the
				// time...
				displayName = "";
			}
			if (displayName != null)
				if (displayName.length() > 0)
					recordText.put(record.getUuid(), displayName);
		}
		
		globalPropList = Context.getAdministrationService().getAllGlobalProperties();
		for (GlobalProperty prop : globalPropList) {
			if (prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS))
				syncPropList.add(prop);
			else if (prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_RECORDS_WEB))
				syncPropList.add(prop);
			else if (prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_RECORDS_FILE))
				syncPropList.add(prop);
			else if (prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT))
				syncPropList.add(prop);
		}
		
		ret.put("keyword", keyword);
		ret.put("syncProps", syncPropList);
		ret.put("totalRecords", totalRecords);
		ret.put("currentPage", page);
		ret.put("maxPages", maxPages);
		ret.put("recordTypes", recordTypes);
		ret.put("itemTypes", itemTypes);
		ret.put("itemUuids", itemUuids);
		// ret.put("itemInfo", itemInfo);
		ret.put("recordText", recordText);
		ret.put("recordChangeType", recordChangeType);
		ret.put("parent", Context.getService(SyncService.class).getParentServer());
		ret.put("servers", Context.getService(SyncService.class).getRemoteServers());
		ret.put(
		    "datePattern",
		    Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_DATE_PATTERN,
		        SyncConstants.DEFAULT_DATE_PATTERN));
		ret.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
		ret.put("synchronizationMaintenanceList", returnList);
		
		TaskDefinition task = (TaskDefinition) obj;
		
		Long interval = task.getRepeatInterval();
		
		//Copied this from the scehduler controller but it displays the wrong value if the time 
		//is not divisible by say 60, 3600 etc. E.g 1hr 30min may be displayed as just 1hr
		if (interval == null || interval < 60) {
			ret.put("units", "seconds");
            ret.put("repeatInterval", interval);
        }
		else if (interval < 3600) {
			ret.put("units", "minutes");
            ret.put("repeatInterval", interval / 60);
		} else if (interval < 86400) {
			ret.put("units", "hours");
            ret.put("repeatInterval", interval / 3600);
		} else {
			ret.put("units", "days");
            ret.put("repeatInterval", interval / 86400);
		}
		
		return ret;
	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
	                                BindException errors) throws Exception {
		
		SyncService syncService = Context.getService(SyncService.class);
		
		String action = ServletRequestUtils.getStringParameter(request, "action");
		
		if ("backporting".equals(action)) {
			Integer serverId = ServletRequestUtils.getRequiredIntParameter(request, "server");
			String dateString = ServletRequestUtils.getRequiredStringParameter(request, "date");
			
			RemoteServer server = syncService.getRemoteServer(serverId);
			Date date = new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
			    SyncConstants.PROPERTY_DATE_PATTERN, SyncConstants.DEFAULT_DATE_PATTERN)).parse(dateString);
			
			Integer numberBackproted = syncService.backportSyncRecords(server, date);
			request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.maintenance.backport.success");
			request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ARGS, numberBackproted);
			
		} else {
			// doing an archive task
			try {
				TaskDefinition task = (TaskDefinition) command;

				Context.addProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_SCHEDULER);

                // set the repeat interval
                String units = request.getParameter("repeatIntervalUnits");
                Long interval = Long.parseLong(request.getParameter("repeatInterval"));

                if ("minutes".equals(units)) {
                    interval = interval * 60;
                } else if ("hours".equals(units)) {
                    interval = interval * 60 * 60;
                } else if ("days".equals(units)) {
                    interval = interval * 60 * 60 * 24;
                }

                task.setRepeatInterval(interval);


                //only reschedule a task if it is started, is not running and the time is not in the past
				if (task.getStarted() && OpenmrsUtil.compareWithNullAsEarliest(task.getStartTime(), new Date()) > 0
				        && (task.getTaskInstance() == null || !task.getTaskInstance().isExecuting()))
					Context.getSchedulerService().rescheduleTask(task);
				else
					Context.getSchedulerService().saveTask(task);
				
				request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.maintenance.manage.changesSaved");
			}
			catch (APIException e) {
				errors.reject("sync.maintenance.manage.failedToSaveTaskProperties");
				return showForm(request, errors, getFormView());
			}
			finally {
				Context.removeProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_SCHEDULER);
			}
		}
		
		return new ModelAndView(new RedirectView(getSuccessView()));
	}
	
}
