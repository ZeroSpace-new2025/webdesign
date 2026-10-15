package com.university.webdesign.domain.operation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 配送任务明细（`delivery_task_item` 子表）。
 * <p>
 * 对应《重构实施规范》第 3 节：`recipe_name` / `quantity` / `unit`
 * （另补 `recipe_id` 便于排序与去重），是 {@link DeliveryTask} 的
 * {@code @ElementCollection} 元素，字段值取自订单明细快照。
 */
@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTaskItem implements Serializable
{
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
	 * 分量
	 */
	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	/**
	 * 计量单位（份/两/个）
	 */
	@Column(name = "unit", length = 20)
	private String unit;
}
