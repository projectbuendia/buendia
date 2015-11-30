/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distrib-
 * uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
 * specific language governing permissions and limitations under the License.
 */

package org.projectbuendia.openmrs;

import org.openmrs.Patient;
import org.openmrs.Person;

import java.util.Date;

/**
 * Represents incremental sync parameters for a {@link Patient}.
 * <p>
 * {@link Person Persons} in OpenMRS have three timestamp fields that get updated when the record
 * changes - dateCreated, dateChanged, and dateVoided. These values aren't usable for incremental
 * paginated patient sync, because our pagination system needs to sort all records based on
 * {@code GREATEST(dateCreated, dateChanged, dateVoided)}. It's not currently possible to create
 * indexes on functions in MySQL [^1], thus adding this computation to every query means that MySQL
 * would have to load every `patient` for every page fetch. We work around this by creating a
 * trigger on the `person` database that creates a record in `buendia_patient_sync_map` with the patient ID
 * and the timestamp of the record update.
 * <p>
 * The records in `buendia_patient_sync_map` are loadable by Hibernate, and have a link back to the Patient
 * that they represent. Thus, by performing Hibernate queries on this class, it is possible to
 * retrieve pages of patients with relative ease. See `PatientSyncParameters.hbm.xml`.
 * <p>
 * This class should be interpreted as read-only - all updates are done by the database triggers.
 * The setters only exist so that the object can be instantiated by Hibernate.
 * <p>
 * [^1]: You can create indexes on generated columns in MySQL 5.7.5+, which is functionally
 * equivalent, but the Buendia project won't be on 5.7.5+ at least until Debian is.
 * Current MySQL version for Debian: http://distrowatch.com/table.php?distribution=debian
 *
 */
public class PatientSyncParameters {
    private int patientId;
    private Date dateUpdated;
    private Patient patient;
    private String uuid;

    public Patient getPatient() {
        return patient;
    }

    protected void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    protected void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public int getPatientId() {
        return patientId;
    }

    protected void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getUuid() {
        return uuid;
    }

    protected void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
