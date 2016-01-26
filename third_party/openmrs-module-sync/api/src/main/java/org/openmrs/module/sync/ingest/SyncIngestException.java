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

import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncException;
import org.openmrs.module.sync.ingest.SyncImportRecord;

public class SyncIngestException extends SyncException {

	private static final long serialVersionUID = -4034873434558271005L;

	String syncItemContent;
	String itemError;
	String itemErrorArgs;
	SyncImportRecord syncImportRecord;

	public SyncIngestException(Throwable t, String errorMessage, String errorMessageArgs, String syncItemContent, SyncImportRecord sir) {
		super(internationalize(errorMessage, errorMessageArgs), t);
		this.setItemError(errorMessage);
		this.setItemErrorArgs(errorMessageArgs);
		this.setSyncItemContent(syncItemContent);
		this.setSyncImportRecord(sir);
	}	
	public SyncIngestException(String errorMessage, String errorMessageArgs, String syncItemContent, SyncImportRecord sir) {
		this(null, errorMessage, errorMessageArgs, syncItemContent, sir);
		if (sir != null)
			sir.setErrorMessage(errorMessage);
	}
	
	/**
	 * Convenience method to get get error message as code from a spring message file
	 * 
	 * @param message
	 * @param errorMessageArgs
	 * @return a translated string or the same string if no code exists by that name
	 */
	public static String internationalize(String message, String errorMessageArgs) {
		String[] args = null;
		if (errorMessageArgs != null)
			args = errorMessageArgs.split(",");
		
		return Context.getMessageSourceService().getMessage(message, args, Context.getLocale());
	}

	public String getSyncItemContent() {
		return syncItemContent;
	}

	public void setSyncItemContent(String syncItemContent) {
		this.syncItemContent = syncItemContent;
	}

	public String getItemErrorArgs() {
    	return itemErrorArgs;
    }

	public void setItemErrorArgs(String itemErrorArgs) {
    	this.itemErrorArgs = itemErrorArgs;
    }

	public String getItemError() {
    	return itemError;
    }

	public void setItemError(String itemError) {
    	this.itemError = itemError;
    }

	public SyncImportRecord getSyncImportRecord() {
		return syncImportRecord;
	}

	public void setSyncImportRecord(SyncImportRecord value) {
		this.syncImportRecord = value;
	}
	
}
