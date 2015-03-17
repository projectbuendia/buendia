package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.DateTimeUtils;
import org.openmrs.projectbuendia.VisitObsValue;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Resource representing multiple observations for a single patient, taken within a single encounter (e.g. a single
 * form entry).
 *
 * @see AbstractReadOnlyResource
 */
// TODO: Ideally, this would be under patient/{uuid}/encounters; it's unclear whether
// that can be supported here...
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patientencounters",
        supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class PatientEncountersResource extends AbstractReadOnlyResource<Patient> implements Creatable {

    // JSON property names
    private static final String UUID = "uuid";
    private static final String ENCOUNTERS = "encounters";
    private static final String OBSERVATIONS = "observations";
    private static final String TIMESTAMP = "timestamp";
    public static final String START_MILLISECONDS_INCLUSIVE = "sm";

    private final PatientService patientService;
    private final EncounterService encounterService;
    private final ObservationsHandler observationsHandler = new ObservationsHandler();

    public PatientEncountersResource() {
        super("patient", Representation.DEFAULT);
        patientService = Context.getPatientService();
        encounterService = Context.getEncounterService();
    }

    /**
     * Retrieves patient encounters for a single patient with the given UUID.
     *
     * @see AbstractReadOnlyResource#retrieve(String, RequestContext)
     * @param context the request context; the following parameters are supported:
     *                <ul>
     *                  <li>sm: timestamp, in ms; if specified, only encounters created after this time (inclusive) will
     *                      be returned
     *                </ul>
     */
    @Override
    protected Patient retrieveImpl(String uuid, RequestContext context, long snapshotTime) {
        return patientService.getPatientByUuid(uuid);
    }

    /**
     * Returns patient encounters for all patients (there is no support for query parameters).
     *
     * @see AbstractReadOnlyResource#search(RequestContext)
     * @param context the request context; the following parameters are supported:
     *                <ul>
     *                  <li>sm: timestamp, in ms; if specified, only encounters created after this time (inclusive) will
     *                      be returned
     *                </ul>
     */
    @Override
    public List<Patient> searchImpl(RequestContext context, long snapshotTime) {
        return patientService.getAllPatients();
    }

    /**
     * Always adds the following fields to the {@link SimpleObject}:
     * <ul>
     *     <li>encounters: {@link List} of {@link SimpleObject}'s, each containing the following pairs:
     *         <ul>
     *             <li>timestamp: ISO6801-formatted timestamp, accurate to the minute
     *             <li>uuid: unique identifier for this encounter
     *             <li>observations: {@link java.util.Map} of concept UUID's to values
     *         </ul>
     * </ul>
     *
     * @param context the request context; specify "v=full" in the URL params for verbose output
     */
    @Override
    protected void populateJsonProperties(Patient patient, RequestContext context, SimpleObject json,
                                          long snapshotTime) {
        String parameter = context.getParameter(START_MILLISECONDS_INCLUSIVE);
        Long startMillisecondsInclusive = null;
        if (parameter != null) {
            // Fail fast throwing number format exception to aid debugging.
            startMillisecondsInclusive = Long.parseLong(parameter);
        }
        List<Encounter> encountersByPatient;
        if (startMillisecondsInclusive == null) {
            encountersByPatient = encounterService.getEncountersByPatient(patient);
        } else {
            /* It would be nice to be able to use the getEncounters() method here, which has the following parameters.
             * Unfortunately the date restrictions are on the encounter date, not on the creation/modification date.
             * This means we would not get encounters added in the past by a later sync. Using creation/modification
             * date as a feature has been added as a feature request to
             * OpenMRS at https://issues.openmrs.org/browse/TRUNK-4571
             *
             * Until this is done, we have two options. 1 Use the DAO directly, hooking in to the spring injection
             * code to get it. 2 load the encounters, and then filter before getting the observations, hoping this is
             * efficient enough. For now we are going with 2.
             *
             * Nullable parameters for getEncounters(), put here for easier readability:
             * who - the patient the encounter is for
             * loc - the location this encounter took place
             * fromDate - the minimum date (inclusive) this encounter took place
             * toDate - the maximum date (exclusive) this encounter took place
             * enteredViaForms - the form that entered this encounter must be in this list
             * encounterTypes - the type of encounter must be in this list
             * providers - the provider of this encounter must be in this list
             * visitTypes - the visit types of this encounter must be in this list
             * visits - the visits of this encounter must be in this list
             * includeVoided - true/false to include the voided encounters or not
             */
            encountersByPatient = filterEncountersByModificationTime(startMillisecondsInclusive,
                    encounterService.getEncountersByPatient(patient));
        }
        List<SimpleObject> encounters = new ArrayList<>();
        for (Encounter encounter : filterBeforeSnapshotTime(snapshotTime, encountersByPatient)) {
            encounters.add(encounterToJson(encounter));
        }
        json.put(ENCOUNTERS, encounters);
    }

    private List<Encounter> filterEncountersByModificationTime(Long startMillisecondsInclusive,
                                                               List<Encounter> encountersByPatient) {
        ArrayList<Encounter> filtered = new ArrayList<>();
        for (Encounter encounter : encountersByPatient) {
            if (encounter.getDateCreated().getTime() >= startMillisecondsInclusive ||
                    (encounter.getDateChanged() != null &&
                            encounter.getDateChanged().getTime() >= startMillisecondsInclusive)) {
                filtered.add(encounter);
            }
        }
        return filtered;
    }

    private List<Encounter> filterBeforeSnapshotTime(long snapshotTime,
                                                     List<Encounter> encountersByPatient) {
        ArrayList<Encounter> filtered = new ArrayList<>();
        for (Encounter encounter : encountersByPatient) {
            if (encounter.getDateCreated().getTime() < snapshotTime) {
                filtered.add(encounter);
            }
        }
        return filtered;
    }

    private SimpleObject encounterToJson(Encounter encounter) {
        SimpleObject encounterJson = new SimpleObject();
        // TODO: Check what format this ends up in.
        encounterJson.put(TIMESTAMP, DateTimeUtils.toIso8601(encounter.getEncounterDatetime()));
        SimpleObject observations = new SimpleObject();
        for (Obs obs : encounter.getObs()) {
            encounterJson.put(UUID, encounter.getUuid());
            Concept concept = obs.getConcept();
            String value = VisitObsValue.visit(obs, new VisitObsValue.ObsValueVisitor<String>() {
                @Override
                public String visitCoded(Concept value) {
                    return value.getUuid();
                }

                @Override
                public String visitNumeric(Double value) {
                    return String.valueOf(value);
                }

                @Override
                public String visitBoolean(Boolean value) {
                    return String.valueOf(value);
                }

                @Override
                public String visitText(String value) {
                    return value;
                }

                @Override
                public String visitDate(Date value) {
                    return DateTimeUtils.YYYYMMDD_FORMAT.format(value);
                }

                @Override
                public String visitDateTime(Date value) {
                    return DateTimeUtils.toIso8601(value);
                }
            });
            observations.put(concept.getUuid(), value);
        }
        encounterJson.put(OBSERVATIONS, observations);
        return encounterJson;
    }

    /**
     * Create a new encounter for a patient. JSON syntax is:
     * {
     *     'uuid': 'patient-uuid-xxxx',
     *     'timestamp': seconds_since_epoch,
     *     'observations' : [
     *       {
     *         'question_uuid': 'xxxx-...',
     *         'answer_date': '2013-01-30', OR
     *         'answer_number': 40, OR
     *         'answer_uuid': 'xxxx-....'
     *       }, ...
     *     ]
     * }
     */
    @Override
    public Object create(SimpleObject obj, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "create", obj);
            Object result = createInner(obj, context);
            logger.reply(context, this, "create", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "create", e);
            throw e;
        }
    }

    public Object createInner(SimpleObject post, RequestContext context) throws ResponseException {
        // Warning! In order to re-use the observation creation code from PatientResource, the
        // JSON Syntax for this method (create) is different from the JSON syntax for get. This is
        // terrible REST design. However, we are close to shipping, and I don't want to introduce the
        // server and client side changes needed if I changed the wire format. So instead, there is this comment.
        // TODO: refactor the wire format for getEncounters so it matches the create format.

        if (!post.containsKey(UUID)) {
            throw new InvalidObjectDataException("No patient UUID specified");
        }
        Patient patient = patientService.getPatientByUuid(post.get(UUID).toString());
        if (patient == null) {
            throw new InvalidObjectDataException("No such patient: " + post.get(UUID));
        }
        Date encounterTime;
        try {
            if (post.containsKey(TIMESTAMP)) {
                encounterTime = new Date(Long.parseLong(post.get(TIMESTAMP).toString()) * 1000L);
            } else {
                // if no timestamp, use the server current time, to allow the client this as an option
                encounterTime = new Date();
            }
        } catch (NumberFormatException ex) {
            throw new InvalidObjectDataException("Bad TIMESTAMP format, should be seconds since epoch: "
                    + ex.getMessage());
        }
        if (observationsHandler.hasObservations(post)) {
            Encounter encounter = observationsHandler.addObservations(post, patient, encounterTime, "new observation",
                    // TODO: send the correct location in the RPC, rather than using the whole facility
                    "ADULTRETURN", LocationResource.EMC_UUID);
            return encounterToJson(encounter);
        } else {
            throw new InvalidObjectDataException("No observations specified");
        }
    }
}
