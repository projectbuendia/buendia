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

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmrs.module.sync.serialization.IItem;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.module.sync.server.SyncServerRecord;

/**
 * SyncRecord is a collection of sync items that represents a smallest transactional unit.
 * In other words, all sync items within a record must be:
 * - transfered from/to sync source 
 * - committed/rolled back together
 * 
 * Information about sync records -- what was sent, received should be stored in DB by each
 * sync source. Minimally, each source should keep track of history of sync records that were
 * sent 'up' to parent. 
 * 
 * Consequently a sync 'transmission' is nothing more than a transport of a set of sync records from 
 * source A to source B.
 * 
 */
public class SyncRecord implements Serializable, IItem {

    public static final long serialVersionUID = 0L;

    // Fields
    private Integer recordId;
    private String uuid = null;
    private String creator = null;
    private String databaseVersion = null;
    private Date timestamp = null;
    private int retryCount;
    private SyncRecordState state = SyncRecordState.NEW;
    private LinkedHashMap<String, SyncItem> items = null;
    private String containedClasses = "";
    private Set<SyncServerRecord> serverRecords = null;
    private RemoteServer forServer = null;
    private String originalUuid = null;

    public String getOriginalUuid() {
        return originalUuid;
    }

    public void setOriginalUuid(String originalUuid) {
        this.originalUuid = originalUuid;
    }

    // Constructors
    /** default constructor */
    public SyncRecord() {
    }

    public String getContainedClasses() {
        return containedClasses;
    }

    public void setContainedClasses(String containedClasses) {
        if ( containedClasses != null ) {
            String[] splits = containedClasses.split(",");
            for ( String split : splits ) {
                this.addContainedClass(split);
            }
        } else {
            this.containedClasses = containedClasses;
        }
    }

    public Integer getRecordId() {
    	return recordId;
    }

	public void setRecordId(Integer recordId) {
    	this.recordId = recordId;
    }

	// Properties
    // globally unique id of the record
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    // The uuid of the creator of the record
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
    
    // The database version used when creating this record
    public String getDatabaseVersion() {
        return databaseVersion;
    }

    public void setDatabaseVersion(String databaseVersion) {
        this.databaseVersion = databaseVersion;
    }
    
    // timestamp of last operation
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    // retry count
    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public void incrementRetryCount() {
    	this.retryCount++;
    }

    //state
    public SyncRecordState getState() {
        return state;
    }

    public void setState(SyncRecordState state) {
        this.state = state;
    }

    //list of sync items
    public Collection<SyncItem> getItems() {
        if (items == null) return null;
                
        return items.values();
    }

    public void addItem(SyncItem syncItem) {
        if (items == null) {
            items = new LinkedHashMap<String,SyncItem>();
        }
        
        items.put(SyncRecord.deriveMapKey(syncItem),syncItem);
    }

    /**
     * If there is already an item with same key, replace it with passed in value, else add it.
     * It will be added as LAST in insert order.
     * 
     * Note: internally key for the LinkedHashMap is uuid + action + contained type
     * 
     * @param syncItem
     */
    public void addOrRemoveAndAddItem(SyncItem syncItem) {
    	if (syncItem == null) {
    		return;
    	};
    	
    	String itemMapKey = SyncRecord.deriveMapKey(syncItem);
    	if (items == null) {
            items = new LinkedHashMap<String,SyncItem>();
        } else {
        	if (items.containsKey(itemMapKey)) {
    			items.remove(itemMapKey);
        	}
        }
        
        //now add it
        this.addItem(syncItem);     
    }

    public void setItems(Collection<SyncItem> newItems) {
    	if(newItems == null) return;
    	items = new LinkedHashMap<String,SyncItem>();
    	for(SyncItem newItem : newItems) {
    		this.addItem(newItem);
    	}
   }

    public boolean hasItems() {
    	if (items == null) return false;
    	if (items.size() > 0) 
    		return true;
    	else
    		return false;
   }
	
