package com.university.webdesign.domain.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 月度销售报表明细（按菜品聚合）。
 * <p>
 * 对应《重构实施规范》第 3 节的 M4 `monthly_report_item` 表：菜名与分类都是
 * 从订单明细快照聚合而来的字面值，菜谱后续改名/下架都不会改变历史报表。
 */
@Entity
@Table(name = "monthly_report_item")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyReportItem
{
	/**
	 * 明细ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * 所属报表
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_id", nullable = false)
	private MonthlyReport report;

	/**
	 * 菜品（菜谱）ID，用于跨月对比
	 */
	@Column(name = "recipe_id")
	private Long recipeId;

	/**
	 * 下单时的菜名快照
	 */
	@Column(name = "recipe_name", nullable = false, length = 100)
	private String recipeName;

	/**
	 * 下单时的菜品分类快照
	 */
	@Column(name = "category", length = 50)
	private String category;

	/**
	 * 售出数量
	 */
	@Column(name = "quantity", nullable = false)
	private Long quantity;

	/**
	 * 销售金额
	 */
	@Column(name = "amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;
}
