package com.university.webdesign.service.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建菜单草稿请求（M1-08）。
 * <p>
 * 入参对应《对外方法表》M1-08：{@code name}、{@code effectiveDate}、
 * {@code items[{recipeId, quantity, menuPrice}]}；创建结果固定为 {@code DRAFT}，
 * 菜品名称/分类/单位/图片从 {@code recipe} 复制成菜单项副本，支持菜单级调价。
 * 操作人身份取自登录上下文。
 * <p>
 * 字段上同时声明 Jakarta Bean Validation 约束：api 层校验失败由
 * {@code GlobalExceptionHandler} 统一转 40001，service 层仍保留同样的业务校验兜底。
 */
@Data
public class MenuCreateCmd
{
	/**
	 * 菜单名称，必填
	 */
	@NotBlank(message = "菜单名称不能为空")
	@Size(max = 100, message = "菜单名称不能超过 100 个字符")
	private String name;

	/**
	 * 菜单描述
	 */
	@Size(max = 1000, message = "菜单描述不能超过 1000 个字符")
	private String description;

	/**
	 * 生效日期，必填；点餐按“生效日期不晚于当日”的已发布菜单取价
	 */
	@NotNull(message = "菜单生效日期不能为空")
	private LocalDate effectiveDate;

	/**
	 * 菜单菜品列表，至少一条
	 */
	@NotEmpty(message = "菜单至少需要一道菜品")
	@Valid
	private List<MenuCreateItem> items = new ArrayList<>();
}
