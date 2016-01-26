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

import java.util.HashMap;
import java.util.Map;

import org.openmrs.module.sync.SyncUtil;

/**
 * Serializes and de-serializes Map<K,V> where K and V are types we can otherwise normalize. The
 * serialization format is as follows: {{key||value}}{{key||value}}
 */
public class MapNormalizer extends Normalizer {
	
	/***
	 * Returns string representation of map object. If o is not an instance of map, null is
	 * returned. If map.size() == 0 empty string is returned. For the serialization format see class
	 * comments.
	 */
	public String toString(Object o) {
		
		Normalizer keyNormalizer = null;
		Normalizer valueNormalizer = null;
		StringBuilder sb = new StringBuilder();
		
		//check to see if it is not null and map
		if (o != null && Map.class.isAssignableFrom(o.getClass())) {
			
			Map m = (Map) o;
			
			if (m.size() == 0) {
				return "";
			} else {
				//figure out what we are dealing with here
				keyNormalizer = SyncUtil.getNormalizer(m.keySet().toArray()[0].getClass());
				valueNormalizer = SyncUtil.getNormalizer(m.get(m.keySet().toArray()[0]).getClass());
				
				if (keyNormalizer != null && valueNormalizer != null) {
					for (Object key : m.keySet()) {
						sb.append("{{");
						sb.append(keyNormalizer.toString(key));
						sb.append("||");
						//sb.append("</key>");
						//sb.append("<value>");
						sb.append(valueNormalizer.toString(m.get(key)));
						sb.append("}}");
						//sb.append("</value>");
						
					}
				}
				
				return sb.toString();
				
			}
		}
		return null;
	}
	
	/***
	 * De-serializes state back into map object. For serialization format see class comments. If
	 * clazz not Map null returned. If s null, null returned. If s empty, empty Map object returned.
	 * If non-empty string passed in but format does not conform IllegalArgumentException thrown.
	 */
	public Object fromString(Class clazz, String s) throws IllegalArgumentException {
		Map map = new HashMap();
		
		if (!Map.class.isAssignableFrom(clazz) || s == null) {
			return null;
		}
		
		//if it is empty string return empty map
		if (s.length() == 0) {
			return map;
		}
		
		// strip leading and closing {{ }} since we're splitting it
		if (!s.startsWith("{{") || s.lastIndexOf("}}") < 3) {
			//this isn't right, we are expecting {{k,v}}||{{k,v}}...
			throw new IllegalArgumentException(
			        "Invalid serialization format for map object. {{key||value}}{{key||value}} expected.");
		}
		
		String tmp = s;
		tmp = tmp.replaceFirst("\\{\\{", "");
		tmp = tmp.substring(0, tmp.lastIndexOf("}}"));
		
		for (String entry : tmp.split("\\}\\}\\{\\{")) {
			//entry = entry.trim(); // take out whitespace
			if (entry.contains("||")) {
				String keyvalArray[] = entry.split("\\|\\|");

				// try to convert to a simple object
				Object tmpKey = SyncUtil.convertStringToObject(keyvalArray[0], String.class);
				
				if (tmpKey == null)
					throw new IllegalArgumentException("cannot deserialize the 'key' from map entry: " + entry);
				
				// we could have empty values
				Object tmpValue = ""; // since we're converting everything to strings, this is effectively 'null'
				if (keyvalArray.length > 1)
					tmpValue = SyncUtil.convertStringToObject(keyvalArray[1], String.class);
				
				if (tmpValue == null)
					throw new IllegalArgumentException("cannot deserialize the 'value' from map entry: " + entry);
				
				map.put(tmpKey, tmpValue);
			} else {
				throw new IllegalArgumentException(
				        "Invalid serialization format for map entry. {{key||value}} expected. encountered:" + entry);
			}
		}
		
		return map;
	}
}
