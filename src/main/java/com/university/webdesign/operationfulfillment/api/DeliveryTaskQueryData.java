package com.university.webdesign.operationfulfillment.api;

import lombok.Data;

/**
 * 配送任务查询参数。
 */
@Data
public class DeliveryTaskQueryData
{
	/** 配送日期（毫秒时间戳，缺省取当日） */
	private Long deliveryDate;

	/** 收餐员工 ID */
	private Long userId;

	/** 工位 */
	private String workstation;

	/** 任务状态 */
	private String status;
}
