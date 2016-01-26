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

import static org.junit.Assert.assertNotNull;

import java.util.List;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.openmrs.Cohort;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.CohortService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.test.annotation.NotTransactional;

/**
 *
 */
public class SyncCollectionsTest extends SyncBaseTest {

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
	public void shouldSyncASetOfNonOpenmrsObjectsWhenAnItemIsAdded() throws Exception {
		
		final Integer cohortId = 1;
		final Integer memberIdToAdd1 = 5;
		final Integer memberIdToAdd2 = 7;
		runSyncTest(new SyncTestHelper() {
			
			CohortService cs = Context.getCohortService();
			
			public void runOnChild() throws Exception {
				executeDataSet("org/openmrs/api/include/PatientServiceTest-findPatients.xml");
				executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");;
				
				Cohort c = cs.getCohort(cohortId);
				Assert.assertFalse(c.isEmpty());
				Assert.assertFalse(c.contains(memberIdToAdd1));
				Assert.assertFalse(c.contains(memberIdToAdd2));
				
				c.addMember(memberIdToAdd1);
				cs.saveCohort(c);
				
				c.addMember(memberIdToAdd2);
				cs.saveCohort(c);
			}
			
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				Assert.assertEquals(1, syncRecords.get(0).getItems().size());
				Assert.assertEquals(SyncItemState.UPDATED, syncRecords.get(0).getItems().iterator().next().getState());
				
				//Finally i can insert more test data into the parent and child2 before syncing
				executeDataSet("org/openmrs/api/include/PatientServiceTest-findPatients.xml");
				executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
			}
			
			public void runOnParent() throws Exception {
				Cohort c = cs.getCohort(cohortId);
				Assert.assertTrue(c.contains(memberIdToAdd1));
				Assert.assertTrue(c.contains(memberIdToAdd2));
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSyncASetOfNonOpenmrsObjectsWhenAnItemIsRemoved() throws Exception {
		
		final Integer cohortId = 1;
		final Integer memberIdToRemove = 2;
		runSyncTest(new SyncTestHelper() {
			
			CohortService cs = Context.getCohortService();
			
			public void runOnChild() throws Exception {
				executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
				
				Cohort c = cs.getCohort(cohortId);
				Assert.assertTrue(c.contains(memberIdToRemove));
				
				c.removeMember(memberIdToRemove);
				cs.saveCohort(c);
			}
			
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				Assert.assertEquals(1, syncRecords.get(0).getItems().size());
				Assert.assertEquals(SyncItemState.UPDATED, syncRecords.get(0).getItems().iterator().next().getState());
				
				executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
			}
			
			public void runOnParent() throws Exception {
				Cohort c = cs.getCohort(cohortId);
				Assert.assertFalse(c.contains(memberIdToRemove));
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSyncASetOfNonOpenmrsObjectsWhenAllItemsAreRemoved() throws Exception {
		
		final Integer cohortId = 1;
		runSyncTest(new SyncTestHelper() {
			
			CohortService cs = Context.getCohortService();
			
			public void runOnChild() throws Exception {
				executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
				
				Cohort c = cs.getCohort(cohortId);
				Assert.assertFalse(c.getMemberIds().isEmpty());
				c.getMemberIds().clear();
				
				cs.saveCohort(c);
			}
			
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				Assert.assertEquals(1, syncRecords.get(0).getItems().size());
				Assert.assertEquals(SyncItemState.UPDATED, syncRecords.get(0).getItems().iterator().next().getState());
				
				executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
			}
			
			public void runOnParent() throws Exception {
				Assert.assertEquals(0, cs.getCohort(cohortId).getMemberIds().size());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSyncAMapOfNonOpenmrsObjectsWhenAnEntryValueIsIsUpdated() throws Exception {
		
		final Integer userId = 1;
		final String newUserLocaleValue = "en";
		final String newChangePasswordValue = "true";
		runSyncTest(new SyncTestHelper() {
			
			UserService us = Context.getUserService();
			
			public void runOnChild() throws Exception {
				executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
				User u = us.getUser(userId);
				Assert.assertFalse(newChangePasswordValue.equals(u
				        .getUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD)));
				Assert.assertFalse("1".equals(u.getUserProperty(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS)));
				Assert.assertFalse(newUserLocaleValue.equals(u
				        .getUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE)));
				
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD, newChangePasswordValue);
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE, newUserLocaleValue);
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS, "1");
				
				us.saveUser(u, null);
				
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS, "2");
				us.saveUser(u, null);
			}
			
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				Assert.assertEquals(2, syncRecords.size());
				SyncItemState userSyncItemState = null;
				for (SyncRecord syncRecord : syncRecords) {
					for (SyncItem si : syncRecord.getItems()) {
						if (User.class.equals(si.getContainedType()))
							userSyncItemState = si.getState();
					}
				}
				
				Assert.assertEquals(SyncItemState.UPDATED, userSyncItemState);
				
				executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
			}
			
			public void runOnParent() {
				User u = us.getUser(userId);
				Assert.assertEquals(newChangePasswordValue,
				    u.getUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD));
				Assert.assertEquals("2", u.getUserProperty(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS));
				Assert.assertEquals(newUserLocaleValue, u.getUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE));
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSyncAMapOfNonOpenmrsObjectsWhenAnEntryIsAdded() throws Exception {
		
		final Integer userId = 1;
		final String newPropertyName = "new property";
		final String newPropertyValue = "new property value";
		
		runSyncTest(new SyncTestHelper() {
			
			UserService us = Context.getUserService();
			
			public void runOnChild() {
				User u = us.getUser(userId);
				Assert.assertEquals("", u.getUserProperty(newPropertyName));
				
				u.setUserProperty(newPropertyName, newPropertyValue);
				
				us.saveUser(u, null);
			}
			
			public void runOnParent() {
				Assert.assertEquals(newPropertyValue, us.getUser(userId).getUserProperty(newPropertyName));
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSyncAMapOfNonOpenmrsObjectsWhenAnEntryIsRemoved() throws Exception {
		
		final Integer userId = 5505;
		final String propertyNameToRemove = "some key";
		runSyncTest(new SyncTestHelper() {
			
			UserService us = Context.getUserService();
			
			public void runOnChild() throws Exception {
				executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
				
				User u = us.getUser(userId);
				Assert.assertFalse(StringUtils.isBlank(u.getUserProperty(propertyNameToRemove)));
				
				u.removeUserProperty(propertyNameToRemove);
				
				us.saveUser(u, null);
			}
			
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
			}
			
			public void runOnParent() {
				Assert.assertEquals("", us.getUser(userId).getUserProperty(propertyNameToRemove));
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSyncAnObjectWhenAMapOfNonOpenmrsKeysAndValuesIsCleared() throws Exception {
		
		final Integer userId = 5505;
		runSyncTest(new SyncTestHelper() {
			
			UserService us = Context.getUserService();
			
			public void runOnChild() throws Exception {
				executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
				
				User u = us.getUser(userId);
				Assert.assertFalse(u.getUserProperties().isEmpty());
				
				u.getUserProperties().clear();
				
				us.saveUser(u, null);
			}
			
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
			}
			
			public void runOnParent() {
				Assert.assertTrue(us.getUser(userId).getUserProperties().isEmpty());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSyncAnObjectThatIsCreatedAlongWithAMapOfElements() throws Exception {
		
		final String username = "random";
		final String defaultLocale = "fr";
		final String changePassword = "true";
		
		runSyncTest(new SyncTestHelper() {
			
			UserService us = Context.getUserService();
			
			public void runOnChild() throws Exception {
				
				User u = new User();
				u.setPerson(new Person());
				
				u.addName(new PersonName("Wyclif", null, "Luyima"));
				u.setUsername("random");
				u.getPerson().setGender("M");
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE, defaultLocale);
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD, changePassword);
				
				us.saveUser(u, "Openmr5xy");
			}
			
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				SyncItemState userSyncItemState = null;
				for (SyncRecord syncRecord : syncRecords) {
					for (SyncItem si : syncRecord.getItems()) {
						if (User.class.equals(si.getContainedType()))
							userSyncItemState = si.getState();
					}
				}
				
				Assert.assertEquals(SyncItemState.NEW, userSyncItemState);
			}
			
			public void runOnParent() {
				User u = us.getUserByUsername(username);
				Assert.assertEquals(2, u.getUserProperties().size());
				Assert.assertEquals(defaultLocale, u.getUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE));
				Assert.assertEquals(changePassword, u.getUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD));
			}
		});
	}
}
