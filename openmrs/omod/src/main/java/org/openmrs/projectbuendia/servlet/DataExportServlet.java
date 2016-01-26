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

package org.openmrs.projectbuendia.servlet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.projectbuendia.ClientConceptNamer;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.VisitObsValue;
import org.openmrs.projectbuendia.webservices.rest.ChartResource;
import org.openmrs.util.FormUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** A servlet that generates a CSV dump of all the patient data. */
public class DataExportServlet extends HttpServlet {
    protected static Log log = LogFactory.getLog(DataExportServlet.class);

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
    private static final Comparator<Concept> CONCEPT_COMPARATOR = new Comparator<Concept>() {
        @Override public int compare(Concept c1, Concept c2) {
            return c1.getUuid().compareTo(c2.getUuid());
        }
    };
    private static final String[] FIXED_HEADERS = new String[] {
        "Patient UUID",
        "MSF patient ID",
        "Approximate date of birth",
        "Encounter UUID",
        "Time in epoch milliseconds",
        "Time in ISO8601 UTC",
        "Time in yyyy-MM-dd HH:mm:ss UTC",
    };
    private static final int COLUMNS_PER_OBS = 3;
    private static final ClientConceptNamer NAMER = new ClientConceptNamer(Locale.ENGLISH);

    public static final int DEFAULT_INTERVAL = 30;

