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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
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
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.resource.api.Updatable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rest API for orders.
 * <p/>
 * <p>Expected behavior:
 * <ul>
 * <li>GET /order?patient=[UUID] returns all orders for a patient ({@link #search(RequestContext)})
 * <li>GET /order/[UUID] returns a single order ({@link #retrieve(String, RequestContext)})
 * <li>POST /order?patient=[UUID] creates an order for a patient ({@link #create(SimpleObject,
 * RequestContext)}
 * <li>POST /order/[UUID] updates a order ({@link #update(String, SimpleObject, RequestContext)})
 * </ul>
 * <p/>
 * <p>Each operation handles Order resources in the following JSON form:
 * <p/>
 * <pre>
 * {
 *   "uuid": "e5e755d4-f646-45b6-b9bc-20410e97c87c", // assigned by OpenMRS, not required for
 *   creation
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
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/order",
    supportedClass = Order.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class OrderResource implements Listable, Searchable, Retrievable, Creatable, Updatable {
    static final User CREATOR = new User(1);  // fake value
    static final RequestLogger logger = RequestLogger.LOGGER;
    static Log log = LogFactory.getLog(OrderResource.class);
    static final String FREE_TEXT_ORDER_UUID = "buendia-concept-free_text_order";

    final PatientService patientService;
    final OrderService orderService;
    final ProviderService providerService;
    final ConceptService conceptService;
    final EncounterService encounterService;

    public OrderResource() {
        patientService = Context.getPatientService();
        orderService = Context.getOrderService();
        providerService = Context.getProviderService();
        conceptService = Context.getConceptService();
        encounterService = Context.getEncounterService();
    }

    @Override public SimpleObject getAll(RequestContext context) throws ResponseException {
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

    private SimpleObject getAllInner() throws ResponseException {
        return getSimpleObjectWithResults(getAllOrders());
    }

    SimpleObject getSimpleObjectWithResults(Collection<Order> orders) {
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Order order : orders) {
            jsonResults.add(orderToJson(order));
        }
        SimpleObject json = new SimpleObject();
        json.add("results", jsonResults);
        return json;
    }

    public Collection<Order> getAllOrders() {
        Map<String, Order> orders = new HashMap<>();
        Set<String> previousOrderUuids = new HashSet<>();
        for (Encounter encounter : encounterService.getEncounters(
            null, null, null, null, null, null, null, null, null, false)) {
            for (Order order : encounter.getOrders()) {
                orders.put(order.getUuid(), order);
                if (order.getPreviousOrder() != null) {
                    previousOrderUuids.add(order.getPreviousOrder().getUuid());
                }
            }
        }
        for (String uuid : previousOrderUuids) {
            orders.remove(uuid);
        }
        return orders.values();
    }

    /** Serializes an order to JSON. */
    protected static SimpleObject orderToJson(Order order) {
        SimpleObject json = new SimpleObject();
        if (order != null) {
            json.add("uuid", order.getUuid());
            json.add("patient_uuid", order.getPatient().getUuid());
            String instructions = order.getInstructions();
            if (instructions != null) {
                json.add("instructions", instructions);
            }
            Date start = order.getScheduledDate();
            if (start != null) {
                json.add("start_millis", start.getTime());
            }
            Date stop = order.getAutoExpireDate();
            if (stop != null) {
                json.add("stop_millis", stop.getTime());
            }
        }
        return json;
    }

    @Override public SimpleObject search(RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "getAll");
            SimpleObject result = searchInner(getPatient(context));
            logger.reply(context, this, "getAll", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "getAll", e);
            throw e;
        }
    }

    SimpleObject searchInner(Patient patient) throws ResponseException {
        return getSimpleObjectWithResults(patient == null ?
            getAllOrders() :
            orderService.getAllOrdersByPatient(patient));
    }

    Patient getPatient(RequestContext context) {
        String patientUuid = context.getParameter("patient");
        if (patientUuid == null) return null;
        Patient patient = patientService.getPatientByUuid(patientUuid);
        if (patient == null) {
            throw new ObjectNotFoundException();
        }
        return patient;
    }

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

    Object createInner(SimpleObject json) throws ResponseException {
        Order order = jsonToOrder(json);
        orderService.saveOrder(order, null);
        return orderToJson(order);
    }

    /** Creates a new Order and a corresponding Encounter containing it. */
    protected Order jsonToOrder(SimpleObject json) {
        String patientUuid = (String) json.get("patient_uuid");
        if (patientUuid == null) {
            throw new IllegalArgumentException("Required key 'patient_uuid' is missing");
        }
        Patient patient = patientService.getPatientByUuid(patientUuid);
        if (patient == null) {
            throw new ObjectNotFoundException();
        }
        String instructions = (String) json.get("instructions");
        if (instructions == null || instructions.isEmpty()) {
            throw new IllegalArgumentException("Required key 'instructions' is missing or empty");
        }
        Long startMillis = (Long) json.get("start_millis");
        Date startDate = startMillis == null ? new Date() : new Date(startMillis);
        Long stopMillis = (Long) json.get("stop_millis");
        Date stopDate = stopMillis == null ? null : new Date(stopMillis);

        Order order = new Order();  // an excellent band
        order.setCreator(CREATOR);  // TODO: do this properly from authentication
        order.setEncounter(createEncounter(patient, new Date()));
        order.setOrderer(getProvider());
        order.setOrderType(DbUtil.getMiscOrderType());
        order.setCareSetting(orderService.getCareSettingByName("Outpatient"));
        order.setConcept(getFreeTextOrderConcept());
        order.setDateCreated(new Date());
        order.setPatient(patient);
        order.setInstructions(instructions);
        order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
        order.setScheduledDate(startDate);
        order.setAutoExpireDate(stopDate);
        return order;
    }

    Encounter createEncounter(Patient patient, Date encounterDateTime) {
        Encounter encounter = new Encounter();
        encounter.setCreator(CREATOR);  // TODO: do this properly from authentication
        encounter.setEncounterDatetime(encounterDateTime);
        encounter.setPatient(patient);
        encounter.setLocation(Context.getLocationService().getDefaultLocation());
        encounter.setEncounterType(encounterService.getEncounterType("ADULTRETURN"));
        encounterService.saveEncounter(encounter);
        return encounter;
    }

    Provider getProvider() {
        return providerService.getAllProviders(false).get(0); // omit retired
    }

    Concept getFreeTextOrderConcept() {
        return DbUtil.getConcept(
            "Order described in free text instructions",
            FREE_TEXT_ORDER_UUID, "N/A", "Misc");
    }

    @Override public String getUri(Object instance) {
        Order order = (Order) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + order.getUuid();
    }

    @Override public Object retrieve(String uuid, RequestContext context) throws ResponseException {
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

    Object retrieveInner(String uuid) throws ResponseException {
        Order order = orderService.getOrderByUuid(uuid);
        if (order == null) {
            throw new ObjectNotFoundException();
        }
        return orderToJson(order);
    }

    @Override public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }

    @Override
    public Object update(String uuid, SimpleObject simpleObject, RequestContext context) throws
        ResponseException {
        try {
            logger.request(context, this, "update", uuid + ", " + simpleObject);
            Object result = updateInner(uuid, simpleObject);
            logger.reply(context, this, "update", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "update", e);
            throw e;
        }
    }

    /**
     * Receives a SimpleObject that is parsed from the Gson serialization of a client-side
     * Order bean.  It has the following semantics:
     * <ul>
     * <li>Any field that is set overwrites the current content
     * <li>Any field with a key but value == null deletes the current content
     * <li>Any field whose key is not present leaves the current content unchanged
     * <li>If the client requests a change that is illegal, that is an error. Really the
     * whole call should fail, but for now there may be partial updates
     * </ul>
     */
    Object updateInner(String uuid, SimpleObject simpleObject) throws ResponseException {
        Order order = orderService.getOrderByUuid(uuid);
        if (order == null) {
            throw new ObjectNotFoundException();
        }

        Order revisedOrder = reviseOrder(order, simpleObject);
        if (revisedOrder != null) {
            orderService.saveOrder(revisedOrder, null);
            order = revisedOrder;
        }
        return orderToJson(order);
    }

    /** Revises an order.  Returns null if no changes were made. */
    protected Order reviseOrder(Order order, SimpleObject edits) {
        Order newOrder = order.cloneForRevision();
        boolean changed = false;

        for (String key : edits.keySet()) {
            Object value = edits.get(key);
            switch (key) {
                case "stop":
                    if (value == null) {
                        newOrder.setAutoExpireDate(null);
                        changed = true;
                    } else if (value instanceof Integer) {
                        newOrder.setAutoExpireDate(new Date((Integer) value));
                        changed = true;
                    } else {
                        log.warn("Key '" + key + "' has value of invalid type: " + value);
                    }
                    break;

                case "instructions":
                    if (value instanceof String) {
                        newOrder.setInstructions((String) value);
                        changed = true;
                    } else {
                        log.warn("Key '" + key + "' has value of invalid type: " + value);
                    }
                    break;

                default:
                    log.warn("Key '" + key + "' is not the name of an editable property");
                    break;
            }
        }

        if (!changed) return null;
        Date now = new Date();
        newOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
        newOrder.setScheduledDate(now);
        newOrder.setEncounter(createEncounter(order.getPatient(), now));
        newOrder.setOrderer(getProvider());
        return newOrder;
    }
}
