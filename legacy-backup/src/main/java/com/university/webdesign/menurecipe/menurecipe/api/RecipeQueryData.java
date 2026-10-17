package com.university.webdesign.menurecipe.api;

import lombok.Data;

/**
 * 菜品查询条件
 */
@Data
public class RecipeQueryData {

	/**
	 * 菜品ID
	 */
	private Long recipeId;

	/**
	 * 菜品名称（模糊匹配）
	 */
	private String recipeName;

	/**
	 * 菜品分类
	 */
	private String category;

	/**
	 * 状态
	 */
	private String status;

	/**
	 * 创建者ID
	 */
	private Long createdBy;

	/**
	 * 创建时间区间起始（毫秒时间戳）
	 */
	private Long startCreatedTime;

	/**
	 * 创建时间区间结束（毫秒时间戳）
	 */
	private Long endCreatedTime;

	/**
	 * 最后修改时间区间起始（毫秒时间戳）
	 */
	private Long startLastModifiedTime;

	/**
	 * 最后修改时间区间结束（毫秒时间戳）
	 */
	private Long endLastModifiedTime;
}
