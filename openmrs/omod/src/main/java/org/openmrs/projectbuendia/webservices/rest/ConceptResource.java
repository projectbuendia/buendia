package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptName;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

@Resource(
    name = RestController.PATH + "/concepts",
    supportedClass = Concept.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class ConceptResource extends BaseResource<Concept> {
    public ConceptResource() {
        super("concepts", Representation.DEFAULT);
    }

    @Override protected Collection<Concept> listItems(RequestContext context) {
        // Retrieves all the concepts that the client needs to know about
        // (the concepts within all the forms served by XFormResource and
        // all the charts served by ChartResource).
        Set<Concept> concepts = new HashSet<>();
        List<Form> forms = new ArrayList<>();
        forms.addAll(ChartResource.getChartForms(formService));
        forms.addAll(XformResource.getXformForms(formService));
        for (Form form : forms) {
            for (FormField formField : form.getFormFields()) {
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
        return conceptService.getConceptByUuid(uuid);
    }

    @Override protected void populateJson(SimpleObject json, Concept concept, RequestContext context) {
        Locale locale = DbUtils.getLocaleForTag(context.getParameter("locale"));
        json.put("xform_id", concept.getId());
        json.put("type", DbUtils.getConceptTypeName(concept));
        json.put("name", DbUtils.getConceptName(concept, locale));
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
}
