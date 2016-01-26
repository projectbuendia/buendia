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
package org.openmrs.module.sync.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.util.StringUtils;

/**
 * This task deletes rows in the sync_record and sync_server_record tables that are older and not
 * needed anymore. (Sync_import is not touched at this point)
 */
public class CleanupSyncTablesTask extends AbstractTask {
	
	private static Log log = LogFactory.getLog(CleanupSyncTablesTask.class);
	
	protected static final String PROPERTY_DAYS_BACK = "delete_entries_files_older_than_x_days";
	
	private Integer DEFAULT_DAYS_BACK_TO_START_DELETE = 90;
	
	/**
	 * Comma delimited list of {@link SyncRecordState} names that will be deleted. These states on
	 * the {@link SyncServerRecord} rows.
	 */
	protected static final String PROPERTY_STATES_TO_DELETE = "sync_record_states_to_delete";
	
	// by default let the service decide which states to delete
	private SyncRecordState[] DEFAULT_STATES_TO_DELETE = new SyncRecordState[] { };
	
	/**
	 * Do the actual deleting of tables.
	 */
	public void execute() {
		Context.openSession();
		log.debug("Starting sync table cleanup ... ");
		try {
			if (Context.isAuthenticated() == false)
				authenticate();
			
			// get the possibly user-defined settings
			Map<String, String> props = null;
			if (taskDefinition != null)
				props = taskDefinition.getProperties();
			
			Integer daysBack = getIntegerProperty(PROPERTY_DAYS_BACK, props, DEFAULT_DAYS_BACK_TO_START_DELETE);
			
			Calendar today = Calendar.getInstance();
			today.add(Calendar.DATE, -1 * daysBack);
			Date deleteTo = today.getTime();
			
			SyncRecordState[] statesToDelete = getSyncRecordStateProperty(PROPERTY_STATES_TO_DELETE, props,
			    DEFAULT_STATES_TO_DELETE);
			
			// do the actual deleting
			SyncService syncService = Context.getService(SyncService.class);
			Integer quantityDeleted = syncService.deleteSyncRecords(statesToDelete, deleteTo);
			
			log.info("There were " + quantityDeleted + " sync records cleaned out");
		}
		catch (Throwable t) {
			log.error("Error while doing sync table cleanup", t);
			throw new APIException(t);
		}
		finally {
			Context.closeSession();
		}
	}
	
	/**
	 * Get the given property name from the given Map object. If not found or if the value is an
	 * invalid integer, return defaultValue
	 * 
	 * @param propertyName the prop key to look for
	 * @param props the key-value map to look in
	 * @param defaultValue the default integer if the value is invalid
	 * @return the defined value for prop in props
	 */
	protected static Integer getIntegerProperty(String propertyName, Map<String, String> props, Integer defaultValue) {
		if (props != null) {
			String prop = props.get(propertyName);
			if (prop != null) {
				try {
					return Integer.valueOf(prop);
				}
				catch (NumberFormatException e) {
					log.error("Unable to convert property value for " + propertyName + " : '" + prop + "' to an integer");
				}
			}
		}
		
		return defaultValue;
	}
	
	/**
	 * Get the given property name from the given props and convert it to an array of
	 * {@link SyncRecordState}s
	 * 
	 * @param propertyName the prop key to look for
	 * @param props the key-value map to look in
	 * @param defaultStates the default array if the value is invalid
	 * @return an array of {@link SyncRecordState}s
	 */
	protected static SyncRecordState[] getSyncRecordStateProperty(String propertyName, Map<String, String> props,
	                                                              SyncRecordState[] defaultStates) {
		if (props != null) {
			String prop = props.get(propertyName);
			if (StringUtils.hasLength(prop)) {
				try {
					List<SyncRecordState> states = new ArrayList<SyncRecordState>();
					for (String stateName : prop.split(",")) {
						SyncRecordState state = SyncRecordState.valueOf(stateName.trim());
						states.add(state);
					}
					return states.toArray(new SyncRecordState[] {});
				}
				catch (Exception e) {
					log.error("Unable to convert property value for " + propertyName + " : '" + prop
					        + "' to an array of states");
				}
			}
		}
		
		return defaultStates;
	}
}
