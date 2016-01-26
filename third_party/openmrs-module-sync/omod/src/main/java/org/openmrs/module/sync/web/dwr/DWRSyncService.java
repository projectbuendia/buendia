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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncTransmissionState;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.SyncUtilTransmission;
import org.openmrs.module.sync.SyncUtilTransmission.ReceivingSize;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.ingest.SyncTransmissionResponse;
import org.openmrs.module.sync.serialization.ZipPackage;
import org.openmrs.module.sync.server.ConnectionResponse;
import org.openmrs.module.sync.server.ServerConnection;
import org.openmrs.module.sync.server.ServerConnectionState;

/**
 * DWR methods used by the sync module
 */
public class DWRSyncService {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Used when doing a full synchronize to get the number of objects the parent server is sending
	 * down to us before the actual update is done
	 */
	private static ReceivingSize receivingSize = new ReceivingSize();
	
	public SyncCloneItem cloneParentDB(String address, String username, String password) {
		SyncCloneItem item = new SyncCloneItem();
		if (address != null && address.length() > 0) {
			File dir = SyncUtil.getSyncApplicationDir();
			File file = new File(dir, SyncConstants.CLONE_IMPORT_FILE_NAME
			        + SyncConstants.SYNC_FILENAME_MASK.format(new Date()) + ".sql");
			ConnectionResponse connResponse = ServerConnection.cloneParentDB(address, username, password);
			item.setConnectionState(connResponse.getState().toString());
			item.setErrorMessage(connResponse.getErrorMessage());
			
			// execute the parent's sql file on our database
			if (ServerConnectionState.OK.equals(connResponse.getState())) {
				byte sql[] = connResponse.getResponsePayload().getBytes(Charset.forName("UTF-8"));
				try {
					IOUtils.write(sql, new FileOutputStream(file));
					
					Context.getService(SyncService.class).execGeneratedFile(file);
					item.setResponsefileName(file.getName());
					item.setErrorMessage("Parent data cloned successfully");
					
					boolean clonedDBLog = Boolean.parseBoolean(Context.getAdministrationService()
							.getGlobalProperty(SyncConstants.PROPERTY_SYNC_CLONED_DATABASE_LOG_ENABLED, "true"));
					
					if (!clonedDBLog){
						file.delete();
					}
				}
				catch (FileNotFoundException e) {
					item.setErrorMessage("Unable to save file(" + file.getAbsolutePath() + ")");
					log.error("Unable to save file(" + file.getAbsolutePath() + ") : Error generated", e);
				}
				catch (IOException e) {
					item.setErrorMessage("Unable to save file(" + file.getAbsolutePath() + ")");
					log.error("Unable to save file(" + file.getAbsolutePath() + ") : Error generated", e);
				}
			}
		}
		
		return item;
	}
	
	/**
	 * Pings the given server with the given username/password to make sure the settings are correct
	 * 
	 * @param address url to ping
	 * @param username username with which to log in
	 * @param password password with which to log in
	 * @return SyncConnectionTestItem that contains success or failure
	 */
	public SyncConnectionTestItem testConnection(String address, String username, String password) {
		SyncConnectionTestItem item = new SyncConnectionTestItem();
		item.setConnectionState(ServerConnectionState.NO_ADDRESS.toString());
		
		if (address != null && address.length() > 0) {
			ConnectionResponse connResponse = ServerConnection.test(address, username, password);
			
			// constructor for SyncTransmissionResponse is null-safe
			SyncTransmissionResponse str = new SyncTransmissionResponse(connResponse);
			
			// constructor for SyncConnectionTestItem is null-safe
			item = new SyncConnectionTestItem(str);
		}
		
		return item;
	}
	
	/**
	 * Call this method after initiating {@link #syncToParent()} to get the number of objects that
	 * the parent is sending to us. This value is updated mid-method for display to the end user.
	 * 
	 * @return integer number of records being sent to us (or null if none)
	 */
	public Integer getNumberOfObjectsBeingReceived() {
		return receivingSize.getSize();
	}
	
