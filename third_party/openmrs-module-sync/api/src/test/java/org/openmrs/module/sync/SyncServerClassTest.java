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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.springframework.test.annotation.NotTransactional;

/**
 * This tests the exclusion of certain classes 
 */
public class SyncServerClassTest extends SyncBaseTest {

	@Override
    public String getInitialDataset() {
	    return "org/openmrs/module/sync/include/SyncRemoteChildServerRestricted.xml";
    }
	
	@Override
	public String getParentDataset() {
		return "org/openmrs/module/sync/include/SyncParentServer.xml";
	}
	
	@Override
	public String getChild2Dataset() {
		return "org/openmrs/module/sync/include/SyncRemoteChildServer2.xml";
	}
	
	@Test
	@NotTransactional
	public void shouldNotSendPersonAttributeType() throws Exception {
		runSyncTest(new SyncTestHelper() {
			PersonService ps = Context.getPersonService();
			PatientService patService = Context.getPatientService();
			
			String ATTR_TYPE_NAME = "A Person Attr type (skn3k2nfe)";
			String PAT_ID_TYPE_NAME = "A patient identifier type (asdfde)";
			public void runOnChild() throws Exception {
				PersonAttributeType attrType = new PersonAttributeType();
				attrType.setName(ATTR_TYPE_NAME);
				attrType.setDescription("An attr type for testing");
				attrType.setFormat("asdf");
				ps.savePersonAttributeType(attrType);
				
				PatientIdentifierType type = new PatientIdentifierType();
				type.setName(PAT_ID_TYPE_NAME);
				type.setDescription("a description");
				patService.savePatientIdentifierType(type);
				
				// sanity check, make sure they're at very least in the child server
				Assert.assertNotNull(ps.getPersonAttributeTypeByName(ATTR_TYPE_NAME));
				Assert.assertNotNull(patService.getPatientIdentifierTypeByName(PAT_ID_TYPE_NAME));
			}
			public void runOnParent() throws Exception {
				Assert.assertNull(ps.getPersonAttributeTypeByName(ATTR_TYPE_NAME));
				Assert.assertNotNull(patService.getPatientIdentifierTypeByName(PAT_ID_TYPE_NAME));
			}
			public void runOnChild2() throws Exception {
				Assert.assertNull(ps.getPersonAttributeTypeByName(ATTR_TYPE_NAME));
				Assert.assertNotNull(patService.getPatientIdentifierTypeByName(PAT_ID_TYPE_NAME));
			}
		});
	}

}
