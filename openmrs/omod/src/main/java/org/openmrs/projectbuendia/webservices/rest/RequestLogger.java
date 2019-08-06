// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openmrs.module.webservices.rest.web.RequestContext;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/** Logs REST API requests in detail, with timings, to a directory of log files. */
public class RequestLogger {
    public static final RequestLogger LOGGER = new RequestLogger("/var/log/large/requests");

    /** The directory under which we write log files. */
    String dir;

    /** Map of log filenames to Logger objects. */
    Map<String, Logger> loggers = new HashMap<>();

    public RequestLogger(String dir) {
        new File(dir).mkdirs();
        this.dir = dir;
    }

    public void request(RequestContext context, Object instance, String method) {
        request(context, instance, method, null);
    }

    /** Emits a "start" line for an incoming request. */
    public void request(RequestContext context, Object instance, String method, Object input) {
        request(context, formatKey(instance, method), input != null ? "(" + input + ")" : "");
    }

    /** Emits a "start" line for an incoming request. */
    protected void request(RequestContext context, String key, String message) {
        try {
            HttpServletRequest request = context.getRequest();
            String filename = request.getRemoteAddr();
            start(filename, key, "\u001b[33m" + request.getMethod() + " "
                + request.getRequestURI() + "\u001b[0m " + message);
        } catch (Exception e) {
        }
    }

    /** Emits a "start" line for the given key to the given log. */
    protected void start(String filename, String key, String message) {
        getLogger(filename).start(key, message);
    }

    /** Gets or creates the Logger for a given filename. */
    protected Logger getLogger(String filename) {
        if (!loggers.containsKey(filename)) {
            loggers.put(filename, new Logger(dir + "/" + filename));
        }
        return loggers.get(filename);
    }

    /** Emits an "end" line for a successful reply. */
    public void reply(RequestContext context, Object instance, String method, Object result) {
        reply(context, formatKey(instance, method), result != null ? "" + result : "");
    }

    /** Emits an "end" line for a successful reply. */
    protected void reply(RequestContext context, String key, String message) {
        try {
            HttpServletRequest request = context.getRequest();
            String filename = request.getRemoteAddr();
            end(filename, key, message);
        } catch (Exception e) {
        }
    }

    /**
     * Emits an "end" line for the given key to the given log.  The Logger
     * records the elapsed time between start and end for each key.
     */
    protected void end(String filename, String key, String message) {
        getLogger(filename).end(key, message);
    }

    /** Emits an "end" line when an exception occurs. */
    public void error(RequestContext context, Object instance, String method, Exception e) {
        error(context, formatKey(instance, method), e);
    }

    /** Emits an "end" line when an exception occurs. */
    protected void error(RequestContext context, String key, Exception error) {
        try {
            HttpServletRequest request = context.getRequest();
            String filename = request.getRemoteAddr();
            end(filename, key, ExceptionUtils.getMessage(error) + ":\n"
                + ExceptionUtils.getStackTrace(error));
        } catch (Exception e) {
        }
    }
    
    protected String formatKey(Object instance, String method) {
        return instance.getClass().getSimpleName() + "." + method;
    }
}
