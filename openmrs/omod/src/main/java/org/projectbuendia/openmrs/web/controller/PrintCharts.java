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
import org.openmrs.api.context.Context;
import org.openmrs.projectbuendia.Utils;
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
import org.openmrs.api.context.Context;

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

    /** This is executed every time a request is made. */
    @ModelAttribute
    public void onStart() {

    }

    @RequestMapping(value = "/module/projectbuendia/openmrs/print-charts", method = RequestMethod.GET)
    public void get(HttpServletRequest request, ModelMap model) {
        model.addAttribute("authorized", authorized());
    }

    @RequestMapping(value = "/module/projectbuendia/openmrs/printable", method = RequestMethod.POST)
    public void post(HttpServletRequest request, ModelMap model) {

        PatientService patientService = Context.getPatientService();
        EncounterService encounterService = Context.getEncounterService();

        List<Patient> patients = new ArrayList<>(patientService.getAllPatients());
        Collections.sort(patients, PATIENT_COMPARATOR);

        //Set<Concept> questionConcepts = new HashSet<>();
        ArrayList<ObsConcept> questionConcepts = new ArrayList<>();
        for (Form form : ChartResource.getCharts(Context.getFormService())) {
            TreeMap<Integer, TreeSet<FormField>> formStructure = FormUtil.getFormStructure(form);
            for (FormField groupField : formStructure.get(0)) {
                for (FormField fieldInGroup : formStructure.get(groupField.getId())) {
                    //questionConcepts.add(fieldInGroup.getField().getConcept());
                    Concept concept = fieldInGroup.getField().getConcept();

                    ObsConcept obsConcept = new ObsConcept();
                    String conceptName = concept.getName(new Locale("fr")).toString();
                    if (conceptName.isEmpty()){
                        conceptName = concept.getName(new Locale("en_GB_client")).toString();
                    }
                    obsConcept.name = conceptName;
                    questionConcepts.add(obsConcept);
                }
            }
        }

        ArrayList<PatientModel> patientModels = new ArrayList<>();
        for (Patient patient : patients) {
            PatientModel patientModel = new PatientModel();
            patientModel.patientIdentifier = patient.getPatientIdentifier("MSF").toString();


            ArrayList<Day> days = new ArrayList<>();

            Day d = new Day();
            d.desc = "Day 1";
            d.date = "15 Oct";
            days.add(d);

            d = new Day();
            d.desc = "Day 2";
            d.date = "16 Oct";
            days.add(d);

            d = new Day();
            d.desc = "Day 3";
            d.date = "17 Oct";
            days.add(d);

            d = new Day();
            d.desc = "Day 4";
            d.date = "18 Oct";
            days.add(d);

            d = new Day();
            d.desc = "Day 5";
            d.date = "19 Oct";
            days.add(d);

            d = new Day();
            d.desc = "Day 6";
            d.date = "20 Oct";
            days.add(d);

            d = new Day();
            d.desc = "Day 7";
            d.date = "21 Oct";
            days.add(d);

            patientModel.days = days;

            patientModels.add(patientModel);







//            ArrayList<Encounter> encounters = new ArrayList<>(
//                encounterService.getEncountersByPatient(patient));
//            Collections.sort(encounters, ENCOUNTER_COMPARATOR);
//
//            for (Encounter encounter : encounters) {
//                try {
//                    for (Obs obs : encounter.getAllObs()) {
//                    }
//                } catch (Exception e) {
//                    log.error("Error exporting encounter", e);
//                }
//            }
        }


        model.addAttribute("patients", patientModels);
        model.addAttribute("concepts", questionConcepts);
    }

    public class PatientModel {
        public String patientIdentifier;
        //public ArrayList<Obs> observations;

        public ArrayList<Day> days;

        public String getPatientIdentifier() {
            return patientIdentifier;
        }

        public ArrayList<Day> getDays() {
            return days;
        }
    }

    public class Day {
        public String desc;
        public String date;

        public String getDesc() {
            return desc;
        }

        public String getDate() {
            return date;
        }
    }

    public class ObsConcept {
        public String name;

        public String getName() {
            return name;
        }
    }
}
