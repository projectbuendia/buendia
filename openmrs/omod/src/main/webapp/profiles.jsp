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
    .none {
        color: #999;
    }
    .filename {
        font-family: monospace;
        font-size: 16px;
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
    #upload > span  {
        position: relative;
    }
    #upload > span > input[type="file"] {
        position: absolute;
        opacity: 0;
    }
    #upload > span > input[type="submit"] {
        position: absolute;
        margin-left: 30px;
    }
    div.section.profile-current > span {
        margin-left: 10px;
    }
</style>

<h3>Project Buendia profiles</h3>

<c:if test="${not authorized}">
    <c:set var="disabledIfUnauthorized" value="disabled"/>
    <div class="section error message">
        You don't have the necessary privileges to manage profiles.
        Please <a href="/openmrs/login.htm">log in</a> as a user authorized to
        <b>Manage Concepts</b> and <b>Manage Forms</b>.
    </div>
</c:if>

<c:if test="${not empty message}">
    <div class="section <c:if test="${success == 'false'}">error</c:if> message">
        ${message}
        <c:if test="${not empty output}">
            <pre class="output">${fn:escapeXml(output)}</pre>
        </c:if>
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

<div class="section profile-upload">
    <form method="post" enctype="multipart/form-data" id="upload">
        Add a profile:
        <span>
            <input type="submit" value="Upload a CSV file">
            <input name="file" type="file">
        </span>
    </form>
</div>

<div class="section profile-select">
    <c:choose>
        <c:when test="${empty profiles}">
            No profiles are available.
        </c:when>
        <c:otherwise>
            <form method="post" id="profile-form">
                Select a profile:
                <select name="profile" id="profile-select" size="3">
                    <c:forEach var="file" items="${profiles}">
                        <fmt:formatDate value="${file.modified}" pattern="MMM dd, HH:mm" var="formattedDate"/>
                        <c:set var="formattedLine" value="${formattedDate} | ${fn:escapeXml(file.formattedName)} | ${file.formattedSize}"/>
                        <option value="${fn:escapeXml(file.name)}" class="filename" ${file.name == currentProfile ? 'selected' : ''}>
                            ${fn:replace(formattedLine, ' ', '&nbsp;')}
                        </option>
                    </c:forEach>
                </select>
                &nbsp;&nbsp;&nbsp;
                <input type="submit" name="op" value="Apply">
                <input type="submit" name="op" value="Download">
                <input type="submit" name="op" value="Delete">
            </form>
        </c:otherwise>
    </c:choose>
</div>

<script type="text/javascript">
    (function() {
        var profileQtd = ${fn:length(profiles)};
        if(profileQtd > 3){
            document.forms["profile-form"]["profile"].size = profileQtd;
        }

        var authorized = ${authorized};
        if (!authorized) {
            for (i = 0; i < document.forms.length; i++) {
                for (j = 0; j < document.forms[i].length; j++) {
                    document.forms[i][j].disabled = true;
                }
            }
        }

        document.forms["upload"]["file"].onchange = function(event){
            document.forms['upload'].submit();
        };

        document.forms["profile-form"].onsubmit = function(event){
            var form = event.target;
            var select = form["profile"];
            return confirm('Delete ' + select.options[select.selectedIndex].value + '?');
        };
    })();
</script>

<%@ include file="/WEB-INF/template/footer.jsp"%>