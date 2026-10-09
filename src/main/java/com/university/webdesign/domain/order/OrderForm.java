package com.university.webdesign.domain.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单主表（需求原文的 {@code Order_Form}）。
 * <p>
 * 业务不变量：
 * <ul>
 *     <li><b>一人一天一单</b>：{@code (employee_id, order_date)} 对有效订单唯一，
 *         服务层校验 + 本表的唯一约束兜底；</li>
 *     <li><b>删单留痕</b>：经理作废走 {@link OrderStatus#INVALID} 并写审计字段，不物理删除；</li>
 *     <li><b>幂等</b>：{@code idempotency_key} 唯一索引保证重复提交不会产生两张订单。</li>
 * </ul>
 */
@Data
@Entity
@Table(name = "order_form",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_order_form_no", columnNames = "order_no"),
				@UniqueConstraint(name = "uk_order_form_idempotency", columnNames = "idempotency_key")
		})
@NoArgsConstructor
@AllArgsConstructor
public class OrderForm
{
	/**
	 * 订单ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Long orderId;

	/**
	 * 订单号（对外展示与打印）
	 * <p>
	 * 规则：{@code yyyyMMdd + 4 位当日流水}，例如 {@code 202601010001}。
	 */
	@Column(name = "order_no", nullable = false, length = 32)
	private String orderNo;

	/**
	 * 员工ID
	 * <p>
	 * 只保存 ID，不建立跨模块外键：员工基础数据的唯一所有者是用户与报表中心。
	 */
	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	/**
	 * 部门ID（下单时快照，便于按部门汇总与筛选，无需反查用户表）
	 */
	@Column(name = "dept_id")
	private Long deptId;

	/**
	 * 就餐日期
	 */
	@Column(name = "order_date", nullable = false)
	private LocalDate orderDate;

	/**
	 * 订单总金额
	 */
	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	/**
	 * 订单状态
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status = OrderStatus.PENDING;

	/**
	 * 备注
	 */
	@Column(name = "remark", length = 500)
	private String remark;

	/**
	 * 幂等键，来自请求头 {@code Idempotency-Key}
	 */
	@Column(name = "idempotency_key", length = 64)
	private String idempotencyKey;

	/**
	 * 作废人ID（仅 {@link OrderStatus#INVALID} 有值）
	 */
	@Column(name = "invalidated_by")
	private Long invalidatedBy;

	/**
	 * 作废原因（仅 {@link OrderStatus#INVALID} 有值）
	 */
	@Column(name = "invalidate_reason", length = 500)
	private String invalidateReason;

	/**
	 * 作废时间（仅 {@link OrderStatus#INVALID} 有值）
	 */
	@Column(name = "invalidate_time")
	private LocalDateTime invalidateTime;

	/**
	 * 取消原因（仅 {@link OrderStatus#CANCELLED} 有值）
	 */
	@Column(name = "cancel_reason", length = 500)
	private String cancelReason;

	/**
	 * 下单时间
	 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	/**
	 * 最后修改时间
	 */
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	/**
	 * 乐观锁版本号
	 */
	@Version
	@Column(name = "version", nullable = false)
	private Integer version;

	/**
	 * 订单明细（下单时刻的快照）
	 */
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("detailId ASC")
	private List<OrderDetail> details = new ArrayList<>();

	/**
	 * 添加一条明细并维护双向关联
	 *
	 * @param detail 明细
	 */
	public void addDetail(OrderDetail detail) {
		detail.setOrder(this);
		this.details.add(detail);
	}

	/**
	 * 清空明细（改单时整体替换）
	 */
	public void clearDetails() {
		this.details.clear();
	}

	/**
	 * 按当前明细重算总金额
	 *
	 * @return 重算后的总金额
	 */
	public BigDecimal recalculateTotal() {
		BigDecimal sum = BigDecimal.ZERO;
		for (OrderDetail detail : this.details) {
			sum = sum.add(detail.subtotal());
		}
		this.totalAmount = sum;
		return sum;
	}

	/**
	 * 标记为已作废（经理删单留痕）
	 *
	 * @param operatorId 操作经理ID
	 * @param reason     作废原因
	 */
	public void markInvalid(Long operatorId, String reason) {
		this.status = OrderStatus.INVALID;
		this.invalidatedBy = operatorId;
		this.invalidateReason = reason;
		this.invalidateTime = LocalDateTime.now();
		this.updatedAt = this.invalidateTime;
	}

	/**
	 * 标记为已取消（员工本人取消）
	 *
	 * @param reason 取消原因
	 */
	public void markCancelled(String reason) {
		this.status = OrderStatus.CANCELLED;
		this.cancelReason = reason;
		this.updatedAt = LocalDateTime.now();
	}

	@PrePersist
	private void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
		if (status == null) {
			status = OrderStatus.PENDING;
		}
		if (totalAmount == null) {
			totalAmount = BigDecimal.ZERO;
		}
	}

	@PreUpdate
	private void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
