package com.university.webdesign.domain.report;

import jakarta.persistence.Converter;

import jakarta.persistence.AttributeConverter;
import java.time.YearMonth;

/**
 * {@link YearMonth} 与数据库 `yyyy-MM` 字符串的转换器。
 * <p>
 * `monthly_report.report_month` 在 PostgreSQL / H2 上统一存为 `yyyy-MM` 短字符串，
 * 既可读又可比较；实体侧仍使用 {@link YearMonth}，由本转换器负责边界转换。
 */
@Converter(autoApply = false)
public class ReportMonthConverter implements AttributeConverter<YearMonth, String>
{
	/**
	 * 实体属性 → 数据库列
	 *
	 * @param attribute 月份
	 * @return `yyyy-MM` 字符串；入参为空时返回 null
	 */
	@Override
	public String convertToDatabaseColumn(YearMonth attribute) {
		return attribute == null ? null : attribute.toString();
	}

	/**
	 * 数据库列 → 实体属性
	 *
	 * @param dbData `yyyy-MM` 字符串
	 * @return 月份；入参为空时返回 null
	 */
	@Override
	public YearMonth convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return null;
		}
		return YearMonth.parse(dbData.trim());
	}
}
