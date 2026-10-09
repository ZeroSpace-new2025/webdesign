package com.university.webdesign.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 闭区间日期范围。
 * <p>
 * 供各模块的查询入参复用：{@code OrderQueryService.pageHistory(employeeId, DateRange, PageQuery)}
 * 等签名直接使用本类型。
 */
public record DateRange(LocalDate from, LocalDate to)
{
	/**
	 * 不限范围
	 *
	 * @return 空范围
	 */
	public static DateRange unbounded() {
		return new DateRange(null, null);
	}

	/**
	 * 单日范围
	 *
	 * @param date 日期
	 * @return 该日闭区间
	 */
	public static DateRange ofDay(LocalDate date) {
		return new DateRange(date, date);
	}

	/**
	 * 按自然月构造范围
	 *
	 * @param month 月份
	 * @return 该月首日到末日
	 */
	public static DateRange ofMonth(java.time.YearMonth month) {
		return new DateRange(month.atDay(1), month.atEndOfMonth());
	}

	/**
	 * 起始时刻（含），为空时返回 null
	 *
	 * @return 当天 00:00
	 */
	public LocalDateTime startDateTime() {
		return from == null ? null : from.atStartOfDay();
	}

	/**
	 * 结束时刻（不含），为空时返回 null
	 *
	 * @return 次日 00:00
	 */
	public LocalDateTime endDateTimeExclusive() {
		return to == null ? null : to.plusDays(1).atStartOfDay();
	}

	/**
	 * 是否无边界
	 *
	 * @return 起止均为空时返回 true
	 */
	public boolean isEmpty() {
		return from == null && to == null;
	}
}
