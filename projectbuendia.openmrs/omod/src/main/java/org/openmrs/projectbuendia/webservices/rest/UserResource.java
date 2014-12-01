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
 *
 * Expected behavior:
 * GET /user returns "full_name" and "user_id" fields,
 *     as well as "given_name" and "family_name" if present.
 * GET /user/[UUID] returns information on a single user
 * GET /user?q=[QUERY] performs a substring search on user full names
 * POST /user creates a new user. This requires a "user_name", "given_name", and "password",
 *      as well as an optional "family_name". Passwords must be >8 characters, contain at
 *      least one number, and contain at least one uppercase character.
 *
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/user", supportedClass = Provider.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class UserResource implements Listable, Searchable, Retrievable, Creatable {
    // JSON property names
    private static final String USER_ID = "user_id";
    private static final String USER_NAME = "user_name";
    private static final String FULL_NAME = "full_name";  // Ignored on create.
    private static final String FAMILY_NAME = "family_name";
    private static final String GIVEN_NAME = "given_name";
    private static final String PASSWORD = "password";

    // Sentinel for unknown values
    private static final String UNKNOWN = "(UNKNOWN)";

    // Defaults for guest account
    private static final String GUEST_FULL_NAME = "Guest User";
    private static final String GUEST_GIVEN_NAME = "Guest";
    private static final String GUEST_FAMILY_NAME = "User";
    private static final String GUEST_USER_NAME = "guest";
    private static final String GUEST_PASSWORD = "Password123";

    private static final String[] REQUIRED_FIELDS = {USER_NAME, GIVEN_NAME, PASSWORD};

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
        addGuestIfNotPresent(providers);
        return getSimpleObjectWithResults(providers);
    }

    private void addGuestIfNotPresent(List<Provider> providers) {
        boolean guestFound = false;
        for (Provider provider : providers) {
            if (provider.getName().equals(GUEST_FULL_NAME)) {
                guestFound = true;
                break;
            }
        }

        if (!guestFound) {
            SimpleObject guestDetails = new SimpleObject();
            guestDetails.put(GIVEN_NAME, GUEST_GIVEN_NAME);
            guestDetails.put(FAMILY_NAME, GUEST_FAMILY_NAME);
            guestDetails.put(USER_NAME, GUEST_USER_NAME);
            guestDetails.put(PASSWORD, GUEST_PASSWORD);
            providers.add(create(guestDetails));
        }
    }

    @Override
    public Object create(SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
        return providerToJson(create(simpleObject));
    }

    private Provider create(SimpleObject simpleObject) {
        checkRequiredFields(simpleObject, REQUIRED_FIELDS);

        // TODO(akalachman): Localize full name construction?
        String fullName = (String)simpleObject.get(GIVEN_NAME) + " " + (String)simpleObject.get(FAMILY_NAME);

        Person person = new Person();
        PersonName personName = new PersonName();
        personName.setGivenName((String)simpleObject.get(GIVEN_NAME));
        personName.setFamilyName((String)simpleObject.get(FAMILY_NAME));
        person.addName(personName);
        person.setGender(UNKNOWN);  // This is required, even though it serves no purpose here.
        personService.savePerson(person);

        User user = new User();
        user.setPerson(person);
        user.setName(fullName);
        user.setUsername((String)simpleObject.get(USER_NAME));
        userService.saveUser(user, (String)simpleObject.get(PASSWORD));

        Provider provider = new Provider();
        provider.setPerson(person);
        provider.setName(fullName);
        providerService.saveProvider(provider);

        log.info("Created user " + fullName);

        return provider;
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

    @Override
    public SimpleObject search(RequestContext requestContext) throws ResponseException {
        // Partial string query for searches.
        String query = requestContext.getParameter("q");

        // Retrieve all patients and filter the list based on the query.
        List<Provider> filteredProviders = new ArrayList<>();

        // Perform a substring search on username.
        for (Provider provider : providerService.getAllProviders()) {
            if (StringUtils.containsIgnoreCase(provider.getName(), query)) {
                filteredProviders.add(provider);
            }
        }

        addGuestIfNotPresent(filteredProviders);
        return getSimpleObjectWithResults(filteredProviders);
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
            jsonForm.add(FULL_NAME, provider.getName());

            Person person = provider.getPerson();
            if (person != null) {
                jsonForm.add(GIVEN_NAME, person.getGivenName());
                jsonForm.add(FAMILY_NAME, person.getFamilyName());
            }
        }
        return jsonForm;
    }
}
