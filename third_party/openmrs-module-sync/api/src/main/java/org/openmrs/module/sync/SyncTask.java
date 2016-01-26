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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.ingest.SyncTransmissionResponse;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * Represents scheduled task to perform full data synchronization with a remote server as identified
 * during the task setup.
 */
public class SyncTask extends AbstractTask {
	
	// Logger
	private static Log log = LogFactory.getLog(SyncTask.class);
	
	// Instance of configuration information for task
	private Integer serverId = 0;
	
	private static Boolean isExecuting = false; // allow only one running
	
	/**
	 * Default Constructor (Uses SchedulerConstants.username and SchedulerConstants.password
	 */
	public SyncTask() {
		// do nothing for now
	}
	
	/**
	 * Runs 'full' data synchronization (i.e. both send local changes and receive changes from the
	 * remote server as identified in the task setup).
	 * <p>
	 * NOTE: Any exception (outside of session open/close) is caught and reported in the error log
	 * thus creating retry behavior based on the scheduled frequency.
	 */
	public void execute() {
		Context.openSession();
		synchronized (isExecuting) {
			if (isExecuting) {
				log.warn("SyncTask processor aborting (another SyncTask already running)");
				return;
			}
			isExecuting = true;
		}
		try {
			log.debug("Synchronizing data to a server.");
			if (Context.isAuthenticated() == false && serverId > 0)
				authenticate();
			
			RemoteServer server = Context.getService(SyncService.class).getRemoteServer(serverId);
			if (server != null) {
				//auto syncing is only via web, so apply the web limit for sync records to send
				SyncTransmissionResponse response = SyncUtilTransmission.doFullSynchronize(server, null,
				    SyncUtil.getGlobalPropetyValueAsInteger(SyncConstants.PROPERTY_NAME_MAX_RECORDS_WEB));
				try {
					response.createFile(false, SyncConstants.DIR_JOURNAL);
				}
				catch (Exception e) {
					log.error("Unable to create file to store SyncTransmissionResponse: " + response.getFileName(), e);
				}
			}
		}
		catch (Exception e) {
			log.error("Scheduler error while trying to synchronize data. Will retry per schedule.", e);
		}
		finally {
			isExecuting = false;
			Context.closeSession();
			log.debug("Synchronization complete.");
		}
	}
	
	/**
	 * Initializes task. Note serverId is in most cases an Id (as stored in sync server table) of
	 * parent. As such, parent Id does not need to be stored separately with the task as it can
	 * always be determined from sync server table. serverId is stored here as we envision using
	 * this feature to also 'export' data to another server -- essentially 'shadow' copying data to
	 * a separate server for other uses such as reporting.
	 * 
	 * @param config
	 */
	@Override
	public void initialize(final TaskDefinition definition) {
		super.initialize(definition);
		try {
			this.serverId = Integer.valueOf(definition.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID));
		}
		catch (Exception e) {
			this.serverId = 0;
			log.error("Could not find serverId for this sync scheduled task.", e);
		}
	}
	
	/**
	 * Checks if the sync task is running.
	 * 
	 * @return true if running, else false.
	 */
	public static Boolean getIsExecuting() {
		synchronized (isExecuting) {
			return isExecuting;
		}
	}
}
