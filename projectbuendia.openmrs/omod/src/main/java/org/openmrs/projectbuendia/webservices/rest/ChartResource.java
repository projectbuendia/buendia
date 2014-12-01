package org.openmrs.projectbuendia.webservices.rest;

import java.util.ArrayList;
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
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.util.FormUtil;
import org.projectbuendia.openmrs.webservices.rest.RestController;

/**
 * REST resource for charts. These are stored as OpenMRS forms, but that's primarily
 * to allow for ease of maintenance.
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/chart", supportedClass = Form.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class ChartResource extends AbstractReadOnlyResource<Form> {
    
    private static final String GROUPS = "groups";
    private static final String VERSION = "version";
    private static final String CONCEPTS = "concepts";
        
    private final FormService formService;
    
    public ChartResource() {
        super("chart", Representation.DEFAULT, Representation.FULL);
        formService = Context.getFormService();
    }

    @Override
    public Form retrieveImpl(String uuid, RequestContext context) throws ResponseException {
        return formService.getFormByUuid(uuid);
    }
    
    @Override
    protected void populateJsonProperties(Form form, RequestContext context, SimpleObject json) {
        json.put(VERSION, form.getVersion());
        if (context.getRepresentation() != Representation.FULL) {
            return;
        }
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
        json.put(GROUPS, groups);
    }
    
    @Override
    protected Iterable<Form> searchImpl(RequestContext context) {
        return getCharts(formService);
    }
    
    static List<Form> getCharts(FormService formService) {
        List<Form> charts = new ArrayList<>();
        String[] uuids = Context.getAdministrationService()
                .getGlobalProperty(GlobalProperties.CHART_UUIDS)
                .split(",");
        for (String uuid : uuids) {
            Form form = formService.getFormByUuid(uuid);
            if (form == null) {
                throw new ConfigurationException("Configured chart UUIDs incorrect - can't find form " + uuid);
            }
            charts.add(form);
        }
        return charts;
    }
}
