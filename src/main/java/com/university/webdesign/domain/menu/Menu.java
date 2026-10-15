package com.university.webdesign.domain.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 菜单实体（菜单头信息），对应表 {@code menu}。
 * <p>
 * 菜单从菜品库挑选菜品生成，支持版本链与发布/下架：
 * <ul>
 *     <li>{@link MenuStatus#DRAFT}：可反复修改，每次改动 {@code version} 自增（乐观锁 {@code @Version}）；</li>
 *     <li>{@link MenuStatus#PUBLISHED}：发布瞬间冻结 {@link MenuSnapshot}，并置 {@code locked=true}，不可改不可删；</li>
 *     <li>{@link MenuStatus#OFFLINE}：下架后不再接单，历史快照与历史订单不受影响。</li>
 * </ul>
 * {@code version} 由 Hibernate 在 UPDATE 时自增；service 层每次改动都会同时刷新
 * {@code updated_at}，保证“只要改过，版本号一定递增”。
 */
@Entity
@Table(name = "menu")
@Getter
@Setter
@NoArgsConstructor
public class Menu
{
	/**
	 * 菜单ID（主键）
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "menu_id")
	private Long menuId;

	/**
	 * 菜单名称
	 */
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	/**
	 * 菜单描述
	 */
	@Column(name = "description", length = 1000)
	private String description;

	/**
	 * 状态：草稿 / 已发布 / 已下架
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private MenuStatus status = MenuStatus.DRAFT;

	/**
	 * 生效日期：点餐按“生效日期不晚于当日”的已发布菜单取价
	 */
	@Column(name = "effective_date", nullable = false)
	private LocalDate effectiveDate;

	/**
	 * 乐观锁版本号：更新入参 version 与库中不一致时抛 40902；每次改动自增
	 */
	@Version
	@Column(name = "version", nullable = false)
	private Integer version;

	/**
	 * 历史锁定标记：首次发布后置 true，此后不可编辑/删除，只能下架
	 */
	@Column(name = "locked", nullable = false)
	private boolean locked;

	/**
	 * 首次发布时间
	 */
	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	/**
	 * 下架时间
	 */
	@Column(name = "offline_at")
	private LocalDateTime offlineAt;

	/**
	 * 下架原因
	 */
	@Column(name = "offline_reason", length = 200)
	private String offlineReason;

	/**
	 * 创建者用户ID，取 {@code UserContextHolder.currentUserId()}，无登录上下文时为空
	 */
	@Column(name = "created_by")
	private Long createdBy;

	/**
	 * 创建时间
	 */
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	/**
	 * 最后修改时间
	 */
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
