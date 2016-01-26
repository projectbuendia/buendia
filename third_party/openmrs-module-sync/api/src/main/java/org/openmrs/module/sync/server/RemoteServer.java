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

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncServerClass;
import org.openmrs.module.sync.SyncTransmissionState;
import org.openmrs.util.OpenmrsUtil;

/**
 * Represents another server that we are going to sync to/from.  
 */
public class RemoteServer {
	
	private Integer serverId;
	
	private String nickname;
	
	private String address;
	
	private RemoteServerType serverType;
	
	private String username;
	
	private String password;
	
	private Date lastSync;
	
	private SyncTransmissionState lastSyncState;
	
	private Set<SyncServerClass> serverClasses;
	
	private Set<SyncServerRecord> serverRecords;
	
	private String uuid;
	
	private Boolean disabled = false;
	
	private String childUsername = null;
	
	private static Map<Integer, Date> syncServersInProgress = new LinkedHashMap<Integer, Date>();
	
	public Boolean getDisabled() {
		return disabled;
	}
	
	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public Set<SyncServerClass> getServerClasses() {
		return serverClasses;
	}
	
	public void setServerClasses(Set<SyncServerClass> serverClasses) {
		this.serverClasses = serverClasses;
	}
	
	public Date getLastSync() {
		return lastSync;
	}
	
	public void setLastSync(Date lastSync) {
		this.lastSync = lastSync;
	}
	
	public SyncTransmissionState getLastSyncState() {
		return lastSyncState;
	}
	
