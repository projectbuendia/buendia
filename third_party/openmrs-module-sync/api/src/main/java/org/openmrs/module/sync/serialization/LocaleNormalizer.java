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

import java.util.Locale;

import org.openmrs.util.LocaleUtility;

/**
 * Deals with {@link Locale} objects
 */
public class LocaleNormalizer extends DefaultNormalizer {
	
	/**
	 * We can't use the {@link DefaultNormalizer#fromString(Class, String)} here because that calls
	 * the {@link Locale#toString()}. That constructor on Locale does not deal with language
	 * variants like en_US well (correctly).
	 * 
	 * @see org.openmrs.module.sync.serialization.DefaultNormalizer#fromString(java.lang.Class,
	 *      java.lang.String)
	 */
	public Object fromString(Class clazz, String s) {
		return LocaleUtility.fromSpecification(s);
	}
}
