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

import java.lang.reflect.InvocationTargetException;

/**
 * Very basic serializer/normalizer that converts an object to/from a string
 * 
 * @see Normalizer
 * @see TimestampNormalizer
 * @see BinaryNormalizer
 */
public class DefaultNormalizer extends Normalizer {
	
	/**
	 * This basic method just returns a toString() call on the object
	 * 
	 * @see org.openmrs.module.sync.serialization.Normalizer#toString(java.lang.Object)
	 */
	public String toString(Object o) {
		if (o == null)
			return "";
		
		return o.toString();
	}
	
	/**
	 * This default implementation attempts to due <code>new clazz(s)</code> if there is a
	 * constructor with a string parameter...otherwise returns null
	 * 
	 * @see org.openmrs.module.sync.serialization.Normalizer#fromString(java.lang.Class,
	 *      java.lang.String)
	 */
	public Object fromString(Class clazz, String s) {
		
		// super simple case
		if ("java.lang.String".equals(clazz.getName()))
			return s;
		
		// try to use a String constructor
		try {
			return clazz.getConstructor(String.class).newInstance(s);
		}
		catch (NoSuchMethodException e) {
			log.debug("There is no String constructor on the " + clazz.getName() + ", so I'm not sure what to do here", e);
		}
		catch (IllegalArgumentException e) {
			log.debug("The string constructor on the " + clazz.getName()
			        + " object didn't take in the string value, so I'm not sure what to do here", e);
		}
		catch (SecurityException e) {
			log.debug("There is no String constructor on the " + clazz.getName()
			        + " object cannot be accessed, so I'm not sure what to do here", e);
		}
		catch (InstantiationException e) {
			log.debug("The " + clazz + " object can't be instantiated, so I'm not sure what to do here", e);
		}
		catch (IllegalAccessException e) {
			log.debug("The String constructor on the " + clazz.getName()
			        + " object can't be accessed, so I'm not sure what to do here", e);
		}
		catch (InvocationTargetException e) {
			log.debug("There is no String constructor on the " + clazz.getName()
			        + "object is bombing, so I'm not sure what to do here", e);
		}
		
		return null;
	}
}
