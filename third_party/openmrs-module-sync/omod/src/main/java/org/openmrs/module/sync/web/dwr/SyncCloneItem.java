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

import org.openmrs.module.sync.server.ServerConnectionState;

/**
 *
 */
public class SyncCloneItem {
	private String connectionState;
	private String errorMessage;
	private String responsefileName;

	/**
	 * @param connectionState
	 * @param errorMessage
	 * @param responsefileName
	 */
	public SyncCloneItem(String connectionState, String errorMessage,
	        String responsefileName) {
		super();
		this.connectionState = connectionState;
		this.errorMessage = errorMessage;
		this.responsefileName = responsefileName;
	}

	/**
     * 
     */
	public SyncCloneItem() {
		super();
		this.connectionState = ServerConnectionState.NO_ADDRESS.toString();
		this.errorMessage = "";
		this.responsefileName = "";
	}

	/**
	 * @return the connectionState
	 */
	public String getConnectionState() {
		return connectionState;
	}

	/**
	 * @param connectionState the connectionState to set
	 */
	public void setConnectionState(String connectionState) {
		this.connectionState = connectionState;
	}

	/**
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * @return the responsefileName
	 */
	public String getResponsefileName() {
		return responsefileName;
	}

	/**
	 * @param responsefileName the responsefileName to set
	 */
	public void setResponsefileName(String responsefileName) {
		this.responsefileName = responsefileName;
	}

}
