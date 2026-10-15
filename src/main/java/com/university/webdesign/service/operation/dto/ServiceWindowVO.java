package com.university.webdesign.service.operation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 时间窗口配置视图（`service_window`）。
 * <p>
 * 对应《对外方法表》M3-15：**M2 下单校验时间窗口的唯一数据来源**。
 */
@Data
public class ServiceWindowVO
{
	/**
	 * 配置ID
	 */
	private Long configId;

	/**
	 * 订餐截止时间
	 */
	private java.time.LocalTime cutoffTime;

	/**
	 * 配餐开始时间
	 */
	private java.time.LocalTime deliveryStartTime;

	/**
	 * 生效起始日期
	 */
	private LocalDate effectiveFrom;

	/**
	 * 作用域：GLOBAL / DEPT
	 */
	private String scope;

	/**
	 * 部门ID（scope=DEPT 时有值）
	 */
	private Long deptId;

	/**
	 * 当前是否还能下单（服务层按服务器时间计算）
	 */
	private boolean canOrder;

	/**
	 * 服务器当前时间（`yyyy-MM-dd HH:mm:ss`）
	 */
	private String serverTime;

	/**
	 * 需求预测：当前配置涉及的当日有效订单数，便于确认改配置的影响面（可为 0）
	 */
	private int todayOrderCount;
}
