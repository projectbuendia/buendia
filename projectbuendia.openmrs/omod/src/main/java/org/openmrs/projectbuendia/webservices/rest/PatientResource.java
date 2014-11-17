package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.*;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Resource for xform templates (i.e. forms without data).
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patient", supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*")
public class PatientResource implements Listable, Searchable, Retrievable, Creatable {

    private static final String GENDER = "gender";
    private static final String GIVEN_NAME = "given_name";
    private static final String FAMILY_NAME = "family_name";
    private static final User CREATOR = new User(1);
    private static final String ID = "id";
    private static final String MSF_IDENTIFIER = "MSF";
    public static final Location LOCATION = new Location(1);
    private final PatientService patientService;


    public PatientResource() {
        patientService = Context.getPatientService();
    }

    @Override
    public SimpleObject getAll(RequestContext requestContext) throws ResponseException {
        List<Patient> patients = patientService.getAllPatients();
        return getSimpleObjectFromPatientList(patients);
    }

    private SimpleObject patientToJson(Patient patient) {
        SimpleObject jsonForm = new SimpleObject();
        jsonForm.add(ID, patient.getUuid() /*TODO(nfortescue): patient.getPatientIdentifier().getIdentifier()*/);
        jsonForm.add(GIVEN_NAME, patient.getGivenName());
        jsonForm.add(FAMILY_NAME, patient.getFamilyName());
        jsonForm.add("status", "probable" /* TODO(nfortescue): work out how to store this */);
        jsonForm.add(GENDER, patient.getGender());
        jsonForm.add("created_timestamp_utc", patient.getDateCreated().getTime());
        return jsonForm;
    }

    @Override
    public Object create(SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
        // We really want this to use XForms, but lets have a simple default implementation for early testing

        if (!simpleObject.containsKey(ID)) {
            throw new ConversionException("No id set in create request");
        }
        PatientIdentifierType identifierType = patientService.getPatientIdentifierTypeByName(MSF_IDENTIFIER);
        ArrayList<PatientIdentifierType> identifierTypes = new ArrayList<>();
        identifierTypes.add(identifierType);
        String id = (String)simpleObject.get(ID);
        List<Patient> existing =
                patientService.getPatients(null, id, identifierTypes, true /* exact identifier match */);
        if (!existing.isEmpty()) {
            throw new ConversionException("Creating an object that already exists " + id);
        }

        Patient patient = new Patient();
        // TODO(nfortescue): do this properly from authentication
        patient.setCreator(CREATOR);
        patient.setDateCreated(new Date());

        if (simpleObject.containsKey(GENDER)) {
            patient.setGender((String)simpleObject.get(GENDER));
        }

        PersonName pn = new PersonName();
        if (simpleObject.containsKey(GIVEN_NAME)) {
            pn.setGivenName((String)simpleObject.get(GIVEN_NAME));
        }
        if (simpleObject.containsKey(FAMILY_NAME)) {
            pn.setFamilyName((String) simpleObject.get(FAMILY_NAME));
        }

        pn.setCreator(patient.getCreator());
        pn.setDateCreated(patient.getDateCreated());
        patient.addName(pn);

        // Fake location
        PatientIdentifier identifier = new PatientIdentifier();
        identifier.setCreator(patient.getCreator());
        identifier.setDateCreated(patient.getDateCreated());
        identifier.setIdentifier(id);
        identifier.setLocation(LOCATION);

        identifier.setIdentifierType(identifierType);
        identifier.setPreferred(true);
        patient.addIdentifier(identifier);

        patientService.savePatient(patient);

        return simpleObject;
    }

    @Override
    public String getUri(Object instance) {
        Patient patient = (Patient) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + patient.getUuid();
    }

    @Override
    public SimpleObject search(RequestContext requestContext) throws ResponseException {
        // Partial string query for searches.
        String query = requestContext.getParameter("q");

        // If set, also search on uuid. By default uuid is skipped.
        boolean searchUuid = (requestContext.getParameter("searchUuid") != null);

        // Retrieve all patients and filter the list based on the query.
        List<Patient> filteredPatients = filterPatients(query, searchUuid, patientService.getAllPatients());

        return getSimpleObjectFromPatientList(filteredPatients);
    }

    @Override
    public Object retrieve(String uuid, RequestContext requestContext) throws ResponseException {
        Patient patient = patientService.getPatientByUuid(uuid);
        return patientToJson(patient);
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }

    private List<Patient> filterPatients(String query, boolean searchUuid, List<Patient> allPatients) {
        List<Patient> filteredPatients = new ArrayList<Patient>();

        // Filter patients by id, name, and MSF id. Don't use patientService.getPatients() for
        // this, as the behavior does not match the expected behavior from the API docs.
        PatientIdentifierType msfIdentifierType = patientService.getPatientIdentifierTypeByName(MSF_IDENTIFIER);
        for (Patient patient : allPatients) {
            boolean match = false;

            // First check the patient's full name.
            for (PersonName name : patient.getNames()) {
                if(StringUtils.containsIgnoreCase(name.getFullName(), query)) {
                    match = true;
                    break;
                }
            }

            // Short-circuit on name match.
            if (match) {
                filteredPatients.add(patient);
                continue;
            }

            // Next check the patient's MSF id.
            for (PatientIdentifier identifier : patient.getPatientIdentifiers(msfIdentifierType)) {
                if (StringUtils.containsIgnoreCase(identifier.getIdentifier(), query)) {
                    match = true;
                    break;
                }
            }

            if (match || (searchUuid && StringUtils.containsIgnoreCase(patient.getUuid(), query))) {
                filteredPatients.add(patient);
            }
        }

        return filteredPatients;
    }

    private SimpleObject getSimpleObjectFromPatientList(List<Patient> patients) {
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Patient patient : patients) {
            SimpleObject jsonForm = patientToJson(patient);
            jsonResults.add(jsonForm);
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }
}
