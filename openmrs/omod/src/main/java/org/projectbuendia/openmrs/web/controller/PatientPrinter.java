package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.webservices.rest.DbUtils;
import org.projectbuendia.models.Catalog.Drug;
import org.projectbuendia.models.Catalog.Format;
import org.projectbuendia.models.Catalog.Route;
import org.projectbuendia.models.CatalogIndex;
import org.projectbuendia.models.Instructions;
import org.projectbuendia.models.MsfCatalog;
import org.projectbuendia.models.Quantity;
import org.projectbuendia.models.Unit;
import org.projectbuendia.openmrs.web.controller.DataHelper.FormSection;
import org.projectbuendia.openmrs.web.controller.HtmlOutput.Sequence;
import org.projectbuendia.openmrs.web.controller.HtmlOutput.Doc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.openmrs.projectbuendia.Utils.eq;
import static org.openmrs.projectbuendia.Utils.toUuid;
import static org.openmrs.projectbuendia.webservices.rest.DbUtils.isNo;
import static org.openmrs.projectbuendia.webservices.rest.DbUtils.isYes;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.el;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.format;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.intl;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.seq;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.text;

class PatientPrinter {
    private final Writer writer;
    private final Locale locale;
    private final DataHelper helper;
    private final CatalogIndex index = MsfCatalog.INDEX;

    private static final List<String> CONCEPTS_OMIT_NO = Arrays.asList(
        toUuid(2900012),  // IV access
        toUuid(2162738),  // oxygen mask
        toUuid(2900020),  // first sample taken
        toUuid(2900021)  // second sample taken
    );

    private static final List<String> CONCEPTS_OMIT_DUPLICATE_VALUES = Arrays.asList(
        ConceptUuids.PLACEMENT_UUID
    );

    public PatientPrinter(Writer writer, Locale locale, DataHelper helper) {
        this.writer = writer;
        this.locale = locale;
        this.helper = helper;
    }

    public void printPreamble() throws IOException {
        writer.write("<meta charset='UTF-8'>");
        try {
            InputStream stream = new FileInputStream("/Users/ping/dev/buendia/openmrs/style.css");
            InputStreamReader reader = new InputStreamReader(stream);
            char[] buffer = new char[1024];
            writer.write("<style>");
            while (reader.ready()) {
                int count = reader.read(buffer);
                if (count < 0) break;
                writer.write(new String(buffer, 0, count));
            }
            writer.write("</style>");
        } catch (IOException e) {
            writer.write("<link rel='stylesheet' href='style.css'>");
        }
    }

    public void printIntro(Patient pat) throws IOException {
        renderIntro(pat).writeTo(writer, locale);
    }

    public void printAdmission(Patient pat) throws IOException {
        renderAdmission(pat).writeTo(writer, locale);
    }

    public void printEvents(Patient pat) throws IOException {
        renderEvents(pat).writeTo(writer, locale);
    }

    public Doc renderIntro(Patient pat) {
        return div("intro",
            el("heading class='name'", pat.getPersonName().getFullName()),
            div("agesex", renderAge(pat), ", ", renderSex(pat))
        );
    }

    public Doc renderAge(Patient pat) {
        Period age = new Period(
            helper.toLocalDateTime(pat.getBirthdate()),
            helper.toLocalDateTime(pat.getDateCreated()));
        int years = age.getYears();
        int months = age.getMonths();
        return span("age", format(
            months > 0 ? "%d y, %d mo [fr:%d a, %d mo]" : "%d y [fr:%d a]",
            years, months
        ));
    }

    public Doc renderSex(Patient pat) {
        Sequence pregnancy = seq();
        if (helper.isPregnant(pat)) pregnancy.add(text(", "), intl("pregnant [fr:enceinte]"));
        return span("sex", pat.getGender(), pregnancy);
    }

    public Doc yesNo() {
        return yesNo(null);
    }

