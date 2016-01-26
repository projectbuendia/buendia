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

import org.openmrs.api.context.Context;

/**
 * Converts Class objects to and from strings. Serialized version of Class object is simply a full
 * type name.
 */
public class ClassNormalizer extends Normalizer {
	
	public String toString(Object o) {
		if (o instanceof Class)
			return ((Class) o).getName();
		else
			return null;
	}
	
	public Object fromString(Class clazz, String s) {
		Object o = null;
		try {
			o = Context.loadClass(s);
		}
		catch (ClassNotFoundException ex) {}
		
		return o;
	}
}
