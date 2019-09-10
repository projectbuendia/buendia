package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.OpenmrsObject;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.resource.api.Deletable;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.resource.api.Updatable;
import org.openmrs.module.webservices.rest.web.response.InvalidSearchException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.Bookmark;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

// The default OpenMRS behaviour when attempting to perform an unsupported
// operation on a resource is terrible (it simply fails to cast the resource
// to the desired interface type) so we claim to support all operations and
// generate our own, better error messages.

public abstract class BaseResource<T extends OpenmrsObject>
    implements Listable, Searchable, Creatable, Retrievable, Updatable, Deletable {
    private static final RequestLogger logger = RequestLogger.LOGGER;
    private final List<Representation> availableRepresentations;
    
    protected final String pluralCollectionName;
    protected final ProjectBuendiaService buendiaService;
    protected final ConceptService conceptService;
    protected final EncounterService encounterService;
    protected final FormService formService;
    protected final LocationService locationService;
    protected final ObsService obsService;
    protected final OrderService orderService;
    protected final PatientService patientService;
    protected final ProviderService providerService;

    protected BaseResource(String pluralCollectionName, Representation... representations) {
        this.pluralCollectionName = pluralCollectionName;
        availableRepresentations = Arrays.asList(representations);
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

    /** Returns the representations acceptable as values for the ?v= parameter. */
    public List<Representation> getAvailableRepresentations() {
        return availableRepresentations;
    }

    public String getUri(Object object) {
        // This will return an incorrect URI if the webservices.rest.uriPrefix
        // property is incorrect.  We don't use getUri() at all in our resource
        // classes, but the Resource interface requires that we implement it.
        OpenmrsObject item = (OpenmrsObject) object;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + item.getUuid();
    }

    /** Returns all items in the collection. */
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        Utils.addVersionHeaders(context);
        try {
            logger.request(context, this, "getAll");
            List<SimpleObject> results = new ArrayList<>();
            for (T item : listItems(context)) {
                results.add(toJson(item, context));
            }
            return logger.reply(context, this, "getAll",
                new SimpleObject().add("results", results));
        } catch (Exception e) {
            logger.error(context, this, "getAll", e);
            throw e;
        }
    }

    /**
     * If there is a bookmark query parameter, fetches items after the bookmark;
     * otherwise searches for items matching the criteria in context.
     */
    public SimpleObject search(RequestContext context) throws ResponseException {
        Utils.addVersionHeaders(context);
        Bookmark bookmark = getBookmark(context);
        String op = bookmark != null ? "sync" : "search";
        logger.request(context, this, op);
        try {
            SimpleObject reply = new SimpleObject();
            List<T> items = new ArrayList<>();
            if (bookmark != null) {
                reply = syncItems(bookmark, items);
            } else {
                items.addAll(searchItems(context));
            }
            List<SimpleObject> results = new ArrayList<>();
            for (T item : items) {
                results.add(toJson(item, context));
            }
            reply.add("results", results);
            logger.reply(context, this, op, abbreviateReply(reply));
            return reply;
        } catch (Exception e) {
            logger.error(context, this, op, e);
            throw e;
        }

    }

    /** Creates a new item from the posted data. */
    public Object create(SimpleObject data, RequestContext context) throws ResponseException {
        Utils.addVersionHeaders(context);
        try {
            logger.request(context, this, "create", data);
            Utils.requirePropertyAbsent(data, "uuid");
            T item = createItem(data, context);
            return logger.reply(context, this, "create", toJson(item, context));
        } catch (Exception e) {
            logger.error(context, this, "create", e);
            throw e;
        }
    }

    /** Retrieves the item with the given UUID. */
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        Utils.addVersionHeaders(context);
        try {
            logger.request(context, this, "retrieve");
            T item = retrieveRequiredItem(uuid);
            return logger.reply(context, this, "retrieve", toJson(item, context));
        } catch (Exception e) {
            logger.error(context, this, "retrieve", e);
            throw e;
        }
    }

    /** Updates the item with the given UUID. */
    public Object update(String uuid, SimpleObject data, RequestContext context) throws ResponseException {
        Utils.addVersionHeaders(context);
        try {
            logger.request(context, this, "update", data);
            T item = retrieveRequiredItem(uuid);
            T newItem = updateItem(item, data, context);
            return logger.reply(context, this, "update", toJson(newItem, context));
        } catch (Exception e) {
            logger.error(context, this, "update", e);
            throw e;
        }
    }

    /** Deletes the item with the given UUID. */
    public void delete(String uuid, String reason, RequestContext context) throws ResponseException {
        Utils.addVersionHeaders(context);
        try {
            logger.request(context, this, "delete", reason);
            T item = retrieveRequiredItem(uuid);
            deleteItem(item, reason, context);
            logger.reply(context, this, "delete", null);
        } catch (Exception e) {
            logger.error(context, this, "delete", e);
            throw e;
        }
    }

    /** Retrieves a list of all items. */
    protected Collection<T> listItems(RequestContext context) {
        throw new UnsupportedOperationException(String.format(
            "Listing all %s is not implemented", pluralCollectionName));
    }

    /** Searches for all items matching the criteria in the RequestContext. */
    protected Collection<T> searchItems(RequestContext context) {
        throw new UnsupportedOperationException(String.format(
            "Searching for %s is not implemented", pluralCollectionName));
    }

    /** Fetches a chunk of items newer than a bookmarked position, returning an updated bookmark. */
    protected SimpleObject syncItems(Bookmark bookmark, List<T> items) {
        throw new UnsupportedOperationException(String.format(
            "Searching for %s is not implemented", pluralCollectionName));
    }

    /** Creates an item from the given data and returns it. */
    protected T createItem(SimpleObject data, RequestContext context) {
        throw new UnsupportedOperationException(String.format(
            "Creating %s is not implemented", pluralCollectionName));
    }

    /** Retrieves a single item by UUID, returning null if it can't be found. */
    protected T retrieveItem(String uuid) {
        throw new UnsupportedOperationException(String.format(
            "Retrieving individual %s is not implemented", pluralCollectionName));
    }

    /** Updates the given item using the given data. */
    protected T updateItem(T item, SimpleObject data, RequestContext context) {
        throw new UnsupportedOperationException(String.format(
            "Updating %s is not implemented", pluralCollectionName));
    }

    /** Deletes (voids or retires) the given item. */
    protected void deleteItem(T item, String reason, RequestContext context) {
        throw new UnsupportedOperationException(String.format(
            "Deleting %s is not implemented", pluralCollectionName));
    }

    /** Converts a single item to JSON (or returns an empty JSON object for null). */
    protected SimpleObject toJson(T item, RequestContext context) {
        SimpleObject json = new SimpleObject();
        if (item != null) {
            json.put("uuid", item.getUuid());
            if (DbUtils.isVoidedOrRetired(item)) return json.add("voided", true);
            populateJson(json, item, context);
        }
        return json;
    }

    /** Populates the given JSON object with data from the given item. */
    protected abstract void populateJson(SimpleObject json, T item, RequestContext context);

    private T retrieveRequiredItem(String uuid) {
        T item = retrieveItem(uuid);
        if (item == null || DbUtils.isVoidedOrRetired(item)) {
            throw new ItemNotFoundException(pluralCollectionName, uuid);
        }
        return item;
    }

    private SimpleObject abbreviateReply(SimpleObject reply) {
        final int MAX_ITEMS = 10;
        List<SimpleObject> items = (List<SimpleObject>) reply.get("results");
        List<Object> abbrevItems = new ArrayList<>();
        Object resultsValue = items;

        if (items.size() > MAX_ITEMS) {
            int omitted = items.size() - MAX_ITEMS;
            abbrevItems.add(String.format(
                "...only logging last %d of %d items (%d omitted)...",
                MAX_ITEMS, items.size(), omitted
            ));
            abbrevItems.addAll(items.subList(omitted, items.size()));
            resultsValue = abbrevItems;
        }

        return new SimpleObject()
            .add("results", resultsValue)
            .add("bookmark", reply.get("bookmark"))
            .add("more", reply.get("more"));
    }

    private static Bookmark getBookmark(RequestContext context) {
        String since = context.getParameter("since");
        if (since == null) return null;
        try {
            return Bookmark.deserialize(since);
        } catch (Exception e) {
            throw new InvalidSearchException("Invalid bookmark \"" + since + "\"");
        }
    }
}
