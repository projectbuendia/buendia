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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Controller behind the history page showing all sync'd items.
 */
@Controller
public class HistoryListController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	public static abstract class Views {
		
		public static final String HISTORY = "/module/sync/history";
		
		public static final String HISTORY_ERROR = "/module/sync/historyNextError";

        public static final String RECENT_ALL_COMMITTED = "/module/sync/historyRecentAllCommitted";
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = Views.HISTORY, method = RequestMethod.GET)
	public void showThePage(ModelMap modelMap,
	                        @RequestParam(value = "firstRecordId", required = false) Integer firstRecordId,
	                        @RequestParam(value = "size", required = false) Integer size,
	                        @RequestParam(value = "state", required = false) String state) throws Exception {
		
		SyncRecord latestRecord = null;
		SyncRecord earliestRecord  = null;
		// default the list size to 20 items
		if (size == null) {
			AdministrationService as = Context.getAdministrationService();
			String max = as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS,
			    SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT_DEFAULT);
			size = Integer.valueOf(max);
		}
		
		log.debug("Vewing history page with size: " + size);
		
		List<SyncRecord> recordList = null;
		
		// only fill the record list if the user has authenticated properly
		if (Context.isAuthenticated()) {
			SyncService ss = Context.getService(SyncService.class);
			if (!StringUtils.hasText(state)) 
				recordList = ss.getSyncRecords(firstRecordId, size);
			else {
				SyncRecordState[] states = new SyncRecordState[] { SyncRecordState.valueOf(state) };
				recordList = ss.getSyncRecords(states, size, firstRecordId);
			}
			latestRecord = ss.getLatestRecord();
			earliestRecord = ss.getEarliestRecord(null);
			
		}
		
		if (recordList == null)
			recordList = Collections.emptyList();
		
		Map<String, String> recordTypes = new HashMap<String, String>();
		Map<Object, String> itemTypes = new HashMap<Object, String>();
		Map<Object, String> itemUuids = new HashMap<Object, String>();
		Map<String, String> recordText = new HashMap<String, String>();
		Map<String, String> recordChangeType = new HashMap<String, String>();
		
		// for paging to work, set the firstRecordId as the current first item in the list
		if (recordList.size() > 0) {
			firstRecordId = recordList.get(0).getRecordId();
		}
		
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
			if (mainClassName.indexOf("Persistent") >= 0)
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
		
		modelMap.put("syncRecords", recordList);
		
		modelMap.put("recordTypes", recordTypes);
		modelMap.put("itemTypes", itemTypes);
		modelMap.put("itemUuids", itemUuids);
		modelMap.put("recordText", recordText);
		modelMap.put("recordChangeType", recordChangeType);
		
		modelMap.put("parent", Context.getService(SyncService.class).getParentServer());
		modelMap.put("servers", Context.getService(SyncService.class).getRemoteServers());
		modelMap.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
		
		modelMap.put("firstRecordId", firstRecordId);
		
		if(latestRecord != null)
		modelMap.put("latestRecordId", latestRecord.getRecordId());
		
		if(earliestRecord != null){
			if(earliestRecord.getRecordId() == recordList.get(recordList.size() -1).getRecordId())
				modelMap.put("isEarliestRecord", "true");	
		}
		modelMap.put("size", size);
	}
	
	@RequestMapping(value = Views.HISTORY_ERROR, method = RequestMethod.GET)
	public String historyNextError(@RequestParam("recordId") Integer recordId, @RequestParam("size") Integer size,
	                               HttpSession session) throws Exception {
		SyncService ss = Context.getService(SyncService.class);
		
		SyncRecord syncRecordInError = ss.getOlderSyncRecordInState(ss.getSyncRecord(recordId),
		    SyncConstants.SYNC_RECORD_ERROR_STATES);
		
		if (syncRecordInError != null) {
			recordId = syncRecordInError.getRecordId();
		} else {
			session.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.general.noNextError");
		}
		
		return "redirect:" + Views.HISTORY + ".list?firstRecordId=" + recordId + "&size=" + size;
	}

	@RequestMapping(value = "/module/sync/historyResetRemoveRecords", method = RequestMethod.GET)
	public String historyResetRemoveRecords(ModelMap modelMap,
											HttpServletRequest request,
											@RequestParam(value = "syncRecordUuids", required = false) String syncRecordUuids,
											@RequestParam(value = "serverRecordIds", required = false) String serverRecordIds,
											@RequestParam String action, @RequestParam Integer recordId,
											@RequestParam Integer size) throws Exception {

		if (Context.isAuthenticated()) {
			SyncService syncService = Context.getService(SyncService.class);

			SyncRecordState state = SyncRecordState.NEW;
			if (action.equals("remove")) {
				state = SyncRecordState.NOT_SUPPOSED_TO_SYNC;
			}

			if (serverRecordIds != null || syncRecordUuids != null) {
				Set<SyncRecord> recordsToUpdate = new HashSet<SyncRecord>();
				//Process records for parent server
				if (syncRecordUuids != null) {
					String[] uuidArray = syncRecordUuids.split(" ");
					for (String uuid : uuidArray) {

						SyncRecord record = syncService.getSyncRecord(uuid);
						if (record != null) {
							record.setRetryCount(0);
							record.setState(state);

							recordsToUpdate.add(record);
						}
					}
				}

				//Process records for child servers
				if (serverRecordIds != null) {
					String[] idArray = serverRecordIds.split(" ");
					for (String id : idArray) {

						SyncServerRecord serverRecord = syncService.getSyncServerRecord(Integer.valueOf(id));
						if (serverRecord != null) {
							serverRecord.setRetryCount(0);
							serverRecord.setState(state);

							recordsToUpdate.add(serverRecord.getSyncRecord());
						}
					}
				}

				//update the parent record so that the changes cascaded to the server records
				for (SyncRecord record : recordsToUpdate) {
					syncService.updateSyncRecord(record);
				}
			}
		}

		return "redirect:" + Views.HISTORY + ".list?firstRecordId=" + recordId + "&size=" + size;
	}

	@RequestMapping(value = Views.RECENT_ALL_COMMITTED, method = RequestMethod.GET)
	public String historyRecentAllCommitted(@RequestParam("recordId") Integer recordId, @RequestParam("size") Integer size,
											HttpSession session) throws Exception {

		int rId = Context.getService(SyncService.class).getMostRecentFullyCommittedRecordId();

		if (rId == -1) {
			session.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.general.noRecentAllCommitted");
			rId = recordId;
		} else {
			if(rId == recordId) {
				session.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.general.onRecentAllCommitted");
			}
		}
		return "redirect:" + Views.HISTORY + ".list?firstRecordId=" + rId + "&size=" + size;
	}
}
