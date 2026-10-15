package com.university.webdesign.service.operation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配送单明细。
 * <p>
 * 对应《对外方法表》M3-07：按员工/工位维度拆分订单，明细取订单详情快照。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryItemVO
{
	/**
	 * 菜品ID
	 */
	private Long recipeId;

	/**
	 * 菜名（快照）
	 */
	private String recipeName;

	/**
	 * 分量
	 */
	private Integer quantity;

	/**
	 * 计量单位（快照）
	 */
	private String unit;
}
