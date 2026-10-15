package com.university.webdesign.service.order.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 个人历史订单查询条件。
 * <p>
 * 对应《对外方法表》M2-08：员工分页查看本人历史订单。员工ID由服务层以登录用户为准，
 * 不接受客户端指定他人。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderHistoryQuery extends com.university.webdesign.common.PageQuery
{
	/**
	 * 起始日期（含），为空表示不限
	 */
	private LocalDate dateFrom;

	/**
	 * 结束日期（含），为空表示不限
	 */
	private LocalDate dateTo;

	/**
	 * 订单状态编码，为空表示不限
	 */
	private String status;
}
