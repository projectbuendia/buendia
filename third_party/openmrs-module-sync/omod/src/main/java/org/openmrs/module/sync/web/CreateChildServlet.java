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
package org.openmrs.module.sync.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * 
 */
public class CreateChildServlet extends HttpServlet {

	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	private static final long serialVersionUID = 1L;
	private CommonsMultipartResolver multipartResolver;

	/**
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		super.init(config);
		multipartResolver = new CommonsMultipartResolver(this.getServletContext());
	}

	protected void doGet(HttpServletRequest request,
	        HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession();
		if(!Context.isAuthenticated()){
			response.sendRedirect(request.getContextPath() + "/login.htm");
			return;
		}
		if (!Context.hasPrivilege(SyncConstants.PRIV_BACKUP_ENTIRE_DATABASE)) {
			session.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
			                     "Privilege required: "
			                             + SyncConstants.PRIV_BACKUP_ENTIRE_DATABASE);
			session.setAttribute(WebConstants.OPENMRS_LOGIN_REDIRECT_HTTPSESSION_ATTR,
			                     request.getRequestURI() + "?"
			                             + request.getQueryString());
			response.sendRedirect(request.getContextPath() + "/login.htm");
			return;
		}
		File generatedFile = Context.getService(SyncService.class).generateDataFile();
		response.setContentType("text/sql");
		response.setHeader("Content-Disposition", "attachment; filename=" + generatedFile.getName());
		response.setHeader("Pragma", "no-cache");
		IOUtils.copy(new FileInputStream(generatedFile), response.getOutputStream());
		response.getOutputStream().flush();
		response.getOutputStream().close();
		
		boolean clonedDBLog = Boolean.parseBoolean(Context.getAdministrationService()
				.getGlobalProperty(SyncConstants.PROPERTY_SYNC_CLONED_DATABASE_LOG_ENABLED, "true"));
		
		if (!clonedDBLog){
			generatedFile.delete();
		}
	}

	protected void doPost(HttpServletRequest request,
	        HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession();
		if(!Context.isAuthenticated()){
			reply(response,"Not logged in, please login and retry again","red");
			return;
		}
		if (!Context.hasPrivilege(SyncConstants.PRIV_BACKUP_ENTIRE_DATABASE)) {
			session.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
			                     "Privilege required: "
			                             + SyncConstants.PRIV_BACKUP_ENTIRE_DATABASE);
			session.setAttribute(WebConstants.OPENMRS_LOGIN_REDIRECT_HTTPSESSION_ATTR,
			                     request.getRequestURI() + "?"
			                             + request.getQueryString());
			response.sendRedirect(request.getContextPath() + "/login.htm");
			return;
		}
		response.setContentType("text/html");
		MultipartHttpServletRequest multipartRequest = multipartResolver.resolveMultipart(request);
		MultipartFile mf = multipartRequest.getFile("cloneFile");
		if (mf != null && !mf.isEmpty()) {
			try {
				File dir = SyncUtil.getSyncApplicationDir();
				File file = new File(dir, SyncConstants.CLONE_IMPORT_FILE_NAME
				        + SyncConstants.SYNC_FILENAME_MASK.format(new Date())
				        + ".sql");
				IOUtils.copy(mf.getInputStream(),
				             new FileOutputStream(file));
				Context.getService(SyncService.class).execGeneratedFile(file);

				reply(response,"Child database successfully updated","green");
				
				boolean clonedDBLog = Boolean.parseBoolean(Context.getAdministrationService()
						.getGlobalProperty(SyncConstants.PROPERTY_SYNC_CLONED_DATABASE_LOG_ENABLED, "true"));
				
				if (!clonedDBLog){
					file.delete();
				}
			} catch (Exception ex) {
				log.warn("Unable to read the clone data file", ex);
				reply(response,"Unable to read the data clonefile"+ ex.toString(),"red");
				ex.printStackTrace();
			}
		} else {

			reply(response,"The file sent is null or empty, please select a file to upload","red");
		}

	}

	private void reply(HttpServletResponse response, String str,String color) {
		try {
			response.getWriter().println("<html><body onload=\"window.parent.showUploadResponse('"+str+"','"+color+"');\"></body></html>");
		} catch (Exception ex) {
			log.warn(ex.toString());
			ex.printStackTrace();
		}
	}
}
