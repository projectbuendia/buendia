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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.action.BeforeTransactionCompletionProcess;
import org.hibernate.collection.AbstractPersistentCollection;
import org.hibernate.collection.PersistentList;
import org.hibernate.collection.PersistentMap;
import org.hibernate.collection.PersistentSet;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Projections;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.Type;
import org.openmrs.Cohort;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncException;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncItemKey;
import org.openmrs.module.sync.SyncItemState;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncSubclassStub;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Normalizer;
import org.openmrs.module.sync.serialization.Package;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implements 'change interception' for data synchronization feature using Hibernate interceptor
 * mechanism. Intercepted changes are recorded into the synchronization journal table in DB.
 * @see org.hibernate.EmptyInterceptor
 */
public class HibernateSyncInterceptor extends EmptyInterceptor implements ApplicationContextAware, Serializable {

	private static final long serialVersionUID = -4905755656754047400L;
	protected final Log log = LogFactory.getLog(getClass());

	private static HibernateSyncInterceptor instance;
	private ApplicationContext context;
	private static ThreadLocal<SyncRecord> syncRecordHolder = new ThreadLocal<SyncRecord>();

	private HibernateSyncInterceptor() {
		log.info("Initializing the synchronization interceptor");
	}

	/**
	 * @return a new HibernateSyncInterceptor instance, or the existing one to ensure it is a singleton
	 */
	public static HibernateSyncInterceptor getInstance() {
		if (instance == null) {
			instance = new HibernateSyncInterceptor();
		}
		return instance;
	}

	/**
	 * No operation, logging only
	 * @see EmptyInterceptor#afterTransactionBegin(Transaction)
	 */
	@Override
	public void afterTransactionBegin(Transaction tx) {
		if (log.isDebugEnabled()) {
			log.debug("Transaction Started");
		}
	}

