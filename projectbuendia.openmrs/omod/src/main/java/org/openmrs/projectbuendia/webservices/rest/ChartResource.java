// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.openmrs.projectbuendia.webservices.rest;

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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * REST resource for charts. These are stored as OpenMRS forms, but that's
 * primarily to allow for ease of maintenance (OpenMRS provides an editing UI).
 *
 * @see AbstractReadOnlyResource
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/chart",
        supportedClass = Form.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class ChartResource extends AbstractReadOnlyResource<Form> {

    private static final String GROUPS = "groups";
    private static final String VERSION = "version";
    private static final String CONCEPTS = "concepts";

    private final FormService formService;

    public ChartResource() {
        super("chart", Representation.DEFAULT, Representation.FULL);
        formService = Context.getFormService();
    }

    /**
     * Retrieves a single form with the given UUID.
     *
     * @see AbstractReadOnlyResource#retrieve(String, RequestContext)
     * @param context the request context; specify the URL query parameter
     *     "?v=full" to get a list of the groups and concepts in the form.
     */
    @Override
    public Form retrieveImpl(String uuid, RequestContext context, long snapshotTime)
            throws ResponseException {
        return formService.getFormByUuid(uuid);
    }

    /**
     * Adds the following fields to the {@link SimpleObject}:
     * <ul>
     *     <li>"version": the version number (e.g. 0.2.3) of the form
     * </ul>
     *
     * <p>If details are requested with the URL query parameter "?v=full",
     * also adds the following fields to the {@link SimpleObject}:
     * <ul>
     * <li>"groups": a {@link List} of {@link SimpleObject}s, each containing:
     *   <ul>
     *   <li>"uuid": the UUID for the concept representing the group (note: when
     *       defining groups in OpenMRS for charts returned by this endpoint,
     *       each group MUST be represented by a concept or this will fail)
     *   <li>"concepts": a {@link List} of UUIDs of the concepts in the group
     *   </ul>
     * </ul>
     *
     * @param context the request context; specify the URL query parameter
     *     "?v=full" to get a list of the groups and concepts in the form.
     */
    @Override
    protected void populateJsonProperties(Form form, RequestContext context, SimpleObject json, long snapshotTime) {
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
            group.put("uuid", groupConcept.getUuid());
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

    /**
     * Returns all charts (there is no support for searching or filtering).
     *
     * @see AbstractReadOnlyResource#search(RequestContext)
     * @param context the request context; specify the URL query parameter
     *     "?v=full" to get a list of the groups and concepts in each form.
     */
    @Override
    protected Iterable<Form> searchImpl(RequestContext context, long snapshotTime) {
        return getCharts(formService);
    }

    public static List<Form> getCharts(FormService formService) {
        List<Form> charts = new ArrayList<>();
        String[] uuids = Context.getAdministrationService()
                .getGlobalProperty(GlobalProperties.CHART_UUIDS)
                .split(",");
        for (String uuid : uuids) {
            Form form = formService.getFormByUuid(uuid);
            if (form == null) {
                throw new ConfigurationException(GlobalProperties.CHART_UUIDS +
                    " property is incorrect; cannot find form " + uuid);
            }
            charts.add(form);
        }
        return charts;
    }
}