    public Doc yesNo(Obs obs) {
        return seq(checkbox("Oui", isYes(obs)), checkbox("Non", isNo(obs)));
    }

    public Doc yesNoUnknown(Obs obs) {
        return seq(checkbox("Oui", isYes(obs)), checkbox("Non", isNo(obs)),
            checkbox("Inconnu", !isYes(obs) && !isNo(obs)));
    }

    public static String TELEPHONE_UUID = toUuid(3159635);
    public static String PHONE_OWNER_UUID = toUuid(3900001);
    public static String AIRE_SANTE_UUID = toUuid(3900004);
    public static String VILLAGE_UUID = toUuid(3001354);
    public static String ADMISSION_TIME_UUID = toUuid(8000000);
    public static String SYMPTOM_START_UUID = toUuid(6001730);
    public static String PREGNANCY_UUID = toUuid(2005272);
    public static String PREGNANCY_TEST_UUID = toUuid(2000045);

    public static String HYPERTENSION_UUID = toUuid(2117399);
    public static String DIABETES_UUID = toUuid(2119481);
    public static String CHRONIC_LUNG_UUID = toUuid(2155569);
    public static String CHRONIC_HEART_UUID = toUuid(2145349);
    public static String HIV_UUID = toUuid(0);
    public static String TB_UUID = toUuid(0);
    public static String RENAL_DISEASE_UUID = toUuid(0);

    public static String WEIGHT_KG_UUID = toUuid(1005089);
    public static String DISCHARGE_DATE_UUID = toUuid(6001641);

