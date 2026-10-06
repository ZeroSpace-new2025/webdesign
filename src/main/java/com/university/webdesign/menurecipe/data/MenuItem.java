package com.university.webdesign.menurecipe.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 菜单项实体。
 * 对应需求中的 Menu_Item 表：存储菜单与菜品的关联关系以及菜单层级的当前价格。
 * 菜名、分类、单位在此做快照，确保菜品日后被修改或删除时，历史菜单仍保持完整。
 */
@Entity
@Table(name = "menu_item")
@Getter
@Setter
public class MenuItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 所属菜单
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "menu_id")
	private Menu menu;

	/**
	 * 关联菜品ID
	 */
	@Column(name = "recipe_id")
	private Long recipeId;

	/**
	 * 菜品名称（快照）
	 */
	@Column(name = "recipe_name", length = 100)
	private String recipeName;

	/**
	 * 菜品分类（快照）
	 */
	@Column(name = "category", length = 50)
	private String category;

	/**
	 * 计量单位（快照）
	 */
	@Column(name = "unit", length = 20)
	private String unit;

	/**
	 * 菜品图片地址（快照），避免菜品图片日后更换时历史菜单显示成新图
	 */
	@Column(name = "image_url", length = 500)
	private String imageUrl;

	/**
	 * 菜单层级的售价（可在菜单层级微调，覆盖菜品标准价）
	 */
	@Column(name = "price", precision = 10, scale = 2)
	private BigDecimal price;
}
