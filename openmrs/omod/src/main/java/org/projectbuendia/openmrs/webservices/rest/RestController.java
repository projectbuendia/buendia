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
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.webservices.rest.VersionInfo;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final VersionInfo versionInfo;

    public RestController() {
        versionInfo = new VersionInfo();
    }

    @Override public String getNamespace() {
        return PATH;
    }

    @Override public Object retrieve(@PathVariable("resource") String resource, @PathVariable("uuid") String uuid, HttpServletRequest request, HttpServletResponse response) throws ResponseException {
        start(request, response);
        try { return super.retrieve(resource, uuid, request, response); }
        finally { finish(request, response); }
    }

    @Override public Object create(@PathVariable("resource") String resource, @RequestBody SimpleObject post, HttpServletRequest request, HttpServletResponse response) throws ResponseException {
        start(request, response);
        try { return super.create(resource, post, request, response); }
        finally { finish(request, response); }
    }

    @Override public Object update(@PathVariable("resource") String resource, @PathVariable("uuid") String uuid, @RequestBody SimpleObject post, HttpServletRequest request, HttpServletResponse response) throws ResponseException {
        start(request, response);
        try { return super.update(resource, uuid, post, request, response); }
        finally { finish(request, response); }
    }

    @Override public Object delete(@PathVariable("resource") String resource, @PathVariable("uuid") String uuid, @RequestParam(value = "reason",defaultValue = "web service call") String reason, HttpServletRequest request, HttpServletResponse response) throws ResponseException {
        start(request, response);
        try { return super.delete(resource, uuid, reason, request, response); }
        finally { finish(request, response); }
    }

    @Override public Object purge(@PathVariable("resource") String resource, @PathVariable("uuid") String uuid, HttpServletRequest request, HttpServletResponse response) throws ResponseException {
        start(request, response);
        try { return super.purge(resource, uuid, request, response); }
        finally { finish(request, response); }
    }

    @Override public SimpleObject get(@PathVariable("resource") String resource, HttpServletRequest request, HttpServletResponse response) throws ResponseException {
        start(request, response);
        try { return super.get(resource, request, response); }
        finally { finish(request, response); }
    }

    private void start(HttpServletRequest request, HttpServletResponse response) {
        // These parameters should not be treated as search criteria.
        RestConstants.SPECIAL_REQUEST_PARAMETERS.add("clear-cache");
        RestConstants.SPECIAL_REQUEST_PARAMETERS.add("locale");

        versionInfo.addHeaders(response);
        if (request.getParameter("clear-cache") != null) {
            Context.getService(ProjectBuendiaService.class).clearCache();
        }
    }

    private void finish(HttpServletRequest request, HttpServletResponse response) {
        // Nothing to do.
    }

    @Override public SimpleObject handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ResponseStatus ann = (ResponseStatus) e.getClass().getAnnotation(ResponseStatus.class);
        int status = ann != null ? ann.value().value() : 500;
        if (RestUtil.hasCause(e, APIAuthenticationException.class)) {
            status = Context.isAuthenticated() ? 403 : 401;
        }
        if (status == 401) {
            response.addHeader("WWW-Authenticate", "Basic realm=\"OpenMRS at " + RestConstants.URI_PREFIX + "\"");
        }
        if (e instanceof APIException && e.getMessage().startsWith("Unknown resource:")) {
            status = 404;
        }
        response.setStatus(status);

        List<Map<String, Object>> errors = new ArrayList<>();
        for (Throwable t = e; t != null; t = t.getCause()) {
            errors.add(describeError(t));
        }
        return new SimpleObject().add("errors", errors);
    }

    @Override public SimpleObject apiAuthenticationExceptionHandler(Exception e, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return handleException(e, request, response);
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
                maxLevels = level + 2;  // show two more stack levels, then stop
            }
        }

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("type", t.getClass().getName());
        if (!Utils.isEmpty(description)) map.put("description", description);
        map.put("message", message);
        map.put("frames", frames);
        return map;
    }
}
