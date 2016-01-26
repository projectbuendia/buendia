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
package org.openmrs.module.sync.ingest;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncException;
import org.openmrs.module.sync.SyncTransmission;
import org.openmrs.module.sync.SyncTransmissionState;
import org.openmrs.module.sync.serialization.FilePackage;
import org.openmrs.module.sync.serialization.IItem;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.ConnectionResponse;
import org.openmrs.module.sync.server.ServerConnectionState;
import org.openmrs.util.OpenmrsUtil;

/**
 * SyncTransmission a collection of sync records to be sent to the parent.
 */
public class SyncTransmissionResponse implements IItem {

    // consts

    // fields
    private final Log log = LogFactory.getLog(getClass());
     
    private String fileName = null;
    private Date timestamp = null;
    private List<SyncImportRecord> syncImportRecords = null;
    private String uuid = null;
    private String fileOutput = "";
    private SyncTransmissionState state;
    private String errorMessage;
    private String syncSourceUuid = null; //UUID of the node where the Tx came from
    private String syncTargetUuid = null; //UUID of the node where Tx is being applied to, and who is now sending a response
    private SyncTransmission syncTransmission = null;

    // constructor(s)
    public SyncTransmissionResponse() {
    	
    }

    public SyncTransmission getSyncTransmission() {
        return syncTransmission;
    }

    public void setSyncTransmission(SyncTransmission syncTransmission) {
        this.syncTransmission = syncTransmission;
    }

    /* 
     * Take passed in records and create a new sync_tx file
     */
    
    public SyncTransmissionResponse(SyncTransmission transmission) {
    	// needs to be null-safe
    	if ( transmission != null ) {
        	this.uuid = transmission.getUuid();
            this.syncSourceUuid = transmission.getSyncSourceUuid();
            this.syncTargetUuid = SyncConstants.UUID_UNKNOWN;
        	fileName = transmission.getFileName();
        	int idx = fileName.lastIndexOf(".");
        	if ( idx > -1 ) fileName = fileName.substring(0, idx) + SyncConstants.RESPONSE_SUFFIX + fileName.substring(idx);
        	else fileName = fileName + SyncConstants.RESPONSE_SUFFIX;
        	this.state = SyncTransmissionState.OK;  // even though we really mean "OK so far" - it'll get overwritten later if there's a prob
    	} else {
    		this.uuid = SyncConstants.UUID_UNKNOWN;
            this.syncSourceUuid = SyncConstants.UUID_UNKNOWN;
            this.syncTargetUuid = SyncConstants.UUID_UNKNOWN;
    		this.errorMessage = SyncConstants.ERROR_TX_NOT_UNDERSTOOD;
    		this.fileName = SyncConstants.FILENAME_TX_NOT_UNDERSTOOD;
    		this.state = SyncTransmissionState.TRANSMISSION_NOT_UNDERSTOOD;
    	}
    }

