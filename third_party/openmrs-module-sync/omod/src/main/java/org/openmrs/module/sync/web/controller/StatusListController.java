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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncException;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncSource;
import org.openmrs.module.sync.SyncSourceJournal;
import org.openmrs.module.sync.SyncTransmission;
import org.openmrs.module.sync.SyncTransmissionState;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.SyncUtilTransmission;
import org.openmrs.module.sync.api.SyncIngestService;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.ingest.SyncDeserializer;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.ingest.SyncTransmissionResponse;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.web.WebConstants;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StatusListController extends SimpleFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		super.initBinder(request, binder);
	}
	
	/**
	 * The onSubmit function receives the form/command object that was modified by the input form
	 * and saves it to the db
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj,
	                                BindException errors) throws Exception {
		
		ModelAndView result = new ModelAndView(new RedirectView(getSuccessView()));
		
		// TODO - replace with privilege check
		if (!Context.isAuthenticated())
			throw new APIAuthenticationException("Not authenticated!");
		
		RemoteServer parent = null;
		HttpSession httpSession = request.getSession();
		String success = "";
		String error = "";
		MessageSourceAccessor msa = getMessageSourceAccessor();
		
		String action = ServletRequestUtils.getStringParameter(request, "action", "");
		
		SyncService syncService = Context.getService(SyncService.class);
		
		if ("resetAttempts".equals(action)) {
			List<SyncRecord> syncRecords = syncService.getSyncRecords(SyncRecordState.FAILED_AND_STOPPED);
			for (SyncRecord syncRecord : syncRecords) {
				syncRecord.setState(SyncRecordState.FAILED);
				syncRecord.setRetryCount(0);
				syncService.updateSyncRecord(syncRecord);
			}
			success = msa.getMessage("sync.status.transmission.reset.attempts.success", new Object[] { syncRecords.size() });
			result.addObject("mode", request.getParameter("mode"));
		} else if ("createTx".equals(action)) { // handle transmission generation
			try {
				parent = syncService.getParentServer();
				if (parent == null) {
					throw new SyncException("Could not retrieve information about the parent server; null returned.");
				}
				
				// we are creating a sync-transmission, so start by generating a SyncTransmission object
				// and this is a sychronization via file	due the value of action being createTx	
				SyncTransmission tx = SyncUtilTransmission.createSyncTransmission(parent, true,
				    SyncUtil.getGlobalPropetyValueAsInteger(SyncConstants.PROPERTY_NAME_MAX_RECORDS_FILE));
				String toTransmit = null; // the actual text that will be sent (either an ST or an STR)
				
				// Pull out the committed records from parent that haven't been sent back for confirmation
				// these are all the records we've received and are not sending a confirmation of receipt
				List<SyncImportRecord> syncImportRecords = syncService.getSyncImportRecords(SyncRecordState.COMMITTED, SyncRecordState.ALREADY_COMMITTED);
				
				SyncTransmissionResponse str = new SyncTransmissionResponse();
				str.setState(SyncTransmissionState.OK);
				str.setSyncImportRecords(syncImportRecords);
				//note: this is transmission *response* object: as in child's response to parent
				//so the target/source is viewed from 'parent' perspective, 
				//hence for responses the target ID contains the server that generated the response
				//in this case that means: the target uuid is current server (child) uuid and source uuid is the parent 
				str.setSyncSourceUuid(tx.getSyncTargetUuid());
				str.setSyncTargetUuid(tx.getSyncSourceUuid());
				str.setSyncTransmission(tx);
				str.setTimestamp(tx.getTimestamp());
				str.setUuid(tx.getUuid());
				str.setFileName(""); //of no relevance; we're not saving to file system
				
				str.createFile(false);
				toTransmit = str.getFileOutput();
				
				// Record last attempt
				parent.setLastSync(new Date());
				syncService.saveRemoteServer(parent);
				
				// Write text to response
				InputStream in = new ByteArrayInputStream(toTransmit.getBytes());
				response.setContentType("text/xml; charset=utf-8");
				response.setHeader("Content-Disposition", "attachment; filename=" + tx.getFileName() + ".xml");
				OutputStream out = response.getOutputStream();
				IOUtils.copy(in, out);
				out.flush();
				out.close();
				
				for (SyncImportRecord record : syncImportRecords) {
					record.setState(SyncRecordState.COMMITTED_AND_CONFIRMATION_SENT);
					syncService.updateSyncImportRecord(record);
				}
				
				// don't return a model/view - we'll need to return a file instead.
				result = null;
			}
			catch (Exception e) {
				e.printStackTrace();
				error = msa.getMessage("sync.status.createTx.error");
			}
		} else if ("uploadResponse".equals(action) && request instanceof MultipartHttpServletRequest) {
			
			try {
				String contents = "";
				parent = syncService.getParentServer();
				
				// first, get contents of file that is being uploaded.  it is clear we are uploading a response from parent at this point
				MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
				MultipartFile multipartSyncFile = multipartRequest.getFile("syncResponseFile");
				if (multipartSyncFile != null && !multipartSyncFile.isEmpty()) {
					InputStream inputStream = null;
					StringBuilder sb = new StringBuilder();
					
					try {
						inputStream = multipartSyncFile.getInputStream();
						BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, SyncConstants.UTF8));
						String line = "";
						while ((line = in.readLine()) != null) {
							sb.append(line);
						}
						contents = sb.toString();
					}
					catch (Exception e) {
						log.error("Unable to read in sync data file", e);
						error = e.getMessage();
					}
					finally {
						try {
							if (inputStream != null)
								inputStream.close();
						}
						catch (IOException io) {
							log.error("Unable to close temporary input stream", io);
						}
					}
				}
				
				if (contents.length() > 0) {
					SyncTransmissionResponse str = SyncDeserializer.xmlToSyncTransmissionResponse(contents);
					
					int numCommitted = 0;
					int numAlreadyCommitted = 0;
					int numFailed = 0;
					int numOther = 0;
					
					if (str.getSyncImportRecords() == null)
						log.debug("No records to process in response");
					else {
						// process each incoming syncImportRecord, this is just status update
						for (SyncImportRecord importRecord : str.getSyncImportRecords()) {
							Context.getService(SyncIngestService.class).processSyncImportRecord(importRecord, parent);
						}
					}
					
					try {
						// store this file on filesystem too
						str.createFile(false, SyncConstants.DIR_JOURNAL);
					}
					catch (Exception e) {
						log.error("Unable to create file to store SyncTransmissionResponse: " + str.getFileName());
						e.printStackTrace();
					}
					
					// now pull out the data that originated on the 'source' server and try to process it
					SyncTransmission st = str.getSyncTransmission();
					
					// now process the syncTransmission if one was received                    
					if (st != null) {
						str = SyncUtilTransmission.processSyncTransmission(st,
						    SyncUtil.getGlobalPropetyValueAsInteger(SyncConstants.PROPERTY_NAME_MAX_RECORDS_FILE));
						// get some numbers about what was just processed to show user the results
						if (str.getSyncImportRecords() != null) {
							for (SyncImportRecord importRecord : str.getSyncImportRecords()) {
								if (importRecord.getState().equals(SyncRecordState.COMMITTED))
									numCommitted++;
								else if (importRecord.getState().equals(SyncRecordState.ALREADY_COMMITTED))
									numAlreadyCommitted++;
								else if (importRecord.getState().equals(SyncRecordState.FAILED))
									numFailed++;
								else
									numOther++;
							}
						}
					}
					
					Object[] args = { numCommitted, numFailed, numAlreadyCommitted, numOther };
					
					success = msa.getMessage("sync.status.uploadResponse.success", args);
				} else {
					error = msa.getMessage("sync.status.uploadResponse.fileEmpty");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				error = msa.getMessage("sync.status.uploadResponse.error");
			}
		}
		
		if (!success.equals(""))
			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success);
		
		if (!error.equals(""))
			httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error);
		
		return result;
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		// default empty Object
		List<SyncRecord> recordList = new ArrayList<SyncRecord>();
		
		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			
			String mode = ServletRequestUtils.getStringParameter(request, "mode", "SEND_FILE");
			
			RemoteServer parent = Context.getService(SyncService.class).getParentServer();
			if (parent != null) {
				SyncSource source = new SyncSourceJournal();
				
				Integer maxConfigured = 0;
				if ("SEND_WEB".equals(mode))
					maxConfigured = SyncUtil.getGlobalPropetyValueAsInteger(SyncConstants.PROPERTY_NAME_MAX_RECORDS_WEB);
				else
					maxConfigured = SyncUtil.getGlobalPropetyValueAsInteger(SyncConstants.PROPERTY_NAME_MAX_RECORDS_FILE);
				Integer maxDefault = Integer.valueOf(SyncConstants.PROPERTY_NAME_MAX_RECORDS_DEFAULT);
				if (maxConfigured > maxDefault)
					maxConfigured = maxDefault; // limit to default value of 50 because we don't really need everything on this page
				
				recordList = source.getChanged(parent, maxConfigured);
			}
			
			//SyncService ss = Context.getService(SyncService.class);
			//recordList.addAll(ss.getSyncRecords());
		}
		
		return recordList;
	}
	
	@Override
	protected Map referenceData(HttpServletRequest request, Object obj, Errors errors) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		
		Map<String, String> recordTypes = new HashMap<String, String>();
		Map<Object, String> itemTypes = new HashMap<Object, String>();
		Map<Object, String> itemUuids = new HashMap<Object, String>();
		//Map<String,String> itemInfo = new HashMap<String,String>();
		//Map<String,String> itemInfoKeys = new HashMap<String,String>();
		Map<String, String> recordText = new HashMap<String, String>();
		Map<String, String> recordChangeType = new HashMap<String, String>();
		List<SyncRecord> recordList = (ArrayList<SyncRecord>) obj;
		
		//itemInfoKeys.put("Patient", "gender,birthdate");
		//itemInfoKeys.put("PersonName", "name");
		//itemInfoKeys.put("User", "username");
		
		for (SyncRecord record : recordList) {
			
			String mainClassName = null;
			String mainUuid = null;
			String mainState = null;
			
			for (SyncItem item : record.getItems()) {
				String syncItem = item.getContent();
				mainState = item.getState().toString();
				Record xml = Record.create(syncItem);
				Item root = xml.getRootItem();
				String className = root.getNode().getNodeName().substring("org.openmrs.".length());
				itemTypes.put(item.getKey().getKeyValue(), className);
				if (mainClassName == null)
					mainClassName = className;
				
				//String itemInfoKey = itemInfoKeys.get(className);
				
				// now we have to go through the item child nodes to find the real UUID that we want
				NodeList nodes = root.getNode().getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
					Node n = nodes.item(i);
					String propName = n.getNodeName();
					if (propName.equalsIgnoreCase("uuid")) {
						String uuid = n.getTextContent();
						itemUuids.put(item.getKey().getKeyValue(), uuid);
						if (mainUuid == null)
							mainUuid = uuid;
					}
				}
			}
			
			// persistent sets should show something other than their mainClassName (persistedSet)
			if (mainClassName.indexOf("Persistent") >= 0 || mainClassName.indexOf("Tree") >= 0)
				mainClassName = record.getContainedClasses();
			
			recordTypes.put(record.getUuid(), mainClassName);
			recordChangeType.put(record.getUuid(), mainState);
			
			// refactored - CA 21 Jan 2008
			String displayName = "";
			try {
				displayName = SyncUtil.displayName(mainClassName, mainUuid);
			}
			catch (Exception e) {
				// some methods like Concept.getName() throw Exception s all the time...
				displayName = "";
			}
			if (displayName != null)
				if (displayName.length() > 0)
					recordText.put(record.getUuid(), displayName);
			
		}
		
		// syncViaWeb error messages
		MessageSourceAccessor msa = getMessageSourceAccessor();
		Map<String, String> state = new HashMap<String, String>();
		state.put(SyncTransmissionState.AUTH_FAILED.toString(), msa.getMessage("sync.status.transmission.noAuthError"));
		state.put(SyncTransmissionState.CERTIFICATE_FAILED.toString(),
		    msa.getMessage("sync.status.transmission.noCertError"));
		state.put(SyncTransmissionState.CONNECTION_FAILED.toString(),
		    msa.getMessage("sync.status.transmission.noConnectionError"));
		state.put(SyncTransmissionState.MALFORMED_URL.toString(), msa.getMessage("sync.status.transmission.badUrl"));
		state.put(SyncTransmissionState.NO_PARENT_DEFINED.toString(),
		    msa.getMessage("sync.status.transmission.noParentError"));
		state.put(SyncTransmissionState.RESPONSE_NOT_UNDERSTOOD.toString(),
		    msa.getMessage("sync.status.transmission.corruptResponseError"));
		state.put(SyncTransmissionState.FAILED.toString(), msa.getMessage("sync.status.transmission.sendError"));
		state.put(SyncTransmissionState.TRANSMISSION_CREATION_FAILED.toString(),
		    msa.getMessage("sync.status.transmission.createError"));
		state.put(SyncTransmissionState.TRANSMISSION_NOT_UNDERSTOOD.toString(),
		    msa.getMessage("sync.status.transmission.corruptTxError"));
		state.put(SyncTransmissionState.OK_NOTHING_TO_DO.toString(),
		    msa.getMessage("sync.status.transmission.okNoSyncNeeded"));
		state.put(SyncTransmissionState.MAX_RETRY_REACHED.toString(),
		    msa.getMessage("sync.status.transmission.maxRetryReached"));
		state.put(SyncTransmissionState.FAILED_RECORDS.toString(), msa.getMessage("sync.status.transmission.failedRecords"));
		state.put(SyncTransmissionState.ERROR_CANNOT_RUN_PARALLEL.toString(),
		    msa.getMessage("sync.status.transmission.cannotRunParallel"));
		state.put(SyncTransmissionState.CANNOT_FIND_SERVER_WITH_UUID.toString(),
		    msa.getMessage("sync.status.transmission.cannotFindServerWithUuid"));
		state.put(SyncRecordState.ALREADY_COMMITTED.toString(), msa.getMessage("sync.record.state_ALREADY_COMMITTED"));
		state.put(SyncRecordState.COMMITTED.toString(), msa.getMessage("sync.record.state_COMMITTED"));
		state.put(SyncRecordState.FAILED.toString(), msa.getMessage("sync.record.state_FAILED"));
		state.put(SyncRecordState.FAILED_AND_STOPPED.toString(), msa.getMessage("sync.record.state_FAILED_AND_STOPPED"));
		state.put(SyncRecordState.NEW.toString(), msa.getMessage("sync.record.state_SENT"));
		state.put(SyncRecordState.PENDING_SEND.toString(), msa.getMessage("sync.record.state_SENT"));
		state.put(SyncRecordState.SEND_FAILED.toString(), msa.getMessage("sync.record.state_FAILED"));
		state.put(SyncRecordState.SENT.toString(), msa.getMessage("sync.record.state_SENT"));
		state.put(SyncRecordState.SENT_AGAIN.toString(), msa.getMessage("sync.record.state_SENT"));
		
		ret.put("mode", ServletRequestUtils.getStringParameter(request, "mode", "SEND_FILE"));
		ret.put("transmissionState", state.entrySet());
		
		ret.put("recordTypes", recordTypes);
		ret.put("itemTypes", itemTypes);
		ret.put("itemUuids", itemUuids);
		//ret.put("itemInfo", itemInfo);
		ret.put("recordText", recordText);
		ret.put("recordChangeType", recordChangeType);
		ret.put("parent", Context.getService(SyncService.class).getParentServer());
		ret.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
		
		return ret;
	}
}
