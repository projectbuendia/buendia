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

import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptName;
import org.openmrs.EncounterRole;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.openmrs.projectbuendia.Utils.ALPHANUMERIC_COMPARATOR;
import static org.openmrs.projectbuendia.Utils.eq;

/** Static helper methods for handling OpenMRS database entities and UUIDs. */
public class DbUtils {
    // The OpenMRS "uuid" field is misnamed; OpenMRS uses the field for arbitrary
    // string IDs unrelated to RFC 4122.  Therefore, to prevent collisions and
    // facilitate readability, all UUIDs specific to Buendia are prefixed "buendia_".

    // Clients must use this concept UUID for observations that signify that an
    // order was executed.  This is the only hardcoded UUID that clients need to know.
    public static final String CONCEPT_ORDER_EXECUTED_UUID = "buendia_concept_order_executed";

    // The concept UUID for all orders.  This is used internally by the server and
    // does not need to be known by clients.
    public static final String CONCEPT_FREE_TEXT_ORDER_UUID = "buendia_concept_free_text_order";

    // The UUIDs of these two PatientIdentifierTypes are hardcoded.  If rows don't
    // exist in the database with these names, they will be created.  Clients don't
    // need to know these constants because the server handles them internally when
    // interpreting and returning a patient's "id" field.
    public static final String IDENTIFIER_TYPE_MSF_UUID = "buendia_identifier_type_msf";
    public static final String IDENTIFIER_TYPE_LOCAL_UUID = "buendia_identifier_type_local";

    // This UUID is hardcoded, but clients don't need to know it because the server
    // internally converts the "assigned_location" to a person attribute.
    public static final String ATTRIBUTE_TYPE_ASSIGNED_LOCATION_UUID = "buendia_attribute_type_location";


    /** Gets or creates the PatientIdentifierType for MSF patient IDs. */
    public static PatientIdentifierType getIdentifierType(String uuid, String name, String description) {
        PatientService service = Context.getPatientService();
        PatientIdentifierType identifierType =
            service.getPatientIdentifierTypeByUuid(uuid);
        if (identifierType == null) {
            identifierType = new PatientIdentifierType();
            identifierType.setUuid(uuid);
            identifierType.setName(name);
            identifierType.setDescription(description);
            service.savePatientIdentifierType(identifierType);
        } else if (!eq(identifierType.getName(), name)) {
            identifierType.setName(name);
            service.savePatientIdentifierType(identifierType);
        }
        return identifierType;
    }

    /** Gets or creates the PatientIdentifierType for MSF patient IDs. */
    public static PatientIdentifierType getIdentifierTypeMsf() {
        return getIdentifierType(IDENTIFIER_TYPE_MSF_UUID, "MSF", "MSF patient identifier");
    }

    /** Gets or creates the PatientIdentifierType for local integers (used for patients with no MSF ID). */
    public static PatientIdentifierType getIdentifierTypeLocal() {
        return getIdentifierType(IDENTIFIER_TYPE_LOCAL_UUID, "LOCAL", "Local numeric patient identifier");
    }

    public static OrderType getDrugOrderType() {
        OrderService orderService = Context.getOrderService();
        OrderType orderType = orderService.getOrderTypeByConceptClass(getConceptClass("Drug"));
        if (orderType == null) {
            orderType = new OrderType();
            orderType.addConceptClass(getConceptClass("Drug"));
            orderType.setName("Drug");
            orderType.setDescription("Drug order");
            orderType.setJavaClassName("org.openmrs.DrugOrder");
            orderService.saveOrderType(orderType);
        }
        return orderType;
    }

    public static ConceptClass getConceptClass(String name) {
        ConceptService conceptService = Context.getConceptService();
        return conceptService.getConceptClassByName(name);
    }

    public static OrderType getMiscOrderType() {
        OrderService orderService = Context.getOrderService();
        OrderType orderType = orderService.getOrderTypeByConceptClass(getConceptClass("Misc"));
        if (orderType == null) {
            orderType = new OrderType();
            orderType.addConceptClass(getConceptClass("Misc"));
            orderType.setName("Misc order");
            orderType.setDescription("Misc order");
            orderType.setJavaClassName("org.openmrs.Order");
            orderService.saveOrderType(orderType);
        }
        return orderType;
    }

    // Gets the concept representing that an order has been executed.  Each time
    // an order is executed, this is recorded in the system as an encounter in
    // which "order executed" is observed for the appropriate order.
    public static Concept getOrderExecutedConcept() {
        return DbUtils.getConcept(
            "Order executed", CONCEPT_ORDER_EXECUTED_UUID, "N/A", "Finding");
    }

    // Gets the default concept for orders in Buendia.  We don't store dosages,
    // frequencies, etc. in the drug_* and order_* tables; our orders encode
    // such information in the "instructions" field and use this concept.
    public static Concept getFreeTextOrderConcept() {
        return getConcept(
            "Order described in free text instructions",
            CONCEPT_FREE_TEXT_ORDER_UUID, "N/A", "Misc");
    }

