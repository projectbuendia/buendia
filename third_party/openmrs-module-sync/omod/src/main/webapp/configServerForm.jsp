<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/configServer.form" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />
<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />

<%@ include file="localHeader.jsp" %>

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
	
    function enableInput(id) {
        var el = document.getElementById(id);
        if ( el ) {
            el.disabled=false;
        }
    }
    function disableInput(id) {
        var el = document.getElementById(id);
        if ( el ) {
            el.disabled=true;
        }
    }
		function getMessage(code) {
			<c:forEach items="${connectionState}" var="state" >
				if ( code == "${state.key}" ) return "${state.value}";
			</c:forEach>
		
			return code;
		}
	
		function showTestResult(result) {

			//alert("state is " + result.connectionState + ", errorMessage is " + result.errorMessage + ", payload is " + result.responsePayload);
		
			var img = '<img src="${pageContext.request.contextPath}/images/error.gif" border="0" style="margin-bottom: -3px;">';
			if ( result.connectionState == "OK" ) {
				img = '<img src="${pageContext.request.contextPath}/moduleResources/sync/accept.gif" border="0" style="margin-bottom: -3px;">';
				img += '&nbsp; The parent server uuid is: ' + result.syncTargetUuid;
			}
			
			var display = getMessage(result.connectionState) + "&nbsp;" + img;
			display += 
			DWRUtil.setValue("testInfo", display, { escapeHtml:false });
			document.getElementById("testConnectionButton").disabled = false;
		}
	
		function testConnection() {
			document.getElementById("testConnectionButton").disabled = true;
			DWRUtil.setValue("testInfo", '<spring:message code="sync.config.server.connection.testing" />'+'<img src="${pageContext.request.contextPath}/moduleResources/sync/connectionTest.gif" border="0" style="margin-bottom: -3px;">', { escapeHtml:false });
			var address = DWRUtil.getValue("address");
			var username = DWRUtil.getValue("username");
			var password = DWRUtil.getValue("password");
			DWRSyncService.testConnection(address, username, password, showTestResult);
		}

		function showCloneResult(result) {

            var img = '<img src="${pageContext.request.contextPath}/images/error.gif" border="0" style="margin-bottom: -3px;">';
            var display = "";
            if ( result.connectionState == "OK" ) {
                img = '<img src="${pageContext.request.contextPath}/moduleResources/sync/accept.gif" border="0" style="margin-bottom: -3px;">';
            }
            else {
                display+=getMessage(result.connectionState)+" - " ;
            }
            if(result.errorMessage != null)
                display+="&nbsp;"+ "&nbsp;"+result.errorMessage;
            display+=img;
            DWRUtil.setValue("cloneInfo", display, {escapeHtml:false});
            if ( result.connectionState == "OK" )
                document.getElementById("cloneViaWebButton").disabled = false;
        }

        function cloneParent() {
            if (confirm("<spring:message code="sync.settings.server.clone.now.warning"/>")) {
	            document.getElementById("cloneViaWebButton").disabled = true;
	            DWRUtil.setValue("cloneInfo", "Cloning parent data ..."+'<img src="${pageContext.request.contextPath}/moduleResources/sync/connectionTest.gif" border="0" style="margin-bottom: -3px;">', { escapeHtml:false });
	            var address = DWRUtil.getValue("address");
	            var username = DWRUtil.getValue("username");
	            var password = DWRUtil.getValue("password");
	            DWRSyncService.cloneParentDB(address, username, password, showCloneResult);
            }
        }

        var cloneStatus='<strong><spring:message code="sync.sending.parent.clone" /></strong>';
        function submitCloneFile(){
            var file=DWRUtil.getValue("cloneFile");
            if(file==null || file==''){
                DWRUtil.setValue("cloneInfo","<span style='color:red;'><b>Please select a file to upload</b></span>", { escapeHtml:false });
                return false;
            }
            DWRUtil.setValue("cloneInfo",cloneStatus+'&nbsp;&nbsp;<img src="${pageContext.request.contextPath}/moduleResources/sync/connectionTest.gif" border="0" style="margin-bottom: -3px;">', { escapeHtml:false });
            return true;
        }
        function showUploadResponse(str,color){
            DWRUtil.setValue("cloneInfo","<span style='color:"+color+";'><b>"+str+"</b></span>", { escapeHtml:false });
            DWRUtil.setValue("cloneFile","");
        }

		function addNewClass(idAndName) {
			var userInputNode = document.getElementById(idAndName);
			var userInput = userInputNode.value;
			var newDiv = document.createElement("div");
			
			var newHiddenInput = document.createElement("input");
			newHiddenInput["type"] = "hidden";
			newHiddenInput["value"] = userInput;
			newHiddenInput["name"] = idAndName;
			newDiv.appendChild(newHiddenInput);
			
			newDiv.appendChild(document.createTextNode(userInput));
			
			var newDeleteImg = document.createElement("img");
			newDeleteImg.src = "${pageContext.request.contextPath}/images/delete.gif";
			newDeleteImg.onclick = function() {this.parentNode.parentNode.removeChild(this.parentNode);};
			newDiv.appendChild(newDeleteImg);

			document.getElementById(idAndName + "Div").appendChild(newDiv);
		}

	-->
