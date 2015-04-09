/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.xforms;

import java.util.Date;
import java.util.UUID;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.User;
import org.openmrs.api.context.Context;


/**
 * This class holds the XForm of a an openmrs form.
 * 
 * @author Daniel Kayiwa
 * 
 */
public class Xform extends BaseOpenmrsObject { 

	/** The xml for the XForm. */
	private String xformXml;
    
    /** The form layout xml. */
    private String layoutXml;
    
    /** The form locale xml. */
    private String localeXml;
	
    /** The form javascript scource. */
    private String javaScriptSrc;
    
    /** The form css scource. */
    private String css;
    
	/** The formId of the form that this XForms represents. */
	private int formId = -1;
	
	/** The user who submitted this XForm to the database. */
	private User creator;
	
	/** The date this XForm was submitted to the database. */
	private Date dateCreated;
    
    /** The user who changed this XForm. */
    private User changedBy;
    
    /** The date this XForm was changed. */
    private Date dateChanged;
    
	
	/**
	 * Default constructor
	 */
	public Xform() {
		this.setUuid(UUID.randomUUID().toString());
	}
    
    /**
     * Creates an xform object form a formid with xforms xml.
     * 
     * @param formId - the form id.
     * @param xformXml - xml of the xform.
     * @param layoutXml - xml layout of the form.
     */
    public Xform(Integer formId, String xformXml) {
    	this();
        setFormId(formId);
        setXformXml(xformXml);
        setDateCreated(new Date());
        setCreator(Context.getAuthenticatedUser());
    }
	
	/**
	 * Creates an xform object form a formid with xforms and layout xml.
	 * 
	 * @param formId - the form id.
	 * @param xformXml - xml of the xform.
     * @param layoutXml - xml layout of the form.
     * @param localeXml - xml for form localization.
	 */
	public Xform(Integer formId, String xformXml, String layoutXml, String localeXml, String javaScriptSrc, String css) {
        this(formId,xformXml);
        setLayoutXml(layoutXml);
        setLocaleXml(localeXml);
        setJavaScriptSrc(javaScriptSrc);
        setCss(css);
		setCreator(Context.getAuthenticatedUser());
	}
	
	/**
	 * @return Returns the creator.
	 */
	public User getCreator() {
		return creator;
	}

	/**
	 * @param creator The creator to set.
	 */
	public void setCreator(User creator) {
		this.creator = creator;
	}

	/**
	 * @return Returns the dateCreated.
	 */
	public Date getDateCreated() {
		return dateCreated;
	}

	/**
	 * @param dateCreated The dateCreated to set.
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	/**
     * @return the formId
     */
    public int getFormId() {
    	return formId;
    }

	/**
     * @param formId the id of the form to set
     */
    public void setFormId(int formId) {
    	this.formId = formId;
    }

    /**
     * 
     * Gets the user who changed the form.
     * 
     * @return
     */
    public User getChangedBy() {
        return changedBy;
    }

    /**
     * 
     * Sets the user who changed the form.
     * 
     * @param changedBy
     */
    public void setChangedBy(User changedBy) {
        this.changedBy = changedBy;
    }

    /**
     * 
     * Gets the date when the form was changed.
     * 
     * @return
     */
    public Date getDateChanged() {
        return dateChanged;
    }

    
    /**
     * 
     * Sets the date when the form was changed.
     * 
     * @param dateChanged
     */
    public void setDateChanged(Date dateChanged) {
        this.dateChanged = dateChanged;
    }

    /**
     * 
     * Gets the form layout xml
     * 
     * @return
     */
    public String getLayoutXml() {
        return layoutXml;
    }

    /**
     * 
     * Sets the form layout xml
     * 
     * @param layoutXml
     */
    public void setLayoutXml(String layoutXml) {
        this.layoutXml = layoutXml;
    }

    /**
     * 
     * Gets the xforms xml
     * 
     * @return
     */
    public String getXformXml() {
        return xformXml;
    }

    /**
     * 
     * Sets the xforms xml.
     * 
     * @param xformXml
     */
    public void setXformXml(String xformXml) {
        this.xformXml = xformXml;
    }

	public String getLocaleXml() {
		return localeXml;
	}

	public void setLocaleXml(String localeXml) {
		this.localeXml = localeXml;
	}

	public String getJavaScriptSrc() {
		return javaScriptSrc;
	}

	public void setJavaScriptSrc(String javaScriptSrc) {
		this.javaScriptSrc = javaScriptSrc;
	}
	
    public String getCss() {
    	return css;
    }

    public void setCss(String css) {
    	this.css = css;
    }

	@Override
	public Integer getId() {
		return getFormId();
	}

	@Override
	public void setId(Integer id) {
		setFormId(id);
	}
}
