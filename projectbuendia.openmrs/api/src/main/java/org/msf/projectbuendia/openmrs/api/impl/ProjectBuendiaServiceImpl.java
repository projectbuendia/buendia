package org.msf.projectbuendia.openmrs.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.msf.projectbuendia.openmrs.api.ProjectBuendiaService;
import org.msf.projectbuendia.openmrs.api.db.ProjectBuendiaDAO;

/**
 * It is a default implementation of {@link ProjectBuendiaService}.
 */
public class ProjectBuendiaServiceImpl extends BaseOpenmrsService implements ProjectBuendiaService {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private ProjectBuendiaDAO dao;
	
	/**
     * @param dao the dao to set
     */
    public void setDao(ProjectBuendiaDAO dao) {
	    this.dao = dao;
    }
    
    /**
     * @return the dao
     */
    public ProjectBuendiaDAO getDao() {
	    return dao;
    }
}