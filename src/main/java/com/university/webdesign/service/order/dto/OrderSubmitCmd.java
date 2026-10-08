package com.university.webdesign.service.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 下单请求（下单主流程入参）。
 * <p>
 * 对应《对外方法表》M2-02：`menuId`、`items[{menuItemId, recipeId, quantity}]`、`remark`、
 * `Idempotency-Key`（必填，由 Controller 从请求头取后写入 {@link #idempotencyKey}）。
 * 员工身份不再由请求体携带，服务层从登录上下文取。
 */
@Data
public class OrderSubmitCmd
{
	/**
	 * 当日菜单ID，用于校验订单只能取当日已发布菜单
	 */
	@NotNull(message = "菜单ID不能为空")
	private Long menuId;

	/**
	 * 明细列表
	 */
	@NotEmpty(message = "请至少选择一道菜品")
	private List<SubmitItem> items;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 幂等键，来自 `Idempotency-Key` 请求头
	 */
	private String idempotencyKey;

	/**
	 * 下单明细
	 */
	@Data
	public static class SubmitItem
	{
		/**
		 * 菜单项ID（`menu_item.id`），用于校验菜品确实在本菜单内
		 */
		private Long menuItemId;

		/**
		 * 菜品（菜谱）ID
		 */
		@NotNull(message = "菜品ID不能为空")
		private Long recipeId;

		/**
		 * 数量，必须为正整数
		 */
		@NotNull(message = "数量不能为空")
		private Integer quantity;
	}
}
