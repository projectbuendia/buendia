<ul id="menu">
	<li class="first">
		<a href="${pageContext.request.contextPath}/admin"><spring:message code="admin.title.short"/></a>
	</li>
	<openmrs:hasPrivilege privilege="View Synchronization Records">
		<li <c:if test='<%= request.getRequestURI().contains("overview") %>'>class="active"</c:if>>
			<a href="${pageContext.request.contextPath}/module/sync/overview.htm">
				<spring:message code="sync.overview.title"/>
			</a>
		</li>
		<li <c:if test='<%= request.getRequestURI().contains("config") %>'>class="active"</c:if>>
			<a href="${pageContext.request.contextPath}/module/sync/config.list">
				<spring:message code="sync.config.title"/>
			</a>
		</li>
		<li <c:if test='<%= request.getRequestURI().contains("history") %>'>class="active"</c:if>>
			<a href="${pageContext.request.contextPath}/module/sync/history.list">
				<spring:message code="sync.history.title"/>
			</a>
		</li>
		<li <c:if test='<%= request.getRequestURI().contains("statistics") %>'>class="active"</c:if>>
			<a href="${pageContext.request.contextPath}/module/sync/statistics.list">
				<spring:message code="sync.statistics.title"/>
			</a>
		</li>
		<li <c:if test='<%= request.getRequestURI().contains("maintenance") %>'>class="active"</c:if>>
			<a href="${pageContext.request.contextPath}/module/sync/maintenance.form">
				<spring:message code="sync.maintenance.title"/>
			</a>
		</li>
	</openmrs:hasPrivilege>
	
	<openmrs:hasPrivilege privilege="Manage Synchronization">
		<li <c:if test='<%= request.getRequestURI().contains("upgrade") %>'>class="active"</c:if>>
			<a href="${pageContext.request.contextPath}/module/sync/upgrade.form">
				<spring:message code="sync.upgrade.title"/>
			</a>
		</li>
	</openmrs:hasPrivilege>
	
	<li <c:if test='<%= request.getRequestURI().contains("help") %>'>class="active"</c:if>>
		<a href="${pageContext.request.contextPath}/module/sync/help.htm">
			<spring:message code="sync.help.title"/>
		</a>
	</li>
	<openmrs:extensionPoint pointId="org.openmrs.admin.sync.localHeader" type="html">
			<c:forEach items="${extension.links}" var="link">
				<li <c:if test='${fn:endsWith(pageContext.request.requestURI, link.key)}'>class="active"</c:if> >
					<a href="${pageContext.request.contextPath}/${link.key}"><spring:message code="${link.value}"/></a>
				</li>
			</c:forEach>
	</openmrs:extensionPoint>
</ul>