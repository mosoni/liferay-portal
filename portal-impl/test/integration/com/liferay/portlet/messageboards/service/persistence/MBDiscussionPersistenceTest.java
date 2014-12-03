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

package com.liferay.portlet.messageboards.service.persistence;

import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.IntegerWrapper;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.OrderByComparatorFactoryUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.service.ServiceTestUtil;
import com.liferay.portal.test.LiferayIntegrationJUnitTestRunner;
import com.liferay.portal.util.PropsValues;

import com.liferay.portlet.messageboards.NoSuchDiscussionException;
import com.liferay.portlet.messageboards.model.MBDiscussion;
import com.liferay.portlet.messageboards.model.impl.MBDiscussionModelImpl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Brian Wing Shun Chan
 */
@RunWith(LiferayIntegrationJUnitTestRunner.class)
public class MBDiscussionPersistenceTest {
	@BeforeClass
	public static void setUpClass() {
		PropsValues.SPRING_HIBERNATE_SESSION_DELEGATED = false;
	}

	@AfterClass
	public static void tearDownClass() {
		PropsValues.SPRING_HIBERNATE_SESSION_DELEGATED = true;
	}

	@Before
	public void setUp() {
		_listeners = _persistence.getListeners();

		for (ModelListener<MBDiscussion> modelListener : _listeners) {
			_persistence.unregisterListener(modelListener);
		}
	}

	@After
	public void tearDown() throws Exception {
		Iterator<MBDiscussion> iterator = _mbDiscussions.iterator();

		while (iterator.hasNext()) {
			_persistence.remove(iterator.next());

			iterator.remove();
		}

		for (ModelListener<MBDiscussion> modelListener : _listeners) {
			_persistence.registerListener(modelListener);
		}
	}

	@Test
	public void testCreate() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		MBDiscussion mbDiscussion = _persistence.create(pk);

		Assert.assertNotNull(mbDiscussion);

