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

package com.liferay.portlet.polls.service.persistence;

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

import com.liferay.portlet.polls.NoSuchVoteException;
import com.liferay.portlet.polls.model.PollsVote;
import com.liferay.portlet.polls.model.impl.PollsVoteModelImpl;

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
public class PollsVotePersistenceTest {
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

		for (ModelListener<PollsVote> modelListener : _listeners) {
			_persistence.unregisterListener(modelListener);
		}
	}

	@After
	public void tearDown() throws Exception {
		Iterator<PollsVote> iterator = _pollsVotes.iterator();

		while (iterator.hasNext()) {
			_persistence.remove(iterator.next());

			iterator.remove();
		}

		for (ModelListener<PollsVote> modelListener : _listeners) {
			_persistence.registerListener(modelListener);
		}
	}

	@Test
	public void testCreate() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		PollsVote pollsVote = _persistence.create(pk);

		Assert.assertNotNull(pollsVote);

		Assert.assertEquals(pollsVote.getPrimaryKey(), pk);
	}

	@Test
	public void testRemove() throws Exception {
		PollsVote newPollsVote = addPollsVote();

		_persistence.remove(newPollsVote);

		PollsVote existingPollsVote = _persistence.fetchByPrimaryKey(newPollsVote.getPrimaryKey());

		Assert.assertNull(existingPollsVote);
	}

	@Test
	public void testUpdateNew() throws Exception {
		addPollsVote();
	}

	@Test
	public void testUpdateExisting() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		PollsVote newPollsVote = _persistence.create(pk);

		newPollsVote.setUuid(ServiceTestUtil.randomString());

		newPollsVote.setGroupId(ServiceTestUtil.nextLong());

		newPollsVote.setCompanyId(ServiceTestUtil.nextLong());

		newPollsVote.setUserId(ServiceTestUtil.nextLong());

		newPollsVote.setUserName(ServiceTestUtil.randomString());

		newPollsVote.setCreateDate(ServiceTestUtil.nextDate());

		newPollsVote.setModifiedDate(ServiceTestUtil.nextDate());

		newPollsVote.setQuestionId(ServiceTestUtil.nextLong());

		newPollsVote.setChoiceId(ServiceTestUtil.nextLong());

		newPollsVote.setVoteDate(ServiceTestUtil.nextDate());

		_pollsVotes.add(_persistence.update(newPollsVote));

		PollsVote existingPollsVote = _persistence.findByPrimaryKey(newPollsVote.getPrimaryKey());

		Assert.assertEquals(existingPollsVote.getUuid(), newPollsVote.getUuid());
		Assert.assertEquals(existingPollsVote.getVoteId(),
			newPollsVote.getVoteId());
		Assert.assertEquals(existingPollsVote.getGroupId(),
			newPollsVote.getGroupId());
		Assert.assertEquals(existingPollsVote.getCompanyId(),
			newPollsVote.getCompanyId());
		Assert.assertEquals(existingPollsVote.getUserId(),
			newPollsVote.getUserId());
		Assert.assertEquals(existingPollsVote.getUserName(),
			newPollsVote.getUserName());
		Assert.assertEquals(Time.getShortTimestamp(
				existingPollsVote.getCreateDate()),
			Time.getShortTimestamp(newPollsVote.getCreateDate()));
		Assert.assertEquals(Time.getShortTimestamp(
				existingPollsVote.getModifiedDate()),
			Time.getShortTimestamp(newPollsVote.getModifiedDate()));
		Assert.assertEquals(existingPollsVote.getQuestionId(),
			newPollsVote.getQuestionId());
		Assert.assertEquals(existingPollsVote.getChoiceId(),
			newPollsVote.getChoiceId());
		Assert.assertEquals(Time.getShortTimestamp(
				existingPollsVote.getVoteDate()),
			Time.getShortTimestamp(newPollsVote.getVoteDate()));
	}

	@Test
	public void testFindByPrimaryKeyExisting() throws Exception {
		PollsVote newPollsVote = addPollsVote();

		PollsVote existingPollsVote = _persistence.findByPrimaryKey(newPollsVote.getPrimaryKey());

		Assert.assertEquals(existingPollsVote, newPollsVote);
	}

	@Test
	public void testFindByPrimaryKeyMissing() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		try {
			_persistence.findByPrimaryKey(pk);

			Assert.fail("Missing entity did not throw NoSuchVoteException");
		}
		catch (NoSuchVoteException nsee) {
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
		return OrderByComparatorFactoryUtil.create("PollsVote", "uuid", true,
			"voteId", true, "groupId", true, "companyId", true, "userId", true,
			"userName", true, "createDate", true, "modifiedDate", true,
			"questionId", true, "choiceId", true, "voteDate", true);
	}

	@Test
	public void testFetchByPrimaryKeyExisting() throws Exception {
		PollsVote newPollsVote = addPollsVote();

		PollsVote existingPollsVote = _persistence.fetchByPrimaryKey(newPollsVote.getPrimaryKey());

		Assert.assertEquals(existingPollsVote, newPollsVote);
	}

	@Test
	public void testFetchByPrimaryKeyMissing() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		PollsVote missingPollsVote = _persistence.fetchByPrimaryKey(pk);

		Assert.assertNull(missingPollsVote);
	}

	@Test
	public void testActionableDynamicQuery() throws Exception {
		final IntegerWrapper count = new IntegerWrapper();

		ActionableDynamicQuery actionableDynamicQuery = new PollsVoteActionableDynamicQuery() {
				@Override
				protected void performAction(Object object) {
					PollsVote pollsVote = (PollsVote)object;

					Assert.assertNotNull(pollsVote);

					count.increment();
				}
			};

		actionableDynamicQuery.performActions();

		Assert.assertEquals(count.getValue(), _persistence.countAll());
	}

	@Test
	public void testDynamicQueryByPrimaryKeyExisting()
		throws Exception {
		PollsVote newPollsVote = addPollsVote();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(PollsVote.class,
				PollsVote.class.getClassLoader());

		dynamicQuery.add(RestrictionsFactoryUtil.eq("voteId",
				newPollsVote.getVoteId()));

		List<PollsVote> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(1, result.size());

		PollsVote existingPollsVote = result.get(0);

		Assert.assertEquals(existingPollsVote, newPollsVote);
	}

	@Test
	public void testDynamicQueryByPrimaryKeyMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(PollsVote.class,
				PollsVote.class.getClassLoader());

		dynamicQuery.add(RestrictionsFactoryUtil.eq("voteId",
				ServiceTestUtil.nextLong()));

		List<PollsVote> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testDynamicQueryByProjectionExisting()
		throws Exception {
		PollsVote newPollsVote = addPollsVote();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(PollsVote.class,
				PollsVote.class.getClassLoader());

		dynamicQuery.setProjection(ProjectionFactoryUtil.property("voteId"));

		Object newVoteId = newPollsVote.getVoteId();

		dynamicQuery.add(RestrictionsFactoryUtil.in("voteId",
				new Object[] { newVoteId }));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(1, result.size());

		Object existingVoteId = result.get(0);

		Assert.assertEquals(existingVoteId, newVoteId);
	}

	@Test
	public void testDynamicQueryByProjectionMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(PollsVote.class,
				PollsVote.class.getClassLoader());

		dynamicQuery.setProjection(ProjectionFactoryUtil.property("voteId"));

		dynamicQuery.add(RestrictionsFactoryUtil.in("voteId",
				new Object[] { ServiceTestUtil.nextLong() }));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testResetOriginalValues() throws Exception {
		if (!PropsValues.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE) {
			return;
		}

		PollsVote newPollsVote = addPollsVote();

		_persistence.clearCache();

		PollsVoteModelImpl existingPollsVoteModelImpl = (PollsVoteModelImpl)_persistence.findByPrimaryKey(newPollsVote.getPrimaryKey());

		Assert.assertTrue(Validator.equals(
				existingPollsVoteModelImpl.getUuid(),
				existingPollsVoteModelImpl.getOriginalUuid()));
		Assert.assertEquals(existingPollsVoteModelImpl.getGroupId(),
			existingPollsVoteModelImpl.getOriginalGroupId());

		Assert.assertEquals(existingPollsVoteModelImpl.getQuestionId(),
			existingPollsVoteModelImpl.getOriginalQuestionId());
		Assert.assertEquals(existingPollsVoteModelImpl.getUserId(),
			existingPollsVoteModelImpl.getOriginalUserId());
	}

	protected PollsVote addPollsVote() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		PollsVote pollsVote = _persistence.create(pk);

		pollsVote.setUuid(ServiceTestUtil.randomString());

		pollsVote.setGroupId(ServiceTestUtil.nextLong());

		pollsVote.setCompanyId(ServiceTestUtil.nextLong());

		pollsVote.setUserId(ServiceTestUtil.nextLong());

		pollsVote.setUserName(ServiceTestUtil.randomString());

		pollsVote.setCreateDate(ServiceTestUtil.nextDate());

		pollsVote.setModifiedDate(ServiceTestUtil.nextDate());

		pollsVote.setQuestionId(ServiceTestUtil.nextLong());

		pollsVote.setChoiceId(ServiceTestUtil.nextLong());

		pollsVote.setVoteDate(ServiceTestUtil.nextDate());

		_pollsVotes.add(_persistence.update(pollsVote));

		return pollsVote;
	}

	private static Log _log = LogFactoryUtil.getLog(PollsVotePersistenceTest.class);
	private List<PollsVote> _pollsVotes = new ArrayList<PollsVote>();
	private ModelListener<PollsVote>[] _listeners;
	private PollsVotePersistence _persistence = (PollsVotePersistence)PortalBeanLocatorUtil.locate(PollsVotePersistence.class.getName());
}