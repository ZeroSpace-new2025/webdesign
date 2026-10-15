package com.university.webdesign.service.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜单编排条目（M1-08 的 {@code items[]}）。
 * <p>
 * {@link #menuPrice} 为菜单级售价，为空时服务端取菜品标准单价填入；
 * {@link #quantity} 仅为兼容接口契约保留——按《重构实施规范》第 3 节，
 * {@code menu_item} 表没有数量列（数量属于下单时的 {@code order_detail} 快照），
 * 因此该字段不落库。
 */
@Data
public class MenuCreateItem
{
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
