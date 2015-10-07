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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.projectbuendia.webservices.rest.ConfigurationException;
import org.openmrs.projectbuendia.webservices.rest.GlobalProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** The controller for the profile management page. */
@Controller
public class ProfileManager {
    static Log log = LogFactory.getLog(ProfileManager.class);
    final File PROFILE_DIR = new File("/usr/share/buendia/profiles");
    final String VALIDATE_CMD = "buendia-profile-validate";
    final String APPLY_CMD = "buendia-profile-apply";

    public ProfileManager() {
        createProfileDirectoryIfNecessary();
    }

    private void createProfileDirectoryIfNecessary() {
        if(!PROFILE_DIR.exists()) {
            if(!PROFILE_DIR.mkdirs()) {
                throw new ConfigurationException(String.format("Error creating profile dir %s. "
                    + "Check its write permissions.", PROFILE_DIR.getAbsolutePath()));
            }
        }
    }

    @RequestMapping(value = "/module/projectbuendia/openmrs/profiles", method = RequestMethod.GET)
    public void get(HttpServletRequest request, ModelMap model) {
        List<FileInfo> files = new ArrayList<>();
        for (File file : PROFILE_DIR.listFiles()) {
            files.add(new FileInfo(file));
        }
        Collections.sort(files, new Comparator<FileInfo>() {
            public int compare(FileInfo a, FileInfo b) {
                return -a.modified.compareTo(b.modified);
            }
        });

        model.addAttribute("user", Context.getAuthenticatedUser());
        model.addAttribute("profiles", files);
        model.addAttribute("currentProfile",
            Context.getAdministrationService().getGlobalProperty(
                GlobalProperties.CURRENT_PROFILE));
        model.addAttribute("authorized", authorized());
    }

    public static boolean authorized() {
        return Context.hasPrivilege("Manage Concepts") &&
            Context.hasPrivilege("Manage Forms");
    }

    @RequestMapping(value = "/module/projectbuendia/openmrs/profiles", method = RequestMethod.POST)
    public View post(HttpServletRequest request, HttpServletResponse response, ModelMap model) {
        if (!authorized()) {
            return new RedirectView("profiles.form");
        }

        if (request instanceof MultipartHttpServletRequest) {
            addProfile((MultipartHttpServletRequest) request, model);
        } else {
            String filename = request.getParameter("profile");
            String op = request.getParameter("op");
            if (filename != null) {
                File file = new File(PROFILE_DIR, filename);
                if (file.isFile()) {
                    model.addAttribute("filename", filename);
                    if ("Apply".equals(op)) {
                        applyProfile(file, model);
                    } else if ("Download".equals(op)) {
                        downloadProfile(file, response);
                        return null;  // download the file, don't redirect
                    } else if ("Delete".equals(op)) {
                        deleteProfile(file, model);
                    }
                }
            }
        }
        return new RedirectView("profiles.form");  // reload this page with a GET request
    }

    /** Chooses a filename based on the given name, with a "-vN" suffix appended for uniqueness. */
    String getNextVersionedFilename(String name) {
        // Separate the name into a sanitized base name and an extension.
        name = sanitizeName(name);
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            ext = name.substring(dot);
            name = name.substring(0, dot);
        }

        // Find the highest version number among all existing files named like "name-vN.ext".
        // If "name.ext" exists, it counts as version 1.
        String prefix = name + "-v";
        int highestVersion = 0;
        for (File file : PROFILE_DIR.listFiles()) {
            int version = 0;
            String n = file.getName();
            if (n.equals(name + ext)) {
                version = 1;
            } else if (n.startsWith(prefix) && n.endsWith(ext)) {
                try {
                    version = Integer.parseInt(
                        n.substring(prefix.length(), n.length() - ext.length()));
                } catch (NumberFormatException e) { }
            }
            highestVersion = Math.max(version, highestVersion);
        }

        // Generate a unique new name, adding the next higher version number if necessary.
        if (highestVersion == 0) {
            return name + ext;
        } else {
            return prefix + (highestVersion + 1) + ext;
        }
    }

    /** Handles an uploaded profile. */
    void addProfile(MultipartHttpServletRequest request, ModelMap model) {
        List<String> lines = new ArrayList<>();
        MultipartFile mpf = request.getFile("file");
        if (mpf != null) {
            try {
                File tempFile = File.createTempFile("profile", null);
                mpf.transferTo(tempFile);
                if (execute(VALIDATE_CMD, tempFile, lines)) {
                    String filename = getNextVersionedFilename(mpf.getOriginalFilename());
                    File newFile = new File(PROFILE_DIR, filename);
                    model.addAttribute("filename", filename);
                    FileUtils.moveFile(tempFile, newFile);
                    model.addAttribute("success", "add");
                } else {
                    model.addAttribute("failure", "add");
                    model.addAttribute("filename", mpf.getOriginalFilename());
                    model.addAttribute("output", StringUtils.join(lines, "\n"));
                }
            } catch (Exception e) {
                log.error("Problem saving uploaded profile", e);
            }
        }
    }

    /** Applies a profile to the OpenMRS database. */
    void applyProfile(File file, ModelMap model) {
        List<String> lines = new ArrayList<>();
        if (execute(APPLY_CMD, file, lines)) {
            setCurrentProfile(file.getName());
            model.addAttribute("success", "apply");
        } else {
            model.addAttribute("failure", "apply");
            model.addAttribute("output", StringUtils.join(lines, "\n"));
        }
    }

    /** Downloads a profile. */
    public void downloadProfile(File file, HttpServletResponse response) {
        response.setContentType("application/octet-stream");
        response.setHeader(
            "Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        try {
            response.getOutputStream().write(
                Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        } catch (IOException e) {
            log.error("Error downloading profile: " + file.getName(), e);
        }
    }

    /** Deletes a profile. */
    void deleteProfile(File file, ModelMap model) {
        if (file.getName().equals(getCurrentProfile())) {
            model.addAttribute("failure", "delete");
            model.addAttribute("output", "Cannot delete the currently active profile.");
        } else if (file.delete()) {
            model.addAttribute("success", "delete");
        } else {
            log.error("Error deleting profile: " + file.getName());
            model.addAttribute("failure", "delete");
        }
    }

    /**
     * Executes a command with one argument, returning true if the command succeeds.
     * Gathers the output from stdout and stderr into the provided list of lines.
     */
    boolean execute(String command, File arg, List<String> lines) {
        ProcessBuilder pb = new ProcessBuilder(command, arg.getAbsolutePath());
        pb.redirectErrorStream(true);  // redirect stderr to stdout
        try {
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
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

    /** Gets the global property for the name of the current profile. */
    String getCurrentProfile() {
        return Context.getAdministrationService().getGlobalProperty(
            GlobalProperties.CURRENT_PROFILE);
    }

    /** Sets the global property for the name of the current profile. */
    void setCurrentProfile(String name) {
        Context.getAdministrationService().setGlobalProperty(
            GlobalProperties.CURRENT_PROFILE, name);
    }

    public class FileInfo {
        String name;
        Long size;
        Date modified;

        public FileInfo(File file) {
            name = file.getName();
            size = file.length();
            modified = new Date(file.lastModified());
        }

        public String getName() {
            return name;
        }
        public String getFormattedName() {
            return (name + "                              ").substring(0, 30);
        }
        public Long getSize() { return size; }
        public String getFormattedSize() {
            return String.format("%7d", size);
        }
        public Date getModified() { return modified; }
    }
}
