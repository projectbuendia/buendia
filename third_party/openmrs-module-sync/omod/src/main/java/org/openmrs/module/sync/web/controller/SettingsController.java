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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncSource;
import org.openmrs.module.sync.SyncSourceJournal;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.ServerConnectionState;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.web.WebConstants;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

/**
 *
 */
public class SettingsController extends SimpleFormController {

	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	protected void initBinder(HttpServletRequest request,
	        ServletRequestDataBinder binder) throws Exception {
		super.initBinder(request, binder);
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request,
	        HttpServletResponse reponse, Object obj, BindException errors)
	        throws Exception {

		log.debug("in processFormSubmission");

		ModelAndView result = new ModelAndView(new RedirectView(getSuccessView()));

		if (!Context.isAuthenticated())
			throw new APIAuthenticationException("Not authenticated!");

		HttpSession httpSession = request.getSession();
		String success = "";
		String error = "";
		MessageSourceAccessor msa = getMessageSourceAccessor();
		String action = ServletRequestUtils.getStringParameter(request,
		                                                       "action",
		                                                       "");
		log.debug("action is " + action);
		if ("saveGeneral".equals(action)) {
			String serverName = ServletRequestUtils.getStringParameter(request,
			                                                           "serverName",
			                                                           "");
			String adminEmail = ServletRequestUtils.getStringParameter(request,
			                                                           "adminEmail",
			                                                           "");
			Context.getService(SyncService.class).saveServerName(serverName);
			Context.getService(SyncService.class).saveAdminEmail(adminEmail);

			String[] startedParams = request.getParameterValues("started");
			boolean started = false;
			if (startedParams != null) {
				for (String startedParam : startedParams) {
					if (startedParam.equals("true"))
						started = true;
				}
			}
			Integer repeatInterval = ServletRequestUtils.getIntParameter(request,
			                                                             "repeatInterval",
			                                                             0) * 60;

			if (started) {
				started = false;
				repeatInterval = null;
				error = msa.getMessage("NzeyiSynchronizationConfig.server.error.disabledSyncSchedule");
			}
			if (started && repeatInterval < 1)
				error = msa.getMessage("SynchronizationConfig.server.error.invalidRepeat");

			if (error.length() == 0) {
				RemoteServer parent = Context.getService(SyncService.class)
				                             .getParentServer();
				if (parent != null) {
					if (parent.getServerId() != null) {
						Integer serverId = parent.getServerId();
						TaskDefinition serverSchedule = null;
						Collection<TaskDefinition> tasks = Context.getSchedulerService()
						                                          .getRegisteredTasks();
						if (tasks != null) {
							for (TaskDefinition task : tasks) {
								if (task.getTaskClass()
								        .equals(SyncConstants.SCHEDULED_TASK_CLASS)) {
									if (serverId.toString()
									            .equals(task.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID))) {
										serverSchedule = task;
									} else {
										log.warn("not equal comparing "
										        + serverId
										        + " to "
										        + task.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID));
									}
								} else {
									log.warn("not equal comparing "
									        + task.getTaskClass()
									        + " to "
									        + SyncConstants.SCHEDULED_TASK_CLASS);
								}
							}
						} else {
							log.warn("tasks is null");
						}
						Map<String, String> props = new HashMap<String, String>();
						props.put(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID,
						          serverId.toString());
						if (serverSchedule != null) {
							if (log.isInfoEnabled())
								log.info("Sync scheduled task exists, and started is "
								        + started
								        + " and interval is "
								        + repeatInterval);
							try {
								Context.getSchedulerService()
								       .shutdownTask(serverSchedule);
							} catch (Exception e) {
								log.warn("Sync task had run wild, couldn't stop it because it wasn't really running",
								         e);
							}
							serverSchedule.setStarted(started);
							serverSchedule.setRepeatInterval((long) repeatInterval);
							serverSchedule.setStartOnStartup(started);
							serverSchedule.setProperties(props);
							if (started) {
								serverSchedule.setStartTime(new Date());
							}
							Context.getSchedulerService()
							       .saveTask(serverSchedule);
							if (started) {
								Context.getSchedulerService()
								       .scheduleTask(serverSchedule);
							}
						} else {
							if (log.isInfoEnabled())
								log.info("Sync scheduled task does not exists, and started is "
								        + started
								        + " and interval is "
								        + repeatInterval);
							if (started) {
								serverSchedule = new TaskDefinition();
								serverSchedule.setName(parent.getNickname()
								        + " "
								        + msa.getMessage("SynchronizationConfig.server.scheduler"));
								serverSchedule.setDescription(msa.getMessage("SynchronizationConfig.server.scheduler.description"));
								serverSchedule.setRepeatInterval((long) repeatInterval);
								serverSchedule.setStartTime(new Date());
								serverSchedule.setTaskClass(SyncConstants.SCHEDULED_TASK_CLASS);
								serverSchedule.setStarted(started);
								serverSchedule.setStartOnStartup(started);
								serverSchedule.setProperties(props);
								Context.getSchedulerService()
								       .saveTask(serverSchedule);
								Context.getSchedulerService()
								       .scheduleTask(serverSchedule);
							}
						}

					}
				}
				success = msa.getMessage("SynchronizationConfig.server.saved");
			}
		} else if ("deleteServer".equals(action)) {
			// check to see if the user is trying to delete a server, react
			// accordingly
			Integer serverId = ServletRequestUtils.getIntParameter(request,
			                                                       "serverId",
			                                                       0);
			String serverName = "Server " + serverId.toString();

			SyncService ss = Context.getService(SyncService.class);

			if (serverId > 0) {
				RemoteServer deleteServer = ss.getRemoteServer(serverId);
				serverName = deleteServer.getNickname();

				try {
					ss.deleteRemoteServer(deleteServer);
					Object[] args = { serverName };
					success = msa.getMessage("SynchronizationConfig.server.deleted",
					                         args);
				} catch (Exception e) {
					Object[] args = { serverName };
					error = msa.getMessage("SynchronizationConfig.server.deleteFailed",
					                       args);
				}
			} else {
				error = msa.getMessage("SynchronizationConfig.server.notDeleted");
			}

		} else if ("saveClasses".equals(action)) {
			String[] classIdsTo = ServletRequestUtils.getRequiredStringParameters(request,
			                                                                      "toDefault");
			String[] classIdsFrom = ServletRequestUtils.getRequiredStringParameters(request,
			                                                                        "fromDefault");
			Set<String> idsTo = new HashSet<String>();
			Set<String> idsFrom = new HashSet<String>();
			if (classIdsTo != null)
				idsTo.addAll(Arrays.asList(classIdsTo));
			if (classIdsFrom != null)
				idsFrom.addAll(Arrays.asList(classIdsFrom));

			List<SyncClass> syncClasses = Context.getService(SyncService.class)
			                                     .getSyncClasses();
			if (syncClasses != null) {
				// log.warn("SYNCCLASSES IS SIZE: " + syncClasses.size());
				for (SyncClass syncClass : syncClasses) {
					if (idsTo.contains(syncClass.getSyncClassId().toString()))
						syncClass.setDefaultSendTo(true);
					else
						syncClass.setDefaultSendTo(false);
					if (idsFrom.contains(syncClass.getSyncClassId().toString()))
						syncClass.setDefaultReceiveFrom(true);
					else
						syncClass.setDefaultReceiveFrom(false);
					Context.getService(SyncService.class)
					       .saveSyncClass(syncClass);
				}
			}

			success = msa.getMessage("SynchronizationConfig.classes.saved");
		}
		if (!success.equals(""))
			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success);
		if (!error.equals(""))
			httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error);

		return result;
	}

	/**
	 * 
	 * This is called prior to displaying a form for the first time. It tells
	 * Spring the form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request)
	        throws ServletException {
		Map<String, Object> obj = new HashMap<String, Object>();
		List<RemoteServer> serverList = new ArrayList<RemoteServer>();

		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			SyncService ss = Context.getService(SyncService.class);

			serverList.addAll(ss.getRemoteServers());
			obj.put("serverList", serverList);
			obj.put("parent", ss.getParentServer());
			SyncSource source = new SyncSourceJournal();
			obj.put("localServerGuid", source.getSyncSourceUuid());
			
			// TODO bwolfe, what happened to source.getSyncStatus?
			//obj.put("localServerSyncStatus", source.getSyncStatus());

			// obj.put("localServerName", SyncUtil.getLocalServerName());
			// obj.put("localServerAdminEmail",SyncUtil.getAdminEmail());

		}

		return obj;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map referenceData(HttpServletRequest request, Object obj,
	        Errors errors) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();

		if (Context.isAuthenticated()) {
			// cast
			Map<String, Object> ref = (Map<String, Object>) obj;
			RemoteServer server = (RemoteServer) ref.get("parent");

			// testConnection error messages
			MessageSourceAccessor msa = getMessageSourceAccessor();
			Map<String, String> connectionState = new HashMap<String, String>();
			connectionState.put(ServerConnectionState.OK.toString(),
			                    msa.getMessage("SynchronizationConfig.server.connection.status.ok"));
			connectionState.put(ServerConnectionState.AUTHORIZATION_FAILED.toString(),
			                    msa.getMessage("SynchronizationConfig.server.connection.status.noAuth"));
			connectionState.put(ServerConnectionState.CONNECTION_FAILED.toString(),
			                    msa.getMessage("SynchronizationConfig.server.connection.status.noConnection"));
			connectionState.put(ServerConnectionState.CERTIFICATE_FAILED.toString(),
			                    msa.getMessage("SynchronizationConfig.server.connection.status.noCertificate"));
			connectionState.put(ServerConnectionState.MALFORMED_URL.toString(),
			                    msa.getMessage("SynchronizationConfig.server.connection.status.badUrl"));
			connectionState.put(ServerConnectionState.NO_ADDRESS.toString(),
			                    msa.getMessage("SynchronizationConfig.server.connection.status.noAddress"));

			// taskConfig for automated syncing
			TaskDefinition serverSchedule = new TaskDefinition();
			String repeatInterval = "";
			if (server != null) {
				if (server.getServerId() != null) {
					Collection<TaskDefinition> tasks = Context.getSchedulerService()
					                                          .getRegisteredTasks();
					if (tasks != null) {
						String serverId = server.getServerId().toString();
						for (TaskDefinition task : tasks) {
							if (task.getTaskClass()
							        .equals(SyncConstants.SCHEDULED_TASK_CLASS)) {
								if (serverId.equals(task.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID))) {
									serverSchedule = task;
									Long repeat = serverSchedule.getRepeatInterval() / 60;
									repeatInterval = repeat.toString();
									if (repeatInterval.indexOf(".") > -1)
										repeatInterval = repeatInterval.substring(0,
										                                          repeatInterval.indexOf("."));
								}
							}
						}
					}
				}
			}

			ret.put("connectionState", connectionState.entrySet());
			ret.put("localServerName", Context.getService(SyncService.class).getServerName());
			ret.put("localServerAdminEmail",Context.getService(SyncService.class).getAdminEmail());
			ret.put("localServerGuid", Context.getService(SyncService.class).getServerUuid());
			ret.put("serverSchedule", serverSchedule);
			ret.put("repeatInterval", repeatInterval);
			ret.put("syncDateDisplayFormat",
			        TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
			ret.put("parent", server);
		}

		return ret;
	}

}