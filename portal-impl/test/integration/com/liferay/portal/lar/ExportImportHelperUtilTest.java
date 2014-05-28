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

package com.liferay.portal.lar;

import com.liferay.portal.kernel.dao.orm.FinderCacheUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.lar.ExportImportHelperUtil;
import com.liferay.portal.kernel.lar.ExportImportPathUtil;
import com.liferay.portal.kernel.lar.MissingReference;
import com.liferay.portal.kernel.lar.MissingReferences;
import com.liferay.portal.kernel.lar.PortletDataContext;
import com.liferay.portal.kernel.lar.PortletDataContextFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.test.ExecutionTestListeners;
import com.liferay.portal.kernel.transaction.Transactional;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.kernel.zip.ZipWriterFactoryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.StagedModel;
import com.liferay.portal.model.User;
import com.liferay.portal.repository.liferayrepository.model.LiferayFileEntry;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ServiceTestUtil;
import com.liferay.portal.test.LiferayIntegrationJUnitTestRunner;
import com.liferay.portal.test.MainServletExecutionTestListener;
import com.liferay.portal.test.Sync;
import com.liferay.portal.test.SynchronousDestinationExecutionTestListener;
import com.liferay.portal.test.TransactionalExecutionTestListener;
import com.liferay.portal.util.GroupTestUtil;
import com.liferay.portal.util.LayoutTestUtil;
import com.liferay.portal.util.PortalImpl;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsValues;
import com.liferay.portal.util.TestPropsValues;
import com.liferay.portal.zip.ZipReaderImpl;
import com.liferay.portal.zip.ZipWriterImpl;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.util.DLAppTestUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.util.JournalTestUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.api.mockito.PowerMockito;

/**
 * @author Zsolt Berentey
 * @author Peter Borkuti
 */
@ExecutionTestListeners(
	listeners = {
		MainServletExecutionTestListener.class,
		SynchronousDestinationExecutionTestListener.class,
		TransactionalExecutionTestListener.class
	})
@RunWith(LiferayIntegrationJUnitTestRunner.class)
@Sync
@Transactional
public class ExportImportHelperUtilTest extends PowerMockito {

