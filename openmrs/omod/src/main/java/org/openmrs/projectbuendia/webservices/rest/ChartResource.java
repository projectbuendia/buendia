package org.openmrs.projectbuendia.webservices.rest;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.FormService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.util.FormUtil;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openmrs.projectbuendia.Utils.isEmpty;

@Resource(
    name = RestController.REST_VERSION_1_AND_NAMESPACE + "/charts",
    supportedClass = Form.class,
    supportedOpenmrsVersions = "1.10.*,1.11.*"
)
public class ChartResource extends BaseResource<Form> {
    private static final Pattern COMPRESSIBLE_UUID = Pattern.compile("^([0-9]+)A+$");


    public ChartResource() {
        super("charts", Representation.DEFAULT, Representation.FULL);
    }

    @Override protected Collection<Form> listItems(RequestContext context) {
        return getChartForms(formService);
    }

    public static Collection<Form> getChartForms(FormService formService) {
        List<Form> charts = new ArrayList<>();
        for (Form form : formService.getAllForms()) {
            if (DbUtils.isChartForm(form)) {
                charts.add(form);
            }
        }
        return charts;
    }

    @Override protected Form retrieveItem(String uuid) {
        return formService.getFormByUuid(uuid);
    }

    @Override protected void populateJson(SimpleObject json, Form form, RequestContext context) {
        json.put("version", form.getVersion());
        if (context.getRepresentation() != Representation.FULL) return;

        List<Map> sections = new ArrayList<>();
        SortedMap<Integer, TreeSet<FormField>> structure = FormUtil.getFormStructure(form);
        // Chart definitions are assumed to have a three-level structure: a single
        // root FormField, whose children are sections, whose children are chart rows.
        for (FormField sectionFormField : structure.get(0)) {
            Field sectionField = sectionFormField.getField();
            Map<String, Object> section = parseFieldDescription(sectionField);
            section.put("label", sectionField.getName());
            List<Map> items = new ArrayList<>();
            // The FormFields in the TreeSet are in display order; return them in that order.
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

    private static Map<String, Object> parseFieldDescription(Field field) {
        String description = field.getDescription().trim();
        if (description.startsWith("{\"")) {
            try {
                Map<String, Object> config =
                    new ObjectMapper().readValue(field.getDescription(), Map.class);
                for (String key : config.keySet()) {
                    if (isEmpty(config.get(key))) {
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
    public static Object compressUuid(String uuid) {
        Matcher matcher = COMPRESSIBLE_UUID.matcher(uuid);
        if (uuid.length() == 36 && matcher.matches()) {
            return Integer.valueOf(matcher.group(1));
        }
        return uuid;
    }

}
