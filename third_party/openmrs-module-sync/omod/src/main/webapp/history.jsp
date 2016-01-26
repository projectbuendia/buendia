<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/history.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />

<script type="text/javascript">

	$j(document).ready( function() {
		$j('.syncViewErrorsDialog').each(function(index, ele){
			$j(ele).dialog({
				autoOpen: false,
				resizable: false,
				width:'700px',
				height:'500',
				modal: true
			});
		});
	});

	function reloadPage(firstRecordId) {
    	var dropdown = document.getElementById("itemsPerPage");
   		var index = dropdown.selectedIndex;
    	var ddVal = dropdown.options[index].value;
        if(firstRecordId==${latestRecordId})
        {
            document.location = "?size=" + ddVal;
        }else{
            document.location = "?firstRecordId=" + firstRecordId + "&size=" + ddVal;
        }

	}
 
	function getNewerItemsList(firstRecordId) {
		var firstRecordNum = parseInt(firstRecordId);
		var dropdown = document.getElementById("itemsPerPage");
    	var index = dropdown.selectedIndex;
    	var ddVal = dropdown.options[index].value;
    	var ddNum = parseInt(ddVal);
    	parseInt(ddNum);
    	firstRecordNum = firstRecordNum + ddNum;
        if(firstRecordNum>${latestRecordId}){
            document.location = "?size=" + ddNum + "&state=${param.state}";
        }else{
            document.location = "?firstRecordId=" + firstRecordNum + "&size=" + ddNum + "&state=${param.state}";
        }
	}
	
	function getOlderItemsList(firstRecordId) {
		var firstRecordNum = parseInt(firstRecordId);
		var dropdown = document.getElementById("itemsPerPage");
    	var index = dropdown.selectedIndex;
    	var ddVal = dropdown.options[index].value;
    	var ddNum = parseInt(ddVal);
    	parseInt(ddNum);
    	firstRecordNum = firstRecordNum - ddNum;
   		document.location = "?firstRecordId=" + firstRecordNum + "&size=" + ddNum + "&state=${param.state}";
	}
	
	function resetRecords(){
		resetRemoveRecords('NEW', 
				'<spring:message code="sync.history.resetErrorMessage"/>',
				'reset');
	}
	
	function removeRecords(){
		resetRemoveRecords('NOT_SUPPOSED_TO_SYNC', 
				'<spring:message code="sync.history.removeErrorMessage"/>',
				'remove');		
	}
	
	function resetRemoveRecords(invalidState, invalidStateMessage, action){
		var inputs = document.getElementsByTagName('input');
		var i = 0;
		var input;
		var syncRecordUuids = null;
		var serverRecordIds = null;
		
		while(input = inputs[i++]){
			if(input.type == 'checkbox' && input.checked){
				if(input.value == invalidState){
					alert(invalidStateMessage);
					return;
				}
				
				if(input.className != 'syncServerRecordCheckBox'){
					if(syncRecordUuids == null)
						syncRecordUuids = '';
					else
						syncRecordUuids += ' ';
					
					syncRecordUuids += input.id;
				}else{
					if(serverRecordIds == null)
						serverRecordIds = '';
					else
						serverRecordIds += ' ';
					
					serverRecordIds += input.id;
				}
			}
		}
		
		if(syncRecordUuids == null && serverRecordIds == null)
			alert('<spring:message code="sync.history.selectRecords"/>');
		else{
			var recordUuidsAndIds = '';
			if(syncRecordUuids != null)
				recordUuidsAndIds+=("&syncRecordUuids="+syncRecordUuids);
			if(serverRecordIds != null)
				recordUuidsAndIds+=("&serverRecordIds="+serverRecordIds);
			
			document.location = "historyResetRemoveRecords.list?recordId=" + ${firstRecordId} + "&size=" + ${size} + recordUuidsAndIds + "&action=" + action;
		}
	}
	
</script>

<h2><spring:message code="sync.history.title"/></h2>

