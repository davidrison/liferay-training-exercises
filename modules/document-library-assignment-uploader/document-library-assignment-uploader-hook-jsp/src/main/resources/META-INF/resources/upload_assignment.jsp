<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ include file="/document_library/init.jsp" %>

<c:if test="<%= themeDisplay.isSignedIn() %>">
	<%
		String cmd = ParamUtil.getString(request, Constants.CMD, Constants.EDIT);

		String redirect = ParamUtil.getString(request, "redirect");

		String uploadProgressId = "dlFileEntryUploadProgress";

		long repositoryId = BeanParamUtil.getLong(fileEntry, request, "repositoryId");

		if (repositoryId <= 0) {

			// <liferay-ui:asset_add_button /> only passes in groupId

			repositoryId = BeanParamUtil.getLong(fileEntry, request, "groupId");
		}

		long folderId = BeanParamUtil.getLong(fileEntry, request, "folderId");
	%>

	<portlet:actionURL name="/document_library/upload_assignment" secure="<%= PropsValues.COMPANY_SECURITY_AUTH_REQUIRES_HTTPS || request.isSecure() %>" var="uploadAssignmentURL" />

	<div <%= portletTitleBasedNavigation ? "class=\"container-fluid-1280\"" : StringPool.BLANK %>>
		<aui:form action="<%= uploadAssignmentURL %>" cssClass="lfr-dynamic-form" enctype="multipart/form-data" method="post" name="fm" onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "submitAssignment(" + saveAsDraft + ");" %>'>
			<aui:input name="<%= Constants.CMD %>" type="hidden" />
			<aui:input name="redirect" type="hidden" value="<%= redirect %>" />
			<aui:input name="uploadProgressId" type="hidden" value="<%= uploadProgressId %>" />
			<aui:input name="repositoryId" type="hidden" value="<%= repositoryId %>" />
			<aui:input name="folderId" type="hidden" value="<%= folderId %>" />
			<aui:input name="fileEntryId" type="hidden" value="<%= fileEntryId %>" />

			<aui:input name="workflowAction" type="hidden" value="<%= String.valueOf(WorkflowConstants.ACTION_PUBLISH) %>" />

		</aui:form>

		<liferay-ui:upload-progress
				id="<%= uploadProgressId %>"
				message="uploading"
		/>
	</div>

	<aui:script>
		function <portlet:namespace />submitAssignment(assignment) {
		var $ = AUI.$;

		var form = $(document.<portlet:namespace />fm);

		var fileValue = form.fm('file').val();

		if (fileValue) {
		<%= HtmlUtil.escape(uploadProgressId) %>.startProgress();
		}

		form.fm('<%= Constants.CMD %>').val('<%= Constants.ADD %>');

		submitForm(form);
		}
	</aui:script>
</c:if>
