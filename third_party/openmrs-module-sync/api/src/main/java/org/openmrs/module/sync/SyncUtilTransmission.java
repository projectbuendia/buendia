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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncIngestService;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.ingest.SyncIngestException;
import org.openmrs.module.sync.ingest.SyncTransmissionResponse;
import org.openmrs.module.sync.server.ConnectionResponse;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.module.sync.server.ServerConnection;
import org.openmrs.module.sync.server.SyncServerRecord;

/**
 *
 */
public class SyncUtilTransmission {
	
	private static Log log = LogFactory.getLog(SyncUtilTransmission.class);
	
	public static SyncTransmission createSyncTransmissionRequest(RemoteServer server) {
		SyncTransmission tx = null;
		
		try {
			SyncSource source = new SyncSourceJournal();
			tx = new SyncTransmission(source.getSyncSourceUuid(), true);
			if (server.getUuid() != null) {
				tx.setSyncTargetUuid(server.getUuid());
			}
			tx.create(false);
		}
		catch (Exception e) {
			log.error("Error while creating sync transmission", e);
			tx = null;
		}
		
		return tx;
	}
	
	/**
	 * Prepares a sync transmission containing local changes to be sent to the remote server.
	 * 
	 * @param server
	 * @param maxSyncRecords The maximum number of sync records to include in the Sync Transmission
	 * @return
	 */
	public static SyncTransmission createSyncTransmission(RemoteServer server, boolean requestResponseWithTransmission,
	                                                      Integer maxSyncRecords) {
		SyncTransmission tx = null;
		long maxRetryCount = 0;
		boolean maxRetryCountReached = false;
		Exception exceptionThrown = null; // the exception thrown
        boolean createTransmissionLog = Boolean.parseBoolean(Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_SYNC_TRANSMISSION_LOG_ENABLED, "true"));

