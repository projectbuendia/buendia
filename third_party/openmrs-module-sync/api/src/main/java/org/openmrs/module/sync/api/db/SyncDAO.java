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
package org.openmrs.module.sync.api.db;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncSubclassStub;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncStatistic;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.SyncServerRecord;

/**
 * Synchronization related database functions
 */
public interface SyncDAO {
	
	/**
	 * Create a new SyncRecord
	 * 
	 * @param SyncRecord The SyncRecord to create
	 * @throws DAOException
	 */
	public void createSyncRecord(SyncRecord record) throws DAOException;
	
	/**
	 * Update a SyncRecord
	 * 
	 * @param SyncRecord The SyncRecord to update
	 * @throws DAOException
	 */
	public void updateSyncRecord(SyncRecord record) throws DAOException;
	
	/**
	 * Delete a SyncRecord
	 * 
	 * @param SyncRecord The SyncRecord to delete
	 * @throws DAOException
	 */
	public void deleteSyncRecord(SyncRecord record) throws DAOException;
	
	public List<SyncRecord> getSyncRecords(String query) throws DAOException;
	
	public SyncRecord getSyncRecord(Integer recordId) throws DAOException;
	
	/**
	 * @param uuid of the SyncRecord to retrieve
	 * @return SyncRecord The SyncRecord or null if not found
	 * @throws DAOException
	 */
	public SyncRecord getSyncRecord(String uuid) throws DAOException;
	
	public SyncRecord getSyncRecordByOriginalUuid(String originalUuid) throws DAOException;
	
	/**
	 * @return SyncRecord The latest SyncRecord or null if not found
	 * @throws DAOException
	 */
	public SyncRecord getLatestRecord() throws DAOException;
	
	/**
	 * @return The earliest SyncRecord or null if not found
	 * @throws DAOException
	 */
	public SyncRecord getEarliestRecord(Date afterDate) throws DAOException;

	/**
	 * @return the next sync record after the passed in record
	 */
	public SyncRecord getNextRecord(SyncRecord record) throws DAOException;

	/**
	 * @return the previous sync record before the passed in record
	 */
	public SyncRecord getPreviousRecord(SyncRecord record) throws DAOException;
	
	/**
	 * Create a new SyncImportRecord
	 * 
	 * @param SyncImportRecord The SyncImportRecord to create
	 * @throws DAOException
	 */
	public void createSyncImportRecord(SyncImportRecord record) throws DAOException;
	
	/**
	 * Update a SyncImportRecord
	 * 
	 * @param SyncImportRecord The SyncImportRecord to update
	 * @throws DAOException
	 */
	public void updateSyncImportRecord(SyncImportRecord record) throws DAOException;
	
	/**
	 * Delete a SyncImportRecord
	 * 
	 * @param SyncImportRecord The SyncImportRecord to delete
	 * @throws DAOException
	 */
	public void deleteSyncImportRecord(SyncImportRecord record) throws DAOException;
	
	/**
	 * Delete SyncImportRecords for a given server
	 * 
	 * @param serverId the serverId of records to delete
	 * @throws DAOException
	 */

	public void deleteSyncImportRecordsByServer(Integer serverId) throws DAOException;
	
	/**
	 * @param uuid of the SyncImportRecord to retrieve
	 * @return SyncImportRecord The SyncImportRecord or null if not found
	 * @throws DAOException
	 */
	public SyncImportRecord getSyncImportRecord(String uuid) throws DAOException;
	
	/**
	 * Get all SyncImportRecords in a specific SyncRecordState
	 * 
	 * @param state SyncRecordState for the SyncImportRecords to be returned
	 * @return SyncRecord A list containing all SyncImportRecords with the given state
	 * @throws DAOException
	 */
	public List<SyncImportRecord> getSyncImportRecords(SyncRecordState... state) throws DAOException;
	
	/**
	 * Returns the first SyncRecord in either the PENDING SEND or the NEW state
	 * 
	 * @return SyncRecord The first SyncRecord matching the criteria, or null if none matches
	 * @throws DAOException
	 */
	public SyncRecord getFirstSyncRecordInQueue() throws DAOException;
	
	/**
	 * Get all SyncRecords
	 * 
	 * @return SyncRecord A list containing all SyncRecords
	 * @throws DAOException
	 */
	public List<SyncRecord> getSyncRecords() throws DAOException;
	
	/**
	 * Get all SyncRecords in a specific SyncRecordState
	 * 
	 * @param state SyncRecordState for the SyncRecords to be returned
	 * @return SyncRecord A list containing all SyncRecords with the given state
	 * @throws DAOException
	 */
	public List<SyncRecord> getSyncRecords(SyncRecordState state) throws DAOException;
	
