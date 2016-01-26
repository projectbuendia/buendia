<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="View Synchronization Status"
	otherwise="/login.htm"
	redirect="/module/sync/settings.list" />

<%@ include file="/WEB-INF/template/header.jsp"%>

<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />
<openmrs:htmlInclude file="/dwr/util.js" />

<%@ include file="localHeader.jsp"%>

<script language="JavaScript">
	<!--
	
		function showHideDiv(id) {
			var div = document.getElementById(id);
			if ( div ) {
				if ( div.style.display != "none" ) {
					div.style.display = "none";
				} else { 
					div.style.display = "";
				}
			}
		}
		function empty(id) {
			var div = document.getElementById(id);
			if ( div ) {
				div.value = "";
			}
		}
		function showDiv(id) {
			var div = document.getElementById(id);
			if ( div ) {
				div.style.display = "";
			}
		}
		
		function hideDiv(id) {
			var div = document.getElementById(id);
			if ( div ) {
				div.style.display = "none";
			}
		}
		
	//Called to disable content when sync is disabled
	function disableDIVs() {
		hideDiv('advanced');
		hideDiv('serverList');
	}

	function confirmDelete(id) {
		var isConfirmed = confirm("<spring:message code="sync.config.server.confirmDelete" />");
		if ( isConfirmed ) {
			document.getElementById("deleteServer" + id).submit();
		}
	}
		
</script>

<table>
	<tr>
		<td>
		<h2><spring:message code="sync.settings.title" /></h2>
		</td>
	</tr>
</table>
<div id="general"><b class="boxHeader"><spring:message
	code="sync.settings.general" /></b>
<div class="box">
<form id="settingsForm" name="settingsForm" method="post"
	action="synchronizationSettings.list"><input type="hidden"
	name="action" value="saveGeneral" />