    /**
     * @param connResponse
     */
    public SyncTransmissionResponse(ConnectionResponse connResponse) {
	    // this needs to be bulletproof
    	if ( connResponse != null ) {
    		
    		if (log.isInfoEnabled())
    			log.info("RESPONSE PAYLOAD IS: " + connResponse.getResponsePayload());
    		
    		if ( connResponse.getState().equals(ServerConnectionState.OK) ) {
    			try {
    				// this method is null safe
    				SyncTransmissionResponse str = SyncDeserializer.xmlToSyncTransmissionResponse(connResponse.getResponsePayload());
    				this.errorMessage = str.getErrorMessage();
    				this.fileName = str.getFileName();
    				this.uuid = str.getUuid();
                    this.syncSourceUuid = str.getSyncSourceUuid();
                    this.syncTargetUuid = str.getSyncTargetUuid();
    				this.state = str.getState();
    				this.syncImportRecords = str.getSyncImportRecords();
                    this.syncTransmission = str.getSyncTransmission();
    			} catch (Exception e) {
    				e.printStackTrace();
    	    		this.errorMessage = SyncConstants.ERROR_RESPONSE_NOT_UNDERSTOOD.toString();
    	        	this.fileName = SyncConstants.FILENAME_RESPONSE_NOT_UNDERSTOOD;
    	        	this.uuid = SyncConstants.UUID_UNKNOWN;
                    this.syncSourceUuid = SyncConstants.UUID_UNKNOWN;
                    this.syncTargetUuid = SyncConstants.UUID_UNKNOWN;
    	        	this.state = SyncTransmissionState.RESPONSE_NOT_UNDERSTOOD;
    			} 
    		} else {
        		this.errorMessage = SyncConstants.ERROR_SEND_FAILED.toString();
            	this.fileName = SyncConstants.FILENAME_SEND_FAILED;
            	this.uuid = SyncConstants.UUID_UNKNOWN;
                this.syncSourceUuid = SyncConstants.UUID_UNKNOWN;
                this.syncTargetUuid = SyncConstants.UUID_UNKNOWN;
            	this.state = SyncTransmissionState.FAILED;
            	if ( connResponse.getState().equals(ServerConnectionState.MALFORMED_URL)) this.state = SyncTransmissionState.MALFORMED_URL;
            	if ( connResponse.getState().equals(ServerConnectionState.CERTIFICATE_FAILED)) this.state = SyncTransmissionState.CERTIFICATE_FAILED;
    		}
    	} else {
    		this.errorMessage = SyncConstants.ERROR_SEND_FAILED.toString();
        	this.fileName = SyncConstants.FILENAME_SEND_FAILED;
        	this.uuid = SyncConstants.UUID_UNKNOWN;
            this.syncSourceUuid = SyncConstants.UUID_UNKNOWN;
            this.syncTargetUuid = SyncConstants.UUID_UNKNOWN;
        	this.state = SyncTransmissionState.FAILED;
    	}
    }

	public List<SyncImportRecord> getSyncImportRecords() {
    	return syncImportRecords;
    }

	public void setSyncImportRecords(List<SyncImportRecord> syncImportRecords) {
    	this.syncImportRecords = syncImportRecords;
    }

	public String getErrorMessage() {
    	return errorMessage;
    }

	public void setErrorMessage(String errorMessage) {
    	this.errorMessage = errorMessage;
    }

    public String getSyncSourceUuid() {
        return syncSourceUuid;
    }

    public void setSyncSourceUuid(String value) {
        this.syncSourceUuid = value;
    }

    public String getSyncTargetUuid() {
        return syncTargetUuid;
    }

    public void setSyncTargetUuid(String value) {
        this.syncTargetUuid = value;
    }    
    
	// methods
    public String getFileOutput() {
    	return fileOutput;
    }

