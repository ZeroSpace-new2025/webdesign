package com.university.webdesign.ordertransaction.api;

import lombok.Data;

import java.util.List;

/**
 * 订单DTO
 */
@Data
public class OrderDTO
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
	 * 订单创建时间
	 */
	private long createdTime;
	
	/**
	 * 订单最后修改时间
	 */
	private long lastModifiedTime;
	
	/**
	 * 订单状态
	 */
	private String status;
	
	/**
	 * 订单项ID列表
	 */
	private List<Long> itemIds;
}