	/**
	 * Get all SyncRecords in specific SyncRecordStates
	 * 
	 * @param states SyncRecordStates for the SyncRecords to be returned
	 * @param inverse if true, will get all records that DO NOT have the given states
	 * @param maxSyncRecords if less than 1 will use {@link SyncConstants#PROPERTY_NAME_MAX_RECORDS_DEFAULT
	 * @param server if not null, will restrict records to only those for the given server (optional)
	 * @param firstRecordId the id of the first record to return, if null, assumes 0
	 * @return SyncRecord A list containing all SyncRecords with the given states
	 * @throws DAOException
	 */
	public List<SyncRecord> getSyncRecords(SyncRecordState[] states, boolean inverse, Integer maxSyncRecords,
	                                       RemoteServer server, Integer firstRecordId) throws DAOException;
	
	/**
	 * Deletes all sync records that have the given state and are before the given date
	 * 
	 * @param states the states to delete
	 * @param to the date to delete before
	 * @return the number of delete records
	 * @throws DAOException
	 */
	public Integer deleteSyncRecords(SyncRecordState[] states, Date to) throws DAOException;
	
	/**
	 * Get all SyncRecords between two timestamps, including the to-timestamp. <br/>
	 * If firstRecordId is not null, that is the earliest record returned, only that record and
	 * after are included. <br/>
	 * If numberToReturn is not null, the size of the returned list is restricted to that size.
	 * 
	 * @param from Timestamp specifying lower bound, not included. (nullable)
	 * @param to Timestamp specifying upper bound, included. (nullable)
	 * @param firstRecordId the first SyncRecord#getRecordId() to return (nullable)
	 * @param numberToReturn the max number of records to return(nullable)
	 * @param oldestToNewest true/false whether to order the records from oldest to most recent
	 * @return SyncRecord A list containing all SyncRecords with a timestamp between the from
	 *         timestamp and up to and including the to timestamp
	 * @throws DAOException
	 */
	public List<SyncRecord> getSyncRecords(Date from, Date to, Integer firstRecordId, Integer numberToReturn,
	                                       boolean oldestToNewest) throws DAOException;
	
	/**
	 * Retrieve value of given global property using synchronization data access meachnisms.
	 * 
	 * @param propertyName
	 * @return
	 */
	public String getGlobalProperty(String propertyName);
	
	/**
	 * Set global property related to synchronization; notably bypasses any changeset recording
	 * mechanisms.
	 * 
	 * @param propertyName String specifying property name which value is to be set.
	 * @param propertyValue String specifying property value to be set.
	 * @throws APIException
	 */
	public void setGlobalProperty(String propertyName, String propertyValue) throws APIException;
	
	/**
	 * Save a new or current RemoteServer in the database
	 * 
	 * @param RemoteServer The RemoteServer to create/update
	 * @return RemoteServer The RemoteServer created or updated
	 * @throws DAOException
	 */
	public RemoteServer saveRemoteServer(RemoteServer server) throws DAOException;
	
	/**
	 * Delete a RemoteServer
	 * 
	 * @param RemoteServer The RemoteServer to delete
	 * @throws DAOException
	 */
	public void deleteRemoteServer(RemoteServer server) throws DAOException;
	
	/**
	 * @param uuid of the RemoteServer to retrieve
	 * @return RemoteServer The RemoteServer or null if not found
	 * @throws DAOException
	 */
	public RemoteServer getRemoteServer(Integer serverId) throws DAOException;
	
	/**
	 * @param uuid of the RemoteServer to retrieve
	 * @return RemoteServer The RemoteServer or null if not found
	 * @throws DAOException
	 */
	public RemoteServer getRemoteServer(String uuid) throws DAOException;
	
	/**
	 * @param username (child_username) of the RemoteServer to retrieve
	 * @return RemoteServer The RemoteServer or null if not found
	 * @throws DAOException
	 */
	public RemoteServer getRemoteServerByUsername(String username) throws DAOException;
	
	/**
	 * @return List of all {@link RemoteServer}s defined -- both parent and child.
	 * @throws DAOException
	 */
	public List<RemoteServer> getRemoteServers() throws DAOException;
	
	/**
	 * @return RemoteServer The RemoteServer or null if not defined
	 * @throws DAOException
	 */
	public RemoteServer getParentServer() throws DAOException;
	
	/**
	 * Update a SyncClass
	 * 
	 * @param SyncClass The SyncClass to update
	 * @throws DAOException
	 */
	public void saveSyncClass(SyncClass record) throws DAOException;
	
	/**
	 * Delete a SyncClass
	 * 
	 * @param SyncClass The SyncClass to delete
	 * @throws DAOException
	 */
	public void deleteSyncClass(SyncClass record) throws DAOException;
	
	/**
	 * @param syncClassId of the SyncClass to retrieve
	 * @return SyncClass The SyncClass or null if not found
	 * @throws DAOException
	 */
	public SyncClass getSyncClass(Integer syncClassId) throws DAOException;
	
	/**
	 * @return SyncClass The latest SyncClass or null if not found
	 * @throws DAOException
	 */
	public List<SyncClass> getSyncClasses() throws DAOException;
	
	/**
	 * @param classname of the SyncClass to retrieve
	 * @return SyncClass The SyncClass or null if not found
	 * @throws DAOException
	 */
	public SyncClass getSyncClassByName(String className) throws DAOException;
	
