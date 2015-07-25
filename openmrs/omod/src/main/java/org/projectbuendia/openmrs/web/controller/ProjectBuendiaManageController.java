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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.openmrs.api.context.Context;
import org.openmrs.projectbuendia.webservices.rest.GlobalProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/** The main controller. */
@Controller
public class ProjectBuendiaManageController {
    final File PROFILE_DIR = new File("/usr/share/buendia/profiles");

    @RequestMapping(value = "/module/projectbuendia/openmrs/manage", method = RequestMethod.GET)
    public void get(ModelMap model) {
        model.addAttribute("user", Context.getAuthenticatedUser());
        model.addAttribute("profiles", PROFILE_DIR.listFiles());
        model.addAttribute("currentProfile",
                Context.getAdministrationService().getGlobalProperty(GlobalProperties.CURRENT_PROFILE));
    }

    private List<String> execute(String command, String arg) {
        List<String> errorLines = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder(command, arg);
        pb.redirectErrorStream(true);  // redirect stderr to stdout
        try {
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) errorLines.add(line);
            proc.waitFor();
            if (proc.exitValue() == 0) {
                return null;
            } else {
                return errorLines;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            errorLines.add("<interrupted>");
            return errorLines;
        } catch (IOException e) {
            e.printStackTrace();
            errorLines.add("<IOException>");
            return errorLines;
        }
    }

    @RequestMapping(value = "/module/projectbuendia/openmrs/manage", method = RequestMethod.POST)
    public View post(HttpServletRequest request, ModelMap model) {
        if (ServletFileUpload.isMultipartContent(request)) {
            // A new profile is being uploaded.
            ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
            try {
                List<FileItem> items = (List<FileItem>) upload.parseRequest(request);
                for (FileItem item : items) {
                    if ("profile".equals(item.getFieldName())) {
                        File temp = File.createTempFile("profile", null);
                        item.write(temp);
                        List<String> errorLines = execute("buendia-profile-validate", temp.getAbsolutePath());
                        if (errorLines == null) {
                            item.write(new File(PROFILE_DIR, item.getName()));
                        } else {
                            model.addAttribute("errors", errorLines);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // A profile is being selected.
            String profile = request.getParameter("profile");
            if (profile != null && new File(PROFILE_DIR, profile).isFile()) {
                Context.getAdministrationService().setGlobalProperty(GlobalProperties.CURRENT_PROFILE, profile);
            }
        }
        return new RedirectView("manage.form");  // reload this page with a GET request
    }
}
