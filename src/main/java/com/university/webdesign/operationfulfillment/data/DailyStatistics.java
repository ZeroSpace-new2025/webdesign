package com.university.webdesign.operationfulfillment.data;

/**
 * 每日汇总快照实体（对应 Daily_Statistics 表）。
 * <p>
 * 在“订餐截止时间”后由聚合算法生成，用于快速查询当日总需求，避免反复扫描订单明细。
 * 字段对应数据库列，按菜品分类统计总数量（如：米饭 50两，小炒肉 30份）。
 */
public class DailyStatistics
{
	/** 主键 ID */
	private Long id;

	/** 统计日期（业务键，每日一条） */
	private Long statisticsDate;

	/** 当日有效订单总数 */
	private int totalOrders;

	/** 当日订单总金额 */
	private double totalAmount;

	/** 按菜品分类汇总的快照（序列化 JSON：dishId -> {name, category, quantity, unit}） */
	private String dishSummary;

	/** 汇总状态：AGGREGATED / PRINTED / VOIDED */
	private String status;

	/** 创建时间（毫秒时间戳） */
	private long createdTime;

	/** 最后修改时间（毫秒时间戳） */
	private long lastModifiedTime;
}
