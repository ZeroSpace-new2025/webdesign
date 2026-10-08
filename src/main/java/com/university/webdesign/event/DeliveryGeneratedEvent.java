package com.university.webdesign.event;

import java.time.LocalDate;
import java.util.List;

/**
 * 配送任务生成完成事件（M3 发布）。
 * <p>
 * 对应《对外方法表》4.3：通知前端刷新与打印队列。
 *
 * @param date      配送日期
 * @param taskIds   本次生成的任务ID列表
 * @param taskCount 生成数量
 */
public record DeliveryGeneratedEvent(LocalDate date, List<Long> taskIds, int taskCount)
{
}
