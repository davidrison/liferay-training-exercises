/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.document.library.web.portlet.action;

import com.liferay.asset.kernel.exception.AssetCategoryException;
import com.liferay.asset.kernel.exception.AssetTagException;
import com.liferay.document.library.kernel.antivirus.AntivirusScannerException;
import com.liferay.document.library.kernel.exception.DuplicateFileEntryException;
import com.liferay.document.library.kernel.exception.DuplicateFolderNameException;
import com.liferay.document.library.kernel.exception.FileEntryLockException;
import com.liferay.document.library.kernel.exception.FileExtensionException;
import com.liferay.document.library.kernel.exception.FileMimeTypeException;
import com.liferay.document.library.kernel.exception.FileNameException;
import com.liferay.document.library.kernel.exception.FileSizeException;
import com.liferay.document.library.kernel.exception.InvalidFileVersionException;
import com.liferay.document.library.kernel.exception.NoSuchFileEntryException;
import com.liferay.document.library.kernel.exception.NoSuchFolderException;
import com.liferay.document.library.kernel.exception.SourceFileNameException;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.web.constants.DLPortletKeys;
import com.liferay.dynamic.data.mapping.kernel.StorageFieldRequiredException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.lock.DuplicateLockException;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.ServletResponseConstants;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.LiferayFileItemException;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.upload.UploadRequestSizeException;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.TextFormatter;
import com.liferay.portal.kernel.util.WebKeys;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

/**
 * @author David Rison
 */
