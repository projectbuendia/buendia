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
 * State for SyncRecords. Has to be in a separate "class"/file due to Hibernate issues with restoring the objects
 */
public enum SyncRecordState {
    
    /** initial state of a sync record */
    NEW, 
    
    /** sync record is being sent to target sync source however it's transmission to the target sync source has not been confirmed */
    PENDING_SEND, 
    
    /** the record has been successful transmitted to the target source, note it may not yet be committed */
    SENT,
    
    /** attempted send failed */
    SEND_FAILED,
    
    /** the record was successfully committed at target source (the source server hasn't been notified yet) */
    COMMITTED, 
    
    /** This record has been committed and the source server has been notified of this **/
    COMMITTED_AND_CONFIRMATION_SENT,
    
    /** the record reached the failed state during ingest: will retry next time */
    FAILED, 

    /** the record reached the final failed state: max retry attempt was reached, no more retries will be attempted */
    FAILED_AND_STOPPED, 

    
    /** we are trying again to send this record */
    SENT_AGAIN, 
    
    /** this record has already been committed */
    ALREADY_COMMITTED,
    
    /** this record is set not to sync with the referenced server */
    NOT_SUPPOSED_TO_SYNC, 
    
    /** record was sent to server, but server does not accept this type of record for sync'ing */
    REJECTED;
    
    /**
     * Determines if a record state if final
     * @return
     */
    public boolean isFinal() {
    	return this == COMMITTED || this == ALREADY_COMMITTED || this == COMMITTED_AND_CONFIRMATION_SENT;
    }

	/**
	 * @return the value of isFinal, for use when getter is needed
	 */
	public boolean getFinal() {
		return isFinal();
	}
    
};
