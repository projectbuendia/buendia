package org.openmrs.projectbuendia.webservices.rest;

import org.apache.commons.lang3.ArrayUtils;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;

import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.api.Bookmark;
import org.projectbuendia.openmrs.api.db.SyncPage;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.openmrs.projectbuendia.Utils.eq;

@Resource(
    name = RestController.PATH + "/orders",
    supportedClass = Order.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*,1.12.*,2.0.*,2.1.*,2.2.*,2.3.*"
)
public class OrderResource extends BaseResource<Order> {
    private static final int MAX_ORDERS_PER_PAGE = 100;
    // Allow all order actions except discontinues, because the client doesn't represent those.
    private static final Order.Action[] ALLOWABLE_ACTIONS =
        ArrayUtils.removeElement(Order.Action.values(), Order.Action.DISCONTINUE);

    public OrderResource() {
        super("orders", Representation.DEFAULT);
    }

    @Override protected Collection<Order> listItems(RequestContext context) {
        return buendiaService.getOrdersModifiedAtOrAfter(
            null, false /* include voided */, MAX_ORDERS_PER_PAGE, ALLOWABLE_ACTIONS).results;
    }

    @Override protected SimpleObject syncItems(Bookmark bookmark, List<Order> items) {
        SyncPage<Order> orders = buendiaService.getOrdersModifiedAtOrAfter(
            bookmark, true /* include voided */, MAX_ORDERS_PER_PAGE, ALLOWABLE_ACTIONS);
        items.addAll(orders.results);
        Bookmark newBookmark = Bookmark.clampToBufferedRequestTime(
            orders.bookmark, new Date());
        // If we fetched a full page, there's probably more data available.
        boolean more = orders.results.size() == MAX_ORDERS_PER_PAGE;
        return new SimpleObject()
            .add("bookmark", newBookmark.serialize())
            .add("more", more);
    }

    @Override protected Order createItem(SimpleObject data, RequestContext context) {
        Patient patient = DbUtils.patientsByUuid.get(Utils.getRequiredString(data, "patient_uuid"));
        Provider provider = DbUtils.providersByUuid.get(Utils.getRequiredString(data, "provider_uuid"));
        Date dateCreated = new Date();

        Order order = new Order();
        order.setCreator(DbUtils.getAuthenticatedUser());
        order.setDateCreated(dateCreated);

        // Entered fields
        order.setOrderer(provider);
        order.setPatient(patient);
        order.setEncounter(createEncounter(patient, provider, dateCreated));
        order.setInstructions(Utils.getOptionalString(data, "instructions"));
        order.setScheduledDate(Utils.getOptionalDatetime(data, "start_time"));
        order.setAutoExpireDate(Utils.getOptionalDatetime(data, "stop_time"));

        // Fixed fields
        order.setOrderType(DbUtils.getMiscOrderType());
        order.setCareSetting(DbUtils.getInpatientCareSetting());
        order.setConcept(DbUtils.getFreeTextOrderConcept());
        order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);

        return orderService.saveOrder(order, null);
    }

    @Override protected Order retrieveItem(String uuid) {
        return orderService.getOrderByUuid(uuid);
    }

    @Override protected Order updateItem(Order order, SimpleObject data, RequestContext context) {
        Patient patient = DbUtils.patientsByUuid.get(Utils.getOptionalString(data, "patient_uuid"));
        Provider provider = DbUtils.providersByUuid.get(Utils.getRequiredString(data, "provider_uuid"));
        Date dateUpdated = new Date();

        Order lastOrder = DbUtils.getLastRevision(order);
        Order newOrder = lastOrder.cloneForRevision();
        newOrder.setCreator(DbUtils.getAuthenticatedUser());
        newOrder.setDateCreated(dateUpdated);

        // Entered fields
        newOrder.setOrderer(provider);
        if (patient != null && !eq(patient, newOrder.getPatient())) {
            throw new IllegalPropertyException("Cannot change the Patient on an Order");
        }
        newOrder.setEncounter(createEncounter(newOrder.getPatient(), provider, dateUpdated));
        if (data.containsKey("instructions")) {
            newOrder.setInstructions(Utils.getOptionalString(data, "instructions"));
        }
        if (data.containsKey("start_time")) {
            newOrder.setScheduledDate(Utils.getOptionalDatetime(data, "start_time"));
        }
        if (data.containsKey("stop_time")) {
            newOrder.setAutoExpireDate(Utils.getOptionalDatetime(data, "stop_time"));
        }

        // OpenMRS refuses to revise any order whose autoexpire date is in the past.  Therefore, for
        // such orders, we have to store revisions with the NEW action instead of the REVISE action.
        if (orderHasExpired(lastOrder)) {
            newOrder.setAction(Order.Action.NEW);
        }
        return orderService.saveOrder(newOrder, null);
    }

    @Override protected void deleteItem(Order order, String reason, RequestContext context) {
        for (order = DbUtils.getLastRevision(order); order != null; order = order.getPreviousOrder()) {
            orderService.voidOrder(order, reason + " (from Buendia client)");
        }
    }

    @Override protected void populateJson(SimpleObject json, Order order, RequestContext context) {
        // The UUID on the client is for the order at the head of the revision chain...
        Order rootOrder = DbUtils.getRootOrder(order);
        // ...but the data we return should come from the last revision in the chain.
        order = DbUtils.getLastRevision(rootOrder);

        json.add("uuid", rootOrder.getUuid());
        if (order.getPatient() != null) {
            json.add("patient_uuid", order.getPatient().getUuid());
        }
        if (order.getOrderer() != null) {
            json.add("provider_uuid", order.getOrderer().getUuid());
        }
        json.add("instructions", order.getInstructions());
        if (order.getScheduledDate() != null) {
            json.add("start_time", Utils.formatUtc8601(order.getScheduledDate()));
        }
        Date stop = order.getDateStopped();
        if (stop == null) stop = order.getAutoExpireDate();
        if (stop != null) {
            json.add("stop_time", Utils.formatUtc8601(stop));
        }
    }

    private static boolean orderHasExpired(Order order) {
        Date now = new Date();
        return (order.getDateStopped() != null && order.getDateStopped().before(now))
            || (order.getAutoExpireDate() != null && order.getAutoExpireDate().before(now));
    }

    private Encounter createEncounter(Patient patient, Provider provider, Date datetime) {
        Encounter encounter = new Encounter();
        encounter.setCreator(DbUtils.getAuthenticatedUser());
        encounter.setEncounterDatetime(datetime);
        encounter.setPatient(patient);
        encounter.setLocation(DbUtils.getDefaultRoot());
        encounter.setEncounterType(encounterService.getEncounterType("ADULTRETURN"));
        encounter.setProvider(DbUtils.getUnknownEncounterRole(), provider);
        encounterService.saveEncounter(encounter);
        return encounter;
    }
}
