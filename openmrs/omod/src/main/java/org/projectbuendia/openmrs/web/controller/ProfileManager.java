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

package org.projectbuendia.openmrs.web.controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.projectbuendia.webservices.rest.GlobalProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** The controller for the profile management page. */
@Controller
public class ProfileManager {
    final View REDIRECT_TO_SELF = new RedirectView("profiles.form");

    final File PROFILE_DIR = new File("/usr/share/buendia/profiles");
    final String VALIDATE_CMD = "buendia-profile-validate";
    final String APPLY_CMD = "buendia-profile-apply";
    static Log log = LogFactory.getLog(ProfileManager.class);

    @RequestMapping(value = "/module/projectbuendia/openmrs/profiles", method = RequestMethod.GET)
    public void get(HttpServletRequest request, ModelMap model) {
        model.addAttribute("user", Context.getAuthenticatedUser());
        model.addAttribute("profiles", PROFILE_DIR.listFiles());
        model.addAttribute("currentProfile",
                Context.getAdministrationService().getGlobalProperty(GlobalProperties.CURRENT_PROFILE));
        model.addAttribute("success", request.getParameter("success"));
        model.addAttribute("filename", request.getParameter("filename"));
    }

    @RequestMapping(value = "/module/projectbuendia/openmrs/profiles", method = RequestMethod.POST)
    public View post(HttpServletRequest request, /*HttpServletResponse response,*/ ModelMap model) {
        if (request instanceof MultipartHttpServletRequest) {
            return addProfile((MultipartHttpServletRequest) request, model);
        } else {
            String filename = request.getParameter("profile");
            String op = request.getParameter("op");
            if (filename != null) {
                File file = new File(PROFILE_DIR, filename);
                if (file.isFile()) {
                    if ("Apply".equals(op)) {
                        return applyProfile(file, model);
                    }
                    if ("Download".equals(op)) {
                        model.addAttribute("filename", filename);
                        return new RedirectView("download.form");
                    }
                    if ("Delete".equals(op)) {
                        return deleteProfile(file, model);
                    }
                }
            }
        }
        return REDIRECT_TO_SELF;
    }

    /**
     * Executes a command with one argument, returning true if the command succeeds.
     * Gathers the output from stdout and stderr in a list of lines.
     */
    boolean execute(String command, File arg, List<String> output) {
        ProcessBuilder pb = new ProcessBuilder(command, arg.getAbsolutePath());
        pb.redirectErrorStream(true);  // redirect stderr to stdout
        try {
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) output.add(line);
            proc.waitFor();
            return proc.exitValue() == 0;
        } catch (Exception e) {
            log.error("Exception while executing: " + command + " " + arg, e);
            return false;
        }
    }

    /** Sanitizes a string to produce a safe filename. */
    String sanitizeName(String filename) {
        String[] parts = filename.split("/");
        return parts[parts.length - 1].replaceAll("[^A-Za-z0-9._-]", " ").replaceAll(" +", " ");
    }

    /** Handles an uploaded profile. */
    View addProfile(MultipartHttpServletRequest request, ModelMap model) {
        List<String> output = new ArrayList<>();
        MultipartFile mpf = request.getFile("file");
        if (mpf != null) {
            try {
                File tempFile = File.createTempFile("profile", null);
                mpf.transferTo(tempFile);
                if (execute(VALIDATE_CMD, tempFile, output)) {
                    String filename = sanitizeName(mpf.getOriginalFilename());
                    File newFile = new File(PROFILE_DIR, filename);
                    model.addAttribute("filename", filename);
                    if (newFile.exists()) {
                        model.addAttribute("failure", "add");
                        output.add("A profile named " + filename + " already exists.");
                        model.addAttribute("output", output);
                        return null;  // render a page with just an error message
                    } else {
                        FileUtils.moveFile(tempFile, newFile);
                        model.addAttribute("success", "add");
                    }
                } else {
                    model.addAttribute("failure", "add");
                    model.addAttribute("filename", mpf.getOriginalFilename());
                    model.addAttribute("output", output);
                    return null;  // render a page with just an error message
                }
            } catch (Exception e) {
                log.error("Problem saving uploaded profile", e);
            }
        }
        return REDIRECT_TO_SELF;
    }

    @RequestMapping(value = "/module/projectbuendia/openmrs/download", method = RequestMethod.GET)
    public void download(HttpServletRequest request, HttpServletResponse response) {
        File file = new File(PROFILE_DIR, request.getParameter("filename"));
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + file.getName() + "\"");
        try {
            response.getOutputStream().write(
                    Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        } catch (IOException e) {
            log.error("Error downloading profile: " + file.getName(), e);
        }
    }

    View applyProfile(File file, ModelMap model) {
        model.addAttribute("filename", file.getName());
        List<String> output = new ArrayList<>();
        if (execute(APPLY_CMD, file, output)) {
            Context.getAdministrationService().setGlobalProperty(
                    GlobalProperties.CURRENT_PROFILE, file.getName());
            model.addAttribute("success", "apply");
            return REDIRECT_TO_SELF;
        } else {
            model.addAttribute("failure", "apply");
            model.addAttribute("output", output);
            return null;  // render a page with just an error message
        }
    }

    View deleteProfile(File file, ModelMap model) {
        model.addAttribute("filename", file.getName());
        if (file.delete()) {
            model.addAttribute("success", "delete");
            return REDIRECT_TO_SELF;
        } else {
            log.error("Error deleting profile: " + file.getName());
            model.addAttribute("failure", "delete");
            return null;
        }
    }
}
