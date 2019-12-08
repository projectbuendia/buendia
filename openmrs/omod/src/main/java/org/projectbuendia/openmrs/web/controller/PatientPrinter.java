package org.projectbuendia.openmrs.web.controller;

import org.apache.velocity.util.ArrayListWrapper;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.webservices.rest.DbUtils;
import org.projectbuendia.models.Catalog;
import org.projectbuendia.models.Catalog.Drug;
import org.projectbuendia.models.Catalog.Format;
import org.projectbuendia.models.Catalog.Route;
import org.projectbuendia.models.CatalogIndex;
import org.projectbuendia.models.Instructions;
import org.projectbuendia.models.MsfCatalog;
import org.projectbuendia.models.Quantity;
import org.projectbuendia.models.Unit;
import org.projectbuendia.openmrs.web.controller.DataHelper.FormSection;
import org.projectbuendia.openmrs.web.controller.HtmlOutput.LocalizedWriter;
import org.projectbuendia.openmrs.web.controller.HtmlOutput.Sequence;
import org.projectbuendia.openmrs.web.controller.HtmlOutput.Writable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.openmrs.projectbuendia.Utils.eq;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.el;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.format;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.html;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.intl;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.seq;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.text;

class PatientPrinter {
    private final LocalizedWriter writer;
    private final DataHelper helper;
    private final CatalogIndex index = MsfCatalog.INDEX;

    private static final String IV_ACCESS_UUID = Utils.toUuid(2900012);
    private static final String OXYGEN_MASK_UUID = Utils.toUuid(2162738);
    private static final String FIRST_SAMPLE_TAKEN_UUID = Utils.toUuid(2900020);
    private static final String SECOND_SAMPLE_TAKEN_UUID = Utils.toUuid(2900021);
    private static final List<String> CONCEPTS_TO_IGNORE_NO = Arrays.asList(
        IV_ACCESS_UUID,
        OXYGEN_MASK_UUID,
        FIRST_SAMPLE_TAKEN_UUID,
        SECOND_SAMPLE_TAKEN_UUID
    );

    public PatientPrinter(PrintWriter writer, Locale locale, DataHelper helper) {
        this.writer = new LocalizedWriter(writer, locale);
        this.helper = helper;
    }

    public void printPreamble() throws IOException {
        write(html("<meta charset='UTF-8'>"));
        try {
            InputStream stream = new FileInputStream("/Users/ping/dev/buendia/openmrs/style.css");
            InputStreamReader reader = new InputStreamReader(stream);
            char[] buffer = new char[1024];
            write(html("<style>"));
            while (reader.ready()) {
                int count = reader.read(buffer);
                if (count < 0) break;
                writer.write(new String(buffer, 0, count));
            }
            write(html("</style>"));
        } catch (IOException e) {
            writer.write("<link rel='stylesheet' href='style.css'>");
        }
    }

    public void printIntro(Patient pat) throws IOException {
        renderIntro(pat).writeHtmlTo(writer);
    }

    public void printAdmission(Patient pat) throws IOException {
        renderAdmission(pat).writeHtmlTo(writer);
    }

    public void printEncounters(Patient pat) throws IOException {
        renderEncounters(pat).writeHtmlTo(writer);
    }

    public void printEvents(Patient pat) throws IOException {
        renderEvents(pat).writeHtmlTo(writer);
    }

    public Writable renderIntro(Patient pat) {
        return div("intro",
            el("h1 class='name'", pat.getPersonName().getFullName()),
            div("agesex", renderAge(pat), ", ", renderSex(pat))
        );
    }