	/**
	 * Checks if the sync record contains an item with its key equal to the one that is generated
	 * for the passed in SyncItem, ideally it checks if a sync item exists that matches the
	 * uuid+action+containedType combination
	 * 
	 * @param syncItem the sync item to match against
	 * @return true if the record contains the sync item otherwise false
	 */
	public boolean hasSyncItem(SyncItem syncItem) {
		if (syncItem != null && items != null) {
			return items.containsKey(SyncRecord.deriveMapKey(syncItem));
		}
		return false;
	}

    // Methods
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SyncRecord) || o == null)
            return false;
        
        
        SyncRecord oSync = (SyncRecord) o;
        
        boolean same = ((oSync.getTimestamp() == null) ? (this.getTimestamp() == null) : oSync.getTimestamp().equals(this.getTimestamp()))
                && ((oSync.getUuid() == null) ? (this.getUuid() == null) : oSync.getUuid().equals(this.getUuid()))
                && ((oSync.getState() == null) ? (this.getState() == null) : oSync.getState().equals(this.getState()))
                && (oSync.getRetryCount() == this.getRetryCount());
        
        //manually check linkedhashset
        Collection<SyncItem> oSyncItems = oSync.getItems();
        Collection<SyncItem> thisItems = this.getItems();
        if (oSyncItems == null || thisItems == null) {
        	same = same && (thisItems == null) && (oSyncItems == null);
        } else {
        	same = same && oSyncItems.containsAll(thisItems) && (oSyncItems.size() == thisItems.size());        	
        }
        
        return same;
    }


    public Item save(Record xml, Item parent) throws Exception {
        Item me = xml.createItem(parent, this.getClass().getSimpleName());
        
        //serialize primitives
        xml.setAttribute(me, "uuid", uuid);
        xml.setAttribute(me, "retryCount", Integer.toString(retryCount));
        xml.setAttribute(me, "containedClasses", this.containedClasses);
        if ( this.originalUuid != null ) {
            xml.setAttribute(me, "originalUuid", originalUuid);
        }
        xml.setAttribute(me, "uuid", uuid);

        if ( this.getForServer() != null ) {
            if ( !this.getForServer().getServerType().equals(RemoteServerType.PARENT)) {
                SyncServerRecord serverRecord = this.getServerRecord(this.getForServer());
                xml.setAttribute(me, "state", serverRecord.getState().toString());
                xml.setAttribute(me, "retryCount", Integer.toString(serverRecord.getRetryCount()));
            } else {
                xml.setAttribute(me, "state", state.toString());
                xml.setAttribute(me, "retryCount", Integer.toString(retryCount));
            }
        } else {
            xml.setAttribute(me, "state", state.toString());
            xml.setAttribute(me, "retryCount", Integer.toString(retryCount));
        }
        
        if (timestamp != null) {
        	xml.setAttribute(me, "timestamp", new TimestampNormalizer().toString(timestamp));
        }
        
        //serialize IItem children
        Item itemsCollection = xml.createItem(me, "items");
        if (items != null) {
        	for(SyncItem item : items.values()) {
        		item.save(xml, itemsCollection);
        	}
        };

        return me;
    }

    public void load(Record xml, Item me) throws Exception {
        
        //deserialize primitives
        this.uuid = me.getAttribute("uuid");
        this.retryCount = Integer.parseInt(me.getAttribute("retryCount"));
        this.state = SyncRecordState.valueOf(me.getAttribute("state"));
        this.containedClasses = me.getAttribute("containedClasses");
        
        if (me.getAttribute("timestamp") == null)
            this.timestamp = null;
        else {
            this.timestamp = (Date)new TimestampNormalizer().fromString(Date.class,me.getAttribute("timestamp"));
        }

        if (me.getAttribute("originalUuid") == null)
            this.originalUuid = null;
        else {
            this.originalUuid = me.getAttribute("originalUuid");
        }

        //now get items
        Item itemsCollection = xml.getItem(me, "items");
        
        if (itemsCollection.isEmpty()) {
            items = null;
        } else {
        	//re-create linked hashmap entries with appropriate keys
            items = new LinkedHashMap<String,SyncItem>();
            List<Item> serItems = xml.getItems(itemsCollection);
            for (int i = 0; i < serItems.size(); i++) {
                Item serItem = serItems.get(i);
                SyncItem syncItem = new SyncItem();
                syncItem.load(xml, serItem);
                items.put(SyncRecord.deriveMapKey(syncItem),syncItem);
            }
        }
    }

    public Set<String> getContainedClassSet() {
        Set<String> ret = new HashSet<String>();
        
        if ( this.containedClasses != null ) {
            String[] classes = this.containedClasses.split(",");
            for ( String clazz : classes ) {
                if ( !ret.contains(clazz) ) ret.add(clazz);
            }
        }
        
        return ret;
    }
    
    public void setContainedClassSet(Set<String> classes) {
        if ( classes != null ) {
            this.containedClasses = "";
            for ( String clazz : classes ) {
                clazz = clazz.trim();
                if ( clazz.length() > 0 ) {
                    if ( this.containedClasses.length() == 0 ) this.containedClasses = clazz;
                    else this.containedClasses += "," + clazz;
                }
            }
        }
    }
    
    /**
     * Auto generated method comment
     * 
     * @param simpleName
     */
    public void addContainedClass(String simpleName) {
        if ( simpleName != null && simpleName.length() > 0 ) {
            Set<String> classes = this.getContainedClassSet();
            if ( classes == null ) classes = new HashSet<String>();
            if ( !classes.contains(simpleName) ) classes.add(simpleName);
            this.setContainedClassSet(classes);
        }
    }

    public Set<SyncServerRecord> getServerRecords() {
        return serverRecords;
    }

    public void setServerRecords(Set<SyncServerRecord> serverRecords) {
        this.serverRecords = serverRecords;
    }

    public SyncServerRecord getServerRecord(RemoteServer server) {
        SyncServerRecord ret = null;
        
        if ( server != null && this.serverRecords != null ) {
            for ( SyncServerRecord record : this.serverRecords ) {
            	// changed to using server ids to avoid an NPE in file transfers
                if ( server.getServerId().equals(record.getSyncServer().getServerId())) {
                    ret = record;
                }
            }
        }
        
        return ret;
    }
    
    public RemoteServer getForServer() {
        return forServer;
    }

    public void setForServer(RemoteServer forServer) {
        this.forServer = forServer;
    }
    
    public Map<RemoteServer, SyncServerRecord> getRemoteRecords() {
    	Map<RemoteServer, SyncServerRecord> ret = new LinkedHashMap<RemoteServer, SyncServerRecord>();
    	
    	if ( this.serverRecords != null ) {
    		for ( SyncServerRecord serverRecord : this.serverRecords ) {
    			ret.put(serverRecord.getSyncServer(), serverRecord);
    		}
    	}
    	
    	return ret;
    }

    /**
     * Internally, sync items are stored as LinkedHashMap, the key into it is: uuid + action + contained type
     * 
     * @param item SyncItem for which to derive map key
     * @return string value of the key
     */
    private static String deriveMapKey(SyncItem item) {
        return item.getKey().getKeyValue().toString() 
        	+ item.getState().toString() 
        	+ ((item.getContainedType() == null) ? "null" : item.getContainedType().getName()); 
        // (was getSimpleName instead of just getName)
    	
    }
    
    public boolean isOutgoing() {
    	return getUuid().equals(getOriginalUuid());
    }
    
    @Override
    public String toString() {
    	return "SyncRecord(" + getRecordId() + ") contains " + getContainedClasses();
    }

	/**
	 * Adds a SyncServerRecord to this SyncRecord for the given <code>server</code>
	 * 
	 * @param server the RemoteServer to make this get sent to
	 */
	public void addServerRecord(RemoteServer server) {
		// only add this if there isn't one for this server already
		if (getServerRecord(server) == null)
			serverRecords.add(new SyncServerRecord(server, this));
	}
}
