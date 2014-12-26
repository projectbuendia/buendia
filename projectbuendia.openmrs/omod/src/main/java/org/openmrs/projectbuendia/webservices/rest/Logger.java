package org.openmrs.projectbuendia.webservices.rest;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kpy on 12/11/14.
 */
public class Logger {
    private Map<String, Date> startTimes = new HashMap<String, Date>();
    private String filename;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Logger(String filename) {
        this.filename = filename;
    }

    public void log(String message) {
        log(new Date(), message);
    }

    public void log(Date time, String message) {
        try {
            PrintWriter w = new PrintWriter(new FileWriter(filename, true /* append */));
            w.println("\n\u001b[32m" + format.format(time) + "\u001b[0m " + message);
            w.close();
        } catch (IOException e) {
        }
    }

    public void start(String key, String message) {
        Date now = new Date();
        startTimes.put(key, now);
        message = "" + message;
        log(now, "-> " + key + (message.isEmpty() ? "" : ": " + message));
    }

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