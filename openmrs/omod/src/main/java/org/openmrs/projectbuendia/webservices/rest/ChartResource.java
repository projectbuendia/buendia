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

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.ConceptService;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.util.FormUtil;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST resource for charts. These are stored as OpenMRS forms, but that's
 * primarily to allow for ease of maintenance (OpenMRS provides an editing UI).
 * @see AbstractReadOnlyResource
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/charts",
    supportedClass = Form.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class ChartResource extends AbstractReadOnlyResource<Form> {
    private static final Pattern COMPRESSIBLE_UUID = Pattern.compile("^([0-9]+)A+$");
    private final FormService formService;
    private final ConceptService conceptService;

    public ChartResource() {
        super("chart", Representation.DEFAULT, Representation.FULL);
        formService = Context.getFormService();
        conceptService = Context.getConceptService();
    }

    /**
     * Retrieves a single form with the given UUID.
     * @param context the request context; specify the URL query parameter
     *                "?v=full" to get a list of the groups and concepts in the form.
     * @see AbstractReadOnlyResource#retrieve(String, RequestContext)
     */
    @Override public Form retrieveImpl(String uuid, RequestContext context, long snapshotTime)
        throws ResponseException {
        Form form = formService.getFormByUuid(uuid);
        return (form == null || form.isRetired()) ? null : form;
    }

    /**
     * Populates the {@link SimpleObject} with 'version' (the version number of the form)
     * and, if details are requested with the query parameter "?v=full", adds the list of
     * chart sections in 'sections' containing the details of each tile or row in the chart.
     * @param context the request context; specify the URL query parameter
     *                "?v=full" to get a list of the groups and concepts in the form.
     */
    @Override
    protected void populateJsonProperties(Form form, RequestContext context, SimpleObject json,
                                          long snapshotTime) {
        json.put("version", form.getVersion());
        if (context.getRepresentation() != Representation.FULL) return;

        List<Map> sections = new ArrayList<>();
        SortedMap<Integer, TreeSet<FormField>> structure = FormUtil.getFormStructure(form);
        for (FormField sectionFormField : structure.get(0)) {
            Field sectionField = sectionFormField.getField();
            Map<String, Object> section = parseFieldDescription(sectionField);
            section.put("label", sectionField.getName());
            List<Map> items = new ArrayList<>();
            for (FormField itemFormField : structure.get(sectionFormField.getId())) {
                Field itemField = itemFormField.getField();
                Map<String, Object> item = parseFieldDescription(itemField);
                item.put("label", itemField.getName());
                item.put("concepts", getConceptUuids(item.get("concepts"), itemField));
                item.remove("concept");
                items.add(item);
            }
            section.put("items", items);
            sections.add(section);
        }
        json.put("sections", sections);
    }

    /**
     * Returns a list of compressed concept UUIDs (see compressUuid), given the "concepts"
     * member of a field's description JSON.  If the "concepts" members is missing, returns
     * the field's concept.  The description is assumed to be correctly formatted, i.e. the
     * concepts argument should be a list of integer IDs of existing concepts.  Otherwise, an
     * uncaught exception will be thrown (as with any other case of internal database corruption).
     */
    private List<Object> getConceptUuids(Object concepts, Field field) {
        if (concepts == null) {
            return Collections.singletonList(compressUuid(field.getConcept().getUuid()));
        }
        // If the casts in this method fail, we want it to produce an uncaught exception and
        // HTTP error 500 (not error 4xx, which is for invalid user input).
        List conceptIds = (List) concepts;
        List<Object> result = new ArrayList<>();
        for (Object item : conceptIds) {
            int id = (Integer) item;
            Concept concept = conceptService.getConcept(id);
            if (concept == null) {
                throw new IllegalStateException("Concept " + id + " not found");
            }
            result.add(compressUuid(concept.getUuid()));
        }
        return result;
    }

    /**
     * Saves a bit of JSON space by using integers to represent UUIDs that are exactly 36
     * consist of an integer followed by a string of "A"s.  All other UUIDs are left as strings.
     */
    private static Object compressUuid(String uuid) {
        Matcher matcher = COMPRESSIBLE_UUID.matcher(uuid);
        if (uuid.length() == 36 && matcher.matches()) {
            return Integer.valueOf(matcher.group(1));
        }
        return uuid;
    }

    private static Map<String, Object> parseFieldDescription(Field field) {
        String description = field.getDescription().trim();
        if (description.startsWith("{\"")) {
            try {
                Map<String, Object> config =
                    new ObjectMapper().readValue(field.getDescription(), Map.class);
                for (String key : config.keySet()) {
                    if (config.get(key) == null || "".equals(config.get(key))) {
                        config.remove(key);
                    }
                }
                return config;
            } catch (IOException e) {
                throw new InvalidObjectDataException(
                    "Invalid JSON in description of field " + field.getId());
            }
        } else return new HashMap<String, Object>();
    }

    /**
     * Returns all charts (there is no support for searching or filtering).
     * @param context the request context; specify the URL query parameter
     *                "?v=full" to get a list of the groups and concepts in each form.
     * @see AbstractReadOnlyResource#search(RequestContext)
     */
    @Override protected Iterable<Form> searchImpl(RequestContext context, long snapshotTime) {
        return getCharts(formService);
    }

    public static List<Form> getCharts(FormService formService) {
        List<Form> charts = new ArrayList<>();
        String[] uuids = Context.getAdministrationService()
            .getGlobalProperty(GlobalProperties.CHART_UUIDS)
            .split(",");
        for (String uuid : uuids) {
            Form form = formService.getFormByUuid(uuid);
            if (form == null || form.isRetired()) {
                continue;
            }
            charts.add(form);
        }
        return charts;
    }
}
