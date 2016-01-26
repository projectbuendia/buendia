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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.notification.Alert;
import org.openmrs.notification.AlertService;
import org.springframework.test.annotation.NotTransactional;

/**
 * Tests sending AlertRecipient objects over the wire
 */
public class SyncAlertRecipientTest extends SyncBaseTest {

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
	public void shouldSaveAlertRecipients() throws Exception {
		runSyncTest(new SyncTestHelper() {

			public void runOnChild() throws Exception {
				User user = Context.getUserService().getUser(1);
				Alert alert = new Alert("Hello User #1!", user);
				Context.getAlertService().saveAlert(alert);
			}

			public void runOnParent() throws Exception {
				AlertService as = Context.getAlertService();
				List<Alert> alerts = as.getAlertsByUser(new User(1));
				Assert.assertEquals(1, alerts.size());
			}
		});
	}
}
