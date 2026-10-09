package com.university.webdesign.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import org.springframework.web.multipart.MultipartFile;
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
 * 员工批量导入的表格解析器（不依赖数据库与 Office 组件）。
 * <p>
 * 由重构前的 {@code user.service.UserImportParser} 迁移而来，改为读取
 * {@link MultipartFile}，并统一抛 {@link BusinessException}（40001）而不是
 * {@code IllegalArgumentException}。
 * <p>
 * 支持三种输入：
 * <ul>
 *     <li>{@code .csv} / {@code .txt}：UTF-8 文本，支持双引号包裹的逗号；</li>
 *     <li>{@code .xlsx}：XLSX 本质是 ZIP，这里只读取共享字符串与第一个工作表，
 *         足够覆盖“员工导入”场景，且无需引入 EasyExcel / POI 依赖。</li>
 * </ul>
 * 离线环境取不到新依赖，因此**不要**换成第三方 Excel 库。
 */
final class UserImportParser
{
	private UserImportParser() {
	}

	/**
	 * 读取表格全部行（含表头行）
	 *
	 * @param file 上传文件
	 * @return 行 → 单元格列表
	 */
	static List<List<String>> readRows(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择导入文件");
		}
		String fileName = file.getOriginalFilename() == null
				? ""
				: file.getOriginalFilename().toLowerCase(Locale.ROOT);
		try {
			byte[] bytes = file.getBytes();
			if (fileName.endsWith(".xlsx")) {
				return readXlsx(bytes);
			}
			if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
				return readCsv(new String(bytes, StandardCharsets.UTF_8));
			}
		} catch (IOException exception) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "导入文件读取失败，请重试", exception);
		}
		throw new BusinessException(ErrorCode.PARAM_INVALID, "仅支持 .xlsx 或 .csv 文件");
	}

	/**
	 * CSV 按行解析，并保留双引号中的逗号
	 *
	 * @param content 文本内容
	 * @return 行 → 单元格列表
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

	/**
	 * 解析一行 CSV
	 *
	 * @param line 行文本
	 * @return 单元格列表
	 */
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
	 * XLSX 本质是 ZIP 包，只读取共享字符串和第一个工作表
	 *
	 * @param bytes 文件字节
	 * @return 行 → 单元格列表
	 * @throws IOException 解析失败
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
				.orElseThrow(() -> new BusinessException(ErrorCode.PARAM_INVALID, "Excel 中未找到工作表"));
		return parseWorksheet(sheet, sharedStrings);
	}

	/**
	 * XLSX 会把重复字符串放在 sharedStrings.xml 中，再由单元格索引引用
	 *
	 * @param bytes 文件字节
	 * @return 共享字符串
	 * @throws IOException 解析失败
	 */
	private static List<String> parseSharedStrings(byte[] bytes) throws IOException {
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

	/**
	 * 根据单元格的 r 属性（如 C5）还原列位置，补齐空单元格
	 *
	 * @param bytes         工作表字节
	 * @param sharedStrings 共享字符串
	 * @return 行 → 单元格列表
	 * @throws IOException 解析失败
	 */
	private static List<List<String>> parseWorksheet(byte[] bytes, List<String> sharedStrings)
			throws IOException {
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

	/**
	 * 读取单个单元格：t=s 表示共享字符串，t=inlineStr 表示内联文本
	 *
	 * @param cell          单元格节点
	 * @param sharedStrings 共享字符串
	 * @return 单元格文本
	 */
	private static String readCell(Element cell, List<String> sharedStrings) {
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

	/**
	 * 安全地解析 XML（禁用 DTD 与外部实体）
	 *
	 * @param bytes 文件字节
	 * @return 文档
	 * @throws IOException 解析失败
	 */
	private static Document parseXml(byte[] bytes) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "Excel 文件解析失败", exception);
		}
	}

	/**
	 * 取元素内的全部文本节点
	 *
	 * @param element 元素
	 * @return 文本
	 */
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

	/**
	 * Excel 列名（A、B、…、AA）转换为从 0 开始的列下标
	 *
	 * @param reference 单元格引用，如 {@code C5}
	 * @return 列下标
	 */
	private static int columnIndex(String reference) {
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

	/**
	 * 归一化单元格文本：去空白，并把 Excel 数字格式的 {@code 123.0} 还原为 {@code 123}
	 *
	 * @param value 原始文本
	 * @return 归一化文本
	 */
	private static String normalizeCell(String value) {
		String normalized = value == null ? "" : value.trim();
		if (normalized.matches("-?\\d+\\.0+")) {
			return normalized.substring(0, normalized.indexOf('.'));
		}
		return normalized;
	}
}
