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
package org.openmrs.module.sync.api.db.hibernate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.impl.SessionImpl;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncSubclassStub;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncStatistic;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.db.SyncDAO;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.ingest.SyncIngestException;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class HibernateSyncDAO implements SyncDAO {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Hibernate session factory
	 */
	private SessionFactory sessionFactory;
	
	public HibernateSyncDAO() {
	}
	
	/**
	 * Set session Factory interceptor
	 * 
	 * @param sessionFactory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#createSyncRecord(org.openmrs.module.sync.SyncRecord)
	 */
	public void createSyncRecord(SyncRecord record) throws DAOException {
		if (record.getUuid() == null) {
			//TODO: Create Uuid if missing?
			throw new DAOException("SyncRecord must have a UUID");
		}

		Session session = sessionFactory.getCurrentSession();
		try {
			session.save(record);
		}
		catch (ConstraintViolationException e) {
			sessionFactory.getCurrentSession().clear();
			SyncRecord existingRecord = getSyncRecord(record.getUuid());
			if (existingRecord != null) {
				//compare the contents
				if (existingRecord.equals(record)) {
					//this was an identical record getting re-saved, ignore the exception
					return;
				}
			}
			throw e;
		}
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#updateSyncRecord(org.openmrs.module.sync.SyncRecord)
	 */
	public void updateSyncRecord(SyncRecord record) throws DAOException {
		Session session = sessionFactory.getCurrentSession();
		session.saveOrUpdate(record);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#deleteSyncRecord(org.openmrs.module.sync.SyncRecord)
	 */
	public void deleteSyncRecord(SyncRecord record) throws DAOException {
		Session session = sessionFactory.getCurrentSession();
		session.delete(record);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#createSyncImportRecord(org.openmrs.module.sync.engine.SyncImportRecord)
	 */
	public void createSyncImportRecord(SyncImportRecord record) throws DAOException {
		if (record.getUuid() == null) {
			//TODO: Create Uuid if missing?
			throw new DAOException("SyncImportRecord must have a UUID");
		}
		Session session = sessionFactory.getCurrentSession();
		session.save(record);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#updateSyncImportRecord(org.openmrs.module.sync.engine.SyncImportRecord)
	 */
	public void updateSyncImportRecord(SyncImportRecord record) throws DAOException {
		Session session = sessionFactory.getCurrentSession();
		session.merge(record);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#deleteSyncImportRecord(org.openmrs.module.sync.engine.SyncImportRecord)
	 */
	public void deleteSyncImportRecord(SyncImportRecord record) throws DAOException {
		Session session = sessionFactory.getCurrentSession();
		session.delete(record);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#deleteSyncImportRecordsByServer(java.lang.Integer)
	 */
	public void deleteSyncImportRecordsByServer(Integer serverId) throws DAOException {
		Session session = sessionFactory.getCurrentSession();
		session.createSQLQuery("delete from sync_import where source_server_id =:serverId").setInteger("serverId",serverId).executeUpdate();                    
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getNextSyncRecord()
	 */
	@SuppressWarnings("unchecked")
	public SyncRecord getFirstSyncRecordInQueue() throws DAOException {
		List<SyncRecord> result = sessionFactory.getCurrentSession().createCriteria(SyncRecord.class)
		        .add(Restrictions.in("state", new SyncRecordState[] { SyncRecordState.NEW, SyncRecordState.PENDING_SEND }))
		        .addOrder(Order.asc("timestamp")).addOrder(Order.asc("recordId")).setFetchSize(1).list();
		
		if (result.size() < 1) {
			return null;
		} else {
			return result.get(0);
		}
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getNextSyncRecord()
	 */
	@SuppressWarnings("unchecked")
	public SyncRecord getLatestRecord() throws DAOException {
		List<Integer> result = sessionFactory.getCurrentSession().createCriteria(SyncRecord.class)
		        .setProjection(Projections.max("recordId")).list();
		
		if (result.size() < 1) {
			return null;
		} else {
			Integer maxRecordId = result.get(0);
			return getSyncRecord(maxRecordId);
		}
	}
	
	@SuppressWarnings("unchecked")
	public SyncRecord getEarliestRecord(Date afterDate) throws DAOException {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(SyncRecord.class)
		        .setProjection(Projections.min("recordId"));
		
		if (afterDate != null)
			criteria.add(Restrictions.ge("timestamp", afterDate));
		
		List<Integer> result = criteria.list();
		
		if (result.size() < 1) {
			return null;
		} else {
			Integer minRecordId = result.get(0);
			return getSyncRecord(minRecordId);
		}
	}

	/**
	 * @see org.openmrs.module.sync.api.SyncService#getNextRecord(SyncRecord)
	 */
	public SyncRecord getNextRecord(SyncRecord record) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(SyncRecord.class);
		criteria.setProjection(Projections.min("recordId"));
		criteria.add(Restrictions.gt("recordId", record.getRecordId()));
		Object nextRecordId = criteria.uniqueResult();
		if (nextRecordId == null) {
			return null;
		}
		return getSyncRecord((Integer)nextRecordId);
	}

	/**
	 * @see org.openmrs.module.sync.api.SyncService#getPreviousRecord(SyncRecord)
	 */
	public SyncRecord getPreviousRecord(SyncRecord record) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(SyncRecord.class);
		criteria.setProjection(Projections.max("recordId"));
		criteria.add(Restrictions.lt("recordId", record.getRecordId()));
		Object prevRecordId = criteria.uniqueResult();
		if (prevRecordId == null) {
			return null;
		}
		return getSyncRecord((Integer)prevRecordId);
	}
	
	@SuppressWarnings("unchecked")
	public List<SyncRecord> getSyncRecords(String query) throws DAOException {
		return sessionFactory
		        .getCurrentSession()
		        .createCriteria(SyncRecord.class)
		        .add(
		            Expression.or(Restrictions.like("items", query, MatchMode.ANYWHERE),
		                Restrictions.eq("originalUuid", query))).addOrder(Order.desc("timestamp")).setMaxResults(250) // max number of records returned
		        .list();
	}
	
	public SyncRecord getSyncRecord(Integer recordId) throws DAOException {
		return (SyncRecord) sessionFactory.getCurrentSession().createCriteria(SyncRecord.class)
		        .add(Restrictions.eq("recordId", recordId)).uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecord(java.lang.String)
	 */
	public SyncRecord getSyncRecord(String uuid) throws DAOException {
		return (SyncRecord) sessionFactory.getCurrentSession().createCriteria(SyncRecord.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public SyncRecord getSyncRecordByOriginalUuid(String originalUuid) throws DAOException {
		return (SyncRecord) sessionFactory.getCurrentSession().createCriteria(SyncRecord.class)
		        .add(Restrictions.eq("originalUuid", originalUuid)).uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncImportRecord(java.lang.String)
	 */
	public SyncImportRecord getSyncImportRecord(String uuid) throws DAOException {
		return (SyncImportRecord) sessionFactory.getCurrentSession().createCriteria(SyncImportRecord.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncImportRecords(org.openmrs.module.sync.engine.SyncRecordState)
	 */
	@SuppressWarnings("unchecked")
	public List<SyncImportRecord> getSyncImportRecords(SyncRecordState... state) throws DAOException {
		return sessionFactory.getCurrentSession().createCriteria(SyncImportRecord.class)
		        .add(Restrictions.in("state", state)).addOrder(Order.asc("timestamp")).addOrder(Order.asc("importId"))
		        .list();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecords()
	 */
	@SuppressWarnings("unchecked")
	public List<SyncRecord> getSyncRecords() throws DAOException {
		return sessionFactory.getCurrentSession().createCriteria(SyncRecord.class).addOrder(Order.asc("timestamp"))
		        .addOrder(Order.asc("recordId")).list();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecords(org.openmrs.module.sync.engine.SyncRecordState)
	 */
	@SuppressWarnings("unchecked")
	public List<SyncRecord> getSyncRecords(SyncRecordState state) throws DAOException {
		return sessionFactory.getCurrentSession().createCriteria(SyncRecord.class).add(Restrictions.eq("state", state))
		        .addOrder(Order.asc("timestamp")).addOrder(Order.asc("recordId")).list();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecords(org.openmrs.module.sync.SyncRecordState[],
	 *      boolean, java.lang.Integer, org.openmrs.module.sync.server.RemoteServer, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	public List<SyncRecord> getSyncRecords(SyncRecordState[] states, boolean inverse, Integer maxSyncRecords,
	                                       RemoteServer server, Integer firstRecordId) throws DAOException {
		if (maxSyncRecords == null) {
			maxSyncRecords = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_RECORDS_DEFAULT);
		}
		
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(SyncRecord.class, "s");
		
		String column = "s.state";
		
		if (server != null) {
			criteria = criteria.createCriteria("serverRecords", "sr");
			criteria.add(Restrictions.eq("sr.syncServer", server));
			column = "sr.state";
		}
		
		if (inverse)
			criteria.add(Restrictions.not(Restrictions.in(column, states)));
		else
			criteria.add(Restrictions.in(column, states));
		
		if (firstRecordId != null)
			criteria.add(Restrictions.ge("s.recordId", firstRecordId));
		
		criteria.addOrder(Order.asc("s.timestamp"));
		criteria.addOrder(Order.asc("s.recordId"));
		
		// if the user sets -1 as the max records, don't restrict the number of records downloaded/transferred
		if (maxSyncRecords > 0)
			criteria.setMaxResults(maxSyncRecords);
		
		return criteria.list();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#deleteSyncRecords(org.openmrs.module.sync.SyncRecordState[],
	 *      java.util.Date)
	 */
	public Integer deleteSyncRecords(SyncRecordState[] states, Date to) throws DAOException {
		List<String> stateStrings = new ArrayList<String>();
		for (SyncRecordState s : states) {
			stateStrings.add(s.name());
		}
		
		// no matter what kind of server this current server is (parent or child), the sync server records
		// must be either committed or not syncing in order to delete them
		String[] syncServerStates = new String[] { SyncRecordState.NOT_SUPPOSED_TO_SYNC.name(),
		        SyncRecordState.COMMITTED.name() };
		
		// delete all rows in sync_server_id that are of the right state and are old
		Query deleteSSRQuery = sessionFactory
		        .getCurrentSession()
		        .createSQLQuery(
		            "delete from sync_server_record where state in (:states) and (select timestamp from sync_record sr where sr.record_id = sync_server_record.record_id) < :to");
		deleteSSRQuery.setParameterList("states", syncServerStates);
		deleteSSRQuery.setDate("to", to);
		Integer quantityDeleted = deleteSSRQuery.executeUpdate(); // this quantity isn't really used
		
		// if a sync_record now has zero sync_record_server rows, then that means all
		// the rows were deleted in the previous query and so the sync_record can also be deleted
		Query deleteQuery = sessionFactory
		        .getCurrentSession()
		        .createSQLQuery(
		            "delete from sync_record where (select count(*) from sync_server_record ssr where ssr.record_id = sync_record.record_id) = 0 and sync_record.timestamp <= :to and sync_record.state in (:states)");
		deleteQuery.setDate("to", to);
		deleteQuery.setParameterList("states", stateStrings);
		quantityDeleted = deleteQuery.executeUpdate();
		
		return quantityDeleted;
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncRecords(java.util.Date, java.util.Date,
	 *      Integer, Integer)
	 */
	@SuppressWarnings("unchecked")
	public List<SyncRecord> getSyncRecords(Date from, Date to, Integer firstRecordId, Integer numberToReturn,
	                                       boolean oldestToNewest) throws DAOException {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(SyncRecord.class);
		
		if (from != null)
			criteria.add(Restrictions.gt("timestamp", from)); // greater than
			
		if (to != null)
			criteria.add(Restrictions.le("timestamp", to)); // less-than or equal
			
		if (numberToReturn != null)
			criteria.setMaxResults(numberToReturn);
		
		if (oldestToNewest) {
			if (firstRecordId != null)
				criteria.add(Restrictions.ge("recordId", firstRecordId));
			
			criteria.addOrder(Order.asc("timestamp"));
			criteria.addOrder(Order.asc("recordId"));
		} else {
			if (firstRecordId != null)
				criteria.add(Restrictions.le("recordId", firstRecordId));
			
			criteria.addOrder(Order.desc("timestamp"));
			criteria.addOrder(Order.desc("recordId"));
		}
		
		return criteria.list();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
	 */
	public String getGlobalProperty(String propertyName) throws DAOException {
		
		if (propertyName == null)
			throw new DAOException("Cannot retrieve property with null property name.");
		
		GlobalProperty gp = (GlobalProperty) sessionFactory.getCurrentSession().get(GlobalProperty.class, propertyName);
		
		if (gp == null)
			return null;
		
		return gp.getPropertyValue();
		
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#setGlobalProperty(String propertyName, String
	 *      propertyValue)
	 */
	public void setGlobalProperty(String propertyName, String propertyValue) throws DAOException {
		
		if (propertyName == null)
			throw new DAOException("Cannot set property with null property name.");
		
		Session session = sessionFactory.getCurrentSession();
		
		// try to look up the global property first so we use the same uuid for the gp
		GlobalProperty gp = (GlobalProperty) session.get(GlobalProperty.class, propertyName);
		if (gp == null) {
			// the gp doesn't exist, create a new one with a new uuid now
			gp = new GlobalProperty(propertyName, propertyValue);
			gp.setUuid(SyncUtil.generateUuid());
		} else {
			gp.setPropertyValue(propertyValue);
		}
		
		session.merge(gp);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#saveRemoteServer(org.openmrs.module.sync.engine.RemoteServer)
	 */
	public RemoteServer saveRemoteServer(RemoteServer server) throws DAOException {
		Session session = sessionFactory.getCurrentSession();
		return (RemoteServer) session.merge(server);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#deleteRemoteServer(org.openmrs.module.sync.engine.RemoteServer)
	 */
	public void deleteRemoteServer(RemoteServer server) throws DAOException {
		Session session = sessionFactory.getCurrentSession();
		deleteSyncImportRecordsByServer(server.getServerId());
		// trying to speed up the deletion process...
		session.createSQLQuery("delete from sync_server_record where server_id =:serverId").setInteger("serverId", server.getServerId()).executeUpdate();
		session.delete(server);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
	 */
	public RemoteServer getRemoteServer(Integer serverId) throws DAOException {
		return (RemoteServer) sessionFactory.getCurrentSession().get(RemoteServer.class, serverId);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
	 */
	public RemoteServer getRemoteServer(String uuid) throws DAOException {
		return (RemoteServer) sessionFactory.getCurrentSession().createCriteria(RemoteServer.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
	 */
	public RemoteServer getRemoteServerByUsername(String username) throws DAOException {
		return (RemoteServer) sessionFactory.getCurrentSession().createCriteria(RemoteServer.class)
		        .add(Restrictions.eq("childUsername", username)).uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
	 */
	@SuppressWarnings("unchecked")
	public List<RemoteServer> getRemoteServers() throws DAOException {
		return (List<RemoteServer>) sessionFactory.getCurrentSession().createCriteria(RemoteServer.class).list();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getGlobalProperty(String propertyName)
	 */
	public RemoteServer getParentServer() throws DAOException {
		return (RemoteServer) sessionFactory.getCurrentSession().createCriteria(RemoteServer.class)
		        .add(Restrictions.eq("serverType", RemoteServerType.PARENT)).uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#saveSyncClass(org.openmrs.module.sync.SyncClass)
	 */
	public void saveSyncClass(SyncClass syncClass) throws DAOException {
		Session session = sessionFactory.getCurrentSession();
		session.saveOrUpdate(syncClass);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#deleteSyncClass(org.openmrs.module.sync.SyncClass)
	 */
	public void deleteSyncClass(SyncClass syncClass) throws DAOException {
		Session session = sessionFactory.getCurrentSession();
		session.delete(syncClass);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncClass(Integer)
	 */
	public SyncClass getSyncClass(Integer syncClassId) throws DAOException {
		return (SyncClass) sessionFactory.getCurrentSession().get(SyncClass.class, syncClassId);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncClasses()
	 */
	@SuppressWarnings("unchecked")
	public List<SyncClass> getSyncClasses() throws DAOException {
		
		List<SyncClass> classes = (List<SyncClass>) sessionFactory.getCurrentSession().createCriteria(SyncClass.class)
		        .addOrder(Order.asc("name")).list();
		
		if (classes == null && log.isWarnEnabled())
			log.warn("getSyncClasses is null.");
		
		return classes;
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncClassByName(String)
	 */
	public SyncClass getSyncClassByName(String className) throws DAOException {
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(SyncClass.class)
		        .add(Expression.eq("name", className));
		
		return (SyncClass) crit.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#deleteOpenmrsObject(org.openmrs.synchronization.OpenmrsObject)
	 */
	public void deleteOpenmrsObject(OpenmrsObject o) throws DAOException {
		sessionFactory.getCurrentSession().delete(o);
	}
	
	/**
	 * Sets hibernate flush mode to org.hibernate.FlushMode.MANUAL.
	 * 
	 * @return true if the flush mode was set already manual
	 * @see #isFlushModeManual()
	 * @see org.hibernate.FlushMode
	 * @see org.openmrs.module.sync.api.db.SyncDAO#setFlushModeManual()
	 */
	public boolean setFlushModeManual() throws DAOException {
		boolean isManualAlready = isFlushModeManual();
		
		// don't need to set it if its already manual
		if (!isManualAlready)
			sessionFactory.getCurrentSession().setFlushMode(org.hibernate.FlushMode.MANUAL);
		
		return isManualAlready;
	}
	
	/**
	 * Sets hibernate flush mode to org.hibernate.FlushMode.AUTO.
	 * 
	 * @see org.hibernate.FlushMode
	 * @see org.openmrs.module.sync.api.db.SyncDAO#setFlushModeAutomatic()
	 */
	public void setFlushModeAutomatic() throws DAOException {
		sessionFactory.getCurrentSession().setFlushMode(org.hibernate.FlushMode.AUTO);
	}
	
	/**
	 * Returns true if the flush mode is currently set to manual
	 * 
	 * @return true if the flush mode is manual
	 * @throws DAOException
	 */
	public boolean isFlushModeManual() throws DAOException {
		return FlushMode.isManualFlushMode(sessionFactory.getCurrentSession().getFlushMode());
	}
	
	/**
	 * Executes hibernate flush.
	 * 
	 * @see org.hibernate.Session#flush()
	 * @see org.openmrs.module.sync.api.db.SyncDAO#flushSession()
	 */
	public void flushSession() throws DAOException {
		sessionFactory.getCurrentSession().flush();
	}
	
	/**
	 * Performs generic save of openmrs object using Hibernate session.saveorupdate.
	 * 
	 * @throws DAOException
	 */
	public void saveOrUpdate(Object object) throws DAOException {
		sessionFactory.getCurrentSession().saveOrUpdate(object);
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncStatistics(java.util.Date, java.util.Date)
	 */
	@SuppressWarnings("unchecked")
	public Map<RemoteServer, LinkedHashSet<SyncStatistic>> getSyncStatistics(Date fromDate, Date toDate) throws DAOException {
		
		//first get the list of remote servers and make map out of it
		List<RemoteServer> servers = this.getRemoteServers();
		
		Map<RemoteServer, LinkedHashSet<SyncStatistic>> map = new HashMap<RemoteServer, LinkedHashSet<SyncStatistic>>();
		
		String hqlChild = "select rs.nickname, ssr.state, count(*) " + "from RemoteServer rs join rs.serverRecords as ssr "
		        + "where rs.serverId = :server_id and ssr.state  <> '" + SyncRecordState.NOT_SUPPOSED_TO_SYNC.toString()
		        + "' " + "group by rs.nickname, ssr.state " + "order by nickname, state";
		
		String hqlParent = "select count(*) from SyncRecord where originalUuid = uuid and state <> '"
		        + SyncRecordState.COMMITTED.toString() + "' and state <> '"
		        + SyncRecordState.NOT_SUPPOSED_TO_SYNC.toString() + "'";
		
		//for each server configured, get its stats
		for (RemoteServer r : servers) {
			if (r.getServerType() == RemoteServerType.CHILD) {
				Query q = sessionFactory.getCurrentSession().createQuery(hqlChild);
				q.setParameter("server_id", r.getServerId());
				List<Object[]> rows = q.list();
				LinkedHashSet<SyncStatistic> props = new LinkedHashSet<SyncStatistic>();
				for (Object[] row : rows) {
					SyncStatistic stat = new SyncStatistic(SyncStatistic.Type.SYNC_RECORD_COUNT_BY_STATE, row[1].toString(),
					        row[2]); //state/count
					props.add(stat);
				}
				map.put(r, props);
			} else {
				//for parent servers, get the number of records in sync record
				Query q = sessionFactory.getCurrentSession().createQuery(hqlParent);
				Long count = (Long) q.uniqueResult();
				LinkedHashSet<SyncStatistic> props = new LinkedHashSet<SyncStatistic>();
				if (count != null) {
					props.add(new SyncStatistic(SyncStatistic.Type.SYNC_RECORD_COUNT_BY_STATE, "AWAITING", count)); //count
				}
				map.put(r, props);
			}
		}
		
		return map;
	}
	
	/*
	 * called at Openmrs sync parent server: exports the openmrs database to a
	 * DDL output stream for sending it back to a new child node being created
	 */
	public void exportChildDB(String uuidForChild, OutputStream os) throws DAOException {
		PrintStream out = new PrintStream(os);
		Set<String> tablesToSkip = new HashSet<String>();
		{
			tablesToSkip.add("hl7_in_archive");
			tablesToSkip.add("hl7_in_queue");
			tablesToSkip.add("hl7_in_error");
			tablesToSkip.add("formentry_archive");
			tablesToSkip.add("formentry_queue");
			tablesToSkip.add("formentry_error");
			tablesToSkip.add("sync_class");
			tablesToSkip.add("sync_import");
			tablesToSkip.add("sync_record");
			tablesToSkip.add("sync_server");
			tablesToSkip.add("sync_server_class");
			tablesToSkip.add("sync_server_record");
			// TODO: figure out which other tables to skip
			// tablesToSkip.add("obs");
			// tablesToSkip.add("concept");
			// tablesToSkip.add("patient");
		}
		List<String> tablesToDump = new ArrayList<String>();
		Session session = sessionFactory.getCurrentSession();
		String schema = (String) session.createSQLQuery("SELECT schema()").uniqueResult();
		log.warn("schema: " + schema);
		// Get all tables that we'll need to dump
		{
			Query query = session
			        .createSQLQuery("SELECT tabs.table_name FROM INFORMATION_SCHEMA.TABLES tabs WHERE tabs.table_schema = '"
			                + schema + "'");
			for (Object tn : query.list()) {
				String tableName = (String) tn;
				if (!tablesToSkip.contains(tableName.toLowerCase()))
					tablesToDump.add(tableName);
			}
		}
		log.warn("tables to dump: " + tablesToDump);
		
		String thisServerGuid = getGlobalProperty(SyncConstants.PROPERTY_SERVER_UUID);
		
		// Write the DDL Header as mysqldump does
		{
			out.println("-- ------------------------------------------------------");
			out.println("-- Database dump to create an openmrs child server");
			out.println("-- Schema: " + schema);
			out.println("-- Parent GUID: " + thisServerGuid);
			out.println("-- Parent version: " + OpenmrsConstants.OPENMRS_VERSION);
			out.println("-- ------------------------------------------------------");
			out.println("");
			out.println("/*!40101 SET CHARACTER_SET_CLIENT=utf8 */;");
			out.println("/*!40101 SET NAMES utf8 */;");
			out.println("/*!40103 SET TIME_ZONE='+00:00' */;");
			out.println("/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;");
			out.println("/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;");
			out.println("/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;");
			out.println("/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;");
			out.println("/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;");
			out.println("/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;");
			out.println("/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;");
			out.println("/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;");
			out.println("");
		}
		try {
			// JDBC way of doing this
			// Connection conn =
			// DriverManager.getConnection("jdbc:mysql://localhost/" + schema,
			// "test", "test");
			Connection conn = sessionFactory.getCurrentSession().connection();
			try {
				Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
				
				// Get the create database statement
				ResultSet rs = st.executeQuery("SHOW CREATE DATABASE " + schema);
				for (String tableName : tablesToDump) {
					out.println();
					out.println("--");
					out.println("-- Table structure for table `" + tableName + "`");
					out.println("--");
					out.println("DROP TABLE IF EXISTS `" + tableName + "`;");
					out.println("SET @saved_cs_client     = @@character_set_client;");
					out.println("SET character_set_client = utf8;");
					rs = st.executeQuery("SHOW CREATE TABLE " + tableName);
					while (rs.next()) {
						out.println(rs.getString("Create Table") + ";");
					}
					out.println("SET character_set_client = @saved_cs_client;");
					out.println();
					
					{
						out.println("-- Dumping data for table `" + tableName + "`");
						out.println("LOCK TABLES `" + tableName + "` WRITE;");
						out.println("/*!40000 ALTER TABLE `" + tableName + "` DISABLE KEYS */;");
						boolean first = true;
						
						rs = st.executeQuery("select * from " + tableName);
						ResultSetMetaData md = rs.getMetaData();
						int numColumns = md.getColumnCount();
						int rowNum = 0;
						boolean insert = false;
						
						while (rs.next()) {
							if (rowNum == 0) {
								insert = true;
								out.print("INSERT INTO `" + tableName + "` VALUES ");
							}
							++rowNum;
							if (first) {
								first = false;
							} else {
								out.print(", ");
							}
							if (rowNum % 20 == 0) {
								out.println();
							}
							out.print("(");
							for (int i = 1; i <= numColumns; ++i) {
								if (i != 1) {
									out.print(",");
								}
								if (rs.getObject(i) == null) {
									out.print("NULL");
								} else {
									switch (md.getColumnType(i)) {
										case Types.VARCHAR:
										case Types.CHAR:
										case Types.LONGVARCHAR:
											out.print("'");
											out.print(rs.getString(i).replaceAll("\n", "\\\\n").replaceAll("'", "\\\\'"));
											out.print("'");
											break;
										case Types.BIGINT:
										case Types.DECIMAL:
										case Types.NUMERIC:
											out.print(rs.getBigDecimal(i));
											break;
										case Types.BIT:
											out.print(rs.getBoolean(i));
											break;
										case Types.INTEGER:
										case Types.SMALLINT:
										case Types.TINYINT:
											out.print(rs.getInt(i));
											break;
										case Types.REAL:
										case Types.FLOAT:
										case Types.DOUBLE:
											out.print(rs.getDouble(i));
											break;
										case Types.BLOB:
										case Types.VARBINARY:
										case Types.LONGVARBINARY:
											Blob blob = rs.getBlob(i);
											out.print("'");
											InputStream in = blob.getBinaryStream();
											while (true) {
												int b = in.read();
												if (b < 0) {
													break;
												}
												char c = (char) b;
												if (c == '\'') {
													out.print("\'");
												} else {
													out.print(c);
												}
											}
											out.print("'");
											break;
										case Types.CLOB:
											out.print("'");
											out.print(rs.getString(i).replaceAll("\n", "\\\\n").replaceAll("'", "\\\\'"));
											out.print("'");
											break;
										case Types.DATE:
											out.print("'" + rs.getDate(i) + "'");
											break;
										case Types.TIMESTAMP:
											out.print("'" + rs.getTimestamp(i) + "'");
											break;
										default:
											throw new RuntimeException("TODO: handle type code " + md.getColumnType(i)
											        + " (name " + md.getColumnTypeName(i) + ")");
									}
								}
							}
							out.print(")");
						}
						if (insert) {
							out.println(";");
							insert = false;
						}
						
						out.println("/*!40000 ALTER TABLE `" + tableName + "` ENABLE KEYS */;");
						out.println("UNLOCK TABLES;");
						out.println();
					}
				}
			}
			finally {
				conn.close();
			}
			
			// Now we mark this as a child
			out.println("-- Now mark this as a child database");
			if (uuidForChild == null)
				uuidForChild = SyncUtil.generateUuid();
			out.println("update global_property set property_value = '" + uuidForChild + "' where property = '"
			        + SyncConstants.PROPERTY_SERVER_UUID + "';");
			
			// Write the footer of the DDL script
			{
				out.println("/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;");
				out.println("/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;");
				out.println("/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;");
				out.println("/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;");
				out.println("/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;");
				out.println("/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;");
				out.println("/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;");
				out.println("/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;");
			}
			out.flush();
			out.close();
		}
		catch (IOException ex) {
			log.error("IOException", ex);
			
		}
		catch (SQLException ex) {
			log.error("SQLException", ex);
		}
	}
	
	/*
	 * Called at Openmrs sync child: imports the DDL backup of the parent server
	 * from an input stream generated from the parent DB @Impl: Reads the DDL
	 * statement line by line and updates the child DB
	 */
	public void importParentDB(InputStream in) {
		try {
			Connection conn = sessionFactory.getCurrentSession().connection();
			Statement statement = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			StringBuffer query = new StringBuffer();
			boolean queryEnds = false;
			
			while ((line = reader.readLine()) != null) {
				if ((line.startsWith("#") || line.startsWith("--"))) {
					continue;
				}
				queryEnds = line.endsWith(";");
				query.append(line);
				if (queryEnds) {
					try {
						statement.execute(query.toString());
					}
					catch (SQLException ex) {
						log.warn(ex);
						ex.printStackTrace();
					}
					query.setLength(0);
				}
			}
			in.close();
			
		}
		catch (IOException ex) {
			log.warn(ex);
			ex.printStackTrace();
		}
		catch (SQLException ex) {
			log.warn(ex);
			ex.printStackTrace();
		}
	}
	
	public void generateDataFile(File outFile, String[] ignoreTables) {
		// TODO totally breaks if someone isn't using mysql as the backend
		// TODO get custom location of mysql instead of just relying on path?
		// TODO make this linux compatible
		String[] props = getConnectionProperties();
		String username = props[0];
		String password = props[1];
		String database = props[2];
		String host = props[3];
		String port = props[4];
		try {
			if (!outFile.exists())
				outFile.createNewFile();
		}
		catch (IOException io) {
			log.warn(io.toString());
		}
		
		List<String> commands = new ArrayList<String>();
		commands.add("mysqldump");
		commands.add("-u" + username);
		commands.add("-p" + password);
		commands.add("-h" + host);
		commands.add("-P" + port);
		commands.add("--hex-blob");
		commands.add("-q");
		commands.add("-e");
		commands.add("--single-transaction");
		commands.add("-r");
		commands.add(outFile.getAbsolutePath());
		commands.add(database);
		
		// mark the tables to ignore
		for (String table : ignoreTables) {
			table = table.trim();
			if (StringUtils.hasLength(table)) {
				commands.add("--ignore-table");
				commands.add(database + "." + table);
			}
		}
		
		String output;
		if (OpenmrsConstants.UNIX_BASED_OPERATING_SYSTEM)
			output = execCmd(outFile.getParentFile(), commands.toArray(new String[] {}));
		else
			output = execCmd(null, commands.toArray(new String[] {}));
		
		if (output != null && output.length() > 0) {
			log.debug("Exec called: " + Arrays.asList(commands));
			log.debug("Output of exec: " + output);
		}
		
	}
	
	/**
	 * Auto generated method comment
	 * 
	 * @return
	 */
	private String[] getConnectionProperties() {
		Properties props = Context.getRuntimeProperties();
		
		// username, password, database, host, port
		String[] connProps = { "test", "test", "openmrs", "localhost", "3306" };
		
		String username = (String) props.get("database.username");
		if (username == null)
			username = (String) props.get("connection.username");
		if (username != null)
			connProps[0] = username;
		
		String password = (String) props.get("database.password");
		if (password == null)
			password = (String) props.get("connection.password");
		if (password != null)
			connProps[1] = password;
		// get database name
		String database = "openmrs";
		String host = "localhost";
		String port = "3306";
		String connectionUrl = (String) props.get("connection.url");
		if (connectionUrl == null)
			connectionUrl = (String) props.get("connection.url");
		if (connectionUrl != null) {
			//jdbc:mysql://localhost:3306/openmrs
			//jdbc:mysql:mxj://127.0.0.1:3317/openmrs?
			//Assuming the last full colon will be that before the port
			int lastColonPos = connectionUrl.lastIndexOf(':');
			int slash = connectionUrl.indexOf('/', lastColonPos);
			int qmark = connectionUrl.indexOf('?', lastColonPos);
			database = connectionUrl.substring(slash + 1, qmark != -1 ? qmark : connectionUrl.length());
			connProps[2] = database;
			
			//Assuming that port is explicitly set
			int doubleSlashPos = connectionUrl.indexOf("://");
			host = connectionUrl.substring(doubleSlashPos + 3, lastColonPos);
			connProps[3] = host;
			
			port = connectionUrl.substring(lastColonPos + 1, slash);
			connProps[4] = port;
		}
		
		return connProps;
	}
	
	/**
	 * @param cmdWithArguments
	 * @param wd
	 * @return
	 */
	private String execCmd(File wd, String[] cmdWithArguments) {
		log.debug("executing command: " + Arrays.toString(cmdWithArguments));
		
		StringBuffer out = new StringBuffer();
		try {
			Process p = (wd != null) ? Runtime.getRuntime().exec(cmdWithArguments, null, wd) : Runtime.getRuntime().exec(
			    cmdWithArguments);
			
			out.append("Normal cmd output:\n");
			Reader reader = new InputStreamReader(p.getInputStream());
			BufferedReader input = new BufferedReader(reader);
			int readChar = 0;
			while ((readChar = input.read()) != -1) {
				out.append((char) readChar);
			}
			input.close();
			reader.close();
			
			out.append("ErrorStream cmd output:\n");
			reader = new InputStreamReader(p.getErrorStream());
			input = new BufferedReader(reader);
			readChar = 0;
			while ((readChar = input.read()) != -1) {
				out.append((char) readChar);
			}
			input.close();
			reader.close();
			
			Integer exitValue = p.waitFor();
			
			log.debug("Process exit value: " + exitValue);
			
		}
		catch (Exception e) {
			log.error("Error while executing command: '" + cmdWithArguments + "'", e);
		}
		
		log.debug("execCmd output: \n" + out.toString());
		
		return out.toString();
	}
	
	public void execGeneratedFile(File generatedDataFile) {
		// TODO this depends on mysql being on the path
		// TODO fix this so that queries are parsed out and run linebyline?
		
		String[] props = getConnectionProperties();
		String username = props[0];
		String password = props[1];
		String database = props[2];
		String host = props[3];
		String port = props[4];
		
		String path = generatedDataFile.getAbsolutePath();
		path = path.replace("\\", "/");
		// replace windows file separator with
		// forward slash
		
		String[] commands = { "mysql", "-e", "source " + path, "-f", "-u" + username, "-p" + password,
				"-h" + host, "-P" + port, "-D" + database };
		String output;
		if (OpenmrsConstants.UNIX_BASED_OPERATING_SYSTEM)
			output = execCmd(generatedDataFile.getParentFile(), commands);
		else
			output = execCmd(null, commands);
		
		if (output != null && output.length() > 0) {
			log.error("Exec call: " + Arrays.asList(commands));
			log.error("Output of exec: " + output);
		}
	}
	
	public <T extends OpenmrsObject> T getOpenmrsObjectByUuid(Class<T> clazz, String uuid) {
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(clazz);
		crit.add(Restrictions.eq("uuid", uuid));
		return (T) crit.uniqueResult();
	}
	
	public <T extends OpenmrsObject> T getOpenmrsObjectByPrimaryKey(String classname, Object primaryKey) {
		Criteria crit;
		try {
			crit = sessionFactory.getCurrentSession().createCriteria(Context.loadClass(classname));
			crit.add(Restrictions.idEq(primaryKey));
			return (T) crit.uniqueResult();
		}
		catch (ClassNotFoundException e) {
			log.warn("getOpenmrsObjectByPrimaryKey couldn't find class: " + classname, e);
			return null;
		}
	}
	
	public <T extends OpenmrsObject> String getUuidForOpenmrsObject(Class<T> clazz, String id) {
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(clazz);
		crit.add(Restrictions.idEq(id));
		crit.setProjection(Projections.property("uuid"));
		List<Object[]> rows = crit.list();
		Object[] rowOne = rows.get(0);
		return (String) rowOne[0]; // get the first column of the first row
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#processCollection(java.lang.Class,
	 *      java.lang.String, java.lang.String)
	 */
	public void processCollection(Class collectionType, String incoming, String originalRecordUuid) throws Exception {
		
		OpenmrsObject owner = null;
		String ownerClassName = null;
		String ownerCollectionPropertyName = null;
		String ownerUuid = null;
		String ownerCollectionAction = null; //is this coll update or recreate?
		NodeList nodes = null;
		Set entries = null;
		int i = 0;
		boolean needsRecreate = false;
		
		//first find out what kid of set we are dealing with:
		//Hibernate PersistentSortedSet == TreeSet, note this is derived from PersistentSet so we have to test for it first
		//Hibernate PersistentSet == HashSet
		if (!org.hibernate.collection.PersistentSet.class.isAssignableFrom(collectionType)) {
			//don't know how to process this collection type
			log.error("Do not know how to process this collection type: " + collectionType.getName());
			throw new SyncIngestException(SyncConstants.ERROR_ITEM_BADXML_MISSING, null, incoming, null);
		}
		
		//next, pull out the owner node and get owner instance: 
		//we need reference to owner object before we start messing with collection entries
		nodes = SyncUtil.getChildNodes(incoming);
		if (nodes == null) {
			throw new SyncIngestException(SyncConstants.ERROR_ITEM_BADXML_MISSING, null, incoming, null);
		}
		for (i = 0; i < nodes.getLength(); i++) {
			if ("owner".equals(nodes.item(i).getNodeName())) {
				//pull out collection owner info: class name of owner, its uuid, and name of poperty on owner that holds this collection
				ownerClassName = ((Element) nodes.item(i)).getAttribute("type");
				ownerCollectionPropertyName = ((Element) nodes.item(i)).getAttribute("properyName");
				ownerCollectionAction = ((Element) nodes.item(i)).getAttribute("action");
				ownerUuid = ((Element) nodes.item(i)).getAttribute("uuid");
				break;
			}
		}
		if (ownerUuid == null) {
			log.error("Owner uuid is null while processing collection.");
			throw new SyncIngestException(SyncConstants.ERROR_ITEM_BADXML_MISSING, null, incoming, null);
		}
		owner = (OpenmrsObject) SyncUtil.getOpenmrsObj(ownerClassName, ownerUuid);
		
		//we didn't get the owner record: throw an exception
		//TODO: in future, when we have conflict resolution, this may be handled differently
		if (owner == null) {
			log.error("Cannot retrieve the collection's owner object.");
			log.error("Owner info: " + "\nownerClassName:" + ownerClassName + "\nownerCollectionPropertyName:"
			        + ownerCollectionPropertyName + "\nownerCollectionAction:" + ownerCollectionAction + "\nownerUuid:"
			        + ownerUuid);
			throw new SyncIngestException(SyncConstants.ERROR_ITEM_BADXML_MISSING, null, incoming, null);
		}
		
		//NOTE: we cannot just new up a collection and assign to parent:
		//if hibernate mapping has cascade deletes, it will orphan existing collection and hibernate will throw error
		//to that effect: "A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance"
		//*only* if this is recreate; clear up the existing collection and start over
		Method m = null;
		m = SyncUtil.getGetterMethod(owner.getClass(), ownerCollectionPropertyName);
		if (m == null) {
			log.error("Cannot retrieve getter method for ownerCollectionPropertyName:" + ownerCollectionPropertyName);
			log.error("Owner info: " + "\nownerClassName:" + ownerClassName + "\nownerCollectionPropertyName:"
			        + ownerCollectionPropertyName + "\nownerCollectionAction:" + ownerCollectionAction + "\nownerUuid:"
			        + ownerUuid);
			throw new SyncIngestException(SyncConstants.ERROR_ITEM_BADXML_MISSING, null, incoming, null);
		}
		entries = (Set) m.invoke(owner, (Object[]) null);
		
		/*Two instances where even after this we may need to create a new collection:
		 * a) when collection is lazy=false and it is newly created; then asking parent for it will
		 * not return new & empty proxy, it will return null
		 * b) Special recreate logic:
		 * if fetched owner instance has nothing attached, then it is safe to just create brand new collection
		 * and assign it to owner without worrying about getting orphaned deletes error
		 * if owner has something attached, then we process recreate as delete/update; 
		 * that is clear out the existing entries and then proceed to add ones received via sync. 
		 * This code essentially mimics hibernate org.hibernate.engine.Collections.prepareCollectionForUpdate()
		 * implementation. 
		 * NOTE: The unfortunate bi-product of this approach is that this series of events will not produce 
		 * 'recreate' event in the interceptor: thus parent's sync journal entries will look slightly diferently 
		 * from what child was sending up: child sent up single 'recreate' collection action however
		 * parent will instead have single 'update' with deletes & updates in it. Presumably, this is a distinction
		 * without a difference.
		 */

		if (entries == null) {
			if (org.hibernate.collection.PersistentSortedSet.class.isAssignableFrom(collectionType)) {
				needsRecreate = true;
				entries = new TreeSet();
			} else if (org.hibernate.collection.PersistentSet.class.isAssignableFrom(collectionType)) {
				needsRecreate = true;
				entries = new HashSet();
			}
		}
		
		if (entries == null) {
			log.error("Was not able to retrieve reference to the collection using owner object.");
			log.error("Owner info: " + "\nownerClassName:" + ownerClassName + "\nownerCollectionPropertyName:"
			        + ownerCollectionPropertyName + "\nownerCollectionAction:" + ownerCollectionAction + "\nownerUuid:"
			        + ownerUuid);
			throw new SyncIngestException(SyncConstants.ERROR_ITEM_BADXML_MISSING, null, incoming, null);
		}
		
		//clear existing entries before adding new ones:
		if ("recreate".equals(ownerCollectionAction)) {
			entries.clear();
		}
		
		//now, finally process nodes, phew!!
		for (i = 0; i < nodes.getLength(); i++) {
			if ("entry".equals(nodes.item(i).getNodeName())) {
				String entryClassName = ((Element) nodes.item(i)).getAttribute("type");
				String entryUuid = ((Element) nodes.item(i)).getAttribute("uuid");
				String entryAction = ((Element) nodes.item(i)).getAttribute("action");
				Object entry = SyncUtil.getOpenmrsObj(entryClassName, entryUuid);
				
				// objects like Privilege, Role, and GlobalProperty might have different
				// uuids for different objects
				if (entry == null && SyncUtil.hasNoAutomaticPrimaryKey(entryClassName)) {
					String key = ((Element) nodes.item(i)).getAttribute("primaryKey");
					entry = getOpenmrsObjectByPrimaryKey(entryClassName, key);
				}
				
				if (entry == null) {
					// blindly ignore this entry if it doesn't exist and we're trying to delete it
					if (!"delete".equals(entryAction)) {
						//the object not found: most likely cause here is data collision
						
						log.error("Was not able to retrieve reference to the collection entry object by uuid.");
						log.error("Entry info: " + "\nentryClassName: " + entryClassName + "\nentryUuid: " + entryUuid
						        + "\nentryAction: " + entryAction);
                        log.error("Sync record original uuid: " + originalRecordUuid);
						throw new SyncIngestException(SyncConstants.ERROR_ITEM_UUID_NOT_FOUND, ownerClassName + " missing "
						        + entryClassName + "," + entryUuid, incoming, null);
					}
				} else if ("update".equals(entryAction)) {
					if (!OpenmrsUtil.collectionContains(entries, entry)) {
						entries.add(entry);
					}
				} else if ("delete".equals(entryAction)) {
					OpenmrsUtil.collectionContains(entries, entry);
					
					if (!entries.remove(entry)) {
						//couldn't find entry in collection: hmm, bad implementation of equals?
						//fall back to trying to find the item in entries by uuid
						OpenmrsObject toBeRemoved = null;
						for (Object o : entries) {
							if (o instanceof OpenmrsObject) {
								if (entryUuid.equals(((OpenmrsObject) o).getUuid())) {
									toBeRemoved = (OpenmrsObject) o;
									break;
								}
							}
						}
						if (toBeRemoved == null) {
							//the item to be removed was not located in the collection: log it for reference and continue
							log.warn("Was not able to process collection entry delete.");
							log.warn("Owner info: " + "\nownerClassName:" + ownerClassName
							        + "\nownerCollectionPropertyName:" + ownerCollectionPropertyName
							        + "\nownerCollectionAction:" + ownerCollectionAction + "\nownerUuid:" + ownerUuid);
							log.warn("entry info: " + "\nentryClassName:" + entryClassName + "\nentryUuid:" + entryUuid);
                            log.warn("Sync record original uuid: " + originalRecordUuid);
						} else {
							//finally, remove it from the collection
							entries.remove(toBeRemoved);
						}
					}
					
				} else {
					log.error("Unknown collection entry action, action was: " + entryAction);
					throw new SyncIngestException(SyncConstants.ERROR_ITEM_NOT_COMMITTED, ownerClassName, incoming, null);
				}
			}
		}
		
		/*
		 * Pass the original uuid to interceptor: this will prevent the change
		 * from being sent back to originating server. 
		 */
		HibernateSyncInterceptor.setOriginalRecordUuid(originalRecordUuid);
		
		//assign collection back to the owner if it is recreated
		if (needsRecreate) {
			SyncUtil.setProperty(owner, ownerCollectionPropertyName, entries);
		}
		
		//finally, trigger update
		try {
			//no need to mess around with precommit actions for collections, at least
			//at this point
			SyncUtil.updateOpenmrsObject(owner, ownerClassName, ownerUuid);
		}
		catch (Exception e) {
			log.error("Unexpected exception occurred while processing hibernate collections", e);
			throw new SyncIngestException(SyncConstants.ERROR_ITEM_NOT_COMMITTED, ownerClassName, incoming, null);
		}
	}
	
	/* For full description of how this works read class comments for 
	 * {@link SyncSubclassStub}.
	 * 
	 * (non-Javadoc)
	 * @see org.openmrs.module.sync.api.db.SyncDAO#processSyncSubclassStub(org.openmrs.module.sync.SyncSubclassStub)
	 */
	public void processSyncSubclassStub(SyncSubclassStub stub) {
		Connection connection = sessionFactory.getCurrentSession().connection();
		
		boolean stubInsertNeeded = false;
		PreparedStatement ps = null;
		int internalDatabaseId = 0;
		
		// check if there is a row with a matching person record and no patient record 
		try {
			// becomes something like "select person_id from person where uuid = x" or "select concept_id from concept where uuid = x"
			ps = connection.prepareStatement("SELECT " + stub.getParentTableId() + " FROM " + stub.getParentTable() + " WHERE uuid = ?");
			ps.setString(1, stub.getUuid());
			ps.execute();
			ResultSet rs = ps.getResultSet();
			if (rs.next()) {
				stubInsertNeeded = true;
				internalDatabaseId = rs.getInt(stub.getParentTableId());
			} else {
				stubInsertNeeded = false;
			}
			
			//this should get no rows
			ps = connection.prepareStatement("SELECT " + stub.getSubclassTableId() + " FROM " + stub.getSubclassTable() + " WHERE " + stub.getSubclassTableId() + " = ?");
			ps.setInt(1, internalDatabaseId);
			ps.execute();
			if (ps.getResultSet().next()) {
				stubInsertNeeded = false;
			}
			ps.close();
			ps = null;
			
		}
		catch (SQLException e) {
			log.error("Error while trying to see if this person is a patient already (or concept is a concept numeric already)", e);
		}
		if (ps != null) {
			try {
				ps.close();
			}
			catch (SQLException e) {
				log.error("Error generated while closing statement", e);
			}
		}
		
		if (stubInsertNeeded) {
			try {
				//insert the stub
				String sql = "INSERT INTO " + stub.getSubclassTable() + " (" + stub.getSubclassTableId() + "COLUMNNAMEGOESHERE) VALUES (?COLUMNVALUEGOESHERE)";

				if (CollectionUtils.isNotEmpty(stub.getRequiredColumnNames())
						&& CollectionUtils.isNotEmpty(stub.getRequiredColumnValues())
						&& CollectionUtils.isNotEmpty(stub.getRequiredColumnClasses())) {
					for (int x = 0; x < stub.getRequiredColumnNames().size(); x++) {
						String column = stub.getRequiredColumnNames().get(x);
						sql = sql.replace("COLUMNNAMEGOESHERE", ", " + column + "COLUMNNAMEGOESHERE");
						sql = sql.replace("COLUMNVALUEGOESHERE", ", ?COLUMNVALUEGOESHERE");
					}
				}
				
				sql = sql.replace("COLUMNNAMEGOESHERE", "");
				sql = sql.replace("COLUMNVALUEGOESHERE", "");
				
				ps = connection.prepareStatement(sql);
				
				ps.setInt(1, internalDatabaseId);
				
				if (CollectionUtils.isNotEmpty(stub.getRequiredColumnNames())
						&& CollectionUtils.isNotEmpty(stub.getRequiredColumnValues())
						&& CollectionUtils.isNotEmpty(stub.getRequiredColumnClasses())) {
					
					for (int x = 0; x < stub.getRequiredColumnValues().size(); x++) {
						String value = stub.getRequiredColumnValues().get(x);
						String className = stub.getRequiredColumnClasses().get(x);
						Class c;
						try {
							c = Context.loadClass(className);
							ps.setObject(x + 2, SyncUtil.getNormalizer(c).fromString(c, value));
						} catch (ClassNotFoundException e) {
							log.error("Unable to convert classname into a Class object " + className);
						}
						
					}
				}

				ps.executeUpdate();
				
				//*and* create sync item for this
				HibernateSyncInterceptor.addSyncItemForSubclassStub(stub);
				log.debug("Sync Inserted " + stub.getParentTable() + " Stub for " + stub.getUuid());
			}
			catch (SQLException e) {
				log.warn("SQL Exception while trying to create a " + stub.getParentTable() + " stub", e);
			}
			finally {
				if (ps != null) {
					try {
						ps.close();
					}
					catch (SQLException e) {
						log.error("Error generated while closing statement", e);
					}
				}
			}
		}
		
		return;
	}
	
	/**
	 * (non-Javadoc)
	 * 
	 * @see org.openmrs.module.sync.api.db.SyncDAO#isConceptIdValidForUuid(int, java.lang.String)
	 */
	public boolean isConceptIdValidForUuid(Integer conceptId, String uuid) {
		
		if (uuid == null || conceptId == null) {
			return true;
		}
		
		boolean ret = true; //assume all is well until proven otherwise
		PreparedStatement ps = null;
		Connection connection = sessionFactory.getCurrentSession().connection();
		int foundId = 0;
		String foundUuid = null;
		
		try {
			ps = connection.prepareStatement("SELECT concept_id, uuid FROM concept WHERE (uuid = ? AND concept_id <> ?)"
			        + "OR (uuid <> ? AND concept_id = ?)");
			ps.setString(1, uuid);
			ps.setInt(2, conceptId);
			ps.setString(3, uuid);
			ps.setInt(4, conceptId);
			ps.execute();
			ResultSet rs = ps.getResultSet();
			if (rs.next()) {
				ret = false;
				foundId = rs.getInt("concept_id");
				foundUuid = rs.getString("uuid");
				String msg = "Found inconsistent data during concept ingest." + "\nto be added conceptid/uuid are:"
				        + conceptId + "/" + uuid + "\nfound in DB conceptid/uuid:" + foundId + "/" + foundUuid;
				log.error(msg);
			}
			;
			
			ps.close();
			ps = null;
		}
		catch (SQLException e) {
			log.error("Error while doing isConceptIdValidForUuid.", e);
			ret = false;
		}
		if (ps != null) {
			try {
				ps.close();
			}
			catch (SQLException e) {
				log.error("Error generated while closing statement", e);
			}
		}
		
		return ret;
	}
	
	public Long getCountOfSyncRecords(RemoteServer server, Date from, Date to, SyncRecordState... states) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(SyncRecord.class);
		criteria.setProjection(Projections.rowCount());
		
		if (from != null)
			criteria.add(Restrictions.gt("timestamp", from)); // greater than
			
		if (to != null)
			criteria.add(Restrictions.le("timestamp", to)); // less-than or equal
			
		// only check for matching states if any were passed in
		if (states != null && states.length > 0)
			criteria.add(Restrictions.in("state", states));
		
		if (server != null) {
			criteria.createCriteria("serverRecords", "sr");
			criteria.add(Restrictions.in("sr.state", states));
			criteria.add(Restrictions.eq("sr.syncServer", server));
		}
		
		return (Long) criteria.uniqueResult();
	}
	
	//this is a utility method that i used for Sync-180
	//won't hurt to leave it around -- may be useful in the future
	// MG: I'm commenting this out since the CriteriaLoader method signature has 
	// changed in the most recent version of Hibernate and so this method
	// now causes a compile issue
	
	/**
	private String getSQL(Criteria crit) {
		String ret = "";
		CriteriaImpl c = (CriteriaImpl) crit;
		SessionImpl s = (SessionImpl) c.getSession();
		SessionFactoryImplementor factory = (SessionFactoryImplementor) s.getSessionFactory();
		String[] implementors = factory.getImplementors(c.getEntityOrClassName());
		CriteriaLoader loader = new CriteriaLoader((OuterJoinLoadable) factory.getEntityPersister(implementors[0]), factory,
		        c, implementors[0], s.getEnabledFilters());
		try {
			Field f = OuterJoinLoader.class.getDeclaredField("sql");
			f.setAccessible(true);
			String sql = (String) f.get(loader);
			return sql;
		}
		catch (Exception ex) {
			//pass
		}
		return ret;
	}
	*/
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getOlderSyncRecordInState(org.openmrs.module.sync.SyncRecord,
	 *      java.util.EnumSet)
	 */
	public SyncRecord getOlderSyncRecordInState(SyncRecord syncRecord, EnumSet<SyncRecordState> states) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(SyncRecord.class);
		criteria.createAlias("serverRecords", "serverRecord", Criteria.LEFT_JOIN);
		criteria.add(Restrictions.le("timestamp", syncRecord.getTimestamp()));
		// We need to look for a lower recordId since some may have the same timestamp.
		criteria.add(Restrictions.lt("recordId", syncRecord.getRecordId()));
		// We need to look for errors in both SyncRecord and SyncRecordServer.
		criteria.add(Restrictions.or(Restrictions.in("state", states), Restrictions.in("serverRecord.state", states)));
		criteria.addOrder(Order.desc("timestamp"));
		criteria.addOrder(Order.desc("recordId"));
		criteria.setMaxResults(1);
		return (SyncRecord) criteria.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.sync.api.db.SyncDAO#getSyncServerRecord(java.lang.Integer)
	 */
	@Override
	public SyncServerRecord getSyncServerRecord(Integer syncServerRecordId) throws DAOException {
		return (SyncServerRecord) sessionFactory.getCurrentSession().get(SyncServerRecord.class, syncServerRecordId);
	}

    /**
     * @see SyncDAO#getMostRecentFullyCommittedRecordId()
     */
     public int getMostRecentFullyCommittedRecordId() {
		 Set<String> committedStates = new HashSet<String>();
		 for (SyncRecordState s : SyncConstants.SYNC_RECORD_COMMITTED_STATES) {
			 committedStates.add(s.name());
		 }
		 StringBuilder q = new StringBuilder();
		 if (getParentServer() != null) {
			 q.append("SELECT record_id FROM sync_record WHERE state IN (:states) ORDER BY record_id DESC");
		 }
		 else {
			 q.append("SELECT sr1.record_id FROM sync_record sr1 ");
			 q.append("INNER JOIN sync_server_record ssr1 ON sr1.record_id = ssr1.record_id ");
			 q.append("WHERE ssr1.state IN (:states) AND NOT EXISTS (");
			 q.append("	SELECT sr2.record_id FROM sync_record sr2 ");
			 q.append("	INNER JOIN sync_server_record ssr2 ON sr2.record_id=ssr2.record_id ");
			 q.append("	WHERE sr1.record_id=sr2.record_id AND ssr2.state NOT IN (:states)");
			 q.append(") ORDER BY sr1.record_id DESC");
		 }

         Query allCommittedSSRQuery = sessionFactory.getCurrentSession().createSQLQuery(q.toString());
         allCommittedSSRQuery.setParameterList("states", committedStates);
         allCommittedSSRQuery.setMaxResults(1);

		 Object result = allCommittedSSRQuery.uniqueResult();
         if(result != null) {
             return Integer.parseInt(result.toString());
         }
         return -1;
     }
}
