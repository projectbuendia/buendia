package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.projectbuendia.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller public class Print {
    private static final DateTimeZone ZONE = DateTimeZone.forOffsetHours(2); // Central Africa Time

    @RequestMapping(method= GET, value="/module/projectbuendia/openmrs/print")
    public void get(HttpServletRequest request, ModelMap model) {
        PrintDataHelper helper = new PrintDataHelper(ZONE);

        List<PrintDataHelper.PatientPlacement> placements = helper.getPresentPatients();
        List<PrintDataHelper.ObsDisplay> admissionTimes = helper.getAdmissionTimes();
        Collections.reverse(admissionTimes);
        admissionTimes = Utils.slice(admissionTimes, 0, 5);
        List<PrintDataHelper.ObsDisplay> discharges = helper.getDischarges();
        Collections.reverse(discharges);
        discharges = Utils.slice(discharges, 0, 5);

        model.addAttribute("zone", ZONE.toTimeZone());
        model.addAttribute("inpatients", placements);
        model.addAttribute("admissions", admissionTimes);
        model.addAttribute("discharges", discharges);
    }

    @RequestMapping(method=POST, value="/module/projectbuendia/openmrs/print")
    public void post(HttpServletRequest request, HttpServletResponse response, ModelMap model) throws IOException {
        PrintDataHelper helper = new PrintDataHelper(ZONE);
        PrintWriter writer = response.getWriter();
        response.setCharacterEncoding("utf-8");

        Enumeration<String> names = request.getParameterNames();
        Set<String> printedUuids = new HashSet<>();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith("patient")) {
                for (String uuid : request.getParameterValues("patient")) {
                    if (!printedUuids.contains(uuid)) {
                        print(writer, helper, uuid);
                        printedUuids.add(uuid);
                    }
                }
            }
        }
    }

    private void print(PrintWriter writer, PrintDataHelper helper, String uuid) {
        Patient pat = helper.getPatient(uuid);
        writer.write("<h1>" + esc(pat.getPersonName()) + "</h1>");
        List<List<Obs>> groups = helper.getEncounterObs(helper.getEncounters(pat));
        for (List<Obs> group : groups) {
            if (group.isEmpty()) continue;
            DateTime start = Utils.toLocalDateTime(
                group.get(0).getEncounter().getEncounterDatetime().getTime(), ZONE);
            writer.write()

        }
    }

    class Tag {
        String tag;

        public Tag(String tag) {
            this.tag = tag;
        }
    }

    private static void write(PrintWriter writer, Object... objects) {
        for (Object object : objects) {
            if (object instanceof Tag) {
                writer.write(((Tag) object).tag);
            } else {
                writer.write(esc(Utils.toNonnullString(object)));
            }
        }
    }

    private static String esc(String x) {
        return x.replace("&", "&amp;").replace("<", "&lt;");
    }
}
