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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.test.annotation.NotTransactional;

/**
 * Tests creating various pieces of data via synchronization
 */
public class SyncPatientTest extends SyncBaseTest {

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
	public void shouldEnrollInProgram() throws Exception {
		runSyncTest(new SyncTestHelper() {
			int numberEnrolledBefore = 0;
			Date dateEnrolled = new Date(System.currentTimeMillis() - 100000);
			Date dateCompleted = new Date(System.currentTimeMillis() - 10000);
			Program hivProgram = null;
			public void runOnChild() {
				User creator = Context.getAuthenticatedUser();
				Patient p = Context.getPatientService().getPatient(2);
				numberEnrolledBefore = Context.getProgramWorkflowService().getPatientPrograms(p).size();
				hivProgram = Context.getProgramWorkflowService().getProgram("HIV PROGRAM"); 
				Context.getProgramWorkflowService().enrollPatientInProgram(p, hivProgram, dateEnrolled, dateCompleted, creator);
			}
			public void runOnParent() {
				int compare = 0;
				Patient p = Context.getPatientService().getPatient(2);
				assertEquals("Enrollment failed",
				             numberEnrolledBefore + 1,
				             Context.getProgramWorkflowService().getPatientPrograms(p).size());
				for (PatientProgram pp : Context.getProgramWorkflowService().getPatientPrograms(p)) {
					if (pp.getProgram().equals(hivProgram)) {
						compare = OpenmrsUtil.compare( pp.getDateEnrolled(), dateEnrolled);						
						assertEquals("Failed to change date", compare, 0);
						compare = OpenmrsUtil.compare( pp.getDateCompleted(), dateCompleted);
						assertEquals("Failed to change date", compare, 0);
					}
				}
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldEnrollInProgramAndState() throws Exception {
		runSyncTest(new SyncTestHelper() {
			int numberEnrolledBefore = 0;
			Date dateEnrolled = new Date();
			Date dateCompleted = null;
			Program hivProgram = null;
			ProgramWorkflow txStat = null;
			ProgramWorkflowState curedState = null;
			public void runOnChild() {
				
				hivProgram = Context.getProgramWorkflowService().getProgramByName("HIV PROGRAM");

				txStat = hivProgram.getWorkflowByName("TREATMENT STATUS");
				curedState = txStat.getStateByName("PATIENT CURED");
				
				Patient p = Context.getPatientService().getPatient(2);
				numberEnrolledBefore = Context.getProgramWorkflowService().getPatientPrograms(p).size(); 
				Context.getProgramWorkflowService().enrollPatientInProgram(p, hivProgram, dateEnrolled,dateCompleted, Context.getAuthenticatedUser());
				PatientProgram pp = null;
				for (PatientProgram ppLoop : Context.getProgramWorkflowService().getPatientPrograms(p)) {
					if (ppLoop.getProgram().equals(hivProgram)) {
						pp = ppLoop;
						break;
					}
				}				
				Context.getProgramWorkflowService().changeToState(pp, txStat, curedState, dateEnrolled);
			}
			public void runOnParent() {
				int compare = 0;
				Patient p = Context.getPatientService().getPatient(2);
				assertEquals("Enrollment failed",
				             numberEnrolledBefore + 1,
				             Context.getProgramWorkflowService().getPatientPrograms(p).size());
				for (PatientProgram pp : Context.getProgramWorkflowService().getPatientPrograms(p)) {
					if (pp.getProgram().equals(hivProgram)) {
						compare = OpenmrsUtil.compare(  pp.getDateEnrolled(), dateEnrolled);						
						assertEquals("Failed to change date", compare, 0);
							
						assertNotNull("Failed to change date", pp.getDateCompleted());

						assertEquals("Wrong state", pp.getCurrentState(txStat).getState(), curedState);						
					}
				}
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldChangeState() throws Exception {
		runSyncTest(new SyncTestHelper() {
			Program hivProgram;
			ProgramWorkflow txStat;
			ProgramWorkflowState curedState;
			public void runOnChild() {
				hivProgram = Context.getProgramWorkflowService().getProgram("HIV PROGRAM");
				txStat = hivProgram.getWorkflowByName("TREATMENT STATUS");
				curedState = txStat.getStateByName("PATIENT CURED");

				Patient p = Context.getPatientService().getPatient(3);
				Collection<PatientProgram> temp = Context.getProgramWorkflowService().getPatientPrograms(p);
				assertEquals("Before test, patient record does not have the expected number of program enrollments", temp.size(), 1);
				PatientProgram pp = temp.iterator().next();
				assertNotSame("Before test, patient record not in expected state", pp.getCurrentState(txStat), curedState);
				Context.getProgramWorkflowService().changeToState(pp, txStat, curedState, new Date());
			}
			public void runOnParent() {
				Patient p = Context.getPatientService().getPatient(3);
				PatientProgram pp = Context.getProgramWorkflowService().getPatientPrograms(p).iterator().next();
				assertEquals("State not set", pp.getCurrentState(txStat).getState(), curedState);
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldCreateEncounterAndObs() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			int numEncountersSoFar = 0;
			Date dateOfNewEncounter = new Date();
			
			Date anotherDate = new Date(System.currentTimeMillis() - 20000l);
			
			public void runOnChild() {
				ConceptService cs = Context.getConceptService();
				Concept weight = cs.getConceptByName("WEIGHT");
				Concept reason = cs.getConceptByName("REASON ORDER STOPPED");
				Concept other = cs.getConceptByName("OTHER NON-CODED");
				Location loc = Context.getLocationService().getLocation("Someplace");
	
				User u = Context.getUserService().getUser(1);
				Patient p = Context.getPatientService().getPatient(2);
				numEncountersSoFar = Context.getEncounterService().getEncountersByPatient(p).size();
				
				Encounter enc = new Encounter();
				enc.setPatient(p);
				enc.setLocation(loc);
				enc.setProvider(u);
				enc.setEncounterDatetime(dateOfNewEncounter);
				Obs o1 = new Obs();
				o1.setConcept(weight);
				o1.setValueNumeric(74.0);
				o1.setObsDatetime(dateOfNewEncounter);
				Obs o2 = new Obs();
				o2.setConcept(reason);
				o2.setValueCoded(other);
				o2.setObsDatetime(dateOfNewEncounter);
				enc.addObs(o1);
				enc.addObs(o2);
				Context.getEncounterService().saveEncounter(enc);
	
				Obs noEnc = new Obs();
				noEnc.setConcept(weight);
				noEnc.setValueNumeric(12.3);
				noEnc.setObsDatetime(anotherDate);
				noEnc.setPerson(p);
				noEnc.setLocation(loc);
				Context.getObsService().saveObs(noEnc,null);
			}
			public void runOnParent() {
				
				ConceptService cs = Context.getConceptService();
				Concept weight = cs.getConceptByName("WEIGHT");
				Concept reason = cs.getConceptByName("REASON ORDER STOPPED");
				Concept other = cs.getConceptByName("OTHER NON-CODED");
				Location loc = Context.getLocationService().getLocation("Someplace");
				Patient p = Context.getPatientService().getPatient(2);
				
				
				List<Encounter> encs = Context.getEncounterService().getEncountersByPatient(p);
				assertEquals("Should now have one more encounter than before",
				             numEncountersSoFar + 1,
				             encs.size());
				Encounter lookAt = null;
				for (Encounter e : encs) {
					if (OpenmrsUtil.compare(e.getEncounterDatetime(), dateOfNewEncounter) == 0) {
						lookAt = e;
						break;
					}
				}
				
				assertEquals(lookAt.getLocation(), loc);
				
				//reload lookAt
				int lookAtId = lookAt.getEncounterId();
				Context.evictFromSession(lookAt);
				lookAt = Context.getEncounterService().getEncounter(lookAtId);
				assertEquals("Should have two obs", 2,lookAt.getObs().size());
				for (Obs o : lookAt.getObs()) {
					if (o.getConcept().equals(weight)) {
						assertEquals("Weight should be 74.0", o.getValueNumeric(), (Double)74.0);
					} else {
						assertEquals("Reason should be OTHER NON-CODED", o.getValueCoded(), other);
					}
				}
				
				boolean found = false;
				for (Obs o : Context.getObsService().getObservationsByPerson(p)) {
					if ( (OpenmrsUtil.compare(o.getObsDatetime(), anotherDate) == 0) && o.getConcept().equals(weight) && o.getValueNumeric().equals(12.3))
						found = true;
				}
				assertTrue("Cannot find newly created encounter-less obs", found);
			}
		});
	}
	
	/**
	 * Fails due to known bug: see ticket #934
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldEditEncounter() throws Exception {
		runSyncTest(new SyncTestHelper() {
			Date d1 = ymd.parse("1978-01-01");
			Date d2 = ymd.parse("1978-12-31");
			public void runOnChild(){
				Patient p = Context.getPatientService().getPatient(2);
				Collection<Encounter> encs = Context.getEncounterService().getEncountersByPatient(p);
				assertEquals(encs.size(), 1);
				Encounter e = encs.iterator().next();
				e.setEncounterDatetime(d2);
				Context.getEncounterService().saveEncounter(e);
			}
			public void runOnParent() {
				Patient p = Context.getPatientService().getPatient(2);
				Collection<Encounter> encs = Context.getEncounterService().getEncounters(p, d1, d2);
				assertEquals(encs.size(), 1);
				Encounter e = encs.iterator().next();
				
				int compare = OpenmrsUtil.compare(e.getEncounterDatetime(), d2);
				
				assertEquals("Failed to change date", compare, 0);
			}
		});
	}

	@Test
	@NotTransactional
	public void shouldEditObs() throws Exception {
		runSyncTest(new SyncTestHelper() {
			Date d = ymd.parse("1978-04-11");
			Concept weight = null;
			int obsCount = 0;
			public void runOnChild(){
				weight = Context.getConceptService().getConceptByName("WEIGHT");
				Patient p = Context.getPatientService().getPatient(2);
				Obs obs = null;
				for (Obs o : Context.getObsService().getObservationsByPersonAndConcept(p, weight)) {
					if (OpenmrsUtil.compare(o.getObsDatetime(), d) == 0) {
						obs = o;
					}
				}
				
				obsCount = obs.getEncounter().getObs().size();
				assertNotNull("Before test, could not find expected obs", obs);
				Context.getObsService().voidObs(obs, "Data entry error");
				
				assertEquals(obsCount - 1,obs.getEncounter().getObs().size() );
				obsCount = obs.getEncounter().getObs().size();
				
				Obs newObs = new Obs();
				newObs.setPerson(obs.getPerson());
				newObs.setConcept(obs.getConcept());
				newObs.setObsDatetime(obs.getObsDatetime());
				newObs.setLocation(obs.getLocation());
				newObs.setCreator(Context.getAuthenticatedUser());
				newObs.setDateCreated(new Date());
				newObs.setValueNumeric(99.9);
				newObs.setEncounter(obs.getEncounter());
				newObs = Context.getObsService().saveObs(newObs, null);
				obsCount++;
				
				int encId = newObs.getEncounter().getEncounterId();
				Context.evictFromSession(newObs.getEncounter());
				assertEquals(obsCount,Context.getEncounterService().getEncounter(encId).getObs().size());
			}
			public void runOnParent() {
				int encId = 0;
				Patient p = Context.getPatientService().getPatient(2);
				Obs obs = null;
				for (Obs o : Context.getObsService().getObservationsByPersonAndConcept(p, weight))
					if (OpenmrsUtil.compare(o.getObsDatetime(),d)==0) {
						obs = o;
						break;
					}
				assertNotNull(obs);
				assertEquals( (Double)99.9, obs.getValueNumeric());
				encId = obs.getEncounter().getEncounterId();				
				Context.evictFromSession(obs.getEncounter());
				assertEquals(obsCount,Context.getEncounterService().getEncounter(encId).getObs().size());
			}
		});
	}
		
	@Test
	@NotTransactional
	public void shouldCreatePatient() throws Exception {
		
		runSyncTest(new SyncTestHelper() {
			public void runOnChild() {
				Location loc = Context.getLocationService().getLocation("Someplace");
				PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(2);
				if (pit.getUuid() == null)
					throw new RuntimeException("pit.uuid is null! " + pit);
				else
					log.info("pit.uuid = " + pit.getUuid() + " , pit = " + pit);
				Patient p = new Patient();
				p.addName(new PersonName("Darius", "Graham", "Jazayeri"));
				p.addIdentifier(new PatientIdentifier("999", pit, loc));
				p.setGender("m");
				p.setBirthdate(new Date());
				Context.getPatientService().savePatient(p);
				List<PatientIdentifier> ids = Context.getPatientService().getPatientIdentifiers("999", pit);
				assertNotNull(ids);
				if (ids.size() != 1)
					assertFalse("Can't find patient we just created. ids.size()==" + ids.size(), true);
				log.info("Patients at end " + Context.getPatientService().findPatients("Darius", false).size());
			}
			public void runOnParent() {
				log.info("Patients at beginning " + Context.getPatientService().findPatients("Darius", false).size());
				Location loc = Context.getLocationService().getLocation("Someplace");
				PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(2);
				PersonName name = new PersonName("Darius", "Graham", "Jazayeri");
				PatientIdentifier id = new PatientIdentifier("999", pit, loc);

				List<PatientIdentifier> ids = Context.getPatientService().getPatientIdentifiers("999", pit);
				assertNotNull(ids);
				if (ids.size() != 1)
					assertFalse("Should only find one patient, not " + ids.size(), true);
				
				// check the name
				Patient p = ids.get(0).getPatient();				
				assertEquals(p.getPersonName().toString(), name.toString());
				
				// check the identifier
				PatientIdentifier firstpid = p.getIdentifiers().iterator().next();
				assertEquals(firstpid.getIdentifier(), id.getIdentifier());
				assertEquals(firstpid.getIdentifierType(), id.getIdentifierType());
			}
		});
	}

	@Test
	@NotTransactional
	public void shouldCreatePatientFromExistingUser() throws Exception {
		
		
		runSyncTest(new SyncTestHelper() {
			String uuid = null;
			public void runOnChild() {
								
				Location loc = Context.getLocationService().getLocation("Someplace");
				PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(2);
				ArrayList<PatientIdentifierType> pits = new ArrayList<PatientIdentifierType>(); pits.add(pit);
				
				if (pit.getUuid() == null)
					throw new RuntimeException("pit.uuid is null! " + pit);
				else
					log.info("pit.uuid = " + pit.getUuid() + " , pit = " + pit);
				
				Person person = Context.getPersonService().getPerson(5);
				Context.clearSession(); // so that this Person doesn't cause hibernate to think the new Patient is in the cache already (only needed until #725 is fixed)
				Patient p = new Patient(person);
				p.setCreator(Context.getUserService().getUser(1));
				p.setDateCreated(Context.getUserService().getUser(1).getDateCreated());
				
				p.addIdentifier(new PatientIdentifier("999", pit, loc));
				Context.getPatientService().savePatient(p);

                Assert.assertEquals(1, p.getIdentifiers().size());
                Assert.assertEquals("999", p.getPatientIdentifier().getIdentifier());

				List<PatientIdentifier> ids = Context.getPatientService().getPatientIdentifiers("999",pits, null, null, null);
				assertNotNull(ids);
				
				this.uuid = p.getUuid();
				
				if (ids.size() != 1)
					assertFalse("Can't find patient we just created. ids.size()==" + ids.size(), true);
				log.info("Patients at end " + Context.getPatientService().getPatients("Stub").size());
			}
			public void runOnParent() {
				
				//check by pat identifier
				Location loc = Context.getLocationService().getLocation("Someplace");
				PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(2);
				ArrayList<PatientIdentifierType> pits = new ArrayList<PatientIdentifierType>(); pits.add(pit);
				PatientIdentifier id = new PatientIdentifier("999", pit, loc);
				List<PatientIdentifier> ids = Context.getPatientService().getPatientIdentifiers("999",pits, null, null, null);
				assertNotNull(ids);
				if (ids.size() != 1)
					assertFalse("Should only find one patient, not " + ids.size(), true);
								
				// fetch by uuid
				Patient p = Context.getPatientService().getPatientByUuid(this.uuid);
				assertNotNull(p);
				
				//fetch by name
				assertEquals(1,Context.getPatientService().getPatients("Stub").size());

			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldEditPatient() throws Exception {
		runSyncTest(new SyncTestHelper() {
			PatientIdentifierType pit;
			public void runOnChild() {
				pit = Context.getPatientService().getPatientIdentifierType(2);
				Location loc = Context.getLocationService().getLocation("Someplace");
				Patient p = Context.getPatientService().getPatient(2);
				p.setGender("F");
				p.removeName(p.getPersonName());
				p.addName(new PersonName("Peter", null, "Parker"));
				p.addIdentifier(new PatientIdentifier("super123", pit, loc));
				Context.getPatientService().savePatient(p);
			}
			public void runOnParent() {
				Patient p = Context.getPatientService().getPatient(2);
				assertEquals("Gender didn't change", p.getGender(), "F");
				assertEquals("Name should be Peter Parker", p.getPersonName().toString(), "Peter Parker");
				boolean found = false;
				for (PatientIdentifier id : p.getIdentifiers())
					if (id.getIdentifier().equals("super123") && id.getIdentifierType().equals(pit))
						found = true;
				assertTrue("Couldn't find new ID", found);
			}
		});
	}

	@Test
	@NotTransactional
	public void shouldEditPatientIdentifiers() throws Exception {
		runSyncTest(new SyncTestHelper() {
			PatientIdentifierType pit;
			public void runOnChild() {
				pit = Context.getPatientService().getPatientIdentifierType(2);
				Location loc = Context.getLocationService().getLocation("Someplace");
				Patient p = Context.getPatientService().getPatient(2);
				p.setGender("F");
				p.removeName(p.getPersonName());
				p.addName(new PersonName("Peter", null, "Parker"));
				p.addIdentifier(new PatientIdentifier("super123", pit, loc));
				// don't save here, in real-world situations savePatient is only called once per transaction
				//Context.getPatientService().savePatient(p);
				
				/* patient 2 has two existing IDs: 
				 * "ID1234" with uuid of '6b920848-1b90-102b-8082-d8cf852a43d2' - this should be removed
				 * "ID1234-2" with uuid of 'e333cc28-6fef-11df-83c8-3679bc4524e5' - this should be voided
				 */
				Set<PatientIdentifier> pis = p.getIdentifiers();
				PatientIdentifier piToDelete = null;
				PatientIdentifier piToVoid = null;
				for(PatientIdentifier pi : pis) {
					if ("ID1234".equals(pi.getIdentifier())) {
						piToDelete = pi;
					} else if ("ID1234-2".equals(pi.getIdentifier())) {
						piToVoid = pi;
					};
				}
				
				//whack ID1234 by removing it from the patient
				p.removeIdentifier(piToDelete);
				//SyncUtil.deleteOpenmrsObject(piToDelete);

				//change ID1234-2 to void and save it via syncUtil to test resorting the treeset
				piToVoid.setVoided(true);
				piToVoid.setVoidReason("testing sync");				
				//SyncUtil.updateOpenmrsObject(piToVoid, "org.openmrs.PatientIdentifier", piToVoid.getUuid());
				Context.getPatientService().savePatient(p);
			}
			public void runOnParent() {
				Patient p = Context.getPatientService().getPatient(2);
				assertEquals("Gender didn't change", p.getGender(), "F");
				assertEquals("Name should be Peter Parker", p.getPersonName().toString(), "Peter Parker");
				boolean foundNew = false;
				boolean foundOld = false;
				for (PatientIdentifier id : p.getIdentifiers()) {
					if (id.getIdentifier().equals("super123") && id.getIdentifierType().equals(pit))
						foundNew = true;
					else if (id.getIdentifier().equals("ID1234") && id.getUuid().equals("6b920848-1b90-102b-8082-d8cf852a43d2"))
						foundOld = true;
				}
		
				assertTrue("Couldn't find new ID", foundNew);
				assertFalse("Still found old ID that should have been removed.", foundOld);
			}
		});
	}
	
	
	@Test
	@NotTransactional
	public void shouldEditPatientName() throws Exception {
		runSyncTest(new SyncTestHelper() {
			int numberBefore;
			public void runOnChild() {
				Patient p = Context.getPatientService().getPatient(2);
				numberBefore = p.getNames().size();
				p.getPersonName().setGivenName("Superman");
				Context.getPatientService().savePatient(p);
			}
			public void runOnParent() {
				Patient p = Context.getPatientService().getPatient(2);
				assertEquals("Should not have added a new name", numberBefore, p.getNames().size());
				assertEquals("Name should be Superman", "Superman", p.getPersonName().getGivenName());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldMergePatients() throws Exception {
		runSyncTest(new SyncTestHelper() {
			Set<PatientIdentifier> pis = null;
			int p1AddressCount = 0;
			int p1EncounterCount = 0;
			int p1ObsCount = 0;
			int p2AddressCount = 0;
			int p2EncounterCount = 0;
			int p2IdentifiersCount = 0;
			int p2ObsCount = 0;
			public void runOnChild() {
								
				Patient p1 = Context.getPatientService().getPatient(2);
				Patient p2 = Context.getPatientService().getPatient(3);
				p1AddressCount = p1.getAddresses().size();
				p1EncounterCount = Context.getEncounterService().getEncountersByPatient(p1).size();
				p1ObsCount = Context.getObsService().getObservationsByPerson(p1).size();
				p2AddressCount = p2.getAddresses().size();
				p2EncounterCount = Context.getEncounterService().getEncountersByPatient(p2).size();
				p2ObsCount = Context.getObsService().getObservationsByPerson(p2).size();
				pis = new TreeSet<PatientIdentifier>();
				pis.addAll(p1.getIdentifiers());
				pis.addAll(p2.getIdentifiers());
				
				try {
					Context.getPatientService().mergePatients(p2, p1);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			public void runOnParent() {
				Patient p1 = Context.getPatientService().getPatient(2);
				Patient p2 = Context.getPatientService().getPatient(3);
				Set<PatientIdentifier> pis2 = p2.getIdentifiers();
				for(PatientIdentifier pi : pis) {
					boolean matched = false;
					for (PatientIdentifier pi2 : pis2) {
						if (pi.getIdentifier().equalsIgnoreCase(pi2.getIdentifier())) {
							matched = true;
							break;
						}
					}
					if (!matched) {
						assertEquals("patient identifier not found after merge: " + pi.toString(), false);
					}
				}
				assertEquals("Patient not voided after merge",true, p1.isVoided());
				assertEquals("Encounters count wrong",p1EncounterCount+p2EncounterCount, Context.getEncounterService().getEncountersByPatient(p2).size());
				assertEquals("Address count wrong",p1AddressCount+p2AddressCount, p2.getAddresses().size());
				assertEquals("Obs count wrong",p1ObsCount+p2ObsCount, Context.getObsService().getObservationsByPerson(p2).size());
				
			}
		});
	}
	
	
}
