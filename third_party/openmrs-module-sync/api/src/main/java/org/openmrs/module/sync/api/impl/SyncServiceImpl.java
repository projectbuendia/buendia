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
package org.openmrs.module.sync.api.impl;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.db.SerializedObjectDAO;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncSubclassStub;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncServerClass;
import org.openmrs.module.sync.SyncStatistic;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.api.db.SyncDAO;
import org.openmrs.module.sync.api.db.hibernate.HibernateSyncInterceptor;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.openmrs.util.OpenmrsConstants;

/**
 * Default implementation of the {@link SyncService}
 */
public class SyncServiceImpl implements SyncService {
	
	private SyncDAO dao;
	
	private List<Class<OpenmrsObject>> allOpenmrsObjects;
	
	private final Log log = LogFactory.getLog(getClass());
	
	private static Set<String> serverClassesCollection;
	
	private SerializedObjectDAO serializedObjectDao;
	
	public void setSerializedObjectDao(SerializedObjectDAO serializedObjectDao) {
		this.serializedObjectDao = serializedObjectDao;
	}
	
	public SerializedObjectDAO getSerializedObjectDao() {
		return serializedObjectDao;
	}
	
	private SyncDAO getSynchronizationDAO() {
		return dao;
	}
	
	public void setSyncDAO(SyncDAO dao) {
		this.dao = dao;
	}
	
	public void setAllObjectsObjects(List<Class<OpenmrsObject>> openmrsObjects) {
		log.fatal("Got openmrs objects: " + openmrsObjects);
		
		this.allOpenmrsObjects = openmrsObjects;
	}
	
	public List<Class<OpenmrsObject>> getAllOpenmrsObjects() {
		
		return this.allOpenmrsObjects;
	}
	
	/**
	 * @see org.openmrs.api.SyncService#createSyncRecord(org.openmrs.module.sync.SyncRecord)
	 */
	
	public void createSyncRecord(SyncRecord record) throws APIException {
		this.createSyncRecord(record, record.getOriginalUuid());
	}
	
	public void createSyncRecord(SyncRecord record, String originalUuidPassed) throws APIException {
		
		if (record != null) {
			// here is a hack to get around the fact that hibernate decides to commit transactions when it feels like it
			// otherwise, we could run this in the ingest methods
			RemoteServer origin = null;
			int idx = originalUuidPassed.indexOf("|");
			if (idx > -1) {
				log.debug("originalPassed is " + originalUuidPassed);
				String originalUuid = originalUuidPassed.substring(0, idx);
				String serverUuid = originalUuidPassed.substring(idx + 1);
				log.debug("serverUuid is " + serverUuid + ", and originalUuid is " + originalUuid);
				record.setOriginalUuid(originalUuid);
				origin = Context.getService(SyncService.class).getRemoteServer(serverUuid);
				if (origin != null) {
					if (origin.getServerType().equals(RemoteServerType.PARENT)) {
						record.setState(SyncRecordState.COMMITTED);
					}
				} else {
					log.warn("Could not get remote server by uuid: " + serverUuid);
				}
			}
			
			// before creation, we need to make sure that we create matching entries for each server (server-record relationship)
			Set<SyncServerRecord> serverRecords = record.getServerRecords();
			if (serverRecords == null) {
				log.debug("IN createSyncRecord(), SERVERRECORDS ARE NULL, SO SETTING DEFAULTS");
				serverRecords = new HashSet<SyncServerRecord>();
				List<RemoteServer> servers = this.getRemoteServers();
				if (servers != null) {
					for (RemoteServer server : servers) {
						// we only need to create extra server-records for servers that are NOT the parent - the parent state is kept in the actual sync record
						if (!server.getServerType().equals(RemoteServerType.PARENT)) {
							SyncServerRecord serverRecord = new SyncServerRecord(server, record);
							// can't compare with .equals because of so many variables in it. SYNC-227
							if (server != null && origin != null && server.getServerId().equals(origin.getServerId())) {
								log.info("this record came from server " + origin.getNickname()
								        + ", so we will set its status to commmitted");
								serverRecord.setState(SyncRecordState.COMMITTED);
							}
							serverRecords.add(serverRecord);
						}
					}
				}
				record.setServerRecords(serverRecords);
			}
			
			getSynchronizationDAO().createSyncRecord(record);
		}
	}
	
