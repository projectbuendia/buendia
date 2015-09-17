<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="template/localHeader.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<style>
  .section { margin: 2em 0; }
  div.message { padding: 1em; background: #ffe; border: 1px solid #aa5; }
  div.error.message { background: #fee; border: 1px solid #a55; }
  .output { background: #ccc; padding: 1em; }
  .selected { font-weight: bold; }
  .none { color: #999; }
  .filename { font-family: monospace; font-size: 16px; }
  select { vertical-align: top; width: 20em; }
  input { font-size: 16px; }
</style>

<h3>Project Buendia profiles</h3>

<c:if test="${not authorized}">
  <div class="section error message">
    You don't have the necessary privileges to manage profiles.
    Please log in as a user authorized to
    <b>Manage Concepts</b> and <b>Manage Forms</b>.
  </div>
</c:if>

<c:if test="${not empty param.failure}">
  <div class="section error message">
    The profile <span class="filename">${fn:escapeXml(param.filename)}</span>
    could not be
    <c:if test="${param.failure == 'add'}">added.</c:if>
    <c:if test="${param.failure == 'apply'}">applied.</c:if>
    <c:if test="${param.failure == 'delete'}">deleted.</c:if>
    <pre class="output">${fn:escapeXml(param.output)}</pre>
    Please correct the problems and try again.
  </div>
</c:if>

<c:if test="${not empty param.success}">
  <div class="section message">
    <c:if test="${param.success == 'add'}">Added profile</c:if>
    <c:if test="${param.success == 'apply'}">Applied profile</c:if>
    <c:if test="${param.success == 'delete'}">Deleted profile</c:if>
    <span class="filename">${fn:escapeXml(param.filename)}</span>.
  </div>
</c:if>

<div class="section profile-current">
  Current profile:
  <c:choose>
    <c:when test="${empty currentProfile}">
      <span class="none">(none)</span>
    </c:when>
    <c:otherwise>
      <span class="selected filename">${fn:escapeXml(currentProfile)}</span>
    </c:otherwise>
  </c:choose>
</div>

<div class="section profile-select">
  <c:choose>
    <c:when test="${empty profiles}">
      No profiles are available.
    </c:when>
    <c:otherwise>
      <form method="post">
        Select a profile:
        <select name="profile" size="${fn:length(profiles) < 3 ? 3 : fn:length(profiles)}">
          <c:forEach var="file" items="${profiles}" varStatus="loop">
            <option value="${fn:escapeXml(file.name)}" class="filename"
                    ${file.name == currentProfile ? 'selected' : ''}>
              <fmt:formatDate value="${file.modified}" pattern="YYYY-MM-dd HH:mm"/>   <fmt:formatNumber value="${file.size}" pattern="########0"/>   ${fn:escapeXml(file.name)}
            </option>
          </c:forEach>
        </select>
        <input type="submit" name="op" value="Apply">
        <input type="submit" name="op" value="Download">
        <input type="submit" name="op" value="Delete">
      </form>
    </c:otherwise>
  </c:choose>
</div>

<div class="section profile-upload">
  <form method="post" enctype="multipart/form-data" id="upload">
    Add a profile:&nbsp;
    <span style="position:relative">
      <input type="submit" value="Upload a CSV file" style="position: absolute;">
      <input name="file" type="file" style="position: absolute; opacity: 0;"
             onchange="document.getElementById('upload').submit()">
    </span>
  </form>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>