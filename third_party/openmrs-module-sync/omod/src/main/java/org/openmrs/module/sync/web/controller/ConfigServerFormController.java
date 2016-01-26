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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncServerClass;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.module.sync.server.ServerConnectionState;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.web.WebConstants;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for the page that lets the user configure a child server, a parent server, or this
 * server.
 */
@Controller
public class ConfigServerFormController {
	
	/** Logger for this class and subclasses */
	protected static transient final Log log = LogFactory.getLog(ConfigServerFormController.class);
	
	@RequestMapping(value = "/module/sync/configServer", method = RequestMethod.POST, params = "action=saveNewChild")
	protected String onSaveNewChild(@RequestParam String nickname, @RequestParam String uuid,
	                                @RequestParam(required = false) String username,
	                                @RequestParam(required = false) String password, @RequestParam String passwordRetype,
	                                @RequestParam(required = false) Boolean shouldEmail, @RequestParam String adminEmail,
	                                HttpSession httpSession, @ModelAttribute("server") RemoteServer server, Errors errors,
	                                @RequestParam(required = false) List<String> notSendTo,
	                                @RequestParam(required = false) List<String> notReceiveFrom) throws Exception {
		
		if (!Context.isAuthenticated())
			throw new APIAuthenticationException("Not authenticated!");
		
		//if the user provided a username, then the passwords are required
		if (StringUtils.hasText(username)) {
			if (!StringUtils.hasText(password) || !StringUtils.hasText(passwordRetype)) {
				errors.rejectValue("password", "sync.config.server.error.passwordRequired");
			} else if (!password.equals(passwordRetype))
				errors.rejectValue("password", "error.password.match");
		}
		
		if (!StringUtils.hasLength(nickname))
			errors.rejectValue("nickname", "sync.config.server.error.nicknameRequired");
		
		if (errors.hasErrors())
			return "/module/sync/configServerForm";
		
		log.debug("in onSave for new child");
		
		MessageSourceService mss = Context.getMessageSourceService();
		SyncService syncService = Context.getService(SyncService.class);
		
		server.setServerType(RemoteServerType.CHILD);
		server.setNickname(nickname);
		server.setUuid(uuid);
		
		if (StringUtils.hasText(username)) {
			// create a new user in either A) 1.5.x or B) 1.6+
			User user = null;
			
			if (Person.class.isAssignableFrom(User.class)) {
				// if we're in a pre-1.6 environment, User extends Person
				user = new User();
				// if 1.6+ the User.setGender method does not exist, so if we 
				// don't do this by reflection we will have a compile-time error
				// (and gender is a required field)
				Method setGenderMethod = User.class.getMethod("setGender", String.class);
				setGenderMethod.invoke(user, SyncConstants.DEFAULT_CHILD_SERVER_USER_GENDER);
				
				user.setUsername(username);
				
				PersonName name = new PersonName();
				name.setFamilyName(nickname);
				name.setGivenName(mss.getMessage(SyncConstants.DEFAULT_CHILD_SERVER_USER_NAME));
				user.addName(name);
			} else {
				// create a new user in a 1.6+ environemnt where
				// User does NOT extend Person
				Person person = new Person();
				person.setGender(SyncConstants.DEFAULT_CHILD_SERVER_USER_GENDER);
				person.setBirthdate(new Date());
				PersonName name = new PersonName();
				name.setFamilyName(nickname);
				name.setGivenName(mss.getMessage(SyncConstants.DEFAULT_CHILD_SERVER_USER_NAME));
				person.addName(name);
				Context.getPersonService().savePerson(person);
				
				user = new User(person);
				user.setUsername(username);
			}
			
			String defaultRole = Context.getAdministrationService().getGlobalProperty("sync.default_role");
			if (defaultRole != null) {
				String[] roles = defaultRole.split(",");
				for (String role : roles) {
					Role r = Context.getUserService().getRole(role.trim());
					if (r != null)
						user.addRole(r);
				}
			}
			
			// create in database
			try {
				Context.getUserService().saveUser(user, password);
				server.setChildUsername(user.getUsername());
			}
			catch (Exception e) {
				log.error("Unable to create new user to associate with child server", e);
				
				//Am using the exception message because it is already localized. 
				//You can look at OpenmrsUtil.validatePassword()
				errors.rejectValue("username", e.getMessage());
				
				return "/module/sync/configServerForm";
			}
		}
		
		server.setAddress("N/A");
		server.setPassword("N/A");
		server.setUsername("N/A");
		
		saveOrUpdateServerClasses(server, notSendTo, notReceiveFrom);
		
		server = syncService.saveRemoteServer(server);
		
		httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.config.server.saved");
		
		if (server.getServerId() != null)
			return "redirect:/module/sync/configServer.form?serverId=" + server.getServerId();
		else
			return "redirect:/module/sync/config.list";
	}
	
