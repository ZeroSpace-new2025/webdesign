package com.university.webdesign.service.menu.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜单明细视图对象。
 * <p>
 * 字段对应《对外方法表》M1 的 {@code MenuItemVO}：
 * {@code itemId(菜谱ID)}、{@code recipeId}、{@code recipeName}、{@code category}、
 * {@code unit}、{@code unitPrice}、{@code menuPrice}、{@code imageUrl}。
 * <p>
 * **下单取价字段是 {@link #menuPrice}**：菜单级价格优先，菜单价为空时回退
 * {@link #unitPrice}，因此服务层输出的 {@code menuPrice} 一定不为空（M2 下单直接取用）。
 * {@link #itemId} 沿用《对外方法表》命名保留“菜谱ID”语义，与 {@link #recipeId} 同值。
 */
@Data
public class MenuItemVO
{
	/**
	 * 菜谱ID（沿用《对外方法表》字段名，与 {@link #recipeId} 同值）
	 */
	private Long itemId;

	/**
	 * 菜单项ID（{@code menu_item.item_id}），快照明细（已发布菜单）为空
	 */
	private Long menuItemId;

	/**
	 * 菜品（食谱）ID，下单校验“菜品是否在本菜单内”用
	 */
	private Long recipeId;

	/**
	 * 菜品名称（已发布菜单取冻结快照）
	 */
	private String recipeName;

	/**
	 * 菜品分类（已发布菜单取冻结快照）
	 */
	private String category;

	/**
	 * 计量单位（已发布菜单取冻结快照）
	 */
	private String unit;

	/**
	 * 菜品标准单价（元）
	 */
	private BigDecimal unitPrice;

	/**
	 * 实际下单取价（元）：菜单级价格优先，为空回退 {@link #unitPrice}
	 */
	private BigDecimal menuPrice;

	/**
	 * 菜品图片地址
	 */
	private String imageUrl;
}