<table id="general" width="99%" border="0" cellpadding="0"
	cellspacing="0">
	<tbody>
		<tr>
			<td width="25%" height="32" align="right"><spring:message
				code="sync.settings.status.is" /></td>
			<td width="2%" height="32">&nbsp;</td>
			<td height="32" colspan="3">${localServerSyncStatusText}<input
				type="checkbox" id="syncEnable" name="syncEnable" value="true"
				<c:if test="${syncStatus=='ENABLED_CONTINUE_ON_ERROR' || syncStatus=='ENABLED_STRICT'}">checked="true"</c:if> />
			<spring:message code="sync.settings.sync.enable" /></td>
			<td align="left"></td>
		</tr>
		<tr style='display: < c : if test = "${empty parent || syncStatus=='DISABLED_SYNC' || syncStatus=='DISABLED_SYNC_DUE_TO_ERROR' || syncStatus=='DISABLED_SYNC_AND_HISTORY' }" > none </ c : if >'>
            <td width="25%" height="32" align="right"><spring:message code="sync.settings.sync.auto"/></td>
            <td width="2%" height="32">&nbsp;</td>
            <td height="32" colspan="3"><input  type="checkbox" id="started" name="started" value="true" <c:if test="${syncStatus!='ENABLED_CONTINUE_ON_ERROR' && syncStatus!='ENABLED_STRICT'}">disabled="true"</c:if><c:if test="${serverSchedule.started==true}">checked="true"</c:if> onClick="document.getElementById('every').style.display = (this.checked == true) ? '' : 'none';" />
			  <span id="every"  style="display:<c:if test="${serverSchedule.started==false}">none</c:if>"><img src="${pageContext.request.contextPath}/images/scheduled_send.gif" border="0" style="margin-bottom: -3px;"><spring:message code="sync.settings.every"/>
                                  <input  name="repeatInterval" type="text" size="4" value="${repeatInterval}" />
                                <spring:message code="sync.settings.min"/></span>								</td>
				<td align="left">								</td>
          </tr>
          <tr>
            <td width="25%" height="32" align="right"><spring:message code="sync.settings.server.name"/></td>
            <td width="2%" height="32">&nbsp;</td>
            <td height="32" colspan="3"><input type="text" name="serverName" value="${localServerName}" /></td>
          </tr>
          <tr>
            <td width="25%" height="32" align="right"><spring:message code="sync.settings.admin.email"/></td>
            <td width="2%" height="32">&nbsp;</td>
            <td height="32" colspan="3"><input name="adminEmail" type="text" size="30"  value="${localServerAdminEmail}"/></td>
          </tr>
          <tr>
            <td width="25%" height="32" align="right"><spring:message code="sync.settings.server.id"/></td>
            <td width="2%" height="32">&nbsp;</td>
            <td height="32">${localServerGuid}</td>
            <td align="right">&nbsp;</td>
            <td align="right"><input name="save" type="submit" id="save" value="<spring:message code="general.save" />" />
            <input name="cancel" type="button" id="cancel" value="<spring:message code="general.cancel" />" onClick="location.reload();" /></td>
          </tr>
		  </tbody>
      </table>
	</form>
   </div>
 </div>
 <br/>
 <div id="serverList">
      <b class="boxHeader"><spring:message code="sync.settings.servers.title"/></b>
   <div class="box">
	  
        <table width="958" height="52" border="0" align="center" cellpadding="0" cellspacing="0">
          <tbody><tr>
            <td height="25" align="center" bgcolor="#E8EBF0"><b><spring:message code="sync.settings.servers.name"/></b></td>
            <td height="25" align="center" bgcolor="#E8EBF0"><b><spring:message code="sync.settings.servers.type"/></b></td>
            <td height="25" align="center" bgcolor="#E8EBF0"><b><spring:message code="sync.settings.servers.address"/></b></td>
            <td height="25" align="center" bgcolor="#E8EBF0"><b><spring:message code="sync.settings.servers.attempt"/></b></td>
            <td height="25" align="center" bgcolor="#E8EBF0">&nbsp;</td>
            <td height="25" align="center" bgcolor="#E8EBF0">&nbsp;</td>
            <td height="25" align="center">&nbsp;</td>
          </tr>
		  <c:set var="bgs" value="EDEED2" />
		  <c:forEach var="server" items="${synchronizationSettingsList.serverList}" varStatus="status">
		  <a href="synchronizationConfigServer.form?serverId=${server.serverId}">
          <tr class="syncTr" bgcolor="#${bgs}">
            <td height="25" align="center"><b><a href="synchronizationConfigServer.form?serverId=${server.serverId}">${server.nickname}</a></b></td>
            <td height="25" align="center">${server.serverType}</td>
            <td height="25" align="center">${server.address}</td>
            <td height="25" align="center">              <openmrs:formatDate date="${server.lastSync}" format="${syncDateDisplayFormat}" />            </td>
            <td height="25" align="center"><b><a href="synchronizationConfigServer.form?serverId=${server.serverId}"><spring:message code="general.edit"/></a></b></td>
            <td height="25" align="center">
              <c:choose>
					<c:when test="${server.serverType != 'PARENT'}"> 
			  
			            <form id="deleteServer${server.serverId}" action="synchronizationSettings.list" method="post">
							
						  <input type="hidden" name="action" value="deleteServer" />
						  <input type="hidden" id="serverId" name="serverId" value="${server.serverId}" />
						  <a href="javascript:confirmDelete('${server.serverId}');"><img src="<%= request.getContextPath() %>/images/trash.gif" alt="delete" border="0" /><span><spring:message code="general.remove"/></span></a>						    
            </form>             </c:when>
					<c:otherwise>
					</c:otherwise>
				</c:choose>         </td>
				
				<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </a>
		  </c:forEach>
     </tbody></table>
		
        <table width="958" height="27" border="0" cellpadding="0" align="center" cellspacing="0">
          <tbody><c:if test="${localServerSyncStatusValue == 'ENABLED_STRICT' || localServerSyncStatusValue == 'ENABLED_CONTINUE_ON_ERROR'}">
		  <tr align="center" >
            <td height="25" align="center"><img src="${pageContext.request.contextPath}/images/add.gif" style="margin-bottom: -3px;" border="0" /><a href="synchronizationConfigServer.form?type=CHILD"><spring:message code="sync.settings.add.child"/></a></td>
            <td height="25" align="center"><c:if test="${empty parent}"><a href="synchronizationConfigServer.form?type=CHILD"><img src="${pageContext.request.contextPath}/images/add.gif" style="margin-bottom: -3px;" border="0" /></a><a href="synchronizationConfigServer.form?type=PARENT"><spring:message code="sync.settings.add.parent"/></a></c:if></td>
          </tr>
		  </c:if>
		  </tbody>
     </table>
   </div>