	@RequestMapping(value = "/module/sync/configServer", method = RequestMethod.POST, params = "action=editChild")
	protected String onSaveCurrentChild(@RequestParam String nickname, @RequestParam String uuid, HttpSession httpSession,
	                                    @ModelAttribute("server") RemoteServer server, Errors errors,
	                                    @RequestParam(required = false) List<String> notSendTo,
	                                    @RequestParam(required = false) List<String> notReceiveFrom) throws Exception {
		
		if (!Context.isAuthenticated())
			throw new APIAuthenticationException("Not authenticated!");
		
		if (!StringUtils.hasLength(nickname))
			errors.rejectValue("nickname", "sync.config.server.error.nicknameRequired");
		
		if (errors.hasErrors())
			return "/module/sync/configServerForm";
		
		server.setNickname(nickname);
		server.setUuid(uuid);
		
		saveOrUpdateServerClasses(server, notSendTo, notReceiveFrom);
		
		server = Context.getService(SyncService.class).saveRemoteServer(server);
		
		httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.config.server.saved");
		
		if (server.getServerId() != null)
			return "redirect:/module/sync/configServer.form?serverId=" + server.getServerId();
		else
			return "redirect:/module/sync/config.list";
		
	}
	
	@RequestMapping(value = "/module/sync/configServer", method = RequestMethod.POST, params = "action=saveParent")
	protected String onSaveParent(@RequestParam String nickname, @RequestParam String address,
	                              @RequestParam String username, @RequestParam String password,
	                              @RequestParam(required = false) Boolean started,
	                              @RequestParam(required = false) Integer repeatInterval, HttpSession httpSession,
	                              @ModelAttribute("server") RemoteServer server, Errors errors,
	                              @RequestParam(required = false) List<String> notSendTo,
	                              @RequestParam(required = false) List<String> notReceiveFrom) throws Exception {
		
		if (!Context.isAuthenticated())
			throw new APIAuthenticationException("Not authenticated!");
		
		if (!StringUtils.hasLength(nickname))
			errors.rejectValue("nickname", "sync.config.server.error.nicknameRequired");
		
		if (!StringUtils.hasLength(address))
			errors.rejectValue("address", "sync.config.server.error.addressRequired");
		
		if (started == null) {
			started = false; // if they didn't check the box, the value is false
			repeatInterval = 0;
		}
		
		if (started && repeatInterval < 1)
			errors.rejectValue("address", "sync.config.server.error.invalidRepeat");
		
		if (errors.hasErrors())
			return "/module/sync/configServerForm";
		
		// interval needs to be in seconds, but we asked the user for minutes. multiply by 60 to convert to minutes
		repeatInterval = repeatInterval * 60;
		
		server.setServerType(RemoteServerType.PARENT);
		server.setNickname(nickname);
		
		// just in case - we want to make sure there is ONLY ever 1 parent
		if (server.getServerType().equals(RemoteServerType.PARENT)) {
			RemoteServer parent = Context.getService(SyncService.class).getParentServer();
			if (parent != null && parent.getServerId() != server.getServerId()) {
				throw new APIException(
				        "Oh no! There is another server already stored in the database as the parent: server id : "
				                + parent.getServerId());
			}
		}
		
		server.setAddress(address);
		server.setPassword(password);
		server.setUsername(username);
		
		saveOrUpdateServerClasses(server, notSendTo, notReceiveFrom);
		
		server = Context.getService(SyncService.class).saveRemoteServer(server);
		
		saveOrUpdateTask(server, started, repeatInterval);
		
		httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.config.parent.saved");
		
		if (server.getServerId() != null)
			return "redirect:/module/sync/configServer.form?serverId=" + server.getServerId();
		else
			return "redirect:/module/sync/config.list";
	}
	
