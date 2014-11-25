package org.openmrs.projectbuendia.webservices.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openmrs.Concept;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.util.FormUtil;
import org.projectbuendia.openmrs.webservices.rest.RestController;

/**
 * REST resource for charts. These are stored as OpenMRS forms, but that's primarily
 * to allow for ease of maintenance.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/chart", supportedClass = Form.class, supportedOpenmrsVersions = "1.10.*")
public class ChartResource implements Listable, Searchable, Retrievable {
    
    private static final String UUID = "uuid";
    private static final String GROUPS = "groups";
    private static final String VERSION = "version";
    private static final String CONCEPTS = "concepts";
        
    private final FormService formService;
    
    public ChartResource() {
        formService = Context.getFormService();
    }
    
    @Override
    public String getUri(Object instance) {
        Form form = (Form) instance;
        Resource res = getClass().getAnnotation(Resource.class);
        return RestConstants.URI_PREFIX + res.name() + "/" + form.getUuid();
    }

    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        Form form = formService.getFormByUuid(uuid);
        if (form == null) {
            throw new ObjectNotFoundException();
        }
        return formToJson(form, context.getRepresentation());
    }
    
    private SimpleObject formToJson(Form form, Representation representation) {
        SimpleObject chart = new SimpleObject();
        chart.put(UUID, form.getUuid());
        chart.put(VERSION, form.getVersion());
        if (representation == Representation.FULL) {
            List<SimpleObject> groups = new ArrayList<>();
            TreeMap<Integer, TreeSet<FormField>> formStructure = FormUtil.getFormStructure(form);
            for (FormField groupField : formStructure.get(0)) {
                Concept groupConcept = groupField.getField().getConcept();
                if (groupConcept == null) {
                    throw new ConfigurationException("Chart %s has non-concept top-level field %s",
                            form.getUuid(), groupField.getField().getName());
                }
                SimpleObject group = new SimpleObject();
                group.put(UUID, groupConcept.getUuid());
                List<String> groupFieldConceptIds = new ArrayList<>();
                for (FormField fieldInGroup : formStructure.get(groupField.getId())) {
                    Concept fieldConcept = fieldInGroup.getField().getConcept();
                    if (fieldConcept == null) {
                        throw new ConfigurationException("Chart %s has non-concept subfield %s",
                                form.getUuid(), fieldInGroup.getField().getName());
                    }
                    groupFieldConceptIds.add(fieldConcept.getUuid());
                }
                group.put(CONCEPTS, groupFieldConceptIds);
                groups.add(group);
            }
            chart.put(GROUPS, groups);
        }
        return chart;
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }

    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        // Searchable is only implemented as a workaround to RESTWS-471
        throw new UnsupportedOperationException("Searching not supported");
    }

    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        String[] uuids = Context.getAdministrationService()
                .getGlobalProperty(GlobalProperties.CHART_UUIDS)
                .split(",");
        List<SimpleObject> jsonResults = new ArrayList<>();
        for (String uuid : uuids) {
            Form form = formService.getFormByUuid(uuid);
            if (form == null) {
                throw new ConfigurationException("Configured chart UUIDs incorrect - can't find form " + uuid);
            }
            jsonResults.add(formToJson(form, context.getRepresentation()));
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }
}
