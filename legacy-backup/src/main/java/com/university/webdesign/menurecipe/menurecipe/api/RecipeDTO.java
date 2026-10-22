package com.university.webdesign.menurecipe.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品（菜谱）DTO
 */
@Data
public class RecipeDTO {

	/**
	 * 菜品ID
	 */
	private Long recipeId;

	/**
	 * 菜品名称
	 */
	private String recipeName;

	/**
	 * 菜品分类
	 */
	private String category;

	/**
	 * 计量单位
	 */
	private String unit;

	/**
	 * 标准单价
	 */
	private BigDecimal price;

	/**
	 * 菜品图片地址
	 */
	private String recipeImageUrl;

	/**
	 * 菜品描述
	 */
	private String recipeDescription;

	/**
	 * 状态：ACTIVE / INACTIVE
	 */
	private String status;

	/**
	 * 创建者ID
	 */
	private Long createdBy;

	/**
	 * 创建时间
	 */
	private LocalDateTime createdTime;

	/**
	 * 最后修改时间
	 */
	private LocalDateTime lastModifiedTime;
}
