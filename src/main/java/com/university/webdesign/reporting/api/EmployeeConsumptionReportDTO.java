package com.university.webdesign.reporting.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * 员工月度消费审计报表。
 */
@Data
public class EmployeeConsumptionReportDTO
{
	/**
	 * 用户ID
	 */
	private Long userId;

	/**
	 * 员工姓名
	 */
	private String employeeName;

	/**
	 * 所属部门
	 */
	private String department;

	/**
	 * 工位
	 */
	private String workstation;

	/**
	 * 统计月份
	 */
	private YearMonth month;

	/**
	 * 有效订单数量
	 */
	private int orderCount;

	/**
	 * 菜品总数量
	 */
	private long totalQuantity;

	/**
	 * 消费总金额
	 */
	private BigDecimal totalAmount;

	/**
	 * 订单消费明细
	 */
	private List<EmployeeConsumptionDetailDTO> details;
}
