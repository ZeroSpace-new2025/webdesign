package com.university.webdesign.service.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新菜单请求（M1-09）。
 * <p>
 * 入参对应《对外方法表》M1-09：{@code name}、{@code effectiveDate}、{@code items[]}、
 * {@code version}。仅 {@code DRAFT} 菜单可改，每次改动版本号自增（返回新版本号）；
 * 非草稿菜单抛 40902，{@link #version} 与库中不一致同样抛 40902。
 */
@Data
public class MenuUpdateCmd
{
	/**
	 * 菜单名称，为空表示不修改
	 */
	@Size(max = 100, message = "菜单名称不能超过 100 个字符")
	private String name;

	/**
	 * 菜单描述，为空表示不修改
	 */
	@Size(max = 1000, message = "菜单描述不能超过 1000 个字符")
	private String description;

	/**
	 * 生效日期，为空表示不修改
	 */
	private LocalDate effectiveDate;

	/**
	 * 菜品列表：给出时整体替换草稿菜单的菜品集合（不可为空列表），为空表示不改动菜品
	 */
	@Valid
	private List<MenuUpdateItem> items;

	/**
	 * 乐观锁版本号，给出时与库中不一致即抛 40902
	 */
	private Integer version;
}