	/**
	 * Packages up deletes and sets the item state to DELETED.
	 * @see EmptyInterceptor#onDelete(Object, java.io.Serializable, Object[], String[], org.hibernate.type.Type[])
	 */
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		log.debug("Delete intercepted");
		if (shouldSynchronize(entity)) {
			log.debug("Packaging: " + SyncUtil.formatObject(entity));
			packageObject((OpenmrsObject) entity, state, propertyNames, types, id, SyncItemState.DELETED);
		}
		else {
			log.debug("Entity configured not to sync: " + SyncUtil.formatObject(entity));
		}
	}

	/**
	 * Packages up inserts and sets the item state to NEW
	 * @see EmptyInterceptor#onSave(Object, java.io.Serializable, Object[], String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		log.debug("Insert intercepted");
		if (shouldSynchronize(entity)) {
			log.debug("Packaging: " + SyncUtil.formatObject(entity));
			packageObject((OpenmrsObject) entity, state, propertyNames, types, id, SyncItemState.NEW);
		}
		else {
			log.debug("Entity configured not to sync: " + SyncUtil.formatObject(entity));
		}
		return false; // This means that we did not modify the passed in entity
	}

	/**
	 * Packages up updates and sets the item state to NEW
	 * @see EmptyInterceptor#onFlushDirty(Object, java.io.Serializable, Object[], Object[], String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		log.debug("Update intercepted");
		if (shouldSynchronize(entity)) {
			log.debug("Packaging: " + SyncUtil.formatObject(entity));
			packageObject((OpenmrsObject) entity, currentState, propertyNames, types, id, SyncItemState.UPDATED);
		}
		else {
			log.debug("Entity configured not to sync: " + SyncUtil.formatObject(entity));
		}
		return false; // This means that we did not modify the passed in entity
	}

	/**
	 * Handles collection remove event. As can be seen in org.hibernate.engine.Collections,
	 * hibernate only calls remove when it is about to recreate a collection.
	 */
	@Override
	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
		if (log.isDebugEnabled()) {
			log.debug("onCollectionRemove key: " + key);
		}
		// TODO: This has never done anything.  We should investigate if we need to process a delete?
		return;
	}

	/**
	 * Handles collection recreate. Recreate is triggered by hibernate when collection object is
	 * replaced by new/different instance.
	 * <p>
	 * remarks: See hibernate AbstractFlushingEventListener and org.hibernate.engine.Collections
	 * implementation to understand how collection updates are hooked up in hibernate, specifically
	 * see Collections.prepareCollectionForUpdate().
	 *
	 * @see org.hibernate.engine.Collections
	 * @see org.hibernate.event.def.AbstractFlushingEventListener
	 */
	@Override
	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
		if (log.isDebugEnabled()) {
			log.debug("onCollectionRecreate key: " + key);
		}
		if (collection instanceof AbstractPersistentCollection) {
			processHibernateCollection((AbstractPersistentCollection) collection, key, "recreate");
		}
		else {
			// TODO: MS - We should look at whether changing this to an exception will cause issues
			log.warn("Unsupported collection type; collection must derive from AbstractPersistentCollection," + " collection type was:" + collection.getClass().getName());
		}
	}

	/**
	 * Handles updates of a collection (i.e. added/removed entries).
	 * <p>
	 * remarks: See hibernate AbstractFlushingEventListener implementation to understand how
	 * collection updates are hooked up in hibernate.
	 *
	 * @see org.hibernate.engine.Collections
	 * @see org.hibernate.event.def.AbstractFlushingEventListener
	 */
	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
		if (log.isDebugEnabled()) {
			log.debug("onCollectionUpdate key: " + key);
		}
		if (collection instanceof AbstractPersistentCollection) {
			processHibernateCollection((AbstractPersistentCollection) collection, key, "update");
		}
		else {
			// TODO: MS - We should look at whether changing this to an exception will cause issues.
			log.warn("Unsupported collection type; collection must derive from AbstractPersistentCollection," + " collection type was:" + collection.getClass().getName());
		}
	}

	/**
	 * Intercept prepared statements for logging purposes only.
	 * <p>
	 * NOTE: At this point, we are ignoring any prepared statements. This method gets called on any
	 * prepared stmt; meaning selects also which makes handling this reliably difficult.
	 * Fundamentally, short of replaying sql as is on parent, it is difficult to imagine safe and
	 * complete implementation.
	 * <p>
	 * Preferred approach is to weed out all dynamic SQL from openMRS DB layer and if absolutely
	 * necessary, create a hook for DB layer code to Explicitly specify what SQL should be passed to
	 * the parent during synchronization.
	 *
	 * @see EmptyInterceptor#onPrepareStatement(String)
	 */
	@Override
	public String onPrepareStatement(String sql) {
		if (log.isTraceEnabled()) {
			log.trace("Prepare Statement: " + sql);
		}
		return sql;
	}

	/**
	 * We register our beforeTransactionCompletion process here to ensure it gets run for this transaction
	 * @see EmptyInterceptor#preFlush(Iterator)
	 */
	@Override
	public void preFlush(Iterator entities) {
		if (log.isDebugEnabled()) {
			log.debug("preFlush intercepted: " + SyncUtil.formatEntities(entities));
		}
		registerBeforeTransactionCompletionProcess();
	}

	/**
	 * No operation, logging only
	 * @see EmptyInterceptor#postFlush(Iterator)
	 */
	@Override
	public void postFlush(Iterator entities) {
		if (log.isDebugEnabled()) {
			log.debug("postFlush intercepted: " + SyncUtil.formatEntities(entities));
		}
	}

	/**
	 * No operation, logging only. The processing of sync records happens in a BeforeTransactionCompletionProcess
	 * (see below), due to the fact that any exceptions thrown by beforeTransactionCompletion are swallowed, so the
	 * user would have no idea that their saving operation failed to save sync records.  In contrast,
	 * beforeTransactionCompletionProcesses are run outside of that try/catch block and result in exceptions
	 * bubbling back up
	 * @see org.hibernate.impl.SessionImpl#beforeTransactionCompletion(org.hibernate.Transaction)
	 * @see EmptyInterceptor#beforeTransactionCompletion(org.hibernate.Transaction)
	 */
	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		if (log.isDebugEnabled()) {
			log.debug("About to Complete Transaction: " + SyncUtil.formatTransactionStatus(tx));
		}
	}

	/**
	 * Registers a {@link BeforeTransactionCompletionProcess} if none has been registered with the
	 * current session, it uses ThreadLocal variable to check if it is already set since sessions
	 * are thread bound
	 */
	private void registerBeforeTransactionCompletionProcess() {
		log.debug("Registering SyncBeforeTransactionCompletionProcess with the current session");

		EventSource eventSource = (EventSource) getSessionFactory().getCurrentSession();
		eventSource.getActionQueue().registerProcess(new BeforeTransactionCompletionProcess() {
			 public void doBeforeTransactionCompletion(SessionImplementor sessionImpl) {
				 log.trace("doBeforeTransactionCompletion process: checking for SyncRecord to save");
				 try {
					 SyncRecord record = getSyncRecord();
					 syncRecordHolder.remove();

					 // Does this transaction contain any serialized changes?
					 if (record != null && record.hasItems()) {

						 // Grab user if we have one, and use the UUID of the user as creator of this SyncRecord
						 User user = Context.getAuthenticatedUser();
						 if (user != null) {
							 record.setCreator(user.getUuid());
						 }

						 // Grab database version
						 record.setDatabaseVersion(OpenmrsConstants.OPENMRS_VERSION_SHORT);

						 // Complete the record
						 record.setUuid(SyncUtil.generateUuid());

						 if (record.getOriginalUuid() == null) {
							 log.debug("OriginalUuid is null, so assigning a new UUID: " + record.getUuid());
							 record.setOriginalUuid(record.getUuid());
						 }
						 else {
							 log.debug("OriginalUuid is: " + record.getOriginalUuid());
						 }

						 record.setState(SyncRecordState.NEW);
						 record.setTimestamp(new Date());
						 record.setRetryCount(0);

						 log.info("Saving SyncRecord " + record.getOriginalUuid() + ": " + record.getItems().size() + " items");

						 // Save SyncRecord
						 getSyncService().createSyncRecord(record, record.getOriginalUuid());
					 }
					 else {
						 // note: this will happen all the time with read-only transactions
						 if (log.isTraceEnabled()) {
							 log.trace("No SyncItems in SyncRecord, save discarded (note: maybe a read-only transaction)!");
						 }
					 }
				 }
				 catch (Exception e) {
					 log.error("A error occurred while trying to save a sync record in the interceptor", e);
					 throw new SyncException("Error in interceptor, see log messages and callstack.", e);
				 }
			 }
		});
		log.debug("Successfully registered SyncBeforeTransactionCompletionProcess with the current session");
	}

	/**
	 * No operation, logging only
	 * @see EmptyInterceptor#afterTransactionCompletion(Transaction)
	 */
	@Override
	public void afterTransactionCompletion(Transaction tx) {
		if (log.isDebugEnabled()) {
			log.debug("Transaction Completed: " + SyncUtil.formatTransactionStatus(tx));
		}
		// Because the beforeTransactionCompletion method is not called on rollback, we need to ensure any syncRecords still on the thread are removed after the tx is completed
		syncRecordHolder.remove();
	}

	/**
	 * Serializes and packages an intercepted change in object state.
	 * <p>
	 * IMPORTANT serialization notes:
	 * <p>
	 * Transient Properties. Transients are not serialized/journalled. Marking an object property as
	 * transient is the supported way of designating it as something not to be recorded into the
	 * journal.
	 * <p/>
	 * Hibernate Identity property. A property designated in Hibernate as identity (i.e. primary
	 * key) *is* not serialized. This is because sync does not enforce global uniqueness of database
	 * primary keys. Instead, custom uuid property is used. This allows us to continue to use native
	 * types for 'traditional' entity relationships.
	 *
	 * @param entity The object changed.
	 * @param currentState Array containing data for each field in the object as they will be saved.
	 * @param propertyNames Array containing name for each field in the object, corresponding to
	 *            currentState.
	 * @param types Array containing Type of the field in the object, corresponding to currentState.
	 * @param state SyncItemState, e.g. NEW, UPDATED, DELETED
	 * @param id Value of the identifier for this entity
	 */
	protected void packageObject(OpenmrsObject entity, Object[] currentState, String[] propertyNames, Type[] types,
	                             Serializable id, SyncItemState state) throws SyncException {

		String objectUuid = null;
		String originalRecordUuid = null;
		Set<String> transientProps = null;
		String infoMsg = null;

		ClassMetadata data = null;
		String idPropertyName = null;
		org.hibernate.tuple.IdentifierProperty idPropertyObj = null;

		// The container of values to be serialized:
		// Holds tuples of <property-name> -> {<property-type-name>,
		// <property-value as string>}
		HashMap<String, PropertyClassValue> values = new HashMap<String, PropertyClassValue>();

		try {
			objectUuid = entity.getUuid();

			// pull-out sync-network wide change id for the sync *record* (not the entity itself),
			// if one was already assigned (i.e. this change is coming from some other server)
			originalRecordUuid = getSyncRecord().getOriginalUuid();

			if (log.isDebugEnabled()) {
				// build up a starting msg for all logging:
				StringBuilder sb = new StringBuilder();
				sb.append("In PackageObject, entity type:");
				sb.append(entity.getClass().getName());
				sb.append(", entity uuid:");
				sb.append(objectUuid);
				sb.append(", originalUuid uuid:");
				sb.append(originalRecordUuid);
				log.debug(sb.toString());
			}

			// Transient properties are not serialized.
			transientProps = new HashSet<String>();
			for (Field f : entity.getClass().getDeclaredFields()) {
				if (Modifier.isTransient(f.getModifiers())) {
					transientProps.add(f.getName());
					if (log.isDebugEnabled())
						log.debug("The field " + f.getName() + " is transient - so we won't serialize it");
				}
			}

			/*
			 * Retrieve metadata for this type; we need to determine what is the
			 * PK field for this type. We need to know this since PK values are
			 * *not* journalled; values of primary keys are assigned where
			 * physical DB records are created. This is so to avoid issues with
			 * id collisions.
			 *
			 * In case of <generator class="assigned" />, the Identifier
			 * property is already assigned value and needs to be journalled.
			 * Also, the prop will *not* be part of currentState,thus we need to
			 * pull it out with reflection/metadata.
			 */
			data = getSessionFactory().getClassMetadata(entity.getClass());
			if (data.hasIdentifierProperty()) {
				idPropertyName = data.getIdentifierPropertyName();
				idPropertyObj = ((org.hibernate.persister.entity.AbstractEntityPersister) data).getEntityMetamodel()
				        .getIdentifierProperty();

				if (id != null && idPropertyObj.getIdentifierGenerator() != null
				        && (idPropertyObj.getIdentifierGenerator() instanceof org.hibernate.id.Assigned
				        //	|| idPropertyObj.getIdentifierGenerator() instanceof org.openmrs.api.db.hibernate.NativeIfNotAssignedIdentityGenerator
				        )) {
					// serialize value as string
					values.put(idPropertyName, new PropertyClassValue(id.getClass().getName(), id.toString()));
				}
			} else if (data.getIdentifierType() instanceof EmbeddedComponentType) {
				// if we have a component identifier type (like AlertRecipient),
				// make
				// sure we include those properties
				EmbeddedComponentType type = (EmbeddedComponentType) data.getIdentifierType();
				for (int i = 0; i < type.getPropertyNames().length; i++) {
					String propertyName = type.getPropertyNames()[i];
					Object propertyValue = type.getPropertyValue(entity, i, org.hibernate.EntityMode.POJO);
					addProperty(values, entity, type.getSubtypes()[i], propertyName, propertyValue, infoMsg);
				}
			}

			/*
			 * Loop through all the properties/values and put in a hash for
			 * duplicate removal
			 */
			for (int i = 0; i < types.length; i++) {
				String typeName = types[i].getName();
				if (log.isDebugEnabled())
					log.debug("Processing, type: " + typeName + " Field: " + propertyNames[i]);

				if (propertyNames[i].equals(idPropertyName) && log.isInfoEnabled())
					log.debug(infoMsg + ", Id for this class: " + idPropertyName + " , value:" + currentState[i]);

				if (currentState[i] != null) {
					// is this the primary key or transient? if so, we don't
					// want to serialize
					if (propertyNames[i].equals(idPropertyName)
					        || ("personId".equals(idPropertyName) && "patientId".equals(propertyNames[i]))
					        //|| ("personId".equals(idPropertyName) && "userId".equals(propertyNames[i]))
					        || transientProps.contains(propertyNames[i])) {
						// if (log.isInfoEnabled())
						log.debug("Skipping property (" + propertyNames[i]
						        + ") because it's either the primary key or it's transient.");

					} else {

						addProperty(values, entity, types[i], propertyNames[i], currentState[i], infoMsg);
					}
				} else {
					// current state null -- skip
					if (log.isDebugEnabled())
						log.debug("Field Type: " + typeName + " Field Name: " + propertyNames[i] + " is null, skipped");
				}
			}

			/*
			 * Now serialize the data identified and put in the value-map
			 */
			// Setup the serialization data structures to hold the state
			Package pkg = new Package();
			String className = entity.getClass().getName();
			Record xml = pkg.createRecordForWrite(className);
			Item entityItem = xml.getRootItem();

			// loop through the map of the properties that need to be serialized
			for (Map.Entry<String, PropertyClassValue> me : values.entrySet()) {
				String property = me.getKey();

				// if we are processing onDelete event all we need is uuid
				if ((state == SyncItemState.DELETED) && (!"uuid".equals(property))) {
					continue;
				}

				try {
					PropertyClassValue pcv = me.getValue();
					appendRecord(xml, entity, entityItem, property, pcv.getClazz(), pcv.getValue());
				}
				catch (Exception e) {
					String msg = "Could not append attribute. Error while processing property: " + property + " - "
					        + e.getMessage();
					throw (new SyncException(msg, e));
				}
			}

			values.clear(); // Be nice to GC

			if (objectUuid == null)
				throw new SyncException("uuid is null for: " + className + " with id: " + id);

			/*
			 * Create SyncItem and store change in SyncRecord kept in
			 * ThreadLocal.
			 */
			SyncItem syncItem = new SyncItem();
			syncItem.setKey(new SyncItemKey<String>(objectUuid, String.class));
			syncItem.setState(state);
			syncItem.setContent(xml.toStringAsDocumentFragement());
			syncItem.setContainedType(entity.getClass());

			if (log.isDebugEnabled())
				log.debug("Adding SyncItem to SyncRecord");

			getSyncRecord().addItem(syncItem);
			getSyncRecord().addContainedClass(entity.getClass().getName());

			// set the originating uuid for the record: do this once per Tx;
			// else we may end up with empty string
			if (getSyncRecord().getOriginalUuid() == null || "".equals(getSyncRecord().getOriginalUuid())) {
				getSyncRecord().setOriginalUuid(originalRecordUuid);
			}
		}
		catch (SyncException ex) {
			log.error("Journal error\n", ex);
			throw (ex);
		}
		catch (Exception e) {
			log.error("Journal error\n", e);
			throw (new SyncException("Error in interceptor, see log messages and callstack.", e));
		}

		return;
	}

	/**
	 * Convenience method to add a property to the given list of values to turn into xml
	 *
	 * @param values
	 * @param entity
	 * @param propertyType
	 * @param propertyName
	 * @param propertyValue
	 * @param infoMsg
	 * @throws Exception
	 */
	private void addProperty(HashMap<String, PropertyClassValue> values, OpenmrsObject entity, Type propertyType,
	                         String propertyName, Object propertyValue, String infoMsg) throws Exception {
		Normalizer n;
		String propertyTypeName = propertyType.getName();
		if ((n = SyncUtil.getNormalizer(propertyTypeName)) != null) {
			// Handle safe types like
			// boolean/String/integer/timestamp via Normalizers
			values.put(propertyName, new PropertyClassValue(propertyTypeName, n.toString(propertyValue)));
		} else if ((n = SyncUtil.getNormalizer(propertyValue.getClass())) != null) {
			values.put(propertyName, new PropertyClassValue(propertyValue.getClass().getName(), n.toString(propertyValue)));
		} else if (propertyType.isCollectionType() && (n = isCollectionOfSafeTypes(entity, propertyName)) != null) {
			// if the property is a list/set/collection AND the members of that
			// collection are a "safe type",
			// then we put the values into the xml
			values.put(propertyName, new PropertyClassValue(propertyTypeName, n.toString(propertyValue)));
		}

		/*
		 * Not a safe type, check if the object implements the OpenmrsObject interface
		 */
		else if (propertyValue instanceof OpenmrsObject) {
			OpenmrsObject childObject = (OpenmrsObject) propertyValue;
			String childUuid = fetchUuid(childObject);
			if (childUuid != null) {
				values.put(propertyName, new PropertyClassValue(propertyTypeName, childUuid));
			}
			else {
				String msg = infoMsg + ", Field value should be synchronized, but uuid is null.  Field Type: " + propertyType + " Field Name: " + propertyName;
				log.error(msg + ".  Turn on debug logging for more details.");
				throw (new SyncException(msg));
			}
		}
		else {
			// state != null but it is not safetype or
			// implements OpenmrsObject: do not package and log
			// as info
			if (log.isDebugEnabled())
				log.debug(infoMsg + ", Field Type: " + propertyType + " Field Name: " + propertyName
				        + " is not safe or OpenmrsObject, skipped!");
		}

	}

	/**
	 * Checks the collection to see if it is a collection of supported types. If so, then it returns
	 * appropriate normalizer. Note, this handles maps too.
	 *
	 * @param object
	 * @param propertyName
	 * @return a Normalizer for the given type or null if not a safe type
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	private Normalizer isCollectionOfSafeTypes(OpenmrsObject object, String propertyName) throws SecurityException,
	                                                                                     NoSuchFieldException {
		try {

			java.lang.reflect.ParameterizedType collectionType = ((java.lang.reflect.ParameterizedType) object.getClass()
			        .getDeclaredField(propertyName).getGenericType());
			if (Map.class.isAssignableFrom((Class) collectionType.getRawType())) {
				//this is a map; Map<K,V>: verify that K and V are of types we know how to process
				java.lang.reflect.Type keyType = collectionType.getActualTypeArguments()[0];
				java.lang.reflect.Type valueType = collectionType.getActualTypeArguments()[1];
				Normalizer keyNormalizer = SyncUtil.getNormalizer((Class) keyType);
				Normalizer valueNormalizer = SyncUtil.getNormalizer((Class) valueType);
				if (keyNormalizer != null && valueNormalizer != null) {
					return SyncUtil.getNormalizer((Class) collectionType.getRawType());
				} else {
					return null;
				}
			} else {
				//this is some other collection, so just get a normalizer for its
				return SyncUtil.getNormalizer((Class) (collectionType.getActualTypeArguments()[0]));
			}

		}
		catch (Throwable t) {
			// might get here if the property is on a superclass to the object

			log.trace("Unable to get collection field: " + propertyName + " from object " + object.getClass()
			        + " for some reason", t);
		}

		// on errors just return null
		return null;
	}

	/**
	 * Adds a property value to the existing serialization record as a string.
	 * <p>
	 * If data is null it will be skipped, no empty serialization items are written. In case of xml
	 * serialization, the data will be serialized as: &lt;property
	 * type='classname'&gt;data&lt;/property&gt;
	 *
	 * @param xml record node to append to
	 * @param entity the object holding the given property
	 * @param parent the pointer to the root parent node
	 * @param property new item name (in case of xml serialization this will be child element name)
	 * @param classname type of the property, will be recorded as attribute named 'type' on the
	 *            child item
	 * @param data String content, in case of xml serialized as text node (i.e. not CDATA)
	 * @throws Exception
	 */
	protected void appendRecord(Record xml, OpenmrsObject entity, Item parent, String property, String classname, String data)
	                                                                                                                          throws Exception {
		// if (data != null && data.length() > 0) {
		// this will break if we don't allow data.length==0 - some string values
		// are required NOT NULL, but can be blank
		if (data != null) {
			Item item = xml.createItem(parent, property);
			item.setAttribute("type", classname);
			data = transformItemForSyncRecord(item, entity, property, data);
			xml.createText(item, data);
		}
	}

	/**
	 * Called while saving a SyncRecord to allow for manipulating what is stored. The impl of this
	 * method transforms the {@link PersonAttribute#getValue()} and {@link Obs#getVoidReason()}
	 * methods to not reference primary keys. (Instead the uuid is referenced and then dereferenced
	 * before being saved). If no transformation is to take place, the data is returned as given.
	 *
	 * @param item the serialized sync item associated with this record
	 * @param entity the OpenmrsObject containing the property
	 * @param property the property name
	 * @param data the current value for the
	 * @return the transformed (or unchanged) data to save in the SyncRecord
	 */
	public String transformItemForSyncRecord(Item item, OpenmrsObject entity, String property, String data) {
		// data will not be null here, so NPE checks are not needed

		if (entity instanceof PersonAttribute && "value".equals(property)) {
			PersonAttribute attr = (PersonAttribute) entity;
			// use PersonAttributeType.format to get the uuid
			if (attr.getAttributeType() == null)
				throw new SyncException("Unable to find person attr type on attr with uuid: " + entity.getUuid());
			String className = attr.getAttributeType().getFormat();
			try {
				Class c = Context.loadClass(className);
				item.setAttribute("type", className);

				// An empty string represents an empty value. Return it as the UUID does not exist.
				if ((data.trim()).isEmpty())
					return data;

				// only convert to uuid if this is an OpenMrs object
				// otherwise, we are just storing a simple String or Integer
				// value
				if (OpenmrsObject.class.isAssignableFrom(c)) {
					String valueObjectUuid = fetchUuid(c, Integer.valueOf(data));
					return valueObjectUuid;
				}
			}
			catch (Throwable t) {
				log.warn("Unable to get class of type: " + className + " for sync'ing attribute.value column", t);
			}
		} else if (entity instanceof PersonAttributeType && "foreignKey".equals(property)) {
			if (StringUtils.hasLength(data)) {
				PersonAttributeType attrType = (PersonAttributeType) entity;
				String className = attrType.getFormat();
				try {
					Class c = Context.loadClass(className);
					String foreignKeyObjectUuid = fetchUuid(c, Integer.valueOf(data));

					// set the class name on this to be the uuid-ized type
					// instead of java.lang.Integer.
					// the SyncUtil.valForField method will handle changing this
					// back to an integer
					item.setAttribute("type", className);
					return foreignKeyObjectUuid;
				}
				catch (Throwable t) {
					log.warn("Unable to get class of type: " + className + " for sync'ing foreignKey column", t);
				}
			}
		} else if (entity instanceof Obs && "voidReason".equals(property)) {
			if (data.contains("(new obsId: ")) {
				// rip out the obs id and replace it with a uuid
				String voidReason = String.copyValueOf(data.toCharArray()); // copy
				// the
				// string
				// so
				// that
				// we're
				// operating
				// on
				// a
				// new
				// object
				int start = voidReason.lastIndexOf(" ") + 1;
				int end = voidReason.length() - 1;
				String obsId = voidReason.substring(start, end);
				try {
					String newObsUuid = fetchUuid(Obs.class, Integer.valueOf(obsId));
					return data.substring(0, data.lastIndexOf(" ")) + " " + newObsUuid + ")";
				}
				catch (Exception e) {
					log.trace("unable to get uuid from obs pk: " + obsId, e);
				}
			}
		} else if (entity instanceof Cohort && "memberIds".equals(property)) {
			// convert integer patient ids to uuids
			try {
				item.setAttribute("type", "java.util.Set<org.openmrs.Patient>");
				StringBuilder sb = new StringBuilder();

				data = data.replaceFirst("\\[", "").replaceFirst("\\]", "");

				sb.append("[");
				String[] fieldVals = data.split(",");
				for (int x = 0; x < fieldVals.length; x++) {
					if (x >= 1)
						sb.append(", ");

					String eachFieldVal = fieldVals[x].trim(); // take out whitespace
					String uuid = fetchUuid(Patient.class, Integer.valueOf(eachFieldVal));
					sb.append(uuid);

				}

				sb.append("]");

				return sb.toString();

			}
			catch (Throwable t) {
				log.warn("Unable to get Patient for sync'ing cohort.memberIds property", t);
			}

		}

		return data;
	}

	/**
	 * Determines if entity is to be 'synchronized', eg. implements OpenmrsObject interface.
	 *
	 * @param entity Object to examine.
	 * @return true if entity should be synchronized, else false.
	 */
	protected boolean shouldSynchronize(Object entity) {

		Boolean ret = true;

		// check if this object is to be sync-ed: compare against the configured classes
		// for time being, suspend any flushing -- we are in the middle of hibernate stack
		org.hibernate.FlushMode flushMode = getSessionFactory().getCurrentSession().getFlushMode();
		getSessionFactory().getCurrentSession().setFlushMode(org.hibernate.FlushMode.MANUAL);

		try {
			ret = getSyncService().shouldSynchronize(entity);
		}
		catch (Exception ex) {
			log.warn("Journal error\n", ex);
			//log error info as warning but continue on
		}
		finally {
			if (getSessionFactory() != null) {
				getSessionFactory().getCurrentSession().setFlushMode(flushMode);
			}
		}

		return ret;
	}

	/**
	 * Retrieves uuid of OpenmrsObject instance from the storage based on identity value (i.e. PK).
	 * <p>
	 * Remarks: It is important for the implementation to avoid loading obj into session while
	 * trying to determine its uuid. As a result, the implementation uses the combination of
	 * reflection to determine the object's identifier value and Hibernate criteria in order to
	 * build select statement for getting the uuid. The reason to avoid fetching the obj is because
	 * doing it causes an error in hibernate when processing disconnected proxies. Specifically,
	 * during obs edit, several properties are are disconnected as the form controller uses Criteria
	 * object to construct select queury session.clear() and then session.merge(). Finally,
	 * implementation suspends any state flushing to avoid any weird auto-flush events being
	 * triggered while select is being executed.
	 *
	 * @param obj Instance of OpenmrsObject for which to retrieve uuid for.
	 * @return uuid from storage if obj identity value is set, else null.
	 * @see ForeignKeys
	 */
	protected String fetchUuid(OpenmrsObject obj) {

		if (obj == null) {
			return null;
		}

		if (log.isDebugEnabled()) {
			log.debug("Attempting to fetch uuid for from OpenmrsObject");
		}

		try {
			return obj.getUuid();
		}
		catch (Exception e) {
			log.debug("Unable to get uuid from OpenmrsObject directly", e);
		}

		try {
			if (obj instanceof HibernateProxy) {
				log.debug("Attempting to retrieve via the Hibernate Proxy class and identifier");
				HibernateProxy proxy = (HibernateProxy) obj;
				Class persistentClass = proxy.getHibernateLazyInitializer().getPersistentClass();
				Object identifier = proxy.getHibernateLazyInitializer().getIdentifier();
				String uuid = fetchUuid(persistentClass, identifier);
				log.debug("Successfully retrieved uuid " + uuid);
				return uuid;
			}
		}
		catch (Exception e) {
			log.debug("Unable to fetch uuid from Hibernate Proxy: ", e);
		}

		try {
			log.debug("Attempting to load from the database given class and id");
			String uuid = fetchUuid(obj.getClass(), obj.getId());
			log.debug("Successfully retrieved uuid " + uuid);
			return uuid;
		}
		catch (Exception e) {
			log.debug("Unable to fetch uuid from class and id", e);
		}

		try {
			log.debug("Attempting to load from the database given class only, using hibernate mapping to determine identifier");
			ClassMetadata data = getSessionFactory().getClassMetadata(obj.getClass());
			if (data != null) {
				String idPropertyName = data.getIdentifierPropertyName();
				if (idPropertyName != null) {
					Method m = SyncUtil.getGetterMethod(obj.getClass(), idPropertyName);
					if (m != null) {
						Object idPropertyValue = m.invoke(obj);
						String uuid = fetchUuid(obj.getClass(), idPropertyValue);
						log.debug("Successfully retrieved uuid " + uuid);
						return uuid;
					}
				}
			}
		}
		catch (Exception e) {
			log.debug("Unable to fetch uuid from reflection via hibernate metadata", e);
		}

		log.warn("*** All attempts failed to fetch the uuid for an OpenmrsObject ***");

		return null;
	}

	/**
	 * See {@link #fetchUuid(OpenmrsObject)}
	 *
	 * @param objTrueType
	 * @param idPropertyValue
	 * @return
	 */
	protected String fetchUuid(Class objTrueType, Object idPropertyValue) {
		String uuid = null;

		// for time being, suspend any flushing
		org.hibernate.FlushMode flushMode = getSessionFactory().getCurrentSession().getFlushMode();
		getSessionFactory().getCurrentSession().setFlushMode(org.hibernate.FlushMode.MANUAL);

		try {
			// try to fetch the instance and get its uuid
			if (idPropertyValue != null) {
				// build sql to fetch uuid - avoid loading obj into session
				org.hibernate.Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(objTrueType);
				criteria.add(Expression.idEq(idPropertyValue));
				criteria.setProjection(Projections.property("uuid"));
				uuid = (String) criteria.uniqueResult();

				if (uuid == null)
					log.warn("Unable to find obj of type: " + objTrueType + " with primary key: " + idPropertyValue);

				return uuid;
			}
		}
		finally {
			if (getSessionFactory() != null) {
				getSessionFactory().getCurrentSession().setFlushMode(flushMode);
			}
		}

		return null;
	}

	/**
	 * Processes changes to hibernate collections. At the moment, only persistent sets are
	 * supported.
	 * <p>
	 * Remarks: Note that simple lists and maps of primitive types are supported also by default via
	 * normalizers and do not require explicit handling as shown here for sets of any reference
	 * types.
	 * <p>
	 *
	 * @param collection Instance of Hibernate AbstractPersistentCollection to process.
	 * @param key key of owner for the collection.
	 * @param action hibernate 'action' being performed: update, recreate. note, deletes are handled
	 *            via re-create
	 */
	protected void processHibernateCollection(AbstractPersistentCollection collection, Serializable key, String action) {

		if (!(collection instanceof PersistentSet || collection instanceof PersistentMap || collection instanceof PersistentList)) {
			log.debug("Unsupported collection type, collection type was:" + collection.getClass().getName());
			return;
		}


		OpenmrsObject owner = null;
		String originalRecordUuid = null;
		LinkedHashMap<String, OpenmrsObject> entriesHolder = null;

		// we only process recreate and update
		if (!"update".equals(action) && !"recreate".equals(action)) {
			log.error("Unexpected 'action' supplied, valid values: recreate, update. value provided: " + action);
			throw new CallbackException("Unexpected 'action' supplied while processing a persistent set.");
		}

		// retrieve owner and original uuid if there is one
		if (collection.getOwner() instanceof OpenmrsObject) {
			owner = (OpenmrsObject) collection.getOwner();

			if (!this.shouldSynchronize(owner)) {
				if (log.isDebugEnabled())
					log.debug("Determined entity not to be journaled, exiting onDelete.");
				return;
			}

			originalRecordUuid = getSyncRecord().getOriginalUuid();

		} else {
			log.debug("Cannot process collection where owner is not OpenmrsObject.");
			return;
		}

		/*
		 * determine if this set needs to be processed. Process if: 1. it is
		 * recreate or 2. is dirty && current state does not equal stored
		 * snapshot
		 */
		boolean process = false;
		if ("recreate".equals(action)) {
			process = true;
		} else {
			if (collection.isDirty()) {
				org.hibernate.persister.collection.CollectionPersister persister = ((org.hibernate.engine.SessionFactoryImplementor) getSessionFactory())
				        .getCollectionPersister(collection.getRole());
				Object ss = null;
				try { // code around hibernate bug:
					  // http://opensource.atlassian.com/projects/hibernate/browse/HHH-2937
					ss = collection.getSnapshot(persister);
				}
				catch (NullPointerException ex) {}
				if (ss == null) {
					log.debug("snapshot is null");
					if (collection.empty())
						process = false;
					else
						process = true;
				} else if (!collection.equalsSnapshot(persister)) {
					process = true;
				}
				;
			}

			if (!process) {
				log.debug("set processing, no update needed: not dirty or current state and snapshots are same");
			}
		}
		if (!process)
			return;

		// pull out the property name on owner that corresponds to the collection
		ClassMetadata data = getSessionFactory().getClassMetadata(owner.getClass());
		String[] propNames = data.getPropertyNames();
		// this is the name of the property on owner object that contains the set
		String ownerPropertyName = null;

		for (String propName : propNames) {
			Object propertyVal = data.getPropertyValue(owner, propName, org.hibernate.EntityMode.POJO);
			// note: test both with equals() and == because
			// PersistentSet.equals()
			// actually does not handle equality of two persistent sets well
			if (collection == propertyVal || collection.equals(propertyVal)) {
				ownerPropertyName = propName;
				break;
			}
		}
		if (ownerPropertyName == null) {
			log.error("Could not find the property on owner object that corresponds to the collection being processed.");
			log.error("owner info: \ntype: " + owner.getClass().getName() + ", \nuuid: " + owner.getUuid()
			        + ",\n property name for collection: " + ownerPropertyName);
			throw new CallbackException(
			        "Could not find the property on owner object that corresponds to the collection being processed.");
		}

		//now we know this needs to be processed. Proceed accordingly:
		if (collection instanceof PersistentSet || collection instanceof PersistentList
		        || collection instanceof PersistentMap) {
			processPersistentCollection(collection, key, action, originalRecordUuid, owner, ownerPropertyName);
		}

		return;
	}

	/**
	 * Processes changes to persistent collection that contains instances of OpenmrsObject objects.
	 * <p>
	 * Remarks:
	 * <p>
	 * Xml 'schema' for the sync item content for the persisted collection follows. Note that for persisted
	 * collections syncItemKey is a composite of owner object uuid and the property name that contains the
	 * collection. <br/>
	 * &lt;persistent-collection&gt; element: wrapper element <br/>
	 * &lt;owner uuid='' propertyName='' type='' action='recreate|update' &gt; element: this
	 * captures the information about the object that holds reference to the collection being
	 * processed <br/>
	 * -uuid: owner object uuid <br/>
	 * -properyName: names of the property on owner object that holds this collection <br/>
	 * -type: owner class name <br/>
	 * -action: recreate, update -- these are collection events defined by hibernate interceptor <br/>
	 * &lt;entry action='update|delete' uuid='' type='' &gt; element: this captures info about
	 * individual collection entries: <br/>
	 * -action: what is being done to this item of the collection: delete (item was removed from the
	 * collection) or update (item was added to the collection) <br/>
	 * -uuid: entry's uuid <br/>
	 * -type: class name
	 *
	 * @param collection Instance of Hibernate AbstractPersistentCollection to process.
	 * @param key key of owner for the collection.
	 * @param action action being performed on the collection: update, recreate
	 */
	private void processPersistentCollection(AbstractPersistentCollection collection, Serializable key, String action, String originalRecordUuid,
	                                  OpenmrsObject owner, String ownerPropertyName) {

		LinkedHashMap<String, OpenmrsObject> entriesHolder = null;

		// Setup the serialization data structures to hold the state
		Package pkg = new Package();
		entriesHolder = new LinkedHashMap<String, OpenmrsObject>();
		try {

			CollectionMetadata collMD = getCollectionMetadata(owner.getClass(), ownerPropertyName, getSessionFactory());
			if (collMD == null) {
				throw new SyncException("Can't find a collection with " + ownerPropertyName + " in class "
				        + owner.getClass());
			}

			Class<?> elementClass = collMD.getElementType().getReturnedClass();
			//If this is a simple type like Integer, serialization of the collection will be as below:
			//<org.openmrs.Cohort>
			//	<memberIds type="java.util.Set(org.openmrs.Cohort)">[2, 3]</memberIds>
			//  ............. and more
			//This should work just fine as long as there is a Normalizer registered for it
			if (!OpenmrsObject.class.isAssignableFrom(elementClass) && SyncUtil.getNormalizer(elementClass) != null) {

				//Check if there is already a NEW/UPDATE sync item for the owner
				SyncItem syncItem = new SyncItem();
				syncItem.setKey(new SyncItemKey<String>(owner.getUuid(), String.class));
				syncItem.setContainedType(owner.getClass());
				syncItem.setState(SyncItemState.UPDATED);

				boolean ownerHasSyncItem = getSyncRecord().hasSyncItem(syncItem);
				syncItem.setState(SyncItemState.NEW);
				if (!ownerHasSyncItem)
					ownerHasSyncItem = getSyncRecord().hasSyncItem(syncItem);

				if (!ownerHasSyncItem) {
					ClassMetadata cmd = getSessionFactory().getClassMetadata(owner.getClass());
					//create an UPDATE sync item for the owner so that the collection changes get recorded along
					Serializable primaryKeyValue = cmd.getIdentifier(owner, (SessionImplementor)getSessionFactory().getCurrentSession());
					packageObject(owner, cmd.getPropertyValues(owner, EntityMode.POJO), cmd.getPropertyNames(),
					    cmd.getPropertyTypes(), primaryKeyValue, SyncItemState.UPDATED);
				} else {
					//There is already an UPDATE OR NEW SyncItem for the owner containing the above updates
				}

				return;
			}
			// find out what entries need to be serialized
			for (Object entry : (Iterable)collection) {
				if (entry instanceof OpenmrsObject) {
					OpenmrsObject obj = (OpenmrsObject) entry;

					// attempt to retrieve entry uuid
					String entryUuid = obj.getUuid();
					if (entryUuid == null) {
						entryUuid = fetchUuid(obj);
						if (log.isDebugEnabled()) {
							log.debug("Entry uuid was null, attempted to fetch uuid with the following results");
							log.debug("Entry type:" + obj.getClass().getName() + ",uuid:" + entryUuid);
						}
					}
					// well, this is messed up: have an instance of
					// OpenmrsObject but has no uuid
					if (entryUuid == null) {
						log.error("Cannot handle collection entries where uuid is null.");
						throw new CallbackException("Cannot handle collection entries where uuid is null.");
					}

					// add it to the holder to avoid possible duplicates: key =
					// uuid + action
					entriesHolder.put(entryUuid + "|update", obj);
				} else {
					log.warn("Cannot handle collections where entries are not OpenmrsObject and have no Normalizers. Type was "
					        + entry.getClass() + " in property " + ownerPropertyName + " in class " + owner.getClass());
					// skip out early because we don't want to write any xml for it
					// it was handled by the normal property writer hopefully
					return;
				}
			}

			// add on deletes
			if (!"recreate".equals(action) && collection.getRole() != null) {
				org.hibernate.persister.collection.CollectionPersister persister = ((org.hibernate.engine.SessionFactoryImplementor) getSessionFactory())
				        .getCollectionPersister(collection.getRole());
				Iterator it = collection.getDeletes(persister, false);
				if (it != null) {
					while (it.hasNext()) {
						Object entryDelete = it.next();
						if (entryDelete instanceof OpenmrsObject) {
							OpenmrsObject objDelete = (OpenmrsObject) entryDelete;
							// attempt to retrieve entry uuid
							String entryDeleteUuid = objDelete.getUuid();
							if (entryDeleteUuid == null) {
								entryDeleteUuid = fetchUuid(objDelete);
								if (log.isDebugEnabled()) {
									log.debug("Entry uuid was null, attempted to fetch uuid with the following results");
									log.debug("Entry type:" + entryDeleteUuid.getClass().getName() + ",uuid:"
									        + entryDeleteUuid);
								}
							}
							// well, this is messed up: have an instance of
							// OpenmrsObject but has no uuid
							if (entryDeleteUuid == null) {
								log.error("Cannot handle collection delete entries where uuid is null.");
								throw new CallbackException("Cannot handle collection delete entries where uuid is null.");
							}

							// add it to the holder to avoid possible
							// duplicates: key = uuid + action
                            // also, only add if there is no update action for the same object: see SYNC-280
                            if (!entriesHolder.containsKey(entryDeleteUuid + "|update")) {
							    entriesHolder.put(entryDeleteUuid + "|delete", objDelete);
                            }

						} else {
							// TODO: more debug info
							log.warn("Cannot handle collections where entries are not OpenmrsObject and have no Normalizers!");
							// skip out early because we don't want to write any
							// xml for it. it
							// was handled by the normal property writer
							// hopefully
							return;
						}
					}
				}
			}

			/*
			 * Create SyncItem and store change in SyncRecord kept in
			 * ThreadLocal. note: when making SyncItemKey, make it a composite
			 * string of uuid + prop. name to avoid collisions with updates to
			 * parent object or updates to more than one collection on same
			 * owner
			 */

			// Setup the serialization data structures to hold the state
			Record xml = pkg.createRecordForWrite(collection.getClass().getName());
			Item entityItem = xml.getRootItem();

			// serialize owner info: we will need type, prop name where collection
			// goes, and owner uuid
			Item item = xml.createItem(entityItem, "owner");
			item.setAttribute("type", this.getType(owner));
			item.setAttribute("properyName", ownerPropertyName);
			item.setAttribute("action", action);
			item.setAttribute("uuid", owner.getUuid());

			// build out the xml for the item content
			Boolean hasNoAutomaticPrimaryKey = null;
			String type = null;
			for (String entryKey : entriesHolder.keySet()) {
				OpenmrsObject entryObject = entriesHolder.get(entryKey);
				if (type == null) {
					type = this.getType(entryObject);
					hasNoAutomaticPrimaryKey = SyncUtil.hasNoAutomaticPrimaryKey(type);
				}

				Item temp = xml.createItem(entityItem, "entry");
				temp.setAttribute("type", type);
				temp.setAttribute("action", entryKey.substring(entryKey.indexOf('|') + 1));
				temp.setAttribute("uuid", entryObject.getUuid());
				if (hasNoAutomaticPrimaryKey) {
					temp.setAttribute("primaryKey", getSyncService().getPrimaryKey(entryObject));
				}
			}

			SyncItem syncItem = new SyncItem();
			syncItem.setKey(new SyncItemKey<String>(owner.getUuid() + "|" + ownerPropertyName, String.class));
			syncItem.setState(SyncItemState.UPDATED);
			syncItem.setContainedType(collection.getClass());
			syncItem.setContent(xml.toStringAsDocumentFragement());

			getSyncRecord().addOrRemoveAndAddItem(syncItem);
			getSyncRecord().addContainedClass(owner.getClass().getName());

			// do the original uuid dance, same as in packageObject
			if (getSyncRecord().getOriginalUuid() == null || "".equals(getSyncRecord().getOriginalUuid())) {
				getSyncRecord().setOriginalUuid(originalRecordUuid);
			}
		}
		catch (Exception ex) {
			log.error("Error processing Persistent collection, see callstack and inner expection", ex);
			throw new CallbackException("Error processing Persistent collection, see callstack and inner expection.", ex);
		}
	}

	/**
	 * Returns string representation of type for given object. The main idea is to strip off the
	 * hibernate proxy info, if it happens to be present.
	 *
	 * @param obj object
	 * @return
	 */
	private String getType(Object obj) {

		// be defensive about it
		if (obj == null) {
			throw new CallbackException("Error trying to determine type for object; object is null.");
		}

		Object concreteObj = obj;
		if (obj instanceof org.hibernate.proxy.HibernateProxy) {
			concreteObj = ((HibernateProxy) obj).getHibernateLazyInitializer().getImplementation();
		}

		return concreteObj.getClass().getName();
	}

	/**
	 * Sets the originating uuid for the sync record. This is done once per Tx; else we may end up
	 * with an empty string. NOTE: This code is needed because we need for entity to know if it is
	 * genuine local change or it is coming from the ingest code. This is what original_uuid field
	 * in sync_journal is used for. The way sync record uuids from ingest code are passed to the
	 * interceptor is by calling this method: the *1st* thing ingest code will do when processing
	 * changes is to issue this call to let interceptor know we are about to process ingest changes.
	 * This is done so that the intercetor can pull out the 'original' record uuid associated with
	 * the change to the entity that is being processed. Since ingest and interceptor do not have
	 * direct reference to each other, there is no simple way to pass this info directly. Note that
	 * this technique *relies* on the fact that syncRecordHolder is ThreadLocal; in other words, the
	 * uuid is passed and stored on the 'stack' by using thread local storage to ensure that
	 * multiple calling worker threads that maybe ingesting records concurrently are storring their
	 * respective record state locally. More: read javadoc on SyncRecord to see what
	 * SyncRecord.OriginalUuid is and how it is used.
	 *
	 * @param originalRecordUuid String representing value of orig record uuid
	 */
	public static void setOriginalRecordUuid(String originalRecordUuid) {
		String currentValue = getSyncRecord().getOriginalUuid();
		if (currentValue == null || "".equals(currentValue)) {
			getSyncRecord().setOriginalUuid(originalRecordUuid);
		}
		else {
			if (!currentValue.equals(originalRecordUuid)) {
				throw new SyncException("originalRecordUuid is already set to a different value than expected");
			}
		}
	}

	/**
	 * Adds syncItem to pending sync record for the patient stub necessary to handle new patient
	 * from the existing user scenario. See {@link SyncSubclassStub} class comments for detailed description
	 * of how this works.
	 *
	 * @see SyncSubclassStub
	 */
	public static void addSyncItemForSubclassStub(SyncSubclassStub stub) {

		try {

			// Setup the serialization data structures to hold the state
			Package pkg = new Package();
			String className = stub.getClass().getName();
			Record xml = pkg.createRecordForWrite(className);
			Item parentItem = xml.getRootItem();
			Item item = null;

			//uuid
			item = xml.createItem(parentItem, "uuid");
			item.setAttribute("type", stub.getUuid().getClass().getName());
			xml.createText(item, stub.getUuid());

			//requiredColumnNames
			item = xml.createItem(parentItem, "requiredColumnNames");
			item.setAttribute("type", "java.util.List<java.lang.String>");
			String value = "[";
			for (int x=0; x < stub.getRequiredColumnNames().size(); x++) {
				if (x != 0)
					value += ",";
				//value += "\"" + stub.getRequiredColumnNames().get(x) + "\"";
				value += stub.getRequiredColumnNames().get(x);
			}
			value += "]";

			xml.createText(item, value);

			//requiredColumnValues
			item = xml.createItem(parentItem, "requiredColumnValues");
			item.setAttribute("type", "java.util.List<java.lang.String>");
			value = "[";
			for (int x=0; x < stub.getRequiredColumnValues().size(); x++) {
				String columnvalue = stub.getRequiredColumnValues().get(x);
				if (x != 0)
					value += ",";
				value += columnvalue;
			}
			value += "]";
			xml.createText(item, value);

			//requiredColumnClasses
			item = xml.createItem(parentItem, "requiredColumnClasses");
			item.setAttribute("type", "java.util.List<java.lang.String>");
			value = "[";
			for (int x=0; x < stub.getRequiredColumnClasses().size(); x++) {
				String columnvalue = stub.getRequiredColumnClasses().get(x);
				if (x != 0)
					value += ",";
				//value += "\"" + columnvalue + "\"";
				value += columnvalue;
			}
			value += "]";
			xml.createText(item, value);

			//parentTable
			item = xml.createItem(parentItem, "parentTable");
			item.setAttribute("type", stub.getParentTable().getClass().getName());
			xml.createText(item, stub.getParentTable());
			//parentTableId
			item = xml.createItem(parentItem, "parentTableId");
			item.setAttribute("type", stub.getParentTableId().getClass().getName());
			xml.createText(item, stub.getParentTableId());
			//subclassTable
			item = xml.createItem(parentItem, "subclassTable");
			item.setAttribute("type", stub.getSubclassTable().getClass().getName());
			xml.createText(item, stub.getSubclassTable());
			//subclassTableId
			item = xml.createItem(parentItem, "subclassTableId");
			item.setAttribute("type", stub.getSubclassTableId().getClass().getName());
			xml.createText(item, stub.getSubclassTableId());

			SyncItem syncItem = new SyncItem();
			syncItem.setKey(new SyncItemKey<String>(stub.getUuid(), String.class));
			syncItem.setState(SyncItemState.NEW);
			syncItem.setContent(xml.toStringAsDocumentFragement());
			syncItem.setContainedType(stub.getClass());

			getSyncRecord().addItem(syncItem);
			getSyncRecord().addContainedClass(stub.getClass().getName());

		}
		catch (SyncException syncEx) {
			//just rethrow it
			throw (syncEx);
		}
		catch (Exception e) {
			throw (new SyncException("Unknow error while creating patient stub for patient uuid: " + stub.getUuid(), e));
		}

		return;
	}

	/**
	 * Utility method that recursively fetches the CollectionMetadata for the specified collection
	 * property and class from given SessionFactory object
	 *
	 * @param clazz the class in which the collection is defined
	 * @param collPropertyName the collection's property name
	 * @param sf SessionFactory object
	 * @return the CollectionMetadata if any
	 */
	private CollectionMetadata getCollectionMetadata(Class<?> clazz, String collPropertyName, SessionFactory sf) {
		CollectionMetadata cmd = sf.getCollectionMetadata(clazz.getName() + "." + collPropertyName);
		//Recursively check if there is collection metadata for the superclass
		if (cmd == null && clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass())) {
			return getCollectionMetadata(clazz.getSuperclass(), collPropertyName, sf);
		}
		return cmd;
	}

	/**
	 * @return the existing sync record for the current thread, creating a new sync record for the thread if none yet exists,
	 */
	private static SyncRecord getSyncRecord() {
		SyncRecord syncRecord = syncRecordHolder.get();
		if (syncRecord == null) {
			syncRecord = new SyncRecord();
			syncRecordHolder.set(syncRecord);
		}
		return syncRecord;
	}

	/**
	 * @return the syncService
	 */
	private SyncService getSyncService() {
		return Context.getService(SyncService.class);
	}

	/**
	 * @return the sessionFactory
	 */
	private SessionFactory getSessionFactory() {
		return (SessionFactory) context.getBean("sessionFactory");
	}

	/**
	 * From Spring docs: There might be a single instance of Interceptor for a SessionFactory, or a
	 * new instance might be specified for each Session. Whichever approach is used, the interceptor
	 * must be serializable if the Session is to be serializable. This means that
	 * SessionFactory-scoped interceptors should implement readResolve().
	 */
	private Object readResolve() throws ObjectStreamException {
		return getInstance();
	}

	/**
	 * @see ApplicationContextAware#setApplicationContext(ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	/**
	 * Helper container class to store type/value tuple for a given object property. Utilized during
	 * serialization of intercepted entity changes.
	 *
	 * @see HibernateSyncInterceptor#packageObject(org.openmrs.OpenmrsObject, Object[], String[], org.hibernate.type.Type[], java.io.Serializable, org.openmrs.module.sync.SyncItemState)
	 */
	protected class PropertyClassValue {

		String clazz;
		String value;

		public PropertyClassValue(String clazz, String value) {
			this.clazz = clazz;
			this.value = value;
		}

		public String getClazz() {
			return clazz;
		}

		public String getValue() {
			return value;
		}
	}
}
