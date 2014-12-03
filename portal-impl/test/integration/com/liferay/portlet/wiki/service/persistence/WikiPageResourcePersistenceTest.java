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

package com.liferay.portlet.wiki.service.persistence;

import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.test.ExecutionTestListeners;
import com.liferay.portal.kernel.util.IntegerWrapper;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.OrderByComparatorFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.ServiceTestUtil;
import com.liferay.portal.service.persistence.PersistenceExecutionTestListener;
import com.liferay.portal.test.LiferayIntegrationJUnitTestRunner;
import com.liferay.portal.util.PropsValues;

import com.liferay.portlet.wiki.NoSuchPageResourceException;
import com.liferay.portlet.wiki.model.WikiPageResource;
import com.liferay.portlet.wiki.model.impl.WikiPageResourceModelImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Brian Wing Shun Chan
 */
@ExecutionTestListeners(listeners =  {
	PersistenceExecutionTestListener.class})
@RunWith(LiferayIntegrationJUnitTestRunner.class)
public class WikiPageResourcePersistenceTest {
	@After
	public void tearDown() throws Exception {
		Iterator<WikiPageResource> iterator = _wikiPageResources.iterator();

		while (iterator.hasNext()) {
			_persistence.remove(iterator.next());

			iterator.remove();
		}
	}

	@Test
	public void testCreate() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		WikiPageResource wikiPageResource = _persistence.create(pk);

		Assert.assertNotNull(wikiPageResource);

