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

/**
 * Represents often fatal errors that occur within the xforms module
 * 
 */
public class XformsException extends RuntimeException {

	public static final long serialVersionUID = 121212344443789L;

	public XformsException() {
	}

	public XformsException(String message) {
		super(message);
	}

	public XformsException(String message, Throwable cause) {
		super(message, cause);
	}

	public XformsException(Throwable cause) {
		super(cause);
	}

}
