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
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.resource.api.Updatable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Rest API for orders.
 *
 * <p>Expected behavior:
 * <ul>
 * <li>GET /order?patient=[UUID] returns all orders for a patient ({@link #search(RequestContext)})
 * <li>GET /order/[UUID] returns a single order ({@link #retrieve(String, RequestContext)})
 * <li>POST /order?patient=[UUID] creates an order for a patient ({@link #create(SimpleObject, RequestContext)}
 * <li>POST /order/[UUID] updates a order ({@link #update(String, SimpleObject, RequestContext)})
 * </ul>
 *
 * <p>Each operation handles Order resources in the following JSON form:
 *
 * <pre>
 * {
 *   "uuid": "e5e755d4-f646-45b6-b9bc-20410e97c87c", // assigned by OpenMRS, not required for creation
 *   "instructions": "Paracetamol 2 tablets 3x/day",
 *   "start": 1438711253000,
 *   "stop": 1438714253000  // optionally present
 * }
 * </pre>
 * (Results may also contain deprecated fields other than those described above.)
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
@Resource(
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/order",
    supportedClass = Order.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class OrderResource implements Searchable, Retrievable, Creatable, Updatable {
    static final User CREATOR = new User(1);  // fake value
    static final RequestLogger logger = RequestLogger.LOGGER;
    static Log log = LogFactory.getLog(OrderResource.class);
    static final String FREE_TEXT_ORDER_UUID = "buendia.free_text_order";

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

    Patient getPatient(RequestContext context) {
        String patientUuid = context.getParameter("patient");
        if (patientUuid == null) {
            throw new IllegalArgumentException("?patient= parameter not specified");
        }
        Patient patient = patientService.getPatientByUuid(patientUuid);
        if (patient == null) {
            throw new ObjectNotFoundException();
        }
        return patient;
    }

    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
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
        List<Order> orders = orderService.getAllOrdersByPatient(patient);  // includes stopped orders
        return getSimpleObjectWithResults(orders);
    }

    public Object create(SimpleObject json, RequestContext context) throws ResponseException {
        try {
            logger.request(context, this, "create", json);
            Object result = createInner(getPatient(context), json);
            logger.reply(context, this, "create", result);
            return result;
        } catch (Exception e) {
            logger.error(context, this, "create", e);
            throw e;
        }
    }

    Object createInner(Patient patient, SimpleObject json) throws ResponseException {
        Order order = jsonToOrder(patient, json);
        orderService.saveOrder(order, null);
        return orderToJson(order);
    }

    protected Order jsonToOrder(Patient patient, SimpleObject json) {
        Encounter encounter = new Encounter();
        encounter.setEncounterDatetime(new Date());
        encounter.setPatient(patient);
        encounter.setLocation(Context.getLocationService().getDefaultLocation());
        encounter.setEncounterType(encounterService.getEncounterType("ADULTRETURN"));
        encounterService.saveEncounter(encounter);

        Order order = new Order();  // an excellent band
        order.setCreator(CREATOR);  // TODO: do this properly from authentication
        List<Provider> providers = providerService.getAllProviders(false); // omit retired
        order.setOrderer(providers.get(0));
        order.setOrderType(DbUtil.getMiscOrderType());
        order.setCareSetting(orderService.getCareSettingByName("Outpatient"));
        order.setConcept(DbUtil.getConcept(
                "Order described in free text instructions",
                FREE_TEXT_ORDER_UUID, "N/A", "Misc"));
        order.setDateCreated(new Date());
        order.setPatient(patient);
        order.setInstructions((String) json.get("instructions"));
        order.setDateActivated(new Date());
        order.setEncounter(encounter);
        return order;
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

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }

    SimpleObject getSimpleObjectWithResults(List<Order> orders) {
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (Order order : orders) {
            jsonResults.add(orderToJson(order));
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }

    @Override
    public Object update(String uuid, SimpleObject simpleObject, RequestContext context) throws ResponseException {
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
     *     <li>Any field that is set overwrites the current content
     *     <li>Any field with a key but value == null deletes the current content
     *     <li>Any field whose key is not present leaves the current content unchanged
     *     <li>If the client requests a change that is illegal, that is an error. Really the
     *         whole call should fail, but for now there may be partial updates
     * </ul>
     */
    Object updateInner(String uuid, SimpleObject simpleObject) throws ResponseException {
        Order order = orderService.getOrderByUuid(uuid);
        if (order == null) {
            throw new ObjectNotFoundException();
        }

        if (applyEdits(order, simpleObject)) {
            orderService.saveOrder(order, null);
        }
        return orderToJson(order);
    }

    /** Applies edits to an Order.  Returns true if any changes were made. */
    protected boolean applyEdits(Order order, SimpleObject edits) {
        boolean changed = false;
        for (String key : edits.keySet()) {
            Object value = edits.get(key);
            switch (key) {
                case "stop":
                    if (value == null) {
                        order.setAutoExpireDate(null);
                        changed = true;
                    } else if (value instanceof Integer) {
                        order.setAutoExpireDate(new Date((Integer) value));
                        changed = true;
                    } else {
                        log.warn("Key '" + key + "' has value of invalid type: " + value);
                    }
                    break;

                case "instructions":
                    if (value instanceof String) {
                        order.setInstructions((String) value);
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
        return changed;
    }

    /** Serializes an order to JSON. */
    protected static SimpleObject orderToJson(Order order) {
        SimpleObject jsonForm = new SimpleObject();
        if (order != null) {
            jsonForm.add("uuid", order.getUuid());
            String instructions = order.getInstructions();
            if (instructions != null) {
                jsonForm.add("instructions", instructions);
            }
            Date start = order.getDateActivated();
            if (start != null) {
                jsonForm.add("start", start.getTime());
            }
            Date stop = order.getAutoExpireDate();
            if (stop != null) {
                jsonForm.add("stop", stop.getTime());
            }
        }
        return jsonForm;
    }
}