</script>

<c:choose>
	<c:when test="${server.serverType == 'CHILD' && not empty server.serverId}">
		<h2><spring:message code="sync.config.child.edit.title"/></h2>
	</c:when>
	<c:when test="${server.serverType == 'PARENT' && not empty server.serverId}">
		<h2><spring:message code="sync.config.parent.edit.title"/></h2>
	</c:when>
	<c:when test="${param.type == 'PARENT' && empty server.serverId}">
		<h2><spring:message code="sync.config.parent.add.title"/></h2>
	</c:when>
	<c:otherwise>
		<h2><spring:message code="sync.config.child.add.title"/></h2>
	</c:otherwise>
</c:choose>

<spring:hasBindErrors name="server">
	<spring:message code="fix.error"/>
	<div class="error">
		<c:forEach items="${errors.allErrors}" var="error">
			<spring:message code="${error.code}" text="${error.code}"/><br/><!-- ${error} -->
		</c:forEach>
	</div>
	<br />
</spring:hasBindErrors>

<div id="general">
	<form method="post" action="configServer.form">
		<input type="hidden" name="type" value="${server.serverType}" />
		<c:if test="${not empty server.serverId}">
			<input type="hidden" name="serverId" value="${server.serverId}" />
		</c:if>
		
		<c:if test="${(server.serverType == 'PARENT' || param.type == 'PARENT')}">
			<spring:message code="sync.config.parent.help"/><br/><br/>
		</c:if>
			
		<b class="boxHeader"><spring:message code="sync.config.server.configure"/></b>
		<div class="box">
			
			<table>
				<tr>
					<td align="right" valign="top" nowrap>
						<b><spring:message code="sync.config.advanced.serverName" /></b>
					</td>
					<td align="left" valign="top">
						<input type="text" size="25" maxlength="250" id="nickname" name="nickname" value="${server.nickname}" />
						<br>
							<c:if test="${(server.serverType == 'CHILD' || param.type == 'CHILD')}">
								<i><span class="syncHint"><spring:message code="sync.config.server.nickname.hint.child" /></span></i>
							</c:if>
							<c:if test="${(server.serverType == 'PARENT' || param.type == 'PARENT')}">
								<i><span class="syncHint"><spring:message code="sync.config.server.nickname.hint.parent" /></span></i>
							</c:if>
					</td>
				</tr>
				<tr>
					<td align="right" valign="top" nowrap>
						<b><spring:message code="sync.config.server.uuid" /></b>
					</td>
					<td align="left" valign="top">
						<input type="text" size="48" maxlength="250" id="uuid" name="uuid" value="${server.uuid}" />
						<br>
						<c:if test="${(server.serverType == 'CHILD' || param.type == 'CHILD')}">
							<i><span class="syncHint"><spring:message code="sync.config.server.uuid.hint.child" /></span></i>
						</c:if>
						<c:if test="${(server.serverType == 'PARENT' || param.type == 'PARENT')}">
							<i><span class="syncHint"><spring:message code="sync.config.server.uuid.hint.parent" /></span></i>
						</c:if>
					</td>
				</tr>
				<c:if test="${(server.serverType == 'CHILD' || param.type == 'CHILD') && not empty server.serverId}">
					<!-- Editing a child server -->
					<input type="hidden" name="action" value="editChild"/>
					<tr>
						<td align="right" valign="top" nowrap>
							<b><spring:message code="sync.config.child.currentusername" /></b>
						</td>
						<td align="left" valign="top">${server.childUsername}</td>
					</tr>
				</c:if>
				<c:if test="${(server.serverType == 'CHILD' || param.type == 'CHILD') && empty server.serverId}">
					<!-- adding a new child server -->
					<input type="hidden" name="action" value="saveNewChild"/>
					<tr>
						<td colspan="2">&nbsp;</td>
					</tr>
					<tr>
						<td align="right" valign="top" nowrap>
							<b><spring:message code="sync.config.child.username" /></b>
						</td>
						<td align="left" valign="top">
							<input type="text" size="25" maxlength="250" id="username" name="username" value="" /> 
							(<spring:message code="general.optional" />)
						</td>
					</tr>
					<tr>
						<td align="right" valign="top" nowrap>
							<b><spring:message code="sync.config.child.password" /></b>
						</td>
						<td align="left" valign="top">
							<input type="password" size="25" maxlength="250" id="password" name="password" value="" /> 
							(<spring:message code="sync.config.required.ifUsernameProvided" />)
						</td>
					</tr>
					<tr>
						<td align="right" valign="top" nowrap>
							<b><spring:message code="sync.config.child.password.retype" /></b>
						</td>
						<td align="left" valign="top">
							<input type="password" size="25" maxlength="250" id="passwordRetype" name="passwordRetype" value="" />
							(<spring:message code="sync.config.required.ifUsernameProvided" />)
							<br>
							<i><span class="syncHint"><spring:message code="sync.config.server.option.login" /></span></i>
						</td>
					</tr>
					<tr><td colspan="2">&nbsp;</td></tr>
					<tr>
						<td align="right" valign="top" nowrap>
							<b><spring:message code="sync.config.server.adminEmail" /></b>
						</td>
						<td align="left" valign="top">
							<input type="checkbox" name="shouldEmail" value="true" checked style="margin-top: 0px; margin-bottom: 0px;" />
							&nbsp;<spring:message code="sync.config.server.adminEmail.address" />
							<input type="text" size="25" maxlength="250" id="adminEmail" name="adminEmail" value="${localServerAdminEmail}" />
							<br />
							<i><span class="syncHint"><spring:message code="sync.config.server.adminEmail.instructions" /></span></i>
						</td>
					</tr>
				</c:if>
				<c:if test="${server.serverType == 'CHILD' || param.type == 'CHILD'}">
					<tr><td colspan="2">&nbsp;</td></tr>
					<tr>
						<td align="right" valign="top"><b><spring:message code="sync.settings.server.clone.down.backup" /></b></td>
						<td align="left" valign="top"><input type="button"
							onClick="document.location='${pageContext.request.contextPath}/moduleServlet/sync/createChildServlet';"
							value="<spring:message code="sync.settings.server.clone.down" />" 
							<c:if test="${(server.serverType == 'CHILD' || param.type == 'CHILD') && empty server.serverId}">
								disabled="disabled"
							</c:if>
						/>
						<c:if test="${(server.serverType == 'CHILD' || param.type == 'CHILD') && empty server.serverId}">
						<br/>
						<i><b><span class="syncHint"><spring:message code="sync.settings.server.clone.down.backup.help3" /></span></b></i>
						</c:if>
						<br/>
						<i><span class="syncHint"><spring:message code="sync.settings.server.clone.down.backup.help" /></span></i>
						<br/>
						<i><span class="syncHint"><spring:message code="sync.settings.server.clone.down.backup.help2" /></span></i>
						</td>
					</tr>
					<tr><td colspan="2">&nbsp;</td></tr>
				</c:if>
				<c:if test="${not (server.serverType == 'CHILD' || param.type == 'CHILD')}">
					<!--  adding/editing a parent server -->
					<input type="hidden" name="action" value="saveParent"/>
					<tr>
						<td align="right" valign="top" nowrap>
							<b><spring:message code="sync.config.server.address" /></b>
						</td>
						<td align="left" valign="top">
							<input type="text" size="70" maxlength="250" id="address" name="address" value="${server.address}" />
							<br>
							<i><span class="syncHint"><spring:message code="sync.config.parent.address.hint" /></span></i>
						</td>
					</tr>
					<tr>
						<td align="right" valign="top" nowrap>
							<b><spring:message code="sync.config.parent.username" /></b>
						</td>
						<td align="left" valign="top">
							<input type="text" size="25" maxlength="250" id="username" name="username" value="${server.username}" />
						</td>
					</tr>
					<tr>
						<td align="right" valign="top" nowrap>
							<b><spring:message code="sync.config.parent.password" /></b>
						</td>
						<td align="left" valign="top">
							<input type="password" size="25" maxlength="250" id="password" name="password" value="${server.password}" />
							&nbsp;&nbsp;
							<input type="button" id="testConnectionButton" onClick="testConnection();" value="<spring:message code="sync.config.parent.test" />" />
							<span id="testInfo"></span>
							<br/>
							<i><span class="syncHint"><spring:message code="sync.config.parent.login.hint" /></span></i>
						</td>
					</tr>
					<tr>
						<td align="right" valign="top" nowrap>
							<b><spring:message code="sync.config.parent.scheduled" /></b>
						</td>
						<td align="left" valign="top">
							<table cellpadding="0" cellspacing="0" border="0">
								<tr>
									<td>
										<input style="margin-left: 0px;" type="checkbox" id="started" name="started" value="true" 
											<c:if test="${serverSchedule.started}">checked</c:if> onClick="showHideDiv('scheduleInfo');" />
									</td>
									<td>
										<div id="scheduleInfo" style="margin-bottom: 0px; <c:if test="${empty serverSchedule || serverSchedule.started == false}">display:none;</c:if>">
											&nbsp;
											<spring:message code="sync.config.parent.scheduled.every" />
											<input type="text" size="3" maxlength="3" id="repeatInterval" name="repeatInterval" value="${repeatInterval}" />
											<spring:message code="sync.config.parent.scheduled.minutes" />
										</div>
									</td>
								</tr>
								<tr>
									<td colspan="2">
										<i><span class="syncHint"><spring:message code="sync.config.parent.scheduled.info"/></span></i>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</c:if>
				<tr>
					<td align="right" valign="top" nowrap>
						<b><spring:message code="sync.config.advanced.objects"/></b>
					</td>
					<td>
						
						<div id="details">

							<i><spring:message code="sync.config.server.classes.description"/></i>
							
							<br/>
							
							<table id="syncChangesTable" cellpadding="4" cellspacing="0">
								<thead>
									<tr>
										<th align="center"><spring:message code="sync.config.server.notSend" /></th>
										<th align="center" style="border-left: 1px solid black"><spring:message code="sync.config.server.notReceive" /></th>
									</tr>
								</thead>
								<tbody>
									<c:if test="${fn:length(server.serverClasses) == 0}">
										<tr id="noClasses">
											<td colspan="2"><i><spring:message code="sync.config.classes.none" /></i></td>
										</tr>
									</c:if>
									<tr>
										<td valign="top">
											<div id="notSendToDiv">
												<c:forEach var="serverClass" items="${server.serverClasses}">
													<c:if test="${!serverClass.sendTo}">
														<div>
															<input type="hidden" name="notSendTo" value="${serverClass.syncClass.name}"/>
															${serverClass.syncClass.name}
															<img src="${pageContext.request.contextPath}/images/delete.gif"
															     onclick="this.parentNode.parentNode.removeChild(this.parentNode)"/> 
															
														</div>
													</c:if>
												</c:forEach>
											</div>
											
											<c:forEach var="openmrsObjectClass" items="${openmrsObjectClasses}">
												${openmrsObjectClass.name}
											</c:forEach>
											
											<br/>
											<spring:message code="sync.config.addNewClass"/>:
											<br/>
											<input type="text" id="notSendTo" />
											<input type="button" id="newNotSendToButton" onClick="addNewClass('notSendTo');" value="<spring:message code="general.add"/>" />
											
										</td>
										<td valign="top" style="border-left: 1px solid black">
											<div id="notReceiveFromDiv">
												<c:forEach var="serverClass" items="${server.serverClasses}">
													<c:if test="${!serverClass.receiveFrom}">
														<div>
															<input type="hidden" name="notReceiveFrom" value="${serverClass.syncClass.name}"/>
															${serverClass.syncClass.name}
															<img src="${pageContext.request.contextPath}/images/delete.gif"
															     onclick="this.parentNode.parentNode.removeChild(this.parentNode)"/>
														</div> 
													</c:if>
												</c:forEach>
											</div>
											
											<br/>
											<spring:message code="sync.config.addNewClass"/>:
											<br/>
											<input type="text" id="notReceiveFrom" />
											<input type="button" id="notReceiveFromButton" onClick="addNewClass('notReceiveFrom');" value="<spring:message code="general.add"/>" />
											
										</td>
									</tr>
								</tbody>
							</table>
							<br/>
						</div>
				
					</td>
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
<c:if test="${not (server.serverType == 'CHILD' || param.type == 'CHILD') && not empty server.serverId}">
<br/>
<b class="boxHeader"><spring:message code="sync.config.server.clone.parent"/></b>
<div class="box">
<table>
<tr>
			<td width="45%" valign="top">
			<div class="syncInfoBox"><spring:message
				code="sync.settings.server.clone.notes" /></div>
			</td>
