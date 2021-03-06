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

package com.liferay.poshi.runner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.liferay.poshi.runner.pql.PQLEntity;
import com.liferay.poshi.runner.pql.PQLEntityFactory;
import com.liferay.poshi.runner.selenium.LiferaySelenium;
import com.liferay.poshi.runner.util.FileUtil;
import com.liferay.poshi.runner.util.MathUtil;
import com.liferay.poshi.runner.util.OSDetector;
import com.liferay.poshi.runner.util.PropsValues;
import com.liferay.poshi.runner.util.StringUtil;
import com.liferay.poshi.runner.util.Validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Method;

import java.net.URI;
import java.net.URL;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * @author Karen Dang
 * @author Michael Hashimoto
 */
public class PoshiRunnerContext {

	public static void clear() {
		_commandElements.clear();
		_commandReturns.clear();
		_commandSummaries.clear();
		_filePaths.clear();
		_functionLocatorCounts.clear();
		_pathLocators.clear();
		_rootElements.clear();
		_seleniumParameterCounts.clear();
	}

	public static String getDefaultNamespace() {
		return _DEFAULT_NAMESPACE;
	}

	public static String getFilePathFromFileName(
		String fileName, String namespace) {

		return _filePaths.get(namespace + "." + fileName);
	}

	public static List<String> getFilePaths() {
		return new ArrayList<>(_filePaths.values());
	}

	public static Element getFunctionCommandElement(
		String classCommandName, String namespace) {

		return _commandElements.get(
			"function#" + namespace + "." + classCommandName);
	}

	public static String getFunctionCommandSummary(
		String classCommandName, String namespace) {

		return _commandSummaries.get(
			"function#" + namespace + "." + classCommandName);
	}

	public static int getFunctionLocatorCount(
		String className, String namespace) {

		String functionLocatorCountKey = namespace + "." + className;

		if (_functionLocatorCounts.containsKey(functionLocatorCountKey)) {
			return _functionLocatorCounts.get(functionLocatorCountKey);
		}

		return 0;
	}

	public static Element getFunctionRootElement(
		String className, String namespace) {

		return _rootElements.get("function#" + namespace + "." + className);
	}

	public static Element getMacroCommandElement(
		String classCommandName, String namespace) {

		return _commandElements.get(
			"macro#" + namespace + "." + classCommandName);
	}

	public static List<String> getMacroCommandReturns(
		String classCommandName, String namespace) {

		return _commandReturns.get(
			"macro#" + namespace + "." + classCommandName);
	}

	public static String getMacroCommandSummary(
		String classCommandName, String namespace) {

		return _commandSummaries.get(
			"macro#" + namespace + "." + classCommandName);
	}

	public static Element getMacroRootElement(
		String className, String namespace) {

		return _rootElements.get("macro#" + namespace + "." + className);
	}

	public static String getNamespaceFromFilePath(String filePath) {
		if (Validator.isNull(filePath)) {
			return getDefaultNamespace();
		}

		for (Map.Entry<String, String> entry : _filePaths.entrySet()) {
			String value = entry.getValue();

			if (value.equals(filePath)) {
				String key = entry.getKey();
				String fileName = PoshiRunnerGetterUtil.getFileNameFromFilePath(
					filePath);

				return key.substring(0, key.indexOf("." + fileName));
			}
		}

		return getDefaultNamespace();
	}

	public static String getPathLocator(
		String pathLocatorKey, String namespace) {

		String pathLocator = _pathLocators.get(
			namespace + "." + pathLocatorKey);

		String className =
			PoshiRunnerGetterUtil.getClassNameFromNamespacedClassCommandName(
				pathLocatorKey);

		if ((pathLocator == null) &&
			_pathExtensions.containsKey(namespace + "." + className)) {

			String pathExtension = _pathExtensions.get(
				namespace + "." + className);
			String commandName =
				PoshiRunnerGetterUtil.
					getCommandNameFromNamespacedClassCommandName(
						pathLocatorKey);

			return getPathLocator(pathExtension + "#" + commandName, namespace);
		}

		return pathLocator;
	}

	public static Element getPathRootElement(
		String className, String namespace) {

		return _rootElements.get("path#" + namespace + "." + className);
	}

	public static int getSeleniumParameterCount(String commandName) {
		return _seleniumParameterCounts.get(commandName);
	}

	public static List<String> getTestCaseAvailablePropertyNames() {
		return _testCaseAvailablePropertyNames;
	}

	public static Element getTestCaseCommandElement(
		String classCommandName, String namespace) {

		return _commandElements.get(
			"test-case#" + namespace + "." + classCommandName);
	}

