package org.projectbuendia.openmrs.webservices.rest;

import org.openmrs.module.xforms.download.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Listable;
import org.openmrs.module.webservices.rest.web.resource.api.Retrievable;
import org.openmrs.module.webservices.rest.web.resource.api.Searchable;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/xform", supportedClass = SimpleObject.class, supportedOpenmrsVersions = "1.10.*")
public class XformResource implements Listable, Retrievable, Searchable {

	private XformDownloadManager xformDownloadManager;
	
    @Override
    public SimpleObject getAll(RequestContext context) throws ResponseException {
        List<SimpleObject> jsonResults = new ArrayList<>();
        SimpleObject list = new SimpleObject();
        list.add("results", jsonResults);
        return list;
    }
    
    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT, Representation.FULL, Representation.REF);
    }

    @Override
    public String getUri(Object instance) {
    	return "";
    	/*
        if (instance == null) {
            return "";
        }
        
        Resource res = getClass().getAnnotation(Resource.class);
        Demo demo = (Demo) instance;

        return RestConstants.URI_PREFIX + res.name() + "/" + demo.getUniqueId();
        */
    }

    @Override
    public Object retrieve(String uuid, RequestContext context) throws ResponseException {
        UUID id = UUID.fromString(uuid);
        throw new ObjectNotFoundException();
    }

    @Override
    public SimpleObject search(RequestContext context) throws ResponseException {
        throw new UnsupportedOperationException();
    }
}
