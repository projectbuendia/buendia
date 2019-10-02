package org.openmrs.projectbuendia.webservices.rest;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.projectbuendia.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class VersionInfo {
    private final Relation[] RELATIONS = {
        new Relation("0.15", "0.18"),
        new Relation("0.16", "0.19")
    };

    public final String serverVersion;
    public final String minimumClientVersion;

    public VersionInfo() {
        GitProperties git = new GitProperties("git.properties");
        serverVersion = git.describe();
        minimumClientVersion = getMinimumClientVersion(git.nearestRelease);
    }

    public void addHeaders(HttpServletResponse response) {
        response.addHeader("Buendia-Server-Version", serverVersion);
        if (minimumClientVersion != null) {
            response.addHeader("Buendia-Client-Minimum-Version", minimumClientVersion);
        }
    }

    private String getMinimumClientVersion(String serverVersion) {
        String result = null;
        for (Relation rel : RELATIONS) {
            if (Utils.ALPHANUMERIC_COMPARATOR.compare(serverVersion, rel.serverVersion) >= 0) {
                result = rel.requiredMinimumClientVersion;
            }
        }
        return result;
    }

    static class Relation {
        String serverVersion;
        String requiredMinimumClientVersion;

        public Relation(String server, String client) {
            serverVersion = server;
            requiredMinimumClientVersion = client;
        }
    }

    static class GitProperties {
        String commitId = null;
        String commitTime = null;
        String nearestTag = null;
        String nearestRelease = null;
        int numCommitsAfterTag = -1;
        boolean dirty = false;

        GitProperties(String filename) {
            Map props = new HashMap();
            try {
                InputStream stream = getClass().getClassLoader().getResourceAsStream(filename);
                props = new ObjectMapper().readValue(stream, Map.class);
            } catch (IOException e) { }
            try {
                commitId = (String) props.get("commit.id.abbrev");
                commitTime = (String) props.get("commit.time");
                nearestTag = (String) props.get("closest.tag.name");
                nearestRelease = nearestTag.replaceAll("^v", "");
                numCommitsAfterTag = Integer.parseInt((String) props.get("closest.tag.commit.count"));
                dirty = Boolean.parseBoolean((String) props.get("dirty"));
            } catch (ClassCastException | NumberFormatException e) { }
        }

        String describe() {
            // TODO(ping): We are ignoring the "git.dirty" property because it
            // is always "true" even if the working tree is clean.  Figure out why.
            boolean release = numCommitsAfterTag == 0;
            String desc = release ? nearestRelease : String.format(
                "%s+%d (%s)", nearestRelease, numCommitsAfterTag, commitId
            );
            return String.format("%s [%s]", desc, commitTime);
        }
    }
}
