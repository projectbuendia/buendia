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

import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.projectbuendia.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import static org.openmrs.projectbuendia.Utils.ALPHANUMERIC_COMPARATOR;
import static org.openmrs.projectbuendia.Utils.eq;

/** Static helper methods for handling OpenMRS database entities and UUIDs. */
public class DbUtils {
    public static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("en");
    public static final Locale BUENDIA_LOCALE = new Locale("buendia");

    // The OpenMRS "uuid" field is misnamed; OpenMRS uses the field for arbitrary
    // string IDs unrelated to RFC 4122.  Therefore, to prevent collisions and
    // facilitate readability, all UUIDs specific to Buendia are prefixed "buendia_".

    // The concept UUID for all orders.  This is used internally by the server and
    // does not need to be known by clients.
    public static final String CONCEPT_FREE_TEXT_ORDER_UUID = "buendia_concept_free_text_order";

    // Clients must use this concept UUID for observations that signify that an
    // order was executed.  This is the only hardcoded UUID that clients need to know.
    public static final String CONCEPT_ORDER_EXECUTED_UUID = "buendia_concept_order_executed";

    // Clients should use this concept UUID for observations that indicate a patient's
    // assignment to a location or bed.  The server will not break if a different UUID
    // is used; the only effect is that the other concept will not be created automatically.
    public static final String CONCEPT_PLACEMENT_UUID = "buendia_concept_placement";

    // This UUID is hardcoded but only used internally by the server to distinguish
    // chart forms (exposed at /charts) from XForm forms (exposed at /xforms).
    public static final String ENCOUNTER_TYPE_CHART_UUID = "buendia_encounter_type_chart";

    public static final String ORDER_TYPE_MISC_UUID = "buendia_order_type_misc";

    // The UUIDs of these two PatientIdentifierTypes are hardcoded.  If rows don't
    // exist in the database with these names, they will be created.  Clients don't
    // need to know these constants because the server handles them internally when
    // interpreting and returning a patient's "id" field.
    public static final String IDENTIFIER_TYPE_MSF_UUID = "buendia_identifier_type_msf";
    public static final String IDENTIFIER_TYPE_LOCAL_UUID = "buendia_identifier_type_local";

    // This UUID is for the default provider, named "Guest".
    private static final String PROVIDER_GUEST_UUID = "buendia_provider_guest";
    private static final String GUEST_NAME = "Guest";

