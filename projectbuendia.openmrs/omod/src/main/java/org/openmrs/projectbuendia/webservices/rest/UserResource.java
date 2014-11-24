package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.*;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.*;

/**
 * Resource for users (note that users are stored as Providers, Persons, and Users, but only
 * Providers will be returned by List calls).
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/user", supportedClass = Provider.class, supportedOpenmrsVersions = "1.10.*")
public class UserResource implements Listable, Retrievable, Creatable {
    // JSON property names
    private static final String USER_ID = "user_id";
    private static final String FAMILY_NAME = "family_name";
    private static final String GIVEN_NAME = "given_name";
    private static final String PASSWORD = "password";

    private static final String[] REQUIRED_FIELDS = {GIVEN_NAME, PASSWORD};

    private static Log log = LogFactory.getLog(UserResource.class);

    private final PersonService personService;
    private final ProviderService providerService;
    private final UserService userService;

    public UserResource() {
        personService = Context.getPersonService();
        providerService = Context.getProviderService();
        userService = Context.getUserService();
    }

    @Override
    public SimpleObject getAll(RequestContext requestContext) throws ResponseException {
        List<Provider> providers = providerService.getAllProviders();
        return getSimpleObjectWithResults(providers);
    }

    @Override
    public Object create(SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
        checkRequiredFields(simpleObject, REQUIRED_FIELDS);

        // TODO(akalachman): Localize full name construction?
        String fullName = (String)simpleObject.get(GIVEN_NAME);
        if (simpleObject.containsKey(FAMILY_NAME)) {
            fullName += " " + (String)simpleObject.get(FAMILY_NAME);
        }

        Person person = new Person();
        PersonName personName = new PersonName();
        personName.setGivenName((String)simpleObject.get(GIVEN_NAME));
        if (simpleObject.containsKey(FAMILY_NAME)) {
            personName.setFamilyName((String)simpleObject.get(FAMILY_NAME));
        }
        person.addName(personName);
        personService.savePerson(person);

        User user = new User();
        user.setPerson(person);
        user.setName(fullName);
        user.setUsername(fullName);
        userService.saveUser(user, (String)simpleObject.get(PASSWORD));

        Provider provider = new Provider();
        provider.setPerson(person);
        provider.setName(fullName);
        providerService.saveProvider(provider);

        log.info("Created user " + fullName);

        return providerToJson(provider);
    }

    @Override
    public String getUri(Object instance) {
        Patient patient = (Patient) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + patient.getUuid();
    }

    @Override
    public Object retrieve(String uuid, RequestContext requestContext) throws ResponseException {
        Provider provider = providerService.getProviderByUuid(uuid);
        if (provider == null) {
            throw new ObjectNotFoundException();
        }
        return providerToJson(provider);
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }

    // Throws an exception if the given SimpleObject is missing any required fields.
    private void checkRequiredFields(SimpleObject simpleObject, String[] requiredFields) {
        List<String> missingFields = new ArrayList<String>();
        for (String requiredField : requiredFields) {
            if (!simpleObject.containsKey(requiredField)) {
                missingFields.add(requiredField);
            }
        }

        if (!missingFields.isEmpty()) {
            throw new InvalidObjectDataException(
                    "JSON object lacks required fields: " + StringUtils.join(missingFields, ","));
        }
    }

    private SimpleObject getSimpleObjectWithResults(List<Provider> providers) {
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Provider provider : providers) {
            jsonResults.add(providerToJson(provider));
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }

    private SimpleObject providerToJson(Provider provider) {
        SimpleObject jsonForm = new SimpleObject();
        if (provider != null) {
            jsonForm.add(USER_ID, provider.getUuid());

            Person person = provider.getPerson();
            if (person != null) {
                jsonForm.add(GIVEN_NAME, person.getGivenName());
                jsonForm.add(FAMILY_NAME, person.getFamilyName());
            }
        }
        return jsonForm;
    }
}
