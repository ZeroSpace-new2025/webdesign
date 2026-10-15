package com.university.webdesign.event;

import java.time.YearMonth;

/**
 * 月度报表生成完成事件（M4 发布）。
 * <p>
 * 对应《对外方法表》5.3：通知/消息推送（可扩展）。
 *
 * @param month    月份
 * @param reportId 报表ID
 */
public record MonthlyReportGeneratedEvent(YearMonth month, Long reportId)
{
}
