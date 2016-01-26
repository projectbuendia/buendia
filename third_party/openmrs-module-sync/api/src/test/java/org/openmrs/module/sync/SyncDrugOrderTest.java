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

import junit.framework.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.springframework.test.annotation.NotTransactional;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class SyncDrugOrderTest extends SyncBaseTest {

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
	public void shouldCreateDrugOrder() throws Exception {

		if (TestUtil.isOpenmrsVersionAtLeast("1.10")) {
			log.warn("NOT RUNNING DRUG ORDER TESTS AGAINST 1.10.  TODO.");
			return;
		}

		runSyncTest(new SyncTestHelper() {			

			OrderService orderService = Context.getOrderService();

			public void runOnChild() throws Exception {

				Patient patient = Context.getPatientService().getPatient(new Integer(2));
				assertNotNull(patient);
				
				Drug drug = Context.getConceptService().getDrugByNameOrId("Advil");
				assertNotNull(drug);
								
				Concept concept = drug.getConcept();
				assertNotNull(concept);

				DrugOrder drugOrder = new DrugOrder();
				drugOrder.setDrug(drug);
				drugOrder.setConcept(concept);
				drugOrder.setPatient(patient);
				drugOrder.setDose(1.0);
				drugOrder.setInstructions("");				
				drugOrder.setStartDate(new Date());	
				drugOrder.setDateCreated(new Date());
				drugOrder.setVoided(new Boolean(false));

				Object orderType = OrderService.class.getMethod("getOrderType", Integer.class).invoke(orderService, 1);
				DrugOrder.class.getMethod("setOrderType", OrderType.class).invoke(drugOrder, orderType);
				DrugOrder.class.getMethod("setUnits", String.class).invoke(drugOrder, "tabs");
				DrugOrder.class.getMethod("setFrequency", String.class).invoke(drugOrder, "4 times per day");

				OrderService.class.getMethod("saveOrder", Order.class).invoke(orderService, drugOrder);

				List orders = (List)OrderService.class.getMethod("getDrugOrdersByPatient", Patient.class).invoke(orderService, patient);
				Assert.assertEquals(2, orders.size());
			}

			public void runOnParent() throws Exception {
				Patient patient = Context.getPatientService().getPatient(new Integer(2));
				List orders = (List)OrderService.class.getMethod("getDrugOrdersByPatient", Patient.class).invoke(orderService, patient);
				Assert.assertEquals(2, orders.size());
			}
		});
	}	

	@Test
    @NotTransactional	
	public void shouldUpdateDrugOrder() throws Exception {

		if (TestUtil.isOpenmrsVersionAtLeast("1.10")) {
			log.warn("NOT RUNNING DRUG ORDER TESTS AGAINST 1.10.  TODO.");
			return;
		}

		runSyncTest(new SyncTestHelper() {

			OrderService orderService = Context.getOrderService();

			public void runOnChild() throws Exception {
				DrugOrder order = (DrugOrder)OrderService.class.getMethod("getOrder", Integer.class, Class.class).invoke(orderService, 1, DrugOrder.class);
				Double d = (Double) DrugOrder.class.getMethod("getDose").invoke(order);
				Assert.assertFalse(d.doubleValue() == 10.0);
				DrugOrder.class.getMethod("setDose", Double.class).invoke(order, 10.0);
				OrderService.class.getMethod("saveOrder", Order.class).invoke(orderService, order);
				d = (Double) DrugOrder.class.getMethod("getDose").invoke(order);
				Assert.assertEquals(10.0, d.doubleValue());
			}

			public void runOnParent() throws Exception {
				DrugOrder order = (DrugOrder)OrderService.class.getMethod("getOrder", Integer.class, Class.class).invoke(orderService, 1, DrugOrder.class);
				Double d = (Double) DrugOrder.class.getMethod("getDose").invoke(order);
				Assert.assertEquals(10.0, d.doubleValue());
			}
		});
	}
}