    public Doc renderAdmission(Patient pat) {
        return div("admission",
            columns(
                column("50%",
                    el("heading", "Admission - Enregistrement CTE")
                ),
                column("50%",
                    el("div class='patient-id'", field("* Nr. Identification (ID) Patient", blank(4)))
                )
            ),
            section("patient-info",
                "Patient Information",
                columns(
                    column("50%",
                        block("name",
                            line(field("* Nom de famille", blank(6, pat.getFamilyName()))),
                            line(field("* Prénom", blank(7, pat.getGivenName())))
                        ),
                        block("contact",
                            subhead("* Information contact Patient:"),
                            line(field("Numéro de téléphone Patient", blank(4, getTextValue(pat, TELEPHONE_UUID)))),
                            line(field("Propriétaire du téléphone", blank(4, getTextValue(pat, PHONE_OWNER_UUID))))
                        )
                    ),
                    column("50%",
                        block("agesex",
                            line(field("* Date de naissance", renderEmptyDate())),
                            line(
                                field("si inconnu, âge", blank(1, renderAge(pat))),
                                field("* Sexe",
                                    checkbox("Masculin", eq(pat.getGender().toUpperCase(), "M")),
                                    checkbox("Féminin", eq(pat.getGender().toUpperCase(), "F"))
                                )
                            )
                        ),
                        block("additional-contact",
                            subhead("* Information contact additionnelle (famille/amis):"),
                            line(field("Numéro de téléphone additionnel", blank(4))),
                            line(field("Propriétaire du téléphone", blank(4)))
                        )
                    )
                ),
                block("residence",
                    subhead("* Lieu de résidence:"),
                    line(
                        field("Aire de Santé", blank(7, getTextValue(pat, AIRE_SANTE_UUID))),
                        field("Village", blank(7, getTextValue(pat, VILLAGE_UUID)))
                    )
                )
            ),
            section("admission-info",
                "Admission Information",
                columns(
                    column("50%",
                        block("dates",
                            line(field("* Date d'admission", renderDate(getDateTimeValue(pat, ADMISSION_TIME_UUID)))),
                            line(field("Heure d'admission", renderTime(getDateTimeValue(pat, ADMISSION_TIME_UUID)))),
                            line(field("* Date de début des symptômes", renderDate(getDateValue(pat, SYMPTOM_START_UUID))))
                        )
                    ),
                    column("50%",
                        block("status",
                            line("* État du Patient à l'admission:"),
                            line(hspace(2), checkbox("Suspect"), checkbox("Probable"), checkbox("Confirmé"))
                        )
                    )
                ),
                block("pregnancy",
                    columns(
                        column("33%",
                            subhead("Si la patiente est âge e procréer:"),
                            line("* Enceinte actuellement:",
                                stack(yesNoUnknown(getObs(pat, PREGNANCY_UUID))),
                                line("* Allaitante:", yesNo())
                            )
                        ),
                        column("33%",
                            vspace(),
                            line("* Test de grossesse:",
                                stack(yesNo(getObs(pat, PREGNANCY_TEST_UUID)))
                            ),
                            line(field("* Date", renderDate(getObs(pat, PREGNANCY_TEST_UUID))))
                        ),
                        column("33%",
                            line(field("Nr de semaines de gestation estimé", blank(1))),
                            line(field("Trimestre de grossesse", blank(3))),
                            line(field("Fetus vivant", yesNo()))
                        )
                    )
                ),
                columns(
                    column("66%",
                        block("comorbidities",
                            subhead(field("Comorbidités"), field("Traitement")),
                            line(checkbox("Hypertension", isYes(getObs(pat, HYPERTENSION_UUID)))),
                            line(checkbox("VIH/SIDA", isYes(getObs(pat, HIV_UUID)))),
                            line(checkbox("TB", isYes(getObs(pat, TB_UUID)))),
                            line(checkbox("Maladie pulmonaire chronique", isYes(getObs(pat, CHRONIC_LUNG_UUID)))),
                            line(checkbox("Maladie cardiaque chronique", isYes(getObs(pat, CHRONIC_HEART_UUID)))),
                            line(checkbox("Maladie rénale", isYes(getObs(pat, RENAL_DISEASE_UUID)))),
                            line(checkbox("Diabètes", isYes(getObs(pat, DIABETES_UUID)))),
                            line(checkbox("Autre"))
                        )
                    ),
                    column("33%",
                        block("physical",
                            line(field("Allergies", checkbox("Oui"), checkbox("Non"))),
                            line(field("Si oui, spécifiez", blank(3))),
                            line(field("", blank(5))),
                            vspace(),
                            line(field("Poids", renderNumber(getNumericValue(pat, WEIGHT_KG_UUID))), "kg"),
                            vspace(),
                            line(field("Vacciné avec rVSV ZEBOV",
                                checkbox("Oui"),
                                checkbox("Non")
                            )),
                            line(field("Si oui, date", renderEmptyDate()))
                        )
                    )
                )
            ),
            section("discharge",
                "Resultat du Patient à la Sortie",
                block("result",
                    line(
                        field("* Statut final", checkbox("Confirmé"), checkbox("Pas un cas"), checkbox("Inconnu")),
                        hspace(),
                        field("* Date de sortie", renderDate(getDateValue(pat, DISCHARGE_DATE_UUID)))
                    ),
                    line(field("* Résultat final",
                        checkbox("Sortie en revalidation"),
                        checkbox("Décédé"),
                        checkbox("Transferé"),
                        checkbox(field("Autre, spécifiez", blank(6)))
                    )),
                    line(
                        field("Si sortie, session d'information donnée", checkbox("Oui"), checkbox("Non")),
                        field("Kit d'hygiène donnée", checkbox("Oui"), checkbox("Non"))
                    ),
                    line("Si transferé, lieu", blank(10))
                ),
                block("experimental",
                    subhead("Traitement expérimental"),
                    columns(
                        column("50%",
                            line(field("Patient a reçu un traitement expérimental ?", checkbox("Oui"), checkbox("Non"))),
                            line(field("Combien de dose", blank(5)))
                        ),
                        column("50%",
                            line(field("Lequel ?", blank(5))),
                            line(field("Date de la dernière dose", renderEmptyDate()))
                        )
                    )
                ),
                block("pregnancy",
                    subhead("Si grossesse:"),
                    columns(
                        column("33%",
                            line(field("Grossesse préservée", yesNo())),
                            line(field("Fausse couche", yesNo()))
                        ),
                        column("33%",
                            line(field("Accouchement", yesNo())),
                            line(field("Interruption de grossesse", yesNo()))
                        ),
                        column("33%",
                            line(field("Nouveau-né vivant", yesNo()))
                        )
                    )
                ),
                block("followup",
                    columns(
                        column("40%",
                            line(field("Visite de suivi prévue ?", yesNo())),
                            line(field("Date", renderEmptyDate()))
                        ),
                        column("20%",
                            line("Si Oui pourquoi?")
                        ),
                        column("40%",
                            line(
                                checkbox("Symptômes persistants"),
                                checkbox("traitement expérimental")
                            ),
                            line(
                                checkbox("Grossesse"), hspace(2),
                                checkbox(field("Autre, précise", blank(2)))
                            )
                        )
                    )
                )
            )
        );
    }

