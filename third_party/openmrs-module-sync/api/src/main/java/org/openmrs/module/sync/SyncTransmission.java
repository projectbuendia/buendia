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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.sync.serialization.FilePackage;
import org.openmrs.module.sync.serialization.IItem;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.util.OpenmrsUtil;

/**
 * SyncTransmission a collection of sync records to be sent to the parent.
 */
public class SyncTransmission implements IItem {

    // fields
    private final Log log = LogFactory.getLog(getClass());
     
    private String fileName = null;
    private Date timestamp = null;
    private List<SyncRecord> syncRecords = null;
    private String uuid = null;
    private String fileOutput = "";
    private String syncSourceUuid = null; //this is UUID of a server where Tx is coming from
    private String syncTargetUuid = null; //this is UUID of server where Tx is headed TO
    private Boolean isRequestingTransmission = false;
    private Boolean isMaxRetryReached = false; 

    // constructor(s)
    public SyncTransmission() { }

    /* 
     * Create new SyncTransmission as a SyncTxRequest
     */
    public SyncTransmission(String sourceUuid, boolean isRequestingTransmission) {
        uuid = SyncUtil.generateUuid();        
        fileName = "sync_tx_" + SyncConstants.SYNC_FILENAME_MASK.format(new Date()) + "_request";
        this.syncSourceUuid  = sourceUuid;
        this.isRequestingTransmission = isRequestingTransmission;
    }

    /* 
     * Take passed in records and create a new sync_tx file
     */
    public SyncTransmission(String sourceUuid, List<SyncRecord> valRecords) {
    	init(sourceUuid,valRecords, null);
    }
    public SyncTransmission(String sourceUuid, List<SyncRecord> valRecords,String targetUuid ) {
    	init(sourceUuid,valRecords,targetUuid);
    }
    private void init(String sourceUuid, List<SyncRecord> valRecords,String targetUuid) {

        uuid = SyncUtil.generateUuid();        
        fileName = "sync_tx_" + SyncConstants.SYNC_FILENAME_MASK.format(new Date());
        this.syncRecords = valRecords;
        this.syncSourceUuid  = sourceUuid;
        this.syncTargetUuid  = targetUuid;
    }

    public Boolean getIsMaxRetryReached() {
        return isMaxRetryReached;
    }

    public void setIsMaxRetryReached(boolean value) {
        isMaxRetryReached = value;
    }

    public Boolean getIsRequestingTransmission() {
        return isRequestingTransmission;
    }

    public void setIsRequestingTransmission(Boolean isRequestingTransmission) {
        this.isRequestingTransmission = isRequestingTransmission;
    }

    public String getSyncSourceUuid() {
        return syncSourceUuid;
    }
    public void setSyncSourceUuid(String value) {
        syncSourceUuid = value;
    }
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
    
    public List<SyncRecord> getSyncRecords() {
        return syncRecords;
    }
    public void setSyncRecords(List<SyncRecord> value) {
        this.syncRecords = value;
    }

    /** Creates a new transmission from records: use org.openmrs.serial to make a file
     *  also, give option to write to a file or not.
     *  <p> When writeFile is true, files are created in 'record' dir under Application Data 
     *  (see openmrs documentation for more information about setting Application Data).
     *  Files are created using the following mask: sync_tx_yyyy_MM_dd_HH_mm_ss_S_request.xml
     *  
     *  @param writeFile if true, local file for this transmission will be created.  
     */
    public void create(boolean writeFile) {

        try {
            
            if (timestamp == null) this.timestamp = new Date(); //set timestamp of this export, if not already set
            
            FilePackage pkg = new FilePackage();
            Record xml = pkg.createRecordForWrite(this.getClass().getName());
            Item root = xml.getRootItem();

            //serialize
            this.save(xml,root);

            //now dump to file if needed
            File dir = OpenmrsUtil.getDirectoryInApplicationDataDirectory("sync");
            File record = new File(dir, "recrd");
            fileOutput = pkg.savePackage(new File(record, fileName), writeFile);

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
        if (syncSourceUuid != null) xml.setAttribute(me, "syncSourceUuid", syncSourceUuid);
        if (timestamp != null) xml.setAttribute(me, "timestamp", new TimestampNormalizer().toString(timestamp));
        if (this.isRequestingTransmission != null) xml.setAttribute(me, "isRequestingTransmission", this.isRequestingTransmission.toString());
        if (this.isMaxRetryReached != null) xml.setAttribute(me, "isMaxRetryReached", this.isMaxRetryReached.toString());

        if (syncTargetUuid != null) xml.setAttribute(me, "syncTargetUuid", syncTargetUuid);
        else xml.setAttribute(me, "syncTargetUuid", SyncConstants.UUID_UNKNOWN);
        
        //serialize Records list
        Item itemsCollection = xml.createItem(me, "records");
        
        if (syncRecords != null) {
            me.setAttribute("itemCount", Integer.toString(syncRecords.size()));
            Iterator<SyncRecord> iterator = syncRecords.iterator();
            while (iterator.hasNext()) {
                iterator.next().save(xml, itemsCollection);
            }
        };

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
        this.isRequestingTransmission = Boolean.valueOf(me.getAttribute("isRequestingTransmission"));
        this.isMaxRetryReached =  Boolean.valueOf(me.getAttribute("isMaxRetryReached"));

        if (me.getAttribute("timestamp") == null)
            this.timestamp = null;
        else
            this.timestamp = (Date)new TimestampNormalizer().fromString(Date.class,me.getAttribute("timestamp"));
        
        //now get items
        Item itemsCollection = xml.getItem(me, "records");
        
        if (itemsCollection.isEmpty()) {
            this.syncRecords = null;
        } else {
            this.syncRecords = new ArrayList<SyncRecord>();
            List<Item> serItems = xml.getItems(itemsCollection);
            for (int i = 0; i < serItems.size(); i++) {
                Item serItem = serItems.get(i);
                SyncRecord syncRecord = new SyncRecord();
                syncRecord.load(xml, serItem);
                syncRecords.add(syncRecord);
            }
        }

    }

    /** Two instances of SyncTransmission are equal if all properties are equal, including the SyncRecords list.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SyncTransmission) || o == null)
            return false;

        
        SyncTransmission oSync = (SyncTransmission) o;
        boolean same = ((oSync.getTimestamp() == null) ? (this.getTimestamp() == null) : oSync.getTimestamp().equals(this.getTimestamp()))
                && ((oSync.getUuid() == null) ? (this.getUuid() == null) : oSync.getUuid().equals(this.getUuid()))
                && ((oSync.getFileName() == null) ? (this.getFileName() == null) : oSync.getFileName().equals(this.getFileName()))
                && ((oSync.getFileOutput() == null) ? (this.getFileOutput() == null) : oSync.getFileOutput().equals(this.getFileOutput()))
                && ((oSync.getSyncSourceUuid() == null) ? (this.getSyncSourceUuid() == null) : oSync.getSyncSourceUuid().equals(this.getSyncSourceUuid()))
                && ((oSync.getSyncRecords() == null) ? (this.getSyncRecords() == null) : oSync.getSyncRecords().equals(this.getSyncRecords()));
        
        return same;
    }

    public String getSyncTargetUuid() {
        return syncTargetUuid;
    }

    public void setSyncTargetUuid(String syncTargetUuid) {
        this.syncTargetUuid = syncTargetUuid;
    }
    
}