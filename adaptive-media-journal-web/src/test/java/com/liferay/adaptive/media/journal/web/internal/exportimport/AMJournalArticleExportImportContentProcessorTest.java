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

package com.liferay.adaptive.media.journal.web.internal.exportimport;

import com.liferay.exportimport.content.processor.ExportImportContentProcessor;
import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.StringUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Adolfo Pérez
 */
public class AMJournalArticleExportImportContentProcessorTest {

	@Before
	public void setUp() throws Exception {
		_amJournalArticleExportImportContentProcessor.
			setAMJournalArticleContentHTMLReplacer(
				_amJournalArticleContentHTMLReplacer);
		_amJournalArticleExportImportContentProcessor.
			setAMHTMLExportImportContentProcessor(
				_amHTMLExportImportContentProcessor);
		_amJournalArticleExportImportContentProcessor.
			setJournalArticleExportImportContentProcessor(
				_journalArticleExportImportContentProcessor);

		Mockito.when(
			_amJournalArticleContentHTMLReplacer.replace(
				Mockito.anyString(),
				Mockito.any(AMJournalArticleContentHTMLReplacer.Replace.class))
		).then(
			answer -> {
				AMJournalArticleContentHTMLReplacer.Replace replace =
					answer.getArgumentAt(
						1, AMJournalArticleContentHTMLReplacer.Replace.class);
				String content = answer.getArgumentAt(0, String.class);

				return replace.apply(content);
			}
		);
	}

	@Test
	public void testExportCallsJournalAndAmHTMLContentProcessors()
		throws Exception {

		String originalContent = StringUtil.randomString();
		String journalReplacedContent = StringUtil.randomString();
		String finalContent = StringUtil.randomString();

		Mockito.doReturn(
			journalReplacedContent
		).when(
			_journalArticleExportImportContentProcessor
		).replaceExportContentReferences(
			_portletDataContext, _journalArticle, originalContent, false, false
		);

		Mockito.doReturn(
			finalContent
		).when(
			_amHTMLExportImportContentProcessor
		).replaceExportContentReferences(
			_portletDataContext, _journalArticle, journalReplacedContent, false,
			false
		);

		Assert.assertEquals(finalContent, _export(originalContent));
	}

	@Test
	public void testImportCallsJournalAndAmHTMLContentProcessors()
		throws Exception {

		String originalContent = StringUtil.randomString();
		String journalReplacedContent = StringUtil.randomString();
		String finalContent = StringUtil.randomString();

		Mockito.doReturn(
			journalReplacedContent
		).when(
			_journalArticleExportImportContentProcessor
		).replaceImportContentReferences(
			_portletDataContext, _journalArticle, originalContent
		);

		Mockito.doReturn(
			finalContent
		).when(
			_amHTMLExportImportContentProcessor
		).replaceImportContentReferences(
			_portletDataContext, _journalArticle, journalReplacedContent
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

		_amJournalArticleExportImportContentProcessor.validateContentReferences(
			1, content);
	}

	@Test(expected = PortalException.class)
	public void testValidateContentFailsWhenJournalProcessorFails()
		throws Exception {

		String content = StringUtil.randomString();

		Mockito.doThrow(
			PortalException.class
		).when(
			_journalArticleExportImportContentProcessor
		).validateContentReferences(
			Mockito.anyLong(), Mockito.anyString()
		);

		_amJournalArticleExportImportContentProcessor.validateContentReferences(
			1, content);
	}

	@Test
	public void testValidateContentSucceedsWhenBothProcessorsSucceed()
		throws Exception {

		_amJournalArticleExportImportContentProcessor.validateContentReferences(
			1, StringUtil.randomString());
	}

	private String _export(String content) throws Exception {
		return _amJournalArticleExportImportContentProcessor.
			replaceExportContentReferences(
				_portletDataContext, _journalArticle, content, false, false);
	}

	private String _import(String exportedContent) throws Exception {
		return _amJournalArticleExportImportContentProcessor.
			replaceImportContentReferences(
				_portletDataContext, _journalArticle, exportedContent);
	}

	private final ExportImportContentProcessor<String>
		_amHTMLExportImportContentProcessor = Mockito.mock(
			ExportImportContentProcessor.class);
	private final AMJournalArticleContentHTMLReplacer
		_amJournalArticleContentHTMLReplacer = Mockito.mock(
			AMJournalArticleContentHTMLReplacer.class);
	private final AMJournalArticleExportImportContentProcessor
		_amJournalArticleExportImportContentProcessor =
			new AMJournalArticleExportImportContentProcessor();
	private final JournalArticle _journalArticle = Mockito.mock(
		JournalArticle.class);
	private final ExportImportContentProcessor<String>
		_journalArticleExportImportContentProcessor = Mockito.mock(
			ExportImportContentProcessor.class);
	private final PortletDataContext _portletDataContext = Mockito.mock(
		PortletDataContext.class);

}