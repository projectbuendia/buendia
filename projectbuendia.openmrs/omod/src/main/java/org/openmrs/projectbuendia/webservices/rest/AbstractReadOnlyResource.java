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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.openmrs.OpenmrsObject;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.Hyperlink;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.DateTimeUtils;

/**
 * Abstract superclass for resources whose REST API only supports read
 * operations. Subclasses must provide the @Resource annotation with a "name"
 * parameter specifying the URL path, "supportedClass" giving the class of
 * items in the REST collection, and "supportedOpenmrsVersions" giving a
 * comma-separated list of supported OpenMRS Platform version numbers.
 *
 * <p>Every AbstractReadOnlyResource provides the operations:
 * <ul>
 *     <li>Retrieve an item: {@link #retrieve(String, RequestContext)}
 *     <li>List all items: {@link #getAll(RequestContext)}
 *     <li>Search for items: {@link #search(RequestContext)}
 * </ul>
 * <p>Each of these methods returns a SimpleObject, which is converted to a
 * JSON response.  If an error occurs, the returned JSON will have the form:
 * <pre>
 * {
 *   "error": {
 *     "message": "error message]",
 *     "code": "[breakpoint]",
 *     "detail": "[stack trace]"
 *   }
 * }
 * </pre>
 * <p>For more details about each operation, see the method-level comments.
 */
public abstract class AbstractReadOnlyResource<T extends OpenmrsObject>
        implements Listable, Retrievable, Searchable {

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
     * Performs a search using the given {@link RequestContext}. The real work
     * is done by the {@link #searchImpl(RequestContext, long)} function, which
     * also defines the search parameters.  If no search parameters are given,
     * this returns all the items in the collection.
     *
     * @param context the request context; see individual implementations of
     *     {@link #searchImpl(RequestContext, long)} for parameter details.
     *     {@link #searchImpl(RequestContext, long)} for the search parameters.
     * @return a {@link SimpleObject} with the following keys:
     * <ul>
     *     <li>"results": a {@link List} of {@link SimpleObject}s representing
     *         items that match the search parameters, in the same form as that
     *         returned by {@link #retrieve(String, RequestContext)}
     *     <li>"snapshotTime": a timestamp in ISO 8601 format, in UTC
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

    /** Wraps searchImpl, converting items (of type T) to SimpleObjects. */
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
     * Retrieves the item with a specified UUID.
     *
     * <p>Responds to: {@code GET [API root]/[resource type]/[UUID]}
     *
     * @param uuid the UUID of the desired item
     * @param context the request context; see individual implementations of
     *     {@link #searchImpl(RequestContext, long)} for parameter details.
     * @return a {@link SimpleObject} with the following pairs:
     * <ul>
     * <li>uuid: the unique identifier of the resource
     * <li>links: a {@link List} of {@link SimpleObject}s each with the keys:
     *     <ul>
     *         <li>"uri": uri for requesting this resource
     *         <li>"rel": what the uri is relative to ("self" if relative to
     *             this server or absolute)
     *     </ul>
     * <li>Resource-specific data provided by {@link #populateJsonProperties(T, RequestContext, SimpleObject, long)}
     * </ul>
     * <p>TODO/deprecate: The information in "links" is incorrect (and would be
     * redundant if it were correct).  It doesn't appear to be used at all by
     * the client anyway, so it should just be removed.
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
     * Delegates to {@link #search(RequestContext)}, which returns all the items
     * in the collection if no search parameters are specified.
     */
    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        return search(context);
    }

    /**
     * Lists all the items matching the specified search parameters.  Note that
     * this method is also used for "get all", so it's entirely possible for the
     * context to not contain any query parameters.
     */
    protected abstract Iterable<T> searchImpl(RequestContext context, long snapshotTime);

    /** Retrieves a single item by UUID, returning null if it can't be found. */
    protected abstract T retrieveImpl(String uuid, RequestContext context, long snapshotTime);

    /**
     * Converts a single item to JSON. By default, this populates the UUID
     * and a self link automatically, then delegates to populateJsonProperties for the
     * remaining information. This is expected to be sufficient for most cases, but
     * subclasses can override this method if they want more flexibility.
     */
    protected SimpleObject convertToJson(T item, RequestContext context, long snapshotTime) {
        SimpleObject json = new SimpleObject();
        json.put(UUID, item.getUuid());
        // TODO(jonskeet): Version, date created etc?
        populateJsonProperties(item, context, json, snapshotTime);
        return json;
    }

    /** Populates the given SimpleObject with data from the given item. */
    protected abstract void populateJsonProperties(
            T item, RequestContext context, SimpleObject json, long snapshotTime);
}
