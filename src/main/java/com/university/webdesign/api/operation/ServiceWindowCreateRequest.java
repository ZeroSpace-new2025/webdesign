package com.university.webdesign.api.operation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 新增时间窗口配置入参（M3-13）。
 * <p>
 * 对应《对外方法表》M3-13 的 `cutoffTime`、`deliveryStartTime`、`effectiveFrom`、
 * `scope=GLOBAL|DEPT`、`deptId`。api 层只做必填与格式校验，业务校验在 service 层。
 */
@Data
public class ServiceWindowCreateRequest
{
	/**
	 * 订餐截止时间，默认 09:00
	 */
	@NotNull(message = "订餐截止时间不能为空")
	private LocalTime cutoffTime;

	/**
	 * 配餐开始时间，默认 11:30
	 */
	@NotNull(message = "配餐开始时间不能为空")
	private LocalTime deliveryStartTime;

	/**
	 * 生效起始日期，为空表示立即生效
	 */
	private LocalDate effectiveFrom;

	/**
	 * 作用域：GLOBAL（默认）/ DEPT
	 */
	private String scope;

	/**
	 * 部门ID，scope=DEPT 时必填
	 */
	private Long deptId;
}
