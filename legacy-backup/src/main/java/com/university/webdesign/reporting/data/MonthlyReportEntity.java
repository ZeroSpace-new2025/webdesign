package com.university.webdesign.reporting.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 月度销售报表缓存主表。
 */
@Entity
@Table(name = "monthly_report")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyReportEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "report_month", nullable = false, unique = true, length = 7)
	private String reportMonth;

	@Column(nullable = false)
	private Integer orderCount;

	@Column(nullable = false)
	private Long totalQuantity;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount;

	@Column(nullable = false)
	private Long generatedTime;

	@OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("salesAmount desc")
	private List<MonthlyReportItemEntity> items = new ArrayList<>();
}
