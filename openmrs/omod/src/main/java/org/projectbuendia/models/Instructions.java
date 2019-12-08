package org.projectbuendia.models;

import org.openmrs.projectbuendia.Utils;

import java.io.Serializable;
import java.util.Locale;

import javax.annotation.Nonnull;

import static org.openmrs.projectbuendia.Utils.eq;

public class Instructions implements Serializable {
    // All String fields are empty if missing, but never null.
    public final @Nonnull String code;  // drug or format code (or free-text name)
    public final Quantity amount;  // amount of drug (mass or volume)
    public final Quantity duration;  // duration of administration, if continuously administered
    public final @Nonnull String route;  // route code
    public final Quantity frequency;  // frequency of repeats, if a series order
    public final @Nonnull String notes;

    // ASCII 30 is the "record separator" character; it renders as a space.
    public static final String RS = "\u001e";

    // ASCII 31 is the "unit separator" character; it renders as a space.
    public static final String US = "\u001f";

    public Instructions(String code, Quantity amount, Quantity duration, String route, Quantity frequency, String notes) {
        this.code = Utils.toNonnull(code);
        this.amount = amount;
        this.duration = duration;
        this.route = Utils.toNonnull(route);
        this.frequency = frequency;
        this.notes = Utils.toNonnull(notes);
    }

    public Instructions(String instructionsText) {
        // Instructions are serialized to a String consisting of records
        // separated by RS, and fields within those records separated by US.
        // The records and fields within them are as follows:
        //   - Record 0: code, route
        //   - Record 1: amount, amount unit, duration, duration unit
        //   - Record 2: frequency, frequency unit
        //   - Record 3: notes
        String[] records = Utils.splitFields(instructionsText, RS, 4);

        // Drug and route
        String[] fields = Utils.splitFields(records[0], US, 2);
        code = fields[0].trim();
        route = fields[1].trim();

        // Dosage
        fields = Utils.splitFields(records[1], US, 4);
        Quantity q = new Quantity(
            Utils.toDoubleOrDefault(fields[0], 0), Unit.get(fields[1]));
        amount = q.mag != 0 ? q : null;
        q = new Quantity(
            Utils.toDoubleOrDefault(fields[2], 0), Unit.get(fields[3]));
        duration = q.mag != 0 ? q : null;

        // Frequency
        fields = Utils.splitFields(records[2], US, 2);
        q = new Quantity(
            Utils.toDoubleOrDefault(fields[0], 0), Unit.get(fields[1]));
        frequency = q.mag != 0 ? q : null;

        // Notes
        fields = Utils.splitFields(records[3], US, 2);
        notes = fields[0].trim();
    }

    /** Packs all the fields into a single instruction string. */
    public String format() {
        return (code + US + route)
            + RS + (formatFields(amount) + US + formatFields(duration))
            + RS + (formatFields(frequency))
            + RS + (notes);
    }

    private String formatFields(Quantity q) {
        return q != null ? Utils.format(q.mag, 6) + US + q.unit : "";
    }

    public boolean isContinuous() {
        return duration != null;
    }

    public boolean isSeries() {
        return frequency != null;
    }

    public boolean equals(Object other) {
        if (other instanceof Instructions) {
            Instructions o = (Instructions) other;
            return eq(code, o.code)
                && eq(amount, o.amount)
                && eq(duration, o.duration)
                && eq(route, o.route)
                && eq(frequency, o.frequency)
                && eq(notes, o.notes);
        }
        return false;
    }

    public String getDrugName(Locale locale) {
        Catalog.Drug drug = MsfCatalog.INDEX.getDrug(code);
        return eq(drug, Catalog.Drug.UNSPECIFIED) ? code : drug.name.loc(locale);
    }
}