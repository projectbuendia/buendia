package org.msf.projectbuendia.openmrs;

import java.io.Serializable;
import org.openmrs.BaseOpenmrsObject;
import org.openmrs.BaseOpenmrsMetadata;

/**
 * Model class, which is currently unused. This only exists to give us a
 * sample to extend if/when we need to store our own model classes in OpenMRS.
 */
public class ProjectBuendia extends BaseOpenmrsObject implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Integer id;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
}