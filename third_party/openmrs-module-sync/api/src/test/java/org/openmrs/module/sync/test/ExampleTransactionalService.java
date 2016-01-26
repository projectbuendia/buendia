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
package org.openmrs.module.sync.test;

import org.openmrs.Encounter;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;

/**
 *  Example service to test out various transactional use cases of hibernate and spring
 */
public interface ExampleTransactionalService extends OpenmrsService {

	/**
	 * Example read-only transaction
	 */
	<T extends OpenmrsObject> T getObject(Class<T> type, Integer id);

	/**
	 * Example save without transaction annotation
	 */
	void saveObjectNoTransaction(OpenmrsObject openmrsObject);

	/**
	 * Example single transaction
	 */
	void saveObjectInTransaction(OpenmrsObject openmrsObject);

	/**
	 * Example single transaction that throws an Exception
	 */
	void saveObjectInTransactionWithException(OpenmrsObject openmrsObject);

	/**
	 * Example single transaction with REQUIRES_NEW
	 */
	void saveObjectInNewTransaction(OpenmrsObject openmrsObject);

	/**
	 * Example nested transaction
	 */
	void saveAllObjectsInSingleTransaction(OpenmrsObject... objects);

	/**
	 * Example nested transaction
	 */
	void saveAllObjectsInNewTransactions(OpenmrsObject... objects);
}
