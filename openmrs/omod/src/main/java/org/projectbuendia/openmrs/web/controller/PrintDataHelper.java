package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
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
import java.util.Locale;
import java.util.Map;

import static org.openmrs.projectbuendia.Utils.eq;
import static org.openmrs.projectbuendia.Utils.parse8601;

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
    private static final String ADMISSION_TIME_UUID = Utils.toUuid(8001640);

    private DateTimeZone zone;

    private static final Comparator<Encounter> ENCOUNTER_TIME = new Comparator<Encounter>() {
        @Override public int compare(Encounter e1, Encounter e2) {
            Date dt1 = e1.getEncounterDatetime();
            Date dt2 = e2.getEncounterDatetime();
            return dt1.compareTo(dt2);
        }
    };

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

    private final Comparator<PatientPlacement> PATIENT_PLACEMENT = new Comparator<PatientPlacement>() {
        @Override public int compare(PatientPlacement p1, PatientPlacement p2) {
            String ln1 = p1.location.getName();
            String ln2 = p2.location.getName();
            if (!eq(ln1, ln2)) return Utils.ALPHANUMERIC_COMPARATOR.compare(ln1, ln2);

            return Utils.ALPHANUMERIC_COMPARATOR.compare(p1.placement, p2.placement);
        }
    };

    public PrintDataHelper(DateTimeZone zone) {
        this.zone = zone;

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

    private List<ObsDisplay> wrapObs(List<Obs> list) {
        List<ObsDisplay> wrapped = new ArrayList<>();
        for (Obs obs : list) {
            wrapped.add(new ObsDisplay(obs));
        }
        return wrapped;
    }

    public List<ObsDisplay> getAdmissionTimes() {
        List<Obs> admissionTimes = new ArrayList<>(getLatestObsByPatient(ADMISSION_TIME_UUID).values());
        Collections.sort(admissionTimes, OBS_VALUE_TIME);
        return wrapObs(admissionTimes);
    }

    public List<ObsDisplay> getDischarges() {
        List<Obs> discharges = new ArrayList<>();
        Map<String, Obs> placements = getLatestObsByPatient(
            DbUtils.CONCEPT_PLACEMENT_UUID);
        for (String patientUuid : placements.keySet()) {
            Obs obs = placements.get(patientUuid);
            String placement = obs.getValueText();
            String locationUuid = getLocationFromPlacement(placement);
            if (eq(locationUuid, DISCHARGED_LOCATION_UUID)) {
                discharges.add(obs);
            }
        }
        Collections.sort(discharges, OBS_ENCOUNTER_TIME);
        return wrapObs(discharges);
    }

    private static String getLocationFromPlacement(String placement) {
        String locationUuid = Utils.splitFields(placement, "/", 2)[0];
        return locationUuid;
    }

    private static DateTime getLocalDateTime(Obs obs) {
        return Utils.toLocalDateTime(
            obs.getEncounter().getEncounterDatetime().getTime());
    }

    public List<PatientPlacement> getPresentPatients() {
        List<PatientPlacement> results = new ArrayList<>();
        Map<String, Obs> placements = getLatestObsByPatient(
            DbUtils.CONCEPT_PLACEMENT_UUID);
        for (String patientUuid : placements.keySet()) {
            Obs obs = placements.get(patientUuid);
            String placement = obs.getValueText();
            String locationUuid = getLocationFromPlacement(placement);
            if (!eq(locationUuid, DISCHARGED_LOCATION_UUID)) {
                Patient patient = patientService.getPatientByUuid(patientUuid);
                results.add(new PatientPlacement(patient, placement));
            }
        }
        Collections.sort(results, PATIENT_PLACEMENT);
        return results;
    }

    public Map<String, Obs> getLatestObsByPatient(String conceptUuid) {
        Map<String, Obs> results = new HashMap<>();
        Concept concept = conceptService.getConceptByUuid(conceptUuid);
        if (concept != null) {
            for (Patient patient : patientService.getAllPatients()) {
                // getObservations promises to sort chronologically by default.
                for (Obs obs : obsService.getObservations(
                    Arrays.asList((Person) patient), null,
                    Arrays.asList(concept), null, null, null, null,
                    1, null, null, null, false)) {
                    results.put(obs.getPerson().getUuid(), obs);
                }
            }
        }
        return results;
    }

    public Patient getPatient(String uuid) {
        return patientService.getPatientByUuid(uuid);
    }

    public List<Encounter> getEncounters(Patient patient) {
        // getEncounters promises to sort its results chronologically.
        List<Encounter> encounters = encounterService.getEncounters(
            patient, null, null, null,
            null, null, null, null, null, false);
        Collections.sort(encounters, ENCOUNTER_TIME);
        return encounters;
    }

    private static int MAX_GAP = 5 * 1000;
    private static int MAX_DURATION = 15 * 1000;

    public List<List<Obs>> getEncounterObs(List<Encounter> encounters) {
        List<List<Obs>> results = new ArrayList<>();
        List<Obs> group = new ArrayList<>();
        long groupStartTime = 0;
        long lastTime = 0;
        for (Encounter encounter : encounters) {
            long time = encounter.getEncounterDatetime().getTime();
            if (time > lastTime + MAX_GAP || time > groupStartTime + MAX_DURATION) {
                if (group.size() > 0) results.add(group);
                group = new ArrayList<>();
            }
            if (group.size() == 0) {
                groupStartTime = time;
            }
            for (Obs obs : encounter.getObs()) {
                group.add(obs);
            }
            lastTime = time;
        }
        if (group.size() > 0) results.add(group);
        return results;
    }

    public List<Obs> getObs(Encounter encounter) {
        List<Obs> obs = new ArrayList<>(encounter.getAllObs());
        return obs;
    }

    public class ObsDisplay {
        private Obs obs;
        private Date encounterDatetime;
        private Date valueDatetime;

        public ObsDisplay(Obs obs) {
            this.obs = obs;
            encounterDatetime = Utils.toLocalDateTime(
                obs.getEncounter().getEncounterDatetime().getTime(), zone).toDate();
            if (obs.getValueDatetime() != null) {
                valueDatetime = Utils.toLocalDateTime(
                    obs.getValueDatetime().getTime(), zone).toDate();
            }
        }

        public Obs getObs() {
            return obs;
        }

        public Patient getPatient() {
            return obs.getPatient();
        }

        public Date getEncounterDatetime() {
            return encounterDatetime;
        }

        public Date getValueDatetime() {
            return valueDatetime;
        }
    }

    public class PatientPlacement {
        private Patient patient;
        private String placement;
        private Location location;
        private String locationName;
        private String bed;

        public PatientPlacement(Patient patient, String placement) {
            this.patient = patient;
            this.placement = placement;
            this.location = locationService.getLocationByUuid(getLocationFromPlacement(placement));
            this.locationName = Utils.localize(this.location.getName());
            this.bed = Utils.splitFields(placement, "/", 2)[1];
        }

        public Patient getPatient() {
            return patient;
        }

        public String getPlacement() {
            return placement;
        }

        public Location getLocation() {
            return location;
        }

        public String getLocationName() {
            return locationName;
        }

        public String getBed() {
            return bed;
        }
    }
}
