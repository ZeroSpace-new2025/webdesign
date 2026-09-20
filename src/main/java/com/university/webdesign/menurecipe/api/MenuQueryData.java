package com.university.webdesign.menurecipe.api;

import lombok.Data;

import java.util.List;

@Data
public class MenuQueryData
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
	private Long startCreatedTime;
	
	/**
	 * 菜单描述
	 */
	private Long endCreatedTime;
	
	/**
	 * 菜单创建者ID
	 */
	private Long startLastModifiedTime;
	
	/**
	 * 菜单创建者ID
	 */
	private Long endLastModifiedTime;
	
	/**
	 * 菜单创建者ID
	 */
	private List<Long> createdBy;
	
	/**
	 * 菜单状态
	 */
	private String status;
}
