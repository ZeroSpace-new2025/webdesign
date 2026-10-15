package com.university.webdesign.service.menu.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单发布结果视图（M1-10）。
 * <p>
 * 字段对应《对外方法表》M1-10 的返回（{@code status}、{@code publishedAt}、
 * {@code snapshotCount}）并补充 {@code menuId}、{@code version}，
 * 便于前端确认“发布的是哪一份菜单的哪个版本、冻结了多少条快照”。
 */
@Data
public class PublishResultVO
{
	/**
	 * 菜单ID
	 */
	private Long menuId;

	/**
	 * 发布后的状态编码，固定为 {@code PUBLISHED}
	 */
	private String status;

	/**
	 * 发布时间
	 */
	private LocalDateTime publishedAt;

	/**
	 * 本次冻结的快照条数
	 */
	private Integer snapshotCount;

	/**
	 * 发布后的菜单版本号
	 */
	private Integer version;
}
