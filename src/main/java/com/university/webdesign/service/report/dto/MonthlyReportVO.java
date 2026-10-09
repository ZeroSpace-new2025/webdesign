package com.university.webdesign.service.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 月度销售报表视图。
 * <p>
 * 对应《对外方法表》M4-22 `GET /reports/monthly` 的返回：
 * `totalAmount`、`totalQuantity`、`items[{recipeName, quantity, amount}]`，
 * 并额外带上报表ID、月份、订单数与生成时间，便于前端展示与归档。
 */
@Data
public class MonthlyReportVO
{
	/**
	 * 报表ID（来自 `monthly_report` 缓存表）
	 */
	private Long reportId;

	/**
	 * 统计月份（`yyyy-MM`）
	 */
	private String month;

	/**
	 * 有效订单数量
	 */
	private Long orderCount;

	/**
	 * 售出菜品总数量
	 */
	private Long totalQuantity;

	/**
	 * 销售总金额
	 */
	private BigDecimal totalAmount;

	/**
	 * 报表生成时间（`yyyy-MM-dd HH:mm:ss`）
	 */
	private String generatedAt;

	/**
	 * 菜品销售明细
	 */
	private List<DishSalesVO> items = new ArrayList<>();
}