	/**
	 * @see org.openmrs.api.SyncService#createSyncImportRecord(org.openmrs.module.sync.SyncImportRecord)
	 */
	public void createSyncImportRecord(SyncImportRecord record) throws APIException {
		getSynchronizationDAO().createSyncImportRecord(record);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getNextSyncRecord()
	 */
	public SyncRecord getFirstSyncRecordInQueue() throws APIException {
		return getSynchronizationDAO().getFirstSyncRecordInQueue();
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncRecords(java.lang.String)
	 */
	public List<SyncRecord> getSyncRecords(String query) throws APIException {
		return getSynchronizationDAO().getSyncRecords(query);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncRecord(java.lang.Integer)
	 */
	public SyncRecord getSyncRecord(Integer id) throws APIException {
		return getSynchronizationDAO().getSyncRecord(id);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncRecord(java.lang.String)
	 */
	public SyncRecord getSyncRecord(String uuid) throws APIException {
		return getSynchronizationDAO().getSyncRecord(uuid);
	}
	
	public SyncRecord getSyncRecordByOriginalUuid(String originalUuid) throws APIException {
		return getSynchronizationDAO().getSyncRecordByOriginalUuid(originalUuid);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getLatestRecord()
	 */
	public SyncRecord getLatestRecord() throws APIException {
		return getSynchronizationDAO().getLatestRecord();
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getEarliestRecord(Date)
	 */
	public SyncRecord getEarliestRecord(Date afterDate) throws APIException {
		return getSynchronizationDAO().getEarliestRecord(afterDate);
	}

	/**
	 * @see SyncService#getNextRecord(SyncRecord)
	 */
	public SyncRecord getNextRecord(SyncRecord record) {
		return getSynchronizationDAO().getNextRecord(record);
	}

	/**
	 * @see SyncService#getPreviousRecord(SyncRecord)
	 */
	public SyncRecord getPreviousRecord(SyncRecord record) {
		return getSynchronizationDAO().getPreviousRecord(record);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncRecord(java.lang.String)
	 */
	public SyncImportRecord getSyncImportRecord(String uuid) throws APIException {
		return getSynchronizationDAO().getSyncImportRecord(uuid);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.SyncService#getOlderSyncRecordInState(org.openmrs.module.sync.SyncRecord,
	 *      java.util.EnumSet)
	 */
	public SyncRecord getOlderSyncRecordInState(SyncRecord syncRecord, EnumSet<SyncRecordState> states) throws APIException {
		return getSynchronizationDAO().getOlderSyncRecordInState(syncRecord, states);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncImportRecords(org.openmrs.module.sync.engine.SyncRecordState)
	 */
	public List<SyncImportRecord> getSyncImportRecords(SyncRecordState... state) throws APIException {
		return getSynchronizationDAO().getSyncImportRecords(state);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncRecords()
	 */
	public List<SyncRecord> getSyncRecords() throws APIException {
		return getSynchronizationDAO().getSyncRecords();
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncRecords(org.openmrs.module.sync.engine.SyncRecordState)
	 */
	public List<SyncRecord> getSyncRecords(SyncRecordState state) throws APIException {
		return getSynchronizationDAO().getSyncRecords(state);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncRecords(org.openmrs.module.sync.engine.SyncRecordState, Integer maxSyncRecords, Integer)
	 */
	public List<SyncRecord> getSyncRecords(SyncRecordState[] states, Integer maxSyncRecords, Integer firstRecordId) throws APIException {
		return this.getSyncRecords(states, false, maxSyncRecords, firstRecordId);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.SyncService#getSyncRecords(org.openmrs.module.sync.SyncRecordState[],
	 *      org.openmrs.module.sync.server.RemoteServer, java.lang.Integer, java.lang.Integer)
	 */
	public List<SyncRecord> getSyncRecords(SyncRecordState[] states, RemoteServer server, Integer maxSyncRecords, Integer firstRecordId)
	                                                                                                             throws APIException {
		List<SyncRecord> temp = null;
		List<SyncRecord> ret = null;
		
		if (server != null) {
			if (server.getServerType().equals(RemoteServerType.PARENT)) {
				ret = this.getSyncRecords(states, maxSyncRecords, firstRecordId);
			} else {
				ret = getSynchronizationDAO().getSyncRecords(states, false, maxSyncRecords, server, firstRecordId);
			}
		}
		
		// filter out classes that are not supposed to be sent to the specified server
		// and update their status
		if (ret != null) {
			temp = new ArrayList<SyncRecord>();
			for (SyncRecord record : ret) {
				if (server.shouldBeSentSyncRecord(record)) {
					record.setForServer(server);
					temp.add(record);
					
				} else {
					log.warn("Omitting record with " + record.getContainedClasses() + " for server: " + server.getNickname()
					        + " with server type: " + server.getServerType());
					if (server.getServerType().equals(RemoteServerType.PARENT)) {
						record.setState(SyncRecordState.NOT_SUPPOSED_TO_SYNC);
					} else {
						// if not the parent, we have to update the record for this specific server
						Set<SyncServerRecord> records = record.getServerRecords();
						for (SyncServerRecord serverRecord : records) {
							if (serverRecord.getSyncServer().equals(server)) {
								serverRecord.setState(SyncRecordState.NOT_SUPPOSED_TO_SYNC);
							}
						}
						record.setServerRecords(records);
					}
					this.updateSyncRecord(record);
				}
			}
			ret = temp;
		}
		
		return ret;
	}
	
	/**
	 * @see org.openmrs.module.sync.api.SyncService#getSyncRecords(org.openmrs.module.sync.SyncRecordState[],
	 *      boolean, java.lang.Integer)
	 */
	public List<SyncRecord> getSyncRecords(SyncRecordState[] states, boolean inverse, Integer maxSyncRecords, Integer firstRecordId)
	                                                                                                         throws APIException {
		return getSynchronizationDAO().getSyncRecords(states, inverse, maxSyncRecords, null, firstRecordId);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#updateSyncRecord(org.openmrs.module.sync.SyncRecord)
	 */
	public void updateSyncRecord(SyncRecord record) throws APIException {
		getSynchronizationDAO().updateSyncRecord(record);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#deleteSyncRecord(org.openmrs.module.sync.SyncRecord)
	 */
	public void deleteSyncRecord(SyncRecord record) throws APIException {
		getSynchronizationDAO().deleteSyncRecord(record);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#updateSyncImportRecord(org.openmrs.module.sync.SyncImportRecord)
	 */
	public void updateSyncImportRecord(SyncImportRecord record) throws APIException {
		getSynchronizationDAO().updateSyncImportRecord(record);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#deleteSyncRecord(org.openmrs.module.sync.SyncRecord)
	 */
	public void deleteSyncImportRecord(SyncImportRecord record) throws APIException {
		getSynchronizationDAO().deleteSyncImportRecord(record);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#deleteSyncImportRecordsByServer(java.lang.Integer)
	 */
	public void deleteSyncImportRecordsByServer(Integer serverId) throws APIException {
		getSynchronizationDAO().deleteSyncImportRecordsByServer(serverId);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncRecordsSince(java.util.Date)
	 */
	public List<SyncRecord> getSyncRecordsSince(Date from) throws APIException {
		return getSynchronizationDAO().getSyncRecords(from, null, null, null, true);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getSyncRecordsBetween(java.util.Date, java.util.Date)
	 */
	public List<SyncRecord> getSyncRecordsBetween(Date from, Date to) throws APIException {
		return getSynchronizationDAO().getSyncRecords(from, to, null, null, true);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.SyncService#getSyncRecords(java.lang.Integer,
	 *      java.lang.Integer)
	 */
	public List<SyncRecord> getSyncRecords(Integer firstRecordId, Integer numberToReturn) throws APIException {
		return getSynchronizationDAO().getSyncRecords(null, null, firstRecordId, numberToReturn, false);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.SyncService#deleteSyncRecords(org.openmrs.module.sync.SyncRecordState[],
	 *      java.util.Date)
	 */
	public Integer deleteSyncRecords(SyncRecordState[] states, Date to) throws APIException {
		
		// if no states passed in, then decide based on current server setup
		if (states == null || states.length == 0) {
			
			if (getParentServer() == null) {
				// if server is not a leaf node (only a parent)
				// state does not matter (but will always be NEW)
				states = new SyncRecordState[] { SyncRecordState.NOT_SUPPOSED_TO_SYNC, SyncRecordState.NEW };
			} else {
				// if a server is a leaf node, then only delete states that 
				// have been successfully sent to the parent already
				states = new SyncRecordState[] { SyncRecordState.NOT_SUPPOSED_TO_SYNC, SyncRecordState.COMMITTED };
			}
		}
		
		return getSynchronizationDAO().deleteSyncRecords(states, to);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#getGlobalProperty(java.lang.String)
	 */
	public String getGlobalProperty(String propertyName) throws APIException {
		return getSynchronizationDAO().getGlobalProperty(propertyName);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#setGlobalProperty(String propertyName, String propertyValue)
	 */
	public void setGlobalProperty(String propertyName, String propertyValue) throws APIException {
		getSynchronizationDAO().setGlobalProperty(propertyName, propertyValue);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#saveRemoteServer(org.openmrs.module.sync.engine.RemoteServer)
	 */
	public RemoteServer saveRemoteServer(RemoteServer server) throws APIException {
		if (server != null) {
			Set<SyncServerClass> serverClasses = server.getServerClasses();
			if (serverClasses == null) {
				log.warn("IN CREATEREMOTESERVER(), SERVERCLASSES ARE NULL, SO SETTING DEFAULTS");
				serverClasses = new HashSet<SyncServerClass>();
				List<SyncClass> classes = this.getSyncClasses();
				if (classes != null) {
					for (SyncClass syncClass : classes) {
						SyncServerClass serverClass = new SyncServerClass(server, syncClass);
						serverClasses.add(serverClass);
					}
				}
				server.setServerClasses(serverClasses);
			}
			
			server = getSynchronizationDAO().saveRemoteServer(server);
			refreshServerClassesCollection();
			
			return server;
		}
		return null;
	}
	
	/**
	 * @see org.openmrs.api.SyncService#deleteRemoteServer(org.openmrs.module.sync.engine.RemoteServer)
	 */
	public void deleteRemoteServer(RemoteServer server) throws APIException {
		getSynchronizationDAO().deleteRemoteServer(server);
	}
	
	public RemoteServer getRemoteServer(Integer serverId) throws APIException {
		return getSynchronizationDAO().getRemoteServer(serverId);
	}
	
	public RemoteServer getRemoteServer(String uuid) throws APIException {
		return getSynchronizationDAO().getRemoteServer(uuid);
	}
	
	public RemoteServer getRemoteServerByUsername(String username) throws APIException {
		return getSynchronizationDAO().getRemoteServerByUsername(username);
	}
	
	public List<RemoteServer> getRemoteServers() throws APIException {
		return getSynchronizationDAO().getRemoteServers();
	}
	
	public RemoteServer getParentServer() throws APIException {
		return getSynchronizationDAO().getParentServer();
	}
	
	/**
	 * Returns globally unique identifier of the local server. This value uniquely indentifies
	 * server in all data exchanges with other servers.
	 */
	public String getServerUuid() throws APIException {
		return Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_SERVER_UUID);
	}
	
	/**
	 * Updates globally unique identifier of the local server.
	 */
	public void saveServerUuid(String uuid) throws APIException {
		Context.getService(SyncService.class).setGlobalProperty(SyncConstants.PROPERTY_SERVER_UUID, uuid);
	}
	
	/**
	 * Returns server friendly name for sync purposes. It should be assigned by convention to be
	 * unique in the synchronization network of servers. This value can be used to scope values that
	 * are otherwise unique only locally (such as integer primary keys).
	 */
	public String getServerName() throws APIException {
		return Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_SERVER_NAME);
	}
	
	/**
	 * Updates/saves the user friendly server name for sync purposes.
	 */
	public void saveServerName(String name) throws APIException {
		Context.getService(SyncService.class).setGlobalProperty(SyncConstants.PROPERTY_SERVER_NAME, name);
	}
	
	public String getAdminEmail() {
		return Context.getService(SyncService.class).getGlobalProperty(SyncConstants.PROPERTY_SYNC_ADMIN_EMAIL);
	}
	
	public void saveAdminEmail(String email) {
		Context.getService(SyncService.class).setGlobalProperty(SyncConstants.PROPERTY_SYNC_ADMIN_EMAIL, email);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#saveSyncClass(org.openmrs.module.sync.SyncClass)
	 */
	public void saveSyncClass(SyncClass syncClass) throws APIException {
		getSynchronizationDAO().saveSyncClass(syncClass);
		refreshServerClassesCollection();
	}
	
	/**
	 * @see org.openmrs.api.SyncService#deleteSyncClass(org.openmrs.module.sync.SyncClass)
	 */
	public void deleteSyncClass(SyncClass syncClass) throws APIException {
		getSynchronizationDAO().deleteSyncClass(syncClass);
		refreshServerClassesCollection();
	}
	
	public SyncClass getSyncClass(Integer syncClassId) throws APIException {
		return getSynchronizationDAO().getSyncClass(syncClassId);
	}
	
	public List<SyncClass> getSyncClasses() throws APIException {
		return getSynchronizationDAO().getSyncClasses();
	}
	
	public SyncClass getSyncClassByName(String className) throws APIException {
		return getSynchronizationDAO().getSyncClassByName(className);
	}
	
	/**
	 * @see org.openmrs.api.SyncService#deleteOpenmrsObject(org.openmrs.synchronization.OpenmrsObject)
	 */
	public void deleteOpenmrsObject(OpenmrsObject o) throws APIException {
		getSynchronizationDAO().deleteOpenmrsObject(o);
	}
	
	/**
	 * Changes flush sematics, delegating directly to the corresponsing DAO method.
	 * 
	 * @see org.openmrs.api.SyncService#setFlushModeManual()
	 * @see org.openmrs.api.db.hibernate.HibernateSyncDAO#setFlushModeManual()
	 */
	public void setFlushModeManual() throws APIException {
		getSynchronizationDAO().setFlushModeManual();
	}
	
	/**
	 * Changes flush sematics, delegating directly to the corresponsing DAO method.
	 * 
	 * @see org.openmrs.api.SyncService#setFlushModeAutomatic()
	 * @see org.openmrs.api.db.hibernate.HibernateSyncDAO#setFlushModeAutomatic()
	 */
	public void setFlushModeAutomatic() throws APIException {
		getSynchronizationDAO().setFlushModeAutomatic();
	}
	
	/**
	 * Performs peristence layer flush, delegating directly to the corresponsing DAO method.
	 * 
	 * @see org.openmrs.api.SyncService#flushSession()
	 * @see org.openmrs.api.db.hibernate.HibernateSyncDAO#flushSession()
	 */
	public void flushSession() throws APIException {
		getSynchronizationDAO().flushSession();
	}
	
	/**
	 * Processes save/update to instance of OpenmrsObject by persisting it into local persistance
	 * store.
	 * 
	 * @param object instance of OpenmrsObject to be processed.
	 * @return
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Records"})
	public void saveOrUpdate(OpenmrsObject object) throws APIException {
		getSynchronizationDAO().saveOrUpdate(object);
	}
	
	/**
	 * Gets stats for the server: 1. Sync Records count by server by state 2. If any sync records
	 * are in 'pending'/failed state and it has been > 24hrs, add statistic for it 3. count of
	 * 'pending' sync records (i.e. the ones that are not in complete or error state
	 * 
	 * @param fromDate start date
	 * @param toDate end date
	 * @return
	 * @throws DAOException
	 */
	public Map<RemoteServer, LinkedHashSet<SyncStatistic>> getSyncStatistics(Date fromDate, Date toDate) throws DAOException {
		
		Map<RemoteServer, LinkedHashSet<SyncStatistic>> stats = getSynchronizationDAO().getSyncStatistics(fromDate, toDate);
		
		//check out the info for the servers: if any records are pending and are older than 1 day, add flag to stats
		for (Map.Entry<RemoteServer, LinkedHashSet<SyncStatistic>> entry1 : stats.entrySet()) {
			Long pendingCount = 0L;
			for (SyncStatistic syncStat : entry1.getValue()) {
				if (syncStat.getType() == SyncStatistic.Type.SYNC_RECORD_COUNT_BY_STATE) {
					if (syncStat.getName() != SyncRecordState.ALREADY_COMMITTED.toString()
					        && syncStat.getName() != SyncRecordState.COMMITTED.toString()
					        && syncStat.getName() != SyncRecordState.NOT_SUPPOSED_TO_SYNC.toString()) {
						pendingCount = pendingCount
						        + ((syncStat.getValue() == null) ? 0L : Long.parseLong(syncStat.getValue().toString()));
					}
				}
			}
			
			//add pending count
			entry1.getValue().add(
			    new SyncStatistic(SyncStatistic.Type.SYNC_RECORDS_PENDING_COUNT,
			            SyncStatistic.Type.SYNC_RECORDS_PENDING_COUNT.toString(), pendingCount)); //careful, manipulating live collection
			
			//if some 'stale' records found see if it has been 24hrs since last sync
			RemoteServer server = entry1.getKey();
			
			if (server.getLastSync() != null) {
				Calendar lastSync = Calendar.getInstance();
				lastSync.setTime(server.getLastSync());
				Calendar threeDayThreshold = Calendar.getInstance();
				threeDayThreshold.add(Calendar.HOUR, -72); // check if last sync is more than 3 days ago
				Calendar oneDayThreshold = Calendar.getInstance();
				oneDayThreshold.add(Calendar.HOUR, -24); // check if last sync is more than 3 days ago
				
				if (lastSync.before(threeDayThreshold)) {
					entry1.getValue().add(
					    new SyncStatistic(SyncStatistic.Type.LAST_SYNC_REALLY_LONG_TIME_AGO,
					            SyncStatistic.Type.LAST_SYNC_REALLY_LONG_TIME_AGO.toString(), pendingCount)); //careful, manipulating live collection
				} else if (lastSync.before(oneDayThreshold)) {
					entry1.getValue().add(
					    new SyncStatistic(SyncStatistic.Type.LAST_SYNC_TIME_SOMEWHAT_TROUBLESOME,
					            SyncStatistic.Type.LAST_SYNC_TIME_SOMEWHAT_TROUBLESOME.toString(), pendingCount)); //careful, manipulating live collection
				}
			}
		}
		
		return stats;
	}
	
	public <T extends OpenmrsObject> T getOpenmrsObjectByUuid(Class<T> clazz, String uuid) {
		T ret = dao.getOpenmrsObjectByUuid(clazz, uuid);
		if (ret == null) {
			try {
				ret = serializedObjectDao.getObjectByUuid(clazz, uuid); //sync-205
			}
			catch (Exception ex) {
				//pass -- not sure if catch/try is necessary
			}
		}
		
		return ret;
	}
	
	/**
	 * @see org.openmrs.api.SynchronizationService#exportChildDB(java.lang.String,
	 *      java.io.OutputStream)
	 */
	public void exportChildDB(String guidForChild, OutputStream os) throws APIException {
		getSynchronizationDAO().exportChildDB(guidForChild, os);
	}
	
	/**
	 * @see org.openmrs.api.SynchronizationService#importParentDB(java.io.InputStream)
	 */
	public void importParentDB(InputStream in) throws APIException {
		getSynchronizationDAO().importParentDB(in);
		//Delete any data kept into sync journal after clone of the parent DB
		for (SyncRecord record : this.getSynchronizationDAO().getSyncRecords()) {
			this.getSynchronizationDAO().deleteSyncRecord(record);
		}
	}
	
	/**
	 * @see org.openmrs.module.sync.api.SyncService#generateDataFile()
	 */
	public File generateDataFile() throws APIException {
		File dir = SyncUtil.getSyncApplicationDir();
		String fileName = SyncConstants.CLONE_IMPORT_FILE_NAME + SyncConstants.SYNC_FILENAME_MASK.format(new Date())
		        + ".sql";
		String[] ignoreTables = { "hl7_in_archive", "hl7_in_queue", "hl7_in_error", "formentry_archive", "formentry_queue",
		        "formentry_error", "sync_class", "sync_import", "sync_record", "sync_server", "sync_server_class",
		        "sync_server_record" };
		
		File outputFile = new File(dir, fileName);
		getSynchronizationDAO().generateDataFile(outputFile, ignoreTables);
		return outputFile;
	}
	
	/**
	 * @see org.openmrs.module.sync.api.SyncService#execGeneratedFile(java.io.File)
	 */
	public void execGeneratedFile(File file) throws APIException {
		AdministrationService adminService = Context.getAdministrationService();
		
		// preserve this server's sync settings
		List<GlobalProperty> syncGPs = adminService.getGlobalPropertiesByPrefix("sync.");
		
		getSynchronizationDAO().execGeneratedFile(file);
		
		// save those GPs again
		for (GlobalProperty gp : syncGPs) {
			adminService.saveGlobalProperty(gp);
		}
		
		//Delete any data in sync record after import of the parent DB
		for (SyncRecord record : this.getSynchronizationDAO().getSyncRecords()) {
			this.getSynchronizationDAO().deleteSyncRecord(record);
		}
	}
	
	/**
	 * Determines if given object is to be sync-ed assuming sync as a feature is turned on. This is
	 * done by: <br/>
	 * 1. type has to implement OpenmrsObject interface 2. comparing the type of the object against
	 * the types in the DB configured for exclusion from sync.
	 * 
	 * @see org.openmrs.module.sync.api.SyncService#execGeneratedFile(java.io.File)
	 */
	public Boolean shouldSynchronize(Object entity) throws APIException {
		Boolean ret = true;
		
		// OpenmrsObject *only*.
		if (!(entity instanceof OpenmrsObject)) {
			if (log.isDebugEnabled())
				log.debug("Do nothing. Flush with type that does not implement OpenmrsObject, type is:"
				        + entity.getClass().getName());
			return false;
		}
		
		//if the server classes haven't been loaded yet, do it now
		if (serverClassesCollection == null) {
			refreshServerClassesCollection();
		}
		
		//now verify
		if (serverClassesCollection != null) {
			String type = entity.getClass().getName();
			for (String temp : serverClassesCollection) {
				if (type.startsWith(temp)) {
					ret = false;
					break;
				}
			}
		}
		
		return ret;
		
	}
	
	/***
	 * Refreshes static helper collection. This is a perf optimization to avoid fetching the
	 * sync_server_classes on every call to {@link #shouldSynchronize(Object)} Remarks:<br/>
	 * The algorithm is as follows: - if no servers to talk to are setup (i.e. no rows in
	 * sync_server_class) then use sync_class only - else only use the classes that are setup in all
	 * servers (i.e.) for the class/type to be excluded it has to be setup for exclusion in all
	 * servers
	 */
	public static synchronized void refreshServerClassesCollection() {
		
		List<RemoteServer> servers = Context.getService(SyncService.class).getRemoteServers();
		Set<String> serverClasses = new HashSet<String>();
		
		if (servers == null || servers.size() == 0) {
			//this is easy, just use the defaults
			for (SyncClass sc : Context.getService(SyncService.class).getSyncClasses()) {
				if (!sc.getDefaultReceiveFrom() && !sc.getDefaultSendTo())
					serverClasses.add(sc.getName());
			}
		} else {
			//some sync servers are set up
			Map<String, Integer> helperMap = new HashMap<String, Integer>();
			
			//crank through and count up the types & occurrences
			for (RemoteServer server : servers) {
				for (String temp : server.getClassesNotReceived()) {
					if (helperMap.containsKey(temp)) {
						//already there, just increment the count
						Integer iTemp = helperMap.get(temp) + 1;
						helperMap.put(temp, iTemp);
					} else {
						//not there yet, just add with count of 0
						helperMap.put(temp, 1);
					}
				}
				for (String temp : server.getClassesNotSent()) {
					if (helperMap.containsKey(temp)) {
						//already there, just increment the count
						Integer iTemp = helperMap.get(temp) + 1;
						helperMap.put(temp, iTemp);
					} else {
						//not there yet, just add with count of 0
						helperMap.put(temp, 1);
					}
				}
			}
			
			//now, walk the map and only use the types where occurrence count = 2 x nbr or servers
			//i.e. the type was listed on all servers as both don't send and don't receive
			int targetCount = servers.size() * 2;
			for (String type : helperMap.keySet()) {
				if (helperMap.get(type).equals(targetCount)) {
					serverClasses.add(type);
				}
			}
		}
		
		//now assign
		serverClassesCollection = serverClasses;
	}
	
	public String getPrimaryKey(OpenmrsObject obj) {
		if (obj instanceof Privilege) {
			return ((Privilege) obj).getPrivilege();
		} else if (obj instanceof Role) {
			return ((Role) obj).getRole();
		} else if (obj instanceof GlobalProperty) {
			return ((GlobalProperty) obj).getProperty();
		} else {
			return null;
		}
	}
	
	/**
	 * Handles the odd case of saving patient who already has person record. This method is invoked
	 * by sync AOP advice on save of new patient (see
	 * {@link org.openmrs.module.sync.advice.SavePatientAdvice}) in order to generate a necessary
	 * sync item for the actions taken inside of
	 * {@link org.openmrs.api.db.hibernate.HibernatePatientDAO#savePatient(Patient)}. The
	 * compensating logic resides in
	 * {@link HibernateSyncInterceptor#addSyncItemForSubclassStub(SyncSubclassStub)}.
	 */
	public void handleInsertPatientStubIfNeeded(Patient p) throws APIException {
		
		SyncSubclassStub stub = new SyncSubclassStub(p, "person", "person_id", "patient", "patient_id", null, null, null);
		stub.addColumn("voided", 0);
		Integer userId = 0;
		if (p.getCreator() != null)
			userId = p.getCreator().getUserId();
		else
			userId = Context.getAuthenticatedUser().getUserId();
		
		stub.addColumn("creator", userId);
		stub.addColumn("date_created", p.getDateCreated());
		
		handleInsertSubclassIfNeeded(stub);
	}
	
	public void handleInsertSubclassIfNeeded(SyncSubclassStub stub) {
		if (stub == null || stub.getId() == null || stub.getUuid() == null) {
			return;
		}
		
		// changing the flush mode temporarily so that nothing is flushed
		// to the db while we are checking for the uuids
		boolean wasFlushModeManualAlready = dao.setFlushModeManual();
		try {
			//check if person obj exists
			Object parentId = null;
			Object subclassId = null;
			
			// TODO: Fix this logic when patient_id != person_id anymore
			List<List<Object>> rows = executeSQLPrivilegeSafe("select " + stub.getParentTableId() + " from " + stub.getParentTable() + " where uuid = '" + stub.getUuid()
			        + "'", true);
			if (rows.size() > 0)
				parentId = rows.get(0).get(0);
			
			rows = executeSQLPrivilegeSafe(
			    "select " + stub.getSubclassTableId() + " from " + stub.getSubclassTable() + " where " + stub.getSubclassTableId() + " = (select " + stub.getParentTableId() + " from " + stub.getParentTable() + " where uuid = '"
			            + stub.getUuid() + "')", true);
			
			if (rows.size() > 0)
				subclassId = rows.get(0).get(0);
			
			if (parentId != null && subclassId == null) {
				//bingo!
				log.info("Create of new parent " + stub.getParentTable() + " who is already other object detected, uuid: " + stub.getUuid());
				
				HibernateSyncInterceptor.addSyncItemForSubclassStub(stub);
			}
		}
		finally {
			// only reset this if we really changed it when setting it to manual
			if (!wasFlushModeManualAlready)
				dao.setFlushModeAutomatic();
		}
		
		return;
	}
	
	public Long getCountOfSyncRecords(RemoteServer server, Date from, Date to, SyncRecordState... states)
	                                                                                                        throws APIException {
		return dao.getCountOfSyncRecords(server, from, to, states);
	}
	
	/**
	 * Utility method for wrapping executeSQL calls in SQL LEVEL ACCESS privilege, if necessary
	 * 
	 * @param sql
	 * @param selectOnly
	 * @return
	 */
	private List<List<Object>> executeSQLPrivilegeSafe(String sql, boolean selectOnly) {
		String privilege = OpenmrsConstants.PRIV_SQL_LEVEL_ACCESS;
		
		if (!Context.isAuthenticated() || !Context.hasPrivilege(privilege)) {
			try {
				Context.addProxyPrivilege(privilege);
				return Context.getAdministrationService().executeSQL(sql, selectOnly);
			}
			finally {
				Context.removeProxyPrivilege(privilege);
			}
			
		} else
			return Context.getAdministrationService().executeSQL(sql, selectOnly);
	}

	
	public Integer backportSyncRecords(RemoteServer server, Date date) {
		int count = 0;
		SyncRecord firstRecord = getEarliestRecord(date);
		SyncRecord latestRecord = getLatestRecord();
		
		// we have no sync records, quit early
		if (firstRecord == null)
			return 0;
		
		// not sure how this would happen without the previous one, but just in case.
		if (latestRecord == null)
			return 0;
		
		Integer firstRecordId = 0;
		Integer latestRecordId = latestRecord.getRecordId();
		
		System.out.println("first record id: " + firstRecord.getRecordId());
		System.out.println("latest record id: " + latestRecord.getRecordId());
		
		boolean recordsFound = false;
		
		do {
			recordsFound = false;
			
			// only getting a small number at a time so that we don't get an OOM
			for (SyncRecord record : getSynchronizationDAO().getSyncRecords(null, null, firstRecordId, 35, true)) {
				recordsFound = true;
				System.out.println("record id: " + record.getRecordId());
				firstRecordId = record.getRecordId();
				SyncServerRecord serverRecord = record.getServerRecord(server);
				if (serverRecord == null) {
					// if this record is not being sent to this server yet, add it as a SyncServerRecord and send it
					record.addServerRecord(server);
					updateSyncRecord(record); // persist to the db
					count++;
					System.out.println("saved record id: " + record.getRecordId());
				}
				if (count % 50 == 0) {
					// flush every so often so we don't get an OOM
					Context.flushSession();
					Context.clearSession();
				}
			}
			
		} while (recordsFound == true && firstRecordId != latestRecordId);
		
		return count;
	}

    /**
     * @see SyncService#getMostRecentFullyCommittedRecordId()
     */
     public int getMostRecentFullyCommittedRecordId() {
        return getSynchronizationDAO().getMostRecentFullyCommittedRecordId();
     }
	
	/**
	 * @see org.openmrs.module.sync.api.SyncService#getSyncServerRecord(java.lang.Integer)
	 */
	public SyncServerRecord getSyncServerRecord(Integer syncServerRecordId) throws APIException {
		return getSynchronizationDAO().getSyncServerRecord(syncServerRecordId);
	}
	
}
