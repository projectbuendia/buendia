package org.openmrs.projectbuendia;

import org.openmrs.Concept;

import java.util.Date;

public interface ObsValueVisitor<T> {
    /** Visits a coded (Concept) value. */
    T visitCoded(Concept value);

    /** Visits a numeric value. */
    T visitNumeric(Double value);

    /** Visits a boolean value. */
    T visitBoolean(Boolean value);

    /** Visits a text value. */
    T visitText(String value);

    /** Visits a date value. */
    T visitDate(Date value);

    /** Visits a datetime value. */
    T visitDatetime(Date value);
}
