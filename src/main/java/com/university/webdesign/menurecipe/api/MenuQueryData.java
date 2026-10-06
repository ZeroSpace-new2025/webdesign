package com.university.webdesign.menurecipe.api;

import lombok.Data;

import java.util.List;

/**
 * 菜单查询条件
 */
@Data
public class MenuQueryData {

	/**
	 * 菜单ID
	 */
	private Long id;

	/**
	 * 菜单名称（模糊匹配）
	 */
	private String name;

	/**
	 * 状态
	 */
	private String status;

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

	/**
	 * 创建者ID集合
	 */
	private List<Long> createdBy;
}
