package org.projectbuendia.models;

import java.util.HashMap;
import java.util.Map;

import static org.openmrs.projectbuendia.Utils.eq;

public class Unit {
    private static final Map<String, Unit> registry = new HashMap<>();

    public static final Unit UNSPECIFIED = new Unit("", " ", " ", " ", "");

    public static final Unit G = new Unit("G", "gram [fr:gramme]", "grams [fr:grammes]", "g");
    public static final Unit MG = new Unit("MG", "milligram [fr:milligramme]", "milligrams [fr:milligrammes]", "mg");
    public static final Unit MCG = new Unit("MCG", "microgram [fr:microgramme]", "micrograms [fr:microgrammes]", "µg");
    public static final Unit L = new Unit("L", "liter [fr:litre]", "liters [fr:litres]", "L");
    public static final Unit ML = new Unit("ML", "milliliter [fr:millilitre]", "milliliters [fr:millilitres]", "mL");
    public static final Unit IU = new Unit("IU", "IU [fr:UI]", "IU [fr:UI]", "IU");
    public static final Unit TABLET = new Unit("TABLET", "tablet [fr:comprimé]", "tablets [fr:comprimés]", "tab. [fr:comp.]");
    public static final Unit CAPSULE = new Unit("CAPSULE", "capsule", "capsules", "caps.");
    public static final Unit DROP = new Unit("DROP", "drop [fr:goutte]", "drops [fr:gouttes]", "drop [fr:goutte]");
    public static final Unit PUFF = new Unit("PUFF", "puff [fr:bouffée]", "puffs [fr:bouffées]", "puff [fr:bouffée]");
    public static final Unit AMPOULE = new Unit("AMPOULE", "ampoule", "ampoules", "amp.");
    public static final Unit SACHET = new Unit("SACHET", "sachet", "sachets", "sach.");
    public static final Unit OVULE = new Unit("OVULE", "ovule", "ovules", "ov.");
    public static final Unit SUPP = new Unit("SUPP", "suppository [fr:suppositoire]", "suppositories [fr:suppositoires]", "supp.");

    public static final Unit DAY = new Unit("DAY", "day [fr:jour]", "days [fr:jours]", "d [fr:j]", "d [fr:j]");
    public static final Unit HOUR = new Unit("HOUR", "hour [fr:heure]", "hours [fr:heures]", "hr", "h");
    public static final Unit MINUTE = new Unit("MINUTE", "minute", "minutes", "min");
    public static final Unit SECOND = new Unit("SECOND", "second", "seconds", "sec", "s");
    public static final Unit PER_DAY = new Unit("PER_DAY", "time per day [fr:fois par jour]",
        "times per day [fr:fois par jour]", "\bx per day [fr:fois par jour]", "\bx/day [fr:\bx/jour]");

    public final String code;  // identifier code, e.g. "SECOND"
    public final Intl singular;  // singular prose, e.g. "second"
    public final Intl plural;  // plural prose, e.g. "seconds"
    public final Intl terse;  // informal short form, e.g. "sec"
    public final Intl abbr;  // standard abbreviation, e.g. "s"

    public Unit(String code, String singular, String plural, String terse) {
        this(code, singular, plural, terse, terse);
    }

    public Unit(String code, String singular, String plural, String terse, String abbr) {
        this(code, new Intl(singular), new Intl(plural), new Intl(terse), new Intl(abbr));
    }

    public Unit(String code, Intl singular, Intl plural, Intl terse, Intl abbr) {
        this.code = code;
        this.singular = singular;
        this.plural = plural;
        this.terse = terse;
        this.abbr = abbr;
        if (!code.isEmpty()) registry.put(code, this);
    }

    @Override public boolean equals(Object other) {
        return other instanceof Unit && eq(code, ((Unit) other).code);
    }

    public String toString() {
        return code;
    }

    public Intl forCount(double count) {
        return count == 1 ? singular : plural;
    }

    public Intl forCount(int count) {
        return count == 1 ? singular : plural;
    }

    public static Unit get(String code) {
        Unit unit = registry.get(code);
        return unit != null ? unit : Unit.UNSPECIFIED;
    }
}
