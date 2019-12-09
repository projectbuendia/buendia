package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTimeZone;
import org.openmrs.Patient;
import org.openmrs.projectbuendia.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller public class Print {
    private static final DateTimeZone DEFAULT_ZONE = DateTimeZone.forOffsetHours(2); // Central Africa Time
    private static final Locale DEFAULT_LOCALE = new Locale("fr");

    private DataHelper getDataHelper(HttpServletRequest request) {
        DateTimeZone zone = DEFAULT_ZONE;
        String tz = request.getParameter("tz");
        if (tz != null) {
            try {
                zone = DateTimeZone.forID(tz);
            } catch (IllegalArgumentException e) { }
        }

        Locale locale = DEFAULT_LOCALE;
        String tag = request.getParameter("lang");
        if (tag != null) {
            try {
                locale = Locale.forLanguageTag(tag);
            } catch (NullPointerException | IllegalArgumentException e) { }
        }
        Locale.setDefault(locale);
        return new DataHelper(zone, locale);
    }

    @RequestMapping(method = GET, value = "/module/projectbuendia/openmrs/print")
    public void get(HttpServletRequest request, ModelMap model) {
        DataHelper helper = getDataHelper(request);
        List<DataHelper.PatientPlacement> placements = helper.getPresentPatients();
        List<DataHelper.ObsDisplay> admissions = helper.getAdmissionTimes();

        List<DataHelper.ObsDisplay> recentAdmissions = new ArrayList<>(admissions);
        Collections.reverse(recentAdmissions);
        recentAdmissions = Utils.slice(recentAdmissions, 0, 6);

        List<DataHelper.ObsDisplay> discharges = helper.getDischargeTimes();
        List<DataHelper.ObsDisplay> recentDischarges = new ArrayList<>(discharges);
        Collections.reverse(recentDischarges);
        recentDischarges = Utils.slice(recentDischarges, 0, 6);

        model.addAttribute("showAll", request.getParameter("all") != null);
        model.addAttribute("allAdmissions", admissions);
        model.addAttribute("inpatients", placements);
        model.addAttribute("admissions", recentAdmissions);
        model.addAttribute("discharges", recentDischarges);
    }

    @RequestMapping(method = POST, value = "/module/projectbuendia/openmrs/print")
    public void post(HttpServletRequest request, HttpServletResponse response, ModelMap model)
        throws IOException {
        DataHelper helper = getDataHelper(request);
        response.setCharacterEncoding("utf-8");
        PatientPrinter printer = new PatientPrinter(response.getWriter(), helper.getLocale(), helper);
        printer.printPreamble();
        Enumeration<String> names = request.getParameterNames();
        Set<String> printedUuids = new HashSet<>();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith("patient")) {
                for (String uuid : request.getParameterValues("patient")) {
                    if (!printedUuids.contains(uuid)) {
                        Patient patient = helper.getPatient(uuid);
                        printer.printAdmissionForm(patient);
                        printer.printHistory(patient);
                        printedUuids.add(uuid);
                    }
                }
            }
        }
    }
}
