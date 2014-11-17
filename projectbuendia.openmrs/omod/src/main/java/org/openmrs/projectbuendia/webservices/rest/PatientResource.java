package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Resource for xform templates (i.e. forms without data).
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patient", supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*")
public class PatientResource implements Listable, Searchable {

    private final PatientService patientService;

    public PatientResource() {
        patientService = Context.getPatientService();
    }

    @Override
    public SimpleObject getAll(RequestContext requestContext) throws ResponseException {
        List<SimpleObject> jsonResults = new ArrayList<>();
        List<Patient> patients = patientService.getAllPatients();
        for (Patient patient : patients) {
            SimpleObject jsonForm = new SimpleObject();
            jsonForm.add("id", patient.getUuid() /*TODO(nfortescue): patient.getPatientIdentifier().getIdentifier()*/);
            jsonForm.add("given_name", patient.getGivenName());
            jsonForm.add("family_name", patient.getFamilyName());
            jsonForm.add("status", "probable" /* TODO(nfortescue): work out how to store this */);
            jsonForm.add("gender", patient.getGender());
            jsonResults.add(jsonForm);
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }

    @Override
    public String getUri(Object instance) {
        Patient patient = (Patient) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + patient.getUuid();
    }

    @Override
    public SimpleObject search(RequestContext requestContext) throws ResponseException {
        throw new UnsupportedOperationException();
    }
}