	/**
	 * @param server
	 * @param notSendTo
	 * @param notReceiveFrom
	 */
	protected void saveOrUpdateServerClasses(RemoteServer server, List<String> notSendTo, List<String> notReceiveFrom) {
		if (notSendTo == null)
			notSendTo = Collections.emptyList();
		
		if (notReceiveFrom == null)
			notReceiveFrom = Collections.emptyList();
		
		log.debug("sendto: " + notSendTo.size());
		log.debug("receiveFrom: " + notReceiveFrom.size());
		
		SyncService syncService = Context.getService(SyncService.class);
		
		Set<SyncServerClass> currentServerClasses = server.getServerClasses();
		
		// mark all current serverClasses that are not in the lists
		for (SyncServerClass syncClass : currentServerClasses) {
			if (!notSendTo.contains(syncClass.getSyncClass().getName()) && !syncClass.getSendTo()) {
				syncClass.setSendTo(true);
			}
			
			if (!notReceiveFrom.contains(syncClass.getSyncClass().getName()) && !syncClass.getReceiveFrom()) {
				syncClass.setReceiveFrom(true);
			}
		}
		
		// unmark all currentSyncClasses that are in the sendTo list
		for (String className : notSendTo) {
			boolean foundClass = false;
			className = className.trim();
			for (SyncServerClass currentClass : currentServerClasses) {
				if (currentClass.getSyncClass().getName().equals(className)) {
					foundClass = true;
					currentClass.setSendTo(false);
				}
			}
			
			// we need to add a new item to the list
			if (!foundClass) {
				SyncClass defaultSyncClass = syncService.getSyncClassByName(className);
				if (defaultSyncClass == null) {
					defaultSyncClass = new SyncClass();
					defaultSyncClass.setName(className);
					syncService.saveSyncClass(defaultSyncClass);
				}
				
				SyncServerClass newSyncClass = new SyncServerClass();
				newSyncClass.setSyncClass(defaultSyncClass);
				newSyncClass.setSendTo(false);
				newSyncClass.setSyncServer(server);
				// we must add this to the list of current classes so that the receiveFrom list picks it up instead of creating a new one
				currentServerClasses.add(newSyncClass);
			}
		}
		
		// unmark all currentSyncClasses that are in the sendTo list
		for (String className : notReceiveFrom) {
			boolean foundClass = false;
			className = className.trim();
			for (SyncServerClass currentClass : currentServerClasses) {
				if (currentClass.getSyncClass().getName().equals(className)) {
					foundClass = true;
					currentClass.setReceiveFrom(false);
				}
			}
			
			// we need to add a new item to the list
			if (!foundClass) {
				SyncClass defaultSyncClass = syncService.getSyncClassByName(className);
				if (defaultSyncClass == null) {
					defaultSyncClass = new SyncClass();
					defaultSyncClass.setName(className);
					syncService.saveSyncClass(defaultSyncClass);
				}
				
				SyncServerClass newSyncServerClass = new SyncServerClass();
				newSyncServerClass.setSyncClass(defaultSyncClass);
				newSyncServerClass.setReceiveFrom(false);
				newSyncServerClass.setSyncServer(server);
				// must put this on the currentServerClasses so it gets saved
				currentServerClasses.add(newSyncServerClass);
			}
		}
		
	}
	
