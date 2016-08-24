/**
 * Copyright (c) 2000-2016 Liferay, Inc. All rights reserved.
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

package com.liferay.frontend.editor.alloyeditor.syntax.extension.web.internal.servlet.taglib;

import com.liferay.frontend.editor.alloyeditor.syntax.extension.web.util.FrontendAlloyEditorSyntaxExtensionHelper;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.taglib.BaseDynamicInclude;
import com.liferay.portal.kernel.servlet.taglib.DynamicInclude;
import com.liferay.portal.kernel.util.StreamUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

/**
 * When the Alloy Editor is requested, this class injects a syntax javascript
 * file into the editor if the extension has been enabled for the
 * portal instance being accessed.
 *
 * For training purposes, the enablement is hardcoded to true.
 *
 * @author David W. Rison
 */
@Component(immediate = true, service = DynamicInclude.class)
public class FrontendAlloyEditorSyntaxExtensionOnEditOrCreateDynamicInclude
	extends BaseDynamicInclude {

	@Override
	public void include(
			HttpServletRequest request, HttpServletResponse response,
			String key)
		throws IOException {

		if (!FrontendAlloyEditorSyntaxExtensionHelper.isEnabled()) {
			return;
		}

		try {
			Bundle bundle = _bundleContext.getBundle();

			URL entryURL = bundle.getEntry(_ALLOYEDITOR_SYNTAX_JS);

			StreamUtil.transfer(entryURL.openStream(), response.getOutputStream());
		}
		catch (Exception e) {
			_log.error("Unable to send editor js " + _ALLOYEDITOR_SYNTAX_JS, e);

			throw new IOException("Unable to include JSP " + _ALLOYEDITOR_SYNTAX_JS, e);
		}
	}

	@Override
	public void register(
		DynamicInclude.DynamicIncludeRegistry dynamicIncludeRegistry) {

		dynamicIncludeRegistry.register(
				"com.liferay.frontend.editor.alloyeditor.web#alloyeditor#onEditorCreate");
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	private BundleContext _bundleContext;

	private static final String _ALLOYEDITOR_SYNTAX_JS =
			"/META-INF/resources/html/editors/alloyeditor/extension/syntax.js";

	private static final Log _log = LogFactoryUtil.getLog(
		FrontendAlloyEditorSyntaxExtensionOnEditOrCreateDynamicInclude.class);


}