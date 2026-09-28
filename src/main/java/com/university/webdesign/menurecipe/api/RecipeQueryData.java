package com.university.webdesign.menurecipe.api;

import lombok.Data;

@Data
public class RecipeQueryData
{
	/**
	 * 菜谱名称
	 */
	private String recipeName;
	
	/**
	 * 菜谱描述
	 */
	private Long recipeId;
	
	/**
	 * 菜谱创建者ID
	 */
	private Long createdBy;
	
	/**
	 * 菜谱状态
	 */
	private Long startCreatedTime;
	
	/**
	 * 菜谱状态
	 */
	private Long endCreatedTime;
	
	/**
	 * 菜谱状态
	 */
	private Long startLastModifiedTime;
	
	/**
	 * 菜谱状态
	 */
	private Long endLastModifiedTime;
}
