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




        PatientService patientService = Context.getPatientService();
        EncounterService encounterService = Context.getEncounterService();

        List<Patient> patients = new ArrayList<>(patientService.getAllPatients());
        Collections.sort(patients, PATIENT_COMPARATOR);

        Set<Concept> questionConcepts = new HashSet<>();
        for (Form form : ChartResource.getCharts(Context.getFormService())) {
            TreeMap<Integer, TreeSet<FormField>> formStructure = FormUtil.getFormStructure(form);
            for (FormField groupField : formStructure.get(0)) {
                for (FormField fieldInGroup : formStructure.get(groupField.getId())) {
                    questionConcepts.add(fieldInGroup.getField().getConcept());
                }
            }
        }

        for (Patient patient : patients) {

            ArrayList<Encounter> encounters = new ArrayList<>(
                encounterService.getEncountersByPatient(patient));
            Collections.sort(encounters, ENCOUNTER_COMPARATOR);

            for (Encounter encounter : encounters) {
                try {
                    for (Obs obs : encounter.getAllObs()) {
                    }
                } catch (Exception e) {
                    log.error("Error exporting encounter", e);
                }
            }
        }





    }

    @RequestMapping(value = "/module/projectbuendia/openmrs/print-charts", method = RequestMethod.POST)
    public View post(HttpServletRequest request, HttpServletResponse response, ModelMap model) {

        return new RedirectView("print-charts.form");  // reload this page with a GET request
    }
}
