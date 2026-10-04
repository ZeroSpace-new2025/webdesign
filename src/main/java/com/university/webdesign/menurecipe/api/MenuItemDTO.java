package com.university.webdesign.menurecipe.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜单项 DTO：描述菜单中的单个菜品及其菜单级售价
 */
@Data
public class MenuItemDTO {

	/**
	 * 菜单项ID
	 */
	private Long id;

	/**
	 * 关联菜品ID
	 */
	private Long recipeId;

	/**
	 * 菜品名称（快照）
	 */
	private String recipeName;

	/**
	 * 菜品分类（快照）
	 */
	private String category;

	/**
	 * 计量单位（快照）
	 */
	private String unit;

	/**
	 * 菜品图片地址（快照）
	 */
	private String imageUrl;

	/**
	 * 菜单级售价
	 */
	private BigDecimal price;
}
