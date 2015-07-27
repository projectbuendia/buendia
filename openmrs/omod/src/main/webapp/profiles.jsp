<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="template/localHeader.jsp"%>
<style>
  .section { margin: 2em 0; }
  div.message { padding: 1em; background: #ffe; border: 1px solid #aa5; }
  div.error.message { background: #fee; border: 1px solid #a55; }
  .profile-errors { margin: 2em; background: #ffdddd; }
  .selected { font-weight: bold; }
  .none { color: #999; }
  .filename { font-family: monospace; font-size: 16px; }
  select { vertical-align: top; }
  input[type="file"], select { width: 300px; }
  input { font-size: 16px; }
</style>

<c:choose>
  <c:when test="${not empty failure}">
    <h3>
      Failed to
      <c:if test="${failure == 'add'}">add</c:if>
      <c:if test="${failure == 'apply'}">apply</c:if>
      profile
    </h3>

    <div class="section error message">
      The profile <span class="filename">${fn:escapeXml(filename)}</span>
      could not be
      <c:if test="${failure == 'add'}">added.</c:if>
      <c:if test="${failure == 'apply'}">applied.</c:if>
      <ul>
        <c:forEach var="line" items="${output}">
          <li>${fn:escapeXml(line)}
        </c:forEach>
      </ul>
    </div>
    <div class="section">
      Please correct the problems and <a href="profiles.form">try again</a>.
    </div>
  </c:when>

  <c:otherwise>
    <h3>Project Buendia profiles</h3>

    <c:if test="${not empty success}">
      <div class="section message">
        <c:if test="${success == 'add'}">Added profile</c:if>
        <c:if test="${success == 'apply'}">Applied profile</c:if>
        <c:if test="${success == 'delete'}">Deleted profile</c:if>
        <span class="filename">${fn:escapeXml(filename)}</span>.
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
                  ${fn:escapeXml(file.name)}
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
      <form method="post" enctype="multipart/form-data" id="upload-form">
        Add a profile:&nbsp;
        <span style="position:relative">
          <input type="submit" value="Upload a CSV file" style="position: absolute;">
          <input name="file" type="file" style="position: absolute; opacity: 0;"
                 onchange="document.getElementById('upload-form').submit()">
        </span>
      </form>
    </div>

  </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/template/footer.jsp"%>