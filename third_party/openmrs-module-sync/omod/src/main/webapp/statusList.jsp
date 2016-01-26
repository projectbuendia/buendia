<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/status.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />
<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />

<h2><spring:message code="sync.status.title"/></h2>

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
	
		function doSubmitFileExport() {
			document.getElementById("fileExportSubmit").disabled = true;
			setTimeout("location.reload();", 5000);
			return true;
		}

		function doSubmitUploadResponse() {
			document.getElementById("uploadResponseSubmit").disabled = true;
			return true;
		}

		function getMessage(code) {
			<c:forEach items="${transmissionState}" var="state" >
				if ( code == "${state.key}" ) return '${state.value}';
			</c:forEach>
		
			return code;
		}

		/**
		 * Set the status on the row in the results table for this record
		 */
		function processRecord(record) {
			var state = "<span class='syncFAILED'><b><spring:message code="sync.record.state_FAILED" /></b></span>";
			if ( record.state == "COMMITTED" || record.state == "COMMITTED_AND_CONFIRMATION_SENT" ) state = "<span class='syncCOMMITTED'><b><spring:message code="sync.record.state_COMMITTED" /></b></span>";
			else if ( record.state !=  "FAILED" ) state = "<span class='syncNEUTRAL'><b>" + getMessage(record.state) + "</b></span>";

			// if the row doesn't exist, create it.  this means it is an incoming change from the parent 
			if (document.getElementById("state_" + record.uuid) == null) {
				var row = document.createElement("tr");
				var nameCell = document.createElement("td");
				nameCell.innerHTML = record.description;
				nameCell.innerHTML += "<div style='color: #bbb'>" + record.state + " - " + record.timestampDisplay + "</div>";
				row.appendChild(nameCell);
				var stateCell = document.createElement("td");
				stateCell.id = "state_" + record.uuid;
				stateCell.innerHTML = state;
				stateCell.className = "centeredColumn";
				row.appendChild(stateCell);
				var retryCell = document.createElement("td");
				retryCell.className = "centeredColumn";
				row.appendChild(retryCell);
				var messageCell = document.createElement("td");
				messageCell.id = "message_" + record.uuid;
				messageCell.className = "centeredColumn";
				if ( record.state != "COMMITTED" && record.state != "COMMITTED_AND_CONFIRMATION_SENT" ) {
					messageCell.innerHTML = "Error was: " + record.errorMessage;
				}
				row.appendChild(messageCell);

				// hide the "no items" row
				var noItemsRow = document.getElementById("noItemsRow");
				if (noItemsRow != null)
					noItemsRow.style.display = "none";

				// add in this new sync record row
				document.getElementById("resultList").appendChild(row);
			}
			else {
				// set the status on that row
				DWRUtil.setValue("state_" + record.uuid, state, { escapeHtml:false });
				if ( record.state != "COMMITTED" ) {
					DWRUtil.setValue("message_" + record.uuid, record.errorMessage);
				}
			}
		}
		
		function displaySyncResults(result) {
			//alert("uuid is " + result.uuid + ", state is " + result.transmissionState + ", em is " + result.errorMessage);
			var success = "<spring:message code="sync.status.transmission.ok.allItems" />";
			//success += " &nbsp;<a href=\"javascript://\" onclick=\"showHideDiv('syncDetails');\">details</a>";
			//var details = "<br>";
			//details += "<spring:message code="sync.status.transmission.details" />:";
			//details += "<br><br>";
			var records = result.syncImportRecords;
			if ( records && records.length > 0 ) {
				for ( var i = 0; i < records.length; i++ ) {
					var record = records[i];
					processRecord(record);
					//details += record.uuid + " - " + record.state + "<br>";
				}
			} else {
				//details += "<spring:message code="sync.status.transmission.details.noItems" />:";
			}

			if ( result.transmissionState == "OK" ) {
				DWRUtil.setValue("syncInfo", success);			
			} else {
				// just show error message
				DWRUtil.setValue("syncInfo", getMessage(result.transmissionState));
			}
			DWRUtil.setValue("syncReceivingSize", "");
			document.getElementById("webExportButton").disabled = false;
			
			// fix the odd/even rows
			toggleRowVisibilityForClass("syncChangesTable", "someNonexistentClass", false);
		}
		
		var getReceivedNumberAttempts = 0;
		
		function displayNumberOfObjectsBeingReceived(num) {
			// keep checking until we get a response
			getReceivedNumberAttempts += 1;
			if (getReceivedNumberAttempts < 60 && !num || num == "") {
				setTimeout("getNumberOfObjectsBeingReceived()", 500); // fire this off again in half a second
				return;
			}
			
			if (num == null || typeof(num)=="undefined")
				num = 0;
			
			DWRUtil.setValue("syncReceivingSize", "<spring:message code="sync.status.export.viaWeb.receiving" />".replace("{0}", num), { escapeHtml: false} );
		}

		function getNumberOfObjectsBeingReceived() {
			DWRSyncService.getNumberOfObjectsBeingReceived(displayNumberOfObjectsBeingReceived);
		}

		function syncToParent() {
			document.getElementById("webExportButton").disabled = true;
			DWRUtil.setValue("syncInfo", "<spring:message code="sync.status.export.viaWeb.sending" arguments="${fn:length(statusCommandObject)}" />", { escapeHtml:false });
			DWRSyncService.syncToParent(displaySyncResults);
			getNumberOfObjectsBeingReceived();
		}
		
	-->
</script>

<style>
  #syncChangesTable td.centeredColumn {
    white-space: nowrap;
    text-align: center;
    vertical-align: middle;
  }
