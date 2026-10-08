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
 * 报表订单中的菜品价格与数量快照。
 */
@Entity
@Table(
	name = "report_order_item",
	indexes = @Index(name = "idx_report_order_item_order", columnList = "order_id"))
@Getter
@Setter
@NoArgsConstructor
public class ReportOrderItemEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private ReportOrderEntity order;

	@Column(nullable = false)
	private Long recipeId;

	@Column(nullable = false, length = 120)
	private String recipeName;

	@Column(length = 80)
	private String category;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;
}
