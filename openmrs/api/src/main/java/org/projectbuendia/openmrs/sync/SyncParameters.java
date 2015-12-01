package org.projectbuendia.openmrs.sync;

import org.openmrs.BaseOpenmrsData;

import java.util.Date;

/**
 * Represents incremental sync parameters for some type of OpenMRS data.
 * <p>
 * Most data types in OpenMRS have three timestamp fields that get updated when the record changes -
 * dateCreated, dateChanged, and dateVoided. These values aren't usable for incremental paginated
 * sync, because our pagination system needs to sort all records based on {@code GREATEST(
 * dateCreated, dateChanged, dateVoided)}. It's not currently possible to create indexes on
 * functions in MySQL [^1], thus adding this computation to every query means that MySQL would have
 * to load every record of the given type for every page fetch. We work around this by creating a
 * trigger on the relevant DB table that creates a record in `buendia_[type]_sync_map` with the ID
 * of the record and the timestamp of the record update.
 * <p>
 * The records in `buendia_[type]_sync_map` are loadable by Hibernate, and have a link back to the
 * record that they represent. Thus, by performing Hibernate queries on this class, it is possible
 * to retrieve pages of records with relative ease.
 * <p>
 * This is a base class only - we use a different subclass for each data type, which allows us to
 * use a different database table for each data type. See `PatientSyncParameters.hbm.xml` and
 * {@link PatientSyncParameters} as an implementation example.
 * <p>
 * This class should be interpreted as read-only - all updates are done by the database triggers.
 * The setters only exist so that the object can be instantiated by Hibernate.
 * <p>
 * [^1]: You can create indexes on generated columns in MySQL 5.7.5+, which is functionally
 * equivalent, but the Buendia project won't be on 5.7.5+ at least until Debian is.
 * Current MySQL version for Debian: http://distrowatch.com/table.php?distribution=debian
 *
 */
public abstract class SyncParameters<T extends BaseOpenmrsData> {
    private int itemId;
    private Date dateUpdated;
    private T item;
    private String uuid;

    public int getItemId() {
        return itemId;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    public T getItem() {
        return item;
    }

    public String getUuid() {
        return uuid;
    }

    protected void setItemId(int itemId) {
        this.itemId = itemId;
    }

    protected void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    protected void setItem(T item) {
        this.item = item;
    }

    protected void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
