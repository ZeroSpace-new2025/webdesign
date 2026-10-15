package com.university.webdesign.domain.report;

/**
 * 用户与报表中心（M4）——报表领域包。
 * <p>
 * 实体：{@link MonthlyReport}（`monthly_report`）与 {@link MonthlyReportItem}
 * （`monthly_report_item`），是月度销售报表的缓存表；
 * {@link ReportMonthConverter} 负责 {@code YearMonth} 与 `yyyy-MM` 字符串的转换。
 * <p>
 * 本包只描述报表自身的数据结构：订单数据一律经
 * {@code com.university.webdesign.service.order.OrderQueryService} 读取，不在此建订单实体。
 */
