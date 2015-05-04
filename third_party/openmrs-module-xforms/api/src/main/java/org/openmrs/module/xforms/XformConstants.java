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

/**
 * This class holds constants used in more than one class in the xforms module.
 * 
 * @author Daniel
 *
 */
public class XformConstants {
	//TODO More constants need to be put in this class from the various classes where they are scattered.
	
	/** 
	 * The name of the global property for the directory where to put xforms that are not submitted to the formentry queue
	 * because of errors.
	 */
	public static final String XFORMS_ERROR_DIR = "xforms.error_dir";
	
	/** The default xforms error directory. */
	public static final String XFORMS_ERROR_DIR_DEFAULT = "xforms/error";
	
	/** The name of the global property for the directory for queuing xforms before they are processed. */
	public static final String XFORMS_QUEUE_DIR = "xforms.queue_dir";
	
	/** The default xforms queue directory. */
	public static final String XFORMS_QUEUE_DIR_DEFAULT = "xforms/queue";
	
	/** The name of the global property for the name of the global property for the directory for storing complex obs. */
	public static final String XFORMS_COMPLEX_OBS_DIR = "xforms.complexobs_dir";
	
	/** The default xforms complex obs directory. */
	public static final String XFORMS_COMPLEX_OBS_DIR_DEFAULT = "xforms/complexobs";
	
	/** 
	 * The dirrectory for archiving xforms after submission to the formentry queue.
	 * The reason for archiving xforms, even after knowing that the formentry module
	 * will also archive them is that, some processing is done on these xforms to make
	 * them consumable by the formentry module, and we want users to always be able to
	 * see how the xform looked like at submission, just incase of any issues or bugs
	 * in the xforms processing.
	 */
	public static final String XFORMS_ARCHIVE_DIR = "xforms.archive_dir";
	
	/** The default xforms archive dirrectory. */
	public static final String XFORMS_ARCHIVE_DIR_DEFAULT = "xforms/archive/%Y/%M";

	/** 
	 * XForms are linked to their correponding openmrs forms by the form_id field. 
	 * For clients to create a new patient on say mobile xform clients, the new patient
	 * is also created as an xform which has to be differentiated from the rest of 
	 * the forms. So as for now, we user this form_id on the assumption that no
	 * openmrs form will have it. This may not be the best way for handling this,
	 * and hence may need further thoughts, but works for now.
	 */
	public static final int PATIENT_XFORM_FORM_ID = 0;
	
	/** The default character encoding used when writting and reading bytes to and from streams. */
	public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";
	
	/** The default submit date format. */
	public static final String DEFAULT_DATE_SUBMIT_FORMAT = "yyyy-MM-dd";//yyyy-mm-dd
	
	/** The default submit datetime format. */
	public static final String DEFAULT_DATE_TIME_SUBMIT_FORMAT = "yyyy-MM-dd hh:mm a";
	
	/** The default submit time format. */
	public static final String DEFAULT_TIME_SUBMIT_FORMAT = "hh:mm a";
	
	/** The default display date format. */
	public static final String DEFAULT_DATE_DISPLAY_FORMAT = "dd-MM-yyyy";//yyyy-mm-dd
	
	/** The default display date format. */
	public static final String DEFAULT_DATE_TIME_DISPLAY_FORMAT = "dd-MM-yyyy hh:mm a";
	
	/** The default display date format. */
	public static final String DEFAULT_TIME_DISPLAY_FORMAT = "hh:mm a";
	
	/** The default flag to determine whether to display the submit success message or not. */
	public static final String DEFAULT_SHOW_SUBMIT_SUCCESS_MSG = "false";
	
	/** The default value for the decimal separators. */
	public static final String DEFAULT_DECIMAL_SEPARATORS = "en:.;fr:.;es:,;it:.;pt:.";
	
	/** The default font family. */
	public static final String DEFAULT_FONT_FAMILY = "Verdana, 'Lucida Grande', 'Trebuchet MS', Arial, Sans-Serif";
		
