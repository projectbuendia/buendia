package org.openmrs.projectbuendia.webservices.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.Hyperlink;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.xforms.XformBuilderEx;
import org.projectbuendia.openmrs.webservices.rest.RestController;

/**
 * Resource for xform templates (i.e. forms without data).
 * Note: this is under org.openmrs as otherwise the resource annotation isn't picked up. 
 */
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xform", supportedClass = Form.class, supportedOpenmrsVersions = "1.10.*")
public class XformResource implements Listable, Retrievable, Searchable {

	private final Log log = LogFactory.getLog(getClass());

	private FormService formService;
	
	public XformResource() {
		this.formService = Context.getFormService();
	}
	
    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        List<SimpleObject> jsonResults = new ArrayList<>();
        List<Form> forms = formService.getAllForms(false);
        for (Form form : forms) {
        	SimpleObject jsonForm = toSimpleObject(form, context.getRepresentation());
        	jsonResults.add(jsonForm);
        }
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }

	private SimpleObject toSimpleObject(Form form, Representation representation) {
		SimpleObject jsonForm = new SimpleObject();
		jsonForm.add("name", form.getName());
		jsonForm.add("date_changed", form.getDateChanged());
		jsonForm.add("date_created", form.getDateCreated());
		jsonForm.add("version", form.getVersion());
		jsonForm.add("uuid", form.getUuid());
		jsonForm.add("links", getLinks(form));
		if (representation == Representation.FULL) {
			try {
				jsonForm.add("xml", XformBuilderEx.buildXform(form));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return jsonForm;
	}
    
    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT, Representation.FULL, Representation.REF);
    }

    private List<Hyperlink> getLinks(Form form) {
        List<Hyperlink> links = new ArrayList<>();        
        links.add(new Hyperlink("self", getUri(form)));
        return links;
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
    	return toSimpleObject(form, context.getRepresentation());
    }

    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        throw new UnsupportedOperationException();
    }
}
