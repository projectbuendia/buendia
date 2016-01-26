<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/configCurrentServer.form" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<h2><spring:message code="sync.config.current"/></h2>

<div id="general">
	<form method="post" action="configCurrentServer.form">
		<input type="hidden" name="action" value="save" />
			
		<b class="boxHeader"><spring:message code="sync.config.server.configure"/></b>
		<div class="box">
			<table>
				<tr>
					<td align="right" valign="top">
						<b><spring:message code="sync.config.server.nickname" /></b>
					</td>
					<td align="left" valign="top">
						<input type="text" size="25" maxlength="250" id="serverName" name="serverName" value="${localServerName}" />
					</td>
					<td>
						<spring:message code="sync.config.advanced.serverName.info" />
					</td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.serverUuid" /></b></td>
					<td><input type="text" size="50" name="serverUuid" id="serverUuid" value="${localServerUuid}" /></td>
					<td><spring:message code="sync.config.advanced.serverUuid.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.serverAdminEmail" /></b></td>
					<td><input type="text" size="50" name="serverAdminEmail" id="serverAdminEmail" value="${localServerAdminEmail}" /></td>
					<td><spring:message code="sync.config.advanced.serverAdminEmail.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.maxPageRecords" /></b></td>
					<td><input type="text" size="6" name="maxPageRecords" id="maxPageRecords" value="${maxPageRecords}" /></td>
					<td><spring:message code="sync.config.advanced.maxPageRecords.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.maxRecordsWeb" /></b></td>
					<td><input type="text" size="6" name="maxRecordsWeb" id="maxRecordsWeb" value="${maxRecordsWeb}" /></td>
					<td><spring:message code="sync.config.advanced.maxRecordsWeb.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.maxRecordsFile" /></b></td>
					<td><input type="text" size="6" name="maxRecordsFile" id="maxRecordsFile" value="${maxRecordsFile}" /></td>
					<td><spring:message code="sync.config.advanced.maxRecordsFile.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.maxRetryCount" /></b></td>
					<td><input type="text" size="6" name="maxRetryCount" id="maxRetryCount" value="${maxRetryCount}" /></td>
					<td><spring:message code="sync.config.advanced.maxRetryCount.info" /></td>
				</tr>
				<tr>
					<td></td>
					<td>
						<input type="submit" value="<spring:message code="general.save" />" />
						<input type="button" onClick="history.back();" value="<spring:message code="general.cancel" />" />					
					</td>
				</tr>
			</table>
		</div>		
	</form>
	
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
