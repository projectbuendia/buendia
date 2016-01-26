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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Controller
public class ViewRecordController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/module/sync/viewrecord", method = RequestMethod.GET)
	public void showThePage(ModelMap modelMap, HttpServletRequest request, 
							@RequestParam String uuid,
							@RequestParam(value="action", required=false) String action) throws Exception {
    	
    	// default empty Object
        SyncRecord record =null;
        
        SyncRecord previousRecord = null;
    	SyncRecord nextRecord = null;
    	
        // only fill the Object if the user has authenticated properly
        if (Context.isAuthenticated()) {
        	SyncService syncService=Context.getService(SyncService.class);
        	
        	record = syncService.getSyncRecord(uuid);
        	
        	if (record != null) {
				previousRecord = syncService.getPreviousRecord(record);
				nextRecord = syncService.getNextRecord(record);
	        	
				if("reset".equals(action)){
					record.setRetryCount(0);
					record.setState(SyncRecordState.NEW);
					
					for(SyncServerRecord serverRecord : record.getServerRecords()){
						serverRecord.setState(SyncRecordState.NEW);
					}        				
					
					syncService.updateSyncRecord(record);
					
				}
				else if("remove".equals(action)){
					record.setRetryCount(0);
					record.setState(SyncRecordState.NOT_SUPPOSED_TO_SYNC);
					
					for(SyncServerRecord serverRecord : record.getServerRecords()){
						serverRecord.setState(SyncRecordState.NOT_SUPPOSED_TO_SYNC);
					}
					
					syncService.updateSyncRecord(record);
				}
	        
	    	
				List<SyncItem> syncItems=new ArrayList<SyncItem>();
				Map<Object, String> itemTypes = new HashMap<Object, String>();
				Map<Object, String> itemUuids = new HashMap<Object, String>();
				
				if (record != null) {
						
					String mainClassName = null;
					String mainuuid = null;
					String mainState = null;
					
					for (SyncItem item : record.getItems()) {
						String syncItem = item.getContent();
						syncItems.add(item);
						if (mainState == null)mainState = item.getState().toString();
						Record xml = Record.create(syncItem);
						Item root = xml.getRootItem();
						String className = root.getNode()
						                       .getNodeName()
						                       .substring("org.openmrs.".length());
						itemTypes.put(item.getKey().getKeyValue(), className);
						if (mainClassName == null)
							mainClassName = className;
						
			
						// String itemInfoKey = itemInfoKeys.get(className);
			
						// now we have to go through the item child nodes to find the
						// real uuid that we want
						NodeList nodes = root.getNode().getChildNodes();
						for (int i = 0; i < nodes.getLength(); i++) {
							Node n = nodes.item(i);
							String propName = n.getNodeName();
							if (propName.equalsIgnoreCase("uuid")) {
								String tmpuuid = n.getTextContent();
								itemUuids.put(item.getKey().getKeyValue(), tmpuuid);
								if (mainuuid == null)
									mainuuid = tmpuuid;
							}
						}
					}
					String displayName = "";
					try {
						displayName = SyncUtil.displayName(mainClassName, mainuuid);
					} catch (Exception e) {
						// some methods like Concept.getName() throw Exception s all the
						// time...
						displayName = "";
					}
					
					modelMap.put("displayName", displayName);
					modelMap.put("mainClassName", mainClassName);
					modelMap.put("mainState", mainState);
				}
				
				modelMap.put("nextRecord", nextRecord);
				modelMap.put("previousRecord", previousRecord);
				modelMap.put("record", record);
				modelMap.put("syncItems", syncItems);
				modelMap.put("itemsNumber", syncItems.size());
				modelMap.put("itemTypes", itemTypes);
				modelMap.put("itemuuids", itemUuids);
				modelMap.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
        	}
        }
        
    }
    
}
