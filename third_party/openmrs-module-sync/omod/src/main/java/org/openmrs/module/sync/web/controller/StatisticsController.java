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
package org.openmrs.module.sync.web.controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.SyncConstants;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 *
 */
public class StatisticsController extends SimpleFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	public class SyncStatisticsCommand {
		
		private String datePattern;
		
		private Date fromDate;
		
		private Date toDate;
		
		public SyncStatisticsCommand() {
		}
		
		public Date getFromDate() {
			return fromDate;
		}
		
		public void setFromDate(Date fromDate) {
			this.fromDate = fromDate;
		}
		
		public Date getToDate() {
			return toDate;
		}
		
		public void setToDate(Date toDate) {
			this.toDate = toDate;
		}
		
		public String getDatePattern() {
			return datePattern;
		}
		
		public void setDatePattern(String datePattern) {
			this.datePattern = datePattern;
		}
		
	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		super.initBinder(request, binder);
		binder.registerCustomEditor(
		    java.util.Date.class,
		    new CustomDateEditor(new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
		        SyncConstants.PROPERTY_DATE_PATTERN, SyncConstants.DEFAULT_DATE_PATTERN)), true));
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		
		SyncStatisticsCommand command = new SyncStatisticsCommand();
		command.setDatePattern(Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_DATE_PATTERN,
		    SyncConstants.DEFAULT_DATE_PATTERN));
		command.setFromDate(null);
		command.setToDate(new Date());
		
		return command;
	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj,
	                                BindException errors) throws Exception {
		
		return showForm(request, response, errors);
	}
	
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request, Object obj, Errors errors) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		
		SyncService ss = Context.getService(SyncService.class);
		SyncStatisticsCommand command = (SyncStatisticsCommand) obj;
		
		Date startDate = command.getFromDate();
		Date endDate = command.getToDate();
		
		// Sync statistics 
		Long totalRecords = ss.getCountOfSyncRecords(null, startDate, endDate, null);

		Long synchronizedRecords = ss.getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.ALREADY_COMMITTED,
		    SyncRecordState.COMMITTED, SyncRecordState.COMMITTED_AND_CONFIRMATION_SENT);
		Long newRecords = ss.getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.NEW);
		Long pendingRecords = ss.getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.PENDING_SEND);
		Long sentRecords = ss.getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.SENT);
		Long sendFailedRecords = ss.getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.SEND_FAILED);
		Long ingestFailedRecords = ss.getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.FAILED);
		Long retriedRecords = ss.getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.SENT_AGAIN);
		Long failedStoppedRecords = ss
		        .getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.FAILED_AND_STOPPED);
		Long notSyncRecords = ss.getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.NOT_SUPPOSED_TO_SYNC);
		Long rejectedRecords = ss.getCountOfSyncRecords(null, startDate, endDate, SyncRecordState.REJECTED);
		
		// all "other" ones from some other state
		Long unknownstateRecords = totalRecords - synchronizedRecords - newRecords - pendingRecords - sentRecords
		        - sendFailedRecords - ingestFailedRecords - retriedRecords - failedStoppedRecords - notSyncRecords
		        - rejectedRecords;
		
		// reference statistics
		ret.put("totalRecords", totalRecords);
		ret.put("synchronizedRecords", synchronizedRecords);
		ret.put("newRecords", newRecords);
		ret.put("pendingRecords", pendingRecords);
		ret.put("sentRecords", sentRecords);
		ret.put("sendFailedRecords", sendFailedRecords);
		ret.put("ingestFailedRecords", ingestFailedRecords);
		ret.put("retriedRecords", retriedRecords);
		ret.put("failedStoppedRecords", failedStoppedRecords);
		ret.put("notSyncRecords", notSyncRecords);
		ret.put("rejectedRecords", rejectedRecords);
		ret.put("unknownstateRecords", unknownstateRecords);
		
		ret.put("parent", Context.getService(SyncService.class).getParentServer());
		
		return ret;
	}
	
}
