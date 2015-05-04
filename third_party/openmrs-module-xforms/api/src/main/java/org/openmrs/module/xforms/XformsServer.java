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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.xforms.download.PatientDownloadManager;
import org.openmrs.module.xforms.download.UserDownloadManager;
import org.openmrs.module.xforms.download.XformDataUploadManager;
import org.openmrs.module.xforms.download.XformDownloadManager;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZOutputStream;

/**
 * Serves xform services to non HTTP connections. Examples of such connections
 * can be SMS, Bluetooth, Data cable, etc.
 * 
 * @author Daniel
 * 
 */
public class XformsServer {

	/** Value representing a not yet set status. */
	public static final byte STATUS_NULL = -1;

	/** Value representing success of an action. */
	public static final byte STATUS_SUCCESS = 1;

	/** Value representing failure of an action. */
	public static final byte STATUS_FAILURE = 0;

	/** Action to get a list of form definitions. */
	public static final byte ACTION_DOWNLOAD_FORMS = 3;

	/** Action to save a list of form data. */
	public static final byte ACTION_UPLOAD_FORMS = 5;

	/** Action to download a list of patients from the server. */
	public static final byte ACTION_DOWNLOAD_PATIENTS = 6;

	/** Action to download a list of users from the server. */
	public static final byte ACTION_DOWNLOAD_USERS = 7;

	/** Action to download a list of users and forms from the server. */
	public static final byte ACTION_DOWNLOAD_COHORTS = 8;
	
	/** Action to download a list of users and forms from the server. */
	public static final byte ACTION_DOWNLOAD_SAVED_SEARCHES = 9;
	
	/** Action to download a list of patients from the server. */
	public static final byte ACTION_DOWNLOAD_SS_PATIENTS = 10;

	/** Action to download a list of users and forms from the server. */
	public static final byte ACTION_DOWNLOAD_USERS_AND_FORMS = 11;
	
	/** Action to download a list of patients filtered by name and identifier. */
	public static final byte ACTION_DOWNLOAD_FILTERED_PATIENTS = 15;
	

	private Log log = LogFactory.getLog(this.getClass());

	public XformsServer() {

	}

	/**
	 * Called when a new connection has been received. Failures are not handled
	 * in this class as different servers (BT,SMS, etc) may want to handle them
	 * differently.
	 * 
	 * @param dis - the stream to read from.
	 * @param dos - the stream to write to.
	 */
	public void processConnection(DataInputStream dis, DataOutputStream dosParam)
	throws IOException, Exception {

		GZIPOutputStream gzip = new GZIPOutputStream(new BufferedOutputStream(dosParam));
		DataOutputStream dos = new DataOutputStream(gzip);

		byte responseStatus = ResponseStatus.STATUS_ERROR;

		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			String name = dis.readUTF();
			String pw = dis.readUTF();
			String serializer = dis.readUTF();
			@SuppressWarnings("unused")
			String locale = dis.readUTF();
			
			byte action = dis.readByte();
			Context.openSession();
			
			try{
				Context.authenticate(name, pw);
			}
			catch(ContextAuthenticationException ex){
				responseStatus = ResponseStatus.STATUS_ACCESS_DENIED;
			}

			if(responseStatus != ResponseStatus.STATUS_ACCESS_DENIED){
				DataOutputStream dosTemp = new DataOutputStream(baos);
				
				if (action == ACTION_DOWNLOAD_PATIENTS)
					downloadPatients(String.valueOf(dis.readInt()), dosTemp,serializer, false);
				else if(action == ACTION_DOWNLOAD_SS_PATIENTS)
					downloadPatients(String.valueOf(dis.readInt()), dosTemp,serializer, true);
				else if(action == ACTION_DOWNLOAD_COHORTS)
					PatientDownloadManager.downloadCohorts(dosTemp,serializer);
				else if(action == ACTION_DOWNLOAD_SAVED_SEARCHES)
					PatientDownloadManager.downloadSavesSearches(dosTemp, serializer);
				else if (action == ACTION_DOWNLOAD_FORMS)
					XformDownloadManager.downloadXforms(dosTemp,serializer);
				else if (action == ACTION_UPLOAD_FORMS)
					submitXforms(dis, dosTemp,serializer);
				else if (action == ACTION_DOWNLOAD_USERS)
					UserDownloadManager.downloadUsers(dosTemp,serializer);
				else if (action == ACTION_DOWNLOAD_USERS_AND_FORMS)
					downloadUsersAndForms(dosTemp,serializer);
				else if(action == ACTION_DOWNLOAD_FILTERED_PATIENTS)
					downloadPatients(dis.readUTF(), dis.readUTF(), dosTemp,serializer);

				responseStatus = ResponseStatus.STATUS_SUCCESS;
			}

			dos.writeByte(responseStatus);

			if(responseStatus == ResponseStatus.STATUS_SUCCESS)
				dos.write(baos.toByteArray());

			dos.close();
			gzip.finish();
		}
		catch(Exception ex){
			log.error(ex.getMessage(),ex);
			try{
				dos.writeByte(responseStatus);
				dos.flush();
				gzip.finish();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		finally{
			Context.closeSession();
		}
	}

	private void downloadPatients(String cohortId, OutputStream os, String serializer, boolean isSavedSearch) throws Exception{
		
		//Context.openSession();
		
		try{
			PatientDownloadManager.downloadPatients(cohortId, os, serializer, isSavedSearch);
		}
		finally{
			//Context.closeSession();
		}
	}
	
	private void downloadPatients(String name, String identifier, OutputStream os, String serializerKey) throws Exception{
		
		//Context.openSession();
		
		try{
			PatientDownloadManager.downloadPatients(name, identifier, os,serializerKey);
		}
		finally{
			//Context.closeSession();
		}
	}
	
	/**
	 * Saves xforms xml models.
	 * 
	 * @param dis - the stream to read from.
	 * @param dos - the stream to write to.
	 */
	private void submitXforms(DataInputStream dis, DataOutputStream dos, String serializerKey) throws Exception {
		XformDataUploadManager.submitXforms(dis, new java.util.Date().toString(),serializerKey);
		try {
			dos.writeByte(STATUS_SUCCESS);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			dos.writeByte(STATUS_FAILURE);
		}
	}

	/**
	 * Downloads a list of users and xforms.
	 * 
	 * @param dos - the stream to write to.
	 * @throws Exception
	 */
	private void downloadUsersAndForms(DataOutputStream dos, String serializerKey) throws Exception {
		//Need to have a away of handing user serialization more smartly
		UserDownloadManager.downloadUsers(dos,null);
		XformDownloadManager.downloadXforms(dos,serializerKey);
	}
}
