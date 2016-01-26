<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/maintenance.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp"%>
<style>
 .descriptionBox {
 	border-color: transparent;
 	border-width: 1px;
 	overflow-y: auto;
 	background-color: transparent;
 	padding: 1px;
 	height: 2.7em;
 }
 td.description {
 	padding-top: 0px;
 }
 #buttonsAtBottom {
 	padding: 5px;
 }
</style>

<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/dwr/engine.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />

<h2><spring:message code="sync.maintenance.title"/></h2>

<script language="JavaScript">
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
	
	function hideDiv(id){
		var div = document.getElementById(id);
		div.style.display = "none";
	}
	
	function showDiv(id){
		var div = document.getElementById(id);
		div.style.display = "";
	}
	function enableDiv(id){
		var div = document.getElementById(id);
		div.disabled = false;
	}
	function disableDiv(id){
		var div = document.getElementById(id);
		div.disabled = true;
	}
	function showJournalArchiveResult(result){
		
		if(result){
			DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/accept.gif' border='0'>" + "&nbsp;" +"<span class='syncCOMMITTED'><b><spring:message code='sync.maintenance.archive.journal.success' /></b></span>");
		}
		else{
			DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/error.gif' border='0'>" + "&nbsp;" +"<span class='syncFAILED'><b><spring:message code='sync.maintenance.archive.journal.error'/></b></span>");
		}
		
		setTimeout("enableDiv('archiveJournalButton');DWRUtil.setValue('archiveResult','');", 4000);
			
	}
	
	function archiveSyncJournal(clearDir){
	disableDiv('archiveJournalButton');
	DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/loader.gif' border='0'>" + "&nbsp;" +"<span class='syncNEUTRAL'><b><spring:message code='sync.maintenance.archive.journal.progress' /></b></span>");
		DWRSyncService.archiveSyncJournal(clearDir,showJournalArchiveResult);
	}
	
	function showImportArchiveResult(result){
		if(result){
			DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/accept.gif' border='0'>" + "&nbsp;" +"<span class='syncCOMMITTED'><b><spring:message code='sync.maintenance.archive.import.success' /></b></span>");
		}
		else{
			DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/error.gif' border='0'>" + "&nbsp;" +"<span class='syncFAILED'><b><spring:message code='sync.maintenance.archive.import.error' /></b></span>");
		}
		setTimeout("enableDiv('archiveImportButton');DWRUtil.setValue('archiveResult','');", 4000);
	}
	function archiveSyncImport(clearDir){
	disableDiv('archiveImportButton');
	DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/loader.gif' border='0'>" + "&nbsp;" +"<span class='syncNEUTRAL'><b><spring:message code='sync.maintenance.archive.import.progress' /></b></span>");
		DWRSyncService.archiveSyncImport(clearDir,showImportArchiveResult);
	}
	
	function removeProperty(btn) {
		btn.parentNode.parentNode.parentNode.removeChild(btn.parentNode.parentNode);
	}
	
	function addNewProperty() {
		var propsTable = document.getElementById("propertiesTable");
		var blankPropRow = document.getElementById("newProperty");
		if(blankPropRow && propsTable){
			var newPropRow = blankPropRow.cloneNode(true);
			$j(newPropRow).show();
			newPropRow.id = '';
		
			propsTable.appendChild(newPropRow);
		}
	}
</script>
<spring:hasBindErrors name="task">
	<spring:message code="fix.error"/>
	<div class="error">
		<c:forEach items="${errors.allErrors}" var="error">
			<spring:message code="${error.code}" text="${error.code}"/><br/><!-- ${error} -->
		</c:forEach>
	</div>
	<br />
</spring:hasBindErrors>
<b class="boxHeader"><spring:message code="sync.maintenance.search.title"/></b>
<div class="box">
<form action="" method="GET">
  <label><strong><spring:message code="sync.maintenance.keyword"/></strong>
  <input type="text" id="keyword" name="keyword" value="${keyword}">
  </label>
  <input type="submit" id="searchButton" value="Search">
