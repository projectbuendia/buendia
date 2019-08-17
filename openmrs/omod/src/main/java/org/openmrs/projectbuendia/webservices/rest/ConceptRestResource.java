package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.ConceptService;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.projectbuendia.ClientConceptNamer;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import static org.openmrs.projectbuendia.Utils.isBlank;

@Resource(
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/concepts",
    supportedClass = Concept.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class ConceptRestResource extends BaseRestResource<Concept> {
    /** A map from HL7 type abbreviations to short names used in JSON output. */
    private static final Map<String, String> HL7_TYPE_NAMES = new HashMap<>();
    static {
        HL7_TYPE_NAMES.put(HL7Constants.HL7_BOOLEAN, "coded");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_CODED, "coded");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_CODED_WITH_EXCEPTIONS, "coded");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_TEXT, "text");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_NUMERIC, "numeric");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_DATE, "date");
        HL7_TYPE_NAMES.put(HL7Constants.HL7_DATETIME, "datetime");
        HL7_TYPE_NAMES.put("ZZ", "none");
    }
    private final ClientConceptNamer namer;

    public ConceptRestResource() {
        super("concepts", Representation.DEFAULT);
        namer = new ClientConceptNamer(Context.getLocale());
    }

    @Override protected Collection<Concept> listItems(RequestContext context) {
        // Retrieves all the concepts that the client needs to know about
        // (the concepts within all the charts served by ChartResource).
        Set<Concept> concepts = new HashSet<>();
        for (Form chart : ChartResource.getCharts(formService)) {
            for (FormField formField : chart.getFormFields()) {
                Field field = formField.getField();
                Concept concept = field.getConcept();
                if (concept != null && !concept.isRetired()) {
                    concepts.add(concept);
                    for (ConceptAnswer answer : concept.getAnswers(false)) {
                        concepts.add(answer.getAnswerConcept());
                    }
                }
                // Add extra concepts that might be mentioned in the field description.
                for (Concept extra : getConceptsFromFieldDescription(field.getDescription())) {
                    if (extra != null && !extra.isRetired()) {
                        concepts.add(extra);
                    }
                }
            }
        }
        return concepts;
    }

    @Override protected Concept retrieveItem(String uuid) {
        Concept concept = conceptService.getConceptByUuid(uuid);
        if (concept == null || concept.isRetired()) {
            throw new ObjectNotFoundException();
        }
        return concept;
    }

    @Override protected void populateJson(SimpleObject json, Concept concept, RequestContext context) {
        List<Locale> locales = getLocalesForRequest(context);
        String type = HL7_TYPE_NAMES.get(concept.getDatatype().getHl7Abbreviation());
        if (type == null) {
            throw new ConfigurationException("Concept %s has unmapped HL7 data type %s",
                concept.getName().getName(), concept.getDatatype().getHl7Abbreviation());
        }
        json.put("xform_id", concept.getId());
        json.put("type", type);
        Map<String, String> names = new HashMap<>();
        for (Locale locale : locales) {
            names.put(locale.toString(), namer.getClientName(concept));
        }
        json.put("names", names);
    }

    /**
     * Buendia supports multiple concepts per field, but OpenMRS does not. To handle
     * this, Buendia packs additional concept IDs into the "field description" that
     * is generated in the profile_apply script.  This method finds and extracts
     * those concept IDs so we can get the complete set of concept IDs in use.
     */
    private static List<Concept> getConceptsFromFieldDescription(
        @Nullable String fieldDescription) {
        if (fieldDescription == null) {
            return Collections.emptyList();
        }
        List<Concept> returnValue = new ArrayList<>();
        try {
            SimpleObject extendedData = SimpleObject.parseJson(fieldDescription);
            Object object = extendedData.get("concepts");
            if (! (object instanceof List)) {
                return Collections.emptyList();
            }

            List list = (List) object;
            for (Object obj : list) {
                if (obj == null) continue;
                // We're ok with a ClassCastException here if this isn't an Integer.
                Integer conceptId = (Integer) obj;
                Concept concept = Context.getConceptService().getConcept(conceptId);
                if (concept == null || concept.isRetired()) continue;
                returnValue.add(concept);
            }
        } catch (IOException ignore) {
        }
        return returnValue;
    }

    private List<Locale> getLocalesForRequest(RequestContext context) {
        // TODO: Make this cheap to call multiple times for a single request.
        String localeIds = context.getRequest().getParameter("locales");
        if (isBlank(localeIds)) {
            return Context.getAdministrationService().getAllowedLocales();
        }
        List<Locale> locales = new ArrayList<>();
        for (String localeId : localeIds.split(",")) {
            locales.add(Locale.forLanguageTag(localeId.trim()));
        }
        return locales;
    }
}
