package com.university.webdesign.domain.operation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 服务时间窗口配置（需求原文的 `Service_Window`）。
 * <p>
 * 对应《重构实施规范》第 3 节 M3 的 `service_window` 表。本表是
 * <b>订餐截止时间与配餐开始时间的唯一数据来源</b>：M2 下单校验、M3 配送放行都走
 * {@code ServiceWindowService.get(date, scope, deptId)}，不再出现散落的配置项。
 * <p>
 * 生效规则：取 `effective_from &lt;= date` 的记录中 `effective_from` 最大的一条；
 * {@code DEPT} 作用域优先于 {@code GLOBAL}。
 */
@Data
@Entity
@Table(name = "service_window")
@NoArgsConstructor
@AllArgsConstructor
public class ServiceWindow
{
	/**
	 * 配置ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * 订餐截止时间（默认 09:00）
	 */
	@Column(name = "cutoff_time", nullable = false)
	private LocalTime cutoffTime;

	/**
	 * 配餐开始时间（默认 11:30）
	 */
	@Column(name = "delivery_start_time", nullable = false)
	private LocalTime deliveryStartTime;

	/**
	 * 生效起始日期（为空表示立即生效）
	 */
	@Column(name = "effective_from")
	private LocalDate effectiveFrom;

	/**
	 * 作用域：GLOBAL（全局）/ DEPT（部门）
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "scope", nullable = false, length = 20)
	private WindowScope scope = WindowScope.GLOBAL;

	/**
	 * 部门ID（scope=DEPT 时有值）
	 */
	@Column(name = "dept_id")
	private Long deptId;

	/**
	 * 创建时间
	 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	/**
	 * 最后修改时间
	 */
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	/**
	 * 是否对指定部门生效
	 *
	 * @param targetDeptId 目标部门ID，可为空
	 * @return 全局配置或部门匹配时返回 true
	 */
	public boolean matchesDept(Long targetDeptId) {
		if (scope == WindowScope.GLOBAL) {
			return true;
		}
		return deptId != null && deptId.equals(targetDeptId);
	}

	@PrePersist
	private void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
		if (scope == null) {
			scope = WindowScope.GLOBAL;
		}
	}

	@PreUpdate
	private void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
