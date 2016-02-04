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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Person;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.projectbuendia.ClientConceptNamer;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.VisitObsValue;
import org.openmrs.projectbuendia.webservices.rest.ChartResource;
import org.openmrs.util.FormUtil;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** The controller for the profile management page. */
@Controller
public class PrintCharts {

    protected static Log log = LogFactory.getLog(ProfileManager.class);

    private static final Comparator<Patient> PATIENT_COMPARATOR = new Comparator<Patient>() {
        @Override public int compare(Patient p1, Patient p2) {
            PatientIdentifier id1 = p1.getPatientIdentifier("MSF");
            PatientIdentifier id2 = p2.getPatientIdentifier("MSF");
            return Utils.alphanumericComparator.compare(
                id1 == null ? null : id1.getIdentifier(),
                id2 == null ? null : id2.getIdentifier()
            );
        }
    };

    private static final Comparator<Encounter> ENCOUNTER_COMPARATOR = new Comparator<Encounter>() {
        @Override public int compare(Encounter e1, Encounter e2) {
            return e1.getEncounterDatetime().compareTo(e2.getEncounterDatetime());
        }
    };

    private boolean authorized() {
        return Context.hasPrivilege("Manage Concepts") &&
            Context.hasPrivilege("Manage Forms");
    }

    public static final DateFormat HEADER_DATE_FORMAT = new SimpleDateFormat("d MMM");

    private static final ClientConceptNamer NAMER = new ClientConceptNamer(Locale.FRENCH);

    private final VisitObsValue.ObsValueVisitor stringVisitor =
        new VisitObsValue.ObsValueVisitor<String>() {
            @Override public String visitCoded(Concept value) {
                return NAMER.getClientName(value);
            }

            @Override public String visitNumeric(Double value) {
                return Double.toString(value);
            }

            @Override public String visitBoolean(Boolean value) {
                return Boolean.toString(value);
            }

            @Override public String visitText(String value) {
                return value;
            }

            @Override public String visitDate(Date d) {
                return Utils.YYYYMMDD_UTC_FORMAT.format(d);
            }

            @Override public String visitDateTime(Date d) {
                return Utils.SPREADSHEET_FORMAT.format(d);
            }
        };

    /** This is executed every time a request is made. */
    @ModelAttribute
    public void onStart() {}

    @RequestMapping(value = "/module/projectbuendia/openmrs/print-charts", method = RequestMethod.GET)
    public void get(HttpServletRequest request, ModelMap model) {
        model.addAttribute("authorized", authorized());
    }

