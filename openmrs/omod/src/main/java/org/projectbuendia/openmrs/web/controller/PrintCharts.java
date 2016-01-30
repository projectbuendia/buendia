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
import org.openmrs.ConceptName;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.projectbuendia.ClientConceptNamer;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.VisitObsValue;
import org.openmrs.projectbuendia.webservices.rest.ChartResource;
import org.openmrs.util.FormUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
    public void onStart() {

    }

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

        ArrayList<Concept> questionConcepts = new ArrayList<>();
        ArrayList<ConceptName> questionConceptNames = new ArrayList<>();
        for (Form form : ChartResource.getCharts(Context.getFormService())) {
            TreeMap<Integer, TreeSet<FormField>> formStructure = FormUtil.getFormStructure(form);
            for (FormField groupField : formStructure.get(0)) {
                for (FormField fieldInGroup : formStructure.get(groupField.getId())) {
                    Concept concept = fieldInGroup.getField().getConcept();
                    questionConcepts.add(concept);
                    questionConceptNames.add(concept.getName());
                }
            }
        }









        try {
            PrintWriter w = response.getWriter();
            w.write("<!doctype html>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <meta charset=\"utf-8\">\n"
                + "  <title>Patient Charts</title>\n"
                + "  <style>\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>");


            for (Patient patient : patients) {









//                ArrayList<Encounter> encounters = new ArrayList<>(
//                    encounterService.getEncountersByPatient(patient));
//                Collections.sort(encounters, ENCOUNTER_COMPARATOR);
//
//                for (Encounter encounter : encounters) {
//                    try {
//                        for (Obs obs : encounter.getAllObs()) {
//
//                        }
//                    } catch (Exception e) {
//                        log.error("Error exporting encounter", e);
//                    }
//                }




                w.write("<table cellpadding=\"0\" cellspacing=\"0\" border=\"1\">\n"
                    + "\t<thead>\n"
                    + "\t\t<th>&nbsp;</th>\n");
                for (int i = 1; i < 8; i++){
                    w.write("<th>Day " + i + "<br/>15 oct</th>");
                }
                w.write("\t</thead>\n"
                        + "\t<tbody>\n");
                for (Concept concept : questionConcepts) {
                    w.write("<tr><td>");
                    String conceptName = concept.getName(Locale.FRENCH).getName();
                    w.write(conceptName);
                    w.write("</td>");
                    List<Obs> obsList = obsService.getObservationsByPersonAndConcept(patient, concept);

                    for (Obs obs : obsList) {
                        String value = (String) VisitObsValue.visit(obs, stringVisitor);
                        w.write("<td>" + value + "</td>");
                    }


                    w.write("</tr>");
                }
                w.write("\t</tbody>\n"
                        + "</table>\n");
            }



            w.write("</body>\n"
                + "</html>");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
