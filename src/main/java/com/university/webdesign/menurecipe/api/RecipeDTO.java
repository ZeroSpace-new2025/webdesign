package com.university.webdesign.menurecipe.api;

import lombok.Data;

import java.util.Dictionary;

/**
 * 菜谱DTO
 */
@Data
public class RecipeDTO
{
	/**
	 * 菜谱ID
	 */
	 private Long recipeId;
	
	/**
	 * 菜谱名称
	 */
	private String recipeName;
	
	/**
	 * 菜谱描述
	 */
	private String recipeDescription;
	
	/**
	 * 菜谱创建时间
	 */
	private String recipeImageUrl;
	
	/**
	 * 菜谱组成
	 */
	private Dictionary<Long, Integer> recipeComposition;
	
	/**
	 * 菜谱创建时间
	 */
	private long createdTime;
	
	/**
	 * 菜谱最后修改时间
	 */
	private long lastModifiedTime;
	
	/**
	 * 菜谱创建者ID
	 */
	private long createdBy;
	
	/**
	 * 菜谱状态
	 */
	private String status;
}
