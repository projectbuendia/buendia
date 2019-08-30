package org.openmrs.projectbuendia.webservices.rest;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;

import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.SyncPage;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.openmrs.projectbuendia.Utils.eq;
import static org.openmrs.projectbuendia.Utils.formatUtcDate;
import static org.openmrs.projectbuendia.Utils.parseLocalDate;

@Resource(
    name = RestController.PATH + "/patients",
    supportedClass = Patient.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class PatientResource extends BaseResource<Patient> {
    private static final int MAX_PATIENTS_PER_PAGE = 100;
    private final PatientIdentifierType IDENTIFIER_TYPE_MSF;
    private final PatientIdentifierType IDENTIFIER_TYPE_LOCAL;

    public PatientResource() {
        super("patients", Representation.DEFAULT);
        IDENTIFIER_TYPE_MSF = DbUtils.getIdentifierTypeMsf();
        IDENTIFIER_TYPE_LOCAL = DbUtils.getIdentifierTypeLocal();
    }

    @Override protected Collection<Patient> listItems(RequestContext context) {
        return patientService.getAllPatients(false /* includeRetired */);
    }

    @Override protected Collection<Patient> searchItems(RequestContext context) {
        String id = context.getParameter("id");
        List<Patient> results = new ArrayList<>();
        PatientIdentifier key = fromClientIdent(context.getParameter("id"));
        for (Patient patient : patientService.getPatients(null /* name */,
            key.getIdentifier(), Arrays.asList(key.getIdentifierType()), true /* exact match */)) {
            if (!patient.isPersonVoided()) {
                results.add(patient);
            }
        }
        return results;
    }

    @Override protected SimpleObject syncItems(String tokenJson, List<Patient> items) {
        SyncToken token;
        try {
            token = SyncTokenUtils.jsonToSyncToken(tokenJson);
        } catch (ParseException | JsonParseException | JsonMappingException e) {
            throw new IllegalPropertyException(String.format(
                "Invalid sync token \"%s\"", tokenJson));
        }
        SyncPage<Patient> patients = buendiaService.getPatientsModifiedAtOrAfter(
            token, true /* include voided */, MAX_PATIENTS_PER_PAGE);
        items.addAll(patients.results);
        SyncToken newToken = SyncTokenUtils.clampSyncTokenToBufferedRequestTime(
            patients.syncToken, new Date());
        // If we fetched a full page, there's probably more data available.
        boolean more = patients.results.size() == MAX_PATIENTS_PER_PAGE;
        return new SimpleObject()
            .add("syncToken", SyncTokenUtils.syncTokenToJson(newToken))
            .add("more", more);
    }

    @Override protected synchronized Patient createItem(SimpleObject data, RequestContext context) {
        User user = DbUtils.getAuthenticatedUser();

        // Patient
        Patient patient = new Patient();
        patient.setCreator(user);
        // OpenMRS calls it "gender"; we use it for physical sex (as other implementations do).
        patient.setGender(normalizeSex(Utils.getOptionalString(data, "sex")));
        if (data.get("birthdate") != null) {
            patient.setBirthdate(parseLocalDate((String) data.get("birthdate")));
        }

        // PersonName
        PersonName name = new PersonName();
        name.setCreator(user);
        name.setGivenName(normalizeName(Utils.getOptionalString(data, "given_name")));
        name.setFamilyName(normalizeName(Utils.getOptionalString(data, "family_name")));

        patient.addName(name);

        // PatientIdentifier
        PatientIdentifier ident = new PatientIdentifier();
        ident.setCreator(user);
        ident.setLocation(DbUtils.getDefaultRoot());
        ident.setPreferred(true);
        // OpenMRS requires that every patient have a preferred identifier.  If the
        // incoming "id" field is non-blank, it becomes the MSF identifier; otherwise,
        // we use our database to generate a numeric locally unique identifier.
        String id = Utils.getOptionalString(data, "id");
        if (id != null) {
            requireValidUniqueMsfIdentifier(id);
            ident.setIdentifierType(DbUtils.getIdentifierTypeMsf());
            ident.setIdentifier(id);
        } else {
            ident.setIdentifierType(DbUtils.getIdentifierTypeLocal());
            // To generate an integer ID, we need to save the patient identifier and
            // let the table fill in the ID AUTO_INCREMENT column.  But OpenMRS will
            // not let us save the patient identifier with a blank identifier string,
            // so let's generate a temporary unique identifier just for this purpose.
            // We also can't save the patient identifier until after the patient is
            // saved, so, after that's done, we'll fix up the identifier to use the
            // generated number in the ID column.
            ident.setIdentifier("temp-" + new Date().getTime());
        }
        patient.addIdentifier(ident);

        // TODO(ping): Replace with an observation.
        // Set assigned location last, as doing so saves the patient, which could fail
        // if performed in the middle of patient creation.
        Map location = (Map) data.get("assigned_location");
        if (location != null) {
            setLocation(patient, (String) location.get("uuid"));
        }
        patientService.savePatient(patient);

        // For LOCAL-type identifiers, we can only determine the identifier after
        // the Patient and PatientIdentifier objects have been saved.  At this
        // point the PatientIdentifier has a freshly-generated ID column, which
        // we use to construct the string identifier.
        if (eq(ident.getIdentifierType(), DbUtils.getIdentifierTypeLocal())) {
            ident.setIdentifier("" + ident.getId());
            patientService.savePatientIdentifier(ident);
        }

        // Store any initial observations that are included with the new patient.
        String entererUuid = (String) data.get("enterer_uuid");
        ObservationUtils.addEncounter(
            (List) data.get("observations"), null,
            patient, patient.getDateCreated(), "ADULTINITIAL", entererUuid, null);
        return patient;

    }

    @Override protected Patient retrieveItem(String uuid) {
        return patientService.getPatientByUuid(uuid);
    }

    @Override protected synchronized Patient updateItem(Patient patient, SimpleObject data, RequestContext context) {
        User user = DbUtils.getAuthenticatedUser();
        PersonName name = patient.getPersonName();

        String sex = Utils.getOptionalString(data, "sex");
        if (sex != null) patient.setGender(normalizeSex(sex));
        String birthdate = Utils.getOptionalString(data, "birthdate");
        if (birthdate != null) patient.setBirthdate(parseLocalDate(birthdate));
        String givenName = Utils.getOptionalString(data, "given_name");
        if (givenName != null) name.setGivenName(normalizeName(givenName));
        String familyName = Utils.getOptionalString(data, "family_name");
        if (familyName != null) name.setFamilyName(normalizeName(familyName));
        Map location = (Map) data.get("assigned_location");
        if (location != null) {
            setLocation(patient, (String) location.get("uuid"));
        }
        String id = Utils.getOptionalString(data, "id");
        PatientIdentifier ident = patient.getPatientIdentifier();
        if (id != null && !eq(id, toClientIdent(ident))) {
            requireValidUniqueMsfIdentifier(id);
            if (eq(ident.getIdentifierType(), DbUtils.getIdentifierTypeMsf())) {
                ident.setIdentifier(id);
            } else {
                ident.setPreferred(false);
                PatientIdentifier newIdent = new PatientIdentifier(
                    id, IDENTIFIER_TYPE_MSF, DbUtils.getDefaultRoot());
                newIdent.setCreator(user);
                newIdent.setPreferred(true);
                patient.addIdentifier(newIdent);
            }
            patientService.savePatientIdentifier(ident);
        }
        patient.setChangedBy(user);
        return patientService.savePatient(patient);
    }

    @Override protected void deleteItem(Patient patient, String reason, RequestContext context) {
        patient.setPersonVoided(true);
        patient.setPersonVoidedBy(DbUtils.getAuthenticatedUser());
        patient.setPersonVoidReason(reason);
        patientService.savePatient(patient);
    }

    @Override protected void populateJson(SimpleObject json, Patient patient, RequestContext context) {
        json.add("id", toClientIdent(patient.getPatientIdentifier()));
        json.add("sex", patient.getGender());
        if (patient.getBirthdate() != null) {
            json.add("birthdate", formatUtcDate(patient.getBirthdate()));
        }
        json.add("given_name", denormalizeName(patient.getGivenName()));
        json.add("family_name", denormalizeName(patient.getFamilyName()));
        String locationId = DbUtils.getPersonAttributeValue(
            patient, DbUtils.getAssignedLocationAttributeType());
        if (locationId != null) {
            Location location = locationService.getLocation(Integer.parseInt(locationId));
            if (location != null) {
                json.add("assigned_location", new SimpleObject().add("uuid", location.getUuid()));
            }
        }
    }

    /** Normalizes a name to something OpenMRS will accept. */
    private static String normalizeName(String name) {
        name = (name == null) ? "" : name.trim();
        // OpenMRS refuses to store empty names, so we use "." to represent a missing name.
        return name.isEmpty() ? "." : name;
    }

    private static String denormalizeName(String normalized) {
        return eq(normalized, ".") ? "" : normalized;
    }

    /** Reconstructs a full name from its normalized parts. */
    private String getFullName(Patient patient) {
        return (denormalizeName(patient.getGivenName()) + " "
            + denormalizeName(patient.getFamilyName())).trim();
    }

    private static String normalizeSex(String sex) {
        if (sex != null && sex.trim().toUpperCase().matches("^[FMOU]")) {
            return sex.trim().toUpperCase().substring(0, 1);  // F, M, O, and U are valid
        } else {
            return "U";
        }
    }

    private void setLocation(Patient patient, String locationUuid) {
        // Apply the given assigned location to a patient, if locationUuid is not null.
        if (locationUuid != null) {
            Location location = locationService.getLocationByUuid(locationUuid);
            if (location != null) {
                DbUtils.setPersonAttributeValue(patient,
                    DbUtils.getAssignedLocationAttributeType(),
                    Integer.toString(location.getId()));
            }
        }
    }

    private PatientIdentifier fromClientIdent(String clientIdent) {
        if (clientIdent.startsWith("*")) {
            return new PatientIdentifier(clientIdent.substring(1), IDENTIFIER_TYPE_LOCAL, DbUtils.getDefaultRoot());
        } else {
            return new PatientIdentifier(clientIdent, IDENTIFIER_TYPE_MSF, DbUtils.getDefaultRoot());
        }
    }

    private String toClientIdent(PatientIdentifier ident) {
        if (ident == null) return null;
        // The client-side representation of an identifier is either:
        // "*" followed by an integer, where the integer is a local
        // (type "LOCAL") server-generated identifier; or otherwise
        // it is an MSF (type "MSF") client-provided identifier.
        if (eq(ident.getIdentifierType(), IDENTIFIER_TYPE_LOCAL)) {
            return "*" + ident.getIdentifier();
        } else {
            return ident.getIdentifier();
        }
    }

    /** Verifies that a string is acceptable as a unique, well-formed identifier. */
    private void requireValidUniqueMsfIdentifier(String ident) {
        // To prevent collision between identifier types, we don't permit the
        // client to try to create an MSF identifier that starts with "*".
        if (ident.startsWith("*")) {
            throw new InvalidObjectDataException(String.format(
                "\"%s\" is not a valid ID; the \"*\" prefix is reserved for server-generated IDs",
                ident
            ));
        }
        List<Patient> existing = patientService.getPatients(null, ident,
            Collections.singletonList(IDENTIFIER_TYPE_MSF), true /* exact match */);
        if (!existing.isEmpty()) {
            String name = getFullName(existing.get(0));
            throw new InvalidObjectDataException(String.format(
                "Another patient (%s) already has the ID \"%s\"",
                name.isEmpty() ? "with no name" : "named " + name, ident
            ));
        }
    }
}
