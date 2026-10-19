package com.university.webdesign.service.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 部门消费汇总中的员工汇总。
 * <p>
 * 随 {@code DeptConsumptionVO.employees} 返回。字段只包含消费统计所需信息，
 * 用于财务或经理继续调用员工消费审计接口查看订单快照明细。
 */
@Data
public class DeptEmployeeConsumptionVO
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
	 * 工位
	 */
	private String workstation;

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
