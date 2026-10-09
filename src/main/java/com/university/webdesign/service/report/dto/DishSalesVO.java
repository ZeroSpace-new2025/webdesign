package com.university.webdesign.service.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜品销售明细。
 * <p>
 * 对应《对外方法表》M4-21 / M4-22 的 `items[{recipeName, quantity, amount}]`，
 * 同时复用于员工消费明细中的订单菜品快照。
 * <p>
 * 全部字段都来自**下单时刻的快照**：菜谱后续改名、调价、下架都不会改变历史报表。
 */
@Data
public class DishSalesVO
{
	/**
	 * 菜品（菜谱）ID
	 */
	private Long recipeId;

	/**
	 * 下单时的菜名快照
	 */
	private String recipeName;

	/**
	 * 下单时的菜品分类快照
	 */
	private String category;

	/**
	 * 售出数量
	 */
	private Long quantity;

	/**
	 * 销售金额
	 */
	private BigDecimal amount;
}
