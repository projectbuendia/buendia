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
package org.openmrs.module.sync.serialization;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

public class TimestampNormalizer extends Normalizer
{
    public static final String DATETIME_MASK = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DATETIME_MASK_BACKUP = "yyyy-MM-dd HH:mm:ss.S";  // because we are converting to strings in more than one way, so need a way to convert back
	public static final String DATETIME_DISPLAY_FORMAT = "dd MMM yyyy HH:mm:ss";
    
    public String toString(Object o) {
        
        java.sql.Date d;
        java.sql.Time t;
        long time;
        String result = null;

        if (o instanceof java.sql.Timestamp){ //this is how hibernate recreates Date objects
            SimpleDateFormat dfm = new SimpleDateFormat(TimestampNormalizer.DATETIME_MASK);
            result = dfm.format((Date)o);            
        }
        else if (o instanceof java.sql.Date){
            d = (java.sql.Date)o;
            t = new java.sql.Time(d.getTime());
            result = d.toString() + ' ' + t.toString();
        }
        else if (o instanceof java.util.Date){
            SimpleDateFormat dfm = new SimpleDateFormat(TimestampNormalizer.DATETIME_MASK);
            result = dfm.format((Date)o);;
        }
        else if (o instanceof java.util.Calendar){
            time = ((java.util.Calendar)o).getTime().getTime();
            d = new java.sql.Date(time);
            t = new java.sql.Time(time);
            result = d.toString() + ' ' + t.toString();
        }
        else {
            log.warn("Unknown class in timestamp " + o.getClass().getName());
            result = o.toString();//ugh
        }

        return result;
    }
    
    @Override
    public Object fromString(Class clazz, String s) {

        if (StringUtils.isBlank(s)) return null;
        
        if ("java.util.Date".equals(clazz.getName()) || "java.sql.Timestamp".equals(clazz.getName())) {
            //result = d.toString() + ' ' + t.toString();
            Date result = null;                          
            SimpleDateFormat dfm = new SimpleDateFormat(DATETIME_MASK);
            try {
                result = dfm.parse(s.trim());
            } catch (ParseException e) {
    			log.debug("DateParsingException trying to turn " + s + " into a date with pattern: " + dfm.toPattern() + " , so retrying with backup mask");
    			try {
    				dfm = new SimpleDateFormat(DATETIME_MASK_BACKUP);
    				result = dfm.parse(s.trim());
    			} catch (ParseException pee) {
    				log.debug("Still getting DateParsingException trying to turn " + s + " into a date using backup pattern: " + dfm.toPattern());
    			}
            }
            return result;
        }
        
        return null;
    }
}
