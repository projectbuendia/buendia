<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="template/localHeader.jsp"%>
<style>
  .section { margin: 2em 0; }
  .profile-errors { margin: 2em; background: #ffdddd; }
  .selected { font-weight: bold; }
  .none { color: #999; }
  input[type="file"], select { width: 300px; }
  input, select { font-size: 16px; }
</style>

<h3>Manage Buendia profiles</h3>

<c:if test="${not empty errorLines}">
  <div class="section profile-errors">
    The uploaded file was not valid:
    <ul>
      <c:forEach var="line" items="${errorLines}">
        <li>${fn:escapeXml(line)}
      </c:forEach>
    </ul>
    <p>
    Please correct the problems and try uploading again below.
  </div>
</c:if>

<div class="section profile-current">
  Current profile:
  <c:choose>
    <c:when test="${empty currentProfile}">
      <span class="none">(none)</span>
    </c:when>
    <c:otherwise>
      <span class="selected">${fn:escapeXml(currentProfile)}</span>
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
        <select name="profile">
          <c:forEach var="file" items="${profiles}" varStatus="loop">
            <option value="${fn:escapeXml(file.name)}"
                    ${file.name == currentProfile ? 'selected' : ''}>
              ${fn:escapeXml(file.name)}
            </option>
          </c:forEach>
        </select>
        <input type="submit" value="Apply">
      </form>
    </c:otherwise>
  </c:choose>
</div>

<div class="section profile-upload">
  <form method="post" enctype="multipart/form-data">
    Add a profile:
    <input name="file" type="file">
    <input type="submit" value="Upload">
  </form>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>