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

import com.liferay.blogs.kernel.model.BlogsEntry;
import com.liferay.exportimport.content.processor.ExportImportContentProcessor;
import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.StringUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Adolfo Pérez
 */
public class AMBlogsEntryExportImportContentProcessorTest {

	@Before
	public void setUp() throws Exception {
		_amBlogsEntryExportImportContentProcessor.
			setAMHTMLExportImportContentProcessor(
				_amHTMLExportImportContentProcessor);
		_amBlogsEntryExportImportContentProcessor.
			setBlogsExportImportContentProcessor(
				_blogsExportImportContentProcessor);
	}

	@Test
	public void testExportCallsBlogsAndAmHTMLContentProcessors()
		throws Exception {

		String originalContent = StringUtil.randomString();
		String blogsReplacedContent = StringUtil.randomString();
		String finalContent = StringUtil.randomString();

		Mockito.doReturn(
			blogsReplacedContent
		).when(
			_blogsExportImportContentProcessor
		).replaceExportContentReferences(
			_portletDataContext, _blogsEntry, originalContent, false, false
		);

		Mockito.doReturn(
			finalContent
		).when(
			_amHTMLExportImportContentProcessor
		).replaceExportContentReferences(
			_portletDataContext, _blogsEntry, blogsReplacedContent, false, false
		);

		Assert.assertEquals(finalContent, _export(originalContent));
	}

	@Test
	public void testImportCallsBlogsAndAmHTMLContentProcessors()
		throws Exception {

		String originalContent = StringUtil.randomString();
		String blogsReplacedContent = StringUtil.randomString();
		String finalContent = StringUtil.randomString();

		Mockito.doReturn(
			blogsReplacedContent
		).when(
			_blogsExportImportContentProcessor
		).replaceImportContentReferences(
			_portletDataContext, _blogsEntry, originalContent
		);

		Mockito.doReturn(
			finalContent
		).when(
			_amHTMLExportImportContentProcessor
		).replaceImportContentReferences(
			_portletDataContext, _blogsEntry, blogsReplacedContent
		);

		Assert.assertEquals(finalContent, _import(originalContent));
	}

	@Test(expected = PortalException.class)
	public void testValidateContentFailsWhAMHTMLProcessorFails()
		throws Exception {

		String content = StringUtil.randomString();

		Mockito.doThrow(
			PortalException.class
		).when(
			_amHTMLExportImportContentProcessor
		).validateContentReferences(
			Mockito.anyLong(), Mockito.anyString()
		);

		_amBlogsEntryExportImportContentProcessor.validateContentReferences(
			1, content);
	}

	@Test(expected = PortalException.class)
	public void testValidateContentFailsWhenBlogsProcessorFails()
		throws Exception {

		String content = StringUtil.randomString();

		Mockito.doThrow(
			PortalException.class
		).when(
			_blogsExportImportContentProcessor
		).validateContentReferences(
			Mockito.anyLong(), Mockito.anyString()
		);

		_amBlogsEntryExportImportContentProcessor.validateContentReferences(
			1, content);
	}

	@Test
	public void testValidateContentSucceedsWhenBothProcessorsSucceed()
		throws Exception {

		_amBlogsEntryExportImportContentProcessor.validateContentReferences(
			1, StringUtil.randomString());
	}

	private String _export(String content) throws Exception {
		return _amBlogsEntryExportImportContentProcessor.
			replaceExportContentReferences(
				_portletDataContext, _blogsEntry, content, false, false);
	}

	private String _import(String exportedContent) throws Exception {
		return _amBlogsEntryExportImportContentProcessor.
			replaceImportContentReferences(
				_portletDataContext, _blogsEntry, exportedContent);
	}

	private final AMBlogsEntryExportImportContentProcessor
		_amBlogsEntryExportImportContentProcessor =
			new AMBlogsEntryExportImportContentProcessor();
	private final ExportImportContentProcessor<String>
		_amHTMLExportImportContentProcessor = Mockito.mock(
			ExportImportContentProcessor.class);
	private final BlogsEntry _blogsEntry = Mockito.mock(BlogsEntry.class);
	private final ExportImportContentProcessor<String>
		_blogsExportImportContentProcessor = Mockito.mock(
			ExportImportContentProcessor.class);
	private final PortletDataContext _portletDataContext = Mockito.mock(
		PortletDataContext.class);

}