    public String getTextValue(Patient pat, String uuid) {
        Obs obs = getObs(pat, uuid);
        return obs != null ? obs.getValueText() : "";
    }

    public Double getNumericValue(Patient pat, String uuid) {
        Obs obs = getObs(pat, uuid);
        return obs != null ? obs.getValueNumeric() : null;
    }

    public LocalDate getDateValue(Patient pat, String uuid) {
        Obs obs = getObs(pat, uuid);
        return obs != null ? helper.toLocalDateTime(obs.getValueDate()).toLocalDate() : null;
    }

    public DateTime getDateTimeValue(Patient pat, String uuid) {
        Obs obs = getObs(pat, uuid);
        return obs != null ? helper.toLocalDateTime(obs.getValueDate()) : null;
    }

    public Obs getObs(Patient pat, String uuid) {
        return null;
    }

    public Doc renderEvents(Patient pat) {
        Sequence results = seq();
        List<DataHelper.Event> events = helper.getEvents(pat);
        events = helper.mergeEvents(events, Duration.standardMinutes(10), Duration.standardMinutes(20));
        events = helper.deduplicateObs(events, CONCEPTS_OMIT_DUPLICATE_VALUES);

        for (DataHelper.Event event : events) {
            Map<String, Obs> obsByQuestion = helper.getLatestObsByQuestion(event.obs);
            Sequence obsList = renderObsList(obsByQuestion);
            Sequence orderList = renderOrderList(event.orders);
            Sequence execList = renderExecList(event.execs);
            if (!obsList.isEmpty() || !orderList.isEmpty() || !execList.isEmpty()) {
                results.add(div(
                    "event",
                    el("h2 class='time'", helper.formatTime(event.time)),
                    obsList.isEmpty() ? seq() : div("observations", obsList),
                    orderList.isEmpty() ? seq() : div("orders",
                        span("label", el("h3", intl("Treatments ordered [fr:Traitements commandés]"))), orderList),
                    execList.isEmpty() ? seq() : div("executions",
                        span("label", el("h3", intl("Treatments given [fr:Traitements donnés]"))), execList)
                ));
            }
        }
        return div("events", results);
    }

