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
package org.openmrs.module.sync.api.db.hibernate.usertype;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.openmrs.api.context.Context;
import org.openmrs.util.HibernateEnumType;

/**
 * This is only here so that sync is backwards compatible to OpenMRS 1.8.3. Once
 * 1.8.4 is released and this module depends on at least 1.8.4, this class can
 * be removed and the hibernate mappings changed. <br/>
 * This method makes sync compatible with both 1.9+ (which has a newer hibernate
 * version) and all versions of 1.8.
 */
public class GenericEnumUserType extends HibernateEnumType {

	private Class clazz = null;

	public void setParameterValues(Properties params) {
		String enumClassName = params.getProperty("enumClassName");
		if (enumClassName == null) {
			throw new MappingException("enumClassName parameter not specified");
		}

		try {
			this.clazz = Context.loadClass(enumClassName);
		} catch (ClassNotFoundException e) {
			throw new MappingException("enumClass " + enumClassName
					+ " not found", e);
		}
	}

	public Class<?> returnedClass() {
		return clazz;
	}

	@SuppressWarnings("unchecked")
	public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner)
			throws HibernateException, SQLException {
		String name = resultSet.getString(names[0]);
		Object result = null;
		if (!resultSet.wasNull() && !StringUtils.isBlank(name)) {
			result = Enum.valueOf(clazz, name);
		}
		return result;
	}
}
