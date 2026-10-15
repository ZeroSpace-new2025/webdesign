package com.university.webdesign.service.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新菜品请求（M1-02）。
 * <p>
 * 入参对应《对外方法表》M1-02：{@code name}、{@code categoryId}、{@code unit}、
 * {@code unitPrice}、{@code description}、{@code version}。
 * 只改 {@code recipe} 表本体，**不触碰任何快照与历史订单**；
 * {@link #version} 与库中不一致时抛 40902（乐观锁）。
 */
@Data
public class RecipeUpdateCmd
{
	/**
	 * 菜品名称，为空表示不修改；修改后仍需全局唯一（重复抛 40901）
	 */
	@Size(max = 100, message = "菜品名称不能超过 100 个字符")
	private String name;

	/**
	 * 菜品分类名，为空表示不修改
	 */
	@Size(max = 50, message = "分类名不能超过 50 个字符")
	private String category;

	/**
	 * 菜品分类ID（派生分类哈希），为空表示不修改；与 {@link #category} 同时给出时必须匹配
	 */
	private Long categoryId;

	/**
	 * 菜品图片地址，为空表示不修改
	 */
	@Size(max = 500, message = "图片地址不能超过 500 个字符")
	private String imageUrl;

	/**
	 * 计量单位，为空表示不修改
	 */
	@Size(max = 20, message = "计量单位不能超过 20 个字符")
	private String unit;

	/**
	 * 标准单价（元），为空表示不修改；给出时不得小于 0
	 */
	@DecimalMin(value = "0.00", message = "菜品单价必须为不小于 0 的数字")
	private BigDecimal unitPrice;

	/**
	 * 菜品描述，为空表示不修改
	 */
	@Size(max = 1000, message = "菜品描述不能超过 1000 个字符")
	private String description;

	/**
	 * 目标状态（{@code ACTIVE}/{@code DISABLED}），为空表示不修改状态。
	 * 仅用于把误下架的菜品重新置为可售；下架请走 M1-03 的 delete 接口。
	 */
	private String status;

	/**
	 * 乐观锁版本号，给出时与库中不一致即抛 40902
	 */
	private Integer version;
}
