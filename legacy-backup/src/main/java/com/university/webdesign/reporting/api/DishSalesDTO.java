package com.university.webdesign.reporting.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜品月度销售明细。
 */
@Data
public class DishSalesDTO
{
	/**
	 * 菜品ID
	 */
	private Long recipeId;

	/**
	 * 下单时的菜品名称快照
	 */
	private String recipeName;

	/**
	 * 菜品分类快照
	 */
	private String category;

	/**
	 * 售出数量
	 */
	private long quantity;

	/**
	 * 销售金额
	 */
	private BigDecimal salesAmount;
}
