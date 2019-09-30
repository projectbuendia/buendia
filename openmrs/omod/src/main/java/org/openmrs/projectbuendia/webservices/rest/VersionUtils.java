package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.projectbuendia.Utils;

public class VersionUtils {
    private static final Relation[] RELATIONS = {
        new Relation("0.15", "0.18"),
        new Relation("0.16", "0.19")
    };

    static class Relation {
        String serverVersion;
        String requiredMinimumClientVersion;

        public Relation(String server, String client) {
            serverVersion = server;
            requiredMinimumClientVersion = client;
        }
    }

    public static String getMinimumClientVersion(String serverVersion) {
        String result = null;
        for (Relation rel : RELATIONS) {
            if (Utils.ALPHANUMERIC_COMPARATOR.compare(
                serverVersion, rel.serverVersion) >= 0) {
                result = rel.requiredMinimumClientVersion;
            }
        }
        return result;
    }
}
