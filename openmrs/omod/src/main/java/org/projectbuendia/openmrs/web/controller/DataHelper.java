package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Form;
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
import org.openmrs.projectbuendia.Intl;
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

public class DataHelper {
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

    private static final Intl TIME_PATTERN = new Intl("MMM d, HH:mm [fr:d MMM, HH'h'mm]");
    private static final Intl DATE_PATTERN = new Intl("MMM d [fr:d MMM]");

    private DateTimeZone zone;
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter dateFormatter;

    private static final Comparator<Encounter> ENCOUNTER_TIME = new Comparator<Encounter>() {
        @Override public int compare(Encounter e1, Encounter e2) {
            DateTime dt1 = toDateTime(e1.getEncounterDatetime());
            DateTime dt2 = toDateTime(e2.getEncounterDatetime());
            return dt1.compareTo(dt2);
        }
    };

    private final Comparator<Obs> OBS_TIME = new Comparator<Obs>() {
        @Override public int compare(Obs o1, Obs o2) {
            DateTime dt1 = toDateTime(o1.getObsDatetime());
            DateTime dt2 = toDateTime(o2.getObsDatetime());
            return dt1.compareTo(dt2);
        }
    };

    private static final Comparator<Obs> OBS_VALUE_TIME = new Comparator<Obs>() {
        @Override public int compare(Obs o1, Obs o2) {
            DateTime dt1 = toDateTime(o1.getValueDatetime());
            DateTime dt2 = toDateTime(o2.getValueDatetime());
            return dt1.compareTo(dt2);
        }
    };

    private final Comparator<PatientPlacement> PATIENT_PLACEMENT = new Comparator<PatientPlacement>() {
        @Override public int compare(PatientPlacement p1, PatientPlacement p2) {
            String ln1 = p1.placement.location.getName();
            String ln2 = p2.placement.location.getName();
            if (!eq(ln1, ln2)) return Utils.ALPHANUMERIC_COMPARATOR.compare(ln1, ln2);
            return Utils.ALPHANUMERIC_COMPARATOR.compare(p1.placement.bed, p2.placement.bed);
        }
    };

    private final Comparator<Form> FORM_TITLE = new Comparator<Form>() {
        @Override public int compare(Form f1, Form f2) {
            return Utils.ALPHANUMERIC_COMPARATOR.compare(f1.getName(), f2.getName());
        }
    };

    public DataHelper(DateTimeZone zone, Locale locale) {
        this.zone = zone;
        timeFormatter = DateTimeFormat.forPattern(TIME_PATTERN.get(locale));
        dateFormatter = DateTimeFormat.forPattern(DATE_PATTERN.get(locale));

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

    public String formatTime(DateTime dt) {
        return timeFormatter.print(dt);
    }

    public String formatDate(DateTime dt) {
        return dateFormatter.print(dt);
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
            String locationUuid = getLocationUuidFromPlacement(placement);
            if (eq(locationUuid, DISCHARGED_LOCATION_UUID)) {
                discharges.add(obs);
            }
        }
        Collections.sort(discharges, OBS_TIME);
        return wrapObs(discharges);
    }

    public static String getLocationUuidFromPlacement(String placement) {
        return Utils.splitFields(placement, "/", 2)[0];
    }

    public List<PatientPlacement> getPresentPatients() {
        List<PatientPlacement> results = new ArrayList<>();
        Map<String, Obs> placements = getLatestObsByPatient(
            DbUtils.CONCEPT_PLACEMENT_UUID);
        for (String patientUuid : placements.keySet()) {
            Obs obs = placements.get(patientUuid);
            String placement = obs.getValueText();
            String locationUuid = getLocationUuidFromPlacement(placement);
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

    private static int MAX_GAP = 5 * 60 * 1000;
    private static int MAX_DURATION = 15 * 60 * 1000;

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

    public List<Form> getForms() {
        List<Form> forms = new ArrayList<>();
        for (Form form : formService.getAllForms()) {
            if (form.getPublished()) {
                forms.add(form);
            }
        }
        Collections.sort(forms, FORM_TITLE);
        return forms;
    }

    public Intl getName(Concept concept) {
        return new Intl(DbUtils.getConceptName(concept));
    }

    public static DateTime toDateTime(Date date) {
        if (date == null) return null;

        // Dates in the MySQL database are stored as their components
        // (e.g. "2019-12-05 11:39:00").  When OpenMRS loads them, it
        // unfortunately interprets the date and time in the local
        // time zone, which means that OpenMRS databaxses will randomly
        // corrupt dates and times when the server's time zone changes
        // or the database is copied to another system.
        //
        // To avoid any ambiguity about stored dates and times, all
        // Buendia systems are required to use UTC as the system-wide
        // time zone.  To interpret a date and time properly, then,
        // we must interpret the components as though they expressed
        // a time in UTC.  It is never safe to use the horrible Java
        // Date class; all date and time calculations within Buendia
        // are performed using the Joda library.  All operations in
        // Buendia should be completely firewalled from Date objects
        // using this function.
        return new DateTime(
            date.getYear(),
            date.getMonth() + 1,  // Java insanely represents months as 0-11
            date.getDate(),  // a terrible name for "day of the month"
            date.getHours(),
            date.getMinutes(),
            date.getSeconds(),
            DateTimeZone.UTC
        );
    }

    public DateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return new DateTime(toDateTime(date).getMillis(), zone);
    }

    public Instant toInstant(Date date) {
        if (date == null) return null;
        return new Instant(toDateTime(date).getMillis());
    }

    public boolean inOrder(Date a, Date b) {
        return toInstant(a).isBefore(toInstant(b));
    }

    public DateTime getObsLocalTime(Obs obs) {
        if (obs == null) return null;
        return toLocalDateTime(obs.getObsDatetime());
    }

    public DateTime getValueLocalTime(Obs obs) {
        if (obs == null) return null;
        return toLocalDateTime(obs.getValueDatetime());
    }

    public class ObsDisplay {
        private Obs obs;
        private DateTime obsTime;
        private DateTime valueTime;

        public ObsDisplay(Obs obs) {
            this.obs = obs;
            this.obsTime = getObsLocalTime(obs);
            this.valueTime = getValueLocalTime(obs);
        }

        public Obs getObs() {
            return obs;
        }

        public Patient getPatient() {
            return obs.getPatient();
        }

        public DateTime getObsTime() {
            return obsTime;
        }

        public DateTime getValueTime() {
            return valueTime;
        }

        public String getFormattedObsTime() {
            return timeFormatter.print(obsTime);
        }

        public String getFormattedValueTime() {
            return timeFormatter.print(valueTime);
        }
    }

    public class PatientPlacement {
        private Patient patient;
        private Placement placement;

        public PatientPlacement(Patient patient, String placement) {
            this.patient = patient;
            this.placement = new Placement(placement);
        }

        public Patient getPatient() {
            return patient;
        }

        public Placement getPlacement() {
            return placement;
        }
    }

    public Placement getPlacement(Obs obs) {
        return new Placement(obs);
    }

    public class Placement {
        private String placement;
        private String locationUuid;
        private Location location;
        private String locationName;
        private String bed;

        public Placement(String placement) {
            this.placement = placement;
            locationUuid = getLocationUuidFromPlacement(placement);
            location = locationService.getLocationByUuid(locationUuid);
            locationName = Utils.localize(location.getName());
            bed = Utils.splitFields(placement, "/", 2)[1];
        }

        public Placement(Obs obs) {
            this(obs.getValueText());
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
