package com.university.webdesign.service.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜单更新条目（M1-09 的 {@code items[]}）。
 * <p>
 * 更新采用“整体替换”语义：给出 {@code items} 时，草稿菜单的菜品集合被完全重排；
 * {@link #menuItemId} 仅供前端回显定位，服务端按 {@link #recipeId} 重建。
 * 与 {@link MenuCreateItem} 相同，{@link #quantity} 不落库（{@code menu_item} 无数量列）。
 */
@Data
public class MenuUpdateItem
{
	/**
	 * 既有菜单项ID（{@code menu_item.item_id}），可为空，仅用于前端回显
	 */
	private Long menuItemId;

	/**
	 * 菜品（食谱）ID，必填；菜品必须存在且为 ACTIVE
	 */
	@NotNull(message = "菜单项必须指定菜品ID")
	private Long recipeId;

	/**
	 * 份数：接口契约字段，菜单表不存数量，服务端不落库
	 */
	@Min(value = 1, message = "份数必须为正整数")
	private Integer quantity;

	/**
	 * 菜单级售价（元），为空表示沿用菜品标准单价
	 */
	@DecimalMin(value = "0.00", message = "菜单价必须为不小于 0 的数字")
	private BigDecimal menuPrice;
}
