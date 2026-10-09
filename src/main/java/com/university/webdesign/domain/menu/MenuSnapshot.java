package com.university.webdesign.domain.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜单发布快照实体，对应表 {@code menu_snapshot}。**数据保护核心**。
 * <p>
 * 字段严格按《重构实施规范》第 3 节：{@code menu_id}、{@code recipe_id}、{@code recipe_name}、
 * {@code category}、{@code unit}、{@code unit_price}、{@code menu_price}、{@code frozen_at}。
 * <p>
 * 语义约定：本表**只允许插入，不允许更新或删除**（发布时由
 * {@code MenuSnapshotService.freeze} 一次性写入）。因此 {@code MenuSnapshotRepository}
 * 只暴露保存与查询方法，从代码层面封死修改入口。
 */
@Entity
@Table(name = "menu_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class MenuSnapshot
{
	/**
	 * 快照ID（主键）
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "snapshot_id")
	private Long snapshotId;

	/**
	 * 所属菜单ID
	 */
	@Column(name = "menu_id", nullable = false)
	private Long menuId;

	/**
	 * 菜品（食谱）ID
	 */
	@Column(name = "recipe_id", nullable = false)
	private Long recipeId;

	/**
	 * 冻结时刻的菜品名称
	 */
	@Column(name = "recipe_name", nullable = false, length = 100)
	private String recipeName;

	/**
	 * 冻结时刻的菜品分类
	 */
	@Column(name = "category", length = 50)
	private String category;

	/**
	 * 冻结时刻的计量单位
	 */
	@Column(name = "unit", length = 20)
	private String unit;

	/**
	 * 冻结时刻的菜品标准单价（元）
	 */
	@Column(name = "unit_price", precision = 10, scale = 2)
	private BigDecimal unitPrice;

	/**
	 * 冻结时刻的菜单级售价（元），为空表示下单取价回退 {@code unitPrice}
	 */
	@Column(name = "menu_price", precision = 10, scale = 2)
	private BigDecimal menuPrice;

	/**
	 * 冻结时间
	 */
	@Column(name = "frozen_at", nullable = false)
	private LocalDateTime frozenAt;
}
