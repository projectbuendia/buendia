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
import static org.junit.Assert.assertNotSame;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.test.annotation.NotTransactional;

/**
 *
 */
public class SyncFormTest extends SyncBaseTest {

	/**
	 * @see SyncBaseTest#getInitialDataset()
	 */
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
	public void shouldEditFormMetadata() throws Exception {
		runSyncTest(new SyncTestHelper() {
			String newDescription = "Awesome new description";
			public void runOnChild() {
				Form form = Context.getFormService().getForm(1);
				form.setDescription(newDescription);
				Context.getFormService().saveForm(form);
			}
			public void runOnParent() {
				Form form = Context.getFormService().getForm(1);
				assertEquals("Description did not change", form.getDescription(), newDescription);
			}
		});
	}
	
	@Test
    @NotTransactional
	public void shouldDuplicateForm() throws Exception {
		runSyncTest(new SyncTestHelper() {
			String newName = "A new form";
			String newDescription = "Awesome new description";
			int numFields;
			int numFormsBefore;
			public void runOnChild() {
				numFormsBefore = Context.getFormService().getAllForms().size();
				Form form = Context.getFormService().getForm(1);
				numFields = form.getFormFields().size();
				assertNotSame("Form should have some fields", numFields, 0);
				Form dup = Context.getFormService().duplicateForm(form);
				dup.setName(newName);
				dup.setDescription(newDescription);
				Context.getFormService().saveForm(dup);
			}
			public void runOnParent() {
				assertEquals("Should now have N+1 forms", Context.getFormService().getAllForms().size(), numFormsBefore + 1);
				Form form = null;
				for (Form f : Context.getFormService().getAllForms())
					if (f.getName().equals(newName))
						form = f;
				assertNotNull("Couldn't find form", form);
				assertEquals("Name is wrong", form.getName(), newName);
				assertEquals("Description is wrong", form.getDescription(), newDescription);
				assertEquals("Wrong fields", form.getFormFields().size(), numFields);
			}
		});
	}

	@Test
    @NotTransactional
	public void shouldAddFieldToForm() throws Exception {
		runSyncTest(new SyncTestHelper() {
			FormService fs = Context.getFormService();
			int numFieldsBefore;
			String name = "LookAtMe";
			public void runOnChild() {
				Concept weight = Context.getConceptService().getConceptByName("WEIGHT");
				
				Field field = new Field();
				field.setConcept(weight);
				field.setFieldType(fs.getFieldType(1));
				field.setName(name);
				fs.saveField(field);
				
				Form form = Context.getFormService().getForm(1);
				numFieldsBefore = form.getFormFields().size();
				FormField ff = new FormField();
				ff.setField(field);
				ff.setFieldNumber(99);
				ff.setPageNumber(55);
				form.addFormField(ff);
				fs.saveForm(form);
			}
			public void runOnParent() {
				Concept weight = Context.getConceptService().getConceptByName("WEIGHT");
				
				Form form = Context.getFormService().getForm(1);
				assertEquals("Added new field", form.getFormFields().size(), numFieldsBefore + 1);
				int numTheSame = 0;
				for (FormField ff : form.getFormFields()) {
					Field f = ff.getField();
					
					if ( (f.getConcept() != null && f.getConcept().equals(weight)) &&
							(ff.getFieldNumber() != null && ff.getFieldNumber() == 99) &&
							(ff.getPageNumber() != null && ff.getPageNumber() == 55) &&
							name.equals(f.getName()) ) {
						++numTheSame;
					}
				}
				assertEquals(numTheSame, 1);
			}
		});
	}
	
	@Test
    @NotTransactional
	public void shouldSaveFieldWithoutSendingAllFormData() throws Exception {
		runSyncTest(new SyncTestHelper() {
			FormService fs = Context.getFormService();
			String name = "New Name for Field";
			public void runOnChild() {
				// a field that is associated with formid=1
				Field field = fs.getField(2);
				field.setName(name);
				fs.saveField(field);
			}
			public void changedBeingApplied(List<SyncRecord> records, Record record) {
				for (SyncRecord syncRecord : records) {
					if (syncRecord.getContainedClassSet().contains("org.openmrs.Form")) {
						Assert.fail("Form objects should not be in the sync records for simply Field saves");
					}
				}
			}
			public void runOnParent() {
				Field field = fs.getField(2);
				assertEquals("The name should have changed", name, field.getName());
			}
		});
	}

}
