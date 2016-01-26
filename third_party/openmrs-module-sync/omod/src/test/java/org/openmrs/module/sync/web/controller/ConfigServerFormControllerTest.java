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
package org.openmrs.module.sync.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncServerClass;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;

/**
 * Tests for the {@link ConfigServerFormController}
 */
@Controller
public class ConfigServerFormControllerTest  extends BaseModuleContextSensitiveTest {
        
        /** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
    

        /**
         * Test of saveOrUpdateServerClasses method, of class ConfigServerFormController.
         */
        @Test
        public void saveOrUpdateServerClasses_shouldTrimClassnames() {
            RemoteServer testServer = new RemoteServer();
            HashSet<String> serverClasses = new HashSet<String>();
            testServer.setServerClasses((Set)serverClasses);
            List <String> classNamesNotToSend = new ArrayList();
            List <String> classNamesNotReceiveFrom = new ArrayList();
            classNamesNotToSend.add("  org.openmrs.GlobalProperty ");
            classNamesNotReceiveFrom.add("  org.openmrs.GlobalProperty ");
            
            ConfigServerFormController instance = new ConfigServerFormController();
            instance.saveOrUpdateServerClasses(testServer, classNamesNotToSend, classNamesNotReceiveFrom);
            Set<String> classesNotSent = testServer.getClassesNotSent();
            Set<String> classesNotReceived = testServer.getClassesNotReceived();
            
            Assert.assertEquals("org.openmrs.GlobalProperty", classesNotSent.iterator().next());
            Assert.assertEquals("org.openmrs.GlobalProperty", classesNotReceived.iterator().next());
        }
        
        /**
         * Test of onSaveParent method, of class ConfigServerFormController.
         * It tests if the parameters passed as arguments are correctly saved
         * @throws Exception 
         */
        @Test
        public void onSaveParent_shouldSaveParentServerParameters() throws Exception{
        	RemoteServer server = new RemoteServer();
        	String nickname = "testnickname";
        	String address = "testaddress";
        	String username = "testusername";
        	String password = "Testpass123";
        	Boolean started = true;
        	Integer repeatInterval = 60;
        	HttpSession session = new MockHttpSession();
        	
        	Set<SyncServerClass> classes = Collections.emptySet();
        	server.setServerClasses(classes);
        	
        	ConfigServerFormController instance = new ConfigServerFormController();
            instance.onSaveParent(nickname, address, username, password, started, repeatInterval, 
            		session, server, new BeanPropertyBindingResult(null,null), null, null);
            
            // Retrieve parent server and check its parameters
            SyncService service = Context.getService(SyncService.class);
            RemoteServer parent = service.getParentServer();
            
            Assert.assertNotNull(parent.getServerId());
            Assert.assertEquals(nickname, parent.getNickname());
            Assert.assertEquals(address, parent.getAddress());
            Assert.assertEquals(username, parent.getUsername());
            Assert.assertEquals(password, parent.getPassword());
            
            boolean taskScheduled = false;
            for (TaskDefinition task : Context.getSchedulerService().getScheduledTasks()){
            	if (task.getTaskClass().equals(SyncConstants.SCHEDULED_TASK_CLASS)
            			&& parent.getServerId().toString().equals(task.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID))){
            		taskScheduled = true;
            		break;
            	}
            }
            Assert.assertTrue(taskScheduled);
        }
}