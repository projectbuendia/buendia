<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="template/localHeader.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page trimDirectiveWhitespaces="true" %>
<style>
  .section {
    margin: 2em 0;
  }
  div.message {
    padding: 1em;
    background: #ffe;
    border: 1px solid #aa5;
  }
  div.error.message {
    background: #fee;
    border: 1px solid #a55;
  }
  .output {
    background: #ccc;
    padding: 1em;
  }
  .selected {
    font-weight: bold;
  }
  .profile-select {
    white-space: nowrap;
  }
  select {
    vertical-align: top;
    width: 50em;
    margin-left: 8px;
  }
  input {
    font-size: 16px;
  }
</style>
<h3>Print Charts</h3>

<c:if test="${not authorized}">
  <c:set var="disabledIfUnauthorized" value="disabled"/>
  <div class="section error message">
    You don't have the necessary privileges to print patient charts.
    Please <a href="/openmrs/login.htm">log in</a> as a user authorized to
    <b>View Patients</b>.
  </div>
</c:if>



<div>
  <form method="post" action="printable.form">
    <label for="patient_id">Patient ID</label>
    <input type="text" name="patient_id"> (Leave blank for all)
    <input type="submit" value="Generate">
  </form>
</div>



<%@ include file="/WEB-INF/template/footer.jsp"%>