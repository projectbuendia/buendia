package org.openmrs.projectbuendia.servlet;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.xforms.util.XformsUtil;
import org.openmrs.projectbuendia.webservices.rest.ChartResource;
import org.openmrs.util.FormUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A servlet for serving up a patient data dump
 */
public class DataExportServlet extends HttpServlet {

    private static final Comparator<Patient> PATIENT_COMPARATOR = new Comparator<Patient>() {
        @Override
        public int compare(Patient p1, Patient p2) {
            return p1.getPatientIdentifier("MSF").compareTo(p2.getPatientIdentifier("MSF"));
        }
    };
    private static final Comparator<Encounter> ENCOUNTER_COMPARATOR = new Comparator<Encounter>() {
        @Override
        public int compare(Encounter e1, Encounter e2) {
            return e1.getEncounterDatetime().compareTo(e2.getEncounterDatetime());
        }
    };
    private static final Comparator<Concept> CONCEPT_COMPARATOR = new Comparator<Concept>() {
        @Override
        public int compare(Concept c1, Concept c2) {
            return c1.getUuid().compareTo(c2.getUuid());
        }
    };

    /**
     * Indexes a fixed set of concepts in sorted UUID order.
     */
    private static class FixedSortedConceptIndexer {
        final Concept [] concepts;

        public FixedSortedConceptIndexer(Collection<Concept> concepts) {
            this.concepts = new Concept[concepts.size()];
            int index = 0;
            for (Concept concept : concepts) {
                this.concepts[index++] = concept;
            }
            Arrays.sort(this.concepts, CONCEPT_COMPARATOR);
        }

        public Integer getIndex(Concept concept) {
            int index = Arrays.binarySearch(concepts, concept, CONCEPT_COMPARATOR);
            if (index < 0) {
                return null;
            }
            return index;
        }

        public Concept getConcept(int i) {
            return concepts[i];
        }

        public int size() {
            return concepts.length;
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();

        //check for authenticated users
        if (!XformsUtil.isAuthenticated(request, response, null)) {
            return;
        }

        PatientService patientService = Context.getPatientService();
        EncounterService encounterService = Context.getEncounterService();

        List<Patient> patients = new ArrayList<>(patientService.getAllPatients());
        Collections.sort(patients, PATIENT_COMPARATOR);

        // We may want to get the observations displayed in the chart/xform, in which case there are a few
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
        final String[] fixedHeaders = new String[]{"Patient UUID",
                "MSF Patient Id",
                "Encounter UUID",
                "Encounter time milliseconds",
                "Encounter time string"};
        final StringBuilder headers = new StringBuilder("");
        for (String fixedHeader : fixedHeaders) {
            headers.append(fixedHeader);
            headers.append(",");
        }
        for (int i=0; i<indexer.size(); i++) {
            headers.append(indexer.getConcept(i).getName());
            headers.append(",");
        }
        writer.println(headers);

        // Write UUID headers
        final StringBuilder uuidHeaders = new StringBuilder("");
        for (int i=0; i<fixedHeaders.length; i++) {
            uuidHeaders.append(",");
        }
        for (int i=0; i<indexer.size(); i++) {
            uuidHeaders.append(indexer.getConcept(i).getUuid());
            uuidHeaders.append(",");
        }
        writer.println(uuidHeaders);

        // Write one encounter per line
        Object [] values = new Object[fixedHeaders.length + indexer.size()];
        final StringBuilder formatStringBuilder = new StringBuilder("");
        for (int i=0; i<values.length; i++) {
            formatStringBuilder.append("%s,");
        }
        formatStringBuilder.append("\n");
        final String formatString = formatStringBuilder.toString();
        for (Patient patient : patients) {
            ArrayList<Encounter> encounters = new ArrayList<>(encounterService.getEncountersByPatient(patient));
            Collections.sort(encounters, ENCOUNTER_COMPARATOR);
            for (Encounter encounter : encounters) {
                values[0] = patient.getUuid();
                values[1] = patient.getPatientIdentifier("MSF");
                values[2] = encounter.getUuid();
                values[3] = encounter.getEncounterDatetime().getTime();
                values[4] = encounter.getEncounterDatetime().toString();
                Arrays.fill(values, fixedHeaders.length + 1, fixedHeaders.length + indexer.size(), "");
                for (Obs obs : encounter.getAllObs()) {
                    Integer index = indexer.getIndex(obs.getConcept());
                    if (index == null) {
                        continue;
                    }
                    values[fixedHeaders.length + index] = obs.getValueAsString(Locale.ENGLISH);
                }
                writer.printf(formatString, values);
            }
        }
    }
}
