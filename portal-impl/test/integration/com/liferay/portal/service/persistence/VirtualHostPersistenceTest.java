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

package com.liferay.portal.service.persistence;

import com.liferay.portal.NoSuchVirtualHostException;
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
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.model.VirtualHost;
import com.liferay.portal.model.impl.VirtualHostModelImpl;
import com.liferay.portal.service.ServiceTestUtil;
import com.liferay.portal.test.LiferayIntegrationJUnitTestRunner;
import com.liferay.portal.util.PropsValues;

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
public class VirtualHostPersistenceTest {
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

		for (ModelListener<VirtualHost> modelListener : _listeners) {
			_persistence.unregisterListener(modelListener);
		}
	}

	@After
	public void tearDown() throws Exception {
		Iterator<VirtualHost> iterator = _virtualHosts.iterator();

		while (iterator.hasNext()) {
			_persistence.remove(iterator.next());

			iterator.remove();
		}

		for (ModelListener<VirtualHost> modelListener : _listeners) {
			_persistence.registerListener(modelListener);
		}
	}

	@Test
	public void testCreate() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		VirtualHost virtualHost = _persistence.create(pk);

		Assert.assertNotNull(virtualHost);

		Assert.assertEquals(virtualHost.getPrimaryKey(), pk);
	}

	@Test
	public void testRemove() throws Exception {
		VirtualHost newVirtualHost = addVirtualHost();

		_persistence.remove(newVirtualHost);

		VirtualHost existingVirtualHost = _persistence.fetchByPrimaryKey(newVirtualHost.getPrimaryKey());

		Assert.assertNull(existingVirtualHost);
	}

	@Test
	public void testUpdateNew() throws Exception {
		addVirtualHost();
	}

	@Test
	public void testUpdateExisting() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		VirtualHost newVirtualHost = _persistence.create(pk);

		newVirtualHost.setCompanyId(ServiceTestUtil.nextLong());

		newVirtualHost.setLayoutSetId(ServiceTestUtil.nextLong());

		newVirtualHost.setHostname(ServiceTestUtil.randomString());

		_virtualHosts.add(_persistence.update(newVirtualHost));

		VirtualHost existingVirtualHost = _persistence.findByPrimaryKey(newVirtualHost.getPrimaryKey());

		Assert.assertEquals(existingVirtualHost.getVirtualHostId(),
			newVirtualHost.getVirtualHostId());
		Assert.assertEquals(existingVirtualHost.getCompanyId(),
			newVirtualHost.getCompanyId());
		Assert.assertEquals(existingVirtualHost.getLayoutSetId(),
			newVirtualHost.getLayoutSetId());
		Assert.assertEquals(existingVirtualHost.getHostname(),
			newVirtualHost.getHostname());
	}

	@Test
	public void testFindByPrimaryKeyExisting() throws Exception {
		VirtualHost newVirtualHost = addVirtualHost();

		VirtualHost existingVirtualHost = _persistence.findByPrimaryKey(newVirtualHost.getPrimaryKey());

		Assert.assertEquals(existingVirtualHost, newVirtualHost);
	}

	@Test
	public void testFindByPrimaryKeyMissing() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		try {
			_persistence.findByPrimaryKey(pk);

			Assert.fail(
				"Missing entity did not throw NoSuchVirtualHostException");
		}
		catch (NoSuchVirtualHostException nsee) {
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
		return OrderByComparatorFactoryUtil.create("VirtualHost",
			"virtualHostId", true, "companyId", true, "layoutSetId", true,
			"hostname", true);
	}

	@Test
	public void testFetchByPrimaryKeyExisting() throws Exception {
		VirtualHost newVirtualHost = addVirtualHost();

		VirtualHost existingVirtualHost = _persistence.fetchByPrimaryKey(newVirtualHost.getPrimaryKey());

		Assert.assertEquals(existingVirtualHost, newVirtualHost);
	}

	@Test
	public void testFetchByPrimaryKeyMissing() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		VirtualHost missingVirtualHost = _persistence.fetchByPrimaryKey(pk);

		Assert.assertNull(missingVirtualHost);
	}

	@Test
	public void testActionableDynamicQuery() throws Exception {
		final IntegerWrapper count = new IntegerWrapper();

		ActionableDynamicQuery actionableDynamicQuery = new VirtualHostActionableDynamicQuery() {
				@Override
				protected void performAction(Object object) {
					VirtualHost virtualHost = (VirtualHost)object;

					Assert.assertNotNull(virtualHost);

					count.increment();
				}
			};

		actionableDynamicQuery.performActions();

		Assert.assertEquals(count.getValue(), _persistence.countAll());
	}

	@Test
	public void testDynamicQueryByPrimaryKeyExisting()
		throws Exception {
		VirtualHost newVirtualHost = addVirtualHost();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(VirtualHost.class,
				VirtualHost.class.getClassLoader());

		dynamicQuery.add(RestrictionsFactoryUtil.eq("virtualHostId",
				newVirtualHost.getVirtualHostId()));

		List<VirtualHost> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(1, result.size());

		VirtualHost existingVirtualHost = result.get(0);

		Assert.assertEquals(existingVirtualHost, newVirtualHost);
	}

	@Test
	public void testDynamicQueryByPrimaryKeyMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(VirtualHost.class,
				VirtualHost.class.getClassLoader());

		dynamicQuery.add(RestrictionsFactoryUtil.eq("virtualHostId",
				ServiceTestUtil.nextLong()));

		List<VirtualHost> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testDynamicQueryByProjectionExisting()
		throws Exception {
		VirtualHost newVirtualHost = addVirtualHost();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(VirtualHost.class,
				VirtualHost.class.getClassLoader());

		dynamicQuery.setProjection(ProjectionFactoryUtil.property(
				"virtualHostId"));

		Object newVirtualHostId = newVirtualHost.getVirtualHostId();

		dynamicQuery.add(RestrictionsFactoryUtil.in("virtualHostId",
				new Object[] { newVirtualHostId }));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(1, result.size());

		Object existingVirtualHostId = result.get(0);

		Assert.assertEquals(existingVirtualHostId, newVirtualHostId);
	}

	@Test
	public void testDynamicQueryByProjectionMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(VirtualHost.class,
				VirtualHost.class.getClassLoader());

		dynamicQuery.setProjection(ProjectionFactoryUtil.property(
				"virtualHostId"));

		dynamicQuery.add(RestrictionsFactoryUtil.in("virtualHostId",
				new Object[] { ServiceTestUtil.nextLong() }));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testResetOriginalValues() throws Exception {
		if (!PropsValues.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE) {
			return;
		}

		VirtualHost newVirtualHost = addVirtualHost();

		_persistence.clearCache();

		VirtualHostModelImpl existingVirtualHostModelImpl = (VirtualHostModelImpl)_persistence.findByPrimaryKey(newVirtualHost.getPrimaryKey());

		Assert.assertTrue(Validator.equals(
				existingVirtualHostModelImpl.getHostname(),
				existingVirtualHostModelImpl.getOriginalHostname()));

		Assert.assertEquals(existingVirtualHostModelImpl.getCompanyId(),
			existingVirtualHostModelImpl.getOriginalCompanyId());
		Assert.assertEquals(existingVirtualHostModelImpl.getLayoutSetId(),
			existingVirtualHostModelImpl.getOriginalLayoutSetId());
	}

	protected VirtualHost addVirtualHost() throws Exception {
		long pk = ServiceTestUtil.nextLong();

		VirtualHost virtualHost = _persistence.create(pk);

		virtualHost.setCompanyId(ServiceTestUtil.nextLong());

		virtualHost.setLayoutSetId(ServiceTestUtil.nextLong());

		virtualHost.setHostname(ServiceTestUtil.randomString());

		_virtualHosts.add(_persistence.update(virtualHost));

		return virtualHost;
	}

	private static Log _log = LogFactoryUtil.getLog(VirtualHostPersistenceTest.class);
	private List<VirtualHost> _virtualHosts = new ArrayList<VirtualHost>();
	private ModelListener<VirtualHost>[] _listeners;
	private VirtualHostPersistence _persistence = (VirtualHostPersistence)PortalBeanLocatorUtil.locate(VirtualHostPersistence.class.getName());
}