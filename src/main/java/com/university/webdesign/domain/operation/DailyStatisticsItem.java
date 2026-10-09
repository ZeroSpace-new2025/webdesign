package com.university.webdesign.domain.operation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 每日汇总明细（`statistics_item` 子表）。
 * <p>
 * 对应《重构实施规范》第 3 节：`category` / `recipe_id` / `recipe_name` / `unit` /
 * `quantity` / `amount`，是 `daily_statistics` 的 {@code @ElementCollection} 元素。
 * 字段值全部来自下单时刻的明细快照，菜谱后续改名、调价、下架都不会影响这里的数据。
 */
@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatisticsItem implements Serializable
{
	/**
	 * 分类名（下单时的分类快照）
	 */
	@Column(name = "category", length = 60)
	private String category;

	/**
	 * 菜品（菜谱）ID
	 */
	@Column(name = "recipe_id")
	private Long recipeId;

	/**
	 * 菜名（下单时的菜名快照）
	 */
	@Column(name = "recipe_name", length = 120)
	private String recipeName;

	/**
	 * 计量单位（份/两/个）
	 */
	@Column(name = "unit", length = 20)
	private String unit;

	/**
	 * 该菜品的需求总数量
	 */
	@Column(name = "quantity", nullable = false)
	private int quantity;

	/**
	 * 该菜品的金额合计
	 */
	@Column(name = "amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal amount = BigDecimal.ZERO;

	/**
	 * 涉及订单数
	 */
	@Column(name = "order_count", nullable = false)
	private int orderCount;
}
