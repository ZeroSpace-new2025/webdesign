package com.university.webdesign.api.menu;

import lombok.Data;

/**
 * 创建菜单草稿的响应（M1-08）。
 * <p>
 * 对应《对外方法表》M1-08 的返回：{@code menuId} 与 {@code version}，
 * 另附 {@code status}（新建草稿固定为 {@code DRAFT}）便于前端直接跳转编辑页。
 */
@Data
public class MenuCreateResultVO
{
	/**
	 * 新菜单ID
	 */
	private Long menuId;

	/**
	 * 新菜单版本号（草稿从 0 开始，每次改动自增）
	 */
	private Integer version;

	/**
	 * 菜单状态编码，新建时为 {@code DRAFT}
	 */
	private String status;
}
