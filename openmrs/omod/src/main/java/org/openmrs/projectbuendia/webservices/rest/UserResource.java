// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
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
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resource for users (note that users are stored as Providers, Persons, and Users, but only
 * Providers will be returned by List calls).
 *
 * <p>Expected behavior:
 * <ul>
 * <li>GET /user returns all users ({@link #getAll(RequestContext)})
 * <li>GET /user/[UUID] returns a single user ({@link #retrieve(String, RequestContext)})
 * <li>GET /user?q=[QUERY] returns users whose full name contains the query string
 *     ({@link #search(RequestContext)})
 * <li>POST /user creates a new user ({@link #create(SimpleObject, RequestContext)})
 * </ul>
 *
 * <p>All GET operations return User resources in the following JSON form:
 * <pre>
 * {
 *   user_id: "5a382-9", // UUID for the user
 *   full_name: "John Smith", // constructed from given and family name
 *   given_name: "John",
 *   family_name: "Smith"
 * }
 * </pre>
 *
 * <p>User creation expects a slightly different format:
 * <pre>
 * {
 *   user_name: "jsmith", // user id which can be used to log into OpenMRS
 *   password: "Password123", // must have &gt; 8 characters, at least 1 digit and 1 uppercase
 *   given_name: "John",
 *   family_name: "Smith"
 * }
 * </pre>
 *
 * <p>If an error occurs, the response will contain the following:
 * <pre>
 * {
 *   "error": {
 *     "message": "[error message]",
 *     "code": "[breakpoint]",
 *     "detail": "[stack trace]"
 *   }
 * }
 * </pre>
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
    private static final Object guestAddLock = new Object();

    private static Log log = LogFactory.getLog(UserResource.class);
    static final RequestLogger logger = RequestLogger.LOGGER;

    private final PersonService personService;
    private final ProviderService providerService;
    private final UserService userService;

    public UserResource() {
        personService = Context.getPersonService();
        providerService = Context.getProviderService();
        userService = Context.getUserService();
    }

    /** Returns all Providers. */
    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "getAll");
            SimpleObject result = getAllInner();
            logger.reply(context, this, "getAll", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "getAll", e);
            throw e;
        }
    }

    /** Returns all Providers. */
    private SimpleObject getAllInner() throws ResponseException {
        List<Provider> providers;
        // Returning providers is not a thread-safe operation as it may add the guest user
        // to the database, which is not idempotent.
        synchronized(this) {
            providers = providerService.getAllProviders(false); // omit retired
            addGuestIfNotPresent(providers);
        }
        return getSimpleObjectWithResults(providers);
    }

    /** Creates a Provider named "Guest User" if one doesn't already exist. */
    private void addGuestIfNotPresent(List<Provider> providers) {
        boolean guestFound = false;
        for (Provider provider : providers) {
            // TODO/robustness: Use a fixed UUID instead of searching for
            // anything with a matching name.
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
            synchronized (guestAddLock) {
                // Fetch again to avoid duplication in case another thread has
                // added Guest User, but use the UserService for the check to
                // avoid Hibernate cache issues.
                User guestUser = userService.getUserByUsername(GUEST_USER_NAME);
                if (guestUser == null) {
                    providers.add(createFromSimpleObject(guestDetails));
                }
            }
        }
    }

    /** Adds a new Provider. */
    @Override
    public Object create(SimpleObject obj, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "create", obj);
            Object result = createInner(obj);
            logger.reply(context, this, "create", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "create", e);
            throw e;
        }
    }

    private Object createInner(SimpleObject simpleObject) throws ResponseException {
        return providerToJson(createFromSimpleObject(simpleObject));
    }

    /** Adds a new Provider (with associated User and Person). */
    private Provider createFromSimpleObject(SimpleObject simpleObject) {
        checkRequiredFields(simpleObject, REQUIRED_FIELDS);

        // TODO: Localize full name construction
        String fullName = simpleObject.get(GIVEN_NAME) + " " + simpleObject.get(FAMILY_NAME);

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

    /** Returns a specific Provider. */
    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "retrieve", uuid);
            Object result = retrieveInner(uuid);
            logger.reply(context, this, "retrieve", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "retrieve", e);
            throw e;
        }
    }

    private Object retrieveInner(String uuid) throws ResponseException {
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

    /** Searches for Providers whose names contain the 'q' parameter. */
    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "search");
            SimpleObject result = searchInner(context);
            logger.reply(context, this, "search", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "search", e);
            throw e;
        }
    }

    /** Searches for Providers whose names contain the 'q' parameter. */
    private SimpleObject searchInner(RequestContext requestContext) throws ResponseException {
        // Partial string query for searches.
        String query = requestContext.getParameter("q");

        // Retrieve all patients and filter the list based on the query.
        List<Provider> filteredProviders = new ArrayList<>();

        // Perform a substring search on username.
        for (Provider provider : providerService.getAllProviders(false)) {
            if (StringUtils.containsIgnoreCase(provider.getName(), query)) {
                filteredProviders.add(provider);
            }
        }

        addGuestIfNotPresent(filteredProviders);
        return getSimpleObjectWithResults(filteredProviders);
    }

    /** Throws an exception if the given SimpleObject is missing any required fields. */
    private void checkRequiredFields(SimpleObject simpleObject, String[] requiredFields) {
        List<String> missingFields = new ArrayList<>();
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

    /**
     * Converts a list of Providers into a SimpleObject in the form
     * {"results": [...]} with an array of SimpleObjects, one for each Provider.
     */
    private SimpleObject getSimpleObjectWithResults(List<Provider> providers) {
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Provider provider : providers) {
            jsonResults.add(providerToJson(provider));
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }

    /** Builds a SimpleObject describing the given Provider. */
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
