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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.OpenmrsObject;
import org.openmrs.Person;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.SerializedObject;
import org.openmrs.module.ModuleUtil;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncItemState;
import org.openmrs.module.sync.SyncProcessedObject;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncSubclassStub;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncIngestService;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.api.db.SyncDAO;
import org.openmrs.module.sync.api.db.hibernate.HibernateSyncInterceptor;
import org.openmrs.module.sync.ingest.SyncImportItem;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.ingest.SyncIngestException;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.w3c.dom.NodeList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncIngestServiceImpl implements SyncIngestService {

    private Log log = LogFactory.getLog(this.getClass());
    
    private SyncDAO dao;
    
    public void setSyncDAO(SyncDAO dao) {
    	this.dao = dao;
    }
    
    /**
     * @see org.openmrs.module.sync.api.SyncIngestService#processSyncImportRecord(SyncImportRecord, RemoteServer)
     * @param importRecord
     * @throws APIException
     */
    public void processSyncImportRecord(SyncImportRecord importRecord, RemoteServer server) throws APIException {
        if ( importRecord != null ) {
            if ( importRecord.getUuid() != null && importRecord.getState() != null ) {
                SyncRecord record = Context.getService(SyncService.class).getSyncRecordByOriginalUuid(importRecord.getUuid());
                // ignore the incoming ack if matching sync record cannot be found
                if (record == null) return;
                if ( server.getServerType().equals(RemoteServerType.PARENT) ) {
                    // with parents, we set the actual state of the record
                    if ( importRecord.getState().equals(SyncRecordState.ALREADY_COMMITTED) ) record.setState(SyncRecordState.COMMITTED);
                    else if ( importRecord.getState().equals(SyncRecordState.REJECTED) ) {
                    		record.setState(SyncRecordState.FAILED); 
                    		log.error("Sync Response for record " + record.getUuid() + " returned REJECTED, meaning that the failure on the target server was caused by openmrs version differences." );
                    } else if ( importRecord.getState().equals(SyncRecordState.NOT_SUPPOSED_TO_SYNC) ) record.setState(SyncRecordState.REJECTED);
                    else record.setState(importRecord.getState());
                } else {
                    // with non-parents we set state in the server-record
                    SyncServerRecord serverRecord = record.getServerRecord(server);
                    if ( importRecord.getState().equals(SyncRecordState.ALREADY_COMMITTED) ) serverRecord.setState(SyncRecordState.COMMITTED);
                    else if ( importRecord.getState().equals(SyncRecordState.REJECTED) ) {
                    		serverRecord.setState(SyncRecordState.FAILED);
                    		log.error("Sync Response for record " + record.getUuid() + " returned REJECTED, meaning that the failure on the target server was caused by openmrs version differences." );
                    } else if ( importRecord.getState().equals(SyncRecordState.NOT_SUPPOSED_TO_SYNC) ) serverRecord.setState(SyncRecordState.REJECTED);
                    else serverRecord.setState(importRecord.getState());
                    
                    // record (or clear out) the error message for this server and this record
                    serverRecord.setErrorMessage(importRecord.getErrorMessage());
                }
                
                Context.getService(SyncService.class).updateSyncRecord(record);
            }
        }        
    }
    
    /**
     * Applies  synchronization record against the local data store in single transaction.  
     * <p/> Remarks: Exceptions are always thrown if something goes wrong while processing the record in order to abort sync items as 
     * one transaction. To report back SyncImportRecord accurately in case of exception, notice that SyncIngestException contains
     * SyncImportRecord. In case of exception, callers should inspect this value as it will contain more information about the status of sync
     * item as it failed.
     * <p/> Processing PatientIdentifier updates: *updates* to PatientIdentifier objects are processed last. This is because
     * patient.identifiers is a TreeSet and any updates to the referenced objects can potentially mess up the treeset.
     * This is especially the case when patient identifier is changed to voided: voiding it changes its ordering per
     * PatientIdentifier.CompareTo() method to 'last'. This means that if there is a treeset when we void the first
     * identifier, the treeset cannot be navigated & all operations such as contains(), remove() will return false.
     * This is because treesets need to be 'resorted' if such changes are made to held objects..however re-sorting it via
     * remove/add() is not feasible in our case since the actual type of patient.identifier collection is hibernate
     * persistensortedset; this overrides remove() method and by calling remove()/add() the actual 'delete' to the 
     * database is generated.
     * To deal with this issue, simply process all patient identifier inserts and deletes first, and only then
     * process updates that can potentially mess up the treeset order. The long-term fix to this is not to use
     * treesets for collection of mutable objects such as patient.identifiers in core.
     * 
     * @param record SyncRecord to be processed
     * @param server Server where the record came from
     * @return
     */
    public SyncImportRecord processSyncRecord(SyncRecord record, RemoteServer server) throws SyncIngestException {
        
    	ArrayList<SyncItem> deletedItems = new ArrayList<SyncItem>();
    	Map<String, Class> deletedObjects = new HashMap<String, Class>(); // actual openmrsobjects deleted.  so that we don't try to process them again when saving/updating things in the treeSetItems list
    	ArrayList<SyncItem> treeSetItems = new ArrayList<SyncItem>();  //these are processed out of order.  See method comments.
    	ArrayList<SyncItem> regularNewAndUpdateItems = new ArrayList<SyncItem>();  //inserts and updates
    	SyncImportRecord importRecord = new SyncImportRecord();
        importRecord.setState(SyncRecordState.FAILED);  // by default, until we know otherwise
        importRecord.setRetryCount(record.getRetryCount());
        importRecord.setTimestamp(record.getTimestamp());
        importRecord.setUuid(record.getOriginalUuid());
        importRecord.setSourceServer(server);
        
        // map of class name to objects of the classes that were updated in this record
        Map<String, List<SyncProcessedObject>> processedObjects = new HashMap<String, List<SyncProcessedObject>>();
        
        SyncService syncService = Context.getService(SyncService.class);
        SyncIngestService syncIngestService = Context.getService(SyncIngestService.class);
		try {
            // first, let's see if this server even accepts this kind of syncRecord
            if ( !server.shouldReceiveSyncRecordFrom(record)) {
                importRecord.setState(SyncRecordState.NOT_SUPPOSED_TO_SYNC);
				String errorMessage = "NOT INGESTING RECORD with " + record.getContainedClasses() + " BECAUSE SERVER IS NOT READY TO ACCEPT ALL CONTAINED OBJECTS";
                importRecord.setErrorMessage(errorMessage);
				log.warn("\n" + errorMessage + "\n");
            }
			else if (!isValidVersion(record)) {
            	importRecord.setState(SyncRecordState.REJECTED);
				String errorMessage = "NOT INGESTING RECORD with version " + record.getDatabaseVersion() + " BECAUSE SERVER IS NOT COMPATIBLE";
                importRecord.setErrorMessage(errorMessage);
				log.warn("\n" + errorMessage + "\n");
            }
			else {
                //log.warn("\nINGESTING ALL CLASSES: " + recordClasses + " BECAUSE SERVER IS READY TO ACCEPT ALL");
                // second, let's see if this SyncRecord has already been imported
                // use the original record id to locate import_record copy
                log.debug("AT THIS POINT, ORIGINALUUID FOR RECORD IS " + record.getOriginalUuid());
                importRecord = syncService.getSyncImportRecord(record.getOriginalUuid());
                boolean isUpdateNeeded = false;
                
                if ( importRecord == null ) {
                	log.info("ImportRecord does not exist, so creating new one");
                    isUpdateNeeded = true;
                    importRecord = new SyncImportRecord(record);
                    importRecord.setState(SyncRecordState.FAILED);
                    importRecord.setUuid(record.getOriginalUuid());
                    importRecord.setSourceServer(server);
                    syncService.createSyncImportRecord(importRecord);
                } else {
                	if (log.isWarnEnabled()) {
                		log.warn("ImportRecord already exists and has retry count: " + importRecord.getRetryCount() + ", state: " + importRecord.getState());
                	}
                    SyncRecordState state = importRecord.getState();
                    if ( state.isFinal() ) {
                        // apparently, the remote/child server exporting to this server doesn't realize it's
                        // committed, so let's remind by sending back this import record with already_committed
                        importRecord.setState(SyncRecordState.ALREADY_COMMITTED);
                    }
					else if (state.equals(SyncRecordState.FAILED)) {
                		//mark as failed and retry next time
                    	importRecord.setState(SyncRecordState.FAILED);
                		importRecord.setRetryCount(importRecord.getRetryCount() + 1);
                		isUpdateNeeded = true;
                    }
					else {
                        isUpdateNeeded = true;
                    }
                }
                
                if ( isUpdateNeeded ) {
                    log.debug("Looks like update is needed");
                	
                    boolean isError = false;
                            
                    //as we start setting properties, suspend session flushing 
                    syncService.setFlushModeManual();

                    // for each sync item, process it and insert/update the database; 
                    //put deletes into deletedItems collection -- these will get processed last
                    for ( SyncItem item : record.getItems() ) {
                    	//System.out.println("item: " + item.getContainedType() + " state: " + item.getState());
                    	//System.out.println("content: " + item.getContent());
                    	if (item.getState() == SyncItemState.DELETED) {
                    		deletedItems.add(item);
                    	}
						else if (item.getState() == SyncItemState.UPDATED && item.getContainedType() != null && (
                    			   "org.openmrs.PatientIdentifier".equals(item.getContainedType().getName())
                    			|| "org.openmrs.PersonAttribute".equals(item.getContainedType().getName())
                    			|| "org.openmrs.PersonAddress".equals(item.getContainedType().getName())
                    			|| "org.openmrs.PersonName".equals(item.getContainedType().getName())
                    			)) {
                    		treeSetItems.add(item);
                    	}
						else if (Person.class.isAssignableFrom(item.getContainedType()) || Concept.class.isAssignableFrom(item.getContainedType()) || SyncSubclassStub.class.isAssignableFrom(item.getContainedType())
                    			|| SerializedObject.class.isAssignableFrom(item.getContainedType())){
                    		//Sync-180: Person items need to be processed first, Concept exhibited same behavior.
		                    SyncImportItem importedItem = syncIngestService.processSyncItem(item, record.getOriginalUuid() + "|" + server.getUuid(), processedObjects);
		                    importedItem.setKey(item.getKey());
		                    importRecord.addItem(importedItem);
		                    if ( !importedItem.getState().equals(SyncItemState.SYNCHRONIZED)) isError = true;
                    	}
						else {
                    		regularNewAndUpdateItems.add(item);
                    	}
                    }

                    for (SyncItem item : regularNewAndUpdateItems){
	                    SyncImportItem importedItem = syncIngestService.processSyncItem(item, record.getOriginalUuid() + "|" + server.getUuid(), processedObjects);
	                    importedItem.setKey(item.getKey());
	                    importRecord.addItem(importedItem);
	                    if ( !importedItem.getState().equals(SyncItemState.SYNCHRONIZED)) isError = true;
                    }
                    
                    syncService.flushSession();
                    syncService.setFlushModeAutomatic();
                    Context.clearSession(); // so that objects aren't resaved at next flush below
                    
                    /* now run through deletes: deletes must be processed after inserts/updates
                     * because of hibernate flushing semantics inside transactions:
                     * if deleted entity is part of a collection on another object within the same session
                     * and this object gets flushed, error is thrown stating that deleted entities must first be removed
                     * from collection; this happens immediately when stmts are executed (and not at the Tx boundary) because
                     * default hibernate FlushMode is AUTO. To further avoid this issue, explicitly suspend flushing for the 
                     * duration of deletes.
                     */
                	syncService.setFlushModeManual(); 
                    for ( SyncItem item : deletedItems ) {
                        SyncImportItem importedItem = this.processSyncItem(item, record.getOriginalUuid() + "|" + server.getUuid(), processedObjects);
                        importedItem.setKey(item.getKey());
                        importRecord.addItem(importedItem);
                        // save this object for later so we're sure to not update it when processing the "treesetitems"
                        deletedObjects.put((String)item.getKey().getKeyValue(), item.getContainedType());
                        if ( !importedItem.getState().equals(SyncItemState.SYNCHRONIZED)) isError = true;
                    }
                    syncService.flushSession();
                    syncService.setFlushModeAutomatic();
                    Context.clearSession(); // so that objects aren't resaved at next flush below
                    
                    /* Run through the updates for patient props that are treesets, see the method comments to understand
                     * why this is done here. 
                     */
                    syncService.setFlushModeManual(); 
                    for ( SyncItem item : treeSetItems ) {
                    	if (item.getContainedType().equals(deletedObjects.get((String)item.getKey().getKeyValue()))) {
                    		log.debug("skipping update of " + item.getContainedType() + ":" + item.getKey() + " because we just deleted it");
                    		continue;
                    	}
                    	
                    	//why is the identifier not getting into a sync item?
                        SyncImportItem importedItem = syncIngestService.processSyncItem(item, record.getOriginalUuid() + "|" + server.getUuid(), processedObjects);
                        importedItem.setKey(item.getKey());
                        importRecord.addItem(importedItem);
                        if ( !importedItem.getState().equals(SyncItemState.SYNCHRONIZED)) isError = true;
                    }
                    syncService.flushSession();
                    syncService.setFlushModeAutomatic();
                    Context.clearSession(); // so that objects aren't resaved at next flush below
                   
                    /* 
                     * finally execute the pending actions that resulted from processing all sync items 
                     */
                    syncService.setFlushModeManual();
                    syncIngestService.applyPreCommitRecordActions(processedObjects);
                    syncService.flushSession();
                    syncService.setFlushModeAutomatic();
                    Context.clearSession(); // so that objects aren't resaved at next flush below
                    
                    if ( !isError ) {
                        importRecord.setState(SyncRecordState.COMMITTED);
                    }
					else {
                    	//One of SyncItem commits failed, throw to rollback and set failure information.
                    	log.warn("Error while processing SyncRecord with original uuid " + record.getOriginalUuid() + " (" + record.getContainedClasses() + ")");
                        importRecord.setState(SyncRecordState.FAILED);
                        throw new SyncIngestException(SyncConstants.ERROR_ITEM_NOT_COMMITTED,null,null,importRecord);
                    }
                    
                }
            }
        }
		catch (SyncIngestException e) {
	        log.error("Unable to ingest a sync request", e);
        	//fill in sync import record and rethrow to abort tx
	        importRecord.setState(SyncRecordState.FAILED);
	        importRecord.setErrorMessage(e.getMessage() + ":  " + OpenmrsUtil.shortenedStackTrace(ExceptionUtils.getFullStackTrace(e)));
        	e.setSyncImportRecord(importRecord);
        	throw (e);
        }
        catch (Exception e ) {
        	log.error("Unexpected exception occurred when processing sync records", e);
            //fill in sync import record and rethrow to abort tx
            importRecord.setState(SyncRecordState.FAILED);
            importRecord.setErrorMessage(e.getMessage() + ":  " + OpenmrsUtil.shortenedStackTrace(ExceptionUtils.getFullStackTrace(e)));
            throw new SyncIngestException(e,SyncConstants.ERROR_RECORD_UNEXPECTED,null,null,importRecord);
        }
		finally {
        	syncService.updateSyncImportRecord(importRecord);
        	
        	//reset the flush mode back to automatic, no matter what
        	syncService.setFlushModeAutomatic();
        }
        //for hibernate SYNC-175
        server = null;
        return importRecord;
    }
	
	/**
	 * Compares the code/database version for the incoming sync record against this server's code
	 * version. If they are different, the record should be denied.
	 * 
	 * @param record the incoming SyncRecord
	 * @return true if the record's database version matches this server's version
	 */
	private boolean isValidVersion(SyncRecord record) {
		return ModuleUtil.compareVersion(OpenmrsConstants.OPENMRS_VERSION_SHORT, record.getDatabaseVersion()) == 0;
	}

	/**
	 * Applies the 'actions' identified during the processing of the record that need to be 
	 * processed (for whatever reason) just before the sync record is to be committed.
	 * 
	 * The actions understood by this method:
	 * <br/>REBUILD XSN 
	 * <br/>- call to formentry module and attempt to rebuild XSN, 
	 * <br/>- HashMap object will contain instance of Form object to be rebuilt
	 * <br/>UPDATE CONCEPT WORDS 
	 * <br/>- call to concept service to update concept words for given concept 
	 * <br/>- HashMap object will contain instance of Concept object which concept words are to be rebuilt
	 * 
	 */
    // TODO: does this really happen precommit?  wouldn't he call to updateConceptWord force a commit?
	public void applyPreCommitRecordActions(Map<String, List<SyncProcessedObject>> processedObjects) {
		
		if (processedObjects == null)
			return;
		
		// rebuild xsns if a form edit comes through
		List<SyncProcessedObject> xsns = processedObjects.get("org.openmrs.module.formentry.FormEntryXsn");
		if (xsns != null) {
			for (SyncProcessedObject xsn : xsns) {
				SyncUtil.rebuildXSN(xsn.getObject());
			}
		} else {
			//even if XSNs aren't sync-ed, look for forms to update form.template if needed
			List<SyncProcessedObject> forms = processedObjects.get("org.openmrs.Form");
			if (forms != null) {
				for (SyncProcessedObject form : forms) {
					if (form.getObject() instanceof org.openmrs.Form) {
						SyncUtil.rebuildXSNForForm((org.openmrs.Form)form.getObject());
					}
				}
			}
			
		}
		
		// fix concept words for all names found
		List<SyncProcessedObject> names = processedObjects.get("org.openmrs.ConceptName");
		if (names != null) {
			for (SyncProcessedObject o : names) {
				// we only want to update the concept words if this is NOT a delete action
				if (o.getState() != SyncItemState.DELETED) {
                    // we need to reload the concept here because the session has been cleared earlier
					Concept c = Context.getConceptService().getConcept(((ConceptName) o.getObject()).getConcept().getId());
					Context.getConceptService().updateConceptWord(c);
				}
			}
		}
	}
    
    /**
     * Note: preCommitRecordActions collection is provided as a way for the OpenmrsObject instances to 'schedule' action that is necessary
     * for processing of the object yet it cannot be applied until the end of the processing of the parent sync record. For example, rebuild XSN
     * cannot happen until all form fields held in the sync items are applied first; thus the call to rebuild XSN need to happen after all
     * sync items were processed and before committing the sync record.
     * 
     * HashMap contained in the collection is to capture the action, and the necessary object to resolve that action. The action
     * is understood and applied by applyPreCommitRecordActions
     * 
     * @see org.openmrs.module.sync.api.SyncIngestService#processSyncItem(org.openmrs.module.sync.SyncItem, java.lang.String, java.util.Map)
     * 
     */
    public SyncImportItem processSyncItem(SyncItem item, String originalRecordUuid, Map<String, List<SyncProcessedObject>> processedObjects)  throws APIException {
    	String itemContent = null;
        SyncImportItem ret = null; 

        try {
        	ret = new SyncImportItem();
            //ret.setContent(itemContent); - no need to copy content back: the server that send it knows it already
            ret.setState(SyncItemState.UNKNOWN);

            Object o = null;
			itemContent = item.getContent();
			
            if (log.isDebugEnabled()) {
                log.debug("STARTING TO PROCESS: " + itemContent);
                log.debug("SyncItem state is: " + item.getState());
            }
            
            o = SyncUtil.getRootObject(itemContent);
            if (o instanceof org.hibernate.collection.PersistentCollection) {
            	log.debug("Processing a persistent collection");
            	dao.processCollection(o.getClass(),itemContent,originalRecordUuid);
            }
			else {
            	// do the saving of the object to the database, etc
            	 OpenmrsObject openmrsObject = processOpenmrsObject((OpenmrsObject)o, item, originalRecordUuid);
				
				// add this object to the proccessedObjects list
            	String className = o.getClass().getName();
            	if (!processedObjects.containsKey(className)) {
            		List<SyncProcessedObject> objects = new ArrayList<SyncProcessedObject>();
            		objects.add(new SyncProcessedObject(openmrsObject, item.getState()));
            		processedObjects.put(className, objects);
            	}
            	else {
            		processedObjects.get(className).add(new SyncProcessedObject(openmrsObject, item.getState()));
            	}
            }
            ret.setState(SyncItemState.SYNCHRONIZED);                
        }
		catch (SyncIngestException e) {
        	e.setSyncItemContent(itemContent);  //MUST RETHROW to abort transaction
        	throw (e);
        }
        catch (Exception e) {
            throw new SyncIngestException(e,SyncConstants.ERROR_ITEM_UNEXPECTED, null, itemContent, null);  //MUST RETHROW to abort transaction
        }       
        
        return ret;        
    }

    /**
     * Takes steps necessary to handle ingest of {@link SyncSubclassStub} by calling 
     * {@link SyncDAO#processSyncSubclassStub(SyncSubclassStub)} 
     * 
     * param stub {@link SyncSubclassStub} to be saved.
     */
    public void processSyncSubclassStub(SyncSubclassStub stub) throws APIException {
    	dao.processSyncSubclassStub(stub);
    	return;
    }
    
    
    /**
     * Processes serialized SyncItem state by attempting to hydrate the object SyncItem represents and then using OpenMRS service layer to
     * update the hydrated instance of OpenmrsObject object.
     * <p/>Remarks: This implementation relies on internal knowledge of how SyncItems are serialized: it iterates over direct child nodes of the root xml
     * node in incoming assuming they are serialized public properties of the object that is being hydrated. Consequently, for each child node, 
     * property setter is determined and then called. After setting all properties, OpenMRS service layer API is used to actually save 
     * the object into persistent store. The details of how property setters are determined and how appropriate service layer methods
     * are determined are contained in SyncUtil class.
     * <p/>
     * SyncItem with status of DELETED is handled differently from insert/update: In case of a delete, all that is needed (and sent) 
     * is the object type and its UUID. Consequently, the process for handling deletes consists of first fetching 
     * existing object by uuid and then deleting it by a call to sync service API. Note, if object is not found in DB by its uuid, we
     * skip the delete and record warning message. 
     * <p/>
     * preCommitRecordActions collection is provided as a way for the OpenmrsObject instances to 'schedule' action that is necessary
     * for processing of the object yet it cannot be applied until the end of the processing of the parent sync record. For example, rebuild XSN
     * cannot happen until all form fields held in the sync items are applied first; thus the call to rebuild XSN need to happen after all
     * sync items were processed and before committing the sync record.
     *  
     * @param o empty instance of class that this SyncItem represents 
     * @param item SyncItem.
     * @param originalRecordUuid Unique id of the sync record that this SyncItem recorded in when this object was first created. NOTE:
     * this value is retained and forwarded unchanged throughout the network of synchronizing servers in order to avoid re-applying
     * same changes over and over.
     * @return the saved OpenmrsObject (could be different than what is passed in if updating a record)
     * 
     * @see SyncUtil#setProperty(Object, String, Object)
     * @see SyncUtil#getOpenmrsObj(String, String)
     * @see SyncUtil#updateOpenmrsObject(OpenmrsObject, String, String)
     */
    private OpenmrsObject processOpenmrsObject(OpenmrsObject o, SyncItem item, String originalRecordUuid) throws Exception {

    	String itemContent = null;
        String className = null;
        boolean alreadyExists = false;
        boolean isDelete = false;
        ArrayList<Field> allFields = null;
        NodeList nodes = null;

        isDelete = (item.getState() == SyncItemState.DELETED) ? true : false; 
        itemContent = item.getContent();
    	className = o.getClass().getName();
        allFields = SyncUtil.getAllFields(o);  // get fields, both in class and superclass - we'll need to know what type each field is
        nodes = SyncUtil.getChildNodes(itemContent);  // get all child nodes (xml) of the root object

	    if ( o == null || className == null || allFields == null || nodes == null ) {
	    	log.warn("Item is missing a className or all fields or nodes");
	    	throw new SyncIngestException(SyncConstants.ERROR_ITEM_NOCLASS, className, itemContent,null);
	    }

	    String uuid = SyncUtil.getAttribute(nodes, "uuid", allFields);
        OpenmrsObject objOld = SyncUtil.getOpenmrsObj(className, uuid);
        if ( objOld != null ) {
            o = objOld;
            alreadyExists = true;
        }
	       
        if (log.isDebugEnabled()) {
	        log.debug("isUpdate: " + alreadyExists);
	        log.debug("isDelete: " + isDelete);
        }
                
        
		//Pass the original uuid to interceptor: this will prevent the change
		//from being sent back to originating server. 
        HibernateSyncInterceptor.setOriginalRecordUuid(originalRecordUuid);
        
    	//execute delete if instance was found and operation is delete
        if (alreadyExists && isDelete) {
        	SyncUtil.deleteOpenmrsObject(o);
        }else if (!alreadyExists && isDelete) { 
        	log.warn("Object to be deleted was not found in the database. skipping delete operation:");
        	log.warn("-object type: " + o.getClass().toString());
        	log.warn("-object uuid: " + uuid);
        } else {
            //if we are doing insert/update:
            //1. set serialized props state
        	//2. force it down the hibernate's throat with help of openmrs api
	        for ( int i = 0; i < nodes.getLength(); i++ ) {
	            try {
	            	log.debug("trying to set property: " + nodes.item(i).getNodeName() + " in className " + className);
	                SyncUtil.setProperty(o, nodes.item(i), allFields);
	            } catch ( Exception e ) {
	            	log.error("Error when trying to set " + nodes.item(i).getNodeName() + ", which is a " + className, e);
	                throw new SyncIngestException(e, SyncConstants.ERROR_ITEM_UNSET_PROPERTY, nodes.item(i).getNodeName() + "," + className + "," + e.getMessage(), itemContent,null);
	            }
	        }
        	        
	        // now try to commit this fully inflated object
	        try {
	        	log.debug("About to update or create a " + className + " object, uuid: '" + uuid + "'");
	            SyncUtil.updateOpenmrsObject(o, className, uuid);
	            Context.getService(SyncService.class).flushSession();
	        } catch ( Exception e ) {
	        	// don't include stacktrace here because the parent classes log it sufficiently
	        	log.error("Unexpected exception occurred while saving openmrsobject: " + className + ", uuid '" + uuid + "'");
	            throw new SyncIngestException(e, SyncConstants.ERROR_ITEM_NOT_COMMITTED, e.getMessage(), itemContent, null);
	        }
        }
        	                
        return o;
    }

    /**
     * (non-Javadoc)
     * @see org.openmrs.module.sync.api.SyncIngestService#isConceptIdValidForUuid(Integer, java.lang.String)
     */
    public boolean isConceptIdValidForUuid(Integer conceptId, String uuid) throws APIException {
 
    	return dao.isConceptIdValidForUuid(conceptId, uuid);
    }
}