	@Before
	public void setUp() throws Exception {
		FinderCacheUtil.clearCache();

		_liveGroup = GroupTestUtil.addGroup();
		_stagingGroup = GroupTestUtil.addGroup();

		_fileEntry = DLAppTestUtil.addFileEntry(
			_stagingGroup.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			ServiceTestUtil.randomString() + ".txt",
			ServiceTestUtil.randomString(), true);

		LiferayFileEntry liferayFileEntry = (LiferayFileEntry)_fileEntry;

		DLFileEntry dlFileEntry = liferayFileEntry.getDLFileEntry();

		dlFileEntry.setLargeImageId(dlFileEntry.getFileEntryId());

		DLFileEntryLocalServiceUtil.updateDLFileEntry(dlFileEntry);

		_zipWriter = new TestZipWriter();

		_portletDataContextExport =
			PortletDataContextFactoryUtil.createExportPortletDataContext(
				_stagingGroup.getCompanyId(), _stagingGroup.getGroupId(),
				new HashMap<String, String[]>(),
				new Date(System.currentTimeMillis() - Time.HOUR), new Date(),
				_zipWriter);

		Element rootElement = SAXReaderUtil.createElement("root");

		_portletDataContextExport.setExportDataRootElement(rootElement);

		_stagingPrivateLayout = LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString(), true);
		_stagingPublicLayout = LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString(), false);

		_portletDataContextExport.setPlid(_stagingPublicLayout.getPlid());

		_zipReader = new TestZipReader(
			_zipWriter.getFile(), _zipWriter.getBinaryEntriesList(),
			_zipWriter.getEntriesMap());

		_portletDataContextImport =
			PortletDataContextFactoryUtil.createImportPortletDataContext(
				_liveGroup.getCompanyId(), _liveGroup.getGroupId(),
				new HashMap<String, String[]>(),
				new CurrentUserIdStrategy(TestPropsValues.getUser()),
				_zipReader);

		_portletDataContextImport.setImportDataRootElement(rootElement);

		Element missingReferencesElement = rootElement.addElement(
			"missing-references");

		_portletDataContextImport.setMissingReferencesElement(
			missingReferencesElement);

		_livePublicLayout = LayoutTestUtil.addLayout(
			_liveGroup.getGroupId(), ServiceTestUtil.randomString(), false);

		_portletDataContextImport.setPlid(_livePublicLayout.getPlid());

		_portletDataContextImport.setSourceGroupId(_stagingGroup.getGroupId());

		rootElement.addElement("entry");

		_referrerStagedModel = JournalTestUtil.addArticle(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString(),
			ServiceTestUtil.randomString());

		_referrerStagedModelElement =
			_portletDataContextExport.getExportDataElement(
				_referrerStagedModel);

		String referrerStagedModelPath = ExportImportPathUtil.getModelPath(
			_referrerStagedModel);

		_portletDataContextExport.addZipEntry(
			referrerStagedModelPath, _referrerStagedModel);

		_portletDataContextImport.setImportDataRootElement(rootElement);

		_referrerStagedModelElement.addAttribute(
			"path", referrerStagedModelPath);
	}

	@After
	public void tearDown() throws Exception {
		GroupLocalServiceUtil.deleteGroup(_liveGroup);
		GroupLocalServiceUtil.deleteGroup(_stagingGroup);
	}

	@Test
	public void testDeleteTimestampFromDLReferenceURLs() throws Exception {
		Element rootElement =
			_portletDataContextExport.getExportDataRootElement();

		String content = replaceParameters(
			getContent("dl_references.txt"), _fileEntry);

		List<String> urls = getURLs(content);

		String urlContent = StringUtil.merge(urls, StringPool.NEW_LINE);

		content = ExportImportHelperUtil.replaceExportContentReferences(
			_portletDataContextExport, _referrerStagedModel,
			rootElement.element("entry"), urlContent, true);

		String[] exportedURLs = content.split(StringPool.NEW_LINE);

		Assert.assertEquals(urls.size(), exportedURLs.length);

		for (int i = 0; i < urls.size(); i++) {
			String exportedUrl = exportedURLs[i];
			String url = urls.get(i);

			Assert.assertFalse(exportedUrl.matches("[?&]t="));

			if (url.contains("/documents/") && url.contains("?")) {
				Assert.assertTrue(exportedUrl.contains("width=100&height=100"));
			}

			if (url.contains("/documents/") && url.contains("mustkeep")) {
				Assert.assertTrue(exportedUrl.contains("mustkeep"));
			}
		}
	}

	@Test
	public void testExportDLReferences() throws Exception {
		Element rootElement =
			_portletDataContextExport.getExportDataRootElement();

		String content = replaceParameters(
			getContent("dl_references.txt"), _fileEntry);

		List<String> urls = getURLs(content);

		_zipWriter.clearPreviousEntries();

		content = ExportImportHelperUtil.replaceExportContentReferences(
			_portletDataContextExport, _referrerStagedModel,
			rootElement.element("entry"), content, true);

		for (String url : urls) {
			Assert.assertFalse(content.contains(url));
		}

		List<String> entries = _zipReader.getEntries();

		Assert.assertEquals(entries.size(), 1);

		List<String> binaryEntries = _zipReader.getBinaryEntries();

		Assert.assertEquals(binaryEntries.size(), entries.size());

		for (String entry : _zipReader.getEntries()) {
			Assert.assertTrue(
				content.contains("[$dl-reference=" + entry + "$]"));
		};
	}

	@Test
	public void testExportDLReferencesInvalidReference() throws Exception {
		_portletDataContextExport.setZipWriter(new TestReaderWriter());

		StringBundler sb = new StringBundler(9);

		sb.append("{{/documents/}}");
		sb.append(StringPool.NEW_LINE);
		sb.append("[[/documents/]]");
		sb.append(StringPool.NEW_LINE);
		sb.append("<a href=/documents/>Link</a>");
		sb.append(StringPool.NEW_LINE);
		sb.append("<a href=\"/documents/\">Link</a>");
		sb.append(StringPool.NEW_LINE);
		sb.append("<a href='/documents/'>Link</a>");

		ExportImportHelperUtil.replaceExportDLReferences(
			_portletDataContextExport, _referrerStagedModel, sb.toString(),
			true);
	}

	@Test
	public void testExportLayoutReferencesWithContext() throws Exception {
		PortalImpl portalImpl = spy(new PortalImpl());

		when(
			portalImpl.getPathContext()
		).thenReturn(
			"/de"
		);

		PortalUtil portalUtil = new PortalUtil();

		portalUtil.setPortal(portalImpl);

		_OLD_LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING =
			PropsValues.LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING;

		setFinalStaticField(
			PropsValues.class.getField(
				"LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING"),
			"/en");

		setFinalStaticField(
			ExportImportHelperImpl.class.getDeclaredField(
				"_PRIVATE_USER_SERVLET_MAPPING"),
			"/en/");

		Element rootElement =
			_portletDataContextExport.getExportDataRootElement();

		String content = replaceParameters(
			getContent("layout_references.txt"), _fileEntry);

		content = ExportImportHelperUtil.replaceExportContentReferences(
			_portletDataContextExport, _referrerStagedModel,
			rootElement.element("entry"), content, true);

		Assert.assertFalse(
			content.contains(
				PropsValues.LAYOUT_FRIENDLY_URL_PRIVATE_GROUP_SERVLET_MAPPING));
		Assert.assertFalse(
			content.contains(
				PropsValues.LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING));
		Assert.assertFalse(content.contains(_stagingGroup.getFriendlyURL()));
		Assert.assertFalse(content.contains(PortalUtil.getPathContext()));
		Assert.assertFalse(content.contains("/en/en"));

		setFinalStaticField(
			PropsValues.class.getDeclaredField(
				"LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING"),
			_OLD_LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING);

		setFinalStaticField(
			ExportImportHelperImpl.class.getDeclaredField(
				"_PRIVATE_USER_SERVLET_MAPPING"),
			PropsValues.LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING +
				StringPool.SLASH);

		portalUtil.setPortal(new PortalImpl());
	}

	@Test
	public void testExportLayoutReferencesWithoutContext() throws Exception {
		_OLD_LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING =
			PropsValues.LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING;

		setFinalStaticField(
			PropsValues.class.getField(
				"LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING"),
			"/en");

		setFinalStaticField(
			ExportImportHelperImpl.class.getDeclaredField(
				"_PRIVATE_USER_SERVLET_MAPPING"), "/en/");

		Element rootElement =
			_portletDataContextExport.getExportDataRootElement();

		String content = replaceParameters(
			getContent("layout_references.txt"), _fileEntry);

		content = ExportImportHelperUtil.replaceExportContentReferences(
			_portletDataContextExport, _referrerStagedModel,
			rootElement.element("entry"), content, true);

		Assert.assertFalse(
			content.contains(
				PropsValues.LAYOUT_FRIENDLY_URL_PRIVATE_GROUP_SERVLET_MAPPING));
		Assert.assertFalse(
			content.contains(
				PropsValues.LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING));
		Assert.assertFalse(content.contains(_stagingGroup.getFriendlyURL()));
		Assert.assertFalse(content.contains("/en/en"));

		setFinalStaticField(
			PropsValues.class.getDeclaredField(
				"LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING"),
			_OLD_LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING);

		setFinalStaticField(
			ExportImportHelperImpl.class.getDeclaredField(
				"_PRIVATE_USER_SERVLET_MAPPING"),
			PropsValues.LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING +
				StringPool.SLASH);
	}

	@Test
	public void testExportLinksToLayouts() throws Exception {
		Element rootElement =
			_portletDataContextExport.getExportDataRootElement();

		String content = replaceLinksToLayoutsParameters(
			getContent("layout_links.txt"), _stagingPrivateLayout,
			_stagingPublicLayout);

		content = ExportImportHelperUtil.replaceExportContentReferences(
			_portletDataContextExport, _referrerStagedModel,
			rootElement.element("entry"), content, true);

		assertLinksToLayouts(content, _stagingPrivateLayout, 0);
		assertLinksToLayouts(
			content, _stagingPrivateLayout, _stagingPrivateLayout.getGroupId());
		assertLinksToLayouts(content, _stagingPublicLayout, 0);
		assertLinksToLayouts(
			content, _stagingPublicLayout, _stagingPublicLayout.getGroupId());
	}

	@Test
	public void testExportLinksToUserLayouts() throws Exception {
		User user = TestPropsValues.getUser();

		Group group = user.getGroup();

		Layout privateLayout = LayoutTestUtil.addLayout(
			group.getGroupId(), ServiceTestUtil.randomString(), true);
		Layout publicLayout = LayoutTestUtil.addLayout(
			group.getGroupId(), ServiceTestUtil.randomString(), false);

		PortletDataContext portletDataContextExport =
			PortletDataContextFactoryUtil.createExportPortletDataContext(
				group.getCompanyId(), group.getGroupId(),
				new HashMap<String, String[]>(),
				new Date(System.currentTimeMillis() - Time.HOUR), new Date(),
				_zipWriter);

		JournalArticle journalArticle = JournalTestUtil.addArticle(
			group.getGroupId(), ServiceTestUtil.randomString(),
			ServiceTestUtil.randomString());

		Element rootElement = SAXReaderUtil.createElement("root");

		rootElement.addElement("entry");

		String content = replaceLinksToLayoutsParameters(
			getContent("layout_links_user_group.txt"), privateLayout,
			publicLayout);

		content = ExportImportHelperUtil.replaceExportContentReferences(
			portletDataContextExport, journalArticle,
			rootElement.element("entry"), content, true);

		assertLinksToLayouts(content, privateLayout, 0);
		assertLinksToLayouts(
			content, privateLayout, privateLayout.getGroupId());
		assertLinksToLayouts(content, publicLayout, 0);
		assertLinksToLayouts(content, publicLayout, publicLayout.getGroupId());
	}

	@Test
	public void testGetSelectedLayoutsJSONSelectAllLayouts() throws Exception {
		Layout layout = LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString());

		Layout childLayout = LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString(),
			layout.getPlid());

		long[] selectedLayoutIds = new long[] {
			layout.getLayoutId(), childLayout.getLayoutId()};

		String selectedLayoutsJSON =
			ExportImportHelperUtil.getSelectedLayoutsJSON(
				_stagingGroup.getGroupId(), false,
				StringUtil.merge(selectedLayoutIds));

		JSONArray selectedLayoutsJSONArray = JSONFactoryUtil.createJSONArray(
			selectedLayoutsJSON);

		Assert.assertEquals(1, selectedLayoutsJSONArray.length());

		JSONObject layoutJSONObject = selectedLayoutsJSONArray.getJSONObject(0);

		Assert.assertTrue(layoutJSONObject.getBoolean("includeChildren"));
		Assert.assertEquals(layout.getPlid(), layoutJSONObject.getLong("plid"));
	}

	@Test
	public void testGetSelectedLayoutsJSONSelectChildLayout() throws Exception {
		Layout layout = LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString());

		Layout childLayout = LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString(),
			layout.getPlid());

		long[] selectedLayoutIds = new long[] {childLayout.getLayoutId()};

		String selectedLayoutsJSON =
			ExportImportHelperUtil.getSelectedLayoutsJSON(
				_stagingGroup.getGroupId(), false,
				StringUtil.merge(selectedLayoutIds));

		JSONArray selectedLayoutsJSONArray = JSONFactoryUtil.createJSONArray(
			selectedLayoutsJSON);

		Assert.assertEquals(1, selectedLayoutsJSONArray.length());

		JSONObject layoutJSONObject = selectedLayoutsJSONArray.getJSONObject(0);

		Assert.assertTrue(layoutJSONObject.getBoolean("includeChildren"));
		Assert.assertEquals(
			childLayout.getPlid(), layoutJSONObject.getLong("plid"));
	}

	@Test
	public void testGetSelectedLayoutsJSONSelectNoLayouts() throws Exception {
		Layout layout = LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString());

		LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString(),
			layout.getPlid());

		String selectedLayoutsJSON =
			ExportImportHelperUtil.getSelectedLayoutsJSON(
				_stagingGroup.getGroupId(), false,
				StringUtil.merge(new long[0]));

		JSONArray selectedLayoutsJSONArray = JSONFactoryUtil.createJSONArray(
			selectedLayoutsJSON);

		Assert.assertEquals(0, selectedLayoutsJSONArray.length());
	}

	@Test
	public void testGetSelectedLayoutsJSONSelectParentLayout()
		throws Exception {

		Layout layout = LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString());

		LayoutTestUtil.addLayout(
			_stagingGroup.getGroupId(), ServiceTestUtil.randomString(),
			layout.getPlid());

		long[] selectedLayoutIds = new long[] {layout.getLayoutId()};

		String selectedLayoutsJSON =
			ExportImportHelperUtil.getSelectedLayoutsJSON(
				_stagingGroup.getGroupId(), false,
				StringUtil.merge(selectedLayoutIds));

		JSONArray selectedLayoutsJSONArray = JSONFactoryUtil.createJSONArray(
			selectedLayoutsJSON);

		Assert.assertEquals(1, selectedLayoutsJSONArray.length());

		JSONObject layoutJSONObject = selectedLayoutsJSONArray.getJSONObject(0);

		Assert.assertFalse(layoutJSONObject.getBoolean("includeChildren"));
		Assert.assertEquals(layout.getPlid(), layoutJSONObject.getLong("plid"));
	}

	@Test
	public void testImportDLReferences() throws Exception {
		String content = replaceParameters(
			getContent("dl_references.txt"), _fileEntry);

		content = ExportImportHelperUtil.replaceExportContentReferences(
			_portletDataContextExport, _referrerStagedModel,
			_referrerStagedModelElement, content, true);
		content = ExportImportHelperUtil.replaceImportContentReferences(
			_portletDataContextImport, _referrerStagedModelElement, content,
			true);

		Assert.assertFalse(content.contains("[$dl-reference="));
	}

	@Test
	public void testImportLayoutReferences() throws Exception {
		String content = replaceParameters(
			getContent("layout_references.txt"), _fileEntry);

		content = ExportImportHelperUtil.replaceExportContentReferences(
			_portletDataContextExport, _referrerStagedModel,
			_referrerStagedModelElement, content, true);
		content = ExportImportHelperUtil.replaceImportContentReferences(
			_portletDataContextImport, _referrerStagedModelElement, content,
			true);

		Assert.assertFalse(
			content.contains("@data_handler_group_friendly_url@"));
		Assert.assertFalse(content.contains("@data_handler_path_context@"));
		Assert.assertFalse(
			content.contains("@data_handler_private_group_servlet_mapping@"));
		Assert.assertFalse(
			content.contains("@data_handler_private_user_servlet_mapping@"));
		Assert.assertFalse(
			content.contains("@data_handler_public_servlet_mapping@"));
	}

	@Test
	public void testImportLinksToLayouts() throws Exception {
		Element rootElement =
			_portletDataContextImport.getImportDataRootElement();

		Element entryElement = rootElement.element("entry");

		String content = replaceLinksToLayoutsParameters(
			getContent("layout_links.txt"), _stagingPrivateLayout,
			_stagingPublicLayout);

		String originalContent = content;

		content = ExportImportHelperUtil.replaceExportContentReferences(
			_portletDataContextExport, _referrerStagedModel, entryElement,
			content, true);

		String importedContent =
			ExportImportHelperUtil.replaceImportContentReferences(
				_portletDataContextImport, _referrerStagedModelElement, content,
				true);

		Assert.assertEquals(originalContent, importedContent);
	}

	@Test
	public void testValidateMissingReferences() throws Exception {
		String xml = replaceParameters(
			getContent("missing_references.txt"), _fileEntry);

		ZipWriter zipWriter = ZipWriterFactoryUtil.getZipWriter();

		zipWriter.addEntry("/manifest.xml", xml);

		MissingReferences missingReferences =
			ExportImportHelperUtil.validateMissingReferences(
				TestPropsValues.getUserId(), _stagingGroup.getGroupId(),
				new HashMap<String, String[]>(), zipWriter.getFile());

		Map<String, MissingReference> dependencyMissingReferences =
			missingReferences.getDependencyMissingReferences();

		Map<String, MissingReference> weakMissingReferences =
			missingReferences.getWeakMissingReferences();

		Assert.assertEquals(2, dependencyMissingReferences.size());
		Assert.assertEquals(1, weakMissingReferences.size());

		FileUtil.delete(zipWriter.getFile());
	}

	protected void assertLinksToLayouts(
			String content, Layout layout, long groupId)
		throws SystemException {

		StringBundler sb = new StringBundler();

		sb.append(StringPool.OPEN_BRACKET);
		sb.append(layout.getLayoutId());
		sb.append(CharPool.AT);

		Group group = GroupLocalServiceUtil.fetchGroup(groupId);

		if (layout.isPrivateLayout()) {
			if (group == null) {
				sb.append("private");
			}
			else if (group.isUser()) {
				sb.append("private-user");
			}
			else {
				sb.append("private-group");
			}
		}
		else {
			sb.append("public");
		}

		sb.append(CharPool.AT);
		sb.append(layout.getUuid());
		sb.append(StringPool.AT);
		sb.append(layout.getFriendlyURL());

		if (group != null) {
			sb.append(CharPool.AT);
			sb.append(String.valueOf(groupId));
		}

		sb.append(StringPool.CLOSE_BRACKET);

		Assert.assertTrue(content.contains(sb.toString()));
	}

	protected String getContent(String fileName) throws Exception {
		Class<?> clazz = getClass();

		InputStream inputStream = clazz.getResourceAsStream(
			"dependencies/" + fileName);

		Scanner scanner = new Scanner(inputStream);

		scanner.useDelimiter("\\Z");

		return scanner.next();
	}

	protected List<String> getURLs(String content) {
		Pattern pattern = Pattern.compile("href=|\\{|\\[");

		Matcher matcher = pattern.matcher(StringPool.BLANK);

		String[] lines = StringUtil.split(content, StringPool.NEW_LINE);

		List<String> urls = new ArrayList<String>();

		for (String line : lines) {
			matcher.reset(line);

			if (matcher.find()) {
				urls.add(line);
			}
		}

		return urls;
	}

	protected String replaceLinksToLayoutsParameters(
		String content, Layout privateLayout, Layout publicLayout) {

		return StringUtil.replace(
			content,
			new String[] {
				"[$GROUP_ID_PRIVATE$]", "[$GROUP_ID_PUBLIC$]",
				"[$LAYOUT_ID_PRIVATE$]", "[$LAYOUT_ID_PUBLIC$]"
			},
			new String[] {
				String.valueOf(privateLayout.getGroupId()),
				String.valueOf(publicLayout.getGroupId()),
				String.valueOf(privateLayout.getLayoutId()),
				String.valueOf(publicLayout.getLayoutId())
			});
	}

	protected String replaceParameters(String content, FileEntry fileEntry) {
		content = StringUtil.replace(
			content,
			new String[] {
				"[$GROUP_FRIENDLY_URL$]", "[$GROUP_ID$]", "[$IMAGE_ID$]",
				"[$PATH_CONTEXT$]", "[$PATH_FRIENDLY_URL_PRIVATE_GROUP$]",
				"[$PATH_FRIENDLY_URL_PRIVATE_USER$]",
				"[$PATH_FRIENDLY_URL_PUBLIC$]", "[$TITLE$]", "[$UUID$]"
			},
			new String[] {
				_stagingGroup.getFriendlyURL(),
				String.valueOf(fileEntry.getGroupId()),
				String.valueOf(fileEntry.getFileEntryId()),
				PortalUtil.getPathContext(),
				PropsValues.LAYOUT_FRIENDLY_URL_PRIVATE_GROUP_SERVLET_MAPPING,
				PropsValues.LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING,
				PropsValues.LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING,
				fileEntry.getTitle(), fileEntry.getUuid()
			});

		if (!content.contains("[$TIMESTAMP")) {
			return content;
		}

		return replaceTimestampParameters(content);
	}

	protected String replaceTimestampParameters(String content) {
		List<String> urls = ListUtil.toList(StringUtil.splitLines(content));

		String timestampParameter = "t=123456789";

		String parameters1 = timestampParameter + "&width=100&height=100";
		String parameters2 = "width=100&" + timestampParameter + "&height=100";
		String parameters3 = "width=100&height=100&" + timestampParameter;
		String parameters4 =
			timestampParameter + "?" + timestampParameter +
				"&width=100&height=100";

		List<String> outURLs = new ArrayList<String>();

		for (String url : urls) {
			if (!url.contains("[$TIMESTAMP")) {
				continue;
			}

			outURLs.add(
				StringUtil.replace(
					url, new String[] {"[$TIMESTAMP$]", "[$TIMESTAMP_ONLY$]"},
					new String[] {"&" + parameters1, "?" + parameters1}));
			outURLs.add(
				StringUtil.replace(
					url, new String[] {"[$TIMESTAMP$]", "[$TIMESTAMP_ONLY$]"},
					new String[] {"&" + parameters2, "?" + parameters2}));
			outURLs.add(
				StringUtil.replace(
					url, new String[] {"[$TIMESTAMP$]", "[$TIMESTAMP_ONLY$]"},
					new String[] {"&" + parameters3, "?" + parameters3}));
			outURLs.add(
				StringUtil.replace(
					url, new String[] {"[$TIMESTAMP$]", "[$TIMESTAMP_ONLY$]"},
					new String[] {StringPool.BLANK, "?" + parameters4}));
		}

		return StringUtil.merge(outURLs, StringPool.NEW_LINE);
	}

	protected void setFinalStaticField(Field field, Object newValue)
		throws Exception {

		field.setAccessible(true);

		Field modifiersField = Field.class.getDeclaredField("modifiers");

		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

		field.set(null, newValue);
	}

	private static String _OLD_LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING;

	private FileEntry _fileEntry;
	private Group _liveGroup;
	private Layout _livePublicLayout;
	private PortletDataContext _portletDataContextExport;
	private PortletDataContext _portletDataContextImport;
	private StagedModel _referrerStagedModel;
	private Element _referrerStagedModelElement;
	private Group _stagingGroup;
	private Layout _stagingPrivateLayout;
	private Layout _stagingPublicLayout;
	private TestZipReader _zipReader;
	private TestZipWriter _zipWriter;

	private class TestZipReader extends ZipReaderImpl {

		public TestZipReader(
			File file, List<String> binaryEntries,
			Map<String, String> entries) {

			super(file);

			_binaryEntries = binaryEntries;

			_entries = entries;
		}

		public List<String> getBinaryEntries() {
			return _binaryEntries;
		}

		@Override
		public List<String> getEntries() {
			return new ArrayList<String>(_entries.keySet());
		}

		private List<String> _binaryEntries = new ArrayList<String>();
		private Map<String, String> _entries = new HashMap<String, String>();

	}

	private class TestZipWriter extends ZipWriterImpl {

		@Override
		public void addEntry(String name, InputStream inputStream)
			throws IOException {

			super.addEntry(name, inputStream);

			if (!name.contains(".xml")) {
				_binaryEntries.add(name);
			}
		}

		@Override
		public void addEntry(String name, String s) throws IOException {
			super.addEntry(name, s);

			_entries.put(name, s);
		}

		public List<String> getBinaryEntriesList() {
			return _binaryEntries;
		}

		public Map<String, String> getEntriesMap() {
			return _entries;
		}

		public void clearPreviousEntries() {
			_binaryEntries.clear();

			_entries.clear();
		}

		private List<String> _binaryEntries = new ArrayList<String>();
		private Map<String, String> _entries = new HashMap<String, String>();

	}

}