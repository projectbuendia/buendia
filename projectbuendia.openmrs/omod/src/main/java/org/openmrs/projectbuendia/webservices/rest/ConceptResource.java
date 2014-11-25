package org.openmrs.projectbuendia.webservices.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptName;
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
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.projectbuendia.openmrs.webservices.rest.RestController;

/**
 * REST resource for charts. These are stored as OpenMRS forms, but that's primarily
 * to allow for ease of maintenance.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/concept", supportedClass = Concept.class, supportedOpenmrsVersions = "1.10.*")
public class ConceptResource implements Listable, Searchable, Retrievable {    
    // TODO(jonskeet): Add versioning, possibly via a global property
//    private static final String VERSION = "version";
    private static final String RESULTS = "results";
    private static final String TYPE = "type";
    private static final String UUID = "uuid";
    private static final String LOCALES_PARAMETER = "locales";
    private static final String NAMES = "names";
    
    private static final Map<String, String> HL7_TO_JSON_TYPE_MAP = new HashMap<>();
    
    static {
        HL7_TO_JSON_TYPE_MAP.put(HL7Constants.HL7_CODED, "coded");
        HL7_TO_JSON_TYPE_MAP.put(HL7Constants.HL7_CODED_WITH_EXCEPTIONS, "coded");
        HL7_TO_JSON_TYPE_MAP.put(HL7Constants.HL7_TEXT, "text");
        HL7_TO_JSON_TYPE_MAP.put(HL7Constants.HL7_NUMERIC, "numeric");
        HL7_TO_JSON_TYPE_MAP.put("ZZ", "none");
    }
        
    private final FormService formService;
    private final ConceptService conceptService;
    
    public ConceptResource() {
        formService = Context.getFormService();
        conceptService = Context.getConceptService();
    }
    
    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        Concept concept = conceptService.getConceptByUuid(uuid);
        if (concept == null) {
            throw new ObjectNotFoundException();
        }
        return conceptToJson(getLocalesForRequest(context), concept);
    }

    // When we have a URL parameter for locales, we end up in the search path instead
    // of getAll. Handle them the same way - we're not searching for specific concepts,
    // just altering the representation.
    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        return getAll(context);
    }

    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        List<Locale> locales = getLocalesForRequest(context);
        Set<Concept> concepts = getRequiredConcepts();
        List<SimpleObject> jsonConcepts = new ArrayList<>();
        for (Concept concept : concepts) {
            jsonConcepts.add(conceptToJson(locales, concept));
        }
        SimpleObject list = new SimpleObject();
        list.add(RESULTS, jsonConcepts);
        return list;
    }

    private List<Locale> getLocalesForRequest(RequestContext context) {
        String localeIds = context.getRequest().getParameter(LOCALES_PARAMETER);
        if (localeIds == null || localeIds.trim().equals("")) {
            return Context.getAdministrationService().getAllowedLocales();
        }
        List<Locale> locales = new ArrayList<>();
        for (String localeId : localeIds.split(",")) {
            locales.add(Locale.forLanguageTag(localeId.trim()));
        }
        return locales;
    }
    
    private SimpleObject conceptToJson(List<Locale> locales, Concept concept) {
        SimpleObject jsonConcept = new SimpleObject();
        jsonConcept.put(UUID, concept.getUuid());
        String jsonType = HL7_TO_JSON_TYPE_MAP.get(concept.getDatatype().getHl7Abbreviation());
        if (jsonType == null) {
            throw new ConfigurationException("Concept %s has unmapped HL7 data type %s",
                    concept.getName().getName(), concept.getDatatype().getHl7Abbreviation());
        }
        jsonConcept.put(TYPE, jsonType);
        Map<String, String> names = new HashMap<>();
        for (Locale locale : locales) {
            ConceptName conceptName = concept.getName(locale, false);
            if (conceptName == null) {
                throw new ConfigurationException("Concept %s has no translation in locale %s",
                        concept.getName().getName(), locale);
            }
            names.put(locale.toString(), conceptName.getName());
        }
        jsonConcept.put(NAMES, names);
        return jsonConcept;
    }
    
    /**
     * Retrieves all the concepts required for the client. Initially, this is
     * just the concepts within all the charts served by ChartResource.
     */
    private Set<Concept> getRequiredConcepts() {
        Set<Concept> ret = new HashSet<Concept>();
        for (Form chart : ChartResource.getCharts(formService)) {
            for (FormField formField : chart.getFormFields()) {
                Field field = formField.getField();
                Concept fieldConcept = field.getConcept();
                if (fieldConcept == null) {
                    continue;
                }
                ret.add(fieldConcept);
                for (ConceptAnswer answer : fieldConcept.getAnswers(false)) {
                    ret.add(answer.getConcept());
                }
            }
        }
        return ret;
    }

    @Override
    public String getUri(Object instance) {
        throw new UnsupportedOperationException("No URI for concept resource");
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }
}
