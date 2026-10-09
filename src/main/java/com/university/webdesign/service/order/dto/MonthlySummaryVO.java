package com.university.webdesign.service.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 个人月度消费统计。
 * <p>
 * 对应《对外方法表》M2-09：`orderCount`、`totalAmount`、`categoryBreakdown[]`。
 */
@Data
public class MonthlySummaryVO
{
	/**
	 * 员工ID
	 */
	private Long employeeId;

	/**
	 * 员工姓名（可能为空）
	 */
	private String employeeName;

	/**
	 * 月份（`yyyy-MM`）
	 */
	private String month;

	/**
	 * 当月有效订单数
	 */
	private int orderCount;

	/**
	 * 当月消费总额
	 */
	private BigDecimal totalAmount = BigDecimal.ZERO;

	/**
	 * 按菜品分类的消费占比
	 */
	private List<CategoryBreakdownVO> categoryBreakdown = new ArrayList<>();

	/**
	 * 按菜品的消费明细汇总
	 */
	private List<DishSummaryVO> dishBreakdown = new ArrayList<>();

	/**
	 * 分类维度汇总
	 */
	@Data
	public static class CategoryBreakdownVO
	{
		/**
		 * 分类名
		 */
		private String category;

		/**
		 * 该分类的菜品总数量
		 */
		private int quantity;

		/**
		 * 该分类的消费金额
		 */
		private BigDecimal amount = BigDecimal.ZERO;

		/**
		 * 占当月总额的百分比（保留一位小数）
		 */
		private BigDecimal percentage = BigDecimal.ZERO;
	}

	/**
	 * 菜品维度汇总
	 */
	@Data
	public static class DishSummaryVO
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
		 * 分类（快照）
		 */
		private String category;

		/**
		 * 数量
		 */
		private int quantity;

		/**
		 * 金额
		 */
		private BigDecimal amount = BigDecimal.ZERO;
	}
}
