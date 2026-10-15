package com.university.webdesign.service.operation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 每日汇总快照视图（`daily_statistics`）。
 * <p>
 * 对应《对外方法表》M3-01 / M3-05：聚合结果落库后供厨房备料、生产单打印快速读取。
 */
@Data
public class DailyStatVO
{
	/**
	 * 汇总快照ID
	 */
	private Long statisticsId;

	/**
	 * 汇总日期
	 */
	private java.time.LocalDate statisticsDate;

	/**
	 * 纳入统计的有效订单数
	 */
	private int totalOrders;

	/**
	 * 需求总金额
	 */
	private BigDecimal totalAmount = BigDecimal.ZERO;

	/**
	 * 快照状态：AGGREGATED（已聚合）
	 */
	private String status;

	/**
	 * 生成时间（毫秒时间戳）
	 */
	private Long generatedTime;

	/**
	 * 最后刷新时间（毫秒时间戳）
	 */
	private Long refreshedTime;

	/**
	 * 分类 → 菜品 的汇总明细
	 */
	private List<CategoryStatVO> items = new ArrayList<>();
}
