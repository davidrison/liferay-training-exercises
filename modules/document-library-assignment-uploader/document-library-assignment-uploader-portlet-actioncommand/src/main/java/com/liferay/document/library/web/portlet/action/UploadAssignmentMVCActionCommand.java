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
import com.liferay.portal.kernel.lock.DuplicateLockException;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.LiferayFileItemException;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.upload.UploadRequestSizeException;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portlet.PortletURLImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
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

	protected FileEntry addFileEntry(ActionRequest actionRequest)
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

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
			throws Exception {

		PortletConfig portletConfig = getPortletConfig(actionRequest);

		try {
			addFileEntry(actionRequest);
		}
		catch (Exception e) {
			handleUploadException(actionRequest, e);

			String redirect = ParamUtil.getString(
					actionRequest, "redirect");

			String errorRedirect = getErrorRedirect(portletConfig, actionRequest, redirect);

			sendRedirect(actionRequest, actionResponse, errorRedirect);
		}
	}

	protected String getErrorRedirect(
			PortletConfig portletConfig, ActionRequest actionRequest, String redirect)
			throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		PortletURLImpl portletURL = new PortletURLImpl(
				actionRequest, portletConfig.getPortletName(),
				themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);

		portletURL.setParameter(
				"mvcRenderCommandName", "/document_library/upload_assignment");
		portletURL.setParameter("redirect", redirect, false);
		portletURL.setWindowState(actionRequest.getWindowState());

		return portletURL.toString();
	}


	protected void handleUploadException(
			ActionRequest actionRequest, Exception e)
			throws Exception {

		if (e instanceof AssetCategoryException ||
			e instanceof AssetTagException ||
			e instanceof AntivirusScannerException ||
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
			e instanceof UploadRequestSizeException ||
			e instanceof DuplicateLockException ||
			e instanceof FileEntryLockException.MustOwnLock ||
			e instanceof InvalidFileVersionException ||
			e instanceof NoSuchFileEntryException ||
			e instanceof PrincipalException) {

			SessionErrors.add(actionRequest, e.getClass(), e);
		}
		else {
			throw e;
		}
	}

	@Reference(unbind = "-")
	protected void setDLAppService(DLAppService dlAppService) {
		_dlAppService = dlAppService;
	}

	private DLAppService _dlAppService;

}