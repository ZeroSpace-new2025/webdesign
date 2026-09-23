package com.university.webdesign.menurecipe.api;

import lombok.Data;

import java.util.List;

/**
 * 菜单DTO
 *
 */
@Data
public class MenuDTO
{
	/**
	 * 菜单ID
	 */
	private Long id;
	
	/**
	 * 菜单名称
	 */
	private String name;
	
	/**
	 * 菜单描述
	 */
	private String description;
	
	/**
	 * 菜单项列表
	 * <p>
	 * //todo 确认：元素类型与菜品与菜单中心约定为 {@link MenuItemData}（菜品ID、菜名、分类、菜单售价）。
	 */
	private List<MenuItemData> menuItems;
	
	/**
	 * 菜单创建时间
	 */
	private long createdTime;
	
	/**
	 * 菜单最后修改时间
	 */
	private long lastModifiedTime;
	
	/**
	 * 菜单创建者ID
	 */
	private long createdBy;
	
	/**
	 * 菜单状态
	 */
	private String status;
}
