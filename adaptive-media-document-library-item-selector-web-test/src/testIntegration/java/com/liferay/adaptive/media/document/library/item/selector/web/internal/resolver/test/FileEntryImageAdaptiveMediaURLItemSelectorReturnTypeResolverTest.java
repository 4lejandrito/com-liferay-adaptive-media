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

package com.liferay.adaptive.media.document.library.item.selector.web.internal.resolver.test;

import com.liferay.adaptive.media.image.configuration.ImageAdaptiveMediaConfigurationEntry;
import com.liferay.adaptive.media.image.configuration.ImageAdaptiveMediaConfigurationHelper;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.item.selector.ItemSelectorReturnTypeResolver;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.messaging.Destination;
import com.liferay.portal.kernel.messaging.MessageBus;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.messaging.SynchronousDestination;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.service.test.ServiceTestUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.registry.Registry;
import com.liferay.registry.RegistryUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Sergio González
 */
@RunWith(Arquillian.class)
@Sync
public class FileEntryImageAdaptiveMediaURLItemSelectorReturnTypeResolverTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			SynchronousDestinationTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_configurationHelper = _getService(
			ImageAdaptiveMediaConfigurationHelper.class);
		_dlAppLocalService = _getService(DLAppLocalService.class);

		_resolver = _getService(
			ItemSelectorReturnTypeResolver.class, _RESOLVER_FILTER);

		ServiceTestUtil.setUser(TestPropsValues.getUser());

		Collection<ImageAdaptiveMediaConfigurationEntry> configurationEntries =
			_configurationHelper.getImageAdaptiveMediaConfigurationEntries(
				TestPropsValues.getCompanyId(), configurationEntry -> true);

		for (ImageAdaptiveMediaConfigurationEntry configurationEntry :
				configurationEntries) {

			_configurationHelper.
				forceDeleteImageAdaptiveMediaConfigurationEntry(
					TestPropsValues.getCompanyId(),
					configurationEntry.getUUID());
		}

		_addTestVariant();
	}

	@After
	public void tearDown() throws Exception {
		Collection<ImageAdaptiveMediaConfigurationEntry> configurationEntries =
			_configurationHelper.getImageAdaptiveMediaConfigurationEntries(
				TestPropsValues.getCompanyId(), configurationEntry -> true);

		for (ImageAdaptiveMediaConfigurationEntry configurationEntry :
				configurationEntries) {

			_configurationHelper.
				forceDeleteImageAdaptiveMediaConfigurationEntry(
					TestPropsValues.getCompanyId(),
					configurationEntry.getUUID());
		}
	}

	@Test
	public void testAddingFileEntryWithImageCreatesMedia() throws Exception {
		try (DestinationReplacer destinationReplacer = new DestinationReplacer(
				"liferay/adaptive_media_processor")) {

			ServiceContext serviceContext =
				ServiceContextTestUtil.getServiceContext(
					_group, TestPropsValues.getUserId());

			final FileEntry fileEntry = _addImageFileEntry(serviceContext);

			String value = _resolver.getValue(fileEntry, null);

			JSONObject jsonObject = JSONFactoryUtil.createJSONObject(value);

			String defaultSource = jsonObject.getString("url");
			long fileEntryId = jsonObject.getLong("fileEntryId");

			Assert.assertEquals(
				DLUtil.getPreviewURL(
					fileEntry, fileEntry.getFileVersion(), null,
					StringPool.BLANK, false, false),
				defaultSource);

			Assert.assertEquals(fileEntry.getFileEntryId(), fileEntryId);
		}
	}

	public class DestinationReplacer implements AutoCloseable {

		public DestinationReplacer(String destinationName) {
			MessageBus messageBus = MessageBusUtil.getMessageBus();

			_destination = messageBus.getDestination(destinationName);

			SynchronousDestination synchronousDestination =
				new SynchronousDestination();

			synchronousDestination.setName(destinationName);

			messageBus.replace(synchronousDestination);
		}

		@Override
		public void close() throws Exception {
			MessageBus messageBus = MessageBusUtil.getMessageBus();

			messageBus.replace(_destination);
		}

		private final Destination _destination;

	}

	private FileEntry _addImageFileEntry(ServiceContext serviceContext)
		throws Exception {

		return _dlAppLocalService.addFileEntry(
			TestPropsValues.getUserId(), _group.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString(), ContentTypes.IMAGE_JPEG,
			_getImageBytes(), serviceContext);
	}

	private void _addTestVariant() throws Exception {
		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "50");
		properties.put("max-width", "50");

		_configurationHelper.addImageAdaptiveMediaConfigurationEntry(
			TestPropsValues.getCompanyId(), "small", "uuid0", properties);

		properties = new HashMap<>();

		properties.put("max-height", "400");
		properties.put("max-width", "280");

		_configurationHelper.addImageAdaptiveMediaConfigurationEntry(
			TestPropsValues.getCompanyId(), "big", "uuid1", properties);

		properties = new HashMap<>();

		properties.put("max-height", "300");
		properties.put("max-width", "200");

		_configurationHelper.addImageAdaptiveMediaConfigurationEntry(
			TestPropsValues.getCompanyId(), "medium", "uuid2", properties);

		properties = new HashMap<>();

		properties.put("max-height", "500");
		properties.put("max-width", "330");

		_configurationHelper.addImageAdaptiveMediaConfigurationEntry(
			TestPropsValues.getCompanyId(), "extra", "uuid3", properties);
	}

	private byte[] _getImageBytes() throws Exception {
		return FileUtil.getBytes(
			FileEntryImageAdaptiveMediaURLItemSelectorReturnTypeResolverTest.
				class,
			"/com/liferay/adaptive/media/document/library/item/selector/web" +
				"/internal/resolver/test/dependencies/image.jpg");
	}

	private <T> T _getService(Class<T> clazz) {
		try {
			Registry registry = RegistryUtil.getRegistry();

			return registry.getService(clazz);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private <T> T _getService(Class<T> clazz, String filter) {
		try {
			Registry registry = RegistryUtil.getRegistry();

			Collection<T> services = registry.getServices(clazz, filter);

			return services.iterator().next();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final String _RESOLVER_FILTER =
		"(objectClass=com.liferay.adaptive.media.document.library.item." +
			"selector.web.internal.resolver." +
				"FileEntryImageAdaptiveMediaURLItemSelectorReturnTypeResolver)";

	private ImageAdaptiveMediaConfigurationHelper _configurationHelper;
	private DLAppLocalService _dlAppLocalService;

	@DeleteAfterTestRun
	private Group _group;

	private ItemSelectorReturnTypeResolver _resolver;

}