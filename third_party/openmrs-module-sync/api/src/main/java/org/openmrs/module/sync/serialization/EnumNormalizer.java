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

import java.lang.reflect.Method;

import org.apache.commons.lang.StringUtils;

/**
 * Converts Enum objects to and from strings.
 */
public class EnumNormalizer extends Normalizer {
	
	/**
	 * @see Normalizer#toString(Object)
	 */
	public String toString(Object o) {
		return o.toString();
	}
	
	/**
	 * @see Normalizer#fromString(Class, String)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object fromString(Class clazz, String s) {
		if (StringUtils.isNotEmpty(s)) {
			try {
				Method m = clazz.getMethod("valueOf", String.class);
				return m.invoke(clazz, s);
			}
			catch (Exception e) {
				throw new RuntimeException("Unable to deserialize enum for " + clazz.getName() + " with value " + s, e);
			}
		}
		return null;
	}
}