	/**
	 * Creates a task for a parent server if that task doesn't exist. If the task does exist, update
	 * it.
	 * 
	 * @param server
	 * @param started
	 * @param repeatInterval
	 */
	protected void saveOrUpdateTask(RemoteServer server, Boolean started, Integer repeatInterval) throws SchedulerException {
		
		Integer serverId = server.getServerId();
		MessageSourceService mss = Context.getMessageSourceService();
		
		try {
			//Add privilege to enable us access the registered tasks
	        Context.addProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_SCHEDULER);
	        
	        TaskDefinition serverSchedule = null;
			Collection<TaskDefinition> tasks = Context.getSchedulerService().getRegisteredTasks();
			if (tasks != null) {
				for (TaskDefinition task : tasks) {
					if (task.getTaskClass().equals(SyncConstants.SCHEDULED_TASK_CLASS)) {
						if (serverId.toString().equals(task.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID))) {
							serverSchedule = task;
						} else {
							log.warn("not equal comparing " + serverId + " to "
							        + task.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID));
						}
					} else {
						log.warn("not equal comparing " + task.getTaskClass() + " to " + SyncConstants.SCHEDULED_TASK_CLASS);
					}
				}
			} else {
				log.warn("tasks is null");
			}
		
			Map<String, String> props = new HashMap<String, String>();
			props.put(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID, serverId.toString());
			if (serverSchedule != null) {
				if (log.isInfoEnabled())
					log.info("Sync scheduled task exists, and started is " + started + " and interval is " + repeatInterval);
				try {
					Context.getSchedulerService().shutdownTask(serverSchedule);
				}
				catch (Exception e) {
					log.warn("Sync task had run wild, couldn't stop it because it wasn't really running", e);
					// nothing to do - means something was wrong or not yet started
					//TODO: is this right? should we report error here on 'STRICT'?
				}
				serverSchedule.setStarted(started);
				serverSchedule.setRepeatInterval((long) repeatInterval);
				serverSchedule.setStartOnStartup(started);
				serverSchedule.setProperties(props);
				if (started) {
					serverSchedule.setStartTime(new Date());
				}
				Context.getSchedulerService().saveTask(serverSchedule);
				if (started) {
					Context.getSchedulerService().scheduleTask(serverSchedule);
				}
			} else {
				if (log.isInfoEnabled())
					log.info("Sync scheduled task does not exists, and started is " + started + " and interval is "
					        + repeatInterval);
				if (started) {
					serverSchedule = new TaskDefinition();
					serverSchedule.setName(server.getNickname() + " " + mss.getMessage("sync.config.server.scheduler"));
					serverSchedule.setDescription(mss.getMessage("sync.config.server.scheduler.description"));
					serverSchedule.setRepeatInterval((long) repeatInterval);
					serverSchedule.setStartTime(new Date());
					serverSchedule.setTaskClass(SyncConstants.SCHEDULED_TASK_CLASS);
					serverSchedule.setStarted(started);
					serverSchedule.setStartOnStartup(started);
					serverSchedule.setProperties(props);
					Context.getSchedulerService().saveTask(serverSchedule);
					Context.getSchedulerService().scheduleTask(serverSchedule);
				}
			}
		}
		finally {
			//We no longer need this privilege.
			Context.removeProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_SCHEDULER);
		}
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * {@link RemoteServer} to use. This same object is rebuilt on form submission and passed into
	 * the onSave* methods.
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("server")
	protected RemoteServer formBackingObject(@RequestParam(value = "type", required = false) String serverType,
	                                         @RequestParam(value = "serverId", required = false) Integer serverId)
	                                                                                                              throws ServletException {
		RemoteServer server = null;
		
		log.debug("IN FORMBACKING, type is " + serverType);
		
		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			if (serverId != null)
				server = Context.getService(SyncService.class).getRemoteServer(serverId);
			
			if (server == null && serverType != null) {
				server = new RemoteServer();
				server.setServerType(RemoteServerType.valueOf(serverType));
				
				// set the classes from the defaults
				Set<SyncServerClass> serverClasses = new HashSet<SyncServerClass>();
				List<SyncClass> classes = Context.getService(SyncService.class).getSyncClasses();
				if (classes != null) {
					for (SyncClass syncClass : classes) {
						SyncServerClass serverClass = new SyncServerClass(server, syncClass);
						serverClasses.add(serverClass);
					}
				}
				server.setServerClasses(serverClasses);
			}
		}
		
		return server;
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/module/sync/configServer", method = RequestMethod.GET)
	protected String showPage(ModelMap modelMap, @ModelAttribute("server") RemoteServer server,
	                          @RequestParam(value = "type", required = false) String serverType) throws Exception {
		
		if (Context.isAuthenticated()) {
			
			if (serverType == null || RemoteServerType.PARENT.equals(serverType)
			        || RemoteServerType.PARENT.equals(server.getServerType())) {
				// testConnection error messages
				MessageSourceService mss = Context.getMessageSourceService();
				Map<String, String> connectionState = new HashMap<String, String>();
				connectionState.put(ServerConnectionState.OK.toString(),
				    mss.getMessage("sync.config.server.connection.status.ok"));
				connectionState.put(ServerConnectionState.AUTHORIZATION_FAILED.toString(),
				    mss.getMessage("sync.config.server.connection.status.noAuth"));
				connectionState.put(ServerConnectionState.CONNECTION_FAILED.toString(),
				    mss.getMessage("sync.config.server.connection.status.noConnection"));
				connectionState.put(ServerConnectionState.CERTIFICATE_FAILED.toString(),
				    mss.getMessage("sync.config.server.connection.status.noCertificate"));
				connectionState.put(ServerConnectionState.MALFORMED_URL.toString(),
				    mss.getMessage("sync.config.server.connection.status.badUrl"));
				connectionState.put(ServerConnectionState.NO_ADDRESS.toString(),
				    mss.getMessage("sync.config.server.connection.status.noAddress"));
				
				try {
					//Add privilege to enable us access the registered tasks
			        Context.addProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_SCHEDULER);
			        
					// get repeatInterval for tasks taskConfig for automated syncing
					TaskDefinition serverSchedule = new TaskDefinition();
					String repeatInterval = "";
					if (server != null) {
						if (server.getServerId() != null) {
							Collection<TaskDefinition> tasks = Context.getSchedulerService().getRegisteredTasks();
							if (tasks != null) {
								String serverId = server.getServerId().toString();
								for (TaskDefinition task : tasks) {
									if (task.getTaskClass().equals(SyncConstants.SCHEDULED_TASK_CLASS)) {
										if (serverId.equals(task.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID))) {
											serverSchedule = task;
											Long repeat = serverSchedule.getRepeatInterval() / 60;
											repeatInterval = repeat.toString();
											if (repeatInterval.indexOf(".") > -1)
												repeatInterval = repeatInterval.substring(0, repeatInterval.indexOf("."));
										}
									}
								}
							}
						}
					}
					modelMap.put("connectionState", connectionState.entrySet());
					modelMap.put("serverSchedule", serverSchedule);
					modelMap.put("repeatInterval", repeatInterval);
				}
				finally {
					//We no longer need this privilege.
					Context.removeProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_SCHEDULER);
				}
			}
			
			modelMap.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
			modelMap.put("type", serverType);
			
			//sync status stuff for this server
			SyncService syncService = Context.getService(SyncService.class);
			modelMap.put("localServerUuid", syncService.getServerUuid());
			modelMap.put("localServerName", syncService.getServerName());
			modelMap.put("localServerAdminEmail", syncService.getAdminEmail());
		}
		
		return "/module/sync/configServerForm";
	}
	
}
