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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.hibernate.AssertionFailure;
import org.junit.Test;
import org.openmrs.ConceptClass;
import org.openmrs.EncounterType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.annotation.NotTransactional;

public class SyncBehaviorTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * Hibernate's ActionQueue re-throws all exceptions that are not Subclasses of
	 * HibernateException as {@link AssertionFailure} exceptions
	 */
	@Test(expected = AssertionFailure.class)
	@NotTransactional
	public void shouldFailAnApiCallIfTheCreationOfASyncRecordFails() throws Exception {
		Advice advice = new FailSyncRecordAdvice();
		try {
			Context.addAdvice(SyncService.class, advice);
			EncounterService es = Context.getEncounterService();
			
			EncounterType encounterType = new EncounterType();
			String name = "Random name";
			encounterType.setName(name);
			encounterType.setDescription("description");
			es.saveEncounterType(encounterType);
		}
		finally {
			Context.removeAdvice(SyncService.class, advice);
		}
	}
	
	@Test
	@NotTransactional
	public void shouldNotCreateASyncRecordWhenTheTransactionIsRolledBack() throws Exception {
		ConceptService cs = Context.getConceptService();
		SyncService ss = Context.getService(SyncService.class);
		
		int initialSyncRecordCount = ss.getSyncRecords().size();
		boolean exceptionThrown = false;
		try {
			ConceptClass cc = cs.getConceptClass(1);
			cc.setUuid("An invalid long uuid that for sure should result into an exception");
			cs.saveConceptClass(cc);
		}
		catch (UncategorizedSQLException e) {
			exceptionThrown = true;
		}
		
		assertTrue(exceptionThrown);
		
		//No sync record should have been created
		assertEquals(initialSyncRecordCount, ss.getSyncRecords().size());
	}
	
	private class FailSyncRecordAdvice implements MethodBeforeAdvice {
		
		@Override
		public void before(Method method, Object[] args, Object target) throws Throwable {
			if ("createSyncRecord".equals(method.getName())) {
				throw new SyncException("Forced Sync Exception");
			}
		}
		
	}
	
}