		Assert.assertEquals(wikiPageResource.getPrimaryKey(), pk);
	}

	@Test
	public void testRemove() throws Exception {
		WikiPageResource newWikiPageResource = addWikiPageResource();

		_persistence.remove(newWikiPageResource);

		WikiPageResource existingWikiPageResource = _persistence.fetchByPrimaryKey(newWikiPageResource.getPrimaryKey());

		Assert.assertNull(existingWikiPageResource);
	}

	@Test
	public void testUpdateNew() throws Exception {
		addWikiPageResource();
	}

	@Test
	public void testUpdateExisting() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		WikiPageResource newWikiPageResource = _persistence.create(pk);

		newWikiPageResource.setUuid(ServiceTestUtil.randomString());

		newWikiPageResource.setNodeId(ServiceTestUtil.nextLong());

		newWikiPageResource.setTitle(ServiceTestUtil.randomString());

		_wikiPageResources.add(_persistence.update(newWikiPageResource));

		WikiPageResource existingWikiPageResource = _persistence.findByPrimaryKey(newWikiPageResource.getPrimaryKey());

		Assert.assertEquals(existingWikiPageResource.getUuid(),
			newWikiPageResource.getUuid());
		Assert.assertEquals(existingWikiPageResource.getResourcePrimKey(),
			newWikiPageResource.getResourcePrimKey());
		Assert.assertEquals(existingWikiPageResource.getNodeId(),
			newWikiPageResource.getNodeId());
		Assert.assertEquals(existingWikiPageResource.getTitle(),
			newWikiPageResource.getTitle());
	}

	@Test
	public void testFindByPrimaryKeyExisting() throws Exception {
		WikiPageResource newWikiPageResource = addWikiPageResource();

		WikiPageResource existingWikiPageResource = _persistence.findByPrimaryKey(newWikiPageResource.getPrimaryKey());

		Assert.assertEquals(existingWikiPageResource, newWikiPageResource);
	}

	@Test
	public void testFindByPrimaryKeyMissing() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		try {
			_persistence.findByPrimaryKey(pk);

			Assert.fail(
				"Missing entity did not throw NoSuchPageResourceException");
		}
		catch (NoSuchPageResourceException nsee) {
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
		return OrderByComparatorFactoryUtil.create("WikiPageResource", "uuid",
			true, "resourcePrimKey", true, "nodeId", true, "title", true);
	}

	@Test
	public void testFetchByPrimaryKeyExisting() throws Exception {
		WikiPageResource newWikiPageResource = addWikiPageResource();

		WikiPageResource existingWikiPageResource = _persistence.fetchByPrimaryKey(newWikiPageResource.getPrimaryKey());

		Assert.assertEquals(existingWikiPageResource, newWikiPageResource);
	}

	@Test
	public void testFetchByPrimaryKeyMissing() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		WikiPageResource missingWikiPageResource = _persistence.fetchByPrimaryKey(pk);

		Assert.assertNull(missingWikiPageResource);
	}

	@Test
	public void testActionableDynamicQuery() throws Exception {
		final IntegerWrapper count = new IntegerWrapper();

		ActionableDynamicQuery actionableDynamicQuery = new WikiPageResourceActionableDynamicQuery() {
				@Override
				protected void performAction(Object object) {
					WikiPageResource wikiPageResource = (WikiPageResource)object;

					Assert.assertNotNull(wikiPageResource);

					count.increment();
				}
			};

		actionableDynamicQuery.performActions();

		Assert.assertEquals(count.getValue(), _persistence.countAll());
	}

	@Test
	public void testDynamicQueryByPrimaryKeyExisting()
		throws Exception {
		WikiPageResource newWikiPageResource = addWikiPageResource();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(WikiPageResource.class,
				WikiPageResource.class.getClassLoader());

		dynamicQuery.add(RestrictionsFactoryUtil.eq("resourcePrimKey",
				newWikiPageResource.getResourcePrimKey()));

		List<WikiPageResource> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(1, result.size());

		WikiPageResource existingWikiPageResource = result.get(0);

		Assert.assertEquals(existingWikiPageResource, newWikiPageResource);
	}

	@Test
	public void testDynamicQueryByPrimaryKeyMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(WikiPageResource.class,
				WikiPageResource.class.getClassLoader());

		dynamicQuery.add(RestrictionsFactoryUtil.eq("resourcePrimKey",
				ServiceTestUtil.nextLong()));

		List<WikiPageResource> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testDynamicQueryByProjectionExisting()
		throws Exception {
		WikiPageResource newWikiPageResource = addWikiPageResource();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(WikiPageResource.class,
				WikiPageResource.class.getClassLoader());

		dynamicQuery.setProjection(ProjectionFactoryUtil.property(
				"resourcePrimKey"));

		Object newResourcePrimKey = newWikiPageResource.getResourcePrimKey();

		dynamicQuery.add(RestrictionsFactoryUtil.in("resourcePrimKey",
				new Object[] { newResourcePrimKey }));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(1, result.size());

		Object existingResourcePrimKey = result.get(0);

		Assert.assertEquals(existingResourcePrimKey, newResourcePrimKey);
	}

	@Test
	public void testDynamicQueryByProjectionMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(WikiPageResource.class,
				WikiPageResource.class.getClassLoader());

		dynamicQuery.setProjection(ProjectionFactoryUtil.property(
				"resourcePrimKey"));

		dynamicQuery.add(RestrictionsFactoryUtil.in("resourcePrimKey",
				new Object[] { ServiceTestUtil.nextLong() }));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testResetOriginalValues() throws Exception {
		if (!PropsValues.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE) {
			return;
		}

		WikiPageResource newWikiPageResource = addWikiPageResource();

		_persistence.clearCache();

		WikiPageResourceModelImpl existingWikiPageResourceModelImpl = (WikiPageResourceModelImpl)_persistence.findByPrimaryKey(newWikiPageResource.getPrimaryKey());

		Assert.assertEquals(existingWikiPageResourceModelImpl.getNodeId(),
			existingWikiPageResourceModelImpl.getOriginalNodeId());
		Assert.assertTrue(Validator.equals(
				existingWikiPageResourceModelImpl.getTitle(),
				existingWikiPageResourceModelImpl.getOriginalTitle()));
	}

	protected WikiPageResource addWikiPageResource() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		WikiPageResource wikiPageResource = _persistence.create(pk);

		wikiPageResource.setUuid(ServiceTestUtil.randomString());

		wikiPageResource.setNodeId(ServiceTestUtil.nextLong());

		wikiPageResource.setTitle(ServiceTestUtil.randomString());

		_wikiPageResources.add(_persistence.update(wikiPageResource));

		return wikiPageResource;
	}

	private static Log _log = LogFactoryUtil.getLog(WikiPageResourcePersistenceTest.class);
	private List<WikiPageResource> _wikiPageResources = new ArrayList<WikiPageResource>();
	private WikiPageResourcePersistence _persistence = (WikiPageResourcePersistence)PortalBeanLocatorUtil.locate(WikiPageResourcePersistence.class.getName());
}