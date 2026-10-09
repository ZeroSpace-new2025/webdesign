package com.university.webdesign.service.operation.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 配送任务分页查询条件。
 * <p>
 * 对应《对外方法表》M3-08：按日期/工位/状态/员工筛选。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeliveryQuery extends com.university.webdesign.common.PageQuery
{
	/**
	 * 配送日期
	 */
	private LocalDate date;

	/**
	 * 工位
	 */
	private String workstation;

	/**
	 * 部门ID
	 */
	private Long deptId;

	/**
	 * 员工ID
	 */
	private Long employeeId;

	/**
	 * 任务状态：PENDING / DELIVERING / DELIVERED / EXCEPTION
	 */
	private String status;
}
