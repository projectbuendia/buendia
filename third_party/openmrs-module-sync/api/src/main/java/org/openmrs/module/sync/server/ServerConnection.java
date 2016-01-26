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
package org.openmrs.module.sync.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.springframework.util.StringUtils;

/**
 * 
 */
public class ServerConnection {

	private static final Log log = LogFactory.getLog(ServerConnection.class);
	
	public static ConnectionResponse cloneParentDB(String address, String username,
			String password) {
		return sendExportedData(address,
		                        username,
		                        password,
		                        SyncConstants.CLONE_MESSAGE);
	}

	public static ConnectionResponse test(String address, String username,
			String password) {
		return sendExportedData(address,
		                        username,
		                        password,
		                        SyncConstants.TEST_MESSAGE);
	}

	public static ConnectionResponse sendExportedData(RemoteServer server,
			String message) {
		return sendExportedData(server.getAddress(),
		                        server.getUsername(),
		                        server.getPassword(),
		                        message,
		                        false);
	}

	public static ConnectionResponse sendExportedData(RemoteServer server,
			String message, boolean isResponse) {
		return sendExportedData(server.getAddress(),
		                        server.getUsername(),
		                        server.getPassword(),
		                        message,
		                        isResponse);
	}

	public static ConnectionResponse sendExportedData(String address,
			String username, String password, String message) {
		return sendExportedData(address, username, password, message, false);
	}

	public static ConnectionResponse sendExportedData(String url, String username, String password, String content, boolean isResponse) {

		// Default response - default constructor instantiates contains error codes 
		ConnectionResponse syncResponse = new ConnectionResponse();
		
		HttpClient client = new HttpClient();
		
		url = url + SyncConstants.DATA_IMPORT_SERVLET;
		log.info("POST multipart request to " + url);
		
		if (url.startsWith("https")){
			try {
				if (Boolean.parseBoolean(Context.getAdministrationService()
						.getGlobalProperty(SyncConstants.PROPERTY_ALLOW_SELFSIGNED_CERTS))){

					// It is necessary to provide a relative url (from the host name and port to the right)
					String relativeUrl;
					
					URI uri = new URI(url, true); 
					String host = uri.getHost();
					int port = uri.getPort();
					
					// URI.getPort() returns -1 if port is not explicitly set
					if (port <= 0){
						port = SyncConstants.DEFAULT_HTTPS_PORT;
						relativeUrl = url.split(host, 2)[1];	
					} else {
						relativeUrl = url.split(host + ":" + port, 2)[1];
					}

					Protocol easyhttps = new Protocol("https", (ProtocolSocketFactory) new EasySSLProtocolSocketFactory(), port);
					client.getHostConfiguration().setHost(host, port, easyhttps);
					
					url = relativeUrl;
				}
			} catch(IOException ioe){
				log.error("Unable to configure SSL to accept self-signed certificates");
			} catch (GeneralSecurityException e) {
				log.error("Unable to configure SSL to accept self-signed certificates");
			}
		}
		
		PostMethod method = new PostMethod(url);
		
		try {
			
			boolean useCompression = 
				Boolean.parseBoolean(Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_ENABLE_COMPRESSION, "true"));
			
			log.info("use compression: " + useCompression);
			// Compress content
			ConnectionRequest request = new ConnectionRequest(content, useCompression);

			// Create up multipart request
			Part[] parts = {
					new FilePart("syncDataFile", new ByteArrayPartSource("syncDataFile", request.getBytes())),
					new StringPart("username", username),				
					new StringPart("password", password),				
					new StringPart("compressed", String.valueOf(useCompression)),
					new StringPart("isResponse", String.valueOf(isResponse)),
					new StringPart("checksum", String.valueOf(request.getChecksum()))
			};	
			
			method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));		

			// Open a connection to the server and post the data
			client.getHttpConnectionManager().getParams().setSoTimeout(ServerConnection.getTimeout().intValue());
			client.getHttpConnectionManager().getParams().setConnectionTimeout(ServerConnection.getTimeout().intValue());
			int status = client.executeMethod(method);	
			
			
			// As long as the response is OK (200)
			if (status == HttpStatus.SC_OK) {
				// Decompress the response from the server
				//log.info("Response from server:" + method.getResponseBodyAsString());
	
				// Check to see if the child/parent sent back a compressed response
				Header compressionHeader = method.getResponseHeader("Enable-Compression");
				useCompression = (compressionHeader!=null)?new Boolean(compressionHeader.getValue()):false;
				log.info("Response header Enable-Compression: " + useCompression);

				// Decompress the data received (if compression is enabled)
				syncResponse = new ConnectionResponse(method.getResponseBodyAsStream(), useCompression);
				
				// Now we want to validate the checksum
				Header checksumHeader = method.getResponseHeader("Content-Checksum");
				long checksumReceived = (checksumHeader!=null)?new Long(checksumHeader.getValue()):0;
				log.info("Response header Content-Checksum: " + checksumReceived);
				
				
				log.info("checksum value received in response header: " + checksumReceived );
	        	log.info("checksum of payload: " +  syncResponse.getChecksum());
	
	        	// TODO Need to figure out what to do with this response
				if (checksumReceived > 0 && (checksumReceived !=  syncResponse.getChecksum())) {
		        	log.error("ERROR: FAILED CHECKSUM!");
		        	syncResponse.setState(ServerConnectionState.CONNECTION_FAILED);	// contains error message           
	            }
			} 
			// if there's an error response code we should set the tran
			else { 
				// HTTP error response code
				syncResponse.setResponsePayload("HTTP " + status + " Error Code: " + method.getResponseBodyAsString());
				syncResponse.setState(ServerConnectionState.CONNECTION_FAILED);	// contains error message 
			}			
				
		} catch (MalformedURLException mue) {
			log.error("Malformed URL " + url, mue);
			syncResponse.setState(ServerConnectionState.MALFORMED_URL);
		} catch (Exception e) { // all other exceptions really just mean that the connection was bad
			log.error("Error occurred while sending/receiving data ", e);
			syncResponse.setState(ServerConnectionState.CONNECTION_FAILED);
		} finally { 			
			method.releaseConnection();
		}
		return syncResponse;
	}

	
	/**
	 * Gets the sync server connection timeout.
	 * 
	 * @return the connection timeout in milliseconds.
	 * 
	 * @should not throw NPE when timeout global property is not set
	 */
	public static Double getTimeout() {
		// let's figure out a suitable timeout
		String timeoutGP = Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_CONNECTION_TIMEOUT);
		try {
			if (StringUtils.hasText(timeoutGP))
				return Double.valueOf(timeoutGP.trim());
			
		} catch (Exception ex){
			log.error("Could not convert " + timeoutGP + " to Double.  Please enter a valid number of miliseconds, or leave " + SyncConstants.PROPERTY_CONNECTION_TIMEOUT + " blank to use the default.");
		}
		Double timeout = 300000.0; // let's just default at 5 min for now
		try {
			Integer maxRecords = new Integer(Context.getAdministrationService()
			                                       .getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RECORDS_WEB,
			                                                           SyncConstants.PROPERTY_NAME_MAX_RECORDS_DEFAULT));
			timeout = (3 + (maxRecords * 0.1)) * 6000;	// formula we cooked
														// up after running
														// several tests:
														// latency + 0.1N
		} catch (NumberFormatException nfe) {
			// it's ok if this fails (not sure how it could) = we'll just do 5 min timeout
		}
		
		return timeout;
	}

}
