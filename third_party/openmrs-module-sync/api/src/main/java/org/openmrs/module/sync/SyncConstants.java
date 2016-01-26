/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync;

import java.text.SimpleDateFormat;
import java.util.EnumSet;

import org.openmrs.module.sync.scheduler.CleanupSyncTablesTask;

/**
 * Common sync constants
 */
public class SyncConstants {
	
	public static final SimpleDateFormat SYNC_FILENAME_MASK = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_S"); //used to format file names
	
	public static final String LAST_SYNC_LOCAL = "sync.last_sync_local";
	
	public static final String LAST_SYNC_REMOTE = "sync.last_sync_remote";
	
	public static final String DATA_IMPORT_SERVLET = "/module/sync/import.list";
	
	public static final String TEST_MESSAGE = "test";
	
	public static final String CLONE_MESSAGE = "clone";
	
	public static final String CLONE_DOWNLOAD_MESSAGE = "clone_download";
	
	public static final String CLONE_IMPORT_FILE_NAME = "db_clone_import_parent_";
	
	public static final String CLONE_EXPORT_FILE_NAME = "db_clone_export_child_";
	
	public static final int CONNECTION_TIMEOUT_MS = 10000; //http connection timeout in milliseconds
	
	public static final SyncRecordState[] SYNC_TO_PARENT_STATES = { SyncRecordState.NEW, SyncRecordState.PENDING_SEND,
	        SyncRecordState.SEND_FAILED, SyncRecordState.SENT, SyncRecordState.SENT_AGAIN, SyncRecordState.FAILED,
	        SyncRecordState.FAILED_AND_STOPPED, //this is here so that we can display the failed record on UI
	//SyncRecordState.REJECTED   SYNC-204
	};
	
	public static final EnumSet<SyncRecordState> SYNC_RECORD_ERROR_STATES = EnumSet.of(SyncRecordState.SEND_FAILED,
	    SyncRecordState.FAILED, SyncRecordState.FAILED_AND_STOPPED, SyncRecordState.REJECTED);
	
	public static final EnumSet<SyncTransmissionState> SYNC_TRANSMISSION_OK_STATES = EnumSet.of(SyncTransmissionState.OK,
	    SyncTransmissionState.OK_NOTHING_TO_DO, SyncTransmissionState.PENDING);

    public static final EnumSet<SyncRecordState> SYNC_RECORD_COMMITTED_STATES = EnumSet.of(SyncRecordState.COMMITTED,
            SyncRecordState.COMMITTED_AND_CONFIRMATION_SENT, SyncRecordState.ALREADY_COMMITTED);

	// error message codes
	public static final String ERROR_NO_RESPONSE = "sync.status.transmission.noResponseError";
	
	public static final String ERROR_TRANSMISSION_CREATION = "sync.status.transmission.createError";
	
	public static final String ERROR_NO_PARENT_DEFINED = "sync.status.transmission.noParentError";
	
	public static final String ERROR_SEND_FAILED = "sync.status.transmission.sendError";
	
	public static final String ERROR_RESPONSE_NOT_UNDERSTOOD = "sync.status.transmission.corruptResponseError";
	
	public static final String ERROR_AUTH_FAILED = "sync.status.transmission.noAuthError";
	
	public static final String ERROR_TX_NOT_UNDERSTOOD = "sync.status.transmission.corruptTxError";
	
	public static final String ERROR_NO_CONNECTION = "sync.status.transmission.noConnectionError";
	
	public static final String ERROR_INVALID_SERVER = "sync.status.transmission.invalidServer";
	
	public static final String ERROR_CANNOT_RUN_PARALLEL = "sync.status.transmission.cannotRunParallel";
	
	// error message codes - at the item/record level
	public static final String ERROR_ITEM_NOT_COMMITTED = "sync.status.item.notCommitted";
	
	public static final String ERROR_ITEM_UUID_NOT_FOUND = "sync.status.item.uuidNotFound";
	
	public static final String ERROR_ITEM_NOCLASS = "sync.status.item.noClassFound";
	
	public static final String ERROR_ITEM_BADXML_MISSING = "sync.status.item.badXml.missing";
	
	public static final String ERROR_ITEM_UNSET_PROPERTY = "sync.status.item.unsetProperty";
	
	public static final String ERROR_ITEM_UNEXPECTED = "sync.status.item.unexpected";
	
	public static final String ERROR_RECORD_UNEXPECTED = "sync.status.record.unexpected";
	
	// error-induced filenames
	public static final String FILENAME_NO_RESPONSE = "no_response_from_server";
	
	public static final String FILENAME_NOT_CREATED = "unable_to_create_transmission";
	