</form>
<c:if test="${not empty synchronizationMaintenanceList}">
<div style="position: relative; border: 1px solid gray; margin: 10px; padding: 0px;">
<table width="100%" border="0" align="center" cellpadding="0"
	cellspacing="0">
	<tr bgcolor="#E1E4EA">
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.type" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.name" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.action" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.timestamp" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.status" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.retryCount" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"></td>

	</tr>
	
		<c:set var="bgs" value="EDEED2" />
		<c:forEach var="syncRecord" items="${synchronizationMaintenanceList}"
			varStatus="status">
			<tr class="syncTr" bgcolor="#${bgs}"
				onclick="location='viewrecord.form?uuid=${syncRecord.uuid}'"
				height="25">
				<td align="left" valign="middle" style="padding: 8px;"><b><a
					href="viewrecord.list?uuid=${syncRecord.uuid}">${recordTypes[syncRecord.uuid]}</a></b></td>
				<td align="left" valign="middle" style="padding: 8px;"><c:if
					test="${not empty recordText[syncRecord.uuid]}">${recordText[syncRecord.uuid]}</c:if>
				</td>
				<td align="left" valign="middle" style="padding: 8px;"><spring:message
					code="sync.item.state_${recordChangeType[syncRecord.uuid]}" /></td>
				<td align="left" valign="middle" style="padding: 8px;"><openmrs:formatDate
					date="${syncRecord.timestamp}" format="${syncDateDisplayFormat}" /></td>
				<td align="left" valign="middle" style="padding: 8px;" id="state_${syncRecord.uuid}"><span
					class="sync${syncRecord.state}"> <spring:message
					code="sync.record.state_${syncRecord.state}" /></span></td>
				<td valign="middle" style="padding: 8px;">${syncRecord.retryCount}</td>
				<td valign="middle" style="padding: 8px;"><span id="message_${syncRecord.uuid}"></span></td>
				<c:choose>
					<c:when test="${bgs == 'EDEED2'}">
						<c:set var="bgs" value="F7F7EA" />
					</c:when>
					<c:otherwise>
						<c:set var="bgs" value="EDEED2" />
					</c:otherwise>
				</c:choose>
			</tr>
		</c:forEach>
</table>
</div>
</c:if>
<c:if test="${maxPages > 1}">
	<table width="100%" border="0" cellspacing="0" cellpadding="0">
		<tr>
			<td>&nbsp;</td>
			<td align="center" valign="middle"><span><spring:message
				code="sync.maintenance.goto" />:</span> <c:forEach var="p"
				begin="${1}" end="${maxPages}" step="${1}">
		 	|
			<c:choose>
					<c:when test="${p==currentPage}">
						<span class="syncPageNum"><a href="?keyword=${keyword}&page=${p}" style="font-size: 18px">${p}</a></span>
					</c:when>
					<c:otherwise>
						<span class="syncPageNum"><a href="?keyword=${keyword}&page=${p}">${p}</a></span>
					</c:otherwise>
			  </c:choose>
			</c:forEach></td>
			<td>&nbsp;</td>
		</tr>
	</table>
</c:if> 
<c:if test="${empty synchronizationMaintenanceList}">
	<table>
		<tr>
			<td align="center" valign="middle"><i><spring:message
				code="sync.maintenance.noItems" /> <strong>${keyword}</strong></i></td>
		</tr>
	</table>
