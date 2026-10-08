package com.university.webdesign.service.menu.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单视图对象。
 * <p>
 * 字段对应《对外方法表》M1 的 {@code MenuVO}：{@code menuId}、{@code name}、
 * {@code status}、{@code version}、{@code effectiveDate}、{@code publishedAt}、
 * {@code offlineAt}、{@code items}。
 * <p>
 * 用途：
 * <ul>
 *     <li>{@code MenuService.getCurrent(date)} 返回当日已发布菜单 + **冻结快照明细**，
 *         是 M2 下单取价的唯一入口；</li>
 *     <li>{@code MenuService.page(q)} 在 {@code withItems=true} 时同样携带明细
 *         （已发布菜单取快照、草稿菜单取当前菜品），供历史菜单复用。</li>
 * </ul>
 * {@link #itemsByCategory} 是按分类分组的同一份明细，供点餐页与生产单直接渲染。
 */
@Data
public class MenuVO
{
	/**
	 * 菜单ID
	 */
	private Long menuId;

	/**
	 * 菜单名称
	 */
	private String name;

	/**
	 * 菜单描述
	 */
	private String description;

	/**
	 * 状态编码：DRAFT / PUBLISHED / OFFLINE
	 */
	private String status;

	/**
	 * 状态中文文案
	 */
	private String statusText;

	/**
	 * 乐观锁版本号（每次改动自增）
	 */
	private Integer version;

	/**
	 * 生效日期
	 */
	private LocalDate effectiveDate;

	/**
	 * 首次发布时间，未发布为空
	 */
	private LocalDateTime publishedAt;

	/**
	 * 下架时间，未下架为空
	 */
	private LocalDateTime offlineAt;

	/**
	 * 下架原因，未下架为空
	 */
	private String offlineReason;

	/**
	 * 是否已锁定：发布后为 true，不可编辑/删除，只能下架
	 */
	private boolean locked;

	/**
	 * 创建者用户ID
	 */
	private Long createdBy;

	/**
	 * 创建时间
	 */
	private LocalDateTime createdAt;

	/**
	 * 最后修改时间
	 */
	private LocalDateTime updatedAt;

	/**
	 * 菜单明细：下单取价按 {@code items[].menuPrice} 取；未请求明细时为空列表
	 */
	private List<MenuItemVO> items = new ArrayList<>();

	/**
	 * 菜单明细的分类分组视图（key 为分类名，无分类归入“未分类”），保持明细的原有顺序
	 */
	private Map<String, List<MenuItemVO>> itemsByCategory = new LinkedHashMap<>();
}
