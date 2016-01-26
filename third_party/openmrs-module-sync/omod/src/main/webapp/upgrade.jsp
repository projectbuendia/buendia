<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="Manage Synchronization" otherwise="/login.htm" redirect="/module/sync/upgrade.form" />
<spring:message var="pageTitle" code="sync.upgrade.titlebar" scope="page"/>

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<h2><spring:message code="sync.upgrade.title"/></h2>

<spring:message code="sync.upgrade.help" />
<br/><br/>
<b class="boxHeader"><spring:message code="sync.upgrade.choose"/></b>
<form class="box" method="post">
    <spring:message code="sync.upgrade.chooseFrom"/>: 
    <select name="fromVersion">
		<c:forEach var="fromOption" items="${fromOptions}">
			<option>${fromOption}</option>
		</c:forEach>
    </select>
    
	<input type="submit" value='<spring:message code="general.download"/>'/>
</form>

<%@ include file="/WEB-INF/template/footer.jsp" %>
