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
import org.openmrs.hl7.HL7Constants;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.projectbuendia.ObsValueVisitor;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.webservices.rest.ChartResource;
import org.openmrs.projectbuendia.webservices.rest.DbUtils;
import org.openmrs.projectbuendia.webservices.rest.ObsUtils;
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CSVPrinter printer = new CSVPrinter(
            response.getWriter(), CSVFormat.EXCEL.withDelimiter(','));
        final Locale locale = DbUtils.getLocaleForTag(request.getParameter("locale"));

        if (!XformsUtil.isAuthenticated(request, response, null)) return;

        Date now = new Date();
        DateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String filename = String.format("buendia-%s.csv", format.format(now));
        String contentDispositionHeader = String.format("attachment; filename=%s;", filename);
        response.addHeader("Content-Disposition", contentDispositionHeader);

        PatientService patientService = Context.getPatientService();
        EncounterService encounterService = Context.getEncounterService();

        List<Patient> patients = new ArrayList<>(patientService.getAllPatients());
        Collections.sort(patients, PATIENT_COMPARATOR);

        // To keep ordering consistent, we emit the observations in UUID order, while
        // using the chart forms to select the concepts to display.
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
        writeHeaders(printer, indexer, locale);

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
                    final Object[] cells = new Object[numCols];
                    Arrays.fill(cells, "");
                    cells[0] = patient.getUuid();
                    cells[1] = patient.getPatientIdentifier("MSF");
                    if (patient.getBirthdate() != null) {
                        cells[2] = Utils.YYYYMMDD_UTC_FORMAT.format(patient.getBirthdate());
                    }
                    cells[3] = encounter.getUuid();
                    cells[4] = encounter.getEncounterDatetime().getTime();
                    cells[5] = Utils.formatUtc8601(encounter.getEncounterDatetime());
                    cells[6] = Utils.SPREADSHEET_FORMAT.format(encounter.getEncounterDatetime());
                    for (Obs obs : encounter.getAllObs()) {
                        Integer index = indexer.getIndex(obs.getConcept());
                        if (index == null) continue;
                        // For each observation there are two columns.  If the value is a concept,
                        // they contain the English name and the compressed concept ID; otherwise,
                        // both columns contain the formatted value.
                        final int col = numEncCols + index * COLUMNS_PER_OBS;

                        String hl7Type = obs.getConcept().getDatatype().getHl7Abbreviation();
                        switch (hl7Type) {
                            case HL7Constants.HL7_BOOLEAN:
                                Boolean bool = obs.getValueAsBoolean();
                                cells[col] = cells[col + 1] = bool == null ? "" : Boolean.toString(bool);
                                break;
                            case HL7Constants.HL7_CODED:
                            case HL7Constants.HL7_CODED_WITH_EXCEPTIONS:
                                Concept coded = obs.getValueCoded();
                                cells[col] =
                                    coded == null ? "" : DbUtils.getConceptName(coded, locale);
                                cells[col + 1] =
                                    coded == null ? "" : compressUuid(coded.getUuid());
                                break;
                            case HL7Constants.HL7_NUMERIC:
                                Double numeric = obs.getValueNumeric();
                                cells[col] = cells[col + 1] =
                                    numeric == null ? "" : Double.toString(numeric);
                                break;
                            case HL7Constants.HL7_TEXT:
                                String text = obs.getValueText();
                                cells[col] = cells[col + 1] = text == null ? "" : text;
                                break;
                            case HL7Constants.HL7_DATE:
                                Date date = obs.getValueDate();
                                cells[col] = cells[col + 1] = date == null ? "" : Utils.YYYYMMDD_UTC_FORMAT.format(date);
                                break;
                            case HL7Constants.HL7_DATETIME:
                                Date datetime = obs.getValueDatetime();
                                cells[col] = cells[col + 1] = datetime == null ? "" : Utils.formatUtc8601(datetime);
                                break;
                        }
                    }
                    printer.printRecord(cells);
                } catch (Exception e) {
                    log.error("Error exporting encounter", e);
                }
            }
        }
    }

    private void writeHeaders(CSVPrinter printer, FixedSortedConceptIndexer indexer, Locale locale) throws
        IOException {
        for (String fixedHeader : ENCOUNTER_COLUMNS) {
            printer.print(fixedHeader);
        }
        for (int i = 0; i < indexer.size(); i++) {
            // For each observation there are three columns: one for the English
            // name, one for the OpenMRS ID, and one for the UUID of the concept.
            assert COLUMNS_PER_OBS == 2;
            Concept concept = indexer.getConcept(i);
            printer.print(DbUtils.getConceptName(concept, locale));
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
