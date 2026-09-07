package com.university.webdesign.menurecipe.api;

import lombok.Data;

import java.util.List;

/**
 * 菜单数据类.
 */
@Data
public class MenuData
{
	/**
	 * 菜单 ID
	 */
	private String name;
	
	/**
	 * 菜单名称
	 */
	private String description;
	
	/**
	 * 菜单图片 URL
	 */
	private String imageUrl;
	
	/**
	 * 菜单包含的菜谱列表
	 */
	private List<RecipeData> recipes;
	
	/**
	 * 菜单状态
	 */
	private MenuState state;
}
