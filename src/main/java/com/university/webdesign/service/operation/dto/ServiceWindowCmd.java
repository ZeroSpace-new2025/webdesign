package com.university.webdesign.service.operation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 时间窗口配置的新增/修改入参。
 * <p>
 * 对应《对外方法表》M3-13 / M3-14。
 */
@Data
public class ServiceWindowCmd
{
	/**
	 * 订餐截止时间（默认 09:00）
	 */
	@NotNull(message = "订餐截止时间不能为空")
	private LocalTime cutoffTime;

	/**
	 * 配餐开始时间（默认 11:30）
	 */
	@NotNull(message = "配餐开始时间不能为空")
	private LocalTime deliveryStartTime;

	/**
	 * 生效起始日期，为空表示立即生效
	 */
	private LocalDate effectiveFrom;

	/**
	 * 作用域：GLOBAL（全局，默认）或 DEPT（按部门）
	 */
	private String scope;

	/**
	 * 部门ID，scope=DEPT 时必填
	 */
	private Long deptId;
}
