package com.university.webdesign.user.service;

import com.university.webdesign.user.api.UserImportData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 读取CSV或简单XLSX表格，不依赖数据库和额外Office组件。
 */
final class UserImportParser
{
	private UserImportParser() {
	}

	static List<List<String>> readRows(UserImportData importData) throws IOException {
		if (importData == null || importData.getInputStream() == null) {
			throw new IllegalArgumentException("请选择导入文件");
		}
		String fileName = importData.getFileName() == null
			? ""
			: importData.getFileName().toLowerCase(Locale.ROOT);
		if (fileName.endsWith(".xlsx")) {
			return readXlsx(importData.getInputStream().readAllBytes());
		}
		if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
			return readCsv(new String(importData.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
		}
		throw new IllegalArgumentException("仅支持 .xlsx 或 .csv 文件");
	}

	/**
	 * CSV按行解析，并保留双引号中的逗号。
	 */
	private static List<List<String>> readCsv(String content) {
		String normalized = content.startsWith("\uFEFF") ? content.substring(1) : content;
		List<List<String>> rows = new ArrayList<>();
		for (String line : normalized.split("\\r?\\n")) {
			if (!line.isBlank()) {
				rows.add(parseCsvLine(line));
			}
		}
		return rows;
	}

	private static List<String> parseCsvLine(String line) {
		List<String> cells = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int index = 0; index < line.length(); index++) {
			char character = line.charAt(index);
			if (character == '"') {
				if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
					current.append('"');
					index++;
				} else {
					quoted = !quoted;
				}
			} else if (character == ',' && !quoted) {
				cells.add(normalizeCell(current.toString()));
				current.setLength(0);
			} else {
				current.append(character);
			}
		}
		cells.add(normalizeCell(current.toString()));
		return cells;
	}

	/**
	 * XLSX本质是ZIP包，只读取共享字符串和第一个工作表即可满足员工导入。
	 */
	private static List<List<String>> readXlsx(byte[] bytes) throws IOException {
		Map<String, byte[]> entries = new LinkedHashMap<>();
		try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					entries.put(entry.getName(), zipInputStream.readAllBytes());
				}
			}
		}
		List<String> sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
		byte[] sheet = entries.entrySet().stream()
			.filter(item -> item.getKey().matches("xl/worksheets/sheet\\d+\\.xml"))
			.map(Map.Entry::getValue)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Excel中未找到工作表"));
		return parseWorksheet(sheet, sharedStrings);
	}

	private static List<String> parseSharedStrings(byte[] bytes) throws IOException {
		// XLSX会把重复字符串放在sharedStrings.xml中，再由单元格索引引用。
		List<String> values = new ArrayList<>();
		if (bytes == null) {
			return values;
		}
		Document document = parseXml(bytes);
		NodeList items = document.getElementsByTagName("si");
		for (int index = 0; index < items.getLength(); index++) {
			values.add(textContent((Element) items.item(index)));
		}
		return values;
	}

	private static List<List<String>> parseWorksheet(byte[] bytes, List<String> sharedStrings) throws IOException {
		// 根据单元格的r属性（如C5）还原列位置，补齐空单元格。
		List<List<String>> rows = new ArrayList<>();
		Document document = parseXml(bytes);
		NodeList rowNodes = document.getElementsByTagName("row");
		for (int rowIndex = 0; rowIndex < rowNodes.getLength(); rowIndex++) {
			Element rowElement = (Element) rowNodes.item(rowIndex);
			List<String> cells = new ArrayList<>();
			NodeList cellNodes = rowElement.getElementsByTagName("c");
			for (int cellIndex = 0; cellIndex < cellNodes.getLength(); cellIndex++) {
				Element cell = (Element) cellNodes.item(cellIndex);
				int columnIndex = columnIndex(cell.getAttribute("r"));
				while (cells.size() <= columnIndex) {
					cells.add("");
				}
				cells.set(columnIndex, readCell(cell, sharedStrings));
			}
			if (cells.stream().anyMatch(value -> !value.isBlank())) {
				rows.add(cells);
			}
		}
		return rows;
	}

	private static String readCell(Element cell, List<String> sharedStrings) {
		// t=s表示共享字符串，t=inlineStr表示直接写在单元格中的文本。
		String type = cell.getAttribute("t");
		if ("inlineStr".equals(type)) {
			return normalizeCell(textContent(cell));
		}
		NodeList valueNodes = cell.getElementsByTagName("v");
		if (valueNodes.getLength() == 0) {
			return "";
		}
		String value = valueNodes.item(0).getTextContent();
		if ("s".equals(type)) {
			int index;
			try {
				index = Integer.parseInt(value);
			} catch (NumberFormatException exception) {
				return "";
			}
			return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
		}
		return normalizeCell(value);
	}

	private static Document parseXml(byte[] bytes) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
		} catch (Exception exception) {
			throw new IOException("Excel文件解析失败", exception);
		}
	}

	private static String textContent(Element element) {
		NodeList textNodes = element.getElementsByTagName("t");
		if (textNodes.getLength() == 0) {
			String value = element.getTextContent();
			return normalizeCell(value == null ? "" : value);
		}
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < textNodes.getLength(); index++) {
			Node node = textNodes.item(index);
			builder.append(node.getTextContent());
		}
		return normalizeCell(builder.toString());
	}

	private static int columnIndex(String reference) {
		// Excel列名使用A、B、...、AA表示，转换为从0开始的列下标。
		int result = 0;
		for (int index = 0; index < reference.length(); index++) {
			char character = reference.charAt(index);
			if (!Character.isLetter(character)) {
				break;
			}
			result = result * 26 + (Character.toUpperCase(character) - 'A' + 1);
		}
		return Math.max(0, result - 1);
	}

	private static String normalizeCell(String value) {
		String normalized = value == null ? "" : value.trim();
		if (normalized.matches("-?\\d+\\.0+")) {
			return normalized.substring(0, normalized.indexOf('.'));
		}
		return normalized;
	}
}
