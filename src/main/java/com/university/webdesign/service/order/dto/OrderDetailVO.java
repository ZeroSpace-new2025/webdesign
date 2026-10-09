package com.university.webdesign.service.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细视图对象。
 * <p>
 * 全部字段都是**下单时刻的快照**：菜谱后续改名、调价、下架都不会改变这里的数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailVO
{
	/**
	 * 明细ID
	 */
	private Long detailId;

	/**
	 * 菜品（菜谱）ID
	 */
	private Long recipeId;

	/**
	 * 下单时的菜名
	 */
	private String recipeName;

	/**
	 * 下单时的菜品分类（供生产单按分类汇总）
	 */
	private String category;

	/**
	 * 下单时的计量单位
	 */
	private String unit;

	/**
	 * 下单时的单价
	 */
	private BigDecimal unitPrice;

	/**
	 * 数量
	 */
	private Integer quantity;

	/**
	 * 小计金额
	 */
	private BigDecimal amount;
}
