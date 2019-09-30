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

package org.projectbuendia.openmrs.webservices.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.webservices.rest.DbUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller for the REST resources in this module. This implicitly picks up
 * all the resources with the Resource annotation.
 */
@Controller
@RequestMapping("/rest/" + RestController.PATH)
public class RestController extends MainResourceController {
    public static final String PATH = "buendia";
    public static GitProperties git;

    private final Log log = LogFactory.getLog(getClass());

    public static class GitProperties {
        public String commitId = null;
        public String commitTime = null;
        public String nearestTag = null;
        public String nearestRelease = null;
        public int numCommitsAfterTag = -1;
        public boolean dirty = false;

        public String describe() {
            // TODO(ping): The "git.dirty" property is always "true" even if the
            // working tree is clean.  Figure out why.  For now, ignore "dirty".
            boolean release = numCommitsAfterTag == 0;
            String desc = release ? nearestRelease : String.format(
                "%s+%d (%s)", nearestRelease, numCommitsAfterTag, commitId
            );
            return String.format("%s [%s]", desc, commitTime);
        }
    }

    public RestController() {
        git = loadGitProperties();
    }

    private GitProperties loadGitProperties() {
        GitProperties git = new GitProperties();
        Map props = new HashMap();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream stream = classLoader.getResourceAsStream("git.properties");
            props = new ObjectMapper().readValue(stream, Map.class);
        } catch (IOException e) { }
        try {
            git.commitId = (String) props.get("git.commit.id.abbrev");
            git.commitTime = (String) props.get("git.commit.time");
            git.nearestTag = (String) props.get("git.closest.tag.name");
            git.nearestRelease = git.nearestTag.replaceAll("^v", "");
            git.numCommitsAfterTag = Integer.parseInt((String) props.get("git.closest.tag.commit.count"));
            git.dirty = Boolean.parseBoolean((String) props.get("git.dirty"));
        } catch (ClassCastException | NumberFormatException e) { }
        return git;
    }

    public SimpleObject get(@PathVariable("resource") String resource, HttpServletRequest request, HttpServletResponse response) throws ResponseException {
        // These parameters should not be treated as search criteria.
        RestConstants.SPECIAL_REQUEST_PARAMETERS.add("clear-cache");
        RestConstants.SPECIAL_REQUEST_PARAMETERS.add("locale");
        return super.get(resource, request, response);
    }

    @Override public String getNamespace() {
    return PATH;
    }

    @Override public SimpleObject handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (RestUtil.hasCause(e, APIAuthenticationException.class)) {
            return apiAuthenticationExceptionHandler(e, request, response);
        }

        ResponseStatus ann = (ResponseStatus) e.getClass().getAnnotation(ResponseStatus.class);
        int status = ann != null ? ann.value().value() : 500;
        response.setStatus(status);

        List<Map<String, Object>> errors = new ArrayList<>();
        for (Throwable t = e; t != null; t = t.getCause()) {
            errors.add(describeError(t));
        }
        return new SimpleObject().add("errors", errors);
    }

    private Map<String, Object> describeError(Throwable t) {
        ResponseStatus ann = (ResponseStatus) t.getClass().getAnnotation(ResponseStatus.class);
        String description = ann != null ? ann.reason() : null;
        String message = t.getMessage();

        StackTraceElement top = t.getStackTrace()[0];
        List<String> frames = new ArrayList<>();
        int maxLevels = 40;
        int level = 0;
        for (StackTraceElement frame : t.getStackTrace()) {
            frames.add(frame.toString());
            level++;
            if (level >= maxLevels) break;
            if (frame.getClassName().contains(".projectbuendia.")) {
                maxLevels = level + 1;  // show one more stack level
            }
        }

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (!Utils.isEmpty(description)) map.put("description", description);
        map.put("message", message);
        map.put("frames", frames);
        return map;
    }
}
