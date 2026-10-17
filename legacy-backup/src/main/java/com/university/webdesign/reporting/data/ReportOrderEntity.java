package com.university.webdesign.reporting.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 报表中心保存的订单快照，避免直接耦合订单服务内部表。
 */
@Entity
@Table(
	name = "report_order",
	indexes = @Index(name = "idx_report_order_user_time", columnList = "user_id,created_at"))
@Getter
@Setter
@NoArgsConstructor
public class ReportOrderEntity
{
	@Id
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("id asc")
	private List<ReportOrderItemEntity> items = new ArrayList<>();
}
