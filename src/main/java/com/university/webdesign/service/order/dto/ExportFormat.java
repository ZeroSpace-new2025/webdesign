package com.university.webdesign.service.order.dto;

/**
 * 消费明细导出格式。
 * <p>
 * 对应《对外方法表》M2-10 的 `format=PDF|XLSX`。当前实现支持 CSV 与 XLSX（以 XML 表格形式写出，
 * 不引入第三方依赖）；PDF 需要额外库，请求时按参数校验失败（40001）返回。
 */
public enum ExportFormat
{
	/**
	 * Excel 工作簿（SpreadsheetML 2003 XML，Excel 可直接打开）
	 */
	XLSX,

	/**
	 * 逗号分隔文本
	 */
	CSV,

	/**
	 * PDF（当前未引入生成库）
	 */
	PDF;

	/**
	 * 解析导出格式，缺省为 XLSX
	 *
	 * @param format 格式字符串，忽略大小写
	 * @return 导出格式
	 */
	public static ExportFormat parse(String format) {
		if (format == null || format.isBlank()) {
			return XLSX;
		}
		for (ExportFormat value : values()) {
			if (value.name().equalsIgnoreCase(format.trim())) {
				return value;
			}
		}
		throw new com.university.webdesign.common.BusinessException(
				com.university.webdesign.common.ErrorCode.PARAM_INVALID,
				"不支持的导出格式：" + format + "（可选 XLSX / CSV）");
	}
}
