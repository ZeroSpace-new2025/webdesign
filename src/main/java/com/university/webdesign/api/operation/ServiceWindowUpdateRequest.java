package com.university.webdesign.api.operation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 修改时间窗口配置入参（M3-14）。
 * <p>
 * 对应《对外方法表》M3-14 的 `cutoffTime`、`deliveryStartTime`、`effectiveFrom`；
 * 另允许同时调整 `scope`/`deptId`（为空表示保持原值），改后立即生效。
 */
@Data
public class ServiceWindowUpdateRequest
{
	/**
	 * 订餐截止时间
	 */
	@NotNull(message = "订餐截止时间不能为空")
	private LocalTime cutoffTime;

	/**
	 * 配餐开始时间
	 */
	@NotNull(message = "配餐开始时间不能为空")
	private LocalTime deliveryStartTime;

	/**
	 * 生效起始日期，为空表示立即生效
	 */
	private LocalDate effectiveFrom;

	/**
	 * 作用域：GLOBAL / DEPT，为空保持原值
	 */
	private String scope;

	/**
	 * 部门ID，scope=DEPT 时必填
	 */
	private Long deptId;
}
