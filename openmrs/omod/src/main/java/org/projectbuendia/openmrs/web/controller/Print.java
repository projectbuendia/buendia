package org.projectbuendia.openmrs.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

@Controller public class Print {
    @RequestMapping(
        value = "/module/projectbuendia/openmrs/print",
        method = RequestMethod.GET
    )

    public void get(HttpServletRequest request, ModelMap model) {
        model.addAttribute("hello", "world");
    }
}