	public static String getTestCaseDescription(String classCommandName) {
		return _testCaseDescriptions.get(classCommandName);
	}

	public static String getTestCaseNamespacedClassCommandName() {
		return _testCaseNamespacedClassCommandName;
	}

	public static List<String> getTestCaseRequiredPropertyNames() {
		return _testCaseRequiredPropertyNames;
	}

	public static Element getTestCaseRootElement(
		String className, String namespace) {

		return _rootElements.get("test-case#" + namespace + "." + className);
	}

	public static boolean isCommandElement(
		String classType, String classCommandName, String namespace) {

		return _commandElements.containsKey(
			classType + "#" + namespace + "." + classCommandName);
	}

	public static boolean isPathLocator(
		String pathLocatorKey, String namespace) {

		String pathLocator = getPathLocator(pathLocatorKey, namespace);

		if (pathLocator != null) {
			return true;
		}

		return false;
	}

	public static boolean isRootElement(
		String classType, String rootElementKey, String namespace) {

		return _rootElements.containsKey(
			classType + "#" + namespace + "." + rootElementKey);
	}

	public static boolean isTestToggle(String toggleName) {
		return _testToggleNames.contains(toggleName);
	}

	public static void main(String[] args) throws Exception {
		readFiles();

		PoshiRunnerValidation.validate();

		_writeTestCaseMethodNamesProperties();
		_writeTestGeneratedProperties();
	}

	public static void readFiles() throws Exception {
		_readPoshiFiles();
		_readSeleniumFiles();
		_readTestToggleFiles();
	}

	public static void setTestCaseNamespacedClassCommandName(
		String testCaseNamespacedClassCommandName) {

		_testCaseNamespacedClassCommandName =
			testCaseNamespacedClassCommandName;
	}

	private static int _getAllocatedTestGroupSize(int testCount) {
		int groupCount = MathUtil.quotient(
			testCount, PropsValues.TEST_BATCH_MAX_GROUP_SIZE, true);

		return MathUtil.quotient(testCount, groupCount, true);
	}

	private static Properties _getClassCommandNameProperties(
			Element rootElement, Element commandElement)
		throws Exception {

		Properties properties = new Properties();

		List<Element> rootPropertyElements = rootElement.elements("property");

		for (Element propertyElement : rootPropertyElements) {
			String propertyName = propertyElement.attributeValue("name");
			String propertyValue = propertyElement.attributeValue("value");

			properties.setProperty(propertyName, propertyValue);
		}

		List<Element> commandPropertyElements = commandElement.elements(
			"property");

		for (Element propertyElement : commandPropertyElements) {
			String propertyName = propertyElement.attributeValue("name");
			String propertyValue = propertyElement.attributeValue("value");

			properties.setProperty(propertyName, propertyValue);
		}

		if (Validator.isNotNull(
				commandElement.attributeValue("known-issues"))) {

			String knownIssues = commandElement.attributeValue("known-issues");

			properties.setProperty("known-issues", knownIssues);
		}

		if (Validator.isNotNull(commandElement.attributeValue("priority"))) {
			String priority = commandElement.attributeValue("priority");

			properties.setProperty("priority", priority);
		}

		return properties;
	}

	private static List<String> _getCommandReturns(Element commandElement) {
		String returns = commandElement.attributeValue("returns");

		if (returns == null) {
			return new ArrayList<>();
		}

		return Arrays.asList(StringUtil.split(returns));
	}

	private static String _getCommandSummary(
		String classCommandName, String classType, Element commandElement,
		Element rootElement) {

		String summaryIgnore = commandElement.attributeValue("summary-ignore");

		if (Validator.isNotNull(summaryIgnore) &&
			summaryIgnore.equals("true")) {

			return null;
		}

		if (Validator.isNotNull(commandElement.attributeValue("summary"))) {
			return commandElement.attributeValue("summary");
		}

		if (classType.equals("function")) {
			if (Validator.isNotNull(rootElement.attributeValue("summary"))) {
				return rootElement.attributeValue("summary");
			}
		}

		return classCommandName;
	}

	private static Exception _getDuplicateLocatorsException() {
		StringBuilder sb = new StringBuilder();

		sb.append(
			"Duplicate locator(s) found while loading Poshi files into " +
				"context:\n");

		for (String exception : _duplicateLocatorMessages) {
			sb.append(exception);
			sb.append("\n\n");
		}

		return new Exception(sb.toString());
	}

