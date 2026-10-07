package com.university.webdesign.operationfulfillment.api;

import lombok.Data;

import java.util.List;

/**
 * 配送任务 DTO（按员工/工位维度批量生成的配送单）。
 * <p>
 * 到达“配餐开始时间”（默认 11:30）后开放打印权限，
 * 配送单包含详细信息：菜名、分量、工位、电话。
 */
@Data
public class DeliveryTaskDTO
{
	/** 配送任务 ID（对应 Delivery_Task.id） */
	private Long id;

	/** 配送任务业务键 */
	private String taskId;

	/** 收餐员工 ID */
	private Long userId;

	/** 收餐员工姓名 */
	private String employeeName;

	/** 工位（送达目的地） */
	private String workstation;

	/** 联系电话 */
	private String phone;

	/** 配送日期（毫秒时间戳） */
	private Long deliveryDate;

	/** 配送明细列表 */
	private List<DeliveryItemDTO> items;

	/** 任务状态：PENDING / PRINTED / DELIVERED */
	private String status;

	/** 创建时间（毫秒时间戳） */
	private long createdTime;
}
