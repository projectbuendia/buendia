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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.serialization.IItem;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;
import org.springframework.util.StringUtils;

/**
 * {@link SyncImportRecord}s are kept on this server for every transactional unit that comes into
 * the server. <br/>
 * <br/>
 * This class is created as soon as information arrives from another server. This table/class is the
 * first place sync looks to know whether something has already come in or not. <br/>
 * <br/> {@link SyncImportRecord#uuid} is the {@link SyncRecord#getUuid()} on the remote server for the
 * record. That remote server can be either a parent or child to this current server.
 */
public class SyncImportRecord implements Serializable, IItem {
	
	public static final long serialVersionUID = 0L;
	
	// Fields
	private Integer importId;
	
	private String uuid = null;
	
	private String creator = null;
	
	private String databaseVersion = null;
	
	private Date timestamp = null;
	
	private int retryCount;
	
	private SyncRecordState state = SyncRecordState.NEW;
	
	private String errorMessage;
	
	private List<SyncImportItem> items = null;
	
	private RemoteServer sourceServer;
	
	// convenience string describing the content of this import record
	private transient String description = null;
	
	// Constructors
	/** default constructor */
	public SyncImportRecord() {
	}
	
	public SyncImportRecord(SyncRecord record) {
		if (record != null) {
			// the uuid should be set to original uuid - this way all subsequent attempts to execute this change are matched to this import
			this.uuid = record.getOriginalUuid();
			this.creator = record.getCreator();
			this.databaseVersion = record.getDatabaseVersion();
			this.timestamp = record.getTimestamp();
			this.retryCount = record.getRetryCount();
			this.state = record.getState();
			
			if (StringUtils.hasLength(record.getContainedClasses())) {
				this.description = record.getContainedClasses().split(",")[0];
			}
		}
	}
	
	public Integer getImportId() {
		return importId;
	}
	
	public void setImportId(Integer importId) {
		this.importId = importId;
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
	
	//state
	public SyncRecordState getState() {
		return state;
	}
	
	public void setState(SyncRecordState state) {
		this.state = state;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SyncImportRecord) || o == null)
			return false;
		
		SyncImportRecord oSync = (SyncImportRecord) o;
		boolean same = ((oSync.getTimestamp() == null) ? (this.getTimestamp() == null) : oSync.getTimestamp().equals(
		    this.getTimestamp()))
		        && ((oSync.getUuid() == null) ? (this.getUuid() == null) : oSync.getUuid().equals(this.getUuid()))
		        && ((oSync.getState() == null) ? (this.getState() == null) : oSync.getState().equals(this.getState()))
		        && (oSync.getRetryCount() == this.getRetryCount());
		return same;
	}
	
	public Item save(Record xml, Item parent) throws Exception {
		Item me = xml.createItem(parent, this.getClass().getName());
		
		//serialize primitives
		xml.setAttribute(me, "uuid", this.uuid);
		xml.setAttribute(me, "retryCount", Integer.toString(this.retryCount));
		xml.setAttribute(me, "state", this.state.toString());
		
		if (timestamp != null) {
			xml.setAttribute(me, "timestamp", new TimestampNormalizer().toString(timestamp));
		}
		
		// serialize error message
		if (StringUtils.hasText(errorMessage)) {
			Item errorMessageItem = xml.createItem(me, "errorMessage");
			xml.createTextAsCDATA(errorMessageItem, errorMessage);
		}
		
		//serialize items list
		Item itemsCollection = xml.createItem(me, "items");
		if (this.items != null) {
			me.setAttribute("itemCount", Integer.toString(this.items.size()));
			for (SyncImportItem importItem : this.items) {
				importItem.save(xml, itemsCollection);
			}
		}
		
		return me;
	}
	
	public void load(Record xml, Item me) throws Exception {
		
		//deserialize primitives
		this.uuid = me.getAttribute("uuid");
		this.retryCount = Integer.parseInt(me.getAttribute("retryCount"));
		this.state = SyncRecordState.valueOf(me.getAttribute("state"));
		
		if (me.getAttribute("timestamp") == null)
			this.timestamp = null;
		else {
			this.timestamp = (Date) new TimestampNormalizer().fromString(Date.class, me.getAttribute("timestamp"));
		}
		
		Item errorMessageItem = xml.getItem(me, "errorMessage");
		if (errorMessageItem != null)
			this.errorMessage = errorMessageItem.getText();
		
		//now get items
		Item itemsCollection = xml.getItem(me, "items");
		
		if (itemsCollection.isEmpty()) {
			this.items = null;
		} else {
			this.items = new ArrayList<SyncImportItem>();
			List<Item> serItems = xml.getItems(itemsCollection);
			for (int i = 0; i < serItems.size(); i++) {
				Item serItem = serItems.get(i);
				SyncImportItem syncImportItem = new SyncImportItem();
				syncImportItem.load(xml, serItem);
				this.addItem(syncImportItem);
			}
		}
		
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "SyncRecord (uuid:" + this.uuid + ") - " + this.state;
	}
	
	public List<SyncImportItem> getItems() {
		return items;
	}
	
	public void setItems(List<SyncImportItem> items) {
		this.items = items;
	}
	
	public void addItem(SyncImportItem item) {
		if (this.items == null)
			this.items = new ArrayList<SyncImportItem>();
		this.items.add(item);
	}
	
	/**
	 * @return the contained type of this import or the first uuid if no description is set
	 */
	public String getDescription() {
		if (description != null)
			return description;
		else
			return uuid;
	}
	
	/**
	 * @return the sourceServer
	 */
	public RemoteServer getSourceServer() {
		return sourceServer;
	}
	
	/**
	 * @param sourceServer the sourceServer to set
	 */
	public void setSourceServer(RemoteServer sourceServer) {
		this.sourceServer = sourceServer;
	}
	
}
