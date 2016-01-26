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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

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
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 */
public class StateController extends SimpleFormController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * 
     * This is called prior to displaying a form for the first time. It tells
     * Spring the form/command object to load into the request
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    protected Object formBackingObject(HttpServletRequest request)
            throws ServletException {
        // default empty Object
        List<SyncRecord> recordList = new ArrayList<SyncRecord>();

        // only fill the Object if the user has authenticated properly
        if (Context.isAuthenticated()) {
        	SyncService ss = Context.getService(SyncService.class);
            recordList.addAll(ss.getSyncRecords());
            log.warn("ALL SYNCRECORDS WERE ADDED SUCCESSFULLY \n");
        }

        return recordList;
    }

	@Override
    protected Map<String, Object> referenceData(HttpServletRequest request, Object obj, Errors errors) throws Exception {
		Map<String,Object> ret = new HashMap<String,Object>();
		
		Map<String,String> recordTypes = new HashMap<String,String>();
		Map<Object,String> itemTypes = new HashMap<Object,String>();
		Map<Object,String> itemGuids = new HashMap<Object,String>();
		Map<String,String> recordText = new HashMap<String,String>();
        Map<String,String> recordChangeType = new HashMap<String,String>();
        // Sync statistics 
        int totalRecords=0;
        int synchronizedRecords=0;
        int newRecords=0;
        int pendingRecords=0;
        int sentRecords=0;
        int sendFailedRecords=0;
        int ingestFailedRecords=0;
        int retriedRecords=0;
        int failedStoppedRecords=0;
        int notSyncRecords=0;
        int rejectedRecords=0;
        int unknownstateRecords=0;
        
        
        List<SyncRecord> recordList = (ArrayList<SyncRecord>)obj;
        totalRecords=recordList.size();
        // warning: right now we are assuming there is only 1 item per record
        for ( SyncRecord record : recordList ) {
            SyncRecordState state=record.getState();
        	if(state.isFinal())
        		synchronizedRecords++;
        	else if(state==SyncRecordState.NEW)
        		newRecords++;
        	else if(state==SyncRecordState.PENDING_SEND)
        		pendingRecords++;
        	else if(state==SyncRecordState.SENT)
        		sentRecords++;
        	else if(state==SyncRecordState.SEND_FAILED)
        		sendFailedRecords++;
        	else if(state==SyncRecordState.FAILED)
        		ingestFailedRecords++;
        	else if(state==SyncRecordState.SENT_AGAIN)
        		retriedRecords++;
        	else if(state==SyncRecordState.FAILED_AND_STOPPED)
        		failedStoppedRecords++;
        	else if(state==SyncRecordState.NOT_SUPPOSED_TO_SYNC)
        		notSyncRecords++;
        	else if(state==SyncRecordState.REJECTED)
        		rejectedRecords++;
        	else 
        		unknownstateRecords++;
        	
            String mainClassName = null;
            String mainGuid = null;
            String mainState = null;
            
			for ( SyncItem item : record.getItems() ) {
				String syncItem = item.getContent();
                mainState = item.getState().toString();
				Record xml = Record.create(syncItem);
				Item root = xml.getRootItem();
				String className = root.getNode().getNodeName().substring("org.openmrs.".length());
				itemTypes.put(item.getKey().getKeyValue(), className);
				if ( mainClassName == null ) mainClassName = className;
                
				//String itemInfoKey = itemInfoKeys.get(className);
				
				// now we have to go through the item child nodes to find the real GUID that we want
				NodeList nodes = root.getNode().getChildNodes();
				for ( int i = 0; i < nodes.getLength(); i++ ) {
					Node n = nodes.item(i);
					String propName = n.getNodeName();
					if ( propName.equalsIgnoreCase("guid") ) {
                        String guid = n.getTextContent();
						itemGuids.put(item.getKey().getKeyValue(), guid);
                        if ( mainGuid == null ) mainGuid = guid;
                    }
				}
			}

			// persistent sets should show something other than their mainClassName (persistedSet)
			if ( mainClassName.indexOf("Persistent") >= 0 ) mainClassName = record.getContainedClasses();
			
            recordTypes.put(record.getUuid(), mainClassName);
            recordChangeType.put(record.getUuid(), mainState);

            // refactored - CA 21 Jan 2008
            String displayName = "";
            try {
                displayName = SyncUtil.displayName(mainClassName, mainGuid);
            } catch ( Exception e ) {
            	// some methods like Concept.getName() throw Exception s all the time...
            	displayName = "";
            }
            if ( displayName != null ) if ( displayName.length() > 0 ) recordText.put(record.getUuid(), displayName);
        }
        
        // reference statistics
        ret.put("totalRecords",new Integer(totalRecords) );
        ret.put("synchronizedRecords", new Integer(synchronizedRecords));
        ret.put("newRecords",  new Integer(newRecords));
        ret.put("pendingRecords",  new Integer(pendingRecords));
        ret.put("sentRecords",  new Integer(sentRecords));
        ret.put("sendFailedRecords",  new Integer(sendFailedRecords));
        ret.put("ingestFailedRecords",  new Integer(ingestFailedRecords));
        ret.put("retriedRecords",  new Integer(retriedRecords));
        ret.put("failedStoppedRecords", new Integer(failedStoppedRecords));
        ret.put("notSyncRecords", new Integer(notSyncRecords));
        ret.put("rejectedRecords",  new Integer(rejectedRecords));
        ret.put("unknownstateRecords", new Integer(unknownstateRecords));
        
        ret.put("recordTypes", recordTypes);
        ret.put("itemTypes", itemTypes);
        ret.put("itemGuids", itemGuids);
        ret.put("recordText", recordText);
        ret.put("recordChangeType", recordChangeType);
        ret.put("parent", Context.getService(SyncService.class).getParentServer());
        ret.put("servers", Context.getService(SyncService.class).getRemoteServers());
        ret.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
        
        
	    return ret;
    }

}