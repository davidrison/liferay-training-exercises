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
		String redirect = ParamUtil.getString(request, "redirect");

		FileEntry fileEntry = null;

		long repositoryId = BeanParamUtil.getLong(fileEntry, request, "repositoryId");

		if (repositoryId <= 0) {
			repositoryId = BeanParamUtil.getLong(fileEntry, request, "groupId");
		}

		long folderId = BeanParamUtil.getLong(fileEntry, request, "folderId");

		long fileEntryTypeId = ParamUtil.getLong(request, "fileEntryTypeId", -1);

		DLFileEntryType dlFileEntryType = null;

		if (fileEntryTypeId >= 0) {
			dlFileEntryType = DLFileEntryTypeLocalServiceUtil.getFileEntryType(fileEntryTypeId);
		}

		DLEditFileEntryDisplayContext dlEditFileEntryDisplayContext =
				dlDisplayContextProvider.getDLEditFileEntryDisplayContext(request, response, dlFileEntryType);

		String headerTitle = LanguageUtil.get(request, "new-document");

		if ((dlFileEntryType != null) && (fileEntryTypeId != 0)) {
			headerTitle = LanguageUtil.format(request, "new-x", dlFileEntryType.getName(locale), false);
		}

		portletDisplay.setShowBackIcon(true);
		portletDisplay.setURLBack(redirect);

		renderResponse.setTitle(headerTitle);

		String uploadProgressId = "dlFileEntryUploadProgress";
	%>

	<portlet:actionURL
			name="/document_library/upload_assignment"
			secure="<%= PropsValues.COMPANY_SECURITY_AUTH_REQUIRES_HTTPS || request.isSecure() %>"
			var="uploadAssignmentURL" />

	<div class="container-fluid-1280">
		<aui:form action="<%= uploadAssignmentURL %>"
				  cssClass="lfr-dynamic-form"
				  enctype="multipart/form-data"
				  method="post"
				  name="fm"
				  onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "submitAssignment();" %>'>

			<aui:input name="redirect" type="hidden" value="<%= redirect %>" />
			<aui:input name="uploadProgressId" type="hidden" value="<%= uploadProgressId %>" />
			<aui:input name="repositoryId" type="hidden" value="<%= repositoryId %>" />
			<aui:input name="folderId" type="hidden" value="<%= folderId %>" />

			<liferay-ui:error exception="<%= AntivirusScannerException.class %>">

				<%
					AntivirusScannerException ase = (AntivirusScannerException)errorException;
				%>

				<liferay-ui:message key="<%= ase.getMessageKey() %>" />
			</liferay-ui:error>

			<liferay-ui:error exception="<%= DuplicateFileEntryException.class %>" message="please-enter-a-unique-document-name" />
			<liferay-ui:error exception="<%= DuplicateFolderNameException.class %>" message="please-enter-a-unique-document-name" />

			<liferay-ui:error exception="<%= LiferayFileItemException.class %>">
				<liferay-ui:message arguments="<%= TextFormatter.formatStorageSize(LiferayFileItem.THRESHOLD_SIZE, locale) %>" key="please-enter-valid-content-with-valid-content-size-no-larger-than-x" translateArguments="<%= false %>" />
			</liferay-ui:error>

			<liferay-ui:error exception="<%= FileExtensionException.class %>">
				<liferay-ui:message key="document-names-must-end-with-one-of-the-following-extensions" /> <%= StringUtil.merge(PrefsPropsUtil.getStringArray(PropsKeys.DL_FILE_EXTENSIONS, StringPool.COMMA), StringPool.COMMA_AND_SPACE) %>.
			</liferay-ui:error>

			<liferay-ui:error exception="<%= FileMimeTypeException.class %>">
				<liferay-ui:message key="media-files-must-be-one-of-the-following-formats" /> <%= StringUtil.merge(dlPortletInstanceSettings.getMimeTypes(), StringPool.COMMA_AND_SPACE) %>.
			</liferay-ui:error>

			<liferay-ui:error exception="<%= FileNameException.class %>" message="please-enter-a-file-with-a-valid-file-name" />
			<liferay-ui:error exception="<%= NoSuchFolderException.class %>" message="please-enter-a-valid-folder" />

			<liferay-ui:error exception="<%= SourceFileNameException.class %>">
				<liferay-ui:message key="the-source-file-does-not-have-the-same-extension-as-the-original-file" />
			</liferay-ui:error>

			<%
				long fileMaxSize = dlEditFileEntryDisplayContext.getMaximumUploadSize();
			%>

			<liferay-ui:error exception="<%= FileSizeException.class %>">
				<liferay-ui:message arguments="<%= TextFormatter.formatStorageSize(fileMaxSize, locale) %>" key="please-enter-a-file-with-a-valid-file-size-no-larger-than-x" translateArguments="<%= false %>" />
			</liferay-ui:error>

			<liferay-ui:error exception="<%= UploadRequestSizeException.class %>">
				<liferay-ui:message arguments="<%= TextFormatter.formatStorageSize(dlEditFileEntryDisplayContext.getMaximumUploadRequestSize(), locale) %>" key="request-is-larger-than-x-and-could-not-be-processed" translateArguments="<%= false %>" />
			</liferay-ui:error>

			<liferay-ui:asset-categories-error />

			<liferay-ui:asset-tags-error />

			<%@ include file="/document_library/edit_file_entry_picker.jspf" %>

			<aui:input name="title" />
			<aui:input name="description" />

			<aui:button-row>
				<aui:button
						cssClass="btn-lg"
						name="submitButton"
						type="submit"
						value="<%= dlEditFileEntryDisplayContext.getPublishButtonLabel() %>" />

				<aui:button cssClass="btn-lg" href="<%= redirect %>" type="cancel" />
			</aui:button-row>
		</aui:form>

		<liferay-ui:upload-progress
				id="<%= uploadProgressId %>"
				message="uploading"
		/>
	</div>

	<aui:script>
		function <portlet:namespace />submitAssignment() {
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
