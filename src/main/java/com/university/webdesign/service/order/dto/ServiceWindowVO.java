package com.university.webdesign.service.order.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 订餐时间窗口视图对象。
 * <p>
 * 对应《对外方法表》M2-01：`cutoffTime`(09:00)、`deliveryStartTime`(11:30)、
 * `canOrder`、`serverTime`。前端据此控制按钮可用性，**不在页面里硬编码时间**。
 */
@Data
public class ServiceWindowVO
{
	/**
	 * 生效日期
	 */
	private LocalDate date;

	/**
	 * 订餐截止时间
	 */
	private LocalTime cutoffTime;

	/**
	 * 配餐开始时间
	 */
	private LocalTime deliveryStartTime;

	/**
	 * 当前是否还能下单/改单/取消
	 */
	private boolean canOrder;

	/**
	 * 当前是否已开放配送单打印
	 */
	private boolean canDeliver;

	/**
	 * 服务器当前时间
	 */
	private String serverTime;
}
