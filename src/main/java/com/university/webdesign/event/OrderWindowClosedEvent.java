package com.university.webdesign.event;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 订餐窗口关闭事件（M2/M3 发布，M3 聚合订阅）。
 * <p>
 * 对应《对外方法表》3.3 与 4.3：定时任务 {@code OrderWindowCloseJob} 在到达订餐截止时间时发布，
 * 触发运营履约聚合当日总括订单与生产单数据。
 *
 * @param date        截止日期（当日）
 * @param cutoffTime  生效的订餐截止时间
 */
public record OrderWindowClosedEvent(LocalDate date, LocalTime cutoffTime)
{
}