    @RequestMapping(value = "/module/projectbuendia/openmrs/printable", method = RequestMethod.POST)
    public void post(HttpServletRequest request, HttpServletResponse response, ModelMap model) {

        PatientService patientService = Context.getPatientService();
        EncounterService encounterService = Context.getEncounterService();
        ObsService obsService = Context.getObsService();

        List<Patient> patients = new ArrayList<>(patientService.getAllPatients());
        Collections.sort(patients, PATIENT_COMPARATOR);

        HashMap<String, ArrayList<Concept>> charts = new HashMap<>();
        String chartName = null;
        for (Form form : ChartResource.getCharts(Context.getFormService())) {
            TreeMap<Integer, TreeSet<FormField>> formStructure = FormUtil.getFormStructure(form);
            for (FormField groupField : formStructure.get(0)) {
                if (groupField.getField().getName().equals("[chart_divider]")) {
                    chartName = ((FormField) formStructure.get(groupField.getId()).toArray()[0]).getField().getName();
                    charts.put(chartName, new ArrayList<Concept>());
                }
                for (FormField fieldInGroup : formStructure.get(groupField.getId())) {
                    charts.get(chartName).add(fieldInGroup.getField().getConcept());
                }
            }
        }

        try {
            final Calendar c = Calendar.getInstance();

            PrintWriter w = response.getWriter();
            w.write("<!doctype html>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <meta charset=\"utf-8\">\n"
                + "  <title>Patient Charts</title>\n"
                + "  <style type=\"text/css\">\n"
                + "    table { page-break-inside:auto; margin-bottom: 22pt; }\n"
                //+ "    tr    { page-break-inside:avoid; page-break-after:auto }\n"
                //+ "    thead { display:table-header-group }\n"
                //+ "    tfoot { display:table-footer-group }\n"
                + "    h3 { margin 10dp; page-break-before: always;}\n"
                + "    td { page-break-inside:avoid; }\n"
                + "    thead {background-color:#C5C5C5;}\n"
//                + "tr    \n"
//                + "{ \n"
//                + "  display: table-row-group;\n"
//                + "  page-break-inside:avoid; \n"
//                + "  page-break-after:auto;\n"
//                + "}\n"
                //+ "@media print {\n"
                //+ "   thead {display: table-header-group;}\n"
                //+ "}"
                + "  </style>\n"
                + "</head>\n"
                + "<body>");

            for (Patient patient : patients) {

                ArrayList<Encounter> encounters = new ArrayList<>(
                    encounterService.getEncountersByPatient(patient));
                Collections.sort(encounters, ENCOUNTER_COMPARATOR);
                LinkedHashSet<Date> days = new LinkedHashSet<>();

                for (Encounter encounter : encounters) {
                    c.setTime(encounter.getEncounterDatetime());
                    int year = c.get(Calendar.YEAR);
                    int month = c.get(Calendar.MONTH);
                    int day = c.get(Calendar.DAY_OF_MONTH);
                    c.set(year, month, day, 0, 0, 0);
                    days.add(c.getTime());
                }

                w.write("<h2>" + patient.getPatientIdentifier("MSF") + ". "
                    + patient.getGivenName() + " " + patient.getFamilyName()
                    + "</h2><hr/>");

                Date[] daysArray = days.toArray(new Date[days.size()]);
                if (daysArray.length > 0) {
                    c.setTime(daysArray[0]);
                } else {
                    w.write("<b>No encounters for this patient</b>");
                    continue;
                }


                // For now we are expecting 3 charts: "Patient Chart", "Admission", and "Immunisations".
                // TODO: Make it more generic.

                // Admission Chart
                w.write("<h3>PATIENT DETAILS / ADMISSION</h3>");
                ArrayList<Concept> questionConcepts = charts.get("Admission");
                w.write("<table cellpadding=\"1\" cellspacing=\"0\" border=\"1\" width=\"100%\">\n"
                    + "\t<thead>\n"
                    + "\t\t<th width=\"20%\">&nbsp;</th>\n"
                    + "\t\t<th>&nbsp;</th>\n");
                w.write("\t</thead>\n"
                    + "\t<tbody>\n");

                for (Concept concept : questionConcepts) {
                    w.write("<tr><td>");
                    String conceptName = NAMER.getClientName(concept);
                    w.write(conceptName);
                    w.write("</td>");

                    List<Person> obsPatientList = new ArrayList<>();
                    obsPatientList.add(patient);
                    List<Concept> obsConceptList = new ArrayList<>();
                    obsConceptList.add(concept);

                    List<Obs> observations = obsService.getObservations(obsPatientList, null,
                        obsConceptList, null, null, null, null, 1, null, null, null, false);
                    String value = "&nbsp;";
                    if (!observations.isEmpty()) {
                        value = (String) VisitObsValue.visit(observations.get(0), stringVisitor);
                    }
                    w.write("<td>" + value + "</td></tr>");
                }

                w.write("\t</tbody>\n"
                    + "</table>\n");
                // End of Admission Chart


                // Patient Chart
                w.write("<h3>PATIENT CHART</h3>");
                questionConcepts = charts.get("Patient Chart");
                int day = 1;
                Date today = daysArray[0];
                Date lastDay = daysArray[daysArray.length - 1];
                do {
                    w.write("<table cellpadding=\"1\" cellspacing=\"0\" border=\"1\" width=\"100%\">\n"
                        + "\t<thead>\n"
                        + "\t\t<th width=\"20%\">&nbsp;</th>\n");
                    c.setTime(today);
                    for (int i = day; i < (day + 7); i++) {
                        w.write("<th width=\"10%\">Day " + i + "<br/>"
                            + HEADER_DATE_FORMAT.format(c.getTime()) + "</th>");
                        c.add(c.DAY_OF_MONTH, 1);
                    }
                    w.write("\t</thead>\n"
                        + "\t<tbody>\n");

                    for (Concept concept : questionConcepts) {

                        List<Person> obsPatientList = new ArrayList<>();
                        obsPatientList.add(patient);
                        List<Concept> obsConceptList = new ArrayList<>();
                        obsConceptList.add(concept);

                        int obsCount = obsService.getObservationCount(obsPatientList, null,
                            obsConceptList, null, null, null, null, null, null, false);
                        if (obsCount == 0) {
                            continue;
                        }

                        w.write("<tr><td>");
                        String conceptName = NAMER.getClientName(concept);
                        w.write(conceptName);
                        w.write("</td>");

                        c.setTime(today);
                        for (int i = 1; i < 8; i++) {
                            Date dayStart = c.getTime();
                            Date dayEnd = OpenmrsUtil.getLastMomentOfDay(dayStart);
                            List<Obs> observations = obsService.getObservations(obsPatientList, null,
                                obsConceptList, null, null, null, null, 1, null, dayStart, dayEnd,
                                false);
                            String value = "&nbsp;";
                            if (!observations.isEmpty()) {
                                value = (String) VisitObsValue.visit(observations.get(0), stringVisitor);
                            }
                            w.write("<td>" + value + "</td>");
                            c.add(c.DAY_OF_MONTH, 1);
                        }
                        w.write("</tr>");
                    }

                    w.write("\t</tbody>\n"
                        + "</table>\n");

                    day += 7;
                    c.setTime(today);
                    c.add(Calendar.DAY_OF_MONTH, 7);
                    today = c.getTime();
                } while (today.before(lastDay) || today.equals(lastDay));

                // End of Patient Chart


                // Immunisations Chart
                w.write("<h3>IMMUNISATIONS</h3>");
                questionConcepts = charts.get("Immunisations");
                w.write("<table cellpadding=\"1\" cellspacing=\"0\" border=\"1\" width=\"100%\">\n"
                    + "\t<thead>\n"
                    + "\t\t<th width=\"20%\">&nbsp;</th>\n"
                    + "\t\t<th>&nbsp;</th>\n");
                w.write("\t</thead>\n"
                    + "\t<tbody>\n");

                for (Concept concept : questionConcepts) {
                    w.write("<tr><td>");
                    String conceptName = NAMER.getClientName(concept);
                    w.write(conceptName);
                    w.write("</td>");

                    List<Person> obsPatientList = new ArrayList<>();
                    obsPatientList.add(patient);
                    List<Concept> obsConceptList = new ArrayList<>();
                    obsConceptList.add(concept);

                    List<Obs> observations = obsService.getObservations(obsPatientList, null,
                        obsConceptList, null, null, null, null, 1, null, null, null, false);
                    String value = "&nbsp;";
                    if (!observations.isEmpty()) {
                        value = (String) VisitObsValue.visit(observations.get(0), stringVisitor);
                    }
                    w.write("<td>" + value + "</td></tr>");
                }

                w.write("\t</tbody>\n"
                    + "</table>\n");
                // End of Immunisations Chart


            }
            w.write("</body>\n"
                + "</html>");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
