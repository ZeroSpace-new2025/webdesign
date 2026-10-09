package com.university.webdesign.common;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 表格文件导出工具。
 * <p>
 * 对应《对外方法表》M2-10 / M3-03 / M3-12 / M4-23 的文件流导出需求。
 * 项目运行在离线环境，不引入 POI / EasyExcel 等第三方依赖，因此：
 * <ul>
 *     <li>{@link #csv} 输出带 UTF-8 BOM 的 CSV，Excel 可直接双击打开；</li>
 *     <li>{@link #xlsx} 输出 SpreadsheetML 2003 XML，扩展名用 {@code .xls}，Excel 可正常识别。</li>
 * </ul>
 * 需要真正的二进制 XLSX 时再统一引入 XlsxWriter/POI，替换本类即可。
 */
public final class TabularExport
{
	private TabularExport() {
	}

	/**
	 * 生成 CSV 资源
	 *
	 * @param headers 表头
	 * @param rows    数据行
	 * @return 文件资源
	 */
	public static Resource csv(List<String> headers, List<List<String>> rows) {
		StringBuilder builder = new StringBuilder("\uFEFF");
		builder.append(join(headers)).append("\r\n");
		for (List<String> row : rows) {
			builder.append(join(row)).append("\r\n");
		}
		return new ByteArrayResource(builder.toString().getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 生成 SpreadsheetML 2003 XML 工作簿资源
	 *
	 * @param sheetName 工作表名
	 * @param headers   表头
	 * @param rows      数据行
	 * @return 文件资源
	 */
	public static Resource xlsx(String sheetName, List<String> headers, List<List<String>> rows) {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
		xml.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
				.append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
		xml.append("<Worksheet ss:Name=\"").append(escape(sheetName)).append("\">\n<Table>\n");
		xml.append(row(headers, true));
		for (List<String> line : rows) {
			xml.append(row(line, false));
		}
		xml.append("</Table>\n</Worksheet>\n</Workbook>\n");
		return new ByteArrayResource(xml.toString().getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 生成纯文本资源（生产单/配送单直接送打印机的场景）
	 *
	 * @param text 文本内容
	 * @return 文件资源
	 */
	public static Resource text(String text) {
		return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
	}

	private static String row(List<String> values, boolean header) {
		StringBuilder builder = new StringBuilder("<Row>");
		for (String value : values) {
			builder.append("<Cell><Data ss:Type=\"String\">")
					.append(escape(value))
					.append("</Data></Cell>");
		}
		builder.append("</Row>\n");
		return builder.toString();
	}

	private static String join(List<String> values) {
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < values.size(); index++) {
			if (index > 0) {
				builder.append(',');
			}
			String value = values.get(index) == null ? "" : values.get(index);
			if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
				builder.append('"').append(value.replace("\"", "\"\"")).append('"');
			} else {
				builder.append(value);
			}
		}
		return builder.toString();
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}
}
