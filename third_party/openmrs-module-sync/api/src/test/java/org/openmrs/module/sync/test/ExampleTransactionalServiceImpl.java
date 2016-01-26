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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *  Example service to test out various transactional use cases of hibernate and spring
 */
public class ExampleTransactionalServiceImpl extends BaseOpenmrsService implements ExampleTransactionalService {

	protected Log log = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;

	public Session session() {
		return sessionFactory.getCurrentSession();
	}

	/**
	 * Example read-only transaction
	 */
	@Transactional(readOnly = true)
	public <T extends OpenmrsObject> T getObject(Class<T> type, Integer id) {
		return (T)session().get(type, id);
	}

	/**
	 * Example save without transaction annotation
	 */
	public void saveObjectNoTransaction(OpenmrsObject openmrsObject) {
		session().save(openmrsObject);
	}

	/**
	 * Example single transaction
	 */
	@Transactional
	public void saveObjectInTransaction(OpenmrsObject openmrsObject) {
		session().save(openmrsObject);
	}

	/**
	 * Example single transaction that throws an Exception
	 */
	@Transactional
	public void saveObjectInTransactionWithException(OpenmrsObject openmrsObject) {
		throw new IllegalArgumentException("Test Exception");
	}

	/**
	 * Example single transaction
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveObjectInNewTransaction(OpenmrsObject openmrsObject) {
		session().save(openmrsObject);
	}

	/**
	 * Example nested transaction
	 */
	@Transactional
	public void saveAllObjectsInSingleTransaction(OpenmrsObject... objects) {
		for (OpenmrsObject o : objects) {
			Context.getService(ExampleTransactionalService.class).saveObjectInTransaction(o);
		}
	}

	/**
	 * Example nested transaction
	 */
	@Transactional
	public void saveAllObjectsInNewTransactions(OpenmrsObject... objects) {
		for (OpenmrsObject o : objects) {
			Context.getService(ExampleTransactionalService.class).saveObjectInNewTransaction(o);
		}
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}
