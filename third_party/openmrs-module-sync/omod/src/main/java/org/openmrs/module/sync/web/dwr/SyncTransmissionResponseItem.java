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
package org.openmrs.module.sync.web.dwr;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncTransmissionState;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.ingest.SyncTransmissionResponse;
import org.openmrs.module.sync.server.ServerConnectionState;

/**
 *
 */
public class SyncTransmissionResponseItem {
    private String fileName = null;
    private Vector<SyncImportRecordItem> syncImportRecords;
    private String uuid;
    private String transmissionState;
    private String errorMessage;

    protected final Log log = LogFactory.getLog(getClass());

    public SyncTransmissionResponseItem() {}
    
    public SyncTransmissionResponseItem(SyncTransmissionResponse transmissionResponse) {
    	if ( transmissionResponse != null ) {
    		this.fileName = transmissionResponse.getFileName();
    		if ( transmissionResponse.getSyncImportRecords() != null ) {
    			this.syncImportRecords = new Vector<SyncImportRecordItem>();
    			for ( SyncImportRecord record : transmissionResponse.getSyncImportRecords() ) {
        			log.debug("Pulling record from SyncTransmissionResponse to DWR item: " + record);
    				// constructor for SyncImportRecordItem is null-safe
    				this.syncImportRecords.add(new SyncImportRecordItem(record));
    			}
    		} else {
    			log.debug("No import records to pull from SyncTransmissionResponse to DWR item");
    		}
    		this.uuid = transmissionResponse.getUuid();
    		if ( transmissionResponse.getState() != null ) this.transmissionState = transmissionResponse.getState().toString();
    		else this.transmissionState = SyncTransmissionState.FAILED.toString();
    		this.errorMessage = transmissionResponse.getErrorMessage();
    	} else {
    		this.fileName = SyncConstants.FILENAME_NO_CONNECTION;
    		this.uuid = SyncConstants.UUID_UNKNOWN;
    		this.transmissionState = ServerConnectionState.CONNECTION_FAILED.toString();
    		this.errorMessage = SyncConstants.ERROR_NO_CONNECTION;
    	}
    }
    
    public String getErrorMessage() {
    	return errorMessage;
    }
	
    public void setErrorMessage(String errorMessage) {
    	this.errorMessage = errorMessage;
    }
	
    public String getFileName() {
    	return fileName;
    }
	
    public void setFileName(String fileName) {
    	this.fileName = fileName;
    }

    public String getUuid() {
    	return uuid;
    }
	
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
	
    public Vector<SyncImportRecordItem> getSyncImportRecords() {
    	return syncImportRecords;
    }
	
    public void setSyncImportRecords(Vector<SyncImportRecordItem> syncImportRecords) {
    	this.syncImportRecords = syncImportRecords;
    }
	
    public String getTransmissionState() {
    	return transmissionState;
    }
	
    public void setTransmissionState(String transmissionState) {
    	this.transmissionState = transmissionState;
    }
    
}
