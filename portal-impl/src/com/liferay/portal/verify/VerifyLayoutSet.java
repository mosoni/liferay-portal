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

package com.liferay.portal.verify;

import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutSet;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;

import java.util.Date;
import java.util.List;

/**
 * @author Mohit Soni
 */
public class VerifyLayoutSet extends VerifyProcess {

	@Override
	protected void doVerify() throws Exception {
		verifyLayoutSet();
	}

	protected void verifyLayoutSet() throws Exception {
		ActionableDynamicQuery actionableDynamicQuery =
			LayoutSetLocalServiceUtil.getActionableDynamicQuery();

		actionableDynamicQuery.setAddCriteriaMethod(
			new ActionableDynamicQuery.AddCriteriaMethod() {

				@Override
				public void addCriteria(DynamicQuery dynamicQuery) {
					Property property =
						PropertyFactoryUtil.forName("layoutSetPrototypeUuid");

					dynamicQuery.add(
						RestrictionsFactoryUtil.and(
							property.isNotNull(),
							property.ne(StringPool.BLANK)));
				}

			});
		actionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod() {

				@Override
				public void performAction(Object object)
					throws PortalException {

					LayoutSet layoutSet = (LayoutSet)object;

					Group group = layoutSet.getGroup();

					boolean updateLayoutSet = false;

					if (group.isSite()) {
						List<Layout> layouts =
							LayoutLocalServiceUtil.getLayouts(
								group.getGroupId(),
								layoutSet.isPrivateLayout());

						if (layouts.isEmpty() &&
							group.isOrganization()) {

							updateLayoutSet = true;
						}
					}
					else if (group.isOrganization()) {
						updateLayoutSet = true;
					}

					if (updateLayoutSet) {
						layoutSet.setModifiedDate(new Date());
						layoutSet.setPageCount(0);
						layoutSet.setLayoutSetPrototypeUuid(StringPool.BLANK);
						layoutSet.setLayoutSetPrototypeLinkEnabled(
							Boolean.FALSE);

						LayoutSetLocalServiceUtil.updateLayoutSet(layoutSet);

						GroupLocalServiceUtil.updateSite(
							group.getGroupId(), false);
					}
				}

			});

		actionableDynamicQuery.performActions();
	}

}