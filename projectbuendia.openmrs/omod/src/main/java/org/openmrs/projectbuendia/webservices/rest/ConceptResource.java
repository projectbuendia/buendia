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
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * REST resource for charts. These are stored as OpenMRS forms, but that's primarily
 * to allow for ease of maintenance.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/concept", supportedClass = Concept.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class ConceptResource extends AbstractReadOnlyResource<Concept> {    
    // TODO(jonskeet): Add versioning, possibly via a global property
//    private static final String VERSION = "version";
    private static final String XFORM_ID = "xform_id";
    private static final String TYPE = "type";
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
    private final ClientConceptNamer namer;

    public ConceptResource() {
        super("concept", Representation.DEFAULT);
        formService = Context.getFormService();
        conceptService = Context.getConceptService();
        namer = new ClientConceptNamer(Context.getLocale());
    }
    
    @Override
    protected Concept retrieveImpl(String uuid, RequestContext context, long snapshotTime) {
        return conceptService.getConceptByUuid(uuid);
    }
    
    @Override
    protected Iterable<Concept> searchImpl(RequestContext context, long snapshotTime) {
        // No querying as yet.
        // Retrieves all the concepts required for the client. Initially, this is
        // just the concepts within all the charts served by ChartResource.
        Set<Concept> ret = new HashSet<>();
        for (Form chart : ChartResource.getCharts(formService)) {
            for (FormField formField : chart.getFormFields()) {
                Field field = formField.getField();
                Concept fieldConcept = field.getConcept();
                if (fieldConcept == null) {
                    continue;
                }
                ret.add(fieldConcept);
                for (ConceptAnswer answer : fieldConcept.getAnswers(false)) {
                    ret.add(answer.getAnswerConcept());
                }
            }
        }
        return ret;
    }
    
    @Override
    protected void populateJsonProperties(Concept concept, RequestContext context, SimpleObject json,
                                          long snapshotTime) {
       // TODO: Cache this in the request context?
       List<Locale> locales = getLocalesForRequest(context); 
       String jsonType = HL7_TO_JSON_TYPE_MAP.get(concept.getDatatype().getHl7Abbreviation());
       if (jsonType == null) {
           throw new ConfigurationException("Concept %s has unmapped HL7 data type %s",
                   concept.getName().getName(), concept.getDatatype().getHl7Abbreviation());
       }
       json.put(XFORM_ID, concept.getId());
       json.put(TYPE, jsonType);
       Map<String, String> names = new HashMap<>();
       for (Locale locale : locales) {
           names.put(locale.toString(), namer.getClientName(concept));
       }
       json.put(NAMES, names);
    }

    private List<Locale> getLocalesForRequest(RequestContext context) {
        // TODO(jonskeet): Make this cheap to call multiple times for a single request.
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
}
