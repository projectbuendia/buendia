package org.projectbuendia.models;

import org.openmrs.projectbuendia.Utils;

import java.util.Locale;

import javax.annotation.Nonnull;

import static org.openmrs.projectbuendia.Utils.eq;

public class Quantity {
    public static final Quantity ZERO = new Quantity(0, Unit.UNSPECIFIED);

    public final double mag;
    public final @Nonnull Unit unit;

    public Quantity(double mag, Unit unit, Unit defaultUnit) {
        this(mag, Utils.orDefault(unit, defaultUnit));
    }

    public Quantity(double mag, Unit unit) {
        this.mag = mag;
        this.unit = unit == null ? Unit.UNSPECIFIED : unit;
    }

    public Quantity(double mag) {
        this(mag, Unit.UNSPECIFIED);
    }

    public boolean equals(Object other) {
        return other instanceof Quantity
            && eq(mag, ((Quantity) other).mag)
            && eq(unit, ((Quantity) other).unit);
    }

    public String toString() {
        return format(null, 6);
    }

    public String format(Locale locale, int maxPrec) {
        String abbr = unit.abbr.loc(locale);
        String suffix = abbr.startsWith("\b") ? abbr.substring(1) : " " + abbr;
        return Utils.format(mag, maxPrec) + suffix;
    }

    public String formatLong(Locale locale, int maxPrec) {
        String suffix = (mag == 1.0 ? unit.singular : unit.plural).loc(locale);
        return Utils.format(mag, maxPrec) + " " + suffix;
    }
}

