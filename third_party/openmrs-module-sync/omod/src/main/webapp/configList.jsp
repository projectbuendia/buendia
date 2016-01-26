<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/config.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />
<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />

<%@ include file="localHeader.jsp" %>

<style>
 .PARENT {
   font-weight: bold;
  }
</style>

<script language="JavaScript">
	function confirmChildServerDeletetion(id) {
		var isConfirmed = confirm("<spring:message code="sync.config.child.server.confirmDelete" />");
		if ( isConfirmed ) {
			document.getElementById("deleteServer" + id).submit();
		}
	}
	
	function confirmParentServerDeletetion(id) {
		var isConfirmed = confirm("<spring:message code="sync.config.parent.server.confirmDelete" />");
		if ( isConfirmed ) {
			var isTwiceConfirmed = confirm("<spring:message code="sync.config.parent.server.SecondConfirmation" />");
			if ( isTwiceConfirmed ) {
			document.getElementById("deleteServer" + id).submit();
			}
		}
	}
</script>

<h2><spring:message code="sync.config.title"/></h2>

<div id="general">
	
	<div id="serverList">
		<b class="boxHeader"><spring:message code="sync.config.servers.remote"/></b>
		<div class="box">
			<table id="syncChangesTable" cellpadding="10" cellspacing="0">
				<c:if test="${not empty configListBackingObject.serverList}">
					<thead>
						<tr>
							<th></th>
							<th></th>
							<th></th>
							<openmrs:hasPrivilege privilege="Manage Synchronization">
								<th align="center" colspan="2" style="background-color: #eef3ff; text-align: center; font-weight: normal;"><spring:message code="sync.config.server.synchronize.manually" /></th>
								<th align="center" style="text-align: center; background-color: #fee; font-weight: normal;"><spring:message code="sync.config.server.synchronize.automatic" /></th>
								<th></th>
							</openmrs:hasPrivilege>
						</tr>
						<tr style="text-align: center;">
							<th style="text-align: left"><spring:message code="sync.config.server.name" /></th>
							<th><spring:message code="sync.config.server.type" /></th>
							<th><spring:message code="sync.config.server.lastSync" /></th>
							<openmrs:hasPrivilege privilege="Manage Synchronization">
								<th style="background-color: #eef;"><img src="${pageContext.request.contextPath}/images/save.gif" border="0" style="margin-bottom: -3px;">
									<spring:message code="sync.config.server.syncViaFile" /></th>
								<th style="background-color: #efe;"><img src="${pageContext.request.contextPath}/images/lookup.gif" border="0" style="margin-bottom: -3px;">
									<spring:message code="sync.config.server.syncViaWeb" /></th>
								<th style="background-color: #fee;"><img src="${pageContext.request.contextPath}/moduleResources/sync/scheduled_send.gif" border="0" style="margin-bottom: -3px;">
									<spring:message code="sync.config.server.syncAutomatic" />
									(<spring:message code="sync.general.scheduled" />)</th>
								<th><spring:message code="sync.config.server.delete" /></th>
							</openmrs:hasPrivilege>
						</tr>
					</thead>
					<tbody>
						<c:set var="bgStyle" value="eee" />				
						<c:set var="bgStyleFile" value="dde" />				
						<c:set var="bgStyleWebMan" value="ded" />				
						<c:set var="bgStyleWebAuto" value="edd" />				
						<c:forEach var="server" items="${configListBackingObject.serverList}" varStatus="status">
							<tr style="background-color: #${bgStyle}; text-align: center;">
								<td nowrap style="text-align: left; padding-bottom: 0px;">
									<openmrs:hasPrivilege privilege="Manage Synchronization">
										<a href="configServer.form?serverId=${server.serverId}">
									</openmrs:hasPrivilege>
										<b>${server.nickname}</b>
									<openmrs:hasPrivilege privilege="Manage Synchronization">
										</a>
									</openmrs:hasPrivilege>
								</td>
								<td class="${server.serverType}" style="padding-bottom: 0px;">
									${server.serverType}
								</td>
								<td style="padding-bottom: 0px;">
									<openmrs:formatDate date="${server.lastSync}" format="${syncDateDisplayFormat}" />
								</td>
								<openmrs:hasPrivilege privilege="Manage Synchronization">
									<td style="background-color: #${bgStyleFile}; padding-bottom: 0px;">
										<c:choose>
											<c:when test="${server.serverType == 'CHILD'}">
												<a href="import.list?serverId=${server.serverId}">
													<spring:message code="sync.config.server.uploadAndReply" />
												</a>
											</c:when>
											<c:otherwise>
												<a href="status.list?mode=SEND_FILE">
													<spring:message code="sync.config.server.sendFile" />
												</a>
												&nbsp;
												<a href="status.list?mode=UPLOAD_REPLY">
													<spring:message code="sync.config.server.uploadResponse" />
												</a>
											</c:otherwise>
										</c:choose>
									</td>
									<td style="background-color: #${bgStyleWebMan}; padding-bottom: 0px;">
										<c:choose>
											<c:when test="${server.serverType == 'CHILD'}">
												<span title="<spring:message code='sync.config.server.na.childWebSyncNotApplicable'/>"><spring:message code="sync.config.server.na"/></span>
											</c:when>
											<c:otherwise>
												<a href="status.list?mode=SEND_WEB">
													<spring:message code="sync.config.server.synchronizeNow" />
												</a>
											</c:otherwise>
										</c:choose>
									</td>
									<td style="background-color: #${bgStyleWebAuto}; padding-bottom: 0px;">
										<c:choose>
											<c:when test="${server.serverType == 'CHILD'}">
												<span title="<spring:message code='sync.config.server.na.childWebSyncNotApplicable'/>"><spring:message code="sync.config.server.na"/></span>
											</c:when>
											<c:otherwise>
												<c:if test="${parentSchedule.started == false}">
													(<spring:message code="sync.config.parent.not.scheduled" />)
													<a href="configServer.form?serverId=${server.serverId}" style="font-size: 0.9em;">
														<spring:message code="sync.general.configure" />
													</a>
												</c:if>
												<c:if test="${parentSchedule.started == true}">
													<spring:message code="sync.config.parent.scheduled.every" />
													<b>${repeatInterval}</b>
													<spring:message code="sync.config.parent.scheduled.minutes" />
													<a href="configServer.form?serverId=${server.serverId}" style="font-size: 0.9em;">
														<spring:message code="sync.general.configure" />
													</a>
												</c:if>
											</c:otherwise>
										</c:choose>
									</td>
									<td style="padding-bottom: 0px;">
												<form id="deleteServer${server.serverId}" action="config.list" method="post">
													<input type="hidden" name="action" value="deleteServer" />
													<input type="hidden" id="serverId" name="serverId" value="${server.serverId}" />
													
													<c:if test="${server.serverType == 'CHILD'}">
													<a href="javascript:confirmChildServerDeletetion('${server.serverId}');"><img src="<%= request.getContextPath() %>/images/trash.gif" alt="delete" border="0" /></a>
													</c:if>
													<c:if test="${server.serverType == 'PARENT'}">
													<a href="javascript:confirmParentServerDeletetion('${server.serverId}');"><img src="<%= request.getContextPath() %>/moduleResources/sync/trash_warning.gif" alt="delete" border="0" /></a>													</c:if>	
												</form>
									</td>
								</openmrs:hasPrivilege>								
							</tr>
							<openmrs:hasPrivilege privilege="Manage Synchronization">
								<tr>
									<td colspan="3" style="background-color: #${bgStyle}; padding-top: 0px;">
										<c:if test="${empty server.uuid}">
											<span class="syncStatsWarning"><spring:message code="sync.config.warning.uuid"/></span><br/>
										</c:if>
										<c:if test="${server.syncInProgress}">
											<span class="syncStatsAttention"><spring:message code="sync.config.warning.syncInProgress" arguments="${server.syncInProgressMinutes}"/></span><br/>
										</c:if>
									</td>
									<td style="background-color: #${bgStyleFile}; padding-top: 0px;"></td>
									<td style="background-color: #${bgStyleWebMan}; padding-top: 0px;"></td>
									<td style="background-color: #${bgStyleWebAuto}; padding-top: 0px;"></td>
									<td style="background-color: #${bgStyle}; padding-top: 0px;"></td>
								</tr>
							</openmrs:hasPrivilege>	
							<c:choose>
								<c:when test="${bgStyle == 'eee'}">
									<c:set var="bgStyle" value="fff" />
									<c:set var="bgStyleFile" value="eef" />				
									<c:set var="bgStyleWebMan" value="efe" />				
									<c:set var="bgStyleWebAuto" value="fee" />				
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
					<c:if test="${empty configListBackingObject.serverList}">
						<td colspan="7" align="left">
							<i><spring:message code="sync.config.servers.noItems" /></i>
						</td>
					</c:if>
					<openmrs:hasPrivilege privilege="Manage Synchronization">
						<tr>
							<td colspan="7">
								<br>
								<a href="configCurrentServer.form"><spring:message code="sync.config.server.config.current" /></a>
								|
								<a href="configServer.form?type=CHILD"><img src="${pageContext.request.contextPath}/images/add.gif" style="margin-bottom: -3px;" border="0" /></a>
								<a href="configServer.form?type=CHILD"><spring:message code="sync.config.server.config.child" /></a>
								<c:if test="${empty parent}">
									 |
									<a href="configServer.form?type=PARENT"><img src="${pageContext.request.contextPath}/images/add.gif" style="margin-bottom: -3px;" border="0" /></a>
									<a href="configServer.form?type=PARENT"><spring:message code="sync.config.server.config.parent" /></a>
								</c:if>
							</td>
						</tr>
					</openmrs:hasPrivilege>
				</tbody>
			</table>
		</div>
	</div>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
