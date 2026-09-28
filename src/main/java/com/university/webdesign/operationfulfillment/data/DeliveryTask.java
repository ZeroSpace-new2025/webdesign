package com.university.webdesign.operationfulfillment.data;

/**
 * 配送任务实体（对应 Delivery_Task 表）。
 * <p>
 * 由配送管理模块按员工/工位维度批量生成，记录当日每个配送对象应送达的菜品清单。
 * 到达“配餐开始时间”（默认 11:30）后开放打印权限。
 */
public class DeliveryTask
{
	/** 主键 ID */
	private Long id;

	/** 配送任务业务键（如 DT-yyyyMMdd-{seq}） */
	private String taskId;

	/** 收餐员工 ID */
	private Long userId;

	/** 收餐员工姓名（冗余存储，便于打印） */
	private String employeeName;

	/** 工位（送达目的地） */
	private String workstation;

	/** 联系电话（打印在配送单上） */
	private String phone;

	/** 配送日期（业务键，每日生成） */
	private Long deliveryDate;

	/** 配送明细快照（序列化 JSON：{dishId, dishName, quantity, unit}） */
	private String items;

	/** 任务状态：PENDING / PRINTED / DELIVERED */
	private String status;

	/** 创建时间（毫秒时间戳） */
	private long createdTime;

	/** 最后修改时间（毫秒时间戳） */
	private long lastModifiedTime;
}