		Assert.assertEquals(mbDiscussion.getPrimaryKey(), pk);
	}

	@Test
	public void testRemove() throws Exception {
		MBDiscussion newMBDiscussion = addMBDiscussion();

		_persistence.remove(newMBDiscussion);

		MBDiscussion existingMBDiscussion = _persistence.fetchByPrimaryKey(newMBDiscussion.getPrimaryKey());

		Assert.assertNull(existingMBDiscussion);
	}

	@Test
	public void testUpdateNew() throws Exception {
		addMBDiscussion();
	}

	@Test
	public void testUpdateExisting() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		MBDiscussion newMBDiscussion = _persistence.create(pk);

		newMBDiscussion.setUuid(ServiceTestUtil.randomString());

		newMBDiscussion.setGroupId(ServiceTestUtil.nextLong());

		newMBDiscussion.setCompanyId(ServiceTestUtil.nextLong());

		newMBDiscussion.setUserId(ServiceTestUtil.nextLong());

		newMBDiscussion.setUserName(ServiceTestUtil.randomString());

		newMBDiscussion.setCreateDate(ServiceTestUtil.nextDate());

		newMBDiscussion.setModifiedDate(ServiceTestUtil.nextDate());

		newMBDiscussion.setClassNameId(ServiceTestUtil.nextLong());

		newMBDiscussion.setClassPK(ServiceTestUtil.nextLong());

		newMBDiscussion.setThreadId(ServiceTestUtil.nextLong());

		_mbDiscussions.add(_persistence.update(newMBDiscussion));

		MBDiscussion existingMBDiscussion = _persistence.findByPrimaryKey(newMBDiscussion.getPrimaryKey());

		Assert.assertEquals(existingMBDiscussion.getUuid(),
			newMBDiscussion.getUuid());
		Assert.assertEquals(existingMBDiscussion.getDiscussionId(),
			newMBDiscussion.getDiscussionId());
		Assert.assertEquals(existingMBDiscussion.getGroupId(),
			newMBDiscussion.getGroupId());
		Assert.assertEquals(existingMBDiscussion.getCompanyId(),
			newMBDiscussion.getCompanyId());
		Assert.assertEquals(existingMBDiscussion.getUserId(),
			newMBDiscussion.getUserId());
		Assert.assertEquals(existingMBDiscussion.getUserName(),
			newMBDiscussion.getUserName());
		Assert.assertEquals(Time.getShortTimestamp(
				existingMBDiscussion.getCreateDate()),
			Time.getShortTimestamp(newMBDiscussion.getCreateDate()));
		Assert.assertEquals(Time.getShortTimestamp(
				existingMBDiscussion.getModifiedDate()),
			Time.getShortTimestamp(newMBDiscussion.getModifiedDate()));
		Assert.assertEquals(existingMBDiscussion.getClassNameId(),
			newMBDiscussion.getClassNameId());
		Assert.assertEquals(existingMBDiscussion.getClassPK(),
			newMBDiscussion.getClassPK());
		Assert.assertEquals(existingMBDiscussion.getThreadId(),
			newMBDiscussion.getThreadId());
	}

	@Test
	public void testFindByPrimaryKeyExisting() throws Exception {
		MBDiscussion newMBDiscussion = addMBDiscussion();

		MBDiscussion existingMBDiscussion = _persistence.findByPrimaryKey(newMBDiscussion.getPrimaryKey());

		Assert.assertEquals(existingMBDiscussion, newMBDiscussion);
	}

	@Test
	public void testFindByPrimaryKeyMissing() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		try {
			_persistence.findByPrimaryKey(pk);

			Assert.fail(
				"Missing entity did not throw NoSuchDiscussionException");
		}
		catch (NoSuchDiscussionException nsee) {
		}
	}

	@Test
	public void testFindAll() throws Exception {
		try {
			_persistence.findAll(QueryUtil.ALL_POS, QueryUtil.ALL_POS,
				getOrderByComparator());
		}
		catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	protected OrderByComparator getOrderByComparator() {
		return OrderByComparatorFactoryUtil.create("MBDiscussion", "uuid",
			true, "discussionId", true, "groupId", true, "companyId", true,
			"userId", true, "userName", true, "createDate", true,
			"modifiedDate", true, "classNameId", true, "classPK", true,
			"threadId", true);
	}

	@Test
	public void testFetchByPrimaryKeyExisting() throws Exception {
		MBDiscussion newMBDiscussion = addMBDiscussion();

		MBDiscussion existingMBDiscussion = _persistence.fetchByPrimaryKey(newMBDiscussion.getPrimaryKey());

		Assert.assertEquals(existingMBDiscussion, newMBDiscussion);
	}

	@Test
	public void testFetchByPrimaryKeyMissing() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		MBDiscussion missingMBDiscussion = _persistence.fetchByPrimaryKey(pk);

		Assert.assertNull(missingMBDiscussion);
	}

	@Test
	public void testActionableDynamicQuery() throws Exception {
		final IntegerWrapper count = new IntegerWrapper();

		ActionableDynamicQuery actionableDynamicQuery = new MBDiscussionActionableDynamicQuery() {
				@Override
				protected void performAction(Object object) {
					MBDiscussion mbDiscussion = (MBDiscussion)object;

					Assert.assertNotNull(mbDiscussion);

					count.increment();
				}
			};

		actionableDynamicQuery.performActions();

		Assert.assertEquals(count.getValue(), _persistence.countAll());
	}

	@Test
	public void testDynamicQueryByPrimaryKeyExisting()
		throws Exception {
		MBDiscussion newMBDiscussion = addMBDiscussion();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(MBDiscussion.class,
				MBDiscussion.class.getClassLoader());

		dynamicQuery.add(RestrictionsFactoryUtil.eq("discussionId",
				newMBDiscussion.getDiscussionId()));

		List<MBDiscussion> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(1, result.size());

		MBDiscussion existingMBDiscussion = result.get(0);

		Assert.assertEquals(existingMBDiscussion, newMBDiscussion);
	}

	@Test
	public void testDynamicQueryByPrimaryKeyMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(MBDiscussion.class,
				MBDiscussion.class.getClassLoader());

		dynamicQuery.add(RestrictionsFactoryUtil.eq("discussionId",
				ServiceTestUtil.nextLong()));

		List<MBDiscussion> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testDynamicQueryByProjectionExisting()
		throws Exception {
		MBDiscussion newMBDiscussion = addMBDiscussion();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(MBDiscussion.class,
				MBDiscussion.class.getClassLoader());

		dynamicQuery.setProjection(ProjectionFactoryUtil.property(
				"discussionId"));

		Object newDiscussionId = newMBDiscussion.getDiscussionId();

		dynamicQuery.add(RestrictionsFactoryUtil.in("discussionId",
				new Object[] { newDiscussionId }));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(1, result.size());

		Object existingDiscussionId = result.get(0);

		Assert.assertEquals(existingDiscussionId, newDiscussionId);
	}

	@Test
	public void testDynamicQueryByProjectionMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(MBDiscussion.class,
				MBDiscussion.class.getClassLoader());

		dynamicQuery.setProjection(ProjectionFactoryUtil.property(
				"discussionId"));

		dynamicQuery.add(RestrictionsFactoryUtil.in("discussionId",
				new Object[] { ServiceTestUtil.nextLong() }));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testResetOriginalValues() throws Exception {
		if (!PropsValues.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE) {
			return;
		}

		MBDiscussion newMBDiscussion = addMBDiscussion();

		_persistence.clearCache();

		MBDiscussionModelImpl existingMBDiscussionModelImpl = (MBDiscussionModelImpl)_persistence.findByPrimaryKey(newMBDiscussion.getPrimaryKey());

		Assert.assertTrue(Validator.equals(
				existingMBDiscussionModelImpl.getUuid(),
				existingMBDiscussionModelImpl.getOriginalUuid()));
		Assert.assertEquals(existingMBDiscussionModelImpl.getGroupId(),
			existingMBDiscussionModelImpl.getOriginalGroupId());

		Assert.assertEquals(existingMBDiscussionModelImpl.getThreadId(),
			existingMBDiscussionModelImpl.getOriginalThreadId());

		Assert.assertEquals(existingMBDiscussionModelImpl.getClassNameId(),
			existingMBDiscussionModelImpl.getOriginalClassNameId());
		Assert.assertEquals(existingMBDiscussionModelImpl.getClassPK(),
			existingMBDiscussionModelImpl.getOriginalClassPK());
	}

	protected MBDiscussion addMBDiscussion() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		MBDiscussion mbDiscussion = _persistence.create(pk);

		mbDiscussion.setUuid(ServiceTestUtil.randomString());

		mbDiscussion.setGroupId(ServiceTestUtil.nextLong());

		mbDiscussion.setCompanyId(ServiceTestUtil.nextLong());

		mbDiscussion.setUserId(ServiceTestUtil.nextLong());

		mbDiscussion.setUserName(ServiceTestUtil.randomString());

		mbDiscussion.setCreateDate(ServiceTestUtil.nextDate());

		mbDiscussion.setModifiedDate(ServiceTestUtil.nextDate());

		mbDiscussion.setClassNameId(ServiceTestUtil.nextLong());

		mbDiscussion.setClassPK(ServiceTestUtil.nextLong());

		mbDiscussion.setThreadId(ServiceTestUtil.nextLong());

		_mbDiscussions.add(_persistence.update(mbDiscussion));

		return mbDiscussion;
	}

	private static Log _log = LogFactoryUtil.getLog(MBDiscussionPersistenceTest.class);
	private List<MBDiscussion> _mbDiscussions = new ArrayList<MBDiscussion>();
	private ModelListener<MBDiscussion>[] _listeners;
	private MBDiscussionPersistence _persistence = (MBDiscussionPersistence)PortalBeanLocatorUtil.locate(MBDiscussionPersistence.class.getName());
}