	/** The default font size. */
	public static final String DEFAULT_FONT_SIZE = "16";
		
	/** The default locale list. */
	public static final String DEFAULT_LOCALE_LIST = "en:English,fr:French,gr:German,swa:Swahili";
	
	/** The default value for rejecting forms for patients considered new when they already exist, by virture of patient identifier. */
	public static final String DEFAULT_REJECT_EXIST_PATIENT_CREATE = "true";
	
	/** The default value for allowing bind editing. */
	public static final String DEFAULT_ALLOW_BIND_EDIT = "false";
	
	/** The global property key for rejecting forms for patients considered new when they already exist, by virture of patient identifier. */
	public static final String GLOBAL_PROP_KEY_REJECT_EXIST_PATIENT_CREATE = "xforms.rejectExistingPatientCreation";
	
	/** The global property key for the date submit format.*/
	public static final String GLOBAL_PROP_KEY_DATE_SUBMIT_FORMAT = "xforms.dateSubmitFormat";
	
	/** The global property key for the datetime submit format.*/
	public static final String GLOBAL_PROP_KEY_DATE_TIME_SUBMIT_FORMAT = "xforms.dateTimeSubmitFormat";
	
	/** The global property key for the time submit format.*/
	public static final String GLOBAL_PROP_KEY_TIME_SUBMIT_FORMAT = "xforms.timeSubmitFormat";
	
	/** The global property key for the date display format.*/
	public static final String GLOBAL_PROP_KEY_DATE_DISPLAY_FORMAT = "xforms.dateDisplayFormat";
	
	/** The global property key for the datetime display format.*/
	public static final String GLOBAL_PROP_KEY_DATE_TIME_DISPLAY_FORMAT = "xforms.dateTimeDisplayFormat";
	
	/** The global property key for the time display format.*/
	public static final String GLOBAL_PROP_KEY_TIME_DISPLAY_FORMAT = "xforms.timeDisplayFormat";
	
	/** The global property key for the time display format.*/
	public static final String GLOBAL_PROP_KEY_SHOW_SUBMIT_SUCCESS_MSG = "xforms.showSubmitSuccessMsg";
	
	/** The global property key for the decimal separators.*/
	public static final String GLOBAL_PROP_KEY_DECIMAL_SEPARATORS = "xforms.decimalSeparators";
	
	/** The global property key for the current locale.*/
	public static final String GLOBAL_PROP_KEY_LOCALE = "xforms.locale";
	
	/** The global property key for the default font family.*/
	public static final String GLOBAL_PROP_KEY_DEFAULT_FONT_FAMILY = "xforms.defaultFontFamily";
	
	/** The global property key for the default font size.*/
	public static final String GLOBAL_PROP_KEY_DEFAULT_FONT_SIZE = "xforms.defaultFontSize";
	
	/** The global property key for the list of locales.*/
	public static final String GLOBAL_PROP_KEY_LOCALE_LIST = "xforms.localeList";
	
	/** The global property key for allowing editing of bindings.*/
	public static final String GLOBAL_PROP_KEY_ALLOW_BIND_EDIT = "xforms.allowBindEdit";
	
	/** The form designer key for the date submit format.*/
	public static final String FORM_DESIGNER_KEY_DATE_SUBMIT_FORMAT = "dateSubmitFormat";
	
	/** The form designer key for determining whether to show the form submit message or not.*/
	public static final String FORM_DESIGNER_KEY_SHOW_SUBMIT_SUCCESS_MSG = "showSubmitSuccessMsg";
	
	/** The form designer key for the decimal separators.*/
	public static final String FORM_DESIGNER_KEY_DECIMAL_SEPARATORS = "decimalSeparators";
	
	/** The form designer key for the current locale.*/
	public static final String FORM_DESIGNER_KEY_LOCALE_KEY = "localeKey";
	
	/** The form designer key for the datetime submit format.*/
	public static final String FORM_DESIGNER_KEY_DATE_TIME_SUBMIT_FORMAT = "dateTimeSubmitFormat";
	
