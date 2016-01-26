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
package org.openmrs.module.sync.serialization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.sync.api.db.hibernate.HibernateSyncInterceptor;

/**
 * Used by {@link HibernateSyncInterceptor} to convert objects to/from a string
 */
public abstract class Normalizer {
	protected final Log log = LogFactory.getLog(Normalizer.class);

	/**
	 * @param o the object to serialize
	 * @return a string representation of the object
	 */
	public abstract String toString(Object o);

	/**
	 * Convert the given string into the given object
	 * 
	 * @param clazz the class of object to return
	 * @param s the value of the serialized object
	 * @return object of class <code>clazz</code> given by <code>s</code>
	 */
	public abstract Object fromString(Class clazz, String s);
}
