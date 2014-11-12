package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.api.Creatable;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.xforms.XformsQueueProcessor;

/**
 * Resource for instances of xforms (i.e. filled in forms). Currently write-only
 */
public class XformInstanceResource implements Creatable {

    private static final XformsQueueProcessor processor = new XformsQueueProcessor();

    @Override
    public String getUri(Object instance) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object create(SimpleObject post, RequestContext context)
            throws ResponseException {
        String xml = (String) post.get("xml");
        try {
            processor.processXForm(xml, null, true, context.getRequest());
        } catch (Exception e) {
            throw new ConversionException("Error processing xform data", e);
        }
        // FIXME
        return post;
    }
}