	/** The form designer key for the time submit format.*/
	public static final String FORM_DESIGNER_KEY_TIME_SUBMIT_FORMAT = "timeSubmitFormat";
	
	/** The form designer key for the date display format.*/
	public static final String FORM_DESIGNER_KEY_DATE_DISPLAY_FORMAT = "dateDisplayFormat";
	
	/** The form designer key for the datetime display format.*/
	public static final String FORM_DESIGNER_KEY_DATE_TIME_DISPLAY_FORMAT = "dateTimeDisplayFormat";
	
	/** The form designer key for the time display format.*/
	public static final String FORM_DESIGNER_KEY_TIME_DISPLAY_FORMAT = "timeDisplayFormat";
	
	/** The form designer key for the default font family.*/
	public static final String FORM_DESIGNER_KEY_DEFAULT_FONT_FAMILY = "defaultFontFamily";
	
	/** The form designer key for the default group box header background color.*/
	public static final String FORM_DESIGNER_KEY_DEFAULT_GROUPBOX_HEADER_BG_COLOR = "defaultGroupBoxHeaderBgColor";
	
	/** The form designer key for the default font size.*/
	public static final String FORM_DESIGNER_KEY_DEFAULT_FONT_SIZE = "defaultFontSize";
	
	/** The form designer key for the list of locales.*/
	public static final String FORM_DESIGNER_KEY_LOCALE_LIST = "localeList";
	
	/** The global property key for the user serializer class.*/
	public static final String GLOBAL_PROP_KEY_USER_SERIALIZER= "xforms.userSerializer";
		
	/** The global property key for the patient serializer class.*/
	public static final String GLOBAL_PROP_KEY_PATIENT_SERIALIZER = "xforms.patientSerializer";
    
    /** The global property key for the cohort serializer class.*/
    public static final String GLOBAL_PROP_KEY_COHORT_SERIALIZER = "xforms.cohortSerializer";
    
    /** The global property key for the saved search serializer class.*/
    public static final String GLOBAL_PROP_KEY_SAVED_SEARCH_SERIALIZER = "xforms.savedSearchSerializer";
	
	/** The global property key for the xform serializer class.*/
	public static final String GLOBAL_PROP_KEY_XFORM_SERIALIZER = "xforms.xformSerializer";
	
	/** The global property key for the xform select1 appearance. */
	public static final String GLOBAL_PROP_KEY_SINGLE_SELECT_APPEARANCE = "xforms.singleSelectAppearance";
	
	/** The global property key for determining whether to use stored xforms or build new ones on the fly. */
	public static final String GLOBAL_PROP_KEY_USER_STORED_XFORMS = "xforms.useStoredXform";
	
	/** The global property key for determining whether to include users when downloading xforms. */
	public static final String GLOBAL_PROP_KEY_INCLUDE_USERS_IN_XFORMS_DOWNLOAD = "xforms.includeUsersInXformsDownload";

	/** The global property key for the patient download cohort.*/
	public static final String GLOBAL_PROP_KEY_PATIENT_DOWNLOAD_COHORT = "xforms.patientDownloadCohort";
	
	public static final String GLOBAL_PROP_KEY_USE_PATIENT_XFORM = "xforms.usePatientXform";

	public static final String GLOBAL_PROP_KEY_USE_ENCOUNTER_XFORM = "xforms.useEncounterXform";
	
	public static final String GLOBAL_PROP_KEY_PREFERRED_CONCEPT_SOURCE = "xforms.preferredConceptSource";
	
	public static final String GLOBAL_PROP_KEY_ADMIN_EMAIL = "xforms.adminEmail";
	
	public static final String GLOBAL_PROP_KEY_EMAIL_SERVER_CONFIG = "xforms.emailServerConfig";
	
	/**
	 * The global property key for determining whether to always include patient relationships in forms.
	 * This effectively defaults to "true" - if set to "false", then the relationship nodes will *not* be added.
	 */
	public static final String GLOBAL_PROP_KEY_INCLUDE_PATIENT_RELATIONSHIPS = "xforms.includePatientRelationships";

