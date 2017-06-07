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

package com.liferay.adaptive.media.blogs.internal.exportimport.content.processor;

import com.liferay.adaptive.media.image.html.AdaptiveMediaImageHTMLTagFactory;
import com.liferay.blogs.kernel.model.BlogsEntry;
import com.liferay.document.library.kernel.exception.NoSuchFileEntryException;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.exportimport.content.processor.ExportImportContentProcessor;
import com.liferay.exportimport.kernel.lar.ExportImportPathUtil;
import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.StagedModel;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Adolfo Pérez
 */
@PrepareForTest({ExportImportPathUtil.class, DLUtil.class})
@RunWith(PowerMockRunner.class)
public class AdaptiveMediaBlogsEntryExportImportContentProcessorTest {

	@Before
	public void setUp() {
		_blogsExportImportContentProcessor.setDLAppLocalService(
			_dlAppLocalService);
		_blogsExportImportContentProcessor.setExportImportContentProcessor(
			_exportImportContentProcessor);
		_blogsExportImportContentProcessor.setAdaptiveMediaImageHTMLTagFactory(
			_adaptiveMediaImageHTMLTagFactory);
		_blogsExportImportContentProcessor.
			setAdaptiveMediaEmbeddedReferenceSetFactory(
				_adaptiveMediaEmbeddedReferenceSetFactory);

		Mockito.doReturn(
			_adaptiveMediaEmbeddedReferenceSet
		).when(
			_adaptiveMediaEmbeddedReferenceSetFactory
		).create(
			Mockito.any(PortletDataContext.class),
			Mockito.any(StagedModel.class)
		);

		PowerMockito.mockStatic(ExportImportPathUtil.class);
		PowerMockito.mockStatic(DLUtil.class);
	}

	@Test
	public void testExportContentWithDynamicReference() throws Exception {
		String prefix = StringUtil.randomString();
		String suffix = StringUtil.randomString();

		String content =
			prefix + "<img data-fileentryid=\"1\" src=\"url\" />" + suffix;

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToExport(1, _fileEntry1);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false);

