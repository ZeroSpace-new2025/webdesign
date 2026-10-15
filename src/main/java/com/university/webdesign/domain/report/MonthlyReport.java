package com.university.webdesign.domain.report;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 月度销售报表（缓存表）。
 * <p>
 * 对应《重构实施规范》第 3 节的 M4 `monthly_report` 表：按月唯一，
 * 由 {@code ReportService.generateMonthly} 从 {@code OrderQueryService} 取数后聚合写入，
 * 查询时优先读本缓存，避免每次实时扫订单。
 * <p>
 * 明细见 {@link MonthlyReportItem}；`report_month` 使用 {@link ReportMonthConverter} 存为 `yyyy-MM`。
 */
@Entity
@Table(name = "monthly_report")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyReport
{
	/**
	 * 报表ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * 统计月份（`yyyy-MM`），全局唯一
	 */
	@Convert(converter = ReportMonthConverter.class)
	@Column(name = "report_month", nullable = false, unique = true, length = 7)
	private YearMonth reportMonth;

	/**
	 * 有效订单数量
	 */
	@Column(name = "order_count", nullable = false)
	private Integer orderCount;

	/**
	 * 售出菜品总数量
	 */
	@Column(name = "total_quantity", nullable = false)
	private Long totalQuantity;

	/**
	 * 销售总金额
	 */
	@Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount;

	/**
	 * 报表生成时间
	 */
	@Column(name = "generated_at", nullable = false)
	private LocalDateTime generatedAt;

	/**
	 * 菜品销售明细（覆盖式重算，级联保存/删除）
	 */
	@OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<MonthlyReportItem> items = new ArrayList<>();

	/**
	 * 追加一条明细并维护双向关联
	 *
	 * @param item 明细
	 */
	public void addItem(MonthlyReportItem item) {
		item.setReport(this);
		items.add(item);
	}
}