	private static List<URL> _getPoshiURLs(
			FileSystem fileSystem, String[] includes, String baseDirName)
		throws IOException {

		List<URL> urls = null;

		if (fileSystem == null) {
			urls = FileUtil.getIncludedResourceURLs(includes, baseDirName);
		}
		else {
			urls = FileUtil.getIncludedResourceURLs(
				fileSystem, includes, baseDirName);
		}

		return urls;
	}

	private static List<URL> _getPoshiURLs(
			String[] includes, String baseDirName)
		throws Exception {

		return _getPoshiURLs(null, includes, baseDirName);
	}

	private static String _getTestBatchGroups() throws Exception {
		String propertyQuery = PropsValues.TEST_BATCH_PROPERTY_QUERY;

		if (propertyQuery == null) {
			String[] propertyNames = PropsValues.TEST_BATCH_PROPERTY_NAMES;
			String[] propertyValues = PropsValues.TEST_BATCH_PROPERTY_VALUES;

			if (propertyNames.length != propertyValues.length) {
				throw new Exception(
					"'test.batch.property.names'" +
						"/'test.batch.property.values' must have matching " +
							"amounts of entries!");
			}

			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < propertyNames.length; i++) {
				sb.append(propertyNames[i]);
				sb.append(" == \"");
				sb.append(propertyValues[i]);
				sb.append("\"");

				if (i < (propertyNames.length - 1)) {
					sb.append(" OR ");
				}
			}

			propertyQuery = sb.toString();
		}

		if (Validator.isNotNull(PropsValues.TEST_RUN_ENVIRONMENT)) {
			StringBuilder sb = new StringBuilder();

			sb.append("(");
			sb.append(propertyQuery);
			sb.append(") AND ");
			sb.append("(test.run.environment == \"");
			sb.append(PropsValues.TEST_RUN_ENVIRONMENT);
			sb.append("\" OR test.run.environment == null)");

			propertyQuery = sb.toString();
		}

		List<String> namespacedClassCommandNames = new ArrayList<>();

		PQLEntity pqlEntity = PQLEntityFactory.newPQLEntity(propertyQuery);

		for (String testCaseNamespacedClassCommandName :
				_testCaseNamespacedClassCommandNames) {

			Properties properties =
				_namespacedClassCommandNamePropertiesMap.get(
					testCaseNamespacedClassCommandName);

			Boolean pqlResultBoolean = (Boolean)pqlEntity.getPQLResult(
				properties);

			if (pqlResultBoolean) {
				namespacedClassCommandNames.add(
					testCaseNamespacedClassCommandName);
			}
		}

		System.out.println(
			"The following query returned " +
				namespacedClassCommandNames.size() +
					" test class command names:");
		System.out.println(propertyQuery);

		if (PropsValues.TEST_BATCH_RUN_TYPE.equals("sequential")) {
			return _getTestBatchSequentialGroups(namespacedClassCommandNames);
		}
		else if (PropsValues.TEST_BATCH_RUN_TYPE.equals("single")) {
			return _getTestBatchSingleGroups(namespacedClassCommandNames);
		}

