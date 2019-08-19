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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** Writes out timestamped HTTP request logs. */
public class Logger {
    private static final int MAX_STDERR_LINE_LENGTH = 480;
    private Map<String, Date> startTimes = new HashMap<String, Date>();
    private String filename;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static boolean SILENT = false;

    public Logger(String filename) {
        this.filename = filename;
    }

    /** Emits a message to the log, timestamped with the current time. */
    public void log(String message) {
        log(new Date(), message);
    }

    /** Emits a message to the log, timestamped with the specified time. */
    public void log(Date time, String message) {
        if (SILENT) return;

        try {
            PrintWriter w = new PrintWriter(new FileWriter(filename, true /* append */));
            w.println("\n\u001b[32m" + format.format(time) + "\u001b[0m " + message);
            w.close();
        } catch (IOException e) {
        }

        // Also print a truncated version of the message to stderr for regular logging.
        try {
            if (message.length() > MAX_STDERR_LINE_LENGTH) {
                message = message.substring(0, MAX_STDERR_LINE_LENGTH) + "...";
            }
            System.err.println("\u001b[32m" + format.format(time) + "\u001b[0m [HTTP] " + message);
        } catch (Throwable t) {
        }
    }

    /** Emits a message to the log, marking the start of a time interval. */
    public void start(String key, String message) {
        Date now = new Date();
        startTimes.put(key, now);
        message = "" + message;
        log(now, "-> " + key + (message.isEmpty() ? "" : ": " + message));
    }

    /** Emits a message to the log, marking the end of a time interval. */
    public void end(String key, String message) {
        Date end = new Date();
        Date start = startTimes.get(key);
        if (start != null) {
            message = "" + message;
            String elapsed = "" + (end.getTime() - start.getTime()) + " ms";
            log(end, "<- " + key + " (\u001b[36m" + elapsed + "\u001b[0m)"
                + (message.isEmpty() ? "" : ": " + message));
        }
    }
}
