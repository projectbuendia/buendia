package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.webservices.rest.DbUtils;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openmrs.projectbuendia.Utils.eq;

public class PrintDataHelper {
    protected final ProjectBuendiaService buendiaService;
    protected final ConceptService conceptService;
    protected final EncounterService encounterService;
    protected final FormService formService;
    protected final LocationService locationService;
    protected final ObsService obsService;
    protected final OrderService orderService;
    protected final PatientService patientService;
    protected final ProviderService providerService;

    private static final String DISCHARGED_LOCATION_UUID = "73010980-d3e7-404a-95bf-180966f04fb8";
    private static final String ADMISSION_TIME_UUID = Utils.toUuid(1640);

    private static final Comparator<Obs> OBS_ENCOUNTER_TIME = new Comparator<Obs>() {
        @Override public int compare(Obs o1, Obs o2) {
            Date dt1 = o1.getEncounter().getEncounterDatetime();
            Date dt2 = o2.getEncounter().getEncounterDatetime();
            return dt1.compareTo(dt2);
        }
    };

    private static final Comparator<Obs> OBS_VALUE_TIME = new Comparator<Obs>() {
        @Override public int compare(Obs o1, Obs o2) {
            return o1.getValueDate().compareTo(o2.getValueDate());
        }
    };

    public PrintDataHelper() {
        buendiaService = Context.getService(ProjectBuendiaService.class);
        conceptService = Context.getConceptService();
        encounterService = Context.getEncounterService();
        formService = Context.getFormService();
        locationService = Context.getLocationService();
        orderService = Context.getOrderService();
        obsService = Context.getObsService();
        patientService = Context.getPatientService();
        providerService = Context.getProviderService();
    }

    public List<Obs> getAdmissionTimes(int count) {
        List<Obs> admissionTimes = new ArrayList<>(getLatestObsByPatient(ADMISSION_TIME_UUID).values());
        Collections.sort(admissionTimes, OBS_VALUE_TIME);
        return admissionTimes;
    }

    public List<Obs> getDischarges(int count) {
        List<Obs> discharges = new ArrayList<>();
        Map<String, Obs> placements = getLatestObsByPatient(
            DbUtils.CONCEPT_PLACEMENT_UUID);
        for (String patientUuid : placements.keySet()) {
            Obs obs = placements.get(patientUuid);
            String locationUuid = getLocationFromPlacement(obs);
            if (eq(locationUuid, DISCHARGED_LOCATION_UUID)) {
                discharges.add(obs);
            }
        }
        Collections.sort(discharges, OBS_ENCOUNTER_TIME);
        return discharges;
    }

    private static String getLocationFromPlacement(Obs obs) {
        String placement = obs.getValueText();
        String locationUuid = Utils.splitFields(placement, "/", 2)[0];
        return locationUuid;
    }

    private static DateTime getLocalDateTime(Obs obs) {
        return Utils.toLocalDateTime(
            obs.getEncounter().getEncounterDatetime().getTime());
    }

    public List<Patient> getPresentPatients() {
        List<Patient> patients = new ArrayList<>();
        Map<String, Obs> placements = getLatestObsByPatient(
            DbUtils.CONCEPT_PLACEMENT_UUID);
        for (String patientUuid : placements.keySet()) {
            String locationUuid = getLocationFromPlacement(placements.get(patientUuid));
            if (!eq(locationUuid, DISCHARGED_LOCATION_UUID)) {
                patients.add(patientService.getPatientByUuid(patientUuid));
            }
        }
        return patients;
    }

    public Map<String, Obs> getLatestObsByPatient(String conceptUuid) {
        Map<String, Obs> results = new HashMap<>();
        Concept concept = conceptService.getConceptByUuid(conceptUuid);
        if (concept != null) {
            for (Obs obs : obsService.getObservations(
                null, null, Arrays.asList(concept), null,
                null, null, null, 1, null, null, null, false)) {
                results.put(obs.getPerson().getUuid(), obs);
            }
        }
        return results;
    }
}