<b class="boxHeader">
<table>
<tr>
<td><spring:message code="sync.changes.all"/>
</td>
<td>&nbsp;&nbsp;&nbsp;&nbsp;<spring:message code="sync.history.recordsPerPage"/>
<select id="itemsPerPage" name="itemsPerPage" onchange="reloadPage(${firstRecordId})">
	<option value="10" ${size == 10 ? 'selected' : ''}> 10</option>
	<option value="50" ${size == 50 ? 'selected' : ''}> 50</option>
	<option value="100" ${size == 100 ? 'selected' : ''}> 100</option>
	<option value="1000" ${size == 1000 ? 'selected' : ''}> 1000</option>
</select>
</td>
</tr>
</table>

</b>
<div class="box">
	<c:if test="${syncRecords[0].recordId != latestRecordId}">
	<a href="javascript: getNewerItemsList(${firstRecordId})">&larr; <spring:message code="sync.general.newer"/></a>
	</c:if>
	<c:if test="${firstRecordId != null}">
	<c:if test="${isEarliestRecord != true}">
	<a href="javascript: getOlderItemsList(${firstRecordId})"><spring:message code="sync.general.older"/> &rarr;</a>
	</c:if>
    &#124;
    <a href="historyRecentAllCommitted.list?recordId=${firstRecordId}&size=${size}"><spring:message code="sync.general.mostRecentAllCommitted"/></a>
	&#124;
	<a href="historyNextError.list?recordId=${firstRecordId}&size=${size}"><spring:message code="sync.general.nextError"/> &rarr;</a>
	</c:if>
	<table id="syncChangesTable" cellpadding="7" cellspacing="0">
		<thead>
			<tr>
				<th><spring:message code="sync.record.details.id" /></th>
				<th><spring:message code="sync.status.itemTypeAndUuid" /></th>
				<%--
				<th colspan="2" style="text-align: center;"><spring:message code="sync.status.timestamp" /></th>
				<th nowrap style="text-align: center;"><spring:message code="sync.status.itemState" /></th>
				<th nowrap style="text-align: center;"><spring:message code="sync.status.recordState" /></th>
				<th nowrap style="text-align: center;"><spring:message code="sync.status.retryCount" /></th>
				--%>
				<c:if test="${not empty servers}">
					<c:forEach items="${servers}" var="server">
						<th style="text-align: center; <c:if test="${server.serverType == 'PARENT'}">background-color: #eef;</c:if>">
							${server.nickname}
							<c:if test="${server.serverType == 'PARENT'}">(<spring:message code="${server.serverType}" />)</c:if>
						</th>
					</c:forEach>
				</c:if>
				<c:if test="${empty servers}">
					<th style="font-weight: normal;"><i><spring:message code="sync.status.servers.none" /></i></th>
				</c:if>
			</tr>
		</thead>
		<tbody id="globalPropsList">
			<c:if test="${not empty syncRecords}">
				<c:set var="bgStyle" value="eee" />
				<c:set var="bgStyleParent" value="dde" />
				<c:forEach var="syncRecord" items="${syncRecords}" varStatus="status">
					<%--<c:forEach var="syncItem" items="${syncRecord.items}" varStatus="itemStatus">--%>
						<tr>
                                                        <td>${syncRecord.recordId}</td>
							<td valign="middle" nowrap style="background-color: #${bgStyle};">
								<b><a href="viewrecord.form?uuid=${syncRecord.uuid}">${recordTypes[syncRecord.uuid]}</a></b>
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
							<c:if test="${not empty servers}">
								<c:forEach items="${servers}" var="server">
									<td valign="middle" nowrap style="background-color: #<c:if test="${server.serverType == 'PARENT'}">${bgStyleParent}</c:if><c:if test="${server.serverType != 'PARENT'}">${bgStyle}</c:if>;" align="center">
										<c:choose>
											<c:when test="${server.serverType == 'PARENT'}">
												<span class="sync${syncRecord.state}"><spring:message code="sync.record.state_${syncRecord.state}" /> </span>
												<br>
												<c:choose>
													<c:when test="${syncRecord.outgoing}">
														<span style="color: #bbb"><spring:message code="sync.record.direction.outgoing"/></span>
													</c:when>
													<c:otherwise>
														<spring:message code="sync.record.direction.incoming"/>
													</c:otherwise>
												</c:choose>
												<c:if test="${!syncRecord.state['final']}">
													<input type="checkbox" class="syncRecordCheckBox" id="${syncRecord.uuid}" value="${syncRecord.state}" />
												</c:if>
											</c:when>
											<c:otherwise>
												<c:if test="${not empty syncRecord.remoteRecords[server]}">
													<c:set var="syncError" value="${syncRecord.remoteRecords[server].outgoing && not empty syncRecord.remoteRecords[server].errorMessage && fn:trim(syncRecord.remoteRecords[server].errorMessage) != ''}"/>
													<span class="sync${syncRecord.remoteRecords[server].state}">

														<c:if test="${syncError}">
															<a style="color:red;" href="javascript:void(0)" onclick="javascript:$j('#${syncRecord.remoteRecords[server].serverRecordId}-errorLog-dialog').dialog('open')">
														</c:if>
														<spring:message code="sync.record.state_${syncRecord.remoteRecords[server].state}" />
														<c:if test="${syncError}">
															</a>
														</c:if>
													</span>

													 <div class="syncViewErrorsDialog" id="${syncRecord.remoteRecords[server].serverRecordId}-errorLog-dialog" title="<openmrs:message code="sync.record.viewErrors.title"/>">
													 	${syncRecord.remoteRecords[server].errorMessage}
													</div>

													<br>
													<c:choose>
														<c:when test="${syncRecord.remoteRecords[server].outgoing}">
															<span style="color: #bbb"><spring:message code="sync.record.direction.outgoing"/></span>
														</c:when>
														<c:otherwise>
															<spring:message code="sync.record.direction.incoming"/>
														</c:otherwise>
													</c:choose>
												</c:if>
												<c:if test="${empty syncRecord.remoteRecords[server]}">
													<span style="color: #bbb"><i><spring:message code="sync.record.server.didNotExist" /></i></span>
												</c:if>
												<c:if test="${not empty syncRecord.remoteRecords[server] && !syncRecord.remoteRecords[server].state['final']}">
													<input type="checkbox" class="syncServerRecordCheckBox" id="${syncRecord.remoteRecords[server].serverRecordId}" value="${syncRecord.remoteRecords[server].state}" />
												</c:if>
											</c:otherwise>
										</c:choose>
									</td>
								</c:forEach>
							</c:if>
							<c:if test="${empty servers}">
								<td></td>
							</c:if>
						</tr>
						<c:choose>
							<c:when test="${bgStyle == 'eee'}">
								<c:set var="bgStyle" value="fff" />
								<c:set var="bgStyleParent" value="eef" />
							</c:when>
							<c:otherwise>
								<c:set var="bgStyle" value="eee" />
								<c:set var="bgStyleParent" value="dde" />
							</c:otherwise>
						</c:choose>
					<%--</c:forEach>--%>
					
				</c:forEach>
			</c:if>
			<c:if test="${empty syncRecords}">
				<tr>
					<td colspan="2" align="left">
						<i><spring:message code="sync.history.noItems" /></i>
					</td>
				</tr>
			</c:if>
		</tbody>
	</table>
	
	<c:if test="${syncRecords[0].recordId != latestRecordId}">
	<a href="javascript: getNewerItemsList(${firstRecordId})">&larr; <spring:message code="sync.general.newer"/></a>
	</c:if>
	<c:if test="${firstRecordId != null}">
	<c:if test="${isEarliestRecord != true}">
	<a href="javascript: getOlderItemsList(${firstRecordId})"><spring:message code="sync.general.older"/> &rarr;</a>
	</c:if>
	&#124;
    <a href="historyRecentAllCommitted.list?recordId=${firstRecordId}&size=${size}"><spring:message code="sync.general.mostRecentAllCommitted"/></a>
    &#124;
	<a href="historyNextError.list?recordId=${firstRecordId}&size=${size}"><spring:message code="sync.general.nextError"/> &rarr;</a> |
	<b><spring:message code="sync.records.action"/>:</b>
	<b><a href="#" onclick="resetRecords();"><spring:message code="sync.record.details.reset" /></a></b> |
	<b><a href="#" onclick="removeRecords();"><spring:message code="sync.record.details.remove" /></a></b>
	</c:if>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>