	public static final String FILENAME_NO_PARENT_DEFINED = "no_parent_defined";
	
	public static final String FILENAME_SEND_FAILED = "send_failed";
	
	public static final String FILENAME_RESPONSE_NOT_UNDERSTOOD = "response_not_understood";
	
	public static final String FILENAME_AUTH_FAILED = "not_authenticated";
	
	public static final String FILENAME_TX_NOT_UNDERSTOOD = "transmission_not_understood";
	
	public static final String FILENAME_NO_CONNECTION = "no_connection";
	
	public static final String FILENAME_INVALID_SERVER = "invalid_server";
	
	public static final String FILENAME_TEST = "test";
	
	public static final String UUID_UNKNOWN = "";
	
	public static final String UTF8 = "UTF-8";
	
	public static final String POST_METHOD = "POST";
	
	public static final String SYNC_DATA_FILE_PARAM = "syncDataFile";
	
	//global props
	public static final String PROPERTY_SERVER_UUID = "sync.server_uuid"; //used internally in sync
	
	public static final String PROPERTY_SERVER_NAME = "sync.server_name"; //used for display purposes, nickname
	
	public static final String PROPERTY_NAME_MAX_RETRY_COUNT = "sync.max_retry_count";
	
	public static final String PROPERTY_NAME_MAX_RETRY_COUNT_DEFAULT = "5";
	
	public static final String PROPERTY_SYNC_ADMIN_EMAIL = "sync.admin_email";
	
	public static final String PROPERTY_NAME_MAX_RECORDS_WEB = "sync.max_records.web";
	
	public static final String PROPERTY_NAME_MAX_RECORDS_FILE = "sync.max_records.file";

    public static final String PROPERTY_SYNC_TRANSMISSION_LOG_ENABLED = "sync.transmission.log.enabled";
	
	public static final String PROPERTY_NAME_MAX_RECORDS_DEFAULT = "50";
	
	public static final String PROPERTY_NAME_MAX_PAGE_RECORDS = "sync.max_page_records";
	
	public static final String PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT = "10";
	
	public static final String PROPERTY_NAME_IGNORED_JAVA_EXCEPTIONS = "sync.ignored_java_exceptions";
	
	public static final String PROPERTY_ENABLE_COMPRESSION = "sync.enable_compression";
	
	public static final String PROPERTY_VERSION = "sync.version";
	
	public static final String PROPERTY_CONNECTION_TIMEOUT = "sync.connection_timeout";
	
	public static final String PROPERTY_SYSTEM_ID_TEMPLATE = "sync.system_id_template";
	
	public static final String PROPERTY_SYSTEM_ID_TEMPLATE_DEFAULT = "{SYNCSERVERNAME}_{NEXTUSERID}{CHECKDIGIT}";
	
	public static final String PROPERTY_ALLOW_SELFSIGNED_CERTS = "sync.allow_selfsigned_certs";
	
	public static final String PROPERTY_SYNC_CLONED_DATABASE_LOG_ENABLED = "sync.cloned_database.log.enabled";
	
	public static final String RESPONSE_SUFFIX = "_response";
	
	public static final String DIR_IMPORT = "import";
	
	public static final String DIR_JOURNAL = "journal";
	
	public static final String SCHEDULED_TASK_CLASS = SyncTask.class.getName();
	
	public static final String SCHEDULED_TASK_PROPERTY_SERVER_ID = "serverId";
	
	public static final String CLEAN_UP_OLD_RECORDS_TASK_CLASS_NAME = CleanupSyncTablesTask.class.getName();
	
	public static final String DEFAULT_PARENT_SCHEDULE_NAME = "sync.status.parent.schedule.default.name";
	
	public static final String DEFAULT_PARENT_SCHEDULE_DESCRIPTION = "sync.status.parent.schedule.default.description";
	
	public static final String DEFAULT_CHILD_SERVER_USER_GENDER = "M";
	
	public static final String DEFAULT_CHILD_SERVER_USER_NAME = "sync.config.child.user.name";
	
	public static final String PRIV_BACKUP_ENTIRE_DATABASE = "Backup Entire Database";
	
	public static final String PRIV_VIEW_SYNC_RECORDS = "View Synchronization Records";
	
	public static final String ROLE_TO_SEND_TO_MAIL_ALERTS = "sync.roleToReceiveAlerts";
	
	public static final String PROPERTY_DATE_PATTERN = "sync.date_pattern";
	
	public static final String DEFAULT_DATE_PATTERN = "MM/dd/yyyy HH:mm:ss";
	
	public static final int DEFAULT_HTTPS_PORT = 443;
	
}
