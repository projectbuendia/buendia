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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

    public static final int DEFAULT_INTERVAL_MINS = 30;

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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws
        ServletException, IOException {

        // Set the default merge mode
        boolean merge = true;

        // Defines the interval in minutes that will be used to merge encounters.
        int interval = DEFAULT_INTERVAL_MINS;
        String intervalParameter = request.getParameter("interval");
        if (intervalParameter != null) {
            int newInterval = Integer.valueOf(intervalParameter);
            if (newInterval >= 0) {
                interval = newInterval;
                if (interval == 0) {
                    merge = false;
                }
            } else {
                log.error("Interval value is less then 0. Default used.");
            }
        }

        // Defaults to chart order of concepts but if true the ordering will be defined by
        // concept UUID.
        String sortParameter = request.getParameter("sortByUUID");
        boolean sort = (sortParameter != null) && (sortParameter.equals("true"));

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

        LinkedHashSet<Concept> questionConcepts = new LinkedHashSet<>();
        Form form = ChartResource.getCharts(Context.getFormService()).get(0);
        TreeMap<Integer, TreeSet<FormField>> formStructure = FormUtil.getFormStructure(form);
        for (FormField groupField : formStructure.get(0)) {
            for (FormField fieldInGroup : formStructure.get(groupField.getId())) {
                questionConcepts.add(fieldInGroup.getField().getConcept());
            }
        }
        FixedSortedConceptIndexer indexer = new FixedSortedConceptIndexer(questionConcepts, sort);

        writeHeaders(printer, indexer);

        Calendar calendar = Calendar.getInstance();

        // Loop through all the patients and get their encounters.
        for (Patient patient : patients) {

            // Define an array that will represent the line that will be inserted in the CSV.
            Object[] previousCSVLine = new Object[FIXED_HEADERS.length + indexer.size()*COLUMNS_PER_OBS];

            Date deadLine = new Date(0);

            ArrayList<Encounter> encounters = new ArrayList<>(
                encounterService.getEncountersByPatient(patient));
            Collections.sort(encounters, ENCOUNTER_COMPARATOR);

            // TODO: For now patients with no encounters are ignored. List them on the future.
            if (encounters.size() == 0) continue;

            // Loop through all the encounters for this patient to get the observations.
            for (Encounter encounter : encounters) {
                try {
                    // Flag to whether we will use the merged version of the encounter
                    // or the single version.
                    boolean useMerged = merge;

                    // Array that will be used to merge in previous encounter with the current one.
                    Object[] mergedCSVLine = new Object[previousCSVLine.length];

                    // Duplicate previous encounter into the (future to be) merged one.
                    System.arraycopy(previousCSVLine, 0, mergedCSVLine, 0, previousCSVLine.length);

                    // Define the array to be used to store the current encounter.
                    Object[] currentCSVLine = new Object[FIXED_HEADERS.length + indexer.size()*COLUMNS_PER_OBS];

                    // If the current encounter is more then "interval" minutes from the previous
                    // print the previous and reset it.
                    Date encounterTime = encounter.getEncounterDatetime();
                    if (encounterTime.after(deadLine)) {
                        printer.printRecord(previousCSVLine);
                        previousCSVLine = new Object[FIXED_HEADERS.length + indexer.size()*COLUMNS_PER_OBS];
                        useMerged = false;
                    }
                    // Set the next deadline as the current encounter time plus "interval" minutes.
                    calendar.setTime(encounterTime);
                    calendar.add(Calendar.MINUTE, interval);
                    deadLine = calendar.getTime();

                    // Fill the fixed columns values.
                    currentCSVLine[0] = patient.getUuid();
                    currentCSVLine[1] = patient.getPatientIdentifier("MSF");
                    if (patient.getBirthdate() != null) {
                        currentCSVLine[2] = Utils.YYYYMMDD_UTC_FORMAT.format(patient.getBirthdate());
                    }
                    currentCSVLine[3] = encounter.getUuid();
                    currentCSVLine[4] = encounterTime.getTime();
                    currentCSVLine[5] = Utils.toIso8601(encounterTime);
                    currentCSVLine[6] = Utils.SPREADSHEET_FORMAT.format(encounterTime);

                    // Loop through all the observations for this encounter
                    for (Obs obs : encounter.getAllObs()) {
                        Integer index = indexer.getIndex(obs.getConcept());
                        if (index == null) continue;
                        // For each observation there are three columns: if the value of the
                        // observation is a concept, then the three columns contain the English
                        // name, the OpenMRS ID, and the UUID of the concept; otherwise all
                        // three columns contain the formatted value.
                        int valueColumn = FIXED_HEADERS.length + index*COLUMNS_PER_OBS;

                        // Coded values are treated differently
                        if (obs.getValueCoded() != null) {
                            Concept value = obs.getValueCoded();
                            currentCSVLine[valueColumn] = NAMER.getClientName(value);
                            currentCSVLine[valueColumn + 1] = value.getId();
                            currentCSVLine[valueColumn + 2] = value.getUuid();
                            if (useMerged) {
                                // If we are still merging the current encounter values into
                                // the previous one get the previous value and see if it had
                                // something in it.
                                String previousValue = (String) mergedCSVLine[valueColumn];
                                if ((previousValue == null) || (previousValue.isEmpty())
                                    || (previousValue.equals(currentCSVLine[valueColumn].toString()))) {
                                    // If the previous value was empty or equal to the current one
                                    // copy the current value into it.
                                    mergedCSVLine[valueColumn] = currentCSVLine[valueColumn];
                                    mergedCSVLine[valueColumn + 1] = currentCSVLine[valueColumn + 1];
                                    mergedCSVLine[valueColumn + 2] = currentCSVLine[valueColumn + 2];
                                } else {
                                    // If the previous encounter have values stored for this
                                    // observation and the value is different from the previous one
                                    // we cannot merge them anymore.
                                    useMerged = false;
                                }
                            }
                        }
                        // All values except the coded ones will be treated equally.
                        else {
                            // Return the value of the the current observation using the visitor.
                            String value = (String) VisitObsValue.visit(obs, stringVisitor);
                            // Check if we have values stored for this observation
                            if ((value != null) && (!value.isEmpty())) {
                                // Save the value of the observation on the current encounter line.
                                currentCSVLine[valueColumn] = value;
                                currentCSVLine[valueColumn + 1] = value;
                                currentCSVLine[valueColumn + 2] = value;
                                if (useMerged) {
                                    // Since we are still merging this encounter with the previous
                                    // one let's get the previous value to see if it had something
                                    // stored on it.
                                    String previousValue = (String) mergedCSVLine[valueColumn];
                                    if ((previousValue != null) && (!previousValue.isEmpty())) {
                                        // Yes, we had information stored for this observation on
                                        // the previous encounter
                                        if (obs.getValueText() != null) {
                                            // We continue merging if the observation is of
                                            // type text, so we concatenate it.
                                            // TODO: add timestamps to the merged values that are of type text
                                            previousValue += "\n" + value;
                                            value = previousValue;
                                        } else if (!previousValue.equals(currentCSVLine[valueColumn].toString())) {
                                            // If the previous value is different from the current
                                            // one we stop merging.
                                            useMerged = false;
                                        }
                                    }
                                    mergedCSVLine[valueColumn] = value;
                                    mergedCSVLine[valueColumn + 1] = value;
                                    mergedCSVLine[valueColumn + 2] = value;
                                }
                            }
                        }
                    }
                    if (useMerged) {
                        // If after looping through all the observations we didn't had any
                        // overlapped values we keep the merged line.
                        previousCSVLine = mergedCSVLine;
                    } else {
                        // We had overlapped values so let's print the previous line and make the
                        // current encounter the previous one. Only if the previous line is not empty.
                        if (previousCSVLine[0] != null) {
                            printer.printRecord(previousCSVLine);
                        }
                        previousCSVLine = currentCSVLine;
                    }
                } catch (Exception e) {
                    log.error("Error exporting encounter", e);
                }
            }
            // For the last encounter we print the remaining line.
            printer.printRecord(previousCSVLine);
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

        public FixedSortedConceptIndexer(Collection<Concept> concepts, boolean sort) {
            this.concepts = concepts.toArray(new Concept[concepts.size()]);
            if (sort) {
                Arrays.sort(this.concepts, CONCEPT_COMPARATOR);
            }
        }

        public FixedSortedConceptIndexer(Collection<Concept> concepts) {
            this(concepts, true);
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
