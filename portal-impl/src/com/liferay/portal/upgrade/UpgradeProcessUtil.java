/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
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

package com.liferay.portal.upgrade;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.util.PropsValues;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Brian Wing Shun Chan
 * @author Alexander Chow
 * @author Raymond Augé
 */
public class UpgradeProcessUtil {

	public static String getDefaultLanguageId(long companyId) throws Exception {
		String languageId = _languageIdCache.get(companyId);

		if (languageId != null) {
			return languageId;
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		languageId = StringPool.BLANK;

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			ps = con.prepareStatement(
				"select languageId from User_ where defaultUser = ? and " +
					"companyId = " + companyId);

			ps.setBoolean(1, true);

			rs = ps.executeQuery();

			if (rs.next()) {
				languageId = rs.getString("languageId");

				_languageIdCache.put(companyId, languageId);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return languageId;
	}

	public static boolean isCreateIGImageDocumentType() {
		return _createIGImageDocumentType;
	}

	public static void setCreateIGImageDocumentType(
		boolean createIGImageDocumentType) {

		_createIGImageDocumentType = createIGImageDocumentType;
	}

	public static boolean upgradeProcess(
			int buildNumber, String[] upgradeProcessClassNames,
			ClassLoader classLoader)
		throws UpgradeException {

		return upgradeProcess(
			buildNumber, upgradeProcessClassNames, classLoader,
			PropsValues.INDEX_ON_UPGRADE);
	}

	public static boolean upgradeProcess(
			int buildNumber, String[] upgradeProcessClassNames,
			ClassLoader classLoader, boolean indexOnUpgrade)
		throws UpgradeException {

		boolean ranUpgradeProcess = false;

		boolean tempIndexReadOnly = SearchEngineUtil.isIndexReadOnly();

		if (indexOnUpgrade) {
			SearchEngineUtil.setIndexReadOnly(true);
		}

		try {
			for (String upgradeProcessClassName : upgradeProcessClassNames) {
				boolean tempRanUpgradeProcess = _upgradeProcess(
					buildNumber, upgradeProcessClassName, classLoader);

				if (tempRanUpgradeProcess) {
					ranUpgradeProcess = true;
				}
			}
		}
		finally {
			SearchEngineUtil.setIndexReadOnly(tempIndexReadOnly);
		}

		return ranUpgradeProcess;
	}

	private static boolean _upgradeProcess(
			int buildNumber, String upgradeProcessClassName,
			ClassLoader classLoader)
		throws UpgradeException {

		if (_log.isDebugEnabled()) {
			_log.debug("Initializing upgrade " + upgradeProcessClassName);
		}

		UpgradeProcess upgradeProcess = null;

		try {
			Class<?> clazz = classLoader.loadClass(upgradeProcessClassName);

			upgradeProcess = (UpgradeProcess)clazz.newInstance();
		}
		catch (Exception e) {
			_log.error(e, e);
		}

		if (upgradeProcess == null) {
			_log.error(upgradeProcessClassName + " cannot be found");

			return false;
		}

		if ((upgradeProcess.getThreshold() == 0) ||
			(upgradeProcess.getThreshold() > buildNumber)) {

			if (_log.isDebugEnabled()) {
				_log.debug("Running upgrade " + upgradeProcessClassName);
			}

			upgradeProcess.upgrade();

			if (_log.isDebugEnabled()) {
				_log.debug("Finished upgrade " + upgradeProcessClassName);
			}

			return true;
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Upgrade threshold " + upgradeProcess.getThreshold() +
					" will not trigger upgrade");

			_log.debug("Skipping upgrade " + upgradeProcessClassName);
		}

		return false;
	}

	private static final Map<Long, String> _languageIdCache =
		new HashMap<Long, String>();

	private static Log _log = LogFactoryUtil.getLog(UpgradeProcessUtil.class);

	private static boolean _createIGImageDocumentType = false;

}