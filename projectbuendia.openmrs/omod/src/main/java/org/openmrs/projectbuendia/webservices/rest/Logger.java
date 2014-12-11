package org.openmrs.projectbuendia.webservices.rest;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kpy on 12/11/14.
 */
public class Logger {
    private Map<String, Date> startTimes = new HashMap<String, Date>();
    private String filename;

    public Logger(String filename) {
        this.filename = filename;
    }

    public void log(String message) {
        log(new Date(), message);
    }

    public void log(Date time, String message) {
        try {
            PrintWriter w = new PrintWriter(new FileWriter(filename, true /* append */));
            w.println("" + time + ": " + message);
            w.close();
        } catch (IOException e) {
        }
    }

    public void start(String key) {
        Date now = new Date();
        startTimes.put(key, now);
        log(now, "start: " + key);
    }

    public void end(String key) {
        Date end = new Date();
        Date start = startTimes.get(key);
        if (start != null) {
            log("end: " + key + " (" + (end.getTime() - start.getTime()) + " ms)");
        }
    }
}