	/** The default value for the user serializer class.*/
	public static final String DEFAULT_USER_SERIALIZER= "org.openmrs.module.xforms.serialization.DefaultUserSerializer";
	
	/** The default value for the patient serializer class.*/
	public static final String DEFAULT_PATIENT_SERIALIZER = "org.openmrs.module.xforms.serialization.DefaultPatientSerializer";
	
    /** The default value for the cohort serializer class.*/
    public static final String DEFAULT_COHORT_SERIALIZER = "org.openmrs.module.xforms.serialization.DefaultCohortSerializer";
    
    /** The default value for the saved search serializer class.*/
    public static final String DEFAULT_SAVED_SEARCH_SERIALIZER = "org.openmrs.module.xforms.serialization.DefaultSavedSearchSerializer";

	/** The default value for the xform serializer class.*/
	public static final String DEFAULT_XFORM_SERIALIZER = "org.openmrs.module.xforms.serialization.DefaultXformSerializer";
	
	/** The session form node. */
	public static final String NODE_SESSION = "session";
	
	/** The uid form node. */
	public static final String NODE_UID = "uid";
	
	/** The date_entered form node. */
	public static final String NODE_DATE_ENTERED = "date_entered";
	
	/** The enterer form node. */
	public static final String NODE_ENTERER = "enterer";
	
	/** The extension for our xforms. */
	public static final String XFORM_FILE_EXTENSION = ".xml";
	
	/** The extension for xslt documents. */
	public static final String XSLT_FILE_EXTENSION = ".xml";
	
	/** The extension for xml files. */
	public static final String XML_FILE_EXTENSION = ".xml";
	
	/** The content disposition http header. */
	public static final String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	
	/** The content type http header. */
	public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
	
	/** The starter xform file name. */
	public static final String STARTER_XFORM = "starter_xform.xml";
	
	/** The starter xform file name. */
	public static final String STARTER_XSLT = "starter_xslt.xml";
	
	/** The empty string constant. */
	public static final String EMPTY_STRING = "";
	
	public static final String HTTP_HEADER_CONTENT_DISPOSITION_VALUE = "attachment; filename=";
	
	/** The application/xhtml+xml http content type. */
	public static final String HTTP_HEADER_CONTENT_TYPE_XHTML_XML = "application/xhtml+xml; charset=utf-8";
	
    /** The text/xml http content type. */
    public static final String HTTP_HEADER_CONTENT_TYPE_XML = "text/xml; charset=utf-8";

	/** The text value for boolean true. */
	public static final String TRUE_TEXT_VALUE = "true";
	
	/** The text value for boolean false. */
	public static final String FALSE_TEXT_VALUE = "false";
	
	/** The patientId request parameter. */
	public static final String REQUEST_PARAM_PATIENT_ID = "patientId";
	
	/** The formId request parameter. */
	public static final String REQUEST_PARAM_FORM_ID = "formId";
	
	/** The batchEntry request parameter. */
	public static final String REQUEST_PARAM_BATCH_ENTRY = "batchEntry";
	
	/** The xformentry request parameter. */
	public static final String REQUEST_PARAM_XFORM_ENTRY = "xformentry";
	
	/** The phrase request parameter. */
	public static final String REQUEST_PARAM_PATIENT_SEARCH_PHRASE = "phrase";
	
	/** The xforms request parameter. */
	public static final String REQUEST_PARAM_XFORMS = "xforms";
	
	/** The xforms list request parameter. */
	public static final String REQUEST_PARAM_XFORMS_LIST = "xformslist";
	
	/** The xform request parameter. */
	public static final String REQUEST_PARAM_XFORM = "xform";
    
    /** The xform refresh request parameter. */
    public static final String REQUEST_PARAM_XFORMREFRESH = "xformrefresh";
    
    /** The layout request parameter. */
    public static final String REQUEST_PARAM_LAYOUT = "layout";
	
	/** The xslt request parameter. */
	public static final String REQUEST_PARAM_XSLT = "xslt";
		
