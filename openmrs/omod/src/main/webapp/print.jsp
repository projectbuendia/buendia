<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="template/localHeader.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page trimDirectiveWhitespaces="true" %>

<style>
input[type="checkbox"] {
  width: 1rem;
  height: 1rem;
}

input[type="submit"], button {
  padding: 0.5rem;
  font-size: 1rem;
  margin-right: 1rem;
}

#content {
  margin-left: 2rem;
}

#content form td {
  padding-right: 2rem;
}

#content form ul {
  padding-left: 1rem;
}

#content form li {
  list-style: none;
  transition: none;
  padding: 0.2rem;
  border-radius: 0.5rem;
  border: 1px solid transparent;
  white-space: nowrap;
}

#content form li:hover {
  border: 1px solid #cde;
  background: #def;
}

#content form li, label {
  cursor: pointer;
}

h2 {
  margin-top: 2rem;
}

.navigation {
  margin-top: 1rem;
}

.navigation a {
  padding: 0.3rem 0.6rem;
  border-radius: 0.5rem;
  border: 1px solid transparent;
}

.navigation a.here {
  font-weight: bold;
  color: #000;
  text-decoration: none;
  background: #def;
  border: 1px solid #cde;
}

.ident {
  padding: 0 0.1rem;
  color: #099;
}

.ident::after {
  content: ".";
}

</style>

<h1>Print</h1>

<div class="body">

<div class="navigation">
  <a href="print.form" class="${!showAll ? 'here' : 'elsewhere'}"
    >Recent patients</a>
  <a href="print.form?all=1" class="${showAll ? 'here' : 'elsewhere'}">
    All patients</a>
</div>

<form method="POST">

<c:choose>
<c:when test="${showAll}">

<h2>All Patient Records</h2>
<ul>
<c:forEach var="obs" items="${allAdmissions}">
  <label for="adm-${obs.patient.uuid}">
    <li><input type=checkbox onchange="update(this)" name="patient" value="${obs.patient.uuid}" id="adm-${obs.patient.uuid}"/>
    Admitted ${obs.formattedValueTime} &#x2014;
    <span class="ident">${fn:escapeXml(obs.patientId)}</span>
    ${fn:escapeXml(obs.patient.personName)}</li>
  </label>
</c:forEach>
</ul>

<button type="button" onclick="selectAll()">Select all patients</button>

</c:when>
<c:otherwise>

<table><tr valign="baseline">
<td width="50%">

<h2>Recent Admissions</h2>
<ul>
<c:forEach var="obs" items="${admissions}">
  <label for="adm-${obs.patient.uuid}">
    <li><input type=checkbox onchange="update(this)" name="patient" value="${obs.patient.uuid}" id="adm-${obs.patient.uuid}"/>
    ${obs.formattedValueTime} &#x2014;
    <span class="ident">${fn:escapeXml(obs.patientId)}</span>
    ${fn:escapeXml(obs.patient.personName)}</li>
  </label>
</c:forEach>
</ul>

<h2>Recent Discharges</h2>
<ul>
<c:forEach var="obs" items="${discharges}">
  <label for="dis-${obs.patient.uuid}">
    <li><input type=checkbox onchange="update(this)" name="patient" value="${obs.patient.uuid}" id="dis-${obs.patient.uuid}"/>
    ${obs.formattedObsTime} &#x2014;
    <span class="ident">${fn:escapeXml(obs.patientId)}</span>
    ${fn:escapeXml(obs.patient.personName)}</li>
  </label>
</c:forEach>
</ul>

</td>
<td width="50%">

<h2>Current Inpatients</h2>

<ul>
<c:forEach var="pp" items="${inpatients}">
  <label for="inp-${pp.patient.uuid}">
    <li><input type=checkbox onchange="update(this)" name="patient" value="${pp.patient.uuid}" id="inp-${pp.patient.uuid}"/>
    ${fn:escapeXml(pp.placement.description)} &#x2014;
    <span class="ident">${fn:escapeXml(pp.patientId)}</span>
    ${fn:escapeXml(pp.patient.personName)}</li>
  </label>
</c:forEach>
</ul>

<button type="button" onclick="selectAllInpatients()">Select all inpatients</button>

</td>
</table>

</c:otherwise>
</c:choose>

<button type="button" onclick="clearSelection()">Clear selection</button>

<input type="submit" value="Print selected patients">

</form>

</div>

<script>
  var checkboxes = document.getElementsByTagName("input");

  function update(checkbox) {
    for (var i = 0; i < checkboxes.length; i++) {
      if (checkboxes[i].value == checkbox.value) {
        checkboxes[i].checked = checkbox.checked;
      }
    }
  }

  function clearSelection() {
    for (var i = 0; i < checkboxes.length; i++) {
      checkboxes[i].checked = false;
    }
  }

  function selectAll() {
    for (var i = 0; i < checkboxes.length; i++) {
      checkboxes[i].checked = true;
    }
  }

  function selectAllInpatients() {
    for (var i = 0; i < checkboxes.length; i++) {
      if (checkboxes[i].id.startsWith('inp-')) {
        checkboxes[i].checked = true;
        update(checkboxes[i]);
      }
    }
  }
</script>
