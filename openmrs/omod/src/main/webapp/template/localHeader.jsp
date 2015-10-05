<spring:htmlEscape defaultHtmlEscape="true" />
<ul id="menu">
  <li class="first">
    <a href="${pageContext.request.contextPath}/admin">
      <spring:message code="admin.title.short"
    /></a>
  </li>

  <li class="${fn:contains(pageContext.request.requestURI, '/profiles') ? 'active' : ''}">
    <a href="${pageContext.request.contextPath}/module/projectbuendia/openmrs/profiles.form">
      <spring:message code="projectbuendia.openmrs.profiles"
    /></a>
  </li>

  <!-- Add further links here -->
</ul>