<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/statistics.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />
<openmrs:htmlInclude file="/dwr/util.js" />

<%@ include file="localHeader.jsp" %>
<h2><spring:message code="sync.statistics.title"/></h2>
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
			
	-->
</script>
<spring:hasBindErrors name="syncStat">
	<spring:message code="fix.error"/>
	<br />
</spring:hasBindErrors>
<b class="boxHeader"><spring:message code="sync.statistics.title"/></b>
<div class="box">
  <table width="100%" border="0" cellpadding="0" cellspacing="0">
    <tr>
      <td width="40%">
	   <div id="formDiv">
	     <form action="" method="post" name="syncStatForm" id="syncStatForm">
	       <table width="100%" border="0" cellspacing="0" cellpadding="0">
             <tr>
               <td height="40" colspan="3" align="center"><span style="font-weight: bold"><spring:message code="sync.statistics.form.title"/></span> </td>
              </tr>
             <tr>
               <td width="40%" align="right"><spring:message code="sync.statistics.form.format"/></td>
               <td width="10%" align="left">&nbsp;</td>
               <td width="60%" height="40" align="left">
			   <spring:bind path="syncStat.datePattern">
					<input type="text" id="datePattern" name="datePattern" size="22" value="${status.value}" disabled/>
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>			   </td>
             </tr>
             <tr>
               <td width="40%" align="right"><spring:message code="sync.statistics.form.from"/></td>
               <td width="10%" align="left">&nbsp;</td>
               <td width="60%" height="40" align="left">
			   <spring:bind path="syncStat.fromDate">
					<input type="text" id="fromDate" name="fromDate" size="22" value="${status.value}"/> 
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>			   </td>
             </tr>
             <tr>
               <td width="40%" align="right"><spring:message code="sync.statistics.form.until"/></td>
               <td width="10%" align="left">&nbsp;</td>
               <td width="60%" height="40" align="left">
			   <spring:bind path="syncStat.toDate">
					<input type="text" id="toDate" name="toDate" size="22" value="${status.value}"/> 
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>			   </td>
             </tr>
             <tr>
               <td width="40%" align="right">&nbsp;</td>
               <td width="10%" align="left">&nbsp;</td>
               <td width="60%" height="40" align="left"><input name="viewBtn" type="submit" id="viewBtn" value="<spring:message code="general.view"/>" /></td>
             </tr>
           </table>
	     </form>
	   </div>
	  </td>
      <td width="60%">
	  <div class="box" >
	    <table width="100%" border="0" cellpadding="0" cellspacing="0">
          <tr>
            <td width="30%" height="30" align="right" valign="top" bgcolor="#EEF1F2"><span style="font-weight: bold">
              <spring:message code="sync.statistics.parent"/></span></td>
            <td width="10%" height="30" valign="top" bgcolor="#EEF1F2">&nbsp;</td>
            <td width="50%" height="30" valign="top" bgcolor="#EEF1F2"><span style="font-weight: bold"><a href="synchronizationConfigServer.form?serverId=${parent.serverId}">${parent.nickname}</a></span></td>
            <td width="10%" height="30" valign="top" bgcolor="#EEF1F2">&nbsp;</td>
          </tr>
          <tr>
            <td width="30%" height="30" align="right" valign="top" bgcolor="#EEF1F2"><span style="font-weight: bold">
              <spring:message code="sync.settings.servers.attempt"/>:</span></td>
            <td width="10%" height="30" valign="top" bgcolor="#EEF1F2">&nbsp;</td>
            <td width="50%" height="30" valign="top" bgcolor="#EEF1F2">${parent.lastSync}</td>
            <td width="10%" height="30" valign="top" bgcolor="#EEF1F2">&nbsp;</td>
          </tr>
        </table>
		
	    <table width="100%" border="0" align="center" cellpadding="0" cellspacing="0">
		<tbody>
		<tr>
            <td width="3%">&nbsp;</td>
            <td width="10%" height="19" align="right">&nbsp;</td>
            <td width="2%" height="19">&nbsp;</td>
            <td width="85%" height="19">&nbsp;</td>
            <td width="5%">&nbsp;</td>
		</tr>
		<c:set var="bgs" value="EDEED2" />
		<c:if test="${synchronizedRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list?state=COMMITTED'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${synchronizedRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncCOMMITTED">
              <spring:message code="sync.statistics.COMMITTED"/></span></td>
            <td width="5%" align="left" >&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${newRecords!=0}">
          <tr class="syncTr" onclick="location='history.list?state=NEW'" bgcolor="#${bgs}">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${newRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncNEW"><spring:message code="sync.statistics.NEW"/>
                <c:if test="${parent!=null}"> <spring:message code="sync.statistics.WAITING"/></c:if>
            </span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${pendingRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list?state=PENDING_SEND'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${pendingRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncNEW">
              <spring:message code="sync.statistics.PENDING_SEND"/></span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${sentRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list?state=SENT'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${sentRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncNEW"><spring:message code="sync.statistics.SENT"/></span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${sendFailedRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list?state=SEND_FAILED'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${sendFailedRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncFAILED">
              <spring:message code="sync.statistics.SEND_FAILED"/></span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${ingestFailedRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list?state=FAILED'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${ingestFailedRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncFAILED"><spring:message code="sync.statistics.FAILED"/></span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${retriedRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list?state=SENT_AGAIN'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${retriedRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncNEW">
              <spring:message code="sync.statistics.SENT_AGAIN"/></span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${failedStoppedRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list?state=FAILED_AND_STOPPED'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${failedStoppedRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncFAILED"><spring:message code="sync.statistics.FAILED_AND_STOPPED"/></span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${notSyncRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list?state=NOT_SUPPOSED_TO_SYNC'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${notSyncRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncNEUTRAL">
              <spring:message code="sync.statistics.NOT_SUPPOSED_TO_SYNC"/></span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${rejectedRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list?state=REJECTED'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${rejectedRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncFAILED"><spring:message code="sync.statistics.REJECTED"/></span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
  		  <c:if test="${unknownstateRecords!=0}">
          <tr class="syncTr" bgcolor="#${bgs}">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right">${unknownstateRecords}</td>
            <td width="2%" height="25">&nbsp;</td>
            <td width="85%" height="25" align="left"><span class="syncNEW">
              <spring:message code="sync.statistics.UNKNOWN"/></span></td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
		  </c:if>
          <tr class="syncTr" bgcolor="#${bgs}" onclick="location='history.list'">
            <td width="3%">&nbsp;</td>
            <td width="10%" height="25" align="right"><span style="font-weight: bold; font-size: 18">${totalRecords}</span></td>
            <td width="2%" height="25"><span style="font-size: 18"></span></td>
            <td width="85%" height="25" align="left"><span style="font-weight: bold; font-family: &quot;Comic Sans MS&quot;; font-size: 18"><spring:message code="sync.statistics.TOTAL"/></span> </td>
            <td width="5%" align="left">&nbsp;</td>
			<c:choose>
				<c:when test="${bgs == 'EDEED2'}">
					<c:set var="bgs" value="F7F7EA" />
				</c:when><c:otherwise>
					<c:set var="bgs" value="EDEED2" />
				</c:otherwise>
			</c:choose>
          </tr>
  		  </tbody>
        </table>
	  </div>
	  </td>
    </tr>
  </table>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>