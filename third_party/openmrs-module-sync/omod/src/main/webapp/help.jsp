<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/help.htm" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />
<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />

<h2><spring:message code="sync.help.title"/></h2>

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
			
    	function changeView(divId){
        	var div = document.getElementById(divId);
        	var oldDivs=div.parentNode.getElementsByTagName('div');
        	for(var i=0;i<oldDivs.length;++i){
            	if(oldDivs[i].id==divId){
                	oldDivs[i].style.display='';
            	}
            	else{
                	oldDivs[i].style.display='none';
            	}
        	}
    	}
			
    -->
</script>

<b class="boxHeader"><spring:message code="sync.help.heading"/></b>
<div class="box">
    <table width="100%" height="117" border="0" cellpadding="0"
           cellspacing="0">
        <tr>
        <td width="65%" id="answer"  valign="top">
                <div class="syncInfoBox" style="height: 100%;">
                    <div  style="display:block" id="whatIsSynchronization"><b><spring:message
                            code="sync.help.whatIsSynchronization" /></b>
                        <p><spring:message
                            code="sync.help.whatIsSynchronizationAnswer" />
                    <p></div>
                    <div style="display:none" id="whatIsDifferenceSyncAndImportExport"><b><spring:message
                            code="sync.help.whatIsDifferenceSyncAndImportExport" /></b>
                        <p><spring:message
                            code="sync.help.whatIsDifferenceSyncAndImportExportAnswer" />
                    <p></div>
                    <div style="display:none" id="howDoIUseSynchronization"><b><spring:message
                            code="sync.help.howDoIUseSynchronization" /></b>
                        <p><spring:message
                            code="sync.help.howDoIUseSynchronizationAnswer" />
                    <p></div>
                    <div style="display:none" id="howDoISetupSyncNode">
                        <p><spring:message
                            code="sync.settings.server.clone.notes" />
                    <p></div>
                    <div style="display:none" id="howDoIConfigureParent"><b><spring:message
                            code="sync.help.howDoIConfigureParent" /></b>
                        <p><spring:message
                            code="sync.help.howDoIConfigureParentAnswer" />
                    <p></div>
                    <div style="display:none" id="howDoIConfigureChild"><b><spring:message
                            code="sync.help.howDoIConfigureChild" /></b>
                        <p><spring:message
                            code="sync.help.howDoIConfigureChildAnswer" />
                    <p></div>
                    <div style="display:none" id="howDoISendToParentViaWeb"><b><spring:message
                            code="sync.help.howDoISendToParentViaWeb" /></b>
                        <p><spring:message
                            code="sync.help.howDoISendToParentViaWebAnswer" />
                    <p></div>
                    <div style="display:none" id="howDoISendToParentViaDisk"><b><spring:message
                            code="sync.help.howDoISendToParentViaDisk" /></b>
                        <p><spring:message
                            code="sync.help.howDoISendToParentViaDiskAnswer" />
                    <p></div>
                    <div style="display:none" id="howDoIEditSyncRecordRetails"><b><spring:message
                            code="sync.help.howDoIEditSyncRecordRetails" /></b>
                        <p><spring:message
                            code="sync.help.howDoIEditSyncRecordRetailsAnswer" />
                    <p></div>
                </div>
            </td>
            <td width="3%" id="answer">&nbsp;</td>
            <td width="32%"><b><spring:message
                    code="sync.help.aboutSynchronization" /></b>
                <ul>
                    <li><a href="javascript:changeView('whatIsSynchronization');"><spring:message
                        code="sync.help.whatIsSynchronization" /></a></li>
                    <br>
                    <li><a href="javascript:changeView('whatIsDifferenceSyncAndImportExport');"><spring:message
                        code="sync.help.whatIsDifferenceSyncAndImportExport" /></a></li>
                    <br>
                    <li><a href="javascript:changeView('howDoIUseSynchronization');"><spring:message
                        code="sync.help.howDoIUseSynchronization" /></a></li>
                </ul>
                <b><spring:message
                    code="sync.help.configureSynchronization" /></b>
                <ul>
                	<li><a href="javascript:changeView('howDoISetupSyncNode');"><spring:message
                        code="sync.help.howDoISetupSyncNode" /></a></li>
                    <br>
                    <li><a href="javascript:changeView('howDoIConfigureParent');"><spring:message
                        code="sync.help.howDoIConfigureParent" /></a></li>
                    <br>
                    <li><a href="javascript:changeView('howDoIConfigureChild');"><spring:message
                        code="sync.help.howDoIConfigureChild" /></a></li>
                </ul>
                <b><spring:message
                    code="sync.help.manageSynchronization" /></b>

                <ul>
                    <li><a href="javascript:changeView('howDoISendToParentViaWeb');"><spring:message
                        code="sync.help.howDoISendToParentViaWeb" /></a></li>
                    <br>
                    <li><a href="javascript:changeView('howDoISendToParentViaDisk');"><spring:message
                        code="sync.help.howDoISendToParentViaDisk" /></a></li>
                    <br>
                    <li><a href="javascript:changeView('howDoIEditSyncRecordRetails');"><spring:message
                        code="sync.help.howDoIEditSyncRecordRetails" /></a></li>
                </ul>
            </td>
        </tr>
    </table>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
