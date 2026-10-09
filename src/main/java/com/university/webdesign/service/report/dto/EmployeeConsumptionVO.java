package com.university.webdesign.service.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 员工月度消费审计视图。
 * <p>
 * 对应《对外方法表》M4-25 `GET /reports/employee-consumption` 的返回：
 * `orderCount`、`totalAmount`、`orders[]`（含快照明细）。
 */
@Data
public class EmployeeConsumptionVO
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
	 * 有效订单数量
	 */
	private Long orderCount;

	/**
	 * 菜品总数量
	 */
	private Long totalQuantity;

	/**
	 * 消费总金额
	 */
	private BigDecimal totalAmount;

	/**
	 * 订单消费明细；`withDetails=false` 时为空列表
	 */
	private List<ConsumptionDetailVO> details = new ArrayList<>();
}
