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
import org.openmrs.Location;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;

import java.util.Locale;

/** Static helper methods for handling OpenMRS database entities and UUIDs. */
public class DbUtil {
    // OpenMRS object names
    public static final String MSF_IDENTIFIER = "MSF";
    public static final String TIMESTAMP_IDENTIFIER = "Timestamp";

    // OpenMRS object UUIDs
    public static final String ASSIGNED_LOCATION_PERSON_ATTRIBUTE_TYPE_UUID =
        "0dd66a70-5d0a-4665-90be-67e2fe01b3fc";

    /** Gets or creates the PatientIdentifierType for MSF patient IDs. */
    public static PatientIdentifierType getIdentifierType(String name, String description) {
        PatientService service = Context.getPatientService();
        PatientIdentifierType identifierType =
            service.getPatientIdentifierTypeByName(name);
        if (identifierType == null) {
            identifierType = new PatientIdentifierType();
            identifierType.setName(name);
            identifierType.setDescription(description);
            service.savePatientIdentifierType(identifierType);
        }
        return identifierType;
    }

    /** Gets or creates the PatientIdentifierType for MSF patient IDs. */
    public static PatientIdentifierType getMsfIdentifierType() {
        return getIdentifierType(MSF_IDENTIFIER, "MSF patient identifier");
    }

    /** Gets or creates the PatientIdentifierType for timestamps (used for patients with no MSF ID). */
    public static PatientIdentifierType getTimestampIdentifierType() {
        return getIdentifierType(TIMESTAMP_IDENTIFIER, "Timestamp-based patient identifier");
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
        return DbUtil.getConcept(
            "Order executed",
            // The OpenMRS "uuid" field is misnamed; OpenMRS uses the field for
            // arbitrary string IDs unrelated to RFC 4122.  Therefore, to prevent
            // collisions, UUIDs specific to this module are prefixed "buendia-".
            "buendia-concept-order_executed",
            "Numeric",
            "Finding");
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
            ASSIGNED_LOCATION_PERSON_ATTRIBUTE_TYPE_UUID, "assigned_location");
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
