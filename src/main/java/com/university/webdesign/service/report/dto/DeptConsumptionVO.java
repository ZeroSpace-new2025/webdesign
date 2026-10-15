package com.university.webdesign.service.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 部门维度消费汇总。
 * <p>
 * 对应《对外方法表》M4-26 `GET /reports/dept-consumption` 的返回，
 * 用于按部门做成本分摊。
 */
@Data
public class DeptConsumptionVO
{
	/**
	 * 部门ID
	 */
	private Long deptId;

	/**
	 * 部门名称
	 */
	private String deptName;

	/**
	 * 该部门有消费记录的员工数
	 */
	private Long employeeCount;

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
}
