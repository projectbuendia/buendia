/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.xforms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * This class implements the task which processes forms in the xforms queue.
 * 
 * @author Daniel Kayiwa
 * @version 1.0
 */
public class ProcessXformsQueueTask extends AbstractTask {

	// Logger
	private static Log log = LogFactory.getLog(ProcessXformsQueueTask.class);

	// Instance of xforms processor.
	private XformsQueueProcessor processor = null;



	/**
	 * Default Constructor (Uses SchedulerConstants.username and
	 * SchedulerConstants.password
	 * 
	 */
	public ProcessXformsQueueTask() {
		if (processor == null)
			processor = new XformsQueueProcessor();
	}

	/**
	 * Process the next xform in the queue and then remove the xform from the
	 * queue.
	 */
	public void execute() {
		Context.openSession();
		log.debug("Running xforms queue task... ");
		try {
			if (Context.isAuthenticated() == false)
				authenticate();
			processor.processXformsQueue();
		} catch (APIException e) {
			log.error("Error running xforms queue task", e);
			throw e;
		} finally {
			Context.closeSession();
		}
	}

	/**
	 * Clean up any resources here
	 * 
	 */
	public void shutdown() {
		processor = null;
		super.shutdown();
		log.debug("Shutting down ProcessXformsQueue task ...");
	}
}