    public Writable renderAge(Patient pat) {
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

    public Writable renderSex(Patient pat) {
        Sequence pregnancy = seq();
        if (helper.isPregnant(pat)) pregnancy.add(text(", "), intl("pregnant[fr:enceinte]"));
        return span("sex", pat.getGender(), pregnancy);
    }

    public Writable renderAdmission(Patient pat) {
        return div(
            "admission",
            el("h1", "Admission - Enregistrement CTE"),
            el("div class='patient-id'", "* Nr. Identification (ID) Patient:", div("field")),
            section(
                "Patient Information",
                columns(
                    column(
                        "50%",
                        line(field("* Nom de famille:", rest())),
                        line(field("* Prénom:", rest()))
                    ),
                    column(
                        "50%",
                        line("* Date de naissance:",
                            field("j", blanks(6)),
                            field("m", blanks(6)),
                            field("a", blanks(6))
                        ),
                        line(
                            field("si inconnu, Âge:", blanks(10)),
                            field("* Sexe:",
                                checkbox("Masculin"),
                                checkbox("Féminin")
                            )
                        )
                    )
                )
            ),
            section("Admission Information"),
            section("Resultat du Patient à la Sortie")
        );
    }

    public Writable renderEvents(Patient pat) {
        Sequence results = seq();
        List<DataHelper.Event> events = helper.mergeEvents(
            helper.getEvents(pat),
            Duration.standardMinutes(10),
            Duration.standardMinutes(20)
        );
        for (DataHelper.Event event : events) {
            Map<String, Obs> obsByQuestion = helper.getLatestObsByQuestion(event.obs);
            Sequence obsList = renderObsList(obsByQuestion);
            Sequence orderList = renderOrderList(event.orders);
            if (!obsList.isEmpty() || !orderList.isEmpty()) {
                results.add(div(
                    "event",
                    el("h2 class='time'", helper.formatTime(event.time)),
                    obsList.isEmpty() ? seq() : div("observations", obsList),
                    orderList.isEmpty() ? seq() : div("orders", orderList)
                ));
            }
        }
        return results;
    }

    public Writable renderEncounters(Patient pat) {
        Sequence encounters = seq();

        List<List<Obs>> groups = helper.getEncounterObs(helper.getEncounters(pat));
        for (List<Obs> group : groups) {
            if (group.isEmpty()) continue;
            DateTime start = helper.toLocalDateTime(group.get(0).getObsDatetime());
            Map<String, Obs> obsByQuestion = helper.getLatestObsByQuestion(group);
            Sequence obsList = renderObsList(obsByQuestion);
            if (!obsList.isEmpty()) {
                encounters.add(div(
                    "encounter",
                    el("h2 class='time'", helper.formatTime(start)),
                    div("observations", obsList)
                ));
            }
        }
        return div("encounters", encounters);
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
                        if (helper.isNo(obs)) noCount++;
                        children.add(obs);
                        done.add(uuid);
                    }
                }
                if (!children.isEmpty()) {
                    Writable title = intl(section.title);
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

    private Writable renderNoneSelected(Writable title, List<Obs> children) {
        return div("obs", span("label", title, ": "), " ", span("value", intl("None [fr:Aucun]")));
    }

    private Writable renderMultipleSelect(Writable title, List<Obs> children) {
        Sequence results = seq();
        boolean first = true;
        for (Obs obs : children) {
            if (helper.isYes(obs)) {
                if (!first) results.add(text(", "));
                results.add(coded(obs.getConcept()));
                first = false;
            }
        }
        return results.isEmpty() ? seq() : div("obs",
            span("label", title, ": "), " ", span("value", results)
        );
    }

    private Writable renderFormSection(Writable title, List<Obs> children) {
        Sequence results = seq();
        for (Obs obs : children) {
            results.add(renderObsContent(obs));
        }
        return results;
    }

    private Writable renderObsContent(Obs obs) {
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
                if (CONCEPTS_TO_IGNORE_NO.contains(DbUtils.getConceptUuid(obs))) return seq();
                return div("obs",
                    span("label", coded(obs.getConcept()), ":"),
                    " ",
                    span("value", renderValue(obs))
                );
        }
    }

    private Writable renderPlacement(DataHelper.Placement p) {
        Sequence result = seq(p.getLocationName());
        if (!p.getBed().isEmpty()) {
            result.add(seq(", ", intl("Bed [fr:Lit]"), " ", p.getBed()));
        }
        return result;
    }

    private Writable renderOrderAction(Order order) {
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

    private Writable renderOrderTreatment(Order order) {
        Instructions instr = new Instructions(order.getInstructions());
        Drug drug = index.getDrug(instr.code);
        Format format = index.getFormat(instr.code);
        Writable dosage = instr.isContinuous() ?
            span("dosage", format("%s in %s [fr:%s dans %s]",
                renderQuantity(instr.amount),
                renderQuantity(instr.duration)
            )) :
            span("dosage", renderQuantity(instr.amount));
        Route route = index.getRoute(instr.route);
        return span("treatment", format("%s, %s — %s %s",
            drug.name, format.description, dosage, route.name));
    }

    private Writable renderQuantity(Quantity quantity) {
        return span("quantity",
            span("mag", Utils.format(quantity.mag, 6)),
            " ",
            span("unit", quantity.unit.abbr)
        );
    }

    private Writable renderOrderSchedule(Order order) {
        DateTime start = helper.toLocalDateTime(order.getScheduledDate());
        DateTime stop = helper.toLocalDateTime(order.getAutoExpireDate());
        Instructions instr = new Instructions(order.getInstructions());
        int doses = 0;
        if (instr.isSeries() && eq(instr.frequency.unit, Unit.PER_DAY)) {
            int days = Days.daysBetween(start.toLocalDate(), stop.toLocalDate()).getDays();
            doses = days * (int) instr.frequency.mag;
        }
        return span("schedule", instr.isSeries() && instr.frequency.mag > 0 ?
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

    private Writable renderValue(Obs obs) {
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

    private Writable coded(Concept concept) {
        return intl(helper.getConceptName(concept));
    }

    private Writable line(Object... objects) {
        return el("div", objects);
    }

    private Writable span(String cls, Object... objects) {
        return el("span class='" + cls + "'", objects);
    }

    private Writable div(String cls, Object... objects) {
        return el("div class='" + cls + "'", objects);
    }

    private Writable section(String heading, Object... objects) {
        return div("section", el("heading", heading), objects);
    }

    private Writable columns(Object... columns) {
        return el("table cellspacing=0 cellpadding=0 class='columns'", el("tr", columns));
    }

    private Writable column(String width, Object... objects) {
        return el("td width='" + width + "'", objects);
    }

    private Writable blanks(int chars) {
        String spaces = "";
        for (int i = 0; i < chars; i++) spaces += "\u00a0";
        return field(text(spaces));
    }

    private Writable rest() {
        return el("span class='rest'", "\ua000");
    }

    private Writable checkbox(Object... objects) {
        return seq("[] ", objects);
    }

    private Writable field(Object label, Object... objects) {
        return el("span class='field'",
            el("span class='field-label'", label),
            el("span class='field-value'", objects));
    }

    private void write(Writable writable) throws IOException {
        writable.writeHtmlTo(writer);
    }
}
