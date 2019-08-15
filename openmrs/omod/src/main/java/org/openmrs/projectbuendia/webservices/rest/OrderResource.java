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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.EncounterService;
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
import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.SyncPage;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * Rest API for orders.
 * <p>Expected behavior:
 * <ul>
 * <li>GET /orders returns all orders
 * <li>GET /orders?since=[bookmark] returns all orders since the last server-provided bookmark.
 * <li>GET /orders/[UUID] returns a single order ({@link #retrieve(String, RequestContext)})
 * <li>POST /orders?patient=[UUID] creates an order for a patient ({@link #create(SimpleObject,
 * RequestContext)}
 * <li>POST /orders/[UUID] updates a order ({@link #update(String, SimpleObject, RequestContext)})
 * </ul>
 * <p/>
 * <p>Each operation handles Order resources in the following JSON form:
 * <p/>
 * <pre>
 * {
 *   "uuid": "e5e755d4-f646-45b6-b9bc-20410e97c87c", // assigned by OpenMRS, not required for
 *   creation
 *   "voided": false, // If true, fields other than UUID are not guaranteed to be set.
 *   "instructions": "Paracetamol 2 tablets 3x/day",
 *   "start_millis": 1438711253000,
 *   "stop_millis": 1438714253000  // optionally present
 * }
 * </pre>
 * (Results may also contain deprecated fields other than those described above.)
 * <p/>
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
@Resource(
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/orders",
    supportedClass = Order.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class OrderResource implements
        Listable, Searchable, Retrievable, Creatable, Updatable, Deletable {
    private static final RequestLogger logger = RequestLogger.LOGGER;
    private static final Log log = LogFactory.getLog(OrderResource.class);

    // JSON fields
    public static final String PATIENT_UUID = "patient_uuid";
    public static final String INSTRUCTIONS = "instructions";
    public static final String START_MILLIS = "start_millis";
    public static final String STOP_MILLIS = "stop_millis";
    public static final String UUID = "uuid";
    public static final String VOIDED = "voided";
    public static final String ORDERER_UUID = "orderer_uuid";

    private static final int MAX_ORDERS_PER_PAGE = 500;

    // Allow all order actions except discontinues, because the client doesn't represent those.
    private static final Order.Action[] ALLOWABLE_ACTIONS =
            ArrayUtils.removeElement(Order.Action.values(), Order.Action.DISCONTINUE);

    private final PatientService patientService;
    private final OrderService orderService;
    private final ProviderService providerService;
    private final EncounterService encounterService;
    private final ProjectBuendiaService buendiaService;

    public OrderResource() {
        patientService = Context.getPatientService();
        orderService = Context.getOrderService();
        providerService = Context.getProviderService();
        encounterService = Context.getEncounterService();
        buendiaService = Context.getService(ProjectBuendiaService.class);
    }

    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        return search(context);
    }

    private SimpleObject handleSync(RequestContext context) throws ResponseException {
        SyncToken syncToken = RequestUtil.mustParseSyncToken(context);
        boolean includeVoided = (syncToken != null);
        Date requestTime = new Date();

        SyncPage<Order> orders = buendiaService.getOrdersModifiedAtOrAfter(
                syncToken,
                includeVoided,
                MAX_ORDERS_PER_PAGE /* maxResults */,
                ALLOWABLE_ACTIONS);

        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Order order : orders.results) {
            jsonResults.add(orderToJson(order));
        }
        SyncToken newToken =
                SyncTokenUtils.clampSyncTokenToBufferedRequestTime(orders.syncToken, requestTime);
        // If we fetched a full page, there's probably more data available.
        boolean more = orders.results.size() == MAX_ORDERS_PER_PAGE;
        return ResponseUtil.createIncrementalSyncResults(jsonResults, newToken, more);
    }

    /** Serializes an order to JSON. */
    private static SimpleObject orderToJson(Order order) {
        // The UUID we send to the client is actually the UUID of the order at the head of the
        // revision chain...
        Order rootOrder = Utils.getRootOrder(order);
        // but the data we supply comes from the latest revision in the chain.
        order = getLatestVersion(rootOrder);

        SimpleObject json = new SimpleObject();
        if (order != null) {
            json.add(UUID, rootOrder.getUuid());
            json.add(VOIDED, order.isVoided());
            if (order.isVoided()) {
                return json;
            }
            json.add(PATIENT_UUID, order.getPatient().getUuid());
            String instructions = order.getInstructions();
            if (instructions != null) {
                json.add(INSTRUCTIONS, instructions);
            }
            Date start = order.getScheduledDate();
            if (start != null) {
                json.add(START_MILLIS, start.getTime());
            }
            Date stop = firstNonNull(order.getDateStopped(), order.getAutoExpireDate());
            json.add(STOP_MILLIS, stop == null ? null : stop.getTime());
        }
        return json;
    }

    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "handleSync");
            SimpleObject result = handleSync(context);
            logger.reply(context, this, "handleSync", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "handleSync", e);
            throw e;
        }
    }

    @Override
    public Object create(SimpleObject json, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "create", json);
            Object result = createInner(json);
            logger.reply(context, this, "create", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "create", e);
            throw e;
        }
    }

    private Object createInner(SimpleObject json) throws ResponseException {
        Order order = jsonToOrder(json);
        orderService.saveOrder(order, null);
        return orderToJson(order);
    }

    /** Creates a new Order and a corresponding Encounter containing it. */
    private Order jsonToOrder(SimpleObject json) {
        Order order = new Order();
        populateFromJson(order, json);
        populateDefaultsForAllOrders(order);
        populateDefaultsForNewOrder(order);
        return order;
    }

    /**
     * Finds the last order in the chain containing the given order. We need to do this instead of
     * using {@link OrderService#getRevisionOrder(Order)} because {@code getRevisionOrder(Order)}
     * only gets orders that have an action of {@link org.openmrs.Order.Action#REVISE}. To use
     * {@code REVISE}, the previous order needs to have not expired, which we can't guarantee.
     * </ul>
     */
    public static Order getLatestVersion(Order order) {
        // Construct a map of forward pointers using the backward pointers from getPreviousOrder().
        Map<String, String> nextOrderUuids = new HashMap<>();
        for (Order o : Context.getOrderService().getAllOrdersByPatient(order.getPatient())) {
            Order prev = o.getPreviousOrder();
            if (prev != null) {
                nextOrderUuids.put(prev.getUuid(), o.getUuid());
            }
        }

        // Walk forward until the end of the chain.
        String uuid = order.getUuid();
        String nextUuid = nextOrderUuids.get(uuid);
        while (nextUuid != null) {
            uuid = nextUuid;
            nextUuid = nextOrderUuids.get(uuid);
        }
        return Context.getOrderService().getOrderByUuid(uuid);
    }

    /**
     * Populates data for an order that was created with the Buendia API. This should be done for
     * both new orders and revisions.
     */
    private void populateDefaultsForAllOrders(Order order) {
        // There is no "changed_by" property; an update is achieved by creating
        // a new order that revises the previous one, so the authenticated user
        // goes in the "creator" property for both create and update operations.
        order.setCreator(Utils.getAuthenticatedUser());
        Provider orderer = order.getOrderer();
        // Populate with a default orderer if none is supplied.
        if (orderer == null) {
            order.setOrderer(getProvider());
        }
        // Will be null if `orderer` is null.
        order.setEncounter(createEncounter(order.getPatient(), new Date()));
    }

    /**
     * Populates data for a NEW order that was created with the Buendia API. This should not be used
     * for revisions, because it may overwrite data set by another source.
     */
    private void populateDefaultsForNewOrder(Order order) {
        order.setOrderType(DbUtil.getMiscOrderType());
        order.setCareSetting(orderService.getCareSettingByName("Outpatient"));
        order.setConcept(DbUtil.getFreeTextOrderConcept());
        order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
    }

    /**
     * Populates an {@link Order} from a JSON representation. Overwrites fields from the JSON where
     * this is valid (all fields except for {@link #PATIENT_UUID}).
     * @param order The order to update
     * @param json The JSON representation to draw updates from.
     * @return {@code true} if the Order was changed.
     */
    private boolean populateFromJson(Order order, SimpleObject json) {
        boolean changed = false;
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            final Object value = entry.getValue();
            switch (entry.getKey()) {
                case PATIENT_UUID: {
                    if (value == null) {
                        throw new IllegalPropertyException(PATIENT_UUID + " cannot be null");
                    }
                    if (!(value instanceof String)) {
                        throw new IllegalPropertyException(
                                "Illegal format for " + PATIENT_UUID + ", expected string");
                    }
                    String patientUuid = (String) value;
                    if (order.getPatient() != null) {
                        if (Objects.equals(order.getPatient().getUuid(), patientUuid)) {
                            // Patient hasn't changed, keep going
                            continue;
                        }
                        throw new IllegalPropertyException("Can't modify " + PATIENT_UUID);
                    }
                    Patient patient = patientService.getPatientByUuid(patientUuid);
                    if (patient == null) {
                        throw new IllegalPropertyException(
                                "Patient with UUID " + patientUuid + " does not exist.");
                    }
                    order.setPatient(patient);
                    changed = true;
                } break;

                case INSTRUCTIONS: {
                    if (!(value instanceof String)) {
                        throw new IllegalPropertyException(
                                "Illegal format for " + INSTRUCTIONS + ", expected string");
                    }
                    if (Objects.equals(order.getInstructions(), value)) {
                        // No change
                        continue;
                    }
                    order.setInstructions((String) value);
                    changed = true;
                } break;

                case START_MILLIS: {
                    Date dateVal = objectToDate(value, START_MILLIS);
                    if (Objects.equals(order.getScheduledDate(), dateVal)) {
                        // No change
                        continue;
                    }
                    order.setScheduledDate(dateVal);
                    changed = true;
                } break;

                case STOP_MILLIS: {
                    Date dateVal = objectToDate(value, STOP_MILLIS);
                    if (Objects.equals(order.getAutoExpireDate(), dateVal)) {
                        // No change
                        continue;
                    }
                    order.setAutoExpireDate(dateVal);
                    changed = true;
                } break;

                case ORDERER_UUID: {
                    if (!(value instanceof String)) {
                        throw new IllegalPropertyException(
                                "Illegal format for " + ORDERER_UUID + ", expected string");
                    }
                    order.setOrderer(providerService.getProviderByUuid((String) value));
                } break;

                default: {
                    log.warn(
                            "Key '" + entry.getKey() + "' is not the name of an editable property");
                } break;
            }
        }
        return changed;
    }

    private static Date objectToDate(Object value, String fieldName) {
        Long millis;
        try {
            millis = Utils.asLong(value);
        } catch (ClassCastException ex) {
            throw new IllegalPropertyException(
                    "Illegal format for " + fieldName + ", expected number");
        }
        return millis == null ? null : new Date(millis);
    }

    private Encounter createEncounter(Patient patient, Date encounterDatetime) {
        Encounter encounter = new Encounter();
        encounter.setCreator(Utils.getAuthenticatedUser());
        encounter.setEncounterDatetime(encounterDatetime);
        encounter.setPatient(patient);
        encounter.setLocation(Context.getLocationService().getDefaultLocation());
        encounter.setEncounterType(encounterService.getEncounterType("ADULTRETURN"));
        encounterService.saveEncounter(encounter);
        return encounter;
    }

    private Provider getProvider() {
        return providerService.getAllProviders(false).get(0); // omit retired
    }

    @Override
    public String getUri(Object instance) {
        Order order = (Order) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + order.getUuid();
    }

    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "retrieve");
            Object result = retrieveInner(uuid);
            logger.reply(context, this, "retrieve", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "retrieve", e);
            throw e;
        }
    }

    private Object retrieveInner(String uuid) throws ResponseException {
        Order order = orderService.getOrderByUuid(uuid);
        if (order == null) {
            throw new ObjectNotFoundException();
        }
        return orderToJson(order);
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Collections.singletonList(Representation.DEFAULT);
    }

    @Override
    public Object update(String uuid, SimpleObject simpleObject, RequestContext context) throws
        ResponseException {
        try {
            logger.request(context, this, "update", simpleObject);
            Object result = updateInner(uuid, simpleObject);
            logger.reply(context, this, "update", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "update", e);
            throw e;
        }
    }

    /**
     * Updates an Order from the JSON representation. This method has the following semantics:
     * <ul>
     * <li>Any field that is set overwrites the current content
     * <li>Any field with a key but value == null deletes the current content
     * <li>Any field whose key is not present leaves the current content unchanged
     * <li>If the client requests a change that is illegal, that is an error. The whole call will
     * fail in this case.
     * </ul>
     */
    private Object updateInner(String uuid, SimpleObject simpleObject) throws ResponseException {
        Order order = orderService.getOrderByUuid(uuid);
        if (order == null || order.isVoided()) {
            throw new ObjectNotFoundException();
        }

        // Skip ahead to the latest order in the chain.
        order = getLatestVersion(order);

        Order revisedOrder = reviseOrder(order, simpleObject);

        // Don't update anything, there's no changes.
        if (revisedOrder == null) {
            return orderToJson(order);
        }

        orderService.saveOrder(revisedOrder, null);
        return orderToJson(revisedOrder);
    }

    /** Revises an order.  Returns null if no changes were made. */
    private Order reviseOrder(Order order, SimpleObject edits) {
        Order newOrder = order.cloneForRevision();
        boolean changed = populateFromJson(newOrder, edits);
        if (!changed) {
            return null;
        }
        populateDefaultsForAllOrders(newOrder);

        // OpenMRS refuses to revise any order whose autoexpire date is in the past.  Therefore, for
        // such orders, we have to store revisions with the NEW action instead of the REVISE action.
        if (orderHasExpired(order)) {
            newOrder.setAction(Order.Action.NEW);
        }
        return newOrder;
    }

    @Override
    public void delete(String uuid, String reason, RequestContext context)
            throws ResponseException {
        try {
            logger.request(context, this, "delete");
            deleteInner(uuid);
            logger.reply(context, this, "delete", null);
        } catch (Exception e) {
            logger.error(context, this, "delete", e);
            throw e;
        }
    }

    public void deleteInner(String uuid) throws ResponseException {
        Order order = orderService.getOrderByUuid(uuid);
        if (order == null) {
            throw new ObjectNotFoundException();
        }
        if (order.isVoided()) {
            return;
        }

        // Void all orders in the chain.
        Order orderToVoid = getLatestVersion(order);
        do {
            orderService.voidOrder(orderToVoid,
                    "Voided by Buendia Android client in delete request");
            orderToVoid = orderToVoid.getPreviousOrder();
        } while (orderToVoid != null);
    }

    private static boolean orderHasExpired(Order order) {
        Date now = new Date();
        return (order.getDateStopped() != null && order.getDateStopped().before(now))
                || (order.getAutoExpireDate() != null && order.getAutoExpireDate().before(now));
    }
}
