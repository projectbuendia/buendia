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

import org.apache.commons.codec.binary.Base64;

/**
 * Encodes and decodes binary (byte[]) data to/from an ascii string using Base64
 * encoding.
 */
public class BinaryNormalizer extends Normalizer {

	public String toString(Object o) {
		return Base64.encodeBase64String((byte[]) o);
	}

	public Object fromString(Class clazz, String s) {
		return Base64.decodeBase64(s);
	}
}