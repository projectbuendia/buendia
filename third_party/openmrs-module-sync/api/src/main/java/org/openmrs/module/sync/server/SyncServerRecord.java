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
package org.openmrs.module.sync.server;

import java.io.Serializable;

import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.ingest.SyncImportRecord;

/**
 * One {@link SyncServerRecord} is kept for each child server for each {@link SyncRecord}. <br/>
 * <br/>
 * A {@link SyncServerRecord} is created for each known child server as soon as a {@link SyncRecord}
 * is created. <br/>
 * <br/>
 * A SyncServerRecord will be PENDING_SEND until a transaction is started. At which point the status
 * will be SENT until the remote server sends back a confirmation that it was successfully applied
 * to the database. At that point the status of the SyncServerRecord is set to COMMITTED and this
 * record will not be sent to the child server any longer. <br/>
 * <br/>
 * A SyncRecord contains a list of all known SyncServerRecords.
 * 
 * @see SyncRecord
 */
public class SyncServerRecord implements Serializable {
	
	public static final long serialVersionUID = 1L;
	
	// Fields
	private Integer serverRecordId;
	
	private RemoteServer syncServer;
	
	private SyncRecord syncRecord;
	
	private SyncRecordState state = SyncRecordState.NEW;
	
	private int retryCount = 0;
	
	private String errorMessage;
	
	// Constructors
	/** default constructor */
	public SyncServerRecord() {
	}
	
	/**
	 * @param server
	 * @param record
	 */
	public SyncServerRecord(RemoteServer server, SyncRecord record) {
		this.syncServer = server;
		this.syncRecord = record;
	}
	
	//state
	public SyncRecordState getState() {
		return state;
	}
	
	public void setState(SyncRecordState state) {
		this.state = state;
	}
	
	public int getRetryCount() {
		return retryCount;
	}
	
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	
	public void incrementRetryCount() {
		this.retryCount++;
	}
	
	public Integer getServerRecordId() {
		return serverRecordId;
	}
	
	public void setServerRecordId(Integer serverRecordId) {
		this.serverRecordId = serverRecordId;
	}
	
	public SyncRecord getSyncRecord() {
		return syncRecord;
	}
	
	public void setSyncRecord(SyncRecord syncRecord) {
		this.syncRecord = syncRecord;
	}
	
	public RemoteServer getSyncServer() {
		return syncServer;
	}
	
	public void setSyncServer(RemoteServer syncServer) {
		this.syncServer = syncServer;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + retryCount;
		result = PRIME * result + ((serverRecordId == null) ? 0 : serverRecordId.hashCode());
		result = PRIME * result + ((state == null) ? 0 : state.hashCode());
		result = PRIME * result + ((syncRecord == null) ? 0 : syncRecord.hashCode());
		result = PRIME * result + ((syncServer == null) ? 0 : syncServer.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SyncServerRecord other = (SyncServerRecord) obj;
		if (retryCount != other.retryCount)
			return false;
		if (serverRecordId == null) {
			if (other.serverRecordId != null)
				return false;
		} else if (!serverRecordId.equals(other.serverRecordId))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (syncRecord == null) {
			if (other.syncRecord != null)
				return false;
		} else if (!syncRecord.equals(other.syncRecord))
			return false;
		if (syncServer == null) {
			if (other.syncServer != null)
				return false;
		} else if (!syncServer.equals(other.syncServer))
			return false;
		return true;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public boolean isOutgoing() {
		if (getSyncRecord().isOutgoing()) {
			return true;
		} else {
			SyncService service = Context.getService(SyncService.class);
			SyncImportRecord syncImportRecord = service.getSyncImportRecord(getSyncRecord().getOriginalUuid());
			return (!syncServer.equals(syncImportRecord.getSourceServer()));
		}
	}
}
