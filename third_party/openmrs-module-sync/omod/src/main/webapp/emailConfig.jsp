<%@ include file="/WEB-INF/template/include.jsp" %>
<openmrs:require privilege="Manage Global Properties" otherwise="/login.htm" redirect="/module/sync/emailConfig.list" />
<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp"%>

<script type="text/javascript" charset="utf-8">
	function testConnection() {
		$j("#validateButton").hide();
		$j("#emailConfigStatus").html('Checking...');
		var url = '${pageContext.request.contextPath}/module/sync/validateEmailSettings.form';
		var args = $j("#settingsForm").serialize();
		$j.getJSON(url, args, function(data) {
			$j("#emailConfigStatus").html(data);
			$j("#validateButton").show();
		});
	}

	$j(document).ready(function() {
		testConnection();

		$j("#validateButton").click(function(event) {
			testConnection();
		});

		$j("#testSendButton").click(function(event) {
			$j("#testSendButton").attr("disabled","true");
			$j("#testSendResults").html('Sending...');
			var url = '${pageContext.request.contextPath}/module/sync/sendTestEmail.form';
			var args = $j("#testSendForm").serialize();
			$j.getJSON(url, args, function(data) {
				$j("#testSendResults").html(data);
				$j("#testSendButton").removeAttr("disabled");
			});
		});
	} );
</script>

<h2><spring:message code="sync.emailConfig.title"/></h2>

<table width="100%" cellpadding="5"><tr>
	<td width="50%" valign="top">
		<b class="boxHeader"><spring:message code="sync.emailConfig.currentSettings"/></b>
		<div class="box">
			<form id="settingsForm" method="post" action="saveEmailConfiguration.form">
				<table>
					<c:forEach items="${ settings }" var="setting" varStatus="status">
						<tr>
							<td style="white-space:nowrap; padding: 0px 10px 0px 10px;">${ setting.key }</td>
							<td><input type="text" name="${setting.key}" value="${ setting.value }" size="50" /></td>
						</tr>
					</c:forEach>
				</table>
				<input type="submit" value="<spring:message code="sync.emailConfig.saveSettings"/>"/>
			</form>
		</div>
	</td>
	<td width="50%" valign="top">
		<b class="boxHeader"><spring:message code="sync.emailConfig.testSettings"/></b>
		<div class="box">
			<span id="emailConfigStatus"></span>
			<button id="validateButton"><spring:message code="sync.emailConfig.validate"/></button>
		</div>
		<br/>
		<b class="boxHeader"><spring:message code="sync.emailConfig.testSend"/></b>
		<div class="box">
			<form method="post" id="testSendForm" action="sendTestEmail.form">
				<table>
					<tr>
						<td valign="top">Recipients:</td>
						<td><input name="recipients" type="text" size="30" value="${settings['sync.admin_email']}"/></td>
					</tr>
					<tr>
						<td valign="top">Subject:</td>
						<td><input name="subject" type="text" size="30" value="Test Sync Email"/></td>
					</tr>
					<tr>
						<td valign="top">Description:</td>
						<td>
							<textarea name="emailBody" rows="5" cols="40">Testing the email configuration of sync</textarea>
						</td>
					</tr>
				</table>
			</form>
			<button id="testSendButton"><spring:message code="sync.emailConfig.send"/></button>
			<br/><br/>
			<div id="testSendResults"></div>
		</div>
	</td>
</tr></table>

<br />

<%@ include file="/WEB-INF/template/footer.jsp" %>