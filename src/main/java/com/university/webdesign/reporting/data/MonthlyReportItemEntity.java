package com.university.webdesign.reporting.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 月度销售报表中的菜品汇总行。
 */
@Entity
@Table(
	name = "monthly_report_item",
	indexes = @Index(name = "idx_monthly_report_item_report", columnList = "report_id"))
@Getter
@Setter
@NoArgsConstructor
public class MonthlyReportItemEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "report_id", nullable = false)
	private MonthlyReportEntity report;

	@Column(nullable = false)
	private Long recipeId;

	@Column(nullable = false, length = 120)
	private String recipeName;

	@Column(length = 80)
	private String category;

	@Column(nullable = false)
	private Long quantity;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal salesAmount;
}
