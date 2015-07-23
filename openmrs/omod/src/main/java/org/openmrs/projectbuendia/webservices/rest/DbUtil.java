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

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;

import java.util.*;

/** Static helper methods for handling OpenMRS database entities and UUIDs. */
public class DbUtil {
    // OpenMRS object names
    public static final String MSF_IDENTIFIER = "MSF";

    // OpenMRS object UUIDs
    public static final String ASSIGNED_LOCATION_PERSON_ATTRIBUTE_TYPE_UUID =
            "0dd66a70-5d0a-4665-90be-67e2fe01b3fc";

    /** Gets or creates the PatientIdentifierType for MSF patient IDs. */
    public static PatientIdentifierType getMsfIdentifierType() {
        PatientService service = Context.getPatientService();
        PatientIdentifierType identifierType =
                service.getPatientIdentifierTypeByName(MSF_IDENTIFIER);
        if (identifierType == null) {
            identifierType = new PatientIdentifierType();
            identifierType.setName(MSF_IDENTIFIER);
            identifierType.setDescription("MSF patient identifier");
            identifierType.setFormatDescription("[facility code].[patient number]");
            service.savePatientIdentifierType(identifierType);
        }
        return identifierType;
    }

    /** Gets or creates a Concept with a given UUID and name. */
    public static Concept getConcept(String name, String uuid, String typeUuid) {
        ConceptService conceptService = Context.getConceptService();
        Concept concept = conceptService.getConceptByUuid(uuid);
        if (concept == null) {
            concept = new Concept();
            concept.setUuid(uuid);
            concept.setShortName(new ConceptName(name, new Locale("en")));
            concept.setDatatype(conceptService.getConceptDatatypeByUuid(typeUuid));
            concept.setConceptClass(conceptService.getConceptClassByUuid(ConceptClass.MISC_UUID));
            conceptService.saveConcept(concept);
        }
        return concept;
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

    /** Gets the attribute type for the patient's assigned location. */
    public static PersonAttributeType getAssignedLocationAttributeType() {
        return getPersonAttributeType(
                ASSIGNED_LOCATION_PERSON_ATTRIBUTE_TYPE_UUID, "assigned_location");
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
