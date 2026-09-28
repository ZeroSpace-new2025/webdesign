package com.university.webdesign.operationfulfillment.api;

import lombok.Data;

import java.util.List;

/**
 * 总括订单 DTO（生产单）。
 * <p>
 * 在“订餐截止时间”后由聚合算法对当日所有有效订单汇总生成，
 * 按菜品分类统计总数量，供厨房备料与生产单打印。
 */
@Data
public class BlanketOrderDTO
{
	/** 当日汇总记录 ID（对应 Daily_Statistics.id） */
	private Long statisticsId;

	/** 统计日期（毫秒时间戳，业务键） */
	private Long statisticsDate;

	/** 当日有效订单总数 */
	private int totalOrders;

	/** 当日订单总金额 */
	private double totalAmount;

	/** 按菜品聚合后的生产明细列表 */
	private List<ProductionItemDTO> items;

	/** 汇总状态：AGGREGATED / PRINTED / VOIDED */
	private String status;

	/** 生成时间（毫秒时间戳） */
	private long createdTime;
}