</style>

<b class="boxHeader"><spring:message code="sync.status.export.changes"/></b>
<div class="box">
	<table cellpadding="4">
		<c:choose>
			<c:when test="${mode == 'SEND_WEB'}">
				<tr>
					<td colspan="4">
						<img src="${pageContext.request.contextPath}/images/lookup.gif" border="0" style="margin-bottom: -3px;">
						<spring:message code="sync.status.export.viaWeb" />
						<c:if test="${parent.syncInProgress}">
							<br/><span class="syncStatsAttention"><spring:message code="sync.config.warning.syncInProgress" arguments="${parent.syncInProgressMinutes}"/></span>
						</c:if>
					</td>
				</tr>
				<tr>
					<td>
						&nbsp;&nbsp;
					</td>
					<td valign="top">
						<form method="post">
							<input type="button" onClick="syncToParent();" id="webExportButton" value='<spring:message code="sync.status.createWebTx"/>'
							<c:if test="${empty parent}">disabled</c:if> />
							<input type="hidden" name="action" value="createWebTx"/>
						</form>
					</td>
					<td></td>
					<td valign="top">
						<c:if test="${empty parent}">
							<span class="error"><i><spring:message code="sync.status.export.viaWeb.enableViaParentConfig" /></i></span>
						</c:if>
						<span id="syncInfo"></span>
						<br>
						<span id="syncReceivingSize"></span>
					</td>
				</tr>
			</c:when>
			<c:otherwise>
				<tr>
					<td colspan="4">
						<img src="${pageContext.request.contextPath}/images/save.gif" border="0" style="margin-bottom: -3px;">
						<spring:message code="sync.status.export.viaFile" />
					</td>
				</tr>
				<tr>
					<td>
						&nbsp;&nbsp;
					</td>
					<td valign="top">
						<form method="post" onSubmit="return doSubmitFileExport();">
							<input type="submit" id="fileExportSubmit" value='<spring:message code="sync.status.createTx"/>' />
							<input type="hidden" name="action" value="createTx"/>
						</form>
					</td>
					<td valign="top">
						|
					</td>
					<td valign="top">
						<form method="post" enctype="multipart/form-data" onSubmit="return doSubmitUploadResponse();">
							<spring:message code="sync.status.responsePrompt" />
							<input type="file" name="syncResponseFile" value="" />
							<input type="hidden" name="action" value="uploadResponse" />
							<input type="submit" id="uploadResponseSubmit" value="<spring:message code="sync.status.uploadResponse" />" id="submitButton" />
							<br>
							<span style="color: #bbbbbb; position: relative; top: 3px;"><i><spring:message code="sync.status.export.viaDisk.instructions" /></i></span>
						</form>
					</td>
				</tr>
			</c:otherwise>
		</c:choose>
	</table>
</div>

<br/>

<b class="boxHeader"><spring:message code="sync.changes.recent"/></b>
<div class="box">
	<form method="post">
		<input type="hidden" name="action" value="resetAttempts" />
		<input type="hidden" name="mode" value="${mode}" />
		<input type="submit" value="<spring:message code="sync.status.transmission.reset.attempts"/>" />
	</form>
	<table id="syncChangesTable" cellpadding="4" cellspacing="0">
		<thead>
			<tr>
				<th><spring:message code="sync.status.itemTypeAndUuid" /></th>
				<th nowrap style="text-align: center;"><spring:message code="sync.status.recordState" /></th>
				<th nowrap style="text-align: center;"><spring:message code="sync.status.retryCount" /></th>
				<th></th>
			</tr>
		</thead>
		<tbody id="resultList">
			<c:if test="${not empty statusCommandObject}">
				<c:forEach var="syncRecord" items="${statusCommandObject}" varStatus="status">
						<tr>
							<td valign="middle" nowrap>
								<b>${recordTypes[syncRecord.uuid]}</b>
								<c:if test="${not empty recordText[syncRecord.uuid]}">
									(${recordText[syncRecord.uuid]})
								</c:if>
								<br>
								<span style="color: #bbb">
									<spring:message code="sync.item.state_${recordChangeType[syncRecord.uuid]}" /> -
									<openmrs:formatDate date="${syncRecord.timestamp}" format="${syncDateDisplayFormat}" />	
									<%--<c:if test="${not empty itemInfo[syncItem.key.keyValue]}">(${itemInfo[syncItem.key.keyValue]})</c:if></b>--%>
								</span>
							</td>
							<td id="state_${syncRecord.uuid}" class="centeredColumn">
								<span class="sync${syncRecord.state}"><spring:message code="sync.record.state_${syncRecord.state}" /></span>
							</td>
							<td class="centeredColumn">${syncRecord.retryCount}</td>
							<td class="centeredColumn"><span id="message_${syncRecord.uuid}"></span></td>

							<%--
							<td valign="middle" nowrap>
								<b>${itemTypes[syncItem.key.keyValue]}</b>
								<br>
								(${itemUuids[syncItem.key.keyValue]})
							</td>
							--%>
						</tr>
				</c:forEach>
			</c:if>
			<c:if test="${empty statusCommandObject}">
				<tr id="noItemsRow">
					<td colspan="5" align="left">
						<i><spring:message code="sync.status.noItems" /></i>
					</td>
				</tr>
			</c:if>
		</tbody>
	</table>
</div>

<script type="text/javascript">
	// make the rows alternating gray/white
	toggleRowVisibilityForClass("syncChangesTable", "someNonexistentClass", false);
</script>


<%@ include file="/WEB-INF/template/footer.jsp" %>
