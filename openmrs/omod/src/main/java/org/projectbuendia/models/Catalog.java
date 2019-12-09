package org.projectbuendia.models;

import org.openmrs.projectbuendia.Utils;

import static org.openmrs.projectbuendia.Utils.eq;

/**
 * A namespace for all the data types that represent a catalog of medications.
 * At the highest level of organization are categories, each of which contains
 * a list of drugs, each of which is associated with a list of formats.
 *
 * A Category is a group of drugs that are administered in the same general
 * fashion (e.g. orally, by injection, by infusion, or externally); thus the
 * category determines the set of possible administration routes (e.g. for the
 * injectable category, the possible routes are IV, SC, and IM).  The category
 * also determines the form in which a dosage is specified (e.g. as a single
 * amount given at once, or as an amount of liquid given continuously over a
 * period of time), which we call a DosingType.
 *
 * A Drug is a substance, or what is sometimes called an "active ingredient"
 * (e.g. aspirin is a drug).  A combination of substances is also considered
 * a drug (e.g. artemether-lumefantrine is a drug).  Each drug may have zero
 * or more captions, which are hints to the user about the therapeutic function
 * of the drug (e.g. "analgesic") to help reduce the likelihood of error.  When
 * the same substance exists in multiple categories (e.g. amoxicillin can be
 * taken orally or injected), there is a separate Drug object in each category.
 *
 * A Format is a formulation of a drug, i.e. the manner in which the substance
 * is prepared for administration.  A format typically specifies the size and
 * mechanism of a unit of delivery (e.g. 300 mg tablets, drops, puffs), or a
 * concentration of the substance in solution (e.g. glucose 5%).  The format
 * determines the dosage unit, i.e. the unit in which doses are prescribed
 * (e.g. number of tablets, milligrams of liquid).
 *
 * Thus, the data structure hierarchy is as follows:
 *
 *         Category (dosingType, routes)
 *            -->* Drug (captions)
 *                  -->* Format (unit)
 *
 * Every Category, Drug, Format, Route, and Unit has a "code", a short, unique,
 * language-independent string identifier, which is used for serialization.
 *
 * In the MSF scheme, each Category has a four-letter code that is a prefix of
 * the Drug code, and each Drug has an eight-letter code that is a prefix of
 * the Format code.  However, these prefix relationships are not used by any
 * application logic.
 *
 * Note that, currently, we use stock codes as Format codes, which conflates
 * the formulation of drugs with the ordering of drug supplies.  This design
 * issue is as yet unresolved.
 */
public interface Catalog {
    enum DosingType {QUANTITY, QUANTITY_OVER_DURATION};

    class Category {
        public static final Category UNSPECIFIED = new Category("", "", false);

        public final String code;  // category identifier, e.g. "DORA", "DINJ", "DEXT"
        public final Intl name;  // drug category, e.g. "oral", "injectable", "external"
        public final boolean isContinuous;  // dosed continuously, in quantity over time
        public final Route[] routes;  // routes of administration
        public final Drug[] drugs;  // drugs in this category

        public Category(String code, Intl name, boolean isContinuous, Route[] routes, Drug[] drugs) {
            this.code = code;
            this.name = name;
            this.isContinuous = isContinuous;
            this.routes = routes;
            this.drugs = drugs;
        }

        public Category(String code, String name, boolean isContinuous, Route[] routes, Drug[] drugs) {
            this(code, new Intl(name), isContinuous, routes, drugs);
        }

        public Category(String code, String name, boolean isContinuous, Route... routes) {
            this(code, name, isContinuous, routes, new Drug[0]);
        }

        public Category withDrugs(Drug... drugs) {
            return new Category(code, name, isContinuous, routes, drugs);
        }

        @Override public boolean equals(Object other) {
            return other instanceof Category && eq(code, ((Category) other).code);
        }
    }

    class Drug {
        public static final Drug UNSPECIFIED = new Drug("", " ").withFormats(Format.UNSPECIFIED);

        public final String code;  // drug identifier, e.g. "DORAACSA"
        public final Intl name;  // active ingredient, title case, e.g. "Acetylsalicylic Acid"
        public final Intl[] aliases;  // alternative names, title case, e.g. {"Aspirin", "ASA"}
        public final Intl[] captions;  // therapeutic action, lowercase noun, e.g. {"analgesic", "antipyretic"}
        public final Format[] formats;

        public Drug(String code, Intl name, Intl[] aliases, Intl[] captions, Format[] formats) {
            this.code = code;
            this.name = name;
            this.aliases = aliases;
            this.captions = Utils.orDefault(captions, new Intl[0]);
            this.formats = Utils.orDefault(formats, new Format[0]);
        }

        public Drug(String code, String name, String... aliases) {
            this(code, new Intl(name), Intl.newArray(aliases), null, null);
        }

        public Drug withCaptions(Intl... captions) {
            return new Drug(code, name, aliases, captions, formats);
        }

        public Drug withFormats(Format... formats) {
            return new Drug(code, name, aliases, captions, formats);
        }

        @Override public boolean equals(Object other) {
            return other instanceof Drug && eq(code, ((Drug) other).code);
        }
    }

    class Format {
        public static final Format UNSPECIFIED = new Format("", " ", Unit.UNSPECIFIED);

        public final String code;  // stock code, e.g. "DORAACSA3TD"
        public final Intl description;  // amount, concentration, form, e.g. "300 mg, disp. tab."
        public final Unit dosageUnit;  // null means this format takes no dosage specification

        public Format(String code, String description, Unit dosageUnit) {
            this(code, new Intl(description), dosageUnit);
        }

        public Format(String code, Intl description, Unit dosageUnit) {
            this.code = code;
            this.description = description;
            this.dosageUnit = dosageUnit;
        }

        @Override public boolean equals(Object other) {
            return other instanceof Format && eq(code, ((Format) other).code);
        }

        @Override public String toString() {
            return description.base;
        }
    }

    class Route {
        public static final Route UNSPECIFIED = new Route("", " ", "");

        public final String code;  // identifier code, e.g. "IV"
        public final Intl name;  // route of administration, e.g. "intravenous"
        public final Intl abbr;  // abbreviation, e.g. "IV"

        public Route(String code, String name, String abbr) {
            this.code = code;
            this.name = new Intl(name);
            this.abbr = new Intl(abbr);
        }

        @Override public boolean equals(Object other) {
            return other instanceof Route && eq(code, ((Route) other).code);
        }

        @Override public String toString() {
            return abbr.base;
        }
    }

}