	public void setLastSyncState(SyncTransmissionState value) {
		this.lastSyncState = value;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public Boolean getIsSSL() {
		return this.address.startsWith("https");
	}
	
	public String getNickname() {
		return nickname;
	}
	
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public Integer getServerId() {
		return serverId;
	}
	
	public void setServerId(Integer serverId) {
		this.serverId = serverId;
	}
	
	public RemoteServerType getServerType() {
		return serverType;
	}
	
	public void setServerType(RemoteServerType serverType) {
		this.serverType = serverType;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
		
	public Set<String> getClassesNotSent() {
		Set<String> ret = new HashSet<String>();
		
		if (this.serverClasses != null) {
			for (SyncServerClass serverClass : this.serverClasses) {
				if (serverClass.getSendTo() == false)
					ret.add(serverClass.getSyncClass().getName());
			}
		}
		
		return ret;
	}
	

	public Set<String> getClassesNotReceived() {
		Set<String> ret = new HashSet<String>();
		
		if (this.serverClasses != null) {
			for (SyncServerClass serverClass : this.serverClasses) {
				if (serverClass.getReceiveFrom() == false)
					ret.add(serverClass.getSyncClass().getName());
			}
		}
		
		return ret;
	}
	
	/**
	 * Find out if a given sync record should be processed (i.e. ingested) by the provided server
	 * based on the contained types. Note, method can be called both on parent and child; i.e on
	 * child it is called while deciding what to send to the parent server (in this case object
	 * instance of the RemoteServer is 'parent').
	 * 
	 * Remarks: The naming of methods shouldReceive() and shouldSend() is from the standpoint of
	 * the server. 
	 * 
	 * @return
	 */
	public Boolean shouldBeSentSyncRecord(SyncRecord record) {
		Boolean ret = true; //assume it is good less we find match
		
		if (record == null)
			return false;
		
		StringBuffer recordTypesStrings = new StringBuffer();
		Set<String> recordTypes = record.getContainedClassSet();
		if (recordTypes == null)
			return ret;
		if (this.serverClasses == null)
			return ret;
		
		//build up the search string of types
		for (String type : recordTypes) {
			recordTypesStrings.append("<");
			recordTypesStrings.append(type);
			recordTypesStrings.append(">");
		}
		
		//now do the comparison, note these can have wild cards
		for (SyncServerClass serverClass : this.serverClasses) {
			if (serverClass.getSendTo() == false) {
				String typeToTest = serverClass.getSyncClass().getName();
				if (Pattern.matches(".*<" + typeToTest + ".*>*", recordTypesStrings)) {
					ret = false;
					break;
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Find out if given sync record should be processed (i.e. ingested) by this server based on the
	 * contained types.
	 * 
	 * Remarks: See shouldBeSentSyncRecord() for more background on how this works.
	 * 
	 * @return
	 */
	public Boolean shouldReceiveSyncRecordFrom(SyncRecord record) {
		Boolean ret = true; //assume it is good less we find match
		
		if (record == null)
			return false;
		
		StringBuffer recordTypesStrings = new StringBuffer();
		Set<String> recordTypes = record.getContainedClassSet();
		if (recordTypes == null)
			return ret;
		if (this.serverClasses == null)
			return ret;
		
		//build up the search string of types
		for (String type : recordTypes) {
			recordTypesStrings.append("<");
			recordTypesStrings.append(type);
			recordTypesStrings.append(">");
		}
		
		//now do the comparison, note these can be package names too thus do the .* Pattern
		for (SyncServerClass serverClass : this.serverClasses) {
			if (serverClass.getReceiveFrom() == false) {
				String typeToExclude = serverClass.getSyncClass().getName();
				if (Pattern.matches(".*<" + typeToExclude + ".*>*", recordTypesStrings)) {
					ret = false;
					break;
				}
			}
		}
		
		return ret;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((address == null) ? 0 : address.hashCode());
		result = PRIME * result + ((uuid == null) ? 0 : uuid.hashCode());
		result = PRIME * result + ((lastSync == null) ? 0 : lastSync.hashCode());
		result = PRIME * result + ((nickname == null) ? 0 : nickname.hashCode());
		result = PRIME * result + ((password == null) ? 0 : password.hashCode());
		result = PRIME * result + ((serverClasses == null) ? 0 : serverClasses.hashCode());
		result = PRIME * result + ((serverId == null) ? 0 : serverId.hashCode());
		result = PRIME * result + ((serverType == null) ? 0 : serverType.hashCode());
		result = PRIME * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof RemoteServer))
			return false;
		final RemoteServer other = (RemoteServer) obj;
		
		if (OpenmrsUtil.nullSafeEquals(uuid, other.uuid))
			return true;
		
		if (OpenmrsUtil.nullSafeEquals(serverId, other.serverId))
			return true;
		
		// both the uuid and id are not equal, return false
		return false;
	}
	
	public String getChildUsername() {
		return childUsername;
	}
	
	public void setChildUsername(String childUsername) {
		this.childUsername = childUsername;
	}
	
	public Set<SyncServerRecord> getServerRecords() {
		return serverRecords;
	}
	
	public void setServerRecords(Set<SyncServerRecord> serverRecords) {
		this.serverRecords = serverRecords;
	}
	
    public Boolean getSyncInProgress() {
    	synchronized (syncServersInProgress) {
	    	if (getServerId() == null)
	    		return false;
	    	else
	    		return syncServersInProgress.get(getServerId()) != null;
    	}
    }

    /**
     * If given true, marks this current server as 'in progress' (static variable not to be saved in the database)
     * 
     * @param syncInProgress
     */
    public synchronized void setSyncInProgress(Boolean syncInProgress) {
    	synchronized (syncServersInProgress) {
	    	if (syncInProgress) {
	    		syncServersInProgress.put(getServerId(), new Date());
	    	}
	    	else {
	    		syncServersInProgress.remove(getServerId());
	    	}
    	}
    }
    
    private static DecimalFormat df = new DecimalFormat("0.00");
	
	/**
	 * @return the number of minutes since the sync was started. If this is new server or sync is
	 *         not in progress the empty string is returned
	 */
	public String getSyncInProgressMinutes() {
		synchronized (syncServersInProgress) {
	    	if (getServerId() == null || syncServersInProgress.get(getServerId()) == null)
	    		return "";
	    	
	    	Long difference = System.currentTimeMillis() - syncServersInProgress.get(getServerId()).getTime();
	    	
	    	return df.format((float)(difference) / 1000 / 60);
		}
    }

	@Override
	public String toString() {
		return "RemoteServer(" + getServerId() + "): " + getNickname();
	}
}