<td>
<iframe id="uploadFrameID" name="uploadFrame" height="0" width="0"
				frameborder="0" scrolling="yes"><spring:message
				code="sync.sending.parent.clone" /></iframe>
			<form method="post" target="uploadFrame"
				enctype="multipart/form-data" onSubmit="return submitCloneFile();"
				action="${pageContext.request.contextPath}/moduleServlet/sync/createChildServlet">
			<div class="syncSmallBox">
			<div class="syncSmallTitle"><spring:message
				code="sync.settings.server.clone.restore" /></div>
			<table width="100%" border="0" align="center" cellpadding="5" cellspacing="0">
				<tr bgcolor="#DFEFF4">
					<td><input name="cloneRadioGroup" type="radio" value="radio"
						checked="true" id="cloneViaWebRadio"
						onchange="disableInput('cloneFile');disableInput('cloneViaFileButton');enableInput('cloneViaWebButton');" />
					<b><spring:message
						code="sync.settings.server.clone.parent.web" /></b></td>
					<td><input type="button" name="cloneViaWebButton"
						id="cloneViaWebButton" onClick="cloneParent();"
						value="<spring:message code="sync.settings.server.clone.now" />" />
					<span style="color: #555555;"> <spring:message
						code="sync.settings.server.clone.expl" /> </span></td>
				</tr>
				<tr bgcolor="#F4ECB9">
					<td><input name="cloneRadioGroup" type="radio" value="radio"
						id="cloneViaWebRadio"
						onchange="disableInput('cloneViaWebButton');enableInput('cloneFile');enableInput('cloneViaFileButton');" />
					<b><spring:message
						code="sync.settings.server.clone.parent.file" /></b></td>
					<td><input id="cloneFile" disabled="true" name="cloneFile"
						type="file" />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input
						type="submit" disabled="true" id="cloneViaFileButton"
						name="cloneViaFileButton"
						value="<spring:message
                                            code="sync.settings.server.clone.upload" />" /></td>
				</tr>
				<tr>
					<td>&nbsp;&nbsp;</td>
					<td align="left">
					<div id="cloneInfo">&nbsp;&nbsp;</div>
					</td>
				</tr>
			</table>
			</div>
			</form>
			</td>
		</tr>
	</table>	
	</div>
	</c:if>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