</c:if>
</div>
<openmrs:hasPrivilege privilege="Manage Synchronization">
	<br/>
	<b class="boxHeader"><spring:message code="sync.maintenance.backport"/></b>
	<div class="box">
		<br/>
		<span><spring:message code="sync.maintenance.backport.description" /></span>
			<form method="post" action="">
				<table>
					<tr>
						<td><spring:message code="sync.config.server.name" /></td>
						<td>
							<select name="server">
								<option value=""><spring:message code="sync.maintenance.backport.chooseServer" /></option>
								<c:forEach var="server" items="${servers}">
									<option value="${server.serverId}">${server.nickname}</option>
								</c:forEach>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<td><spring:message code="sync.maintenance.backport.date" /></td>
						<td><input type="text" name="date" value="" id="backport_date_input"/></td>
						<td class="description">${datePattern}</td>
					</tr>
					<tr>
						<td></td>
						<td>
							<input type="hidden" value="backporting" name="action"/>
							<input type="submit" value="<spring:message code="general.submit"/>"/>
						</td>
						<td></td>
					</tr>
				</table>
				
			</form>
	</div>
	
	<br/>
	<b class="boxHeader"><spring:message code="sync.maintenance.archive.title"/></b>
	<div class="box"><br/>
	<span><spring:message code="sync.maintenance.archive.description" /></span>
		<ul>
			<li>
				<span>
					<spring:message code="sync.maintenance.archive.journal" />
					<input id="archiveJournalButton" type="button"  onclick="archiveSyncJournal(true);" value="<spring:message code="sync.maintenance.archive.now" />"/></span>
			</li>
			<br/>
			<li>
				<span><spring:message code="sync.maintenance.archive.import" />
				<input id="archiveImportButton" type="button"  onclick="archiveSyncImport(true);" value="<spring:message
				code="sync.maintenance.archive.now" />" /></span>
			</li>
		</ul>
		<br/>
	<div id="archiveResult">
	
	</div>
	<br/>
	</div>
</openmrs:hasPrivilege>

<br />

<%-- If the task exists(not deleted) --%>
<c:if test="${task.id != null}">
<div class="boxHeader"><spring:message code="sync.maintenance.manage.cleanUpOldRecordsTaskProperties" /></div>
<div class="box">
<form method="post">
	<input type="hidden" name="taskId" value="${task.id}" />
	<table>
		<tr>
			<th valign="top"><spring:message code="Scheduler.scheduleForm.startTime"/></th>
			<td>
				<spring:bind path="task.startTime">
					<input type="text" id="startTime" name="startTime" size="25" value="${status.value}"/> 
					<b><i><spring:message code="Scheduler.scheduleForm.startTimePattern"/>: </i></b><spring:message code="Scheduler.scheduleForm.startTime.pattern"/>
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<th valign="top"><spring:message code="Scheduler.scheduleForm.repeatInterval"/></th>
			<td>
				<spring:bind path="task.repeatInterval">
					<input type="text" id="repeatInterval" name="repeatInterval" size="10" value="${repeatInterval}" />
					<select name="repeatIntervalUnits">
						<option value="seconds" <c:if test="${units=='seconds'}">selected="selected"</c:if>><spring:message code="Scheduler.scheduleForm.repeatInterval.units.seconds" /></option>
						<option value="minutes" <c:if test="${units=='minutes'}">selected="selected"</c:if>><spring:message code="Scheduler.scheduleForm.repeatInterval.units.minutes" /></option>
						<option value="hours" <c:if test="${units=='hours'}">selected="selected"</c:if>><spring:message code="Scheduler.scheduleForm.repeatInterval.units.hours" /></option>
						<option value="days" <c:if test="${units=='days'}">selected="selected"</c:if>><spring:message code="Scheduler.scheduleForm.repeatInterval.units.days" /></option>
					</select>
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<th valign="top"><spring:message code="general.properties"/>:</th>
			<td>
				<table id="propertiesTable">
					<tr>
						<th><spring:message code="general.name" /></th>
						<th><spring:message code="general.value" /></th>
						<th></th>
					</tr>
					<c:forEach var="property" items="${task.properties}">			
					<tr>
						<td><input type="text" name="propertyName" size="20" value="${property.key}" /></td>
						<td><input type="text" name="propertyValue" size="30" value="${property.value}" /></td>
						<td>
							<input type="button" class="smallButton" onclick="removeProperty(this)" value="<spring:message code="general.remove"/>" />
						</td>
					</tr>
					</c:forEach>
					<tr id="newProperty" style="display: none">
						<td>
							<input type="text" name="propertyName" size="20"/> 
						</td>
						<td>
							<input type="text" name="propertyValue" size="30"/> 
						</td>
						<td>
							<input type="button" class="smallButton" onclick="removeProperty(this)" value="<spring:message code="general.remove"/>">
						</td>
					</tr>
				</table>
				<input type="button" class="smallButton" onclick="addNewProperty(this)" value="<spring:message code="general.add"/>" />
			</td>
		</tr>
	</table>
	<br /><br />
	<input type="submit" value="<spring:message code="general.save"/>">
</form>
</div>
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp" %>