    private Sequence renderObsList(Map<String, Obs> obsMap) {
        Sequence results = new Sequence();
        Set<String> done = new HashSet<>();
        boolean admissionFormSubmitted = obsMap.containsKey(
            ConceptUuids.ADMISSION_DATETIME_UUID);
        if (admissionFormSubmitted) {
            results.add(el("h3", "Admission"));
        }
        for (Form form : helper.getForms()) {
            Sequence items = new Sequence();
            if (!admissionFormSubmitted && isAdmissionForm(form)) continue;
            for (FormSection section : helper.getFormSections(form)) {
                List<Obs> children = new ArrayList<>();
                int noCount = 0;
                for (FormField option : section.fields) {
                    String uuid = DbUtils.getConceptUuid(option);
                    if (uuid != null && obsMap.containsKey(uuid) && !done.contains(uuid)) {
                        Obs obs = obsMap.get(uuid);
                        if (isNo(obs)) noCount++;
                        children.add(obs);
                        done.add(uuid);
                    }
                }
                if (!children.isEmpty()) {
                    Doc title = intl(section.title);
                    if (section.title.contains("[binary]")) {
                        if (noCount == section.fields.size()) {
                            items.add(renderNoneSelected(title, children));
                        } else {
                            items.add(renderMultipleSelect(title, children));
                        }
                    } else {
                        items.add(renderFormSection(title, children));
                    }
                }
            }
            if (!items.isEmpty()) {
                results.add(div("form", items));
            }
        }
        Sequence extras = new Sequence();
        for (String uuid : obsMap.keySet()) {
            if (!done.contains(uuid)) {
                Obs obs = obsMap.get(uuid);
                extras.add(renderObsContent(obs));
            }
        }
        if (!extras.isEmpty()) {
            results.add(div("form extras", extras));
        }
        return results;
    }

    private Sequence renderExecList(List<Obs> execs) {
        Sequence items = seq();
        for (Obs exec : execs) {
            items.add(renderObsContent(exec));
        }
        return items;
    }

    private Sequence renderOrderList(List<Order> orders) {
        Sequence results = seq();
        for (Order order : orders) {
            results.add(
                div("order",
                    span("label", intl("Treatment ordered [fr:Traitement commandé]"), ": "),
                    renderOrderAction(order),
                    renderOrderTreatment(order),
                    renderOrderSchedule(order)
                )
            );
        }
        return results;
    }

    private boolean isAdmissionForm(Form form) {
        return DbUtils.getName(form).toLowerCase().contains("admiss");
    }

    private Doc renderNoneSelected(Doc title, List<Obs> children) {
        return div("obs", span("label", title, ": "), " ", span("value", intl("None [fr:Aucun]")));
    }

    private Doc renderMultipleSelect(Doc title, List<Obs> children) {
        Sequence results = seq();
        boolean first = true;
        for (Obs obs : children) {
            if (isYes(obs)) {
                if (!first) results.add(text(", "));
                results.add(coded(obs.getConcept()));
                first = false;
            }
        }
        return results.isEmpty() ? seq() : div("obs",
            span("label", title, ": "), " ", span("value", results)
        );
    }

    private Doc renderFormSection(Doc title, List<Obs> children) {
        Sequence results = seq();
        for (Obs obs : children) {
            results.add(renderObsContent(obs));
        }
        return results;
    }

    private Doc renderObsContent(Obs obs) {
        switch (DbUtils.getConceptUuid(obs)) {
            case DbUtils.CONCEPT_PLACEMENT_UUID:
                DataHelper.Placement p = helper.getPlacement(obs);
                return div("obs placement",
                    span("label", intl("New placement [fr:Nouveau emplacement]"), ": "),
                    span("value", renderPlacement(p))
                );

            case DbUtils.CONCEPT_ORDER_EXECUTED_UUID:
                Order order = obs.getOrder();
                return div("execution",
                    span("label", intl("Treatment given [fr:Traitement donné]"), ": "),
                    span("value", renderOrderTreatment(order))
                );

            default:
                if (CONCEPTS_OMIT_NO.contains(DbUtils.getConceptUuid(obs))) return seq();
                return div("obs " + getConceptCssClass(obs.getConcept()),
                    span("label", coded(obs.getConcept()), ":"),
                    " ",
                    span("value", renderValue(obs))
                );
        }
    }

    private String getConceptCssClass(Concept concept) {
        if (concept == null) return "";
        String uuid = concept.getUuid();
        Object comp = Utils.compressUuid(uuid);
        if (comp instanceof Integer) {
            return "c-" + (((int) comp) % 1000000);
        } else {
            return "c-" + comp;
        }
    }