@Component(
		property = {
				"javax.portlet.name=" + DLPortletKeys.MEDIA_GALLERY_DISPLAY,
				"mvc.command.name=/document_library/upload_assignment"
		},
		service = MVCActionCommand.class
)
public class UploadAssignmentMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
			throws Exception {

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		FileEntry fileEntry = null;

		PortletConfig portletConfig = getPortletConfig(actionRequest);

		try {
			fileEntry = updateFileEntry(
					portletConfig, actionRequest, actionResponse);

			String redirect = ParamUtil.getString(
						actionRequest, "redirect");

			sendRedirect(actionRequest, actionResponse, redirect);

		}
		catch (Exception e) {
			handleUploadException(
					portletConfig, actionRequest, actionResponse, cmd, e);
		}
	}

	protected String[] getAllowedFileExtensions(
			PortletConfig portletConfig, PortletRequest portletRequest,
			PortletResponse portletResponse)
			throws PortalException {
		return PrefsPropsUtil.getStringArray(
				PropsKeys.DL_FILE_EXTENSIONS, StringPool.COMMA);
	}

	protected void handleUploadException(
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse, String cmd, Exception e)
			throws Exception {

		if (e instanceof AssetCategoryException ||
				e instanceof AssetTagException) {

			SessionErrors.add(actionRequest, e.getClass(), e);
		}
		else if (e instanceof AntivirusScannerException ||
				e instanceof DuplicateFileEntryException ||
				e instanceof DuplicateFolderNameException ||
				e instanceof FileExtensionException ||
				e instanceof FileMimeTypeException ||
				e instanceof FileNameException ||
				e instanceof FileSizeException ||
				e instanceof LiferayFileItemException ||
				e instanceof NoSuchFolderException ||
				e instanceof SourceFileNameException ||
				e instanceof StorageFieldRequiredException ||
				e instanceof UploadRequestSizeException) {

			if (!cmd.equals(Constants.ADD_DYNAMIC) &&
					!cmd.equals(Constants.ADD_MULTIPLE) &&
					!cmd.equals(Constants.ADD_TEMP)) {

				if (e instanceof AntivirusScannerException) {
					SessionErrors.add(actionRequest, e.getClass(), e);
				}
				else {
					SessionErrors.add(actionRequest, e.getClass());
				}

				return;
			}
			else if (cmd.equals(Constants.ADD_TEMP)) {
				hideDefaultErrorMessage(actionRequest);
			}

			if (e instanceof AntivirusScannerException ||
					e instanceof DuplicateFileEntryException ||
					e instanceof FileExtensionException ||
					e instanceof FileNameException ||
					e instanceof FileSizeException ||
					e instanceof UploadRequestSizeException) {

				HttpServletResponse response =
						PortalUtil.getHttpServletResponse(actionResponse);

				response.setContentType(ContentTypes.TEXT_HTML);
				response.setStatus(HttpServletResponse.SC_OK);

				String errorMessage = StringPool.BLANK;
				int errorType = 0;

				ThemeDisplay themeDisplay =
						(ThemeDisplay)actionRequest.getAttribute(
								WebKeys.THEME_DISPLAY);

				if (e instanceof AntivirusScannerException) {
					AntivirusScannerException ase =
							(AntivirusScannerException)e;

					errorMessage = themeDisplay.translate(ase.getMessageKey());
					errorType =
							ServletResponseConstants.SC_FILE_ANTIVIRUS_EXCEPTION;
				}

				if (e instanceof DuplicateFileEntryException) {
					errorMessage = themeDisplay.translate(
							"please-enter-a-unique-document-name");
					errorType =
							ServletResponseConstants.SC_DUPLICATE_FILE_EXCEPTION;
				}
				else if (e instanceof FileExtensionException) {
					errorMessage = themeDisplay.translate(
							"please-enter-a-file-with-a-valid-extension-x",
							StringUtil.merge(
									getAllowedFileExtensions(
											portletConfig, actionRequest, actionResponse)));
					errorType =
							ServletResponseConstants.SC_FILE_EXTENSION_EXCEPTION;
				}
				else if (e instanceof FileNameException) {
					errorMessage = themeDisplay.translate(
							"please-enter-a-file-with-a-valid-file-name");
					errorType = ServletResponseConstants.SC_FILE_NAME_EXCEPTION;
				}
				else if (e instanceof FileSizeException) {
					long fileMaxSize = PrefsPropsUtil.getLong(
							PropsKeys.DL_FILE_MAX_SIZE);

					if (fileMaxSize == 0) {
						fileMaxSize = PrefsPropsUtil.getLong(
								PropsKeys.UPLOAD_SERVLET_REQUEST_IMPL_MAX_SIZE);
					}

					errorMessage = themeDisplay.translate(
							"please-enter-a-file-with-a-valid-file-size-no-larger" +
									"-than-x",
							TextFormatter.formatStorageSize(
									fileMaxSize, themeDisplay.getLocale()));

					errorType = ServletResponseConstants.SC_FILE_SIZE_EXCEPTION;
				}

				JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

				jsonObject.put("message", errorMessage);
				jsonObject.put("status", errorType);

				JSONPortletResponseUtil.writeJSON(
						actionRequest, actionResponse, jsonObject);
			}

			if (e instanceof AntivirusScannerException) {
				SessionErrors.add(actionRequest, e.getClass(), e);
			}
			else {
				SessionErrors.add(actionRequest, e.getClass());
			}
		}
		else if (e instanceof DuplicateLockException ||
				e instanceof FileEntryLockException.MustOwnLock ||
				e instanceof InvalidFileVersionException ||
				e instanceof NoSuchFileEntryException ||
				e instanceof PrincipalException) {

			if (e instanceof DuplicateLockException) {
				DuplicateLockException dle = (DuplicateLockException)e;

				SessionErrors.add(actionRequest, dle.getClass(), dle.getLock());
			}
			else {
				SessionErrors.add(actionRequest, e.getClass());
			}

			actionResponse.setRenderParameter(
					"mvcPath", "/document_library/error.jsp");
		}
		else {
			Throwable cause = e.getCause();

			if (cause instanceof DuplicateFileEntryException) {
				SessionErrors.add(
						actionRequest, DuplicateFileEntryException.class);
			}
			else {
				throw e;
			}
		}
	}

	protected FileEntry updateFileEntry(
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse)
			throws Exception {

		UploadPortletRequest uploadPortletRequest =
				PortalUtil.getUploadPortletRequest(actionRequest);

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		long repositoryId = ParamUtil.getLong(
				uploadPortletRequest, "repositoryId");
		long folderId = ParamUtil.getLong(uploadPortletRequest, "folderId");
		String sourceFileName = uploadPortletRequest.getFileName("file");
		String title = ParamUtil.getString(uploadPortletRequest, "title");
		String description = ParamUtil.getString(
				uploadPortletRequest, "description");

		if (folderId > 0) {
			Folder folder = _dlAppService.getFolder(folderId);

			if (folder.getGroupId() != themeDisplay.getScopeGroupId()) {
				throw new NoSuchFolderException("{folderId=" + folderId + "}");
			}
		}

		InputStream inputStream = null;

		try {
			String contentType = uploadPortletRequest.getContentType("file");
			long size = uploadPortletRequest.getSize("file");

			if (size == 0) {
				contentType = MimeTypesUtil.getContentType(title);
			}

			inputStream = uploadPortletRequest.getFileAsStream("file");

			ServiceContext serviceContext = ServiceContextFactory.getInstance(
					DLFileEntry.class.getName(), uploadPortletRequest);

			FileEntry fileEntry = _dlAppService.addFileEntry(
					repositoryId, folderId, sourceFileName, contentType, title,
					description, StringPool.BLANK, inputStream, size, serviceContext);

			return fileEntry;
		}
		finally {
			StreamUtil.cleanUp(inputStream);
		}
	}

	@Reference(unbind = "-")
	protected void setDLAppService(DLAppService dlAppService) {
		_dlAppService = dlAppService;
	}

	private DLAppService _dlAppService;

}