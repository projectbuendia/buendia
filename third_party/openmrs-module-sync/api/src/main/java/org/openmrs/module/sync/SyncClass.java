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

/**
 * A component of a {@link SyncServerClass} that stores the class not to sync
 * 
 */
public class SyncClass {
	
	private Integer syncClassId;
	
	private String name;
	
	private Boolean defaultSendTo = Boolean.TRUE;
	
	private Boolean defaultReceiveFrom = Boolean.TRUE;
	
	/**
	 * If true, this object is imported from other servers
	 * 
	 * @return true/false whether or not to import the class named in {@link #name}
	 */
	public Boolean getDefaultReceiveFrom() {
		return defaultReceiveFrom;
	}
	
	/**
	 * If true, this object is imported from other servers
	 * 
	 * @param defaultFrom true/false whether or not to import the class named in {@link #name}
	 */
	public void setDefaultReceiveFrom(Boolean defaultFrom) {
		this.defaultReceiveFrom = defaultFrom;
	}
	
	/**
	 * If true, this object is not exported to other servers
	 * 
	 * @return true/false whether or not to export the class named in {@link #name}
	 */
	public Boolean getDefaultSendTo() {
		return defaultSendTo;
	}
	
	/**
	 * If true, this object is not exported to other servers
	 * 
	 * @param defaultTo true/false whether or not to export the class named in {@link #name}
	 */
	public void setDefaultSendTo(Boolean defaultTo) {
		this.defaultSendTo = defaultTo;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Integer getSyncClassId() {
		return syncClassId;
	}
	
	public void setSyncClassId(Integer syncClassId) {
		this.syncClassId = syncClassId;
	}
	
}
