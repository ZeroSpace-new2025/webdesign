package com.university.webdesign.service.order.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 订单分页查询条件。
 * <p>
 * 对应《对外方法表》M2-07：员工查询时服务层强制注入 `employeeId = 当前用户`；
 * 经理/财务可按员工、部门、日期区间、状态自由筛选。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQuery extends com.university.webdesign.common.PageQuery
{
	/**
	 * 员工ID：员工角色由服务层强制覆盖为当前登录用户
	 */
	private Long employeeId;

	/**
	 * 部门ID：经理/财务可按部门筛选
	 */
	private Long deptId;

	/**
	 * 起始日期（含），为空表示不限
	 */
	private LocalDate dateFrom;

	/**
	 * 结束日期（含），为空表示不限
	 */
	private LocalDate dateTo;

	/**
	 * 订单状态编码：PENDING / VALID / INVALID / CANCELLED，为空表示不限
	 */
	private String status;

	/**
	 * 是否包含已取消/已作废订单，默认 false（只查有效订单）
	 */
	private Boolean includeInactive;
}
