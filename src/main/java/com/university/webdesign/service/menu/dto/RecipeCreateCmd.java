package com.university.webdesign.service.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增菜品请求（M1-01）。
 * <p>
 * 入参对应《对外方法表》M1-01：{@code name}、{@code categoryId}、{@code imageUrl}、
 * {@code unit}、{@code unitPrice}、{@code description}。
 * 分类字典当前由 {@code recipe.category} 去重派生，因此允许直接传分类名
 * {@link #category}（新分类自动登记）；两者都传时 {@link #categoryId} 必须与分类名匹配。
 * 操作人身份取自登录上下文，不在请求体中传递。
 * <p>
 * 字段上同时声明 Jakarta Bean Validation 约束：api 层的必填/范围校验失败由
 * {@code GlobalExceptionHandler} 统一转 40001，service 层仍保留同样的业务校验兜底。
 */
@Data
public class RecipeCreateCmd
{
	/**
	 * 菜品名称，必填且全局唯一（重复抛 40901）
	 */
	@NotBlank(message = "菜品名称不能为空")
	@Size(max = 100, message = "菜品名称不能超过 100 个字符")
	private String name;

	/**
	 * 菜品分类名，如“荤菜”；与 {@link #categoryId} 至少提供一个
	 */
	@Size(max = 50, message = "分类名不能超过 50 个字符")
	private String category;

	/**
	 * 菜品分类ID：派生分类的稳定哈希（{@code abs(categoryName.hashCode())}），可为空
	 */
	private Long categoryId;

	/**
	 * 菜品图片地址（由 {@code POST /recipes/images} 上传后回填，如 {@code /uploads/xxx.png}）
	 */
	@Size(max = 500, message = "图片地址不能超过 500 个字符")
	private String imageUrl;

	/**
	 * 计量单位，如“份”
	 */
	@Size(max = 20, message = "计量单位不能超过 20 个字符")
	private String unit;

	/**
	 * 标准单价（元），必填且不小于 0
	 */
	@NotNull(message = "菜品单价不能为空")
	@DecimalMin(value = "0.00", message = "菜品单价必须为不小于 0 的数字")
	private BigDecimal unitPrice;

	/**
	 * 菜品描述
	 */
	@Size(max = 1000, message = "菜品描述不能超过 1000 个字符")
	private String description;
}
