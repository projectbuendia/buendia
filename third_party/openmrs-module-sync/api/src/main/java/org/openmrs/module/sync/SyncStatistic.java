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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class SyncStatistic {
	
	public enum Type {
	    SYNC_RECORD_COUNT_BY_STATE,
	    
	    /**
	     * The last time this server was sync'd was greater than 3 days ago.
	     * The admin should look into why this is and fix it. 
	     */
	    LAST_SYNC_REALLY_LONG_TIME_AGO,
	    
	    /**
	     * The last time the server was sync'd was between 1-3 days.  This should
	     * be looked into by an admin and fixed
	     */
	    LAST_SYNC_TIME_SOMEWHAT_TROUBLESOME,
	    
	    /**
	     * The number of records waiting to be sent out to a specific server
	     */
	    SYNC_RECORDS_PENDING_COUNT,
	    OTHER
	}
	
    public static final long serialVersionUID = 213243L;
    private Log log = LogFactory.getLog(this.getClass());
 
    // Fields
    protected Type type = null;
    protected String name = null;
    protected Object value = null;

    //Constructors
    public SyncStatistic(){}
    public SyncStatistic(Type type){this.setType(type);}
    public SyncStatistic(Type type,String name){this.setType(type);this.setName(name);}
    public SyncStatistic(Type type,String name, Object value){this.setType(type);this.setName(name);this.setValue(value);}
    
    // Properties
    public Type getType() {
        return type;
    }
    public void setType(Type v) {
        type = v;
    }	

    public String getName() {
        return name;
    }
    public void setName(String v) {
        name = v;
    }	

    public Object getValue() {
        return value;
    }
    public void setValue(Object v) {
        value = v;
    }	
}
