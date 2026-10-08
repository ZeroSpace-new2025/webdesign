package com.university.webdesign.domain.operation;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 每日汇总快照（需求原文的 `Daily_Statistics`）。
 * <p>
 * 对应《重构实施规范》第 3 节 M3 的 `daily_statistics` 表：
 * `statistics_date` 唯一（一天一条快照），明细存在 `statistics_item` 子表。
 * <p>
 * 聚合算法见 {@code BlanketOrderServiceImpl.aggregate}：取当日有效订单 →
 * 按 `recipeId` 汇总数量与金额 → 覆盖写本表；`force=false` 且已存在时直接复用既有快照（幂等）。
 */
@Data
@Entity
@Table(name = "daily_statistics",
		uniqueConstraints = @UniqueConstraint(name = "uk_daily_statistics_date", columnNames = "statistics_date"))
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatistics
{
	/**
	 * 快照状态：已聚合
	 */
	public static final String STATUS_AGGREGATED = "AGGREGATED";

	/**
	 * 汇总快照ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * 汇总日期（一天一条）
	 */
	@Column(name = "statistics_date", nullable = false, unique = true)
	private LocalDate statisticsDate;

	/**
	 * 纳入统计的有效订单数
	 */
	@Column(name = "total_orders", nullable = false)
	private int totalOrders;

	/**
	 * 需求总金额
	 */
	@Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	/**
	 * 快照状态：{@link #STATUS_AGGREGATED}
	 */
	@Column(name = "status", nullable = false, length = 20)
	private String status = STATUS_AGGREGATED;

	/**
	 * 生成时间
	 */
	@Column(name = "generated_at", nullable = false)
	private LocalDateTime generatedAt;

	/**
	 * 最后刷新时间
	 */
	@Column(name = "refreshed_at")
	private LocalDateTime refreshedAt;

	/**
	 * 分类 → 菜品 的汇总明细（`statistics_item` 子表）
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "statistics_item", joinColumns = @JoinColumn(name = "statistics_id"))
	@OrderBy("recipeId ASC")
	private List<DailyStatisticsItem> items = new ArrayList<>();

	/**
	 * 添加一条汇总明细并维护关联
	 *
	 * @param item 汇总明细
	 */
	public void addItem(DailyStatisticsItem item) {
		this.items.add(item);
	}

	@PrePersist
	private void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (generatedAt == null) {
			generatedAt = now;
		}
		refreshedAt = now;
		if (totalAmount == null) {
			totalAmount = BigDecimal.ZERO;
		}
		if (status == null) {
			status = STATUS_AGGREGATED;
		}
	}

	@PreUpdate
	private void onUpdate() {
		refreshedAt = LocalDateTime.now();
	}
}
