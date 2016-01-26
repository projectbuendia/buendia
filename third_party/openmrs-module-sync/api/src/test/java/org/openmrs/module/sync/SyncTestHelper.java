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

import java.util.List;

import org.openmrs.module.sync.serialization.Record;

/**
 * This class is meant to be implemented inline by sync tests. The
 * {@link SyncBaseTest#runSyncTest(SyncTestHelper)} method calls the methods here to test
 * synchronization from a child, to a parent, and then to another child.
 */
public abstract class SyncTestHelper {
	
	/**
	 * This method should do the initial work of saving new changes to the child server
	 * 
	 * @throws Exception
	 */
	public abstract void runOnChild() throws Exception;
	
	/**
	 * This is called right before applying the sync records to the parent and right before apply
	 * the sync records to child2
	 * 
	 * @param syncRecords the sync records to apply
	 * @param record the serialized package of the given syncRecords
	 * @throws Exception
	 */
	public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
		// no default checks here
	}
	
	/**
	 * This method runs assertions to make sure the parent got all the changes.
	 * 
	 * @throws Exception
	 */
	public abstract void runOnParent() throws Exception;
	
	/**
	 * The assertions in this method are usually the same as the ones in the {@link #runOnParent()},
	 * so implementing this method is optional
	 * 
	 * @throws Exception
	 */
	public void runOnChild2() throws Exception {
		this.runOnParent();
	}
	
}
