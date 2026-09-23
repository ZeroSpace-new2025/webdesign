package com.university.webdesign.ordertransaction.api;

import lombok.Data;

/**
 * 订单查询条件
 * <p>
 * 字段之间是“与”关系，为 null 的条件不参与过滤；时间范围均使用毫秒时间戳。
 */
@Data
public class OrderQueryData
{
	/**
	 * 订单ID（主键）
	 */
	private Long orderId;
	
	/**
	 * 订单号
	 */
	private Long orderNumber;
	
	/**
	 * 用户ID
	 */
	private Long userId;
	
	/**
	 * 订单状态（中文，见 {@code OrderStatus}）
	 */
	private String orderStatus;
	
	/**
	 * 下单时间范围起点（毫秒时间戳，含）
	 */
	private Long startTime;
	
	/**
	 * 下单时间范围终点（毫秒时间戳，含）
	 */
	private Long endTime;
	
	/**
	 * 是否包含已取消订单，默认 false（只查有效订单）
	 */
	private Boolean includeCancelled;
}
