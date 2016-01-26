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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.annotation.Rollback;

/**
 *
 */
public class SyncEncounterTest extends SyncBaseTest {

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
    @Rollback(false)
	public void shouldCreateEncounterType() throws Exception {
		runSyncTest(new SyncTestHelper() {			
			EncounterService encounterService = Context.getEncounterService();

			public void runOnChild() {
				assertNotNull(Context.getAuthenticatedUser());
				EncounterType encounterType = new EncounterType();
				encounterType.setName("name");
				encounterType.setDescription("description");
				encounterService.saveEncounterType(encounterType);
			}
			public void runOnParent() {
				assertNotNull(Context.getAuthenticatedUser());
				EncounterType encounterType = encounterService.getEncounterType("name");
				assertNotNull(encounterType);
			}
		});
	}	

	@Test
    @NotTransactional
	public void shouldUpdateEncounterType() throws Exception {
		runSyncTest(new SyncTestHelper() {			
			AdministrationService adminService = Context.getAdministrationService();
			EncounterService encounterService = Context.getEncounterService();
			public void runOnChild() {
				
				EncounterType encounterType = new EncounterType();
				encounterType.setName("name");
				encounterType.setDescription("description");
				adminService.createEncounterType(encounterType);	
				
				EncounterType updateEncounterType = encounterService.getEncounterType("name");
				encounterType.setName("new name");
				adminService.updateEncounterType(updateEncounterType);
			}
			public void runOnParent() {
				EncounterType encounterType = encounterService.getEncounterType("name");
				assertNull(encounterType);
				
				encounterType = encounterService.getEncounterType("new name");				
				assertNotNull(encounterType);
			}
		});
	}
	
	@Test
    @NotTransactional
	public void shouldDeleteEncounterType() throws Exception { 
		
		runSyncTest(new SyncTestHelper() {			
			public void runOnChild() {
				EncounterType existing = Context.getEncounterService().getEncounterType("DELETETEST");
				assertNotNull(existing);
				Context.getAdministrationService().deleteEncounterType(existing);
			}
			public void runOnParent() {
				EncounterType encounterType = Context.getEncounterService().getEncounterType("DELETETEST");
				assertNull(encounterType);
			}
		});
		
	}

	@Test
    @NotTransactional
	public void shouldCreateEncounter() throws Exception {
		runSyncTest(new SyncTestHelper() {			
			String eid = null;
			Calendar c;

			public void runOnChild() {
				
				Encounter e = new Encounter();
				c = Calendar.getInstance();
				c.set(2000,1,1);
				e.setCreator(Context.getAuthenticatedUser());
				e.setEncounterDatetime(c.getTime());
				e.setPatient(Context.getPatientService().getPatient(2));
				e.setEncounterType(Context.getEncounterService().getEncounterType("ADULTINITIAL"));
				Context.getEncounterService().createEncounter(e);
				eid = e.getUuid();
			}
			public void runOnParent() {
				Encounter e = Context.getEncounterService().getEncounterByUuid(eid);
				assertNotNull(e);
				assertEquals(c.getTime(),e.getEncounterDatetime());
				assertEquals(e.getEncounterType(),Context.getEncounterService().getEncounterType("ADULTINITIAL"));
			}
		});
	}	

	@Test
    @NotTransactional
	public void shouldDeleteEncounter() throws Exception {
		runSyncTest(new SyncTestHelper() {			

			public void runOnChild() {
				
				//delete existing
				Encounter existing = Context.getEncounterService().getEncounter(1);
				assertNotNull(existing);
				Context.getEncounterService().deleteEncounter(existing);

			}
			public void runOnParent() {
				Encounter e = null;
				e = Context.getEncounterService().getEncounter(1);
				assertNull(e);
			}
		});
	}
	
	@Test
    @NotTransactional
	public void shouldAddObsWithEncounter() throws Exception {
		runSyncTest(new SyncTestHelper() {			
			
			String encUuid;
			
			public void runOnChild() {
				
				Encounter e = new Encounter();
				e.setEncounterDatetime(new Date());
				e.setPatient(Context.getPatientService().getPatient(2));
				e.setEncounterType(Context.getEncounterService().getEncounterType("ADULTINITIAL"));
				
				// add some observations
                Person person = Context.getPersonService().getPerson(2);
                Concept concept = Context.getConceptService().getConcept(1);
                Location loc = Context.getLocationService().getLocation(1);

				Obs o = new Obs(person, concept, new Date(), loc);
				e.addObs(o);
				
				Context.getEncounterService().saveEncounter(e);
				
				encUuid = e.getUuid();
			}
			public void runOnParent() {
				Context.clearSession();
				
				Encounter e = Context.getEncounterService().getEncounterByUuid(encUuid);
				assertNotNull(e);
				Assert.assertTrue("No obs were found", e.getObs().size() > 0);
			}
		});
	}
	
	
	
}
