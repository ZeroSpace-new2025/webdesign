package com.university.webdesign.service.operation.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类维度金额汇总。
 * <p>
 * 对应《对外方法表》M3-04 `sumByCategory`：只到分类粒度的总量与金额。
 */
@Data
public class CategorySumVO
{
	/**
	 * 分类名
	 */
	private String category;

	/**
	 * 该分类涉及菜品数
	 */
	private int dishCount;

	/**
	 * 该分类的需求总数量
	 */
	private int totalQuantity;

	/**
	 * 该分类的金额合计
	 */
	private BigDecimal totalAmount = BigDecimal.ZERO;
}