	/**
	 * Used by the status.list page to send data to the parent and show the results. <br/>
	 * Use #getNumberOfObjectsBeingReceived() before this method is done to know how many objects
	 * the parent is sending to our server.
	 * 
	 * @return results of the transmission
	 */
	public SyncTransmissionResponseItem syncToParent() {
		// the doFullSync method updates this 'objectsBeingReceived' so that the
		// jsp page can know what we're dealing with
		// before the whole SyncTransmissionsResponse is returned
		SyncTransmissionResponse response = SyncUtilTransmission.doFullSynchronize(receivingSize,
		    SyncUtil.getGlobalPropetyValueAsInteger(SyncConstants.PROPERTY_NAME_MAX_RECORDS_WEB));
		
		receivingSize.setSize(null); // reset variable
		
		if (response != null) {
			return new SyncTransmissionResponseItem(response);
		} else {
			SyncTransmissionResponseItem transmissionResponse = new SyncTransmissionResponseItem();
			transmissionResponse.setErrorMessage(SyncConstants.ERROR_SEND_FAILED.toString());
			transmissionResponse.setFileName(SyncConstants.FILENAME_SEND_FAILED);
			transmissionResponse.setUuid(SyncConstants.UUID_UNKNOWN);
			transmissionResponse.setTransmissionState(SyncTransmissionState.FAILED.toString());
			return transmissionResponse;
		}
		
	}
	
	public String getSyncItemContent(String guid, String key) {
		String content = "";
        StringBuilder contentBuilder = new StringBuilder();
		Collection<SyncItem> itemList;
		if (guid != null && guid != "" && key != null && key != "") {
            SyncRecord syncRecord = Context.getService(SyncService.class).getSyncRecord(guid);
            if (syncRecord != null ) {
                itemList = syncRecord.getItems();
                for (SyncItem item : itemList) {
                    if (item.getKey().getKeyValue().equals(key))
                        content = item.getContent();
                }
            }
            /** building an ad-hoc xml document that contains the items and the payload, eg:
             <syncRecord>
                  <org.openmrs.PatientIdentifier>...</org.openmrs.PatientIdentifier>
                      <payload>
                            <items><SyncItem containedType...
                            </items></>
                      </payload>
             </syncRecord>
            **/
            if (StringUtils.isNotBlank(content)) {
                contentBuilder.append("<syncRecord>").append(content);
                contentBuilder.append("<payload>").append(SyncUtil.getSyncRecordPayload(syncRecord)).append("</payload>");
                contentBuilder.append("</syncRecord>");
                content = contentBuilder.toString();
            }
		}

        return content;
	}

    public String setSyncRecordPayload(String guid, String key, String payload) {
        String ret = "Error: Not saved";

        if (guid != null && guid != "" && key != null && key != "") {
            SyncRecord record = Context.getService(SyncService.class).getSyncRecord(guid);
            if (StringUtils.isNotBlank(payload)) {
                record.setItems(SyncUtil.getSyncItemsFromPayload(payload));
            }
            Context.getService(SyncService.class).updateSyncRecord(record);
            ret = "Item payload saved";
        }
        return ret;
    }
    public String setSyncItemContent(String guid, String key, String content) {
		String ret = "Error: Not saved";
		Collection<SyncItem> itemList;
		if (guid != null && guid != "" && key != null && key != "") {
			itemList = Context.getService(SyncService.class).getSyncRecord(guid).getItems();
			for (SyncItem item : itemList) {
				if (item.getKey().getKeyValue().equals(key))
					item.setContent(content);
			}
			SyncRecord record = Context.getService(SyncService.class).getSyncRecord(guid);
			record.setItems(itemList);
			Context.getService(SyncService.class).updateSyncRecord(record);
			ret = "Item payload saved";
		}
		return ret;
	}
	
	public boolean archiveSyncJournal(boolean clearDir) {
		return new ZipPackage(org.openmrs.util.OpenmrsUtil.getDirectoryInApplicationDataDirectory("sync"), "journal")
		        .zip(clearDir);
	}
	
	public boolean archiveSyncImport(boolean clearDir) {
		return new ZipPackage(org.openmrs.util.OpenmrsUtil.getDirectoryInApplicationDataDirectory("sync"), "import")
		        .zip(clearDir);
	}
	
}
