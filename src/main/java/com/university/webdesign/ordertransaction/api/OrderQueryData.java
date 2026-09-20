package com.university.webdesign.ordertransaction.api;

import lombok.Data;

@Data
public class OrderQueryData
{
	/**
	 * 订单ID
	 */
	private Long orderId;
	
	/**
	 * 用户ID
	 */
	private Long userId;
	
	/**
	 * 订单状态
	 */
	private String orderStatus;
	
	/**
	 * 订单创建时间范围
	 */
	private Long startTime;
	
	/**
	 * 订单创建时间范围
	 */
	private Long endTime;
}
