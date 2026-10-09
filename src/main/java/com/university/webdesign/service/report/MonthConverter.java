package com.university.webdesign.service.report;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * 月份文本与 {@link YearMonth} 的转换工具。
 * <p>
 * api 层的 `month` 统一用 `yyyy-MM` 字符串传输（例如 {@code 2026-01}），
 * service 层再转成 {@link YearMonth}；同时兼容误传的 `yyyy-MM-dd` 写法。
 */
public final class MonthConverter
{
	private MonthConverter() {
	}

	/**
	 * 解析月份文本
	 *
	 * @param month `yyyy-MM`（兼容 `yyyy-MM-dd`）
	 * @return 月份
	 */
	public static YearMonth parse(String month) {
		if (month == null || month.isBlank()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "月份不能为空，格式 yyyy-MM");
		}
		String normalized = month.trim();
		if (normalized.length() > 7) {
			normalized = normalized.substring(0, 7);
		}
		try {
			return YearMonth.parse(normalized);
		} catch (DateTimeParseException exception) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"月份格式不正确，应为 yyyy-MM：" + month, exception);
		}
	}

	/**
	 * 格式化为 `yyyy-MM`
	 *
	 * @param month 月份
	 * @return 文本；入参为空返回 null
	 */
	public static String format(YearMonth month) {
		return month == null ? null : month.toString();
	}
}