    public String getFileName() {
        return fileName;
    }
    public void setFileName(String value) {
        fileName = value;
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String value) {
        uuid = value;
    }
    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date value) {
        timestamp = value;
    }
    
    /** Create a new transmission from records: use org.openmrs.serial to make a file
     *  also, give option to write to a file or not 
     */
    public void createFile(boolean writeFile) {
    	createFile(writeFile, SyncConstants.DIR_IMPORT);
    }
    	
    /** Create a new transmission from records: use org.openmrs.serial to make a file
     *  also, give option to write to a file or not 
     */
    public void createFile(boolean writeFile, String path) {

    	if ( path == null ) path = SyncConstants.DIR_IMPORT;
    	if ( path.length() == 0 ) path = SyncConstants.DIR_IMPORT;
    	
        try {            
            if (timestamp == null) this.timestamp = new Date(); //set timestamp of this export, if not already set
            
            FilePackage pkg = new FilePackage();
            Record xml = pkg.createRecordForWrite(this.getClass().getName());
            Item root = xml.getRootItem();

            //serialize
            this.save(xml,root);

            //now dump to file
            //TODO: use path!
            File dir = OpenmrsUtil.getDirectoryInApplicationDataDirectory("sync");
            File importdir = new File(dir, "import");
            importdir.mkdir();
            fileOutput = pkg.savePackage(new File(importdir, fileName), writeFile);

        } catch (Exception e) {
            log.error("Cannot create sync transmission.",e);
            throw new SyncException("Cannot create sync transmission", e);
        }
        return;

    }

    /** IItem.save() implementation
     * 
     */
    public Item save(Record xml, Item me) throws Exception {
        //Item me = xml.createItem(parent, this.getClass().getName());
        
        //serialize primitives
        if (uuid != null) xml.setAttribute(me, "uuid", uuid);
        if (fileName != null) xml.setAttribute(me, "fileName", fileName);
        if (state != null) xml.setAttribute(me, "state", state.toString());
        if (errorMessage != null ) xml.setAttribute(me, "errorMessage", errorMessage);
        if (syncSourceUuid != null)  xml.setAttribute(me, "syncSourceUuid", syncSourceUuid);
        if (syncTargetUuid != null)  xml.setAttribute(me, "syncTargetUuid", syncTargetUuid);
        if (timestamp != null) xml.setAttribute(me, "timestamp", new TimestampNormalizer().toString(timestamp));
        
        //serialize Records list
        Item itemsCollection = xml.createItem(me, "records");
        
        if (syncImportRecords != null) {
            me.setAttribute("itemCount", Integer.toString(syncImportRecords.size()));
            for ( SyncImportRecord importRecord : syncImportRecords ) {
            	importRecord.save(xml, itemsCollection);
            }
        }

        Item syncTx = xml.createItem(me, "syncTransmission");
        
        if (syncTransmission != null) {
            syncTransmission.save(xml, syncTx);
        }

        return me;
    }

    /** IItem.load() implementation
     * 
     */
    public void load(Record xml, Item me) throws Exception {

        this.uuid = me.getAttribute("uuid");
        this.fileName = me.getAttribute("fileName");
        this.syncSourceUuid = me.getAttribute("syncSourceUuid");
        this.syncTargetUuid = me.getAttribute("syncTargetUuid");

        if (me.getAttribute("timestamp") == null)
            this.timestamp = null;
        else
            this.timestamp = (Date)new TimestampNormalizer().fromString(Date.class,me.getAttribute("timestamp"));
        
        try {
        	this.state = SyncTransmissionState.valueOf(me.getAttribute("state"));
        } catch ( Exception e ) {
        	log.info("STATE IS [" + me.getAttribute("state") + "], defaulting to RESPONSE_NOT_UNDERSTOOD", e);
        	this.state = SyncTransmissionState.RESPONSE_NOT_UNDERSTOOD;
        }
        this.errorMessage = me.getAttribute("errorMessage");
        
        //now get items
        Item itemsCollection = xml.getItem(me, "records");
        
        if (itemsCollection.isEmpty()) {
            this.syncImportRecords = null;
        } else {
            this.syncImportRecords = new ArrayList<SyncImportRecord>();
            List<Item> serItems = xml.getItems(itemsCollection);
            for (int i = 0; i < serItems.size(); i++) {
                Item serItem = serItems.get(i);
                SyncImportRecord syncImportRecord = new SyncImportRecord();
                syncImportRecord.load(xml, serItem);
                this.syncImportRecords.add(syncImportRecord);
            }
        }

        Item syncTx = xml.getItem(me, "syncTransmission");
        if ( syncTx.isEmpty() ) {
            this.syncTransmission = null;
        } else {
            this.syncTransmission = new SyncTransmission();
            this.syncTransmission.load(xml, syncTx);
        }
    }

	public SyncTransmissionState getState() {
    	return state;
    }

	public void setState(SyncTransmissionState state) {
    	this.state = state;
    }
}