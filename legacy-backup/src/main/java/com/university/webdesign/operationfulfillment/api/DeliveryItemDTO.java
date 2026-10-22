package com.university.webdesign.operationfulfillment.api;

import lombok.Data;

/**
 * 配送单明细项 DTO（一名员工应送达的一道菜）。
 */
@Data
public class DeliveryItemDTO
{
	/** 菜品 ID */
	private Long dishId;

	/** 菜品名称 */
	private String dishName;

	/** 分量/数量 */
	private double quantity;

	/** 计量单位（份/两等） */
	private String unit;
}