		throw new Exception(
			"'test.batch.run.type' must be set to 'single' or 'sequential'");
	}

	private static String _getTestBatchSequentialGroups(
			List<String> namespacedClassCommandNames)
		throws Exception {

		Multimap<Properties, String> multimap = HashMultimap.create();

		for (String namespacedClassCommandName : namespacedClassCommandNames) {
			Properties properties = new Properties();

			properties.putAll(
				_namespacedClassCommandNamePropertiesMap.get(
					namespacedClassCommandName));

			if (Validator.isNotNull(
					PropsValues.TEST_BATCH_GROUP_IGNORE_REGEX)) {

				Set<String> propertyNames = properties.stringPropertyNames();

				for (String propertyName : propertyNames) {
					if (propertyName.matches(
							PropsValues.TEST_BATCH_GROUP_IGNORE_REGEX)) {

						properties.remove(propertyName);
					}
				}
			}

			multimap.put(properties, namespacedClassCommandName);
		}

		Map<Integer, List<String>> classCommandNameGroups = new HashMap<>();
		int classCommandNameIndex = 0;
		Map<Properties, Collection<String>> map = multimap.asMap();

		for (Collection<String> value : map.values()) {
			List<String> classCommandNameGroup = new ArrayList(value);

			Collections.sort(classCommandNameGroup);

			int groupSize = _getAllocatedTestGroupSize(
				classCommandNameGroup.size());

			List<List<String>> partitions = Lists.partition(
				classCommandNameGroup, groupSize);

			for (int j = 0; j < partitions.size(); j++) {
				classCommandNameGroups.put(
					classCommandNameIndex, partitions.get(j));

				classCommandNameIndex++;
			}
		}

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < classCommandNameGroups.size(); i++) {
			List<String> classCommandNameGroup = classCommandNameGroups.get(i);
			int subgroupSize = PropsValues.TEST_BATCH_MAX_SUBGROUP_SIZE;

			int subgroupCount = MathUtil.quotient(
				classCommandNameGroup.size(), subgroupSize, true);

			sb.append("RUN_TEST_CASE_METHOD_GROUP_");
			sb.append(i);
			sb.append("=");

			for (int j = 0; j < subgroupCount; j++) {
				sb.append(i);
				sb.append("_");
				sb.append(j);

				if (j < (subgroupCount - 1)) {
					sb.append(" ");
				}
			}

			sb.append("\n");

			for (int j = 0; j < classCommandNameGroup.size(); j++) {
				if ((j % subgroupSize) == 0) {
					sb.append("RUN_TEST_CASE_METHOD_GROUP_");
					sb.append(i);
					sb.append("_");
					sb.append(j / subgroupSize);
					sb.append("=");
					sb.append(classCommandNameGroup.get(j));
				}
				else if (((j + 1) % subgroupSize) == 0) {
					sb.append(",");
					sb.append(classCommandNameGroup.get(j));
					sb.append("\n");
				}
				else {
					sb.append(",");
					sb.append(classCommandNameGroup.get(j));
				}
			}

			sb.append("\n");
		}

		sb.append("RUN_TEST_CASE_METHOD_GROUPS=");

		for (int i = 0; i < classCommandNameGroups.size(); i++) {
			sb.append(i);

			if (i < (classCommandNameGroups.size() - 1)) {
				sb.append(" ");
			}
		}

		return sb.toString();
	}

	private static String _getTestBatchSingleGroups(
		List<String> namespacedClassCommandNames) {

		StringBuilder sb = new StringBuilder();

		int groupSize = 15;

		List<List<String>> partitions = Lists.partition(
			namespacedClassCommandNames, groupSize);

		for (int i = 0; i < partitions.size(); i++) {
			sb.append("RUN_TEST_CASE_METHOD_GROUP_");
			sb.append(i);
			sb.append("=");

			List<String> partition = partitions.get(i);

			for (int j = 0; j < partition.size(); j++) {
				sb.append(i);
				sb.append("_");
				sb.append(j);

				if (j < (partition.size() - 1)) {
					sb.append(" ");
				}
			}

			sb.append("\n");

			for (int j = 0; j < partition.size(); j++) {
				sb.append("RUN_TEST_CASE_METHOD_GROUP_");
				sb.append(i);
				sb.append("_");
				sb.append(j);
				sb.append("=");
				sb.append(partition.get(j));
				sb.append("\n");
			}
		}

		sb.append("RUN_TEST_CASE_METHOD_GROUPS=");

		for (int i = 0; i < partitions.size(); i++) {
			sb.append(i);

			if (i < (partitions.size() - 1)) {
				sb.append(" ");
			}
		}

		return sb.toString();
	}

	private static void _initComponentCommandNamesMap() {
		for (String testCaseNamespacedClassName :
				_testCaseNamespacedClassNames) {

			String className =
				PoshiRunnerGetterUtil.getClassNameFromNamespacedClassName(
					testCaseNamespacedClassName);
			String namespace =
				PoshiRunnerGetterUtil.getNamespaceFromNamespacedClassName(
					testCaseNamespacedClassName);

			Element rootElement = getTestCaseRootElement(className, namespace);

			if (Objects.equals(rootElement.attributeValue("ignore"), "true")) {
				continue;
			}

			if (rootElement.attributeValue("extends") != null) {
				String extendsTestCaseClassName = rootElement.attributeValue(
					"extends");

				Element extendsRootElement = getTestCaseRootElement(
					extendsTestCaseClassName, namespace);

				List<Element> extendsCommandElements =
					extendsRootElement.elements("command");

				for (Element extendsCommandElement : extendsCommandElements) {
					String extendsCommandName =
						extendsCommandElement.attributeValue("name");

					if (_isIgnorableCommandNames(
							rootElement, extendsCommandElement,
							extendsCommandName)) {

						continue;
					}

					_commandElements.put(
						"test-case#" + testCaseNamespacedClassName + "#" +
							extendsCommandName,
						extendsCommandElement);
				}
			}

			List<Element> commandElements = rootElement.elements("command");

			for (Element commandElement : commandElements) {
				String commandName = commandElement.attributeValue("name");

				if (_isIgnorableCommandNames(
						rootElement, commandElement, commandName)) {

					continue;
				}

				String namespacedClassCommandName =
					testCaseNamespacedClassName + "#" + commandName;

				_testCaseNamespacedClassCommandNames.add(
					namespacedClassCommandName);
			}
		}
	}

	private static boolean _isIgnorableCommandNames(
		Element rootElement, Element commandElement, String commandName) {

		if (commandElement.attributeValue("ignore") != null) {
			String ignore = commandElement.attributeValue("ignore");

			if (ignore.equals("true")) {
				return true;
			}
		}

		List<String> ignorableCommandNames = new ArrayList<>();

		if (rootElement.attributeValue("ignore-command-names") != null) {
			String ignoreCommandNamesString = rootElement.attributeValue(
				"ignore-command-names");

			ignorableCommandNames = Arrays.asList(
				ignoreCommandNamesString.split(","));
		}

		if (ignorableCommandNames.contains(commandName)) {
			return true;
		}

		return false;
	}

	private static void _readPoshiFiles() throws Exception {
		String[] poshiFileNames = {
			"**/*.action", "**/*.function", "**/*.macro", "**/*.path",
			"**/*.testcase"
		};

		_readPoshiFilesFromClassPath(poshiFileNames, "testFunctional");

		if (Validator.isNotNull(PropsValues.TEST_INCLUDE_DIR_NAMES)) {
			_readPoshiFiles(
				new String[] {
					"**/*.action", "**/*.function", "**/*.macro", "**/*.path"
				},
				PropsValues.TEST_INCLUDE_DIR_NAMES);
		}

		if (Validator.isNotNull(PropsValues.TEST_SUBREPO_DIRS)) {
			_readPoshiFiles(poshiFileNames, PropsValues.TEST_SUBREPO_DIRS);
		}

		_readPoshiFiles(poshiFileNames, _TEST_BASE_DIR_NAME);

		_initComponentCommandNamesMap();

		if (!_duplicateLocatorMessages.isEmpty()) {
			throw _getDuplicateLocatorsException();
		}
	}

	private static void _readPoshiFiles(
			String[] includes, String... baseDirNames)
		throws Exception {

		for (String baseDirName : baseDirNames) {
			List<URL> poshiURLs = _getPoshiURLs(includes, baseDirName);

			_storeRootElements(poshiURLs, _DEFAULT_NAMESPACE);
		}
	}

	private static void _readPoshiFilesFromClassPath(
			String[] includes, String... resourceNames)
		throws Exception {

		for (String resourceName : resourceNames) {
			ClassLoader classLoader = PoshiRunnerContext.class.getClassLoader();

			Enumeration<URL> enumeration = classLoader.getResources(
				resourceName);

			while (enumeration.hasMoreElements()) {
				URL resourceURL = enumeration.nextElement();

				String resourceURLString = resourceURL.toString();

				int x = resourceURLString.indexOf("!");

				try (FileSystem fileSystem = FileSystems.newFileSystem(
						URI.create(resourceURLString.substring(0, x)),
						new HashMap<String, String>(), classLoader)) {

					Matcher matcher = _poshiResourceJarNamePattern.matcher(
						resourceURLString);

					if (!matcher.find()) {
						throw new RuntimeException(
							"A namespace must be defined for resource: " +
								resourceURLString);
					}

					String namespace = StringUtils.capitalize(
						matcher.group("namespace"));

					if (namespace.equals(_DEFAULT_NAMESPACE)) {
						throw new RuntimeException(
							"Namespace: '" + _DEFAULT_NAMESPACE +
								"' is reserved");
					}
					else if (_namespaces.contains(namespace)) {
						throw new RuntimeException(
							"Duplicate namespace " + namespace);
					}

					_namespaces.add(namespace);

					List<URL> poshiURLs = _getPoshiURLs(
						fileSystem, includes,
						resourceURLString.substring(x + 1));

					_storeRootElements(poshiURLs, namespace);
				}
			}
		}
	}

	private static void _readSeleniumFiles() throws Exception {
		Method[] methods = LiferaySelenium.class.getMethods();

		for (Method method : methods) {
			Class<?>[] classes = method.getParameterTypes();

			_seleniumParameterCounts.put(method.getName(), classes.length);
		}

		_seleniumParameterCounts.put("open", 1);
	}

	private static void _readTestToggleFiles() throws Exception {
		for (String testToggleFileName : PropsValues.TEST_TOGGLE_FILE_NAMES) {
			if (!FileUtil.exists(testToggleFileName)) {
				continue;
			}

			SAXReader saxReader = new SAXReader();

			String content = FileUtil.read(testToggleFileName);

			InputStream inputStream = new ByteArrayInputStream(
				content.getBytes("UTF-8"));

			Document document = saxReader.read(inputStream);

			Element rootElement = document.getRootElement();

			List<Element> toggleElements = rootElement.elements("toggle");

			for (Element toggleElement : toggleElements) {
				String toggleName = toggleElement.attributeValue("name");

				Element dateElement = toggleElement.element("date");

				if (dateElement == null) {
					StringBuilder sb = new StringBuilder();

					sb.append("Unable to parse toggle:\n");
					sb.append(testToggleFileName);
					sb.append(":");
					sb.append(toggleName);
					sb.append(" because the date was not found");

					Exception e = new RuntimeException(sb.toString());

					e.printStackTrace();

					throw e;
				}
				else {
					try {
						_toggleDateFormat.parse(dateElement.getText());
					}
					catch (ParseException pe) {
						StringBuilder sb = new StringBuilder();

						sb.append("Unable to parse date \"");
						sb.append(dateElement.getText());
						sb.append("\" in ");
						sb.append(testToggleFileName);
						sb.append(":");
						sb.append(toggleName);
						sb.append(" because it doesn't match the format \"");
						sb.append(_toggleDateFormat.toPattern());
						sb.append("\"");

						Exception e = new RuntimeException(sb.toString(), pe);

						e.printStackTrace();

						throw e;
					}
				}

				Element ownerElement = toggleElement.element("owner");

				if ((ownerElement == null) ||
					Validator.isNull(ownerElement.getText())) {

					Exception exception = new Exception(
						"Please set an author for this toggle:\n" +
							testToggleFileName + ":" + toggleName);

					exception.printStackTrace();

					throw exception;
				}

				_testToggleNames.add(toggleName);
			}
		}

		System.out.println("Active Toggles:");

		for (String testToggleName : _testToggleNames) {
			System.out.println("* " + testToggleName);
		}

		System.out.println();
	}

	private static void _storePathElement(
			Element rootElement, String className, String filePath,
			String namespace)
		throws Exception {

		_rootElements.put("path#" + namespace + "." + className, rootElement);

		List<String> locatorKeys = new ArrayList<>();

		Element bodyElement = rootElement.element("body");

		Element tableElement = bodyElement.element("table");

		Element tBodyElement = tableElement.element("tbody");

		List<Element> trElements = tBodyElement.elements("tr");

		for (Element trElement : trElements) {
			List<Element> tdElements = trElement.elements("td");

			Element locatorKeyElement = tdElements.get(0);

			String locatorKey = locatorKeyElement.getText();

			Element locatorElement = tdElements.get(1);

			String locator = locatorElement.getText();

			if (locatorKey.equals("") && locator.equals("")) {
				continue;
			}

			if (locatorKeys.contains(locatorKey)) {
				StringBuilder sb = new StringBuilder();

				sb.append("Duplicate path locator key '");
				sb.append(className + "#" + locatorKey);
				sb.append("' at namespace '");
				sb.append(namespace);
				sb.append("' \n");
				sb.append(filePath);
				sb.append(": ");
				sb.append(locatorKeyElement.attributeValue("line-number"));

				_duplicateLocatorMessages.add(sb.toString());
			}

			locatorKeys.add(locatorKey);

			if (locatorKey.equals("EXTEND_ACTION_PATH")) {
				_pathExtensions.put(namespace + "." + className, locator);
			}
			else {
				_pathLocators.put(
					namespace + "." + className + "#" + locatorKey, locator);
			}
		}
	}

	private static void _storeRootElement(
			Element rootElement, String filePath, String namespace)
		throws Exception {

		String className = PoshiRunnerGetterUtil.getClassNameFromFilePath(
			filePath);
		String classType = PoshiRunnerGetterUtil.getClassTypeFromFilePath(
			filePath);

		if (classType.equals("test-case")) {
			_testCaseNamespacedClassNames.add(namespace + "." + className);

			if (rootElement.element("set-up") != null) {
				Element setUpElement = rootElement.element("set-up");

				String classCommandName = className + "#set-up";

				_commandElements.put(
					classType + "#" + namespace + "." + classCommandName,
					setUpElement);
			}

			if (rootElement.element("tear-down") != null) {
				Element tearDownElement = rootElement.element("tear-down");

				String classCommandName = className + "#tear-down";

				_commandElements.put(
					classType + "#" + namespace + "." + classCommandName,
					tearDownElement);
			}
		}

		if (classType.equals("action") || classType.equals("function") ||
			classType.equals("macro") || classType.equals("test-case")) {

			_rootElements.put(
				classType + "#" + namespace + "." + className, rootElement);

			List<Element> commandElements = rootElement.elements("command");

			for (Element commandElement : commandElements) {
				String commandName = commandElement.attributeValue("name");

				String classCommandName = className + "#" + commandName;

				if (isCommandElement(classType, classCommandName, namespace)) {
					StringBuilder sb = new StringBuilder();

					sb.append("Duplicate command name '");
					sb.append(classCommandName);
					sb.append("' at namespace '");
					sb.append(namespace);
					sb.append("'\n");
					sb.append(filePath);
					sb.append(": ");
					sb.append(commandElement.attributeValue("line-number"));
					sb.append("\n");

					String duplicateElementFilePath = getFilePathFromFileName(
						PoshiRunnerGetterUtil.getFileNameFromFilePath(filePath),
						namespace);

					if (Validator.isNull(duplicateElementFilePath)) {
						duplicateElementFilePath = filePath;
					}

					sb.append(duplicateElementFilePath);
					sb.append(": ");

					Element duplicateElement = _commandElements.get(
						classType + "#" + namespace + "." + classCommandName);

					sb.append(duplicateElement.attributeValue("line-number"));

					_duplicateLocatorMessages.add(sb.toString());

					continue;
				}

				String namespacedClassCommandName =
					namespace + "." + className + "#" + commandName;

				_commandElements.put(
					classType + "#" + namespacedClassCommandName,
					commandElement);

				_commandSummaries.put(
					classType + "#" + namespacedClassCommandName,
					_getCommandSummary(
						classCommandName, classType, commandElement,
						rootElement));

				_commandReturns.put(
					classType + "#" + namespacedClassCommandName,
					_getCommandReturns(commandElement));

				if (classType.equals("test-case")) {
					Properties properties = _getClassCommandNameProperties(
						rootElement, commandElement);

					_namespacedClassCommandNamePropertiesMap.put(
						namespace + "." + classCommandName, properties);

					if (Validator.isNotNull(
							commandElement.attributeValue("description"))) {

						_testCaseDescriptions.put(
							namespacedClassCommandName,
							commandElement.attributeValue("description"));
					}
				}
			}

			if (classType.equals("function")) {
				String defaultClassCommandName =
					className + "#" + rootElement.attributeValue("default");

				Element defaultCommandElement = getFunctionCommandElement(
					defaultClassCommandName, namespace);

				_commandElements.put(
					classType + "#" + namespace + "." + className,
					defaultCommandElement);

				_commandSummaries.put(
					classType + "#" + namespace + "." + className,
					_getCommandSummary(
						defaultClassCommandName, classType,
						defaultCommandElement, rootElement));

				String xml = rootElement.asXML();

				for (int i = 1;; i++) {
					if (xml.contains("${locator" + i + "}")) {
						continue;
					}

					if (i > 1) {
						i--;
					}

					_functionLocatorCounts.put(namespace + "." + className, i);

					break;
				}
			}
		}
		else if (classType.equals("path")) {
			_storePathElement(rootElement, className, filePath, namespace);
		}
	}

	private static void _storeRootElements(List<URL> urls, String namespace)
		throws Exception {

		Map<String, String> filePaths = new HashMap<>();

		for (URL url : urls) {
			String filePath = url.getFile();

			if (OSDetector.isWindows()) {
				if (filePath.startsWith("/")) {
					filePath = filePath.substring(1);
				}

				filePath = filePath.replace("/", "\\");
			}

			String fileName = PoshiRunnerGetterUtil.getFileNameFromFilePath(
				filePath);

			if (filePaths.containsKey(fileName)) {
				System.out.println(
					"WARNING: Duplicate file name '" + fileName +
						"' found within the namespace '" + namespace + "':\n" +
							filePath + "\n" + filePaths.get(fileName) + "\n");
			}

			filePaths.put(fileName, filePath);

			_storeRootElement(
				PoshiRunnerGetterUtil.getRootElementFromURL(url), filePath,
				namespace);

			if (OSDetector.isWindows()) {
				if (filePath.startsWith("/")) {
					filePath = filePath.substring(1);
				}

				filePath = filePath.replace("/", "\\");
			}

			_filePaths.put(
				namespace + "." +
					PoshiRunnerGetterUtil.getFileNameFromFilePath(filePath),
				filePath);
		}
	}

	private static void _writeTestCaseMethodNamesProperties() throws Exception {
		StringBuilder sb = new StringBuilder();

		if ((PropsValues.TEST_BATCH_MAX_GROUP_SIZE > 0) &&
			(((PropsValues.TEST_BATCH_PROPERTY_NAMES != null) &&
			  (PropsValues.TEST_BATCH_PROPERTY_VALUES != null)) ||
			 (PropsValues.TEST_BATCH_PROPERTY_QUERY != null))) {

			sb.append(_getTestBatchGroups());
		}

		FileUtil.write("test.case.method.names.properties", sb.toString());
	}

	private static void _writeTestGeneratedProperties() throws Exception {
		StringBuilder sb = new StringBuilder();

		for (String testCaseNamespacedClassCommandName :
				_testCaseNamespacedClassCommandNames) {

			Properties properties =
				_namespacedClassCommandNamePropertiesMap.get(
					testCaseNamespacedClassCommandName);
			String testNamespacedClassName =
				PoshiRunnerGetterUtil.
					getNamespacedClassNameFromNamespacedClassCommandName(
						testCaseNamespacedClassCommandName);
			String testCommandName =
				PoshiRunnerGetterUtil.
					getCommandNameFromNamespacedClassCommandName(
						testCaseNamespacedClassCommandName);

			for (String propertyName : properties.stringPropertyNames()) {
				sb.append(testNamespacedClassName);
				sb.append("TestCase.test");
				sb.append(testCommandName);
				sb.append(".");
				sb.append(propertyName);
				sb.append("=");
				sb.append(properties.getProperty(propertyName));
				sb.append("\n");
			}
		}

		FileUtil.write("test.generated.properties", sb.toString());
	}

	private static final String _DEFAULT_NAMESPACE = "LocalFile";

	private static final String _TEST_BASE_DIR_NAME =
		PoshiRunnerGetterUtil.getCanonicalPath(PropsValues.TEST_BASE_DIR_NAME);

	private static final Map<String, Element> _commandElements =
		new HashMap<>();
	private static final Map<String, List<String>> _commandReturns =
		new HashMap<>();
	private static final Map<String, String> _commandSummaries =
		new HashMap<>();
	private static final Set<String> _duplicateLocatorMessages =
		new HashSet<>();
	private static final Map<String, String> _filePaths = new HashMap<>();
	private static final Map<String, Integer> _functionLocatorCounts =
		new HashMap<>();
	private static final Map<String, Properties>
		_namespacedClassCommandNamePropertiesMap = new HashMap<>();
	private static final List<String> _namespaces = new ArrayList<>();
	private static final Map<String, String> _pathExtensions = new HashMap<>();
	private static final Map<String, String> _pathLocators = new HashMap<>();
	private static final Pattern _poshiResourceJarNamePattern = Pattern.compile(
		"jar:.*\\/(?<namespace>\\w+)\\-(?<branchName>\\w+" +
			"(\\-\\w+)*)\\-(?<sha>\\w+)\\.jar.*");
	private static final Map<String, Element> _rootElements = new HashMap<>();
	private static final Map<String, Integer> _seleniumParameterCounts =
		new HashMap<>();
	private static final List<String> _testCaseAvailablePropertyNames =
		new ArrayList<>();
	private static final Map<String, String> _testCaseDescriptions =
		new HashMap<>();
	private static String _testCaseNamespacedClassCommandName;
	private static final List<String> _testCaseNamespacedClassCommandNames =
		new ArrayList<>();
	private static final List<String> _testCaseNamespacedClassNames =
		new ArrayList<>();
	private static final List<String> _testCaseRequiredPropertyNames =
		new ArrayList<>();
	private static final Set<String> _testToggleNames = new HashSet<>();
	private static final SimpleDateFormat _toggleDateFormat =
		new SimpleDateFormat("YYYY-MM-dd");

	static {
		String testCaseAvailablePropertyNames =
			PropsValues.TEST_CASE_AVAILABLE_PROPERTY_NAMES;

		if (Validator.isNotNull(testCaseAvailablePropertyNames)) {
			Collections.addAll(
				_testCaseAvailablePropertyNames,
				StringUtil.split(testCaseAvailablePropertyNames));
		}

		_testCaseAvailablePropertyNames.add("known-issues");
		_testCaseAvailablePropertyNames.add("priority");
		_testCaseAvailablePropertyNames.add("test.run.environment");

		String testCaseRequiredPropertyNames =
			PropsValues.TEST_CASE_REQUIRED_PROPERTY_NAMES;

		if (Validator.isNotNull(testCaseRequiredPropertyNames)) {
			Collections.addAll(
				_testCaseRequiredPropertyNames,
				StringUtil.split(testCaseRequiredPropertyNames));
		}
	}

}