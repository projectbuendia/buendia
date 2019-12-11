package org.projectbuendia.openmrs.web.controller;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
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
import org.projectbuendia.models.Catalog.Drug;
import org.projectbuendia.models.Catalog.Format;
import org.projectbuendia.models.Catalog.Route;
import org.projectbuendia.models.CatalogIndex;
import org.projectbuendia.models.Instructions;
import org.projectbuendia.models.MsfCatalog;
import org.projectbuendia.models.Quantity;
import org.projectbuendia.models.Unit;
import org.projectbuendia.openmrs.web.controller.DataHelper.FormSection;
import org.projectbuendia.openmrs.web.controller.DataHelper.History;
import org.projectbuendia.openmrs.web.controller.HtmlOutput.Doc;
import org.projectbuendia.openmrs.web.controller.HtmlOutput.Sequence;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.openmrs.projectbuendia.Utils.eq;
import static org.openmrs.projectbuendia.Utils.eqAny;
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

    public void printPrologue() throws IOException {
        writer.write("<meta charset='UTF-8'>");
        writer.write("<style>");
        writer.write(PrintCss.CSS);
        writer.write("</style>");
    }

    public void printEpilogue() throws IOException {
        writer.write("<script>");
        writer.write("window.onload = print;");
        writer.write("</script>");
    }

    public void printAdmissionForm(Patient pat) throws IOException {
        renderAdmissionFormFront(pat).writeTo(writer, locale);
        renderAdmissionFormBack(pat).writeTo(writer, locale);
    }

    public void printHistory(Patient pat) throws IOException {
        History history = helper.getHistory(pat);
        div("history",
            renderIntro(pat),
            !history.admission.isEmpty() ? renderAdmission(history.admission) : seq(),
            !history.evolution.isEmpty() ? renderEvents(pat, history.evolution) : seq(),
            !history.discharge.isEmpty() ? renderDischarge(history.discharge) : seq()
        ).writeTo(writer, locale);
    }

    public Doc renderIntro(Patient pat) {
        Doc age = renderAge(pat);
        Doc sex = renderSex(pat);
        return div("intro",
            div("ident", pat.getPatientIdentifier("MSF")),
            div("name", pat.getPersonName().getFullName()),
            div("agesex", sex, sex.isEmpty() ? "" : ", ", age)
        );
    }

    public Doc renderAge(Patient pat) {
        if (pat.getBirthdate() == null) return intl("age unknown [fr:âge inconnu]");
        Period age = new Period(
            helper.toLocalDateTime(pat.getBirthdate()),
            helper.toLocalDateTime(pat.getDateCreated()));
        int years = age.getYears();
        int months = age.getMonths();
        return span("age", format(
            years == 0 ? "%2$d mo [fr:%2$d mo]" :
                months == 0 ? "%1$d y [fr:%1$d a]" :
                    "%d y %d mo [fr:%d a %d mo]",
            years, months
        ));
    }

    public Doc renderSex(Patient pat) {
        if (!Utils.hasChars(pat.getGender())) return seq();
        Sequence pregnancy = seq();
        if (helper.isPregnant(pat)) pregnancy.add(text(", "), intl("pregnant [fr:enceinte]"));
        return span("sex", pat.getGender(), pregnancy);
    }

    public Doc yesNo() {
        return yesNo(null);
    }

    public Doc yesNo(Obs obs) {
        return seq(checkitem("Oui", isYes(obs)), checkitem("Non", isNo(obs)));
    }

    public static String UNKNOWN = "[unknown]";

    public static String TELEPHONE_UUID = toUuid(3159635);
    public static String PHONE_OWNER_UUID = toUuid(3900001);
    public static String AIRE_SANTE_UUID = toUuid(3900004);
    public static String VILLAGE_UUID = toUuid(3001354);

    public static String ADMISSION_DATETIME_UUID = toUuid(8001640);
    public static String SYMPTOM_START_UUID = toUuid(6001730);
    public static String STATUS_UUID = toUuid(2900005);
    public static String STATUS_SUSPECT_UUID = toUuid(4142177);
    public static String STATUS_PROBABLE_UUID = UNKNOWN;
    public static String STATUS_CONFIRMED_UUID = toUuid(4159392);
    public static String STATUS_DISCHARGED_NONCASE_UUID = toUuid(4900025);
    public static String STATUS_DISCHARGED_CURED_UUID = toUuid(4159791);
    public static String STATUS_DEATH_NONCASE_UUID = toUuid(4900026);
    public static String STATUS_DEATH_CONFIRMED_UUID = toUuid(4900027);
    public static String DISCHARGE_DATETIME_UUID = toUuid(8001641);
    public static String DISCHARGE_DESTINATION_UUID = toUuid(2001695);
    public static String HOME_UUID = toUuid(2001692);
    public static String HOSPITAL_UUID = toUuid(2001693);

    public static String PREGNANCY_UUID = toUuid(2005272);
    public static String PREGNANCY_TEST_UUID = toUuid(2000045);
    public static String WEIGHT_KG_UUID = toUuid(1005089);

    public static String HYPERTENSION_UUID = toUuid(2117399);
    public static String DIABETES_UUID = toUuid(2119481);
    public static String CHRONIC_LUNG_UUID = toUuid(2155569);
    public static String CHRONIC_HEART_UUID = toUuid(2145349);
    public static String HIV_UUID = UNKNOWN;
    public static String TB_UUID = UNKNOWN;
    public static String RENAL_DISEASE_UUID = UNKNOWN;

    public static String NO_KNOWN_ALLERGIES_UUID = toUuid(10160557);
    public static String ALLERGY_DESCRIPTION_UUID = toUuid(3160647);

    public static String TRANSPORT_MODE_UUID = toUuid(2001375);
    public static String TAXI_CAR_UUID = toUuid(4162711);
    public static String TAXI_MOTO_UUID = toUuid(4900008);
    public static String ON_FOOT_UUID = toUuid(4001613);
    public static String PRIVATE_CAR_UUID = toUuid(4001615);
    public static String PRIVATE_MOTO_UUID = toUuid(4001614);
    public static String MSP_AMBULANCE_UUID = toUuid(4900009);
    public static String MSF_AMBULANCE_UUID = toUuid(4900010);
    public static String RED_CROSS_AMBULANCE_UUID = toUuid(4900011);
    public static String OTHER_AMBULANCE_UUID = toUuid(4901377);
    public static String OTHER_UUID = toUuid(4005622);
    public static String OTHER_TRANSPORT_MODE_UUID = toUuid(3001378);

    public static String ACCOMPANYING_NAME_UUID = toUuid(3900044);
    public static String ACCOMPANYING_AGE_UUID = toUuid(3900013);
    public static String ACCOMPANYING_RELATION_UUID = toUuid(3900014);
    public static String ACCOMPANYING_SUSPECTED_UUID = toUuid(2162743);

    public Doc renderAdmissionFormBack(Patient pat) {
        History history = helper.getHistory(pat);
        Map<String, Obs> admissionObs = helper.getLastObsByConcept(history.admission);

        String admStat = getCodedValue(admissionObs.get(STATUS_UUID));
        DateTime admDateTime = getDateTimeValue(pat, ADMISSION_DATETIME_UUID);
        String facilityName = "Bunia";
        String providerName = helper.getProvider(helper.getFirstObs(pat, ADMISSION_DATETIME_UUID)).getName();
        String mode = getCodedValue(admissionObs.get(TRANSPORT_MODE_UUID));
        String aireSante = getTextValue(pat, AIRE_SANTE_UUID);
        String village = getTextValue(pat, VILLAGE_UUID);
        String aireSanteVillage = aireSante + (!aireSante.isEmpty() && !village.isEmpty() ? " / " : "") + village;

        Sequence accompanyingRows = seq();
        int rows = 0;
        for (Encounter enc : helper.getEncountersWithConcept(pat, ACCOMPANYING_NAME_UUID)) {
            Map<String, Obs> encObs = helper.getLastObsByConcept(enc);
            String name = getTextValue(encObs.get(ACCOMPANYING_NAME_UUID));
            String age = getTextValue(encObs.get(ACCOMPANYING_AGE_UUID));
            String relation = getTextValue(encObs.get(ACCOMPANYING_RELATION_UUID));
            Obs suspectedObs = encObs.get(ACCOMPANYING_SUSPECTED_UUID);
            accompanyingRows.add(
                el("tr",
                    el("td", line(name)),
                    el("td", age),
                    el("td", relation),
                    el("td",
                        isYes(suspectedObs) ? "Oui" :
                            isNo(suspectedObs) ? "Non" : "")
                )
            );
            rows++;
        }
        while (rows++ < 5) {
            accompanyingRows.add(
                el("tr",
                    el("td", line("")),
                    el("td"),
                    el("td"),
                    el("td")
                )
            );
        }

        return div("admission-form",
            div("title",
                columns(
                    column("50%",
                        div("heading", "ENREGISTREMENT CTE")
                    ),
                    column("50%",
                        div("patient-id",
                            field("* Nr. Identification Patient ID:",
                                blank(4, pat.getPatientIdentifier("MSF"))
                            )
                        )
                    )
                )
            ),
            section("status-pcr",
                null,
                div("shaded",
                    columns(
                        column("50%",
                            line(field("Date d'admission:", renderDate(admDateTime)))
                        ),
                        column("50%",
                            line(field("Heure d'admission:", renderTime(admDateTime)))
                        )
                    )
                ),
                columns(
                    column("50%", line(field("Nom du CTE:", blank(8, facilityName)))),
                    column("50%", line(field("Nom de l'enregistreur:", blank(8, providerName))))
                ),
                line(
                    field("* Statut du patient à l'admission:",
                        checkitem("Suspect", eq(admStat, STATUS_SUSPECT_UUID)),
                        hspace(2),
                        checkitem("Probable", eq(admStat, STATUS_PROBABLE_UUID)),
                        hspace(2),
                        checkitem("Confirmé", eq(admStat, STATUS_CONFIRMED_UUID))
                    )
                ),
                line("Date du dernier PCR:", renderEmptyDate())
            ),
            section("arrival",
                "Arrivée patient à CTE",
                block("origin",
                    columns(
                        column("50%", subhead("* D'où le patient a-t-il/elle été transporté(e)?")),
                        column("50%", line(field("Aire de Santé / Village:", blank(10, aireSanteVillage))))
                    )
                ),
                block("transport",
                    line(subhead("* Comment le patient est-il/elle arrivé(e) au CTE:")),
                    line(
                        checkitem("Taxi car", eq(mode, TAXI_CAR_UUID)),
                        checkitem("Taxi moto", eq(mode, TAXI_MOTO_UUID)),
                        checkitem("A pied", eq(mode, ON_FOOT_UUID)),
                        checkitem("Voiture privée", eq(mode, PRIVATE_CAR_UUID)),
                        checkitem("Moto privée", eq(mode, PRIVATE_MOTO_UUID))
                    ),
                    line(
                        checkitem("Ambulance: Si oui avec", eqAny(mode,
                            MSP_AMBULANCE_UUID, MSF_AMBULANCE_UUID,
                            RED_CROSS_AMBULANCE_UUID, OTHER_AMBULANCE_UUID
                        )),
                        checkitem("Min.San", eq(mode, MSP_AMBULANCE_UUID)),
                        checkitem("MSF", eq(mode, MSF_AMBULANCE_UUID)),
                        checkitem("Croix Rouge", eq(mode, RED_CROSS_AMBULANCE_UUID)),
                        checkitem("Autre", eq(mode, OTHER_AMBULANCE_UUID))
                    ),
                    line(
                        checkitem("Par d'autres moyens, spécifiez"), blank(8)
                    ),
                    vspace()
                ),
                block("accompanying",
                    subhead("* Le patient est accompagné par:"),
                    el("table cellpadding=0 cellspacing=0",
                        el("row",
                            el("th width='40%'", line("Name")),
                            el("th width='10%'", line("Age")),
                            el("th width='35%'", line("Lien avec le patient")),
                            el("th width='15%'", line("Cas suspect O/N"))
                        ),
                        accompanyingRows
                    )
                )
            )
        );
    }

    public Doc renderAdmissionFormFront(Patient pat) {
        History history = helper.getHistory(pat);
        DateTime admDateTime = getDateTimeValue(pat, ADMISSION_DATETIME_UUID);

        Map<String, Obs> admissionObs = helper.getLastObsByConcept(history.admission);
        Obs pregnancy = admissionObs.get(PREGNANCY_UUID);
        Obs pregnancyTest = admissionObs.get(PREGNANCY_TEST_UUID);
        String admStat = getCodedValue(admissionObs.get(STATUS_UUID));
        String allergyDesc = getTextValue(admissionObs.get(ALLERGY_DESCRIPTION_UUID));

        String disStat = getCodedValue(pat, STATUS_UUID);
        String disDest = getCodedValue(pat, DISCHARGE_DESTINATION_UUID);

        return div("admission-form",
            div("title",
                columns(
                    column("50%",
                        div("heading", "Admission - Enregistrement CTE")
                    ),
                    column("50%",
                        div("patient-id",
                            field("* Nr. Identification (ID) Patient:",
                                blank(4, pat.getPatientIdentifier("MSF"))
                            )
                        )
                    )
                )
            ),
            section("patient-info",
                "Patient Information",
                columns(
                    column("50%",
                        block("name",
                            line(field("* Nom de famille:", blank(9, pat.getFamilyName()))),
                            line(field("* Prénom:", blank(10, pat.getGivenName())))
                        ),
                        block("contact",
                            subhead("* Information contact Patient:"),
                            line(field("Numéro de téléphone Patient:", blank(6, getTextValue(pat, TELEPHONE_UUID)))),
                            line(field("Propriétaire du téléphone:", blank(7, getTextValue(pat, PHONE_OWNER_UUID))))
                        )
                    ),
                    column("50%",
                        block("agesex",
                            line(field("* Date de naissance:", renderEmptyDate())),
                            line(
                                field("si inconnu, âge:", blank(2, renderAge(pat))),
                                hspace(),
                                field("* Sexe:",
                                    checkitem("Masculin", eq(pat.getGender().toUpperCase(), "M")),
                                    checkitem("Féminin", eq(pat.getGender().toUpperCase(), "F"))
                                )
                            )
                        ),
                        block("additional-contact",
                            subhead("* Information contact additionnelle (famille/amis):"),
                            line(field("Numéro de téléphone additionnel:", blank(5))),
                            line(field("Propriétaire du téléphone:", blank(7)))
                        )
                    )
                ),
                block("residence",
                    subhead("* Lieu de résidence:"),
                    line(
                        field("Aire de Santé:", blank(10, getTextValue(pat, AIRE_SANTE_UUID))),
                        hspace(),
                        field("Village:", blank(10, getTextValue(pat, VILLAGE_UUID)))
                    )
                )
            ),
            section("admission-info",
                "Admission Information",
                columns(
                    column("50%",
                        block("dates",
                            line(field("* Date d'admission:", renderDate(admDateTime))),
                            line(field("Heure d'admission:", renderTime(admDateTime))),
                            line(field("* Date de début des symptômes:", renderDate(getDateValue(pat, SYMPTOM_START_UUID))))
                        )
                    ),
                    column("50%",
                        block("status",
                            line(field("* État du Patient à l'admission:")),
                            line(hspace(2),
                                checkitem("Suspect", eq(admStat, STATUS_SUSPECT_UUID)),
                                checkitem("Probable", eq(admStat, STATUS_PROBABLE_UUID)),
                                checkitem("Confirmé", eq(admStat, STATUS_CONFIRMED_UUID))
                            )
                        )
                    )
                ),
                block("pregnancy",
                    columns(
                        column("33%",
                            subhead("Si la patiente est âge de procréer:"),
                            line(field(
                                "* Enceinte actuellement:",
                                stack(
                                    checkitem("Oui", isYes(pregnancy)),
                                    checkitem("Non", isNo(pregnancy)),
                                    checkitem("Inconnu", !isYes(pregnancy) && !isNo(pregnancy))
                                )
                            )),
                            line(field("* Allaitante:", yesNo()))
                        ),
                        column("33%",
                            vspace(),
                            line(field("* Test de grossesse:",
                                stack(
                                    checkitem("Positif", isYes(pregnancyTest)),
                                    checkitem("Negatif", isNo(pregnancyTest))
                                )
                            )),
                            line(field("* Date:", renderDate(getObs(pat, PREGNANCY_TEST_UUID))))
                        ),
                        column("33%",
                            vspace(),
                            line(field("Nr de semaines de gestation estimé:", blank(1))),
                            line(field("Trimestre de grossesse:", blank(3))),
                            line(field("Fetus vivant:", yesNo()))
                        )
                    )
                ),
                columns(
                    column("60%",
                        block("comorbidities",
                            subhead(columns(column("50%", "Comorbidités"), column("50%", "Traitement"))),
                            line(checkitem("Hypertension", isYes(admissionObs.get(HYPERTENSION_UUID)))),
                            line(checkitem("VIH/SIDA", isYes(admissionObs.get(HIV_UUID)))),
                            line(checkitem("TB", isYes(admissionObs.get(TB_UUID)))),
                            line(checkitem("Maladie pulmonaire chronique", isYes(admissionObs.get(CHRONIC_LUNG_UUID)))),
                            line(checkitem("Maladie cardiaque chronique", isYes(admissionObs.get(CHRONIC_HEART_UUID)))),
                            line(checkitem("Maladie rénale", isYes(admissionObs.get(RENAL_DISEASE_UUID)))),
                            line(checkitem("Diabètes", isYes(admissionObs.get(DIABETES_UUID)))),
                            line(checkitem("Autre"))
                        )
                    ),
                    column("40%",
                        block("physical",
                            div("multiline",
                                line(field("Allergies:",
                                    checkitem("Oui", isNo(admissionObs.get(NO_KNOWN_ALLERGIES_UUID)) || !allergyDesc.isEmpty()),
                                    checkitem("Non", isYes(admissionObs.get(NO_KNOWN_ALLERGIES_UUID)) && allergyDesc.isEmpty())
                                ))
                            ),
                            line(field("Si oui, spécifiez:", blank(6))),
                            line(field(allergyDesc, blank(8))),
                            vspace(),
                            line(field("Poids:", blank(2, renderNumber(getNumericValue(pat, WEIGHT_KG_UUID))), " kg")),
                            vspace(),
                            line(field("Vacciné avec rVSV ZEBOV:",
                                checkitem("Oui"),
                                checkitem("Non")
                            )),
                            line(field("Si oui, date:", renderEmptyDate()))
                        )
                    )
                )
            ),
            section("discharge",
                "Resultat du Patient à la Sortie",
                block("result",
                    line(
                        field("* Statut final:",
                            checkitem("Confirmé",
                                eq(disStat, STATUS_CONFIRMED_UUID) ||
                                eq(disStat, STATUS_DEATH_CONFIRMED_UUID)),
                            checkitem("Pas un cas",
                                eq(disStat, STATUS_DISCHARGED_NONCASE_UUID) ||
                                eq(disStat, STATUS_DEATH_NONCASE_UUID)
                            ),
                            checkitem("Inconnu",
                                !eq(disStat, STATUS_CONFIRMED_UUID) &&
                                !eq(disStat, STATUS_DISCHARGED_NONCASE_UUID) &&
                                !eq(disStat, STATUS_DISCHARGED_CURED_UUID) &&
                                !eq(disStat, STATUS_DEATH_NONCASE_UUID) &&
                                !eq(disStat, STATUS_DEATH_CONFIRMED_UUID)
                            )
                        ),
                        hspace(),
                        field("* Date de sortie:", renderDate(getDateValue(pat, DISCHARGE_DATETIME_UUID)))
                    ),
                    line(field("* Résultat final:",
                        checkitem("Sortie en revalidation"),
                        checkitem("Décédé",
                            eq(disStat, STATUS_DEATH_NONCASE_UUID) ||
                            eq(disStat, STATUS_DEATH_CONFIRMED_UUID)
                        ),
                        checkitem("Transferé",
                            eq(getCodedValue(pat, DISCHARGE_DESTINATION_UUID), HOSPITAL_UUID)
                        ),
                        checkitem(
                            field("Autre, spécifiez:", blank(6,
                                eq(disStat, STATUS_DISCHARGED_CURED_UUID) ? "Guéri" :
                                eq(disStat, STATUS_DISCHARGED_NONCASE_UUID) ? "Sortie non-cas" :
                                eq(disDest, HOME_UUID) ? "Sortie à la maison" : ""
                            )),
                            eq(disStat, STATUS_DISCHARGED_CURED_UUID) ||
                            eq(disStat, STATUS_DISCHARGED_NONCASE_UUID) ||
                            eq(getCodedValue(pat, DISCHARGE_DESTINATION_UUID), HOME_UUID)
                        )
                    )),
                    line(
                        field("Si sortie, session d'information donnée:", checkitem("Oui"), checkitem("Non")),
                        hspace(),
                        field("Kit d'hygiène donnée:", checkitem("Oui"), checkitem("Non"))
                    ),
                    line(field("Si transferé, lieu", blank(10)))
                ),
                block("experimental",
                    subhead("Traitement expérimental"),
                    columns(
                        column("50%",
                            line(field("Patient a reçu un traitement expérimental ?", yesNo())),
                            line(field("Combien de dose:", blank(5)))
                        ),
                        column("50%",
                            line(field("Lequel ?", blank(5))),
                            line(field("Date de la dernière dose:", renderEmptyDate()))
                        )
                    )
                ),
                block("pregnancy",
                    subhead("Si grossesse:"),
                    columns(
                        column("33%",
                            line(field("Grossesse préservée:", yesNo())),
                            line(field("Fausse couche:", yesNo()))
                        ),
                        column("33%",
                            line(field("Accouchement:", yesNo())),
                            line(field("Interruption de grossesse:", yesNo()))
                        ),
                        column("33%",
                            line(field("Nouveau-né vivant:", yesNo()))
                        )
                    )
                ),
                block("followup",
                    columns(
                        column("40%",
                            line(field("Visite de suivi prévue ?", yesNo())),
                            line(field("Date:", renderEmptyDate()))
                        ),
                        column("60%",
                            line(field("Si Oui pourquoi?",
                                stack(
                                    checkitem("Symptômes persistants"),
                                    checkitem("Grossesse")
                                ),
                                stack(
                                    checkitem("traitement expérimental"),
                                    checkitem(field("Autre, précise:", blank(2)))
                                )
                            ))
                        )
                    )
                )
            )
        );
    }

    public String getTextValue(Patient pat, String conceptUuid) {
        return getTextValue(getObs(pat, conceptUuid));
    }

    public String getTextValue(Obs obs) {
        return obs != null ? obs.getValueText() : "";
    }

    public Double getNumericValue(Patient pat, String conceptUuid) {
        Obs obs = getObs(pat, conceptUuid);
        return obs != null ? obs.getValueNumeric() : null;
    }

    public String getCodedValue(Patient pat, String conceptUuid) {
        return getCodedValue(getObs(pat, conceptUuid));
    }

    public String getCodedValue(Obs obs) {
        return obs != null ? DbUtils.getUuid(obs.getValueCoded()) : null;
    }

    public LocalDate getDateValue(Patient pat, String conceptUuid) {
        Obs obs = getObs(pat, conceptUuid);
        return obs != null ? helper.toLocalDateTime(obs.getValueDate()).toLocalDate() : null;
    }

    public DateTime getDateTimeValue(Patient pat, String conceptUuid) {
        return getDateTimeValue(getObs(pat, conceptUuid));
    }

    public DateTime getDateTimeValue(Obs obs) {
        return obs != null ? helper.toLocalDateTime(obs.getValueDatetime()) : null;
    }

    public Obs getObs(Patient pat, String conceptUuid) {
        return helper.getLatestObs(pat, conceptUuid);
    }

    public Doc renderAdmission(List<Encounter> encounters) {
        Map<String, Obs> map = helper.getLastObsByConcept(encounters);
        DateTime admissionTime = getDateTimeValue(map.get(ADMISSION_DATETIME_UUID));
        return div("admission",
            div("heading", intl("Admission [fr:Admission]")),
            div("time", helper.formatTime(admissionTime)),
            div("observations", renderObsList(map))
        );
    }

    public Doc renderDischarge(List<Encounter> encounters) {
        Map<String, Obs> map = helper.getLastObsByConcept(encounters);
        DateTime dischargeTime = getDateTimeValue(map.get(DISCHARGE_DATETIME_UUID));
        return div("discharge",
            div("heading", intl("Discharge [fr:Sorti]")),
            div("time", helper.formatTime(dischargeTime)),
            div("observations", renderObsList(map))
        );
    }

    public Doc renderEvents(Patient pat, List<Encounter> evolution) {
        Sequence results = seq();
        List<DataHelper.Event> events = helper.getEvolution(pat, evolution);
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
                    columns(
                        column("50%",
                            obsList.isEmpty() ? seq() : div("observations", obsList)
                        ),
                        column("50%", div("treatments",
                            orderList.isEmpty() ? seq() : div("orders",
                                span("label", el("h3", intl("Treatments ordered [fr:Traitements commandés]"))), orderList),
                            execList.isEmpty() ? seq() : div("executions",
                                span("label", el("h3", intl("Treatments given [fr:Traitements donnés]"))), execList)
                        ))
                    )
                ));
            }
        }
        return div("evolution",
            div("heading", intl("Evolution [fr:Evolution]")),
            div("events", results)
        );
    }

    private Sequence renderObsList(Map<String, Obs> obsMap) {
        Sequence results = new Sequence();
        Set<String> done = new HashSet<>();
        boolean admissionFormSubmitted = obsMap.containsKey(
            ConceptUuids.ADMISSION_DATETIME_UUID);
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
            instr.amount != null ?
                span("dosage", renderQuantity(instr.amount)) : seq();

        Route route = index.getRoute(instr.route);
        return div("treatment",
            div("drug", span("label", intl("Drug [fr:Médicament]"), ": "), drug.name),
            div("format", span("label", intl("Format"), ": "), format.description),
            div("dosageroute",
                span("label", intl("Dosage"), ": "), dosage, " ",
                span("route", format("%s (%s)", route.abbr, route.name))
            ),
            Utils.hasChars(instr.notes) ?
                div("notes", span("label", intl("Notes [fr:Remarques]"), ": "), instr.notes) : seq()
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
        if (stop != null && instr.isSeries() && eq(instr.frequency.unit, Unit.PER_DAY)) {
            int days = Days.daysBetween(start.toLocalDate(), stop.toLocalDate()).getDays();
            doses = days * (int) instr.frequency.mag;
        }
        return div("schedule", span("label", intl("Schedule [fr:Horaire]"), ": "), instr.isSeries() && instr.frequency.mag > 0 ?
            (stop != null ?
                (doses > 0 ?
                    seq(renderQuantity(instr.frequency), ", ",
                        format("starting %s; stopping %s after %d doses [fr:commencer %s; arrêter %s après %d doses]",
                            helper.formatTime(start), helper.formatTime(stop), doses)) :
                    seq(renderQuantity(instr.frequency), ", ",
                        format("starting %s; stopping %s [fr:commencer %s; arrêter %s]",
                            helper.formatTime(start), helper.formatTime(stop)))
                ) :
                seq(renderQuantity(instr.frequency), ", ",
                    format("starting %s, continuing indefinitely [fr:commencer %s; continuer indéfiniment]",
                        helper.formatTime(start)))
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
        return renderHoursMinutes(
            Utils.format("%02d", value.getHourOfDay()),
            Utils.format("%02d", value.getMinuteOfHour())
        );
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
            rows.add(div("row", obj));
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
        return div("section " + cls, div("heading", heading), objects);
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
            spaces.add(span("space"));
        }
        return span("blank size" + size, span("spaces", spaces), span("contents", contents));
    }

    private Doc vspace() {
        return div("vspace");
    }

    private Doc hspace(int size) {
        Sequence spaces = seq();
        for (int i = 0; i < size; i++) {
            spaces.add(span("space"));
        }
        return span("hspace size" + size, spaces);
    }

    private Doc hspace() {
        return span("hspace", span("space"));
    }

    private Doc checkitem(Object label) {
        return checkitem(label, false);
    }

    private Doc checkitem(Object label, boolean state) {
        String checked = state ? "checked" : "";
        return span("checkitem " + checked, el("input type='checkbox' " + checked), label);
    }

    private Doc field(String label, Object... objects) {
        return span("field", span("label", label), span("value", objects));
    }
}
