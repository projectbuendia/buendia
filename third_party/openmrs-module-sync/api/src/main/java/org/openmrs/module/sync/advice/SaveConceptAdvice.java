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
package org.openmrs.module.sync.advice;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptNumeric;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncSubclassStub;
import org.openmrs.module.sync.api.SyncService;
import org.springframework.aop.MethodBeforeAdvice;

/**
 * This class intercepts {@link ConceptService#saveConcept(Concept)}. Sync does
 * this in order to handle the special case of saving concept numerics who is
 * already concept in the system (e.g. someone changes the datatype to/from a
 * concept numeric). See {@link SyncSubclassStub} class comments for detailed
 * description of how this works.
 * 
 * @see org.openmrs.module.sync.SyncSubclassStub
 */
public class SaveConceptAdvice implements MethodBeforeAdvice {

	private static final long serialVersionUID = 38539204394323L;

	private Log log = LogFactory.getLog(this.getClass());

	/**
	 * @see org.springframework.aop.MethodBeforeAdvice#before(java.lang.reflect.Method,
	 *      java.lang.Object[], java.lang.Object)
	 * @should not fail on update method with no arguments
	 */
	public void before(Method method, Object[] args, Object target)
			throws Throwable {

		if (!method.getName().equals("saveConcept")) {
			return;
		}

		log.debug("Executing advice on saveConcept() " + args[0].toString());

		// pull out the concept object that is being saved, double check type
		// safety
		if (args[0] == null) {
			return;
		}
		if (!Concept.class.isAssignableFrom(args[0].getClass())) {
			return;
		}

		SyncSubclassStub stub = null;
		if (ConceptNumeric.class.isAssignableFrom(args[0].getClass())) {
			stub = new SyncSubclassStub((Concept) args[0], "concept",
					"concept_id", "concept_numeric", "concept_id", null, null,
					null);
			stub.addColumn("precise", 0);
		} else if (ConceptComplex.class.isAssignableFrom(args[0].getClass())) {
			stub = new SyncSubclassStub((Concept) args[0], "concept",
					"concept_id", "concept_complex", "concept_id", null, null,
					null);
		}
		Context.getService(SyncService.class)
				.handleInsertSubclassIfNeeded(stub);

		return;
	}

}
