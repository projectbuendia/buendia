<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/import.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<h2><spring:message code="sync.import.title"/></h2>

<script language="JavaScript">
	<!--

		function showHideDiv(id) {
			var div = document.getElementById(id);
			if ( div ) {
				if ( div.style.display != "none" ) {
					div.style.display = "none";
				} else { 
					div.style.display = "";
				}
			}
		}
	
		function doSubmit() {
			document.getElementById("submitButton").disabled = true;
			showDiv("infoText");
			return true;
		}
		
	-->
</script>

<b class="boxHeader"><spring:message code="sync.import.import.from.file"/></b>
<div class="box">
	<table>
		<tr>
			<td>
				<form method="post" enctype="multipart/form-data" onSubmit="return doSubmit();">
				
					<spring:message code="sync.import.filePrompt" />
					
					<input type="file" name="syncDataFile" value="" />
					<input type="hidden" name="upload" value="true" />
					<input type="hidden" name="isResponse" value="true" />
					<input type="submit" value="<spring:message code="sync.import.importData" />" id="submitButton" />
				
					<span id="infoText" style="display:none;"><spring:message code="sync.import.generatingResponse" /></span>
				</form>
			</td>
		</tr>
	</table>
</div>

<br>
&nbsp;&nbsp;<a href="javascript://" onclick="showHideDiv('pasteImport');"><spring:message code="sync.import.copyPaste"/></a>

<div id="pasteImport" style="display:none;">
	<br>
	<br>
	
	<b class="boxHeader"><spring:message code="sync.import.paste.data"/></b>
	<div class="box">
		<form method="post" action="import.list">
			<table>
				<tr>
					<td align="right" valign="top">
						<b><spring:message code="sync.import.paste.here" /></b>
					</td>
					<td align="left" valign="top">
						<textarea name="syncData" rows="16" cols="80"></textarea>
					</td>
				</tr>
				<tr>
					<td></td>
					<td>
						<input type="submit" value="<spring:message code="sync.import.importData" />" />
					</td>
				</tr>
			</table>
		</form>
	</div>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
