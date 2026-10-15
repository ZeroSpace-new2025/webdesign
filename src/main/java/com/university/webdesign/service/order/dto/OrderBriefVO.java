package com.university.webdesign.service.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单精简视图。
 * <p>
 * 对应《对外方法表》M2 的 `OrderBriefVO` / `OrderQueryService.listValidByDate`：
 * 供 M3 聚合总括订单与生成配送单使用（替代 v1.0 的内部 HTTP 接口），
 * 只返回有效订单，且携带生成生产单/配送单所需的全部快照字段。
 */
@Data
public class OrderBriefVO
{
	/**
	 * 订单ID
	 */
	private Long orderId;

	/**
	 * 订单号
	 */
	private String orderNo;

	/**
	 * 员工ID
	 */
	private Long employeeId;

	/**
	 * 就餐日期
	 */
	private LocalDate orderDate;

	/**
	 * 总金额
	 */
	private BigDecimal totalAmount;

	/**
	 * 明细快照（含分类与单位，供分类汇总）
	 */
	private List<OrderDetailVO> items = new ArrayList<>();
}
