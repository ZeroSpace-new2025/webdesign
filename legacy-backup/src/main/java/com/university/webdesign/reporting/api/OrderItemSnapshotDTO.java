package com.university.webdesign.reporting.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 报表使用的订单项快照。
 */
@Data
public class OrderItemSnapshotDTO
{
	/**
	 * 菜品ID
	 */
	private Long recipeId;

	/**
	 * 下单时的菜品名称
	 */
	private String recipeName;

	/**
	 * 下单时的单价
	 */
	private BigDecimal unitPrice;

	/**
	 * 数量
	 */
	private int quantity;

	/**
	 * 金额小计
	 */
	private BigDecimal amount;
}
