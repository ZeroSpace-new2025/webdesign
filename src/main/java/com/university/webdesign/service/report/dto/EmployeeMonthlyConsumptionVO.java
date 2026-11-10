package com.university.webdesign.service.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 员工维度月度消费汇总。
 * <p>
 * 对应《对外方法表》5.2 的 {@code ConsumptionAuditService.sumByEmployee}：
 * 按月就餐日期归集当月有效订单，一个员工一行，供《员工月度消费报表》页面与 CSV 导出使用。
 * <p>
 * 只含统计所需字段，不携带订单明细；需要逐单快照时按 {@code employeeId} 调
 * {@code ConsumptionAuditService.auditEmployee}。
 */
@Data
public class EmployeeMonthlyConsumptionVO
{
	/**
	 * 员工ID
	 */
	private Long employeeId;

	/**
	 * 工号
	 */
	private String employeeNo;

	/**
	 * 员工姓名
	 */
	private String employeeName;

	/**
	 * 部门ID
	 */
	private Long deptId;

	/**
	 * 部门名称
	 */
	private String deptName;

	/**
	 * 工位
	 */
	private String workstation;

	/**
	 * 统计月份（`yyyy-MM`）
	 */
	private String month;

	/**
	 * 当月有订单的就餐天数（按 `order_date` 去重）
	 */
	private Long activeDays;

	/**
	 * 有效订单数量
	 */
	private Long orderCount;

	/**
	 * 菜品总份数（订单明细数量之和）
	 */
	private Long totalQuantity;

	/**
	 * 消费总金额
	 */
	private BigDecimal totalAmount;
}
