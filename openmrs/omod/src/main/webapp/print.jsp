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
  <li><input type=checkbox onchange="update(this)" name="patient" value="${obs.patient.uuid}" id="adm-${obs.patient.uuid}"/>
  <label for="adm-${obs.patient.uuid}">
    ${obs.formattedValueTime}: ${fn:escapeXml(obs.patient.personName)}
  </label>
</c:forEach>
</ul>

<h2>Recent Discharges</h2>
<ul>
<c:forEach var="obs" items="${discharges}">
  <li><input type=checkbox onchange="update(this)" name="patient" value="${obs.patient.uuid}" id="dis-${obs.patient.uuid}"/>
  <label for="dis-${obs.patient.uuid}">
    ${obs.formattedObsTime}: ${fn:escapeXml(obs.patient.personName)}
  </label>
</c:forEach>
</ul>

</td>
<td width="50%">

<h2>Current Inpatients</h2>

<ul>
<c:forEach var="pp" items="${inpatients}">
  <li><input type=checkbox onchange="update(this)" name="patient" value="${pp.patient.uuid}" id="inp-${pp.patient.uuid}"/>
  <label for="inp-${pp.patient.uuid}">
    ${fn:escapeXml(pp.placement.locationName)}&nbsp;
    ${fn:escapeXml(pp.placement.bed)} -
    ${fn:escapeXml(pp.patient.personName)}
  </label>
</c:forEach>
</ul>

</td>
</table>

<input type="submit" value="Print selected patients">

</form>

<script>
  var checkboxes = document.getElementsByTagName("input");

  function update(checkbox) {
    for (var i = 0; i < checkboxes.length; i++) {
      if (checkboxes[i].value == checkbox.value) {
        checkboxes[i].checked = checkbox.checked;
      }
    }
  }
</script>
