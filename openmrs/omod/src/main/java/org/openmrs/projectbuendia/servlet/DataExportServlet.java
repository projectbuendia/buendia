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

import static org.openmrs.projectbuendia.webservices.rest.ChartResource.compressUuid;

/** A servlet that generates a CSV dump of all the patient data. */
public class DataExportServlet extends HttpServlet {
    protected static Log log = LogFactory.getLog(DataExportServlet.class);

    private static final Comparator<Patient> PATIENT_COMPARATOR = new Comparator<Patient>() {
        @Override public int compare(Patient p1, Patient p2) {
            PatientIdentifier id1 = p1.getPatientIdentifier("MSF");
            PatientIdentifier id2 = p2.getPatientIdentifier("MSF");
            return Utils.ALPHANUMERIC_COMPARATOR.compare(
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
    private static final String[] ENCOUNTER_COLUMNS = new String[] {
        "Patient UUID",
        "MSF patient ID",
        "Approximate date of birth",
        "Encounter UUID",
        "Time in epoch milliseconds",
        "Time in ISO8601 UTC",
        "Time in yyyy-MM-dd HH:mm:ss UTC",
    };
    private static final int COLUMNS_PER_OBS = 2;
    private static final ClientConceptNamer NAMER = new ClientConceptNamer(Locale.ENGLISH);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws
        ServletException, IOException {
        CSVPrinter printer = new CSVPrinter(response.getWriter(), CSVFormat.EXCEL.withDelimiter
            (','));

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
        for (Form form : ChartResource.getChartForms(Context.getFormService())) {
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

        // Write one encounter per line
        for (Patient patient : patients) {
            ArrayList<Encounter> encounters = new ArrayList<>(encounterService
                .getEncountersByPatient(patient));
            Collections.sort(encounters, ENCOUNTER_COMPARATOR);
            int numEncCols = ENCOUNTER_COLUMNS.length;
            int numObsCols = indexer.size() * COLUMNS_PER_OBS;
            int numCols = numEncCols + numObsCols;
            for (Encounter encounter : encounters) {
                try {
                    final Object[] values = new Object[numCols];
                    Arrays.fill(values, "");
                    values[0] = patient.getUuid();
                    values[1] = patient.getPatientIdentifier("MSF");
                    if (patient.getBirthdate() != null) {
                        values[2] = Utils.YYYYMMDD_UTC_FORMAT.format(patient.getBirthdate());
                    }
                    values[3] = encounter.getUuid();
                    values[4] = encounter.getEncounterDatetime().getTime();
                    values[5] = Utils.formatUtc8601(encounter.getEncounterDatetime());
                    values[6] = Utils.SPREADSHEET_FORMAT.format(encounter.getEncounterDatetime());
                    for (Obs obs : encounter.getAllObs()) {
                        Integer index = indexer.getIndex(obs.getConcept());
                        if (index == null) continue;
                        // For each observation there are two columns.  If the value is a concept,
                        // they contain the English name and the compressed concept ID; otherwise,
                        // both columns contain the formatted value.
                        final int col = numEncCols + index * COLUMNS_PER_OBS;
                        VisitObsValue.visit(obs, new VisitObsValue.ObsValueVisitor<Void>() {
                            @Override public Void visitCoded(Concept value) {
                                if (value == null || value.getUuid() == null || value.getUuid().isEmpty()) {
                                    values[col] = values[col + 1] = "";
                                } else {
                                    values[col] = NAMER.getClientName(value);
                                    values[col + 1] = compressUuid(value.getUuid());
                                }
                                return null;
                            }

                            @Override public Void visitNumeric(Double value) {
                                values[col] = values[col + 1] = value != null ? Double.toString(value) : "";
                                return null;
                            }

                            @Override public Void visitBoolean(Boolean value) {
                                values[col] = values[col + 1] = value != null ? Boolean.toString(value) : "";
                                return null;
                            }

                            @Override public Void visitText(String value) {
                                values[col] = values[col + 1] = value != null ? value : "";
                                return null;
                            }

                            @Override public Void visitDate(Date d) {
                                values[col] = values[col + 1] = d != null ? Utils.YYYYMMDD_UTC_FORMAT.format(d) : "";
                                return null;
                            }

                            @Override public Void visitDatetime(Date d) {
                                values[col] = values[col + 1] = d != null ? Utils.SPREADSHEET_FORMAT.format(d) : "";
                                return null;
                            }
                        });
                    }
                    printer.printRecord(values);
                } catch (Exception e) {
                    log.error("Error exporting encounter", e);
                }
            }
        }
    }

    private void writeHeaders(CSVPrinter printer, FixedSortedConceptIndexer indexer) throws
        IOException {
        for (String fixedHeader : ENCOUNTER_COLUMNS) {
            printer.print(fixedHeader);
        }
        for (int i = 0; i < indexer.size(); i++) {
            // For each observation there are three columns: one for the English
            // name, one for the OpenMRS ID, and one for the UUID of the concept.
            assert COLUMNS_PER_OBS == 2;
            Concept concept = indexer.getConcept(i);
            printer.print(NAMER.getClientName(concept));
            printer.print(compressUuid(concept.getUuid()));
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
