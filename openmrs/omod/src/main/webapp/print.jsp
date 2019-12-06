<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="template/localHeader.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page trimDirectiveWhitespaces="true" %>

<h1>Print</h1>

<form method="POST">

<table><tr valign="baseline">
<td width="50%">

<h2>Recent Admissions</h2>
<ul>
<c:forEach var="obs" items="${admissions}">
  <li><input type=checkbox name="patient" id="${obs.patient.uuid}" value="${obs.patient.uuid}"/>
  <fmt:formatDate value="${obs.valueDatetime}" timeZone="${zone}" pattern="MMM dd, HH'h'mm" var="time"/>
  <label for="${obs.patient.uuid}">
    ${time}: ${fn:escapeXml(obs.patient.personName)}
  </label>
</c:forEach>
</ul>

<h2>Recent Discharges</h2>
<ul>
<c:forEach var="obs" items="${discharges}">
  <li><input type=checkbox name="patient" id="${obs.patient.uuid}" value="${obs.patient.uuid}"/>
  <label for="${obs.patient.uuid}">
    <fmt:formatDate value="${obs.encounterDatetime}" timeZone="${zone}"  pattern="MMM dd, HH'h'mm" var="time"/>
    <fmt:formatDate value="${obs.obs.encounter.encounterDatetime}" timeZone="${zone}"  pattern="MMM dd, HH'h'mm" var="time"/>
    ${time}: ${fn:escapeXml(obs.patient.personName)}
  </label>
</c:forEach>
</ul>

</td>
<td width="50%">

<h2>Current Inpatients</h2>

<ul>
<c:forEach var="pp" items="${inpatients}">
  <li><input type=checkbox name="patient" id="${pp.patient.uuid}" value="${pp.patient.uuid}"/>
  <label for="${pp.patient.uuid}">
    ${fn:escapeXml(pp.locationName)}
    ${pp.bed} -
    ${fn:escapeXml(pp.patient.personName)}
  </label>
</c:forEach>
</ul>

</td>
</table>

<input type="submit" value="Print selected patients">

</form>
