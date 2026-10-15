package com.university.webdesign.service.operation.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类维度汇总条目。
 * <p>
 * 对应《对外方法表》M3-02 / M3-04：输出“分类 → 菜品 → 总量/单位”，供厨房备料与生产单打印。
 * 同一个分类下的每一条记录代表一个菜品。
 */
@Data
public class CategoryStatVO
{
	/**
	 * 分类名（来自下单时的分类快照）
	 */
	private String category;

	/**
	 * 菜品（菜谱）ID
	 */
	private Long recipeId;

	/**
	 * 菜名（来自下单时的菜名快照）
	 */
	private String recipeName;

	/**
	 * 计量单位（来自快照），如“份”“两”
	 */
	private String unit;

	/**
	 * 该菜品的需求总数量
	 */
	private int totalQuantity;

	/**
	 * 该菜品的金额合计
	 */
	private BigDecimal totalAmount = BigDecimal.ZERO;

	/**
	 * 涉及订单数
	 */
	private int orderCount;
}
