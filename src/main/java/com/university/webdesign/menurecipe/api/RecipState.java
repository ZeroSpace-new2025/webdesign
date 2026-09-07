package com.university.webdesign.menurecipe.api;

/**
 * 菜品状态枚举类.
 */
public enum RecipState
{
	/**
	 * 菜品存在
	 */
	EXIST,
	/**
	 * 菜品不存在
	 */
	NOT_EXIST,
	/**
	 * 菜品已删除
	 */
	DELETED,
	/**
	 * 菜品已过期
	 */
	EXPIRED,
	/**
	 * 菜品草稿
	 */
	DRAFT,
	/**
	 * 菜品已发布
	 */
	PUBLISHED
}
