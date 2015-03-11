package org.openmrs.projectbuendia.webservices.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.openmrs.OpenmrsObject;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.Hyperlink;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.DateTimeUtils;

/**
 * Abstract superclass for resources where the REST API only supports read-only
 * operations. Subclasses are required to provide the Resource annotation to specify the URL etc.
 *
 * <p>Every AbstractReadOnlyResource supports retrieve, list (getAll), and search operations.
 *
 * <p>Retrieve operations are implemented by {@link #retrieve(String, RequestContext)}.
 * List operations are implemented by {@link #getAll(RequestContext)}.
 * Search operations are implemented by {@link #search(RequestContext)}.
 *
 * <p>Each of these functions returns a SimpleObject, which is converted to a JSON representation in the response. If
 * an error occurs, this SimpleObject will contain the following:
 * <pre>
 * {
 *   "error": {
 *     "message": "[error message]",
 *     "code": "[breakpoint]",
 *     "detail": "[stack trace]"
 *   }
 * }
 * </pre>
 *
 * <p>For more details about each operation, including expected input/output and request parameters,
 * see method-level comments.
 */
public abstract class AbstractReadOnlyResource<T extends OpenmrsObject> implements Listable, Retrievable, Searchable {

    static final String RESULTS = "results";
    static final String SNAPSHOT_TIME = "snapshotTime";
    static final String UUID = "uuid";
    static final String LINKS = "links";
    static final String SELF = "self";
    static final RequestLogger logger = RequestLogger.LOGGER;

    private final String resourceAlias;
    private final List<Representation> availableRepresentations;

    protected AbstractReadOnlyResource(String resourceAlias, Representation... representations) {
        availableRepresentations = Arrays.asList(representations);
        this.resourceAlias = resourceAlias;
    }
    
    @Override
    public String getUri(Object instance) {
        OpenmrsObject mrsObject = (OpenmrsObject) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + mrsObject.getUuid();
    }

    /**
     * Performs a search, using the given {@link RequestContext}. What parameters are used for a
     * search, as well as how the search is performed, is delegated to the
     * {@link #searchImpl(RequestContext, long)} function.
     *
     * <p>If no search query is specified, this function returns all results.
     *
     * @param context the request context; see {@link #searchImpl(RequestContext, long)} to determine what parameters
     * are expected
     * @return a {@link SimpleObject} with the following pairs:
     * <ul>
     *     <li>results: a {@link List} of {@link SimpleObject}'s, each representing a single resource, equivalent to the
     *         output of a {@link #retrieve(String, RequestContext)} operation
     *     <li>snapshotTime: a timestamp in ISO 6801 format labeled snapshotTime
     * </ul>
     * @throws ResponseException if anything goes wrong
     */
    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "search");
            long snapshotTime = System.currentTimeMillis();
            SimpleObject result = searchInner(context, snapshotTime);
            logger.reply(context, this, "search", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "search", e);
            throw e;
        }
    }

    private SimpleObject searchInner(RequestContext context, long snapshotTime) throws ResponseException {
        List<SimpleObject> results = new ArrayList<>();
        for (T item : searchImpl(context, snapshotTime)) {
            results.add(convertToJson(item, context, snapshotTime));
        }
        SimpleObject response = new SimpleObject();
        response.put(RESULTS, results);
        response.put(SNAPSHOT_TIME, DateTimeUtils.toIso8601(new Date(snapshotTime)));
        return response;
    }

    /**
     * Retrieves the resource with the specified UUID.
     *
     * <p>Responds to: {@code GET [API root]/[resource type]/[UUID]}
     *
     * @param uuid the UUID that uniquely identifies the requested resource
     * @param context the request context; see {@link #retrieveImpl(String, RequestContext, long)} to determine what
     * parameters are expected
     * @return a {@link SimpleObject} with the following pairs:
     * <ul>
     * <li>
     *     uuid: the unique identifier of the resource
     * </li>
     * <li>
     *     links: a {@link List} of {@link SimpleObject}'s, each containing the following pairs:
     *     <ul>
     *         <li>uri: uri for requesting this resource
     *         <li>rel: what the uri is relative to ("self" if relative to this server or absolute)
     *     </ul>
     * <li>Resource-specific data provided by {@link #populateJsonProperties(T, RequestContext, SimpleObject, long)}
     * </ul>
     * @throws ResponseException if anything goes wrong
     */
    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "retrieve", uuid);
            Object result = retrieveInner(uuid, context, System.currentTimeMillis());
            logger.reply(context, this, "retrieve", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "retrieve", e);
            throw e;
        }
    }

    private Object retrieveInner(String uuid, RequestContext context, long snapshotTime) throws ResponseException {
        T item = retrieveImpl(uuid, context, snapshotTime);
        if (item == null) {
            throw new ObjectNotFoundException();
        }
        return convertToJson(item, context, snapshotTime);
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return availableRepresentations;
    }

    /**
     * Delegates directly to {@link #search(RequestContext)}; the implementation of searchImpl should list all
     * appropriate items if no query parameters have been given. 
     */
    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        return search(context);
    }

    /**
     * Find all the domain objects matching the query within the request context.
     * Note that this method is also used for "get all" so it's entirely possible
     * for the context to not contain any query parameters.
     */
    protected abstract Iterable<T> searchImpl(RequestContext context, long snapshotTime);
    
    /**
     * Retrieve a single domain object by UUID, returning null if it can't be found.
     */
    protected abstract T retrieveImpl(String uuid, RequestContext context, long snapshotTime);
    
    /**
     * Converts a single domain object to JSON. By default, this populates the UUID
     * and a self link automatically, then delegates to populateJsonProperties for the
     * remaining information. This is expected to be sufficient for most cases, but
     * subclasses can override this method if they want more flexibility.
     */
    protected SimpleObject convertToJson(T item, RequestContext context, long snapshotTime) {
        SimpleObject json = new SimpleObject();
        json.put(UUID, item.getUuid());
        json.put(LINKS, getLinks(item));
        // TODO(jonskeet): Version, date created etc?
        populateJsonProperties(item, context, json, snapshotTime);
        return json;
    }
    
    protected abstract void populateJsonProperties(T item, RequestContext context, SimpleObject json,
                                                   long snapshotTime);
    
    /**
     * Retrieves the links for the given item. The default implementation just adds a self link. 
     */
    protected List<Hyperlink> getLinks(T item) {
        Hyperlink self = new Hyperlink(SELF, getUri(item));
        self.setResourceAlias(resourceAlias);
        List<Hyperlink> links = new ArrayList<>();
        links.add(self);
        return links;
    }
}
