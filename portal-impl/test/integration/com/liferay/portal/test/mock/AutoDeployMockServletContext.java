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

package com.liferay.portal.test.mock;

import java.io.File;

import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletContext;

/**
 * @author Will Newbury
 */
public class AutoDeployMockServletContext extends MockServletContext {

	public AutoDeployMockServletContext() {
		super(resourceBasePath, new FileSystemResourceLoader());
	}

	/**
	 * @see com.liferay.portal.server.capabilities.TomcatServerCapabilities
	 */
	protected Boolean autoDeploy = Boolean.TRUE;

	protected static final String resourceBasePath;

	static {
		File file = new File("portal-web/docroot");

		resourceBasePath = "file:" + file.getAbsolutePath();
	}

}