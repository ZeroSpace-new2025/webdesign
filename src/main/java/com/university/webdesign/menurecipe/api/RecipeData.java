package com.university.webdesign.menurecipe.api;

import lombok.Data;

import java.util.List;

/**
 * 菜品数据类.
 */
@Data
public class RecipeData
{
	/**
	 * 菜品 ID
	 */
	private long id;
	
	/**
	 * 菜品名称
	 */
	private String name;
	
	/**
	 * 菜品描述
	 */
	private String description;
	
	/**
	 * 菜品图片 URL
	 */
	private String imageUrl;
	
	/**
	 * 菜品价格
	 */
	private int price;
	
	/**
	 * 菜品状态
	 */
	private RecipState state;
	
	/**
	 * 菜品创建时间
	 */
	private long createTime;
	
	/**
	 * 菜品由那些菜品组成
	 */
	private List<Long> componentIds = List.of();
}