    /** A map from HL7 type abbreviations to short names used in JSON output. */
    private static final Map<String, String> HL7_TYPE_NAMES = new HashMap<>();
    static {
        HL7_TYPE_NAMES.put(HL7Constants.HL7_BOOLEAN, "coded");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_CODED, "coded");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_CODED_WITH_EXCEPTIONS, "coded");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_TEXT, "text");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_NUMERIC, "numeric");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_DATE, "date");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_DATETIME, "datetime");
        HL7_TYPE_NAMES.put("ZZ", "none");
    }

    // Gets the default concept for orders in Buendia.  We don't store dosages,
    // frequencies, etc. in the drug_* and order_* tables; our orders encode
    // such information in the "instructions" field and use this concept.
    public static Concept getFreeTextOrderConcept() {
        ensureRequiredObjectsExist();
        return Context.getConceptService().getConceptByUuid(CONCEPT_FREE_TEXT_ORDER_UUID);
    }

    // Gets the concept representing that an order has been executed.  Each time
    // an order is executed, this is recorded in the system as an encounter in
    // which "order executed" is observed for the appropriate order.
    public static Concept getOrderExecutedConcept() {
        ensureRequiredObjectsExist();
        return Context.getConceptService().getConceptByUuid(CONCEPT_ORDER_EXECUTED_UUID);
    }

    // Gets the concept representing the placement of patients at a location and bed.
    public static Concept getPlacementConcept() {
        ensureRequiredObjectsExist();
        return Context.getConceptService().getConceptByUuid(CONCEPT_PLACEMENT_UUID);
    }

    public static EncounterType getChartEncounterType() {
        ensureRequiredObjectsExist();
        return Context.getEncounterService().getEncounterTypeByUuid(ENCOUNTER_TYPE_CHART_UUID);
    }

    public static OrderType getMiscOrderType() {
        ensureRequiredObjectsExist();
        return Context.getOrderService().getOrderTypeByUuid(ORDER_TYPE_MISC_UUID);
    }

    public static PatientIdentifierType getLocalIdType() {
        ensureRequiredObjectsExist();
        return Context.getPatientService().getPatientIdentifierTypeByUuid(IDENTIFIER_TYPE_LOCAL_UUID);
    }

    public static PatientIdentifierType getMsfIdType() {
        ensureRequiredObjectsExist();
        return Context.getPatientService().getPatientIdentifierTypeByUuid(IDENTIFIER_TYPE_MSF_UUID);
    }

    public static Provider getGuestProvider() {
        ensureRequiredObjectsExist();
        return Context.getProviderService().getProviderByUuid(PROVIDER_GUEST_UUID);
    }

    /** Creates all the required objects at once (in a fixed order, yielding predictable IDs). */
    public static void ensureRequiredObjectsExist() {
        getConcept(CONCEPT_FREE_TEXT_ORDER_UUID, "Free text order", "N/A", "Misc");
        getConcept(CONCEPT_ORDER_EXECUTED_UUID, "Order executed", "Numeric", "Misc");
        getConcept(CONCEPT_PLACEMENT_UUID, "Placement", "Text", "Misc");
        getEncounterType(ENCOUNTER_TYPE_CHART_UUID, "Chart");
        getOrderType(ORDER_TYPE_MISC_UUID, "Misc", "Misc order", "org.openmrs.Order");
        getIdentifierType(IDENTIFIER_TYPE_LOCAL_UUID, "LOCAL", "Local numeric patient identifier");
        getIdentifierType(IDENTIFIER_TYPE_MSF_UUID, "MSF", "MSF patient identifier");
        getProvider(PROVIDER_GUEST_UUID, GUEST_NAME);
    }

    /** Gets or creates a Concept with a given UUID and name. */
    private static Concept getConcept(String uuid, String name, String typeName, String className) {
        ConceptService conceptService = Context.getConceptService();
        Concept concept = conceptService.getConceptByUuid(uuid);
        if (concept == null) {
            ConceptDatatype type = conceptService.getConceptDatatypeByName(typeName);
            if (eq(type.getHl7Abbreviation(), HL7Constants.HL7_NUMERIC)) {
                concept = new ConceptNumeric();
            } else {
                concept = new Concept();
            }
            concept.setUuid(uuid);
            concept.setShortName(new ConceptName(name, Locale.forLanguageTag("en")));
            concept.setDatatype(type);
            concept.setConceptClass(conceptService.getConceptClassByName(className));
            conceptService.saveConcept(concept);
        }
        return concept;
    }

    /** Gets or creates an EncounterType with the given UUID and name. */
    private static EncounterType getEncounterType(String uuid, String name) {
        EncounterService encounterService = Context.getEncounterService();
        EncounterType encType = encounterService.getEncounterTypeByUuid(uuid);
        if (encType == null) {
            encType = new EncounterType();
            encType.setUuid(uuid);
            encType.setName(name);
            encType.setDescription(name);
            encounterService.saveEncounterType(encType);
        }
        return encType;
    }

    /** Gets or creates an OrderType with the given properties. */
    private static OrderType getOrderType(String uuid, String conceptClassName, String name, String javaClassName) {
        OrderService orderService = Context.getOrderService();
        OrderType orderType = orderService.getOrderTypeByConceptClass(getConceptClass(conceptClassName));
        if (orderType == null) {
            orderType = new OrderType();
            orderType.setUuid(uuid);
            orderType.addConceptClass(getConceptClass(conceptClassName));
            orderType.setName(name);
            orderType.setDescription(name);
            orderType.setJavaClassName(javaClassName);
            orderService.saveOrderType(orderType);
        }
        return orderType;
    }

    /** Gets or creates a PatientIdentifierType with the given UUID and name. */
    private static PatientIdentifierType getIdentifierType(String uuid, String name, String description) {
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

    /** Gets or creates a Provider with the given UUID and name. */
    private static Provider getProvider(String uuid, String name) {
        ProviderService providerService = Context.getProviderService();
        Provider provider = providerService.getProviderByUuid(uuid);
        if (provider == null) {
            provider = new Provider();
            provider.setCreator(DbUtils.getAuthenticatedUser());
            provider.setUuid(uuid);
            provider.setName(name);
            providerService.saveProvider(provider);
        }
        return provider;
    }

    public static CareSetting getInpatientCareSetting() {
        return Context.getOrderService().getCareSettingByName("INPATIENT");
    }

    public static Locale getLocaleForTag(String languageTag) {
        if (Utils.isBlank(languageTag)) return DEFAULT_LOCALE;
        return Locale.forLanguageTag(languageTag.trim());
    }

    public static String getConceptName(Concept concept) {
        return getConceptName(concept, BUENDIA_LOCALE);
    }

    public static String getConceptName(Concept concept, Locale locale) {
        ConceptName name = concept.getName(locale, true);
        if (name != null) return name.getName();

        String lang = locale.getLanguage();
        String region = locale.getCountry();  // this Locale method is misnamed
        String variant = locale.getVariant();
        if (variant != null) name = concept.getName(new Locale(lang, region, variant), true);
        if (name != null) return name.getName();
        if (region != null) name = concept.getName(new Locale(lang, region), true);
        if (name != null) return name.getName();
        name = concept.getName(new Locale(lang), true);
        if (name != null) return name.getName();
        name = concept.getName(DEFAULT_LOCALE);
        if (name != null) return name.getName();

        return concept.getId() + "?";
    }

    public static String getConceptTypeName(Concept concept) {
        return HL7_TYPE_NAMES.get(concept.getDatatype().getHl7Abbreviation());
    }

    public static ConceptClass getConceptClass(String name) {
        return Context.getConceptService().getConceptClassByName(name);
    }

    public static String getUuid(Concept concept) {
        if (concept == null) return null;
        return concept.getUuid();
    }

    public static Concept getConcept(Obs obs) {
        if (obs == null) return null;
        return obs.getConcept();
    }

    public static String getConceptUuid(Obs obs) {
        return getUuid(getConcept(obs));
    }

    public static Concept getConcept(Field field) {
        if (field == null) return null;
        return field.getConcept();
    }

    public static Field getField(FormField ff) {
        if (ff == null) return null;
        return ff.getField();
    }

    public static String getConceptUuid(Field field) {
        return getUuid(getConcept(field));
    }

    public static String getConceptUuid(FormField ff) {
        return getUuid(getConcept(getField(ff)));
    }

    public static String getName(BaseOpenmrsMetadata obj) {
        if (obj == null) return null;
        return Utils.toNonnull(obj.getName());
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
        if (datetime == null || datetime.after(now)) {
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

    /**
     * Finds the last order in the chain containing the given order. We need to do this instead of
     * using {@link OrderService#getRevisionOrder(Order)} because {@code getRevisionOrder(Order)}
     * only gets orders that have an action of {@link Order.Action#REVISE}. To use
     * {@code REVISE}, the previous order needs to have not expired, which we can't guarantee.
     */
    public static Order getLastRevision(Order order) {
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
        while (nextOrderUuids.containsKey(uuid)) {
            uuid = nextOrderUuids.get(uuid);
        }
        return Context.getOrderService().getOrderByUuid(uuid);
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

    /** Gets the default root location, where identifiers will be placed. */
    public static Location getDefaultRoot() {
        LocationService locationService = Context.getLocationService();
        List<Location> locations = locationService.getAllLocations(false);
        // The default location is the root with the alphanumerically first (lowest) name.
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

    public static boolean isPublishedXform(Form form) {
        ensureRequiredObjectsExist();
        EncounterType encType = form.getEncounterType();
        return !form.isRetired() && form.getPublished() && !eq(encType, getChartEncounterType());
    }

    public static boolean isChartForm(Form form) {
        ensureRequiredObjectsExist();
        EncounterType encType = form.getEncounterType();
        return !form.isRetired() && encType != null && eq(encType, getChartEncounterType());
    }

    protected static abstract class Getter<T> {
        String type;

        Getter(Class<T> cls) { type = cls.getSimpleName(); }

        protected abstract @Nullable T lookup(String uuid);

        public @Nullable T get(@Nullable String uuid) {
            if (uuid == null) return null;
            T entity = lookup(uuid);
            if (entity == null) throw new UuidNotFoundException(type, uuid);
            return entity;
        }
    }

    public static final Getter<Concept> conceptsByUuid = new Getter<Concept>(Concept.class) {
        public Concept lookup(String uuid) { return Context.getConceptService().getConceptByUuid(uuid); }
    };
    public static final Getter<Form> formsByUuid = new Getter<Form>(Form.class) {
        public Form lookup(String uuid) { return Context.getFormService().getFormByUuid(uuid); }
    };
    public static final Getter<Location> locationsByUuid = new Getter<Location>(Location.class) {
        public Location lookup(String uuid) { return Context.getLocationService().getLocationByUuid(uuid); }
    };
    public static final Getter<Order> ordersByUuid = new Getter<Order>(Order.class) {
        public Order lookup(String uuid) { return Context.getOrderService().getOrderByUuid(uuid); }
    };
    public static final Getter<Patient> patientsByUuid = new Getter<Patient>(Patient.class) {
        public Patient lookup(String uuid) { return Context.getPatientService().getPatientByUuid(uuid); }
    };
    public static final Getter<Provider> providersByUuid = new Getter<Provider>(Provider.class) {
        public Provider lookup(String uuid) { return Context.getProviderService().getProviderByUuid(uuid); }
    };

    public static boolean isVoidedOrRetired(OpenmrsObject entity) {
        if (entity instanceof Encounter && ((Encounter) entity).isVoided()) return true;
        if (entity instanceof Obs && ((Obs) entity).isVoided()) return true;
        if (entity instanceof Order && ((Order) entity).isVoided()) return true;
        if (entity instanceof Person && ((Person) entity).isPersonVoided()) return true;

        if (entity instanceof Concept && ((Concept) entity).isRetired()) return true;
        if (entity instanceof Form && ((Form) entity).isRetired()) return true;
        if (entity instanceof Location && ((Location) entity).isRetired()) return true;
        if (entity instanceof Provider && ((Provider) entity).isRetired()) return true;
        if (entity instanceof User && ((User) entity).isRetired()) return true;

        return false;
    }
}
