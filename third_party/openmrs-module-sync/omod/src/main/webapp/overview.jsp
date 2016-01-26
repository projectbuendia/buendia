<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/overview.htm" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />
<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />

<%@ include file="localHeader.jsp" %>

<script language="JavaScript">

	//Called to disable content when sync is disabled
	function disableDIVs() {
		hideDiv('serverList');
	}
        var timerId;
        $j(document).ready(function() {
		$j('#refreshField').change(function() {
			refreshInterval();
		});
                refreshInterval();
        });
		
	function refreshInterval(){
		clearInterval(timerId);
		refreshTime = $j('#refreshField').attr('value');
		timerId = setInterval(refreshIds, (refreshTime*1000));
	}
		
	function refreshIds(){
		$j.get('overview.htm', function(data){
			$j.each(['syncCurrentTime', 'syncStatus', 'statsList'], function(i, key) {
				$j('#'+key).replaceWith($j(data).find('#'+key));
			});
		});
	}
</script>

<table width="100%">
	<tr>
		<td>
			<h2><spring:message code="sync.overview.title"/></h2>
		</td>
		<td>
			<div style="float: right"><spring:message code="sync.overview.refreshMsg"/> <input id="refreshField" type="text" size="3" value="30" /> secs</div>
                </td>
	</tr>
</table>

