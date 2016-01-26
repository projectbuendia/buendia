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

import org.openmrs.OpenmrsObject;

/**
 * Transient class used to hold an Openmrs Object and the sync item state associated with that
 * object. Used in the SyncIngestService.processSyncItem to keep track of all objects processed so
 * that any class-specified or state-specified pre-commit actions can be applied.
 */
public class SyncProcessedObject {
	
	OpenmrsObject object;
	
	SyncItemState state;
	
	public SyncProcessedObject() {
	}
	
	public SyncProcessedObject(OpenmrsObject object, SyncItemState state) {
		this.object = object;
		this.state = state;
	}
	
	public OpenmrsObject getObject() {
		return object;
	}
	
	public void setObject(OpenmrsObject object) {
		this.object = object;
	}
	
	public SyncItemState getState() {
		return state;
	}
	
	public void setState(SyncItemState state) {
		this.state = state;
	}
	
}
