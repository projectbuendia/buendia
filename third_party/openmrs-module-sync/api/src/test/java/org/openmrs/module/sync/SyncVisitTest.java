/*
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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.springframework.test.annotation.NotTransactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SyncVisitTest extends SyncBaseTest {

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
    public void shouldSyncNewVisitWithNewEncounter() throws Exception {
        runSyncTest(new SyncTestHelper() {

            Date date = new Date();

            public void runOnChild() {

                Patient patient = Context.getPatientService().getPatient(2);
                Provider provider = Context.getProviderService().getProvider(1);
                Location location = Context.getLocationService().getLocation(1);
                EncounterRole encounterRole = Context.getEncounterService().getEncounterRole(1);
                EncounterType encounterType = Context.getEncounterService().getEncounterType(1);
                VisitType visitType = Context.getVisitService().getVisitType(1);


                // assert that we are starting with a patient with one encounter
                Assert.assertEquals(1, Context.getEncounterService().getEncountersByPatient(patient).size());


                Set<Encounter> encounters = new HashSet<Encounter>();

                // create a new encounter
                Encounter encounter = new Encounter();
                encounter.setProvider(encounterRole, provider);
                encounter.setEncounterDatetime(date);
                encounter.setPatient(patient);
                encounter.setLocation(location);
                encounter.setEncounterType(encounterType);
                encounters.add(encounter);

                // now create a visit for the encounter
                Visit visit = new Visit();
                visit.setVisitType(visitType);
                visit.setPatient(patient);
                visit.setStartDatetime(date);
                visit.setEncounters(encounters);

                // save the visit (and the encounter should cascade)
                Context.getVisitService().saveVisit(visit);

                // sanity check
                List<Visit> visits = Context.getVisitService().getVisitsByPatient(patient);

                Assert.assertNotNull(visits);
                Assert.assertEquals(1, visits.size());


            }


            public void runOnParent() {
                Patient patient = Context.getPatientService().getPatient(2);

                List<Encounter> encounters = Context.getEncounterService().getEncountersByPatient(patient);

                Assert.assertNotNull(encounters);
                Assert.assertEquals(2, encounters.size());


                List<Visit> visits = Context.getVisitService().getVisitsByPatient(patient);

                Assert.assertNotNull(visits);
                Assert.assertEquals(1, visits.size());

                Visit visit = visits.get(0);

                Assert.assertEquals(visit.getPatient(), patient);
                Assert.assertNotNull(visit.getEncounters());
                Assert.assertEquals(1, visit.getEncounters().size());

            }
        });
    }

}
