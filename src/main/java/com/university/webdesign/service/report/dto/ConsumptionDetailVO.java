package com.university.webdesign.service.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 员工消费审计中的一张订单。
 * <p>
 * 对应《对外方法表》M4-25 的 `orders[]`：订单基本信息 + 下单时刻的菜品快照明细。
 */
@Data
public class ConsumptionDetailVO
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
	 * 就餐日期（`yyyy-MM-dd`）
	 */
	private String orderDate;

	/**
	 * 下单时间（`yyyy-MM-dd HH:mm:ss`）
	 */
	private String createdAt;

	/**
	 * 订单状态编码
	 */
	private String status;

	/**
	 * 订单状态中文文案
	 */
	private String statusText;

	/**
	 * 订单金额
	 */
	private BigDecimal totalAmount;

	/**
	 * 菜品快照明细
	 */
	private List<DishSalesVO> items = new ArrayList<>();
}