		Assert.assertEquals(
			prefix + "<img src=\"url\" export-import-path=\"PATH_1\" />" +
				suffix,
			replacedContent);
	}

	@Test
	public void testExportContentWithDynamicReferenceContainingMoreAttributes()
		throws Exception {

		String prefix = StringUtil.randomString();
		String suffix = StringUtil.randomString();

		String content =
			prefix + "<img attr1=\"1\" data-fileentryid=\"1\" attr2=\"2\" " +
				"src=\"url\" attr3=\"3\"/>" + suffix;

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToExport(1, _fileEntry1);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false);

		Assert.assertEquals(
			prefix + "<img attr1=\"1\" attr2=\"2\" src=\"url\" attr3=\"3\" " +
				"export-import-path=\"PATH_1\" />" + suffix,
			replacedContent);
	}

	@Test
	public void testExportContentWithDynamicReferenceDoesNotEscape()
		throws Exception {

		String content = "&<img data-fileentryid=\"1\" src=\"url\" />&";

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToExport(1, _fileEntry1);

		Assert.assertEquals(
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false),
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, true));
	}

	@Test
	public void testExportContentWithMultipleDynamicReferences()
		throws Exception {

		String prefix = StringUtil.randomString();
		String infix = StringUtil.randomString();
		String suffix = StringUtil.randomString();

		String content =
			prefix + "<img data-fileentryid=\"1\" src=\"url1\" />" + infix +
				"<img data-fileentryid=\"2\" src=\"url2\" />" + suffix;

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToExport(1, _fileEntry1);
		_defineFileEntryToExport(2, _fileEntry2);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false);

		StringBundler sb = new StringBundler(4);

		sb.append(prefix);
		sb.append("<img src=\"url1\" export-import-path=\"PATH_1\" />");
		sb.append(infix);
		sb.append("<img src=\"url2\" export-import-path=\"PATH_2\" />");
		sb.append(suffix);

		Assert.assertEquals(sb.toString(), replacedContent);
	}

	@Test
	public void testExportContentWithMultipleStaticReferences()
		throws Exception {

		String prefix = StringUtil.randomString();
		String infix = StringUtil.randomString();
		String suffix = StringUtil.randomString();

		String content =
			prefix + "<picture data-fileentryid=\"1\"></picture>" + infix +
				"<picture data-fileentryid=\"2\"></picture>" + suffix;

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToExport(1, _fileEntry1);
		_defineFileEntryToExport(2, _fileEntry2);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false);

		StringBundler sb = new StringBundler();

		sb.append(prefix);
		sb.append("<picture export-import-path=\"PATH_1\"></picture>");
		sb.append(infix);
		sb.append("<picture export-import-path=\"PATH_2\"></picture>");
		sb.append(suffix);

		Assert.assertEquals(sb.toString(), replacedContent);
	}

	@Test
	public void testExportContentWithNoReferences() throws Exception {
		String content = StringUtil.randomString();

		_makeOverridenProcessorReturn(content);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false);

		Assert.assertEquals(content, replacedContent);
	}

	@Test
	public void testExportContentWithNoReferencesDoesNotEscape()
		throws Exception {

		String content = StringPool.AMPERSAND;

		_makeOverridenProcessorReturn(content);

		Assert.assertEquals(
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false),
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, Mockito.mock(BlogsEntry.class), content,
				false, true));
	}

	@Test
	public void testExportContentWithStaticReference() throws Exception {
		String prefix = StringUtil.randomString();
		String suffix = StringUtil.randomString();

		String content =
			prefix + "<picture data-fileentryid=\"1\"></picture>" + suffix;

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToExport(1, _fileEntry1);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false);

		Assert.assertEquals(
			prefix + "<picture export-import-path=\"PATH_1\"></picture>" +
				suffix,
			replacedContent);
	}

	@Test
	public void testExportContentWithStaticReferenceDoesNotEscape()
		throws Exception {

		String content = "&<picture data-fileentryid=\"1\"></picture>&";

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToExport(1, _fileEntry1);

		Assert.assertEquals(
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false),
			_blogsExportImportContentProcessor.replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, true));
	}

	@Test
	public void testImportContentIgnoresInvalidDynamicReferences()
		throws Exception {

		String content = "<img export-import-path=\"PATH_1\" />";

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToImport(1, _fileEntry1);

		Mockito.doThrow(
			PortalException.class
		).when(
			_dlAppLocalService
		).getFileEntry(
			Mockito.anyLong()
		);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceImportContentReferences(
				_portletDataContext, _blogsEntry, content);

		Assert.assertEquals(content, replacedContent);
	}

	@Test
	public void testImportContentIgnoresInvalidStaticReferences()
		throws Exception {

		String content = "<picture export-import-path=\"PATH_1\"></picture>";

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToImport(1, _fileEntry1);

		Mockito.doThrow(
			PortalException.class
		).when(
			_dlAppLocalService
		).getFileEntry(
			Mockito.anyLong()
		);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceImportContentReferences(
				_portletDataContext, _blogsEntry, content);

		Assert.assertEquals(content, replacedContent);
	}

	@Test
	public void testImportContentWithDynamicReference() throws Exception {
		String prefix = StringUtil.randomString();
		String suffix = StringUtil.randomString();

		String content =
			prefix + "<img export-import-path=\"PATH_1\" />" + suffix;

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToImport(1, _fileEntry1);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceImportContentReferences(
				_portletDataContext, _blogsEntry, content);

		Assert.assertEquals(
			prefix + "<img data-fileEntryId=\"1\" src=\"URL_1\" />" + suffix,
			replacedContent);
	}

	@Test
	public void testImportContentWithMultipleDynamicReferences()
		throws Exception {

		String prefix = StringUtil.randomString();
		String infix = StringUtil.randomString();
		String suffix = StringUtil.randomString();

		String content =
			prefix + "<img export-import-path=\"PATH_1\" />" + infix +
				"<img export-import-path=\"PATH_2\" />" + suffix;

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToImport(1, _fileEntry1);
		_defineFileEntryToImport(2, _fileEntry2);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceImportContentReferences(
				_portletDataContext, _blogsEntry, content);

		Assert.assertEquals(
			prefix + "<img data-fileEntryId=\"1\" src=\"URL_1\" />" + infix +
				"<img data-fileEntryId=\"2\" src=\"URL_2\" />" + suffix,
			replacedContent);
	}

	@Test
	public void testImportContentWithMultipleStaticReferences()
		throws Exception {

		String prefix = StringUtil.randomString();
		String infix = StringUtil.randomString();
		String suffix = StringUtil.randomString();

		String content =
			prefix + "<picture export-import-path=\"PATH_1\"></picture>" +
				infix + "<picture export-import-path=\"PATH_2\"></picture>" +
					suffix;

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToImport(1, _fileEntry1);
		_defineFileEntryToImport(2, _fileEntry2);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceImportContentReferences(
				_portletDataContext, _blogsEntry, content);

		Assert.assertEquals(
			prefix + "<picture data-fileEntryId=\"1\"><source /></picture>" +
				infix + "<picture data-fileEntryId=\"2\"><source /></picture>" +
					suffix,
			replacedContent);
	}

	@Test
	public void testImportContentWithNoReferences() throws Exception {
		String content = StringUtil.randomString();

		_makeOverridenProcessorReturn(content);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceImportContentReferences(
				_portletDataContext, _blogsEntry, content);

		Assert.assertEquals(content, replacedContent);
	}

	@Test
	public void testImportContentWithStaticReference() throws Exception {
		String prefix = StringUtil.randomString();
		String suffix = StringUtil.randomString();

		String content =
			prefix + "<picture export-import-path=\"PATH_1\"></picture>" +
				suffix;

		_makeOverridenProcessorReturn(content);

		_defineFileEntryToImport(1, _fileEntry1);

		String replacedContent =
			_blogsExportImportContentProcessor.replaceImportContentReferences(
				_portletDataContext, _blogsEntry, content);

		Assert.assertEquals(
			prefix + "<picture data-fileEntryId=\"1\"><source /></picture>" +
				suffix,
			replacedContent);
	}

	@Test(expected = NoSuchFileEntryException.class)
	public void testValidateContentFailsWhenInvalidDynamicReferences()
		throws Exception {

		String content = "<img data-fileentryid=\"1\" src=\"PATH_1\" />";

		Mockito.doThrow(
			NoSuchFileEntryException.class
		).when(
			_dlAppLocalService
		).getFileEntry(
			Mockito.anyLong()
		);

		_blogsExportImportContentProcessor.validateContentReferences(
			1, content);
	}

	@Test(expected = NoSuchFileEntryException.class)
	public void testValidateContentFailsWhenInvalidStaticReferences()
		throws Exception {

		String content = "<picture data-fileentryid=\"1\"></picture>";

		Mockito.doThrow(
			NoSuchFileEntryException.class
		).when(
			_dlAppLocalService
		).getFileEntry(
			Mockito.anyLong()
		);

		_blogsExportImportContentProcessor.validateContentReferences(
			1, content);
	}

	@Test(expected = PortalException.class)
	public void testValidateContentFailsWhenOverridenProcessorFails()
		throws Exception {

		String content = StringUtil.randomString();

		Mockito.doThrow(
			PortalException.class
		).when(
			_exportImportContentProcessor
		).validateContentReferences(
			Mockito.anyLong(), Mockito.anyString()
		);

		_blogsExportImportContentProcessor.validateContentReferences(
			1, content);
	}

	@Test
	public void testValidateContentSucceedsWhenAllDynamicReferencesAreValid()
		throws Exception {

		String content = "<img data-fileentryid=\"1\" src=\"PATH_1\" />";

		Mockito.doReturn(
			_fileEntry1
		).when(
			_dlAppLocalService
		).getFileEntry(
			Mockito.anyLong()
		);

		_blogsExportImportContentProcessor.validateContentReferences(
			1, content);
	}

	@Test
	public void testValidateContentSucceedsWhenAllStaticReferencesAreValid()
		throws Exception {

		String content = "<picture data-fileentryid=\"1\"></picture>";

		Mockito.doReturn(
			_fileEntry1
		).when(
			_dlAppLocalService
		).getFileEntry(
			Mockito.anyLong()
		);

		_blogsExportImportContentProcessor.validateContentReferences(
			1, content);
	}

	private void _defineFileEntryToExport(long fileEntryId, FileEntry fileEntry)
		throws PortalException {

		Mockito.doReturn(
			fileEntry
		).when(
			_dlAppLocalService
		).getFileEntry(
			fileEntryId
		);

		Mockito.when(
			ExportImportPathUtil.getModelPath(fileEntry)
		).thenReturn(
			"PATH_" + fileEntryId
		);
	}

	private void _defineFileEntryToImport(long fileEntryId, FileEntry fileEntry)
		throws Exception {

		Mockito.doReturn(
			true
		).when(
			_adaptiveMediaEmbeddedReferenceSet
		).containsReference(
			"PATH_" + fileEntryId
		);

		Mockito.doReturn(
			fileEntryId
		).when(
			_adaptiveMediaEmbeddedReferenceSet
		).importReference(
			"PATH_" + fileEntryId
		);

		Mockito.doReturn(
			fileEntry
		).when(
			_dlAppLocalService
		).getFileEntry(
			fileEntryId
		);

		Mockito.when(
			DLUtil.getPreviewURL(
				fileEntry, fileEntry.getFileVersion(), null, StringPool.BLANK)
		).thenReturn(
			"URL_" + fileEntryId
		);

		Mockito.when(
			_adaptiveMediaImageHTMLTagFactory.create(
				Mockito.anyString(), Mockito.eq(fileEntry))
		).thenReturn(
			"<picture><source/></picture>"
		);
	}

	private void _makeOverridenProcessorReturn(String content)
		throws Exception {

		Mockito.doReturn(
			content
		).when(
			_exportImportContentProcessor
		).replaceExportContentReferences(
			Mockito.any(PortletDataContext.class),
			Mockito.any(StagedModel.class), Mockito.anyString(),
			Mockito.anyBoolean(), Mockito.anyBoolean()
		);

		Mockito.doReturn(
			content
		).when(
			_exportImportContentProcessor
		).replaceImportContentReferences(
			Mockito.any(PortletDataContext.class),
			Mockito.any(StagedModel.class), Mockito.anyString()
		);

		Mockito.doReturn(
			_adaptiveMediaEmbeddedReferenceSet
		).when(
			_adaptiveMediaEmbeddedReferenceSetFactory
		).create(
			Mockito.any(PortletDataContext.class),
			Mockito.any(StagedModel.class)
		);
	}

	private final AdaptiveMediaEmbeddedReferenceSet
		_adaptiveMediaEmbeddedReferenceSet = Mockito.mock(
			AdaptiveMediaEmbeddedReferenceSet.class);
	private final AdaptiveMediaEmbeddedReferenceSetFactory
		_adaptiveMediaEmbeddedReferenceSetFactory = Mockito.mock(
			AdaptiveMediaEmbeddedReferenceSetFactory.class);
	private final AdaptiveMediaImageHTMLTagFactory
		_adaptiveMediaImageHTMLTagFactory = Mockito.mock(
			AdaptiveMediaImageHTMLTagFactory.class);
	private final BlogsEntry _blogsEntry = Mockito.mock(BlogsEntry.class);
	private final AdaptiveMediaBlogsEntryExportImportContentProcessor
		_blogsExportImportContentProcessor =
			new AdaptiveMediaBlogsEntryExportImportContentProcessor();
	private final DLAppLocalService _dlAppLocalService = Mockito.mock(
		DLAppLocalService.class);
	private final ExportImportContentProcessor<String>
		_exportImportContentProcessor = Mockito.mock(
			ExportImportContentProcessor.class);
	private final FileEntry _fileEntry1 = Mockito.mock(FileEntry.class);
	private final FileEntry _fileEntry2 = Mockito.mock(FileEntry.class);
	private final PortletDataContext _portletDataContext = Mockito.mock(
		PortletDataContext.class);

}