</div>
<br>
		&nbsp;&nbsp;<a href="javascript:showHideDiv('advanced');"><spring:message code="sync.settings.adv.set"/></a></br>
<div id="advanced" style="display:none;">

	<form action="synchronizationSettings.list" method="post">
	<input type="hidden" name="action" value="saveClasses" />
	
		<b class="boxHeader"><spring:message code="SynchronizationConfig.advanced.objects"/></b>
		<div class="box">
			<table>
				<tr>
					<td style="padding-right: 80px;" valign="top">
						<table id="syncChangesTable" cellpadding="4" cellspacing="0">
							<thead>
								<tr>
									<th colspan="2" valign="bottom"><spring:message code="SynchronizationConfig.class.item" /></th>
									<th colspan="2" align="center">&nbsp;&nbsp;<spring:message code="general.default.behavior" /></th>
								</tr>
							</thead>
							<tbody id="globalPropsList">
								<c:if test="${not empty syncClassGroupsLeft}">
									<c:forEach var="syncClasses" items="${syncClassGroupsLeft}" varStatus="status">
										<tr>
											<td style="border-top: 1px solid #aaa; background-color: whitesmoke;" colspan="2" align="left">
												<b>${syncClasses.key}</b>
											</td>
											<td style="padding-right: 20px; border-top: 1px solid #aaa; background-color: whitesmoke;" align="center">
												<input onclick="toggleChecks('${syncClasses.key}', 'to');" id="to_${syncClasses.key}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="groupToDefault" value="true" <c:if test="${syncClassGroupTo[syncClasses.key]}">checked</c:if>
													 <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
												><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><b><spring:message code="SynchronizationConfig.class.defaultTo" /></b></span>
											</td>
											<td style="border-top: 1px solid #aaa; background-color: whitesmoke;" align="center">
												<input onclick="toggleChecks('${syncClasses.key}', 'from');" id="from_${syncClasses.key}" style="margin-top: 0px; margin-bottom: 0px; margin-right: 1px;" type="checkbox" name="groupFromDefault" value="true" <c:if test="${syncClassGroupFrom[syncClasses.key]}">checked</c:if>
													 <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
												><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><b><spring:message code="SynchronizationConfig.class.defaultFrom" /></b></span>
											</td>
										</tr>
										<c:if test="${not empty syncClasses.value}">
											<c:forEach var="syncClass" items="${syncClasses.value}" varStatus="statusClass">
												<tr>
													<td>&nbsp;</td>
													<td align="left">
														${syncClass.name}
													</td>
													<td align="center" style="padding-right: 20px;">
														<input id="to_${syncClass.syncClassId}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="toDefault" value="${syncClass.syncClassId}" 
															<c:if test="${syncClass.defaultTo}">checked</c:if> <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
														><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><spring:message code="SynchronizationConfig.class.defaultTo" /></span>
													</td>
													<td align="center">
														<input id="from_${syncClass.syncClassId}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="fromDefault" value="${syncClass.syncClassId}" 
															<c:if test="${syncClass.defaultFrom}">checked</c:if> <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
														><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><spring:message code="SynchronizationConfig.class.defaultFrom" /></span>
													</td>
												</tr>
											</c:forEach>
										</c:if>
										<c:if test="${empty syncClasses.value}">
											<td colspan="5" align="left">
												<i><spring:message code="SynchronizationConfig.classes.none" /></i>
											</td>
										</c:if>
									</c:forEach>
								</c:if>
								<c:if test="${empty syncClassGroupsLeft}">
									<td colspan="4" align="left">
										<i><spring:message code="SynchronizationConfig.classes.none" /></i>
									</td>
								</c:if>
							</tbody>
						</table>
					</td>
					<td valign="top">
						<table id="syncChangesTable" cellpadding="4" cellspacing="0">
							<thead>
								<tr>
									<th colspan="2" valign="bottom"><spring:message code="SynchronizationConfig.class.item" /></th>
									<th colspan="2" align="center">&nbsp;&nbsp;<spring:message code="general.default.behavior" /></th>
								</tr>
							</thead>
							<tbody id="globalPropsList">
								<c:if test="${not empty syncClassGroupsRight}">
									<c:forEach var="syncClasses" items="${syncClassGroupsRight}" varStatus="status">
										<tr>
											<td style="border-top: 1px solid #aaa; background-color: whitesmoke;" colspan="2" align="left">
												<b>${syncClasses.key}</b>
											</td>
											<td style="padding-right: 20px; border-top: 1px solid #aaa; background-color: whitesmoke;" align="center">
												<input onclick="toggleChecks('${syncClasses.key}', 'to');" id="to_${syncClasses.key}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="groupToDefault" value="true" <c:if test="${syncClassGroupTo[syncClasses.key]}">checked</c:if>
													 <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
												><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><b><spring:message code="SynchronizationConfig.class.defaultTo" /></b></span>
											</td>
											<td style="border-top: 1px solid #aaa; background-color: whitesmoke;" align="center">
												<input onclick="toggleChecks('${syncClasses.key}', 'from');" id="from_${syncClasses.key}" style="margin-top: 0px; margin-bottom: 0px; margin-right: 1px;" type="checkbox" name="groupFromDefault" value="true" <c:if test="${syncClassGroupFrom[syncClasses.key]}">checked</c:if>
													 <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
												><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><b><spring:message code="SynchronizationConfig.class.defaultFrom" /></b></span>
											</td>
										</tr>
										<c:if test="${not empty syncClasses.value}">
											<c:forEach var="syncClass" items="${syncClasses.value}" varStatus="statusClass">
												<tr>
													<td>&nbsp;</td>
													<td align="left">
														${syncClass.name}
													</td>
													<td align="center" style="padding-right: 20px;">
														<input id="to_${syncClass.syncClassId}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="toDefault" value="${syncClass.syncClassId}" 
															<c:if test="${syncClass.defaultTo}">checked</c:if> <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
														><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><spring:message code="SynchronizationConfig.class.defaultTo" /></span>
													</td>
													<td align="center">
														<input id="from_${syncClass.syncClassId}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="fromDefault" value="${syncClass.syncClassId}" 
															<c:if test="${syncClass.defaultFrom}">checked</c:if> <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
														><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><spring:message code="SynchronizationConfig.class.defaultFrom" /></span>
													</td>
												</tr>
											</c:forEach>
										</c:if>
										<c:if test="${empty syncClasses.value}">
											<td colspan="5" align="left">
												<i><spring:message code="SynchronizationConfig.classes.none" /></i>
											</td>
										</c:if>
									</c:forEach>
								</c:if>
								<c:if test="${empty syncClassGroupsRight}">
									<td colspan="4" align="left">
										<i><spring:message code="SynchronizationConfig.classes.none" /></i>
									</td>
								</c:if>
							</tbody>
						</table>
					</td>
				</tr>
				<tr>
					<td colspan="2" align="center">
						<input type="submit" value="<spring:message code="general.save" />" />
						<input type="button" onclick="location.reload();" value="<spring:message code="general.cancel" />" />
					</td>
				</tr>
			</table>
		</div>

	</form>
</div>
<!-- turn off content based on value of  localServerSyncStatusValue-->
<c:if test="${localServerSyncStatusValue == 'DISABLED_SYNC_AND_HISTORY'}">			
	<script language="JavaScript">
		disableDIVs();
	</script>
</c:if>
<%@ include file="/WEB-INF/template/footer.jsp" %>
