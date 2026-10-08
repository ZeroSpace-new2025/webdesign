package com.university.webdesign.domain.operation;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import com.university.webdesign.service.operation.dto.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 配送任务（需求原文的 `Delivery_Task`）。
 * <p>
 * 对应《重构实施规范》第 3 节 M3 的 `delivery_task` 表：按员工维度拆单，
 * `(statistics_date, employee_id)` 唯一，保证**同一员工同一就餐日只派一次单**；
 * 明细存在 `delivery_task_item` 子表。
 * <p>
 * 状态机见 {@link com.university.webdesign.service.operation.dto.TaskStatus}：
 * `PENDING → DELIVERING → DELIVERED / EXCEPTION`，非法流转抛 40902。
 */
@Data
@Entity
@Table(name = "delivery_task",
		uniqueConstraints = @UniqueConstraint(name = "uk_delivery_task_date_employee",
				columnNames = {"statistics_date", "employee_id"}))
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTask
{
	/**
	 * 任务ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * 任务编号，形如 `DT-20260101-1`
	 */
	@Column(name = "task_no", nullable = false, length = 40)
	private String taskNo;

	/**
	 * 就餐日期（配送日期）
	 */
	@Column(name = "statistics_date", nullable = false)
	private LocalDate statisticsDate;

	/**
	 * 员工ID
	 */
	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	/**
	 * 部门ID（快照）
	 */
	@Column(name = "dept_id")
	private Long deptId;

	/**
	 * 工位（配送单核心字段，生成任务时从用户中心补齐）
	 */
	@Column(name = "workstation", length = 50)
	private String workstation;

	/**
	 * 联系电话（生成任务时从用户中心补齐）
	 */
	@Column(name = "phone", length = 20)
	private String phone;

	/**
	 * 任务状态
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private TaskStatus status = TaskStatus.PENDING;

	/**
	 * 打印次数（审计用）
	 */
	@Column(name = "print_count", nullable = false)
	private int printCount;

	/**
	 * 最近打印时间
	 */
	@Column(name = "printed_at")
	private LocalDateTime printedAt;

	/**
	 * 签收人
	 */
	@Column(name = "receiver", length = 50)
	private String receiver;

	/**
	 * 备注
	 */
	@Column(name = "remark", length = 500)
	private String remark;

	/**
	 * 送达时间
	 */
	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;

	/**
	 * 生成时间
	 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	/**
	 * 最后修改时间
	 */
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	/**
	 * 配送明细（菜名、分量、单位）
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "delivery_task_item", joinColumns = @JoinColumn(name = "task_id"))
	@OrderBy("recipeId ASC")
	private List<DeliveryTaskItem> items = new ArrayList<>();

	/**
	 * 添加一条配送明细
	 *
	 * @param item 配送明细
	 */
	public void addItem(DeliveryTaskItem item) {
		this.items.add(item);
	}

	/**
	 * 累加打印次数并记录打印时间（批量打印审计）
	 *
	 * @param printedAt 打印时间
	 */
	public void markPrinted(LocalDateTime printedAt) {
		this.printCount = this.printCount + 1;
		this.printedAt = printedAt;
	}

	@PrePersist
	private void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
		if (status == null) {
			status = TaskStatus.PENDING;
		}
	}

	@PreUpdate
	private void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