	/**
	 * Deletes instance of OpenmrsObject from storage.
	 * 
	 * @param o instance to delete from storage
	 * @throws DAOException
	 */
	public void deleteOpenmrsObject(OpenmrsObject o) throws DAOException;
	
	/**
	 * Sets session flush mode to manual thus suspending session flush.
	 * 
	 * @return true if the flush mode was manual already
	 * @throws DAOException
	 */
	public boolean setFlushModeManual() throws DAOException;
	
	/**
	 * Sets session flush mode to automatic thus enabling persistence library's default flush
	 * behavior.
	 * 
	 * @throws DAOException
	 */
	public void setFlushModeAutomatic() throws DAOException;
	
	/**
	 * Returns true if the flush mode is currently set to manual
	 * 
	 * @return true if the flush mode is manual
	 * @throws DAOException
	 */
	public boolean isFlushModeManual() throws DAOException;
	
	/**
	 * Flushes presistence library's session.
	 * 
	 * @throws DAOException
	 */
	public void flushSession() throws DAOException;
	
	/**
	 * Performs generic save of openmrs object using persistance api.
	 * 
	 * @throws DAOException
	 */
	public void saveOrUpdate(Object object) throws DAOException;
	
	/**
	 * retrieves statistics about sync servers
	 * 
	 * @throws DAOException
	 */
	public Map<RemoteServer, LinkedHashSet<SyncStatistic>> getSyncStatistics(Date fromDate, Date toDate) throws DAOException;
	
	public <T extends OpenmrsObject> T getOpenmrsObjectByUuid(Class<T> clazz, String uuid);
	
	/**
	 * Gets the uuid for the given class according to its (usually integer) primary key
	 * 
	 * @param <T> the {@link OpenmrsObject} to return
	 * @param clazz the class of object to return/lookup
	 * @param id the primary key value
	 * @return the uuid for the given class with given id
	 */
	public <T extends OpenmrsObject> String getUuidForOpenmrsObject(Class<T> clazz, String id);
	
	/**
	 * Processes the serializes state of a collection.
	 * <p>
	 * (This typically handles two types of hibernate collections: PersistentSortedSet and
	 * PersistentSet.) </br> Processing of collections is handled as follows based on the serialized
	 * info stored in incoming:
	 * <p>
	 * 1. Pull out owner info, and collection action (i.e. update, recreate). Attempt to create
	 * instance of the owner using openmrs API and retrieve the reference to the existing collection
	 * that is associated with the owner. <br/>
	 * 2. Iterate owner serialized entries and process actions (i.e entry update, delete) <br/>
	 * 3. Record the original uuid using owner finally, trigger owner update using openmrs api <br/>
	 * For algorithmic details, see code comments as the implementation is extensively commented.
	 * 
	 * @param type collection type.
	 * @param incoming serialized state, interceptor implementation for serialization details
	 * @param originalRecordUuid unique uuid assigned to this update (i.e. sync record) that will be
	 *            propagated throughout the synchronization to avoid duplicating this change
	 */
	public void processCollection(Class collectionType, String incoming, String originalRecordUuid) throws Exception;
	
	/**
	 * Dumps the entire database, much like what you'd get from the mysqldump command, and adds a
	 * few lines to set the child's GUID, and delete sync history
	 * 
	 * @param guidForChild if not null, use this as the guid for the child server, otherwise
	 *            autogenerate one
	 * @param out write the sql here
	 * @throws DAOException
	 */
	public void exportChildDB(String guidForChild, OutputStream os) throws DAOException;
	
	/**
	 * imports a synchronization database backup from the parent
	 * 
	 * @throws DAOException
	 */
	public void importParentDB(InputStream in) throws DAOException;
	
	public void generateDataFile(File outFile, String[] ignoreTables);
	
	public void execGeneratedFile(File generatedDataFile);
	
	/**
	 * Mimics the hack for saving patients who are already users/persons. For full description of
	 * how this works see {@link SyncSubclassStub}.
	 * 
	 * @see SyncSubclassStub
	 * @param stub
	 */
	public void processSyncSubclassStub(SyncSubclassStub stub);
	
	/**
	 * See
	 * {@link org.openmrs.module.sync.api.SyncIngestService#isConceptIdValidForUuid(int, String)}
	 */
	public boolean isConceptIdValidForUuid(Integer conceptId, String uuid);
	
	public Long getCountOfSyncRecords(RemoteServer server, Date from, Date to, SyncRecordState... states);
	
	/**
	 * @see SyncService#getOlderSyncRecordInState(SyncRecord, EnumSet)
	 */
	public SyncRecord getOlderSyncRecordInState(SyncRecord syncRecord, EnumSet<SyncRecordState> states);
    /**
     * @see SyncService#getMostRecentFullyCommittedRecordId()
     */
    public int getMostRecentFullyCommittedRecordId();
	
	/**
	 * @see SyncService#getSyncServerRecord(Integer)
	 * @throws DAOException
	 */
	public SyncServerRecord getSyncServerRecord(Integer syncServerRecordId) throws DAOException;
	
}