    private VisitObsValue.ObsValueVisitor stringVisitor;
    private void defineStringVisitor() {
        stringVisitor = new VisitObsValue.ObsValueVisitor<String>() {
            @Override public String visitCoded(Concept value) {
                //if (value != null || value.getUuid() != null || !value.getUuid().isEmpty()) {
                    /*values[valueColumn] = NAMER.getClientName(value);
                    values[valueColumn + 1] = value.getId();
                    values[valueColumn + 2] = value.getUuid();*/
                    return NAMER.getClientName(value);
                //}
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
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws
        ServletException, IOException {

        // Defines the interval in minutes that will be used to merge encounters.
        int interval = DEFAULT_INTERVAL;
        String intervalParameter = request.getParameter("interval");
        if (intervalParameter != null) {
            interval = Integer.valueOf(intervalParameter);
        }

        CSVPrinter printer = new CSVPrinter(response.getWriter(), CSVFormat.EXCEL.withDelimiter(','));

        //check for authenticated users
        if (!XformsUtil.isAuthenticated(request, response, null)) return;

        Date now = new Date();
        DateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String filename = String.format("buendiadata_%s.csv", format.format(now));
        String contentDispositionHeader = String.format("attachment; filename=%s;", filename);
        response.addHeader("Content-Disposition", contentDispositionHeader);

        PatientService patientService = Context.getPatientService();
        EncounterService encounterService = Context.getEncounterService();

        List<Patient> patients = new ArrayList<>(patientService.getAllPatients());
        Collections.sort(patients, PATIENT_COMPARATOR);

        // We may want to get the observations displayed in the chart/xform, in which case there
        // are a few
        // sensible orders:
        // 1: UUID
        // 2: Order in chart
        // 3: Order in Xform

        // Order in Xform/chart is not good as stuff changes every time we change xform
        // So instead we will use UUID order, but use the Chart form to use the concepts to display.
        Set<Concept> questionConcepts = new HashSet<>();
        for (Form form : ChartResource.getCharts(Context.getFormService())) {
            TreeMap<Integer, TreeSet<FormField>> formStructure = FormUtil.getFormStructure(form);
            for (FormField groupField : formStructure.get(0)) {
                for (FormField fieldInGroup : formStructure.get(groupField.getId())) {
                    questionConcepts.add(fieldInGroup.getField().getConcept());
                }
            }
        }
        FixedSortedConceptIndexer indexer = new FixedSortedConceptIndexer(questionConcepts);

        // Write English headers
        writeHeaders(printer, indexer);

        Calendar calendar = Calendar.getInstance();
        defineStringVisitor();

        // Write each line with the encounters merged within interval
        for (Patient patient : patients) {

            Object[] values = new Object[FIXED_HEADERS.length + indexer.size()*COLUMNS_PER_OBS];

            Date deadLine = new Date(0);

            ArrayList<Encounter> encounters = new ArrayList<>(
                encounterService.getEncountersByPatient(patient));
            Collections.sort(encounters, ENCOUNTER_COMPARATOR);

            for (int i = 0; i < encounters.size(); i++) {
                try {
                    Encounter encounter = encounters.get(i);
                    Date encounterTime = encounter.getEncounterDatetime();
                    if (encounterTime.after(deadLine)) {
                        printer.printRecord(values);
                        values = new Object[FIXED_HEADERS.length + indexer.size()*COLUMNS_PER_OBS];
                    }
                    calendar.setTime(encounterTime);
                    calendar.add(Calendar.MINUTE, interval);
                    deadLine = calendar.getTime();

                    values[0] = patient.getUuid();
                    values[1] = patient.getPatientIdentifier("MSF");
                    if (patient.getBirthdate() != null) {
                        values[2] = Utils.YYYYMMDD_UTC_FORMAT.format(patient.getBirthdate());
                    }
                    values[3] = encounter.getUuid();
                    values[4] = encounterTime.getTime();
                    values[5] = Utils.toIso8601(encounterTime);
                    values[6] = Utils.SPREADSHEET_FORMAT.format(encounterTime);
                    for (Obs obs : encounter.getAllObs()) {
                        Integer index = indexer.getIndex(obs.getConcept());
                        if (index == null) continue;
                        // For each observation there are three columns: if the value of the
                        // observation is a concept, then the three columns contain the English
                        // name, the OpenMRS ID, and the UUID of the concept; otherwise all
                        // three columns contain the formatted value.
                        int valueColumn = FIXED_HEADERS.length + index*COLUMNS_PER_OBS;

                        if (obs.getValueCoded() != null){
                            Concept value = obs.getValueCoded();
                            values[valueColumn] = NAMER.getClientName(value);
                            values[valueColumn + 1] = value.getId();
                            values[valueColumn + 2] = value.getUuid();
                        } else {
                            String value = (String) VisitObsValue.visit(obs, stringVisitor);
                            values[valueColumn] = value;
                            values[valueColumn + 1] = value;
                            values[valueColumn + 2] = value;
                        }
                    }
                    if (i == (encounters.size() - 1)){
                        printer.printRecord(values);
                    }
                } catch (Exception e) {
                    log.error("Error exporting encounter", e);
                }
            }
        }
    }

    private void writeHeaders(CSVPrinter printer, FixedSortedConceptIndexer indexer) throws
        IOException {
        for (String fixedHeader : FIXED_HEADERS) {
            printer.print(fixedHeader);
        }
        for (int i = 0; i < indexer.size(); i++) {
            // For each observation there are three columns: one for the English
            // name, one for the OpenMRS ID, and one for the UUID of the concept.
            assert COLUMNS_PER_OBS == 3;
            Concept concept = indexer.getConcept(i);
            printer.print(NAMER.getClientName(concept));
            printer.print(concept.getId());
            printer.print(concept.getUuid());
        }
        printer.println();
    }

    /** Indexes a fixed set of concepts in sorted UUID order. */
    private static class FixedSortedConceptIndexer {
        final Concept[] concepts;

        public FixedSortedConceptIndexer(Collection<Concept> concepts) {
            this.concepts = concepts.toArray(new Concept[concepts.size()]);
            Arrays.sort(this.concepts, CONCEPT_COMPARATOR);
        }

        public Integer getIndex(Concept concept) {
            int index = Arrays.binarySearch(concepts, concept, CONCEPT_COMPARATOR);
            if (index < 0) return null;
            return index;
        }

        public Concept getConcept(int i) {
            return concepts[i];
        }

        public int size() {
            return concepts.length;
        }
    }
}
