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
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncSubclassStub;
import org.openmrs.module.sync.api.SyncService;
import org.springframework.aop.MethodBeforeAdvice;

/**
 * This class intercepts {@link PatientService#savePatient(Patient)}. Sync does this in
 *  order to handle the special  case of saving patient who is already
 * user in the system. See {@link SyncSubclassStub} class comments for detailed description 
 * of how this works.
 * 
 * @see org.openmrs.module.sync.SyncSubclassStub
 */
public class SavePatientAdvice implements MethodBeforeAdvice  {
	
	private static final long serialVersionUID = 38539204394323L;
	
	private Log log = LogFactory.getLog(this.getClass());
	

	/**
	 * @see org.springframework.aop.MethodBeforeAdvice#before(java.lang.reflect.Method,
	 *      java.lang.Object[], java.lang.Object)
	 * @should not fail on update method with no arguments
	 */
	public void before(Method method, Object[] args, Object target) throws Throwable {

		if (!method.getName().equals("savePatient")) {
			return;
		}
		
		log.debug("Executing advice on savePatient() " + args[0].toString());
		
		//pull out the patient object that is being saved, double check type safety
		if (args[0] == null) {
			return;
		}
		if (!Patient.class.isAssignableFrom(args[0].getClass())) {
			return;
		}
		
		Context.getService(SyncService.class).handleInsertPatientStubIfNeeded((Patient)args[0]);
		
		return;
	}
		
}