		try {
			SyncSource source = new SyncSourceJournal();
			SyncStrategyFile strategy = new SyncStrategyFile();
			
			try {
				tx = strategy.createStateBasedSyncTransmission(source, createTransmissionLog, server, requestResponseWithTransmission,
				    maxSyncRecords);
			}
			catch (Exception e) {
				exceptionThrown = e; // save the exception so we can check if one happened in the finally block
				
				log.error("Error while creating state based sync", e);
				// difference is that this time we'll do this without trying to create a file (just getting the output)
				// if it works, that probably means that there was a problem writing file to disk
				tx = strategy.createStateBasedSyncTransmission(source, false, server, requestResponseWithTransmission,
				    maxSyncRecords);
			}
			finally {
				boolean incrementRetryCount = true;
				
				if (exceptionThrown != null) {
					
					// check to see if this exception should be ignored and so NOT increment the retry count
					String ignoredExceptionClassNames = Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_NAME_IGNORED_JAVA_EXCEPTIONS, "");
					String[] exceptionNames = ignoredExceptionClassNames.split(",");
					for (String exname : exceptionNames) {
						if (exname.trim().equals(exceptionThrown.getClass().getName())) {
							incrementRetryCount = false;
							break;
						}
					}
					
				}
				if (incrementRetryCount && tx != null) {
						
						if (server != null) {
							tx.setSyncTargetUuid(server.getUuid());
						}
						// let's update SyncRecords to reflect the fact that we now have tried to sync them, by setting state to SENT or SENT_AGAIN
						maxRetryCount = Long.parseLong(Context.getAdministrationService().getGlobalProperty(
						    SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT, SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT_DEFAULT));
						
						log.info("Max retry count: " + maxRetryCount);
						if (tx.getSyncRecords() != null) {
							for (SyncRecord record : tx.getSyncRecords()) {
								//if max re-try was reached stop now: 
								//a) mark the record as failed save it to DB, 
								//b) clear out the Tx we are creating
								//c) decrement the retry count on records 'after' the one that failed -- they never really got a chance
								//  and if offending record is fixed, the dudes that follow would fail with max retry error
								
								log.info("Checking record retry count (" + record.getRetryCount()
								        + ") against max retry count (" + maxRetryCount + ")");
								if (record.getRetryCount() >= maxRetryCount) {
									record.setState(SyncRecordState.FAILED_AND_STOPPED);
									Context.getService(SyncService.class).updateSyncRecord(record);
									maxRetryCountReached = true;
									SyncUtil.sendSyncErrorMessage(record, server, new SyncException("Max retry count reached"));
									continue;
								}
								if (record.getServerRecords() != null && !server.getServerType().equals(RemoteServerType.PARENT)) {
									//parent -> child: this Tx is part of exchange where parent is sending its changes down to child
									//mark row in the synchronization_server_record table as being sent, 
									for (SyncServerRecord serverRecord : record.getServerRecords()) {
										if (serverRecord.getSyncServer().equals(server)) {
											serverRecord.incrementRetryCount();
											if (serverRecord.getState().equals(SyncRecordState.NEW))
												serverRecord.setState(SyncRecordState.SENT);
											else
												serverRecord.setState(SyncRecordState.SENT_AGAIN);
										}
									}
									Context.getService(SyncService.class).updateSyncRecord(record);
								} else if (server.getServerType().equals(RemoteServerType.PARENT)) {
									//child -> parent scenario: we are about to send data from child to parent
									record.incrementRetryCount();
									if (record.getState().equals(SyncRecordState.NEW))
										record.setState(SyncRecordState.SENT);
									else
										record.setState(SyncRecordState.SENT_AGAIN);
									Context.getService(SyncService.class).updateSyncRecord(record);
								} else {
									log.error("Odd state: trying to get syncRecords for a non-parent server with no corresponding server-records");
								}
							}
							if (tx.getIsMaxRetryReached() || maxRetryCountReached) {
								tx.setSyncRecords(null);
							}
						}
					}
				}
		}
		catch (Exception e) {
			log.error("Error while writing creating sync transmission for server: " + server.getNickname(), e);
			throw new SyncException("Error while performing synchronization, see log messages and callstack.", e);
		}
		
		return tx;
	}
	
	public static SyncTransmissionResponse sendSyncTranssmission(RemoteServer server, SyncTransmission transmission) {
		return SyncUtilTransmission.sendSyncTransmission(server, transmission, null);
	}
	
	public static SyncTransmissionResponse sendSyncTransmission(RemoteServer server, SyncTransmission transmission,
	                                                            SyncTransmissionResponse responseInstead) {
		SyncTransmissionResponse response = new SyncTransmissionResponse();
		response.setErrorMessage(SyncConstants.ERROR_SEND_FAILED.toString());
		response.setFileName(SyncConstants.FILENAME_SEND_FAILED);
		response.setUuid(SyncConstants.UUID_UNKNOWN);
		response.setState(SyncTransmissionState.FAILED);
		
		SyncService syncService = Context.getService(SyncService.class);
		
		try {
			//handle the case of getting to too many retries
			if (transmission != null)
				if (transmission.getIsMaxRetryReached()) {
					response.setState(SyncTransmissionState.MAX_RETRY_REACHED);
					return response;
				}
			
			if (server != null) {
				String toTransmit = null;
				server.setLastSyncState(SyncTransmissionState.PENDING);
				syncService.saveRemoteServer(server);
				if (responseInstead != null) {
					toTransmit = responseInstead.getFileOutput();
					log.info("Sending a response (with tx inside): " + toTransmit);
				} else if (transmission != null) {
					toTransmit = transmission.getFileOutput();
					log.info("Sending an actual tx: " + toTransmit);
				}
				
				if (toTransmit != null && toTransmit.length() > 0) {
					if (responseInstead == null && transmission != null && transmission.getSyncRecords() != null
					        && transmission.getSyncRecords().size() == 0) {
						response.setState(SyncTransmissionState.OK_NOTHING_TO_DO);
						response.setErrorMessage("");
						response.setFileName(transmission.getFileName() + SyncConstants.RESPONSE_SUFFIX);
						response.setUuid(transmission.getUuid());
						response.setTimestamp(transmission.getTimestamp());
					} else {
						ConnectionResponse connResponse = null;
						boolean isResponse = responseInstead != null;
						
						try {
							connResponse = ServerConnection.sendExportedData(server, toTransmit, isResponse);
						}
						catch (Exception e) {
							log.error("Unable to get send exported data over connection to: " + server, e);
							// no need to change state or error message - it's already set properly; just update last sync state
							server.setLastSyncState(SyncTransmissionState.FAILED);
							syncService.saveRemoteServer(server);
						}
						
						if (connResponse != null) {
							// constructor for SyncTransmissionResponse is null-safe
							response = new SyncTransmissionResponse(connResponse);
							
							//if we got something back, mark status appropriately
							if (response.getState() == SyncTransmissionState.FAILED) {
								server.setLastSyncState(SyncTransmissionState.FAILED);
							} else {
								server.setLastSyncState(SyncTransmissionState.OK);
							}
							;
							
							if (response.getSyncImportRecords() == null) {
								log.debug("No records to process in response");
							} else {
								// process each incoming syncImportRecord; if any records failed, mark last send as failed
								boolean allOK = true;
								for (SyncImportRecord importRecord : response.getSyncImportRecords()) {
									Context.getService(SyncIngestService.class)
									        .processSyncImportRecord(importRecord, server);
									if (importRecord.getState() != SyncRecordState.COMMITTED
									        && importRecord.getState() != SyncRecordState.ALREADY_COMMITTED
									        && importRecord.getState() != SyncRecordState.NOT_SUPPOSED_TO_SYNC) {
										allOK = false;
									}
								}
								
								//now if some records failed, record it in lastSyncState
								if (allOK == false) {
									server.setLastSyncState(SyncTransmissionState.FAILED_RECORDS);
								}
							}
							//update lastSyncState
							syncService.saveRemoteServer(server);
						}
					}
				} else {
					response.setErrorMessage(SyncConstants.ERROR_TRANSMISSION_CREATION.toString());
					response.setFileName(SyncConstants.FILENAME_NOT_CREATED);
					response.setUuid(SyncConstants.UUID_UNKNOWN);
					response.setState(SyncTransmissionState.TRANSMISSION_CREATION_FAILED);
				}
			} else {
				// server is null
				response.setErrorMessage(SyncConstants.ERROR_INVALID_SERVER.toString());
				response.setFileName(SyncConstants.FILENAME_INVALID_SERVER);
				response.setUuid(SyncConstants.UUID_UNKNOWN);
				response.setState(SyncTransmissionState.INVALID_SERVER);
			}
		}
		catch (Exception e) {
			log.error("Unable to send sync transmission to: " + server, e);
			if (server != null) {
				server.setLastSyncState(SyncTransmissionState.FAILED);
				syncService.saveRemoteServer(server);
			}
		}
		
		return response;
	}
	
	/**
	 * Performs 'full' synchronization (from a child perspective) with parent server identified by
	 * parent parameter.
	 * 
	 * @param parent the server to do send/receive to/from
	 * @param size (nullable) updated mid-method to be the number of objects coming from the parent
	 * @param maxSyncRecords The maximum number of sync records to include in the SyncTransmission
	 * @return
	 */
	public static SyncTransmissionResponse doFullSynchronize(RemoteServer parent, ReceivingSize size, Integer maxSyncRecords) {
		SyncTransmissionResponse response = new SyncTransmissionResponse();
		response.setErrorMessage(SyncConstants.ERROR_TRANSMISSION_CREATION.toString());
		response.setFileName(SyncConstants.FILENAME_NOT_CREATED);
		response.setUuid(SyncConstants.UUID_UNKNOWN);
		response.setState(SyncTransmissionState.TRANSMISSION_CREATION_FAILED);
		
		try {
			if (parent != null) {
				Integer parentId = parent.getServerId();
				//set the date
				parent.setLastSync(new Date());
				
				// set the flag saying that sync'ing is happening
				parent.setSyncInProgress(true);
				
				//this is the initial handshake only; no state sent
				SyncTransmission tx = SyncUtilTransmission.createSyncTransmissionRequest(parent);
				
				if (tx != null) {
					if (log.isInfoEnabled()) {
						log.info("SYNC TX created was: " + tx.getFileOutput());
					}
					// start by sending request to parent server
					SyncTransmissionResponse initialResponse = SyncUtilTransmission.sendSyncTranssmission(parent, tx);
					if (initialResponse != null) {
						// get syncTx from that response, and process it
						SyncTransmission initialTxFromParent = initialResponse.getSyncTransmission();
						SyncTransmissionResponse str = null;
						SyncService syncService = Context.getService(SyncService.class);
						if (initialTxFromParent != null) {
							// since we know what server this should be from, 
							//let's check to make sure we've got the uuid - we'll need it later
							String remoteUuid = initialTxFromParent.getSyncSourceUuid();
							if (parent.getUuid() == null) {
								parent.setUuid(remoteUuid);
								syncService.saveRemoteServer(parent);
							}
							
							if (size != null && initialTxFromParent.getSyncRecords() != null)
								size.setSize(initialTxFromParent.getSyncRecords().size());
							
							// process syncTx from parent, and generate response
							// tx may be null - meaning no updates from parent
							parent = null; //for SYNC-175
							str = SyncUtilTransmission.processSyncTransmission(initialTxFromParent, maxSyncRecords);
							
						} else {
							log.info("initialTxFromParent was null coming back from parent(?)");
							initialResponse.createFile(false, "requestResponse");
							log.info("response was: " + initialResponse.getFileOutput());
						}
						
						// now get local changes destined for parent, and package those inside
						parent = syncService.getRemoteServer(parentId);
						SyncTransmission st = SyncUtilTransmission.createSyncTransmission(parent, false, maxSyncRecords);
						if (str != null) {
							if (str.getState() != SyncTransmissionState.CANNOT_FIND_SERVER_WITH_UUID) {
								log.info("Received updates from parent, so replying and sending updates of our own: "
								        + st.getFileOutput());
								str.setSyncTransmission(st);
								str.createFile(false, "/receiveAndSend");
								//reload parent
								//parent = syncService.getRemoteServer(parentId);
								response = SyncUtilTransmission.sendSyncTransmission(parent, null, str);
								
								// add all changes from parent into response
								if (str.getSyncImportRecords() != null) {
									if (response.getSyncImportRecords() == null)
										response.setSyncImportRecords(str.getSyncImportRecords());
									else
										response.getSyncImportRecords().addAll(str.getSyncImportRecords());
									
									// mark all of these imported records as "committed plus confirmed"
									for (SyncImportRecord record : str.getSyncImportRecords()) {
										if (record.getState().equals(SyncRecordState.COMMITTED)
										        || record.getState().equals(SyncRecordState.ALREADY_COMMITTED)) {
											record.setState(SyncRecordState.COMMITTED_AND_CONFIRMATION_SENT);
											syncService.updateSyncImportRecord(record);
										} else {
											response.setState(SyncTransmissionState.FAILED_RECORDS);
										}
									}
								}
							}
						} else {
							log.info("No updates from parent, generating our own transmission");
							response = SyncUtilTransmission.sendSyncTransmission(parent, st, null);
						}
						
					} else {
						log.warn("INITIAL RESPONSE CAME BACK AS NULL IN DOFULLSYNCHRONIZATION(SERVER)");
						log.warn("TX was: " + tx.getFileOutput());
					}
				} else {
					log.warn("SEEMS WE COULND'T CREATE A NEW SYNC TRANMISSION FOR SERVER: " + parent.getNickname());
					// no need for handling else - the correct error messages, etc have been written already
				}
			} else {
				response.setErrorMessage(SyncConstants.ERROR_INVALID_SERVER.toString());
				response.setFileName(SyncConstants.FILENAME_INVALID_SERVER);
				response.setUuid(SyncConstants.UUID_UNKNOWN);
				response.setState(SyncTransmissionState.INVALID_SERVER);
			}
		}
		catch (Exception e) {
			//log.error("Unexpected Error during full synchronize.", e);
			throw (new SyncException("Error while performing synchronization to parent, see log messages and callstack.", e));
		}
		finally {
			// unset the flag so we know that sync'ing is done
			parent.setSyncInProgress(false);
		}
		
		return response;
	}
	
	/**
	 * Processes incoming sync transmission.
	 * <p/>
	 * Remarks: This method is used both by child and parent. On child, it is used to process any
	 * new incoming records from parent; on parent it is used to process child's changes.
	 * 
	 * @param st transmission to process.
	 * @param maxSyncRecords The maximum number of sync records to include in the SyncTransmission
	 * @return Returns SyncTransmissionResponse object that represents the confirmation status for
	 *         the records that were sent.
	 */
	public static SyncTransmissionResponse processSyncTransmission(SyncTransmission st, Integer maxSyncRecords) {
		SyncTransmissionResponse str = new SyncTransmissionResponse(st);
		
		//fill-in the server uuid for the response AGAIN
		SyncService syncService = Context.getService(SyncService.class);
		str.setSyncTargetUuid(syncService.getServerUuid());
		String sourceUuid = st.getSyncSourceUuid();
		RemoteServer origin = syncService.getRemoteServer(sourceUuid);
		
		if (origin != null) {
			log.info("Receiving Sync Transmission from " + origin.getNickname());
		}
		
		User authenticatedUser = Context.getAuthenticatedUser();
		if (origin == null && authenticatedUser != null) {
			// make a last-ditch effort to try to figure out what server this is coming from, so we can behave appropriately.
			String username = authenticatedUser.getUsername();
			log.warn("CANNOT GET ORIGIN SERVER FOR THIS REQUEST, get by username '" + username + "' instead");
			origin = syncService.getRemoteServerByUsername(username);
			if (origin != null && sourceUuid != null && sourceUuid.length() > 0) {
				// take this opportunity to save the uuid, now we've identified which server this is
				origin.setUuid(sourceUuid);
				syncService.saveRemoteServer(origin);
			} else {
				log.warn("STILL UNABLE TO GET ORIGIN WITH username " + username + " and sourceuuid " + sourceUuid);
			}
		} else {
			if (origin != null)
				log.debug("ORIGIN SERVER IS " + origin.getNickname());
			else
				log.debug("ORIGIN SERVER IS STILL NULL");
		}
		
		if (origin == null) {
			str.setState(SyncTransmissionState.CANNOT_FIND_SERVER_WITH_UUID);
			return str;
		}
		
		//update timestamp for origin server, set the status to processing
		origin.setLastSync(new Date());
		origin.setLastSyncState(SyncTransmissionState.PENDING); //set it failed to start with
		syncService.saveRemoteServer(origin);
		
		//now start processing
		boolean success = true;
		List<SyncImportRecord> importRecords = new ArrayList<SyncImportRecord>();
		if (st.getSyncRecords() != null) {
			log.info("Processing Sync Transmission from " + origin.getNickname());
			SyncImportRecord importRecord = null;
			for (SyncRecord record : st.getSyncRecords()) {
				try {
					//pre-create import record in case we get exception            		
					importRecord = new SyncImportRecord();
					importRecord.setState(SyncRecordState.FAILED); // by default, until we know otherwise
					importRecord.setRetryCount(record.getRetryCount());
					importRecord.setTimestamp(record.getTimestamp());
					importRecord.setSourceServer(origin);
					
					//TODO: write record as pending to prevent someone else trying to process this record at the same time
					
					//reload origin for SYNC-175
					Integer originId = origin.getServerId();
					//now attempt to process
					if (log.isInfoEnabled())
						log.info("Processing record " + record.getUuid() + " which contains "
					        + record.getContainedClassSet().toString());
					importRecord = Context.getService(SyncIngestService.class).processSyncRecord(record, origin);
					origin = syncService.getRemoteServer(originId);
				}
				catch (SyncIngestException e) {
					log.error("Sync error while ingesting records for server: " + origin.getNickname(), e);
					importRecord = e.getSyncImportRecord();
				}
				catch (Exception e) {
					//just report error, import record already set to failed
					log.error("Unexpected exception while ingesting records for server: " + origin.getNickname(), e);
					if (importRecord != null)
						importRecord.setErrorMessage(e.getMessage());
				}
				importRecords.add(importRecord);
				
				//if the record update failed for any reason, do not continue on, stop now
				//adding NOT_SUPPOSED_TO_SYNC: SYNC-204.
				if (importRecord.getState() != SyncRecordState.COMMITTED
				        && importRecord.getState() != SyncRecordState.ALREADY_COMMITTED
				        && importRecord.getState() != SyncRecordState.NOT_SUPPOSED_TO_SYNC) {
					success = false;
					break;
				}
			}
		}
		
		//what ever happened here, send the status for the import records back
		if (importRecords.size() > 0) {
			str.setSyncImportRecords(importRecords);
		}
		
		// now we're ready to see if we need to fire back a response transmission
		if (origin != null) {
			if (!origin.getDisabled() && st.getIsRequestingTransmission()) {
				SyncTransmission tx = SyncUtilTransmission.createSyncTransmission(origin, false, maxSyncRecords);
				if (tx != null) {
					log.info("processing transmission with this many records: " + tx.getSyncRecords().size());
					str.setSyncTransmission(tx);
				} else {
					log.info("transmission tx is null");
				}
			} else if (log.isInfoEnabled()) {
				log.info("Did not create transmission. orgin is disabled? " + origin.getDisabled()
				        + " && st.isRequestingTransmission? " + st.getIsRequestingTransmission());
			}
		}
		
		//update the last sync status appropriately
		if (st.getIsMaxRetryReached()) {
			origin.setLastSyncState(SyncTransmissionState.MAX_RETRY_REACHED);
		} else if (success) {
			origin.setLastSyncState(SyncTransmissionState.OK);
		} else {
			origin.setLastSyncState(SyncTransmissionState.FAILED); //set it failed to start with
		}
		syncService.saveRemoteServer(origin);
		//for SYNC-175
		origin = null;
		return str;
	}
	
	public static class ReceivingSize {
		
		private Integer size;
		
		public Integer getSize() {
			return size;
		}
		
		public void setSize(Integer s) {
			this.size = s;
		}
	}
	
	/**
	 * Main method to initiate data synchronization from a child to its parent.
	 * 
	 * @param size (nullable) updated mid-method to be the number of objects coming in from
	 *            the parent
	 * @param maxSyncRecords The maximum number of sync records to sync from child to parent server
	 * @return the {@link SyncTransmissionResponse} from the parent
	 * @see #doFullSynchronize(RemoteServer, ReceivingSize, Integer)
	 * @should run one sync task at a time
	 */
	public static SyncTransmissionResponse doFullSynchronize(ReceivingSize size, Integer maxSyncRecords) {
		// sends to parent server (by default)
		RemoteServer parent = Context.getService(SyncService.class).getParentServer();
		
		if (parent != null) {
			if (parent.getSyncInProgress() || SyncTask.getIsExecuting()) {
				SyncTransmissionResponse response = new SyncTransmissionResponse();
				response.setErrorMessage(SyncConstants.ERROR_CANNOT_RUN_PARALLEL.toString());
				response.setFileName(SyncConstants.FILENAME_NOT_CREATED);
				response.setUuid(SyncConstants.UUID_UNKNOWN);
				response.setState(SyncTransmissionState.ERROR_CANNOT_RUN_PARALLEL);
				
				return response;
			}
			
			return SyncUtilTransmission.doFullSynchronize(parent, size, maxSyncRecords);
		} else {
			SyncTransmissionResponse response = new SyncTransmissionResponse();
			response.setErrorMessage(SyncConstants.ERROR_NO_PARENT_DEFINED.toString());
			response.setFileName(SyncConstants.FILENAME_NO_PARENT_DEFINED);
			response.setUuid(SyncConstants.UUID_UNKNOWN);
			response.setState(SyncTransmissionState.NO_PARENT_DEFINED);
			
			return response;
		}
	}
}
