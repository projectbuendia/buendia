package org.projectbuendia.openmrs.web.controller;

import org.openmrs.api.context.Context;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** The main controller. */
@Controller
public class ProjectBuendiaManageController {
    @RequestMapping(value = "/module/projectbuendia/openmrs/manage", method = RequestMethod.GET)
    public void manage(ModelMap model) {
        model.addAttribute("user", Context.getAuthenticatedUser());
    }
}
