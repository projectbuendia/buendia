/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.xforms;

import org.openmrs.Form;

public class FormModuleHandler extends Form {

	private static final long serialVersionUID = 1L;

	private Form form;
	private String moduleId;
	private boolean appendModuleId;

	public FormModuleHandler(Form form, String moduleId) {
		this.form = form;
		this.moduleId = moduleId;
	}

	public String getName() {
		return form.getName() + (appendModuleId ? " - (" + moduleId + ")" : "");
	}

	public Integer getFormId() {
		return form.getFormId();
	}
	
	public Boolean getPublished() {
		return form.getPublished();
	}
	
	public Boolean getRetired() {
		return form.getRetired();
	}

	public boolean isAppendModuleId() {
		return appendModuleId;
	}

	public void setAppendModuleId(boolean appendModuleId) {
		this.appendModuleId = appendModuleId;
	}

	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = form;
	}
}
