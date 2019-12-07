package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.projectbuendia.Intl;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.webservices.rest.DbUtils;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.projectbuendia.openmrs.web.controller.HtmlOutput.el;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.format;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.html;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.intl;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.seq;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.text;

class PatientPrinter {
    private final LocalizedWriter writer;
    private final DataHelper helper;

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

    public void printAdmission(Patient pat) throws IOException {
        write(
            el(
                "div class='admission'",
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
            )
        );
    }

    public void printEncounters(Patient pat) throws IOException {
        Sequence encounters = seq();

        List<List<Obs>> groups = helper.getEncounterObs(helper.getEncounters(pat));
        for (List<Obs> group : groups) {
            if (group.isEmpty()) continue;
            DateTime start = helper.toLocalDateTime(group.get(0).getObsDatetime());
            Map<String, Obs> obsByQuestion = getLatestObsByQuestion(group);
            Sequence obsList = renderObsList(obsByQuestion);
            if (!obsList.isEmpty()) {
                encounters.add(div(
                    "encounter",
                    div("time", helper.formatTime(start)),
                    div("observations", obsList)
                ));
            }
        }
        write(div("patient",
            div("name", pat.getPersonName().getFullName()),
            div("encounters", encounters)
        ));
    }

    private Map<String, Obs> getLatestObsByQuestion(List<Obs> group) {
        Map<String, Obs> obsByQuestion = new HashMap<>();
        for (Obs obs : group) {
            String key = obs.getConcept().getUuid();
            Obs previous = obsByQuestion.get(key);
            if (previous == null || helper.inOrder(previous.getObsDatetime(), obs.getObsDatetime())) {
                obsByQuestion.put(key, obs);
            }
        }
        return obsByQuestion;
    }

    private Sequence renderObsList(Map<String, Obs> obsMap) {
        Sequence results = new Sequence();
        Set<String> done = new HashSet<>();
        for (Form form : helper.getForms()) {
            Sequence items = new Sequence();
            if (!isAdmissionForm(form)) {
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

    private boolean isAdmissionForm(Form form) {
        return DbUtils.getName(form).toLowerCase().contains("admiss");
    }

    private boolean isNo(Obs obs) {
        if (obs == null) return false;
        Boolean value = obs.getValueAsBoolean();
        if (value == null) return false;
        return !value;
    }

    private boolean isYes(Obs obs) {
        if (obs == null) return false;
        Boolean value = obs.getValueAsBoolean();
        if (value == null) return false;
        return value;
    }

    private Writable renderNoneSelected(Writable title, List<Obs> children) {
        return div("obs", span("label", title, ": "), " ", span("value", intl("None [fr:Aucun]")));
    }

    private Writable renderMultipleSelect(Writable title, List<Obs> children) {
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

    private Writable renderFormSection(Writable title, List<Obs> children) {
        Sequence results = seq();
        for (Obs obs : children) {
            results.add(renderObsContent(obs));
        }
        return results;
    }

    private Writable renderObsContent(Obs obs) {
        switch (Utils.compressUuid(DbUtils.getConceptUuid(obs)).toString()) {
            case DbUtils.CONCEPT_PLACEMENT_UUID:
                DataHelper.Placement p = helper.getPlacement(obs);
                return div("obs placement",
                    span("label", intl("New placement[fr:Nouveau emplacement]"), ": "),
                    span("value", renderPlacement(p))
                );

            case DbUtils.CONCEPT_ORDER_EXECUTED_UUID:
                Order order = obs.getOrder();
                return div("execution",
                    span("label", intl("Treatment given[fr:Traitement donné]"), ": "),
                    span("value", renderOrderTreatment(order))
                );

            default:
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
            result.add(seq(", ", intl("Bed[fr:Lit]"), " ", p.getBed()));
        }
        return result;
    }

    private Writable renderOrderTreatment(Order order) {
        Sequence result = seq(order.getInstructions());
        return result;
    }

    private Writable renderOrderSchedule(Order order) {
        Sequence result = seq(helper.formatTime(
            helper.toLocalDateTime(order.getScheduledDate())
        ));
        return result;
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
