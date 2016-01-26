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
package org.openmrs.module.sync.api.db.hibernate;

import java.lang.reflect.Method;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests methods in HibernateSyncDAO
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class HibernateSyncDAOTest {
	
	@Test
	public void getConnectionProperties_shouldHandleStandaloneUrl() {
		parseConnectionProperties("jdbc:mysql:mxj://127.0.0.1:3317/openmrs?autoReconnect=true&sessionVariables=storage_engine=InnoDB&useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&server.initialize-user=true&createDatabaseIfNotExist=true&server.basedir=database&server.datadir=database/data&server.collation-server=utf8_general_ci&server.character-set-server=utf8&server.max_allowed_packet=96M");
	}
	
	@Test
	public void getConnectionProperties_shouldHandleNonStandaloneUrl() {
		parseConnectionProperties("jdbc:mysql://localhost:3306/openmrs?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");
	}
	
	@Test
	public void getConnectionProperties_shouldHandleUrlWithoutParams() {
		parseConnectionProperties("jdbc:mysql://localhost:3306/openmrs");
	}
	
	@Test
	public void getConnectionProperties_shouldHandleNonDefaultUrl() {
		parseConnectionPropertiesWithHostAndPort("jdbc:mysql://12.34.56.78:3306/openmrs",
				new String[]{"", "", "openmrs", "12.34.56.78", "3306"});
	}
	
	@Test
	public void getConnectionProperties_shouldHandleNonDefaultPort(){
		parseConnectionPropertiesWithHostAndPort("jdbc:mysql://127.0.0.1:6033/openmrs",
				new String[]{"", "", "openmrs", "127.0.0.1", "6033"});
	}
	
	private void parseConnectionProperties(String url) {
		Properties properties = new Properties();
		properties.put("connection.url", url);
		
		PowerMockito.mockStatic(Context.class);
		Mockito.when(Context.getRuntimeProperties()).thenReturn(properties);
		
		try {
			HibernateSyncDAO dao = new HibernateSyncDAO();
			Method method = dao.getClass().getDeclaredMethod("getConnectionProperties", null);
			method.setAccessible(true);
			String[] connProps = (String[])method.invoke(dao, null);
			
			Assert.assertEquals("openmrs", connProps[2]);
		}
		catch (Exception ex) {
			Assert.assertFalse("Should correctly handle database connection url", true);
		}
	}
	
	private void parseConnectionPropertiesWithHostAndPort(String url, String[] expected){
		Properties properties = new Properties();
		properties.put("connection.url", url);
		
		PowerMockito.mockStatic(Context.class);
		Mockito.when(Context.getRuntimeProperties()).thenReturn(properties);
		
		try {
			HibernateSyncDAO dao = new HibernateSyncDAO();
			Method method = dao.getClass().getDeclaredMethod("getConnectionProperties", null);
			method.setAccessible(true);
			String[] connProps = (String[])method.invoke(dao, null);
			
			for (int i = 2; i < expected.length; i++){
				Assert.assertEquals(expected[i], connProps[i]);				
			}
		}
		catch (Exception ex) {
			Assert.assertFalse("Should correctly handle database connection url", true);
		}
	}
}
