package com.university.webdesign.service.operation.dto;

/**
 * 生产单打印格式。
 * <p>
 * 对应《对外方法表》M3-03 的 `format=PDF|TXT|PRINT`。当前实现支持 TXT（纯文本，可直接送打印）
 * 与 XLSX（SpreadsheetML 2003 XML）；PDF 未引入生成库，请求时按 40001 返回提示。
 */
public enum PrintFormat
{
	/**
	 * 纯文本（可直接推打印机）
	 */
	TXT,

	/**
	 * Excel 工作簿
	 */
	XLSX,

	/**
	 * 直连打印机（当前等同 TXT 文本，由前端/打印服务接手）
	 */
	PRINT,

	/**
	 * PDF（当前未引入生成库）
	 */
	PDF;

	/**
	 * 解析打印格式，缺省为 TXT
	 *
	 * @param format 格式字符串，忽略大小写
	 * @return 打印格式
	 */
	public static PrintFormat parse(String format) {
		if (format == null || format.isBlank()) {
			return TXT;
		}
		for (PrintFormat value : values()) {
			if (value.name().equalsIgnoreCase(format.trim())) {
				return value;
			}
		}
		throw new com.university.webdesign.common.BusinessException(
				com.university.webdesign.common.ErrorCode.PARAM_INVALID,
				"不支持的打印格式：" + format + "（可选 TXT / XLSX / PRINT）");
	}
}
