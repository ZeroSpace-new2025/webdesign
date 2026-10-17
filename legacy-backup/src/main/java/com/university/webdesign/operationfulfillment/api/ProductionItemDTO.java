package com.university.webdesign.operationfulfillment.api;

import lombok.Data;

/**
 * 生产单明细项 DTO（按菜品聚合后的一行）。
 * <p>
 * 供厨房备料使用，例如：米饭 50两、小炒肉 30份。
 */
@Data
public class ProductionItemDTO
{
	/** 菜品 ID */
	private Long dishId;

	/** 菜品名称 */
	private String dishName;

	/** 菜品分类（用于按类归集打印） */
	private String category;

	/** 聚合后的总数量 */
	private double totalQuantity;

	/** 计量单位（份/两/斤等） */
	private String unit;
}
