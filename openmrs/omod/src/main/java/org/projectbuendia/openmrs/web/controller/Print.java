package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTimeZone;
import org.openmrs.Patient;
import org.openmrs.projectbuendia.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
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
        return new DataHelper(zone, locale);
    }

    @RequestMapping(method = GET, value = "/module/projectbuendia/openmrs/print")
    public void get(HttpServletRequest request, ModelMap model) {
        DataHelper helper = getDataHelper(request);
        List<DataHelper.PatientPlacement> placements = helper.getPresentPatients();
        List<DataHelper.ObsDisplay> admissionTimes = helper.getAdmissionTimes();
        Collections.reverse(admissionTimes);
        admissionTimes = Utils.slice(admissionTimes, 0, 5);
        List<DataHelper.ObsDisplay> discharges = helper.getDischarges();
        Collections.reverse(discharges);
        discharges = Utils.slice(discharges, 0, 5);

        model.addAttribute("inpatients", placements);
        model.addAttribute("admissions", admissionTimes);
        model.addAttribute("discharges", discharges);
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
                        printer.printAdmission(patient);
                        printer.printEncounters(patient);
                        printedUuids.add(uuid);
                    }
                }
            }
        }
    }
}
