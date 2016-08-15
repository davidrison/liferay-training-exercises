package com.liferay.document.library.web.portlet.action;

import com.liferay.document.library.web.constants.DLPortletKeys;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import org.osgi.service.component.annotations.Component;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

/**
 * Created by davidrison on 8/10/16.
 */
@Component(
		property = {
				"javax.portlet.name=" + DLPortletKeys.MEDIA_GALLERY_DISPLAY,
				"mvc.command.name=/document_library/upload_assignment"
		},
		service = MVCRenderCommand.class
)
public class UploadAssignmentMVCRenderCommand implements MVCRenderCommand {

	public String render(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException {
		return "/upload_assignment.jsp";
	}
}