	/** The target request parameter. */
	public static final String REQUEST_PARAM_TARGET = "target";
    
    /** The contentType request parameter. */
    public static final String REQUEST_PARAM_CONTENT_TYPE = "contentType";
    
    /** The contentType value xml. */
    public static final String REQUEST_PARAM_CONTENT_TYPE_VALUE_XML = "xml";
	
	/** The cohortId request parameter. */
	public static final String REQUEST_PARAM_INCLUDE_USERS = "includeUsers";
	
	/** The cohortId request parameter. */
	public static final String REQUEST_PARAM_COHORT_ID = "cohortId";
	
	/** The downloadPatients request parameter. */
	public static final String REQUEST_PARAM_DOWNLOAD_PATIENTS = "downloadPatients";
    
    /** The downloadCohorts request parameter. */
    public static final String REQUEST_PARAM_DOWNLOAD_COHORTS = "downloadCohorts";
	
	/** The setCohort request parameter. */
	public static final String REQUEST_PARAM_SET_COHORT = "setCohort";
	
	/** The uploadPatientXform request parameter. */
	public static final String REQUEST_PARAM_UPLOAD_PATIENT_XFORM = "uploadPatientXform";
	
	/** The patientXformFile request parameter. */
	public static final String REQUEST_PARAM_PATIENT_XFORM_FILE = "patientXformFile";
	
	/** The downloadPatientXform request parameter. */
	public static final String REQUEST_PARAM_DOWNLOAD_PATIENT_XFORM = "downloadPatientXform";
	
	/** The relative url for xforms data uploads. */
	public static final String XFORM_DATA_UPLOAD_RELATIVE_URL = "/moduleServlet/xforms/xformDataUpload";

    public static final String PURCFORMS_FORMDEF_LAYOUT_XML_SEPARATOR = " PURCFORMS_FORMDEF_LAYOUT_XML_SEPARATOR ";
    
    public static final String PURCFORMS_FORMDEF_LOCALE_XML_SEPARATOR = " PURCFORMS_FORMDEF_LOCALE_XML_SEPARATOR ";

    /** The separator between the xforms xml and javascript source in a combined xml document for a form. */
	public static final String PURCFORMS_FORMDEF_JAVASCRIPT_SRC_SEPARATOR = " PURCFORMS_FORMDEF_JAVASCRIPT_SRC_SEPARATOR ";

    /** The separator between the xforms xml and css source in a combined xml document for a form. */
	public static final String PURCFORMS_FORMDEF_CSS_SEPARATOR = " PURCFORMS_FORMDEF_CSS_SEPARATOR ";

	public static final String HEADER_PURCFORMS_ERROR_MESSAGE = "PURCFORMS-ERROR-MESSAGE";
	
	/**
	 * Used in hl7: 123^concept name^99DCT
	 */
	public static final String HL7_LOCAL_CONCEPT = "99DCT";
	
	public static final String REQUEST_ATTRIBUTE_ID_ERROR_MESSAGE = "ERROR_MESSAGE";
	
	public static final String REQUEST_ATTRIBUTE_ID_PATIENT_ID = "PATIENT_ID";
	
	public static final String REQUEST_ATTRIBUTE_ACCEPT_PATIENT_UUID = "ACCEPT_PATIENT_UUID";

	public static final String XFORM_XSLT_FORM_RESOURCE_NAME_SUFFIX = ".xFormXslt";
	
	//Specifies that when creating a new xform the location selection element should use autocomplete,
	//Note that this only applies for OpenMRS Versions 1.8 and above
	public static final String XFORM_GP_USE_AUTOCOMPLETE_FOR_LOCATIONS = "xforms.useAutocompleteForLocations";
	
	//Specifies that when creating a new xform the provider selection element should use autocomplete,
	//Note that this only applies for OpenMRS Versions 1.9 and above
	public static final String XFORM_GP_USE_AUTOCOMPLETE_FOR_PROVIDERS = "xforms.useAutocompleteForProviders";
}
