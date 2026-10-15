package com.university.webdesign.event;

import java.time.LocalDate;

/**
 * 订单变更事件（M2 发布，M3/M4 订阅）。
 * <p>
 * 对应《对外方法表》3.3：修改/取消/作废后触发；运营履约据此重算当日快照，报表据此失效缓存。
 *
 * @param orderId        订单ID
 * @param employeeId     员工ID
 * @param orderDate      就餐日期
 * @param changeType     变更类型：MODIFIED / CANCELLED / INVALIDATED
 * @param previousStatus 变更前状态编码
 * @param currentStatus  变更后状态编码
 */
public record OrderUpdatedEvent(
		Long orderId,
		Long employeeId,
		LocalDate orderDate,
		String changeType,
		String previousStatus,
		String currentStatus)
{
	/**
	 * 变更类型常量
	 */
	public static final String MODIFIED = "MODIFIED";

	/**
	 * 变更类型常量
	 */
	public static final String CANCELLED = "CANCELLED";

	/**
	 * 变更类型常量
	 */
	public static final String INVALIDATED = "INVALIDATED";
}