<div id="general">

	<b class="boxHeader"><spring:message code="sync.config.syncStatus"/></b>
	<div class="box">
        <table id="syncCurrentTime" cellpadding="10" cellspacing="0">
             <tr>
                 <td style="font-weight: bold;"><spring:message code="sync.config.current.time" /></td>
                 <td><openmrs:formatDate date="${commandObject.currentTime}" format="${syncDateDisplayFormat}" /></td>
            </tr>
        </table>
		<c:choose>
			<c:when test="${empty parent}">
                <i style="padding-left: 10px"><spring:message code="sync.status.parent"/></i>
			</c:when>
			<c:otherwise>
			<table id="syncStatus" cellpadding="10" cellspacing="0">
				<tbody>
					<tr>
						<td style="font-weight: bold;"><spring:message code="sync.status.LastSync" /></td>
						<td>
							<c:if test="${empty parent.lastSync}"><spring:message code="sync.status.LastSync.none"/></c:if>
							<openmrs:formatDate date="${parent.lastSync}" format="${syncDateDisplayFormat}" />
						</td>
					</tr>
					<c:if test="${not empty parent.lastSyncState}">
					<tr>
						<td style="font-weight: bold;"><spring:message code="sync.status.LastSync.result" /></td>
						<td>
							<c:if test="${parent.lastSyncState != 'OK'}">
								<div class="syncStatsWarning">${parent.lastSyncState}</div>
							</c:if>
							<c:if test="${parent.lastSyncState == 'OK'}">
								<div class="syncStatsOK">${parent.lastSyncState}</div>
							</c:if>
						</td>
					</tr>
					</c:if>
                                        <c:if test="${not empty commandObject.nextParentSyncTime}">
                                        <tr>
                                                <td style="font-weight: bold;"><spring:message code="sync.status.NextSync.time" /></td>
                                                <td>
                                                        <div><openmrs:formatDate date="${commandObject.nextParentSyncTime}" format="${syncDateDisplayFormat}" /></div>
						</td>
                                        </tr>
                                        </c:if>
				</tbody>
			</table>
			</c:otherwise>
		</c:choose>
	</div>
	
	&nbsp;
	<div id="serverList">
		<b class="boxHeader"><spring:message code="sync.config.servers.remote"/></b>
		<div class="box">
			<table id="sync" cellpadding="10" cellspacing="0">
				<c:if test="${not empty commandObject.serverList}">
					<thead>
						<tr>
							<th></th>
							<th></th>
							<th align="center" colspan="2" style="text-align: center; font-weight: bold;"><spring:message code="sync.config.server.lastSync"/></th>
							<th align="center" colspan="2" style="background-color: #eef3ff; text-align: center; font-weight: bold;"><spring:message code="sync.general.outgoing"/></th>
							<%-- <th align="center" colspan="2" style="text-align: center; background-color: #fee; font-weight: bold;"><spring:message code="sync.general.incoming"/></th> --%>
						</tr>
						<tr>
							<th><spring:message code="sync.config.server.name" /></th>
							<th style="text-align: center;"><spring:message code="sync.config.server.type" /></th>
							<th style="text-align: center;"><spring:message code="sync.config.server.lastSync.datetime"/></th>
							<th style="text-align: center;"><spring:message code="sync.config.server.lastSync.status"/></th>
							<th style="background-color: #eef; text-align: center;"><spring:message code="sync.status.itemState"/></th>
							<th style="background-color: #eef; text-align: center;"><spring:message code="sync.general.count"/></th>
							<%-- <th style="background-color: #fee; text-align: center;">state</th>
							<th style="background-color: #fee; text-align: center;">count</th> --%>
						</tr>
					</thead>
					<tbody id="statsList">
						<c:set var="bgStyle" value="eee" />				
						<c:set var="bgStyleReceive" value="dde" />				
						<c:set var="bgStyleSend" value="ded" />				
						<!-- iterate over servers and their stats -->
						<c:forEach var="server" items="${commandObject.serverList}" varStatus="status">
							<c:set var="serverStatsSet" value="${commandObject.syncStats[server]}" />
							<c:set var="wroteServerInfo" value="false" />
							<c:forEach var="serverStats" items="${serverStatsSet}" varStatus="statusServerStats">
							<c:choose>
								<c:when test="${serverStats.type == 'SYNC_RECORD_COUNT_BY_STATE'}">
									<tr>
										<!-- Server name -->
										<td nowrap style="background-color: #${bgStyle};">
											<c:if test="${wroteServerInfo == false}">
												<b>${server.nickname}</b>
											</c:if>
										</td>
										<!-- Server type -->
										<td style="background-color: #${bgStyle}; text-align:center;">
											<c:if test="${wroteServerInfo == false}">
												<c:choose>
													<c:when test="${server.serverType == 'PARENT'}">
														<b>${server.serverType}</b>
													</c:when>
													<c:otherwise>
														${server.serverType}
													</c:otherwise>
												</c:choose>
											</c:if>
										</td>
										<!-- Last Attempt -->
										<td style="background-color: #${bgStyle}; text-align:center;">
											<c:if test="${wroteServerInfo == false}">
												<openmrs:formatDate date="${server.lastSync}" format="${syncDateDisplayFormat}" />
											</c:if>
										</td>
										<!-- Last Attempt Status -->
										<td style="background-color: #${bgStyle}; text-align:center; <c:if test="${server.lastSyncState.error}">color: red;</c:if>">
											<c:if test="${wroteServerInfo == false}">
												<c:set var="wroteServerInfo" value="true" />
												${server.lastSyncState} 
											</c:if>
										</td>
										<!-- Data receive/send stats local <- CHILD server, i.e. local is parent -->
										<td style="background-color: #${bgStyleReceive}; text-align:center;">
											${serverStats.name}
										</td>								
										<td style="background-color: #${bgStyleReceive}; text-align:center;">
											${serverStats.value}
										</td>								
										<%-- <td style="background-color: #${bgStyleSend}; text-align:center;" />
										<td style="background-color: #${bgStyleSend}; text-align:center;" /> --%>
									</tr>
								</c:when>
								<c:when test="${serverStats.type == 'SYNC_RECORDS_PENDING_COUNT'}">
									<c:set var="recordsPending" value="${serverStats.value}" />
								</c:when>
								<c:otherwise>
									<%-- <tr><td>--${serverStats.type}--</td></tr> --%>
									<%-- <tr><td>--${serverStats.type}--</td></tr> --%>
									<!--Write out summary for this server based on pending value records -->
									<tr>
										<td colspan="6" style="background-color: #${bgStyle};font-weight: bold; padding-top: 0px;">
											<c:set var="allSyncStatsOk" value="true" />
											<c:if test="${empty server.uuid}">
												<div class="syncStatsWarning"><spring:message code="sync.overview.warning.uuid" arguments="${server.serverId}"/></div>
												<c:set var="allSyncStatsOk" value="false" />
											</c:if>
											<c:if test="${server.lastSyncState != 'OK'}">
												<div class="syncStatsWarning"><spring:message code="sync.overview.warning.lastComm" arguments="${server.lastSyncState}"/></div>
												<c:set var="allSyncStatsOk" value="false" />
											</c:if>
											<c:if test="${serverStats.type == 'LAST_SYNC_REALLY_LONG_TIME_AGO'}">
												<div class="syncStatsWarning"><spring:message code="sync.overview.warning.lastSyncTimeVeryBad" arguments="${recordsPending}"/></div>
												<c:set var="allSyncStatsOk" value="false" />
											</c:if>
											<c:if test="${serverStats.type == 'LAST_SYNC_TIME_SOMEWHAT_TROUBLESOME'}">
												<div class="syncStatsAttention"><spring:message code="sync.overview.warning.lastSyncTimeTroublesome" arguments="${recordsPending}"/></div>
												<c:set var="allSyncStatsOk" value="false" />
											</c:if>
											<c:if test="${recordsPending > 0}">
												<div class="syncStatsAttention"><spring:message code="sync.overview.pending" arguments="${recordsPending}"/></div>
												<c:set var="allSyncStatsOk" value="false" />
											</c:if>
											<c:if test="${server.syncInProgress}">
												<div class="syncStatsAttention"><spring:message code="sync.config.warning.syncInProgress" arguments="${server.syncInProgressMinutes}"/></div><br/>
												<c:set var="allSyncStatsOk" value="false" />
											</c:if>
											<c:if test="${allSyncStatsOk=='true'}">
												<div class="syncStatsOK"><spring:message code="sync.overview.fullySyncd"/></div>
											</c:if>
										</td>
									</tr>
								</c:otherwise>
							</c:choose>
							</c:forEach>
							
							<c:choose>
								<c:when test="${bgStyle == 'eee'}">
									<c:set var="bgStyle" value="fff" />
									<c:set var="bgStyleReceive" value="eef" />				
									<c:set var="bgStyleSend" value="efe" />				
								</c:when>
								<c:otherwise>
									<c:set var="bgStyle" value="eee" />
									<c:set var="bgStyleFile" value="dde" />				
									<c:set var="bgStyleWebMan" value="ded" />				
									<c:set var="bgStyleWebAuto" value="edd" />				
								</c:otherwise>
							</c:choose>
						</c:forEach>
					</c:if>
					<c:if test="${empty commandObject.serverList}">
						<td colspan="3" align="left">
							<i><spring:message code="sync.config.servers.noItems" /></i>
						</td>
					</c:if>
				</tbody>
			</table>
		</div>
	</div>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
