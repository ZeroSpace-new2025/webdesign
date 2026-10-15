package com.university.webdesign.api.operation;

import lombok.Data;

import java.time.LocalDate;

/**
 * 总括订单聚合入参（M3-01）。
 * <p>
 * 对应《对外方法表》M3-01 的 `date`（默认当天）与 `force`（是否强制覆盖既有快照）。
 */
@Data
public class AggregateRequest
{
	/**
	 * 汇总日期，为空取当天
	 */
	private LocalDate date;

	/**
	 * 是否强制覆盖既有快照，默认 false（幂等）
	 */
	private Boolean force;
}