    private Doc renderPlacement(DataHelper.Placement p) {
        Sequence result = seq(p.getLocationName());
        if (!p.getBed().isEmpty()) {
            result.add(seq(", ", intl("Bed [fr:Lit]"), " ", p.getBed()));
        }
        return result;
    }

    private Doc renderOrderAction(Order order) {
        String prev = order.getPreviousOrder() != null ?
            order.getPreviousOrder().getInstructions() : "<null>";
        switch (order.getAction()) {
            case NEW:
                return span("action", "[new]");
            case RENEW:
                return span("action", "[renew ", prev, "]");
            case DISCONTINUE:
                return span("action", "[discontinue ", prev, "]");
            case REVISE:
                return span("action", "[revise ", prev, "]");
        }
        return seq();
    }

    private Doc renderOrderTreatment(Order order) {
        Instructions instr = new Instructions(order.getInstructions());
        Drug drug = index.getDrug(instr.code);
        Format format = index.getFormat(instr.code);
        Doc dosage = instr.isContinuous() ?
            span("dosage", format("%s in %s [fr:%s dans %s]",
                renderQuantity(instr.amount),
                renderQuantity(instr.duration)
            )) :
            span("dosage", renderQuantity(instr.amount));
        Route route = index.getRoute(instr.route);
        return div("treatment",
            div("drug", span("label", intl("Drug [fr:Méd.]")), ": ", drug.name),
            div("format", span("label", intl("Format")), ": ", format.description),
            div("dosageroute", span("label", intl("Dosage")), ": ", dosage, span("route", route.name)),
            div("notes", span("label", intl("Notes [fr:Remarques]")), ": ", instr.notes)
        );
    }

    private Doc renderQuantity(Quantity quantity) {
        return span("quantity",
            span("mag", Utils.format(quantity.mag, 6)),
            " ",
            span("unit", quantity.unit.abbr)
        );
    }

    private Doc renderOrderSchedule(Order order) {
        DateTime start = helper.toLocalDateTime(order.getScheduledDate());
        DateTime stop = helper.toLocalDateTime(order.getAutoExpireDate());
        Instructions instr = new Instructions(order.getInstructions());
        int doses = 0;
        if (instr.isSeries() && eq(instr.frequency.unit, Unit.PER_DAY)) {
            int days = Days.daysBetween(start.toLocalDate(), stop.toLocalDate()).getDays();
            doses = days * (int) instr.frequency.mag;
        }
        return div("schedule", span("label", intl("Schedule [fr:Horaire]")), ": ", instr.isSeries() && instr.frequency.mag > 0 ?
            (stop != null ?
                (doses > 0 ?
                    seq(renderQuantity(instr.frequency), ", ",
                        format("starting %s, stopping %s after %d doses [fr:commencer %s, arreter %s après %d doses]",
                            helper.formatTime(start), helper.formatTime(stop), doses)) :
                    seq(renderQuantity(instr.frequency), ", ",
                        format("starting %s, stopping %s [fr:commencer %s, arreter %s]",
                            helper.formatTime(start), helper.formatTime(stop)))
                ) :
                seq(renderQuantity(instr.frequency), ", ",
                    format("starting %s, continuing indefinitely [fr:commencer %s, continuer indéfiniment]"))
            ) :
            format("one dose only, ordered %s [fr:dose unique, commandé %s]",
                helper.formatTime(start))
        );
    }

