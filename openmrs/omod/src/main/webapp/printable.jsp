<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page trimDirectiveWhitespaces="true" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>Patient Charts</title>
  <style>
  </style>
</head>
<body>
  <c:forEach var="patient" items="${patients}">
    <div>${patient.patientIdentifier}</div>
    <table cellpadding="0" cellspacing="0" border="1">
    	<thead>
    		<th>&nbsp;</th>
    		<c:forEach var="day" items="${patient.days}">
    		<th>${day.desc}<br/>${day.date}</th>
    		</c:forEach>
    	</thead>
    	<tbody>
          <c:forEach var="concept" items="${concepts}">
          <tr>
            <td>
                ${concept.name}
			</td>
          </th>
          </c:forEach>
          <!--tr>
              <td>teste 1</td><td>teste 1</td><td>teste 1</td><td>teste 1</td><td>teste 1</td><td>teste 1</td><td>teste 1</td><td>teste 1</td>
          </tr-->
    	</tbody>
    </table>
  </c:forEach>
</body>
</html>