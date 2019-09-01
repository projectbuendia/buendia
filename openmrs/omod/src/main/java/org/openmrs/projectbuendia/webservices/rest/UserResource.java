package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.Collection;

import static org.openmrs.projectbuendia.Utils.getRequiredString;

@Resource(
    name = RestController.PATH + "/users",
    supportedClass = Provider.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class UserResource extends BaseResource<Provider> {
    private static final String PROVIDER_GUEST_UUID = "buendia_provider_guest";
    private static final String GUEST_NAME = "Guest User";

    public UserResource() {
        super("users", Representation.DEFAULT);
    }

    @Override protected Collection<Provider> listItems(RequestContext context) {
        ensureGuestProviderExists();
        ensurePlacementConceptExists();
        return providerService.getAllProviders(false /* include retired */);
    }

    @Override protected Provider createItem(SimpleObject data, RequestContext context) {
        String givenName = getRequiredString(data, "given_name");
        String familyName = getRequiredString(data, "family_name");
        String name = (givenName + " " + familyName).trim();
        if (name.isEmpty()) {
            throw new InvalidObjectDataException("Both name fields are empty");
        }

        Provider provider = new Provider();
        provider.setCreator(DbUtils.getAuthenticatedUser());
        provider.setName(name);
        return providerService.saveProvider(provider);
    }

    @Override protected Provider retrieveItem(String uuid) {
        return providerService.getProviderByUuid(uuid);
    }

    @Override protected void populateJson(SimpleObject json, Provider provider, RequestContext context) {
        json.add("user_id", provider.getUuid());
        json.add("full_name", provider.getName());
        Person person = provider.getPerson();
        if (person != null) {
            json.add("given_name", person.getGivenName());
            json.add("family_name", person.getFamilyName());
        }
    }

    /** Creates a Provider named "Guest User" if one doesn't already exist. */
    private synchronized void ensureGuestProviderExists() {
        Provider guest = providerService.getProviderByUuid(PROVIDER_GUEST_UUID);
        if (guest == null) {
            guest = new Provider();
            guest.setCreator(DbUtils.getAuthenticatedUser());
            guest.setUuid(PROVIDER_GUEST_UUID);
            guest.setName(GUEST_NAME);
            providerService.saveProvider(guest);
        }
    }

    private synchronized void ensurePlacementConceptExists() {
        DbUtils.getPlacementConcept();
    }
}