    private Doc renderValue(Obs obs) {
        ConceptDatatype type = obs.getConcept().getDatatype();
        switch (type.getHl7Abbreviation()) {
            case ConceptDatatype.BOOLEAN:
                return text(Boolean.toString(obs.getValueBoolean()));
            case ConceptDatatype.CODED:
                return coded(obs.getValueCoded());
            case ConceptDatatype.NUMERIC:
                return text(Utils.format(obs.getValueNumeric(), 6));
            case ConceptDatatype.TEXT:
                return text(obs.getValueText().replace("\n", "; "));
            case ConceptDatatype.DATE:
                return text(helper.formatDate(helper.toLocalDateTime(obs.getValueDate())));
            case ConceptDatatype.DATETIME:
                return text(helper.formatTime(helper.toLocalDateTime(obs.getValueDatetime())));
        }
        return text("?");
    }

    private Doc renderNumber(Double value) {
        if (value == null) return seq();
        return text(Utils.format(value, 1));
    }

    private Doc renderDate(DateTime value) {
        if (value == null) return renderEmptyDate();
        return renderDate(value.toLocalDate());
    }

    private Doc renderDate(LocalDate value) {
        if (value == null) return renderEmptyDate();
        return renderYearMonthDay(value.getYear(), value.getMonthOfYear(), value.getDayOfMonth());
    }

    private Doc renderDate(Obs obs) {
        if (obs == null) return renderEmptyDate();
        return renderDate(helper.toLocalDateTime(obs.getObsDatetime()));
    }

    private Doc renderEmptyDate() {
        return renderYearMonthDay("", "", "");
    }

    private Doc renderYearMonthDay(Object year, Object month, Object day) {
        return seq("j", blank(1, day), "/ m", blank(1, month), "/ a", blank(2, year));
    }

    private Doc renderTime(DateTime value) {
        if (value == null) return renderHoursMinutes("", "");
        return renderHoursMinutes(value.getHourOfDay(), value.getMinuteOfHour());
    }

    private Doc renderHoursMinutes(Object hours, Object minutes) {
        return seq(blank(2, hours), " : ", blank(2, minutes));
    }

    private Doc coded(Concept concept) {
        return intl(helper.getConceptName(concept));
    }

    private Doc line(Object... objects) {
        return div("line", objects);
    }

    private Doc block(String cls, Object... objects) {
        return div("block " + cls, objects);
    }

    private Doc subhead(Object... objects) {
        return div("subhead", objects);
    }

    private Doc stack(Object... objects) {
        Sequence rows = seq();
        for (Object obj : objects) {
            rows.add(div("stack", obj));
        }
        return div("stack", rows);
    }

    private Doc span(String cls, Object... objects) {
        return el("span class='" + cls + "'", objects);
    }

    private Doc div(String cls, Object... objects) {
        return el("div class='" + cls + "'", objects);
    }

    private Doc section(String cls, String heading, Object... objects) {
        return div("section " + cls, el("heading", heading), objects);
    }

    private Doc columns(Object... columns) {
        return el("table cellspacing=0 cellpadding=0 class='columns'", el("tr", columns));
    }

    private Doc column(String width, Object... objects) {
        return el("td width='" + width + "'", objects);
    }

    private Doc blank(int size, Object... contents) {
        Sequence spaces = seq();
        for (int i = 0; i < size; i++) {
            spaces.add(span("unit"));
        }
        return span("blank size=" + size, span("spaces", spaces), span("contents", contents));
    }

    private Doc vspace() {
        return div("vspace");
    }

    private Doc hspace(int size) {
        Sequence spaces = seq();
        for (int i = 0; i < size; i++) {
            spaces.add(span("unit"));
        }
        return span("hspace size=" + size, spaces);
    }

    private Doc hspace() {
        return span("hspace", span("unit"));
    }

    private Doc checkbox(Object label) {
        return checkbox(label, false);
    }

    private Doc checkbox(Object label, boolean state) {
        String checked = state ? "checked" : "";
        return seq(span("checkbox " + checked, el("input type='checkbox' " + checked)), label);
    }

    private Doc field(String label, Object... objects) {
        if (!label.matches("[.!?:]$")) label += " : ";
        return el("span class='field'",
            el("span class='label'", label),
            el("span class='value'", objects));
    }
}
