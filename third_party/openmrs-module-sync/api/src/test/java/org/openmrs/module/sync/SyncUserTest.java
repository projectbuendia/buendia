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
package org.openmrs.module.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Date;

import junit.framework.Assert;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.advice.GenerateSystemIdAdvisor;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.test.annotation.NotTransactional;

/**
 *
 */
public class SyncUserTest extends SyncBaseTest {

	@Override
    public String getInitialDataset() {
        try {
            return "org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	@Test
	@NotTransactional
	public void shouldCreateUser() throws Exception {
		runSyncTest(new SyncTestHelper() {
			UserService us = Context.getUserService();
			public void runOnChild() {
				User u = new User();
				u.setPerson(new Person());
				u.setUsername("djazayeri");
				u.addName(new PersonName("Darius", "Graham", "Jazayeri"));
				u.getPerson().setGender("M");
				u.addRole(us.getRole("Administrator"));
				u.addRole(us.getRole("Provider"));
				us.saveUser(u, "Test1234");
			}
			public void runOnParent() {
				User u = us.getUserByUsername("djazayeri");
				assertNotNull("User not created", u);
				assertEquals("Failed to create person name", u.getPersonName().getGivenName(), "Darius");
				assertEquals("Failed to assign roles", u.getRoles().size(), 2);
			}
		});
	}

	@Test
	@NotTransactional
	public void shouldChangePwd() throws Exception {
		runSyncTest(new SyncTestHelper() {
			UserService us = Context.getUserService();
			String newPWD = "NewPassword123";
			String userUuid = null; 
			public void runOnChild() {
				userUuid = us.getUser(1).getUuid();
				us.changePassword(us.getUser(1), newPWD);
				assertEquals(userUuid, us.getUser(1).getUuid());
			}
			public void runOnParent() {
				assertEquals(userUuid, us.getUser(1).getUuid());
				Context.authenticate(us.getUser(1).getSystemId(), newPWD);
			}
		});
	}

	@Test
	@NotTransactional
	public void shouldEditUser() throws Exception {
		runSyncTest(new SyncTestHelper() {
			UserService us = Context.getUserService();
			Date d = ymd.parse("1978-04-11");
			int numRolesBefore;
			public void runOnChild() {
				User u = us.getUser(1);
				u.getPerson().setBirthdate(d);
				u.addName(new PersonName("Darius", "Graham", "Jazayeri"));
				numRolesBefore = u.getRoles().size();
				u.addRole(us.getRole("Provider"));
				us.saveUser(u, null);
			}
			public void runOnParent() {
				User u = us.getUser(1);
				assertEquals("Failed to create person name", u.getNames().size(), 2);
				assertEquals("Failed to assign roles", u.getRoles().size(), numRolesBefore + 1);
				assertEquals("Failed to set birthdate", OpenmrsUtil.compare(u.getPerson().getBirthdate(), d), 0);
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldCreateRoleAndPrivilege() throws Exception {
		runSyncTest(new SyncTestHelper() {
			public void runOnChild() {
				Privilege priv = new Privilege("Kitchen Use");
				priv.setDescription("Can step into the kitchen");
				Context.getUserService().savePrivilege(priv);
				Role role = new Role("Chef");
				role.setDescription("One who cooks");
				role.addPrivilege(priv);
				Context.getUserService().saveRole(role);
			}
			public void runOnParent() {
				Privilege priv = Context.getUserService().getPrivilege("Kitchen Use");
				assertEquals("Privilege failed", "Can step into the kitchen", priv.getDescription());
				Role role = Context.getUserService().getRole("Chef");
				assertEquals("Role failed", "One who cooks", role.getDescription());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldAddPrivilegeToRole() throws Exception {
		runSyncTest(new SyncTestHelper() {
			int numAtStart = 0;
			public void runOnChild() {
				Privilege priv = Context.getUserService().getPrivilege("Manage Locations");
				Role role = Context.getUserService().getRole("Provider");
				numAtStart = role.getPrivileges().size();
				role.addPrivilege(priv);
				Context.getUserService().saveRole(role);
			}
			public void runOnParent() {
				Role role = Context.getUserService().getRole("Provider");
				assertEquals("Failed to create role",
				             numAtStart + 1,
				             role.getPrivileges().size());
				assertTrue("Does not have newly granted privilege", role.hasPrivilege("Manage Locations"));
			}
		});
	}
	
	
	/**
	 * This validates that the {@link GenerateSystemIdAdvisor} is working
	 * and prepending the server id to the system id when a new user is created
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldPrependServerIdToNewUsersGeneratedSystemId() throws Exception {
		runSyncTest(new SyncTestHelper() {
			String EXPECTED_SYSTEM_ID = "parent_3-9";
			
			UserService us = Context.getUserService();
			public void runOnChild() {
				User u = new User();
				u.setPerson(new Person());
				u.setUsername("djazayeri");
				u.addName(new PersonName("Darius", "Graham", "Jazayeri"));
				u.getPerson().setGender("M");
				us.saveUser(u, "Test1234");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
			public void runOnParent() {
				User u = us.getUserByUsername("djazayeri");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
		});
	}
	
	/**
	 * This validates that the {@link GenerateSystemIdAdvisor} is just
	 * silently not doing anything if the server id is not defined yet
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldNotGenerateSystemIdIfGPNotDefined() throws Exception {
		runSyncTest(new SyncTestHelper() {
			String EXPECTED_SYSTEM_ID = "3-4";
			
			UserService us = Context.getUserService();
			public void runOnChild() {
				// override the xml defined server id with a blank server id
				Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(SyncConstants.PROPERTY_SYSTEM_ID_TEMPLATE, ""));

				User u = new User();
				u.setPerson(new Person());
				u.setUsername("djazayeri");
				u.addName(new PersonName("Darius", "Graham", "Jazayeri"));
				u.getPerson().setGender("M");
				us.saveUser(u, "Test1234");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
			public void runOnParent() {
				User u = us.getUserByUsername("djazayeri");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
		});
	}
	
	/**
	 * This validates that the {@link GenerateSystemIdAdvisor} is is replacing the SYNCSERVERUUID var
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldAddServerUuidToGeneratedSystemId() throws Exception {
		runSyncTest(new SyncTestHelper() {
			String EXPECTED_SYSTEM_ID = "46b16ac6144e102b8d9ce44ed545d86c_3-3";
			
			UserService us = Context.getUserService();
			public void runOnChild() {
				// override the xml defined server id with a blank server id
				Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(SyncConstants.PROPERTY_SYSTEM_ID_TEMPLATE, "{SYNCSERVERUUID}_{NEXTUSERID}{CHECKDIGIT}"));
				
				User u = new User();
				u.setPerson(new Person());
				u.setUsername("djazayeri");
				u.addName(new PersonName("Darius", "Graham", "Jazayeri"));
				u.getPerson().setGender("M");
				us.saveUser(u, "Test1234");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
			public void runOnParent() {
				User u = us.getUserByUsername("djazayeri");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
		});
	}
	
	/**
	 * This validates that the {@link GenerateSystemIdAdvisor} is is replacing the CHECKDIGIT var
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldGenerateCheckDigitInGeneratedSystemId() throws Exception {
		runSyncTest(new SyncTestHelper() {
			String EXPECTED_SYSTEM_ID = "parent-2";
			
			UserService us = Context.getUserService();
			public void runOnChild() {
				// override the xml defined server id with a blank server id
				Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(SyncConstants.PROPERTY_SYSTEM_ID_TEMPLATE, "{CHECKDIGIT}{SYNCSERVERNAME}"));
				
				User u = new User();
				u.setPerson(new Person());
				u.setUsername("djazayeri");
				u.addName(new PersonName("Darius", "Graham", "Jazayeri"));
				u.getPerson().setGender("M");
				us.saveUser(u, "Test1234");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
			public void runOnParent() {
				User u = us.getUserByUsername("djazayeri");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
		});
	}
	
	/**
	 * This validates that the {@link GenerateSystemIdAdvisor} is is replacing the CHECKDIGIT var
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldGenerateCheckDigitWithNextUserIdInGeneratedSystemId() throws Exception {
		runSyncTest(new SyncTestHelper() {
			String EXPECTED_SYSTEM_ID = "3-4";
			
			UserService us = Context.getUserService();
			public void runOnChild() {
				// override the xml defined server id with a blank server id
				Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(SyncConstants.PROPERTY_SYSTEM_ID_TEMPLATE, "{NEXTUSERID}{CHECKDIGIT}"));
				
				User u = new User();
				u.setPerson(new Person());
				u.setUsername("djazayeri");
				u.addName(new PersonName("Darius", "Graham", "Jazayeri"));
				u.getPerson().setGender("M");
				us.saveUser(u, "Test1234");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
			public void runOnParent() {
				User u = us.getUserByUsername("djazayeri");
				assertEquals(EXPECTED_SYSTEM_ID, u.getSystemId());
			}
		});
	}

	@Test
	@NotTransactional
	public void shouldSyncUserPropertyUpdate() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			UserService us = Context.getUserService();
			
			public void runOnChild() {				
				User u = us.getUser(1);
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE, "fr");
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD, "false");
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS, "5");
				us.saveUser(u, null);
			}
			
			public void runOnParent() {
				User u = us.getUser(1);
				assertFalse(Boolean.valueOf(u.getUserProperties().get(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD)));
				assertTrue("fr".equals(u.getUserProperties().get(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE)));
				assertTrue("5".equals(u.getUserProperties().get(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS)));
			}
		});
	}

	@Test
	@NotTransactional
	public void shouldSyncUserPropertyRemove() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			UserService us = Context.getUserService();
			public void runOnChild() {
				
				User u = us.getUser(1);
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD, "true");
				us.saveUser(u, null);
				u.removeUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD);
				us.saveUser(u, null);
			}
			public void runOnParent() {
				User u = us.getUser(1);
				assertFalse(u.getUserProperties().containsKey(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD));
			}
		});
	}

	@Test
	@NotTransactional
	public void shouldDeleteUserAccount() throws Exception {
		runSyncTest(new SyncTestHelper() {

			UserService us = Context.getUserService();

			public void runOnChild() {
				User u = us.getUserByUsername("bwayne");
				Assert.assertNotNull(u);
				us.purgeUser(u);
			}

			public void runOnParent() {
				User u = us.getUserByUsername("bwayne");
				Assert.assertNull(u);
			}
		});
	}
	
}
