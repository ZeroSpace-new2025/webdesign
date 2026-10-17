package com.university.webdesign.reporting.api;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 员工消费订单明细。
 */
@Data
public class EmployeeConsumptionDetailDTO
{
	/**
	 * 订单ID
	 */
	private Long orderId;

	/**
	 * 订单创建时间
	 */
	private long createdTime;

	/**
	 * 订单金额
	 */
	private BigDecimal totalAmount;

	/**
	 * 订单项快照
	 */
	private List<OrderItemSnapshotDTO> items;
}
