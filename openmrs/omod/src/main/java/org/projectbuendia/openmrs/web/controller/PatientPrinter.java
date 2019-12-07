package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.projectbuendia.Utils;
import org.openmrs.projectbuendia.webservices.rest.DbUtils;
import org.projectbuendia.openmrs.web.controller.HtmlOutput.LocalizedWriter;
import org.projectbuendia.openmrs.web.controller.HtmlOutput.Writable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import static org.projectbuendia.openmrs.web.controller.HtmlOutput.el;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.format;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.formatItems;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.html;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.seq;
import static org.projectbuendia.openmrs.web.controller.HtmlOutput.text;

class PatientPrinter {
    private final LocalizedWriter writer;
    private final DataHelper helper;
    private final PatientRenderer formatter;

    public PatientPrinter(PrintWriter writer, Locale locale, DataHelper helper) {
        this.writer = new LocalizedWriter(writer, locale);
        this.helper = helper;
        this.formatter = new PatientRenderer();
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
        write(el("h1", pat.getPersonName().getFullName()));
        List<List<Obs>> groups = helper.getEncounterObs(helper.getEncounters(pat));
        for (List<Obs> group : groups) {
            if (group.isEmpty()) continue;
            DateTime start = helper.toLocalDateTime(group.get(0).getObsDatetime());
            write(el("h2", helper.formatTime(start)));
            write(el("ul", formatItems(formatter, group)));
        }
    }

    class PatientRenderer implements HtmlOutput.Renderer {
        @Override public Writable render(Object obj) {
            if (obj instanceof Obs) return renderObs((Obs) obj);
            return null;
        }

        private Writable renderObs(Obs obs) {
            return div("obs", renderObsContent(obs));
        }

        private Writable renderObsContent(Obs obs) {
            switch (Utils.compressUuid(obs.getConcept().getUuid()).toString()) {
                case DbUtils.CONCEPT_PLACEMENT_UUID:
                    DataHelper.Placement p = helper.getPlacement(obs);
                    return format("Patient moved to %s", p.getLocationName());
                default:
                    return seq(helper.getName(obs.getConcept()), ": ", renderValue(obs));
            }
        }

        private Writable renderValue(Obs obs) {
            ConceptDatatype type = obs.getConcept().getDatatype();
            switch (type.getHl7Abbreviation()) {
                case ConceptDatatype.BOOLEAN:
                    return text(Boolean.toString(obs.getValueBoolean()));
                case ConceptDatatype.CODED:
                    return text(helper.getName(obs.getValueCoded()));
                case ConceptDatatype.NUMERIC:
                    return text(Utils.format(obs.getValueNumeric(), 6));
                case ConceptDatatype.TEXT:
                    return text(obs.getValueText());
                case ConceptDatatype.DATE:
                    return text(helper.formatTime(helper.toLocalDateTime(obs.getValueDate())));
                case ConceptDatatype.DATETIME:
                    return text(helper.formatTime(helper.toLocalDateTime(obs.getValueDatetime())));
            }
            return text("?");
        }
    }

    private Writable line(Object... objects) {
        return el("div", objects);
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
