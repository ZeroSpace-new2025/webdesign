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

/**
 * 菜单项实体（菜单与菜品的关联 + 菜单级调价），对应表 {@code menu_item}。
 * <p>
 * 字段严格按《重构实施规范》第 3 节：{@code menu_id}、{@code recipe_id}、{@code recipe_name}、
 * {@code category}、{@code unit}、{@code image_url}、{@code menu_price}。
 * <p>
 * 菜名/分类/单位/图片是**组建菜单时的副本**，菜品日后改名改价不会影响既有菜单；
 * {@code menu_price} 为菜单级售价（为空表示沿用菜品标准单价，下单取价时按
 * “菜单价优先、为空回退 {@code unitPrice}”处理）。
 */
@Entity
@Table(name = "menu_item")
@Getter
@Setter
@NoArgsConstructor
public class MenuItem
{
	/**
	 * 菜单项ID（主键）
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_id")
	private Long itemId;

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
	 * 菜品名称（组建菜单时的副本）
	 */
	@Column(name = "recipe_name", nullable = false, length = 100)
	private String recipeName;

	/**
	 * 菜品分类（组建菜单时的副本）
	 */
	@Column(name = "category", length = 50)
	private String category;

	/**
	 * 计量单位（组建菜单时的副本）
	 */
	@Column(name = "unit", length = 20)
	private String unit;

	/**
	 * 菜品图片地址（组建菜单时的副本）
	 */
	@Column(name = "image_url", length = 500)
	private String imageUrl;

	/**
	 * 菜单级售价（元），为空表示沿用菜品标准单价
	 */
	@Column(name = "menu_price", precision = 10, scale = 2)
	private BigDecimal menuPrice;
}
