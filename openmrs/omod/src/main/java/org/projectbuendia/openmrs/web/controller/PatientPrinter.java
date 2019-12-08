package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
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
        Utils.toUuid(2900012),  // IV access
        Utils.toUuid(2162738),  // oxygen mask
        Utils.toUuid(2900020),  // first sample taken
        Utils.toUuid(2900021)  // second sample taken
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

    public void printEncounters(Patient pat) throws IOException {
        renderEncounters(pat).writeTo(writer, locale);
    }

    public void printEvents(Patient pat) throws IOException {
        renderEvents(pat).writeTo(writer, locale);
    }

    public Doc renderIntro(Patient pat) {
        return div("intro",
            el("h1 class='name'", pat.getPersonName().getFullName()),
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
        if (helper.isPregnant(pat)) pregnancy.add(text(", "), intl("pregnant[fr:enceinte]"));
        return span("sex", pat.getGender(), pregnancy);
    }

    public Doc renderAdmission(Patient pat) {
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

    public Doc renderEvents(Patient pat) {
        Sequence results = seq();
        List<DataHelper.Event> events = helper.getEvents(pat);
        events = helper.mergeEvents(events, Duration.standardMinutes(10), Duration.standardMinutes(20));
        events = helper.deduplicateObs(events, CONCEPTS_OMIT_DUPLICATE_VALUES);

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

    public Doc renderEncounters(Patient pat) {
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
        return span("treatment", format("%s, %s — %s %s",
            drug.name, format.description, dosage, route.name));
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

    private Doc coded(Concept concept) {
        return intl(helper.getConceptName(concept));
    }

    private Doc line(Object... objects) {
        return el("div", objects);
    }

    private Doc span(String cls, Object... objects) {
        return el("span class='" + cls + "'", objects);
    }

    private Doc div(String cls, Object... objects) {
        return el("div class='" + cls + "'", objects);
    }

    private Doc section(String heading, Object... objects) {
        return div("section", el("heading", heading), objects);
    }

    private Doc columns(Object... columns) {
        return el("table cellspacing=0 cellpadding=0 class='columns'", el("tr", columns));
    }

    private Doc column(String width, Object... objects) {
        return el("td width='" + width + "'", objects);
    }

    private Doc blanks(int chars) {
        String spaces = "";
        for (int i = 0; i < chars; i++) spaces += "\u00a0";
        return field(text(spaces));
    }

    private Doc rest() {
        return el("span class='rest'", "\ua000");
    }

    private Doc checkbox(Object... objects) {
        return seq("[] ", objects);
    }

    private Doc field(Object label, Object... objects) {
        return el("span class='field'",
            el("span class='field-label'", label),
            el("span class='field-value'", objects));
    }

    private void write(Doc doc) throws IOException {
        doc.writeTo(writer, locale);
    }
}
