package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.webservices.rest.DbUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
    private static final DateTimeZone ZONE = DateTimeZone.forOffsetHours(2); // Central Africa Time
    private static final Locale LOCALE = new Locale("fr");
    private DataHelper helper = null;

    @RequestMapping(method=GET, value="/module/projectbuendia/openmrs/print")
    public void get(HttpServletRequest request, ModelMap model) {
        helper = new DataHelper(LOCALE, ZONE);
        List<DataHelper.PatientPlacement> placements = helper.getPresentPatients();
        List<DataHelper.ObsDisplay> admissionTimes = helper.getAdmissionTimes();
        Collections.reverse(admissionTimes);
        admissionTimes = Utils.slice(admissionTimes, 0, 5);
        List<DataHelper.ObsDisplay> discharges = helper.getDischarges();
        Collections.reverse(discharges);
        discharges = Utils.slice(discharges, 0, 5);

        model.addAttribute("zone", ZONE.toTimeZone());
        model.addAttribute("inpatients", placements);
        model.addAttribute("admissions", admissionTimes);
        model.addAttribute("discharges", discharges);
    }

    @RequestMapping(method=POST, value="/module/projectbuendia/openmrs/print")
    public void post(HttpServletRequest request, HttpServletResponse response, ModelMap model) throws IOException {
        helper = new DataHelper(LOCALE, ZONE);
        PrintWriter writer = response.getWriter();
        printPreamble(writer);
        response.setCharacterEncoding("utf-8");

        Enumeration<String> names = request.getParameterNames();
        Set<String> printedUuids = new HashSet<>();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith("patient")) {
                for (String uuid : request.getParameterValues("patient")) {
                    if (!printedUuids.contains(uuid)) {
                        print(writer, uuid);
                        printedUuids.add(uuid);
                    }
                }
            }
        }
    }

    private void printPreamble(PrintWriter writer) {
        write(writer, tag("<meta charset='UTF-8'>"));
        try {
            InputStream stream = new FileInputStream("/tmp/style.css");
            InputStreamReader reader = new InputStreamReader(stream);
            char[] buffer = new char[1024];
            write(writer, tag("<style>"));
            while (reader.ready()) {
                int count = reader.read(buffer);
                if (count < 0) break;
                writer.write(new String(buffer, 0, count));
            }
            write(writer, tag("</style>"));
        } catch (IOException e) {
            writer.write("<link rel='stylesheet' href='style.css'>");
        }
    }

    private void print(PrintWriter writer, String patientUuid) {
        Patient pat = helper.getPatient(patientUuid);
        write(writer, tag("<h1>"), pat.getPersonName().getFullName(), tag("</h1>"));
        List<List<Obs>> groups = helper.getEncounterObs(helper.getEncounters(pat));
        for (List<Obs> group : groups) {
            if (group.isEmpty()) continue;
            DateTime start = helper.toLocalDateTime(group.get(0).getObsDatetime());
            write(writer, tag("<h2>"), helper.formatTime(start), tag("</h2>"));
            write(writer, tag("<ul>"));
            for (Obs obs : group) {
                write(writer, tag("<li>"));
                writeObs(writer, obs);
            }
            write(writer, tag("</ul>"));
        }
    }

    private void writeObs(PrintWriter writer, Obs obs) {
        switch (Utils.compressUuid(obs.getConcept().getUuid()).toString()) {
            case DbUtils.CONCEPT_PLACEMENT_UUID:
                DataHelper.Placement p = helper.getPlacement(obs);
                write(writer, "Moved to ", p.getLocationName());
                break;
            default:
                write(writer, helper.getLocalizedName(obs.getConcept()), ": ", formatValue(obs));
        }
    }


    class Tag {
        String tag;
        public Tag(String tag) { this.tag = tag; }
    }
    private Tag tag(String tag) {
        return new Tag(tag);
    }

    private static void write(PrintWriter writer, Object... objects) {
        for (Object object : objects) {
            if (object instanceof Tag) {
                writer.write(((Tag) object).tag);
            } else {
                writer.write(esc(Utils.toNonnullString(object)));
            }
        }
        writer.write("\n");
    }

    private static String esc(String x) {
        return x.replace("&", "&amp;").replace("<", "&lt;");
    }

    private String formatValue(Obs obs) {
        ConceptDatatype type = obs.getConcept().getDatatype();
        switch (type.getHl7Abbreviation()) {
            case ConceptDatatype.BOOLEAN:
                return Boolean.toString(obs.getValueBoolean());
            case ConceptDatatype.CODED:
                return helper.getLocalizedName(obs.getValueCoded());
            case ConceptDatatype.NUMERIC:
                return Utils.format(obs.getValueNumeric(), 6);
            case ConceptDatatype.TEXT:
                return obs.getValueText();
            case ConceptDatatype.DATE:
                return helper.formatTime(helper.toLocalDateTime(obs.getValueDate()));
            case ConceptDatatype.DATETIME:
                return helper.formatTime(helper.toLocalDateTime(obs.getValueDatetime()));
        }
        return "?";
    }
}
