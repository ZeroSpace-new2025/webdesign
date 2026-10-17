package com.university.webdesign.reporting.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * 餐厅月度销售总报表。
 */
@Data
public class MonthlySalesReportDTO
{
	/**
	 * 统计月份
	 */
	private YearMonth month;

	/**
	 * 有效订单数量
	 */
	private int orderCount;

	/**
	 * 售出菜品总数量
	 */
	private long totalQuantity;

	/**
	 * 销售总金额
	 */
	private BigDecimal totalAmount;

	/**
	 * 菜品销售明细
	 */
	private List<DishSalesDTO> items;

	/**
	 * 报表生成时间
	 */
	private long generatedTime;
}