    /** Gets or creates a Concept with a given UUID and name. */
    public static Concept getConcept(String name, String uuid, String typeName, String className) {
        ConceptService conceptService = Context.getConceptService();
        Concept concept = conceptService.getConceptByUuid(uuid);
        if (concept == null) {
            concept = new Concept();
            concept.setUuid(uuid);
            concept.setShortName(new ConceptName(name, new Locale("en")));
            concept.setDatatype(conceptService.getConceptDatatypeByName(typeName));
            concept.setConceptClass(conceptService.getConceptClassByName(className));
            conceptService.saveConcept(concept);
        }
        return concept;
    }

    /** Gets the attribute type for the patient's assigned location. */
    public static PersonAttributeType getAssignedLocationAttributeType() {
        return getPersonAttributeType(
            ATTRIBUTE_TYPE_ASSIGNED_LOCATION_UUID, "assigned_location");
    }

    /** Returns the currently authenticated user. */
    public static User getAuthenticatedUser() {
        return Context.getUserContext().getAuthenticatedUser();
    }

    /** Gets the designated EncounterRole object for an unknown role. */
    public static EncounterRole getUnknownEncounterRole() {
        EncounterService encounterService = Context.getEncounterService();
        EncounterRole role = encounterService.getEncounterRoleByUuid(
            EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID);
        if (role == null) {
            role = new EncounterRole();
            role.setUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID);
            role.setName("Unknown");
            encounterService.saveEncounterRole(role);
        }
        return role;
    }

    /**
     * Adjusts an encounter datetime to ensure that OpenMRS will accept it.
     * The OpenMRS core is not designed for a client-server setup -- it will
     * summarily reject a submitted encounter if the encounter_datetime is in
     * the future, even if the client's clock is off by only one millisecond.
     */
    public static Date fixEncounterDatetime(Date datetime) {
        Date now = new Date();
        if (datetime.after(now)) {
            datetime = now;
        }
        return datetime;
    }

    /** Searches back through revision orders until it finds the root order. */
    public static Order getRootOrder(Order order) {
        while (order.getPreviousOrder() != null) {
            order = order.getPreviousOrder();
        }
        return order;
    }

    /** Gets or creates a PersonAttributeType with a given UUID and name. */
    private static PersonAttributeType getPersonAttributeType(String uuid, String name) {
        PersonService personService = Context.getPersonService();
        PersonAttributeType personAttributeType = personService.getPersonAttributeTypeByUuid(uuid);
        if (personAttributeType == null) {
            personAttributeType = new PersonAttributeType();
            personAttributeType.setUuid(uuid);
            personAttributeType.setName(name);
            personAttributeType.setDescription(name);
            personAttributeType.setForeignKey(0);
            personAttributeType.setFormat("org.openmrs.Location");
            personService.savePersonAttributeType(personAttributeType);
        }
        return personAttributeType;
    }

    /** Gets the value of an attribute on a person. */
    public static String getPersonAttributeValue(Person person, PersonAttributeType attrType) {
        PersonAttribute attribute = person.getAttribute(attrType);
        return attribute != null ? attribute.getValue() : null;
    }

    /** Sets an attribute on a person. */
    public static void setPersonAttributeValue(
        Patient patient, PersonAttributeType attrType, String value) {
        PersonService personService = Context.getPersonService();
        PersonAttribute attribute = patient.getAttribute(attrType);
        if (attribute == null) {
            attribute = new PersonAttribute();
            attribute.setAttributeType(attrType);
            attribute.setValue(value);
            patient.addAttribute(attribute);
        } else {
            attribute.setValue(value);
        }
        personService.savePerson(patient);
    }

    public static final Comparator<Location> ALPHANUMERIC_NAME_COMPARATOR = new Comparator<Location>() {
        @Override public int compare(Location a, Location b) {
            return ALPHANUMERIC_COMPARATOR.compare(a.getName(), b.getName());
        }
    };

    /**
     * Gets the default location (where identifiers will be placed).  For consistency
     * with the client, this is the root with the alphanumerically first (lowest) name.
     */
    public static Location getDefaultLocation() {
        LocationService locationService = Context.getLocationService();
        List<Location> locations = locationService.getAllLocations(false);
        Collections.sort(locations, ALPHANUMERIC_NAME_COMPARATOR);
        for (Location location : locationService.getAllLocations(false)) {
            if (location.getParentLocation() == null) {
                return location;
            }
        }
        throw new IllegalStateException("Location tree contains a cycle");
    }

    /** Gets a Location entity by its exact name. */
    public static Location getLocationByName(String locationName, Location parent) {
        LocationService locationService = Context.getLocationService();
        Location location = locationService.getLocation(locationName);
        if (location == null) {
            location = new Location();
            location.setName(locationName);
            location.setDescription(locationName);
            if (parent != null) {
                location.setParentLocation(parent);
            }
            locationService.saveLocation(location);
        }
        return location;
    }
}
