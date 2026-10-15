package com.university.webdesign.service.menu.dto;

import com.university.webdesign.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 菜单分页查询条件（M1-13）。
 * <p>
 * 对应《对外方法表》M1-13：{@code dateFrom}、{@code dateTo}、{@code status}、
 * {@code menuId}、{@code withItems}，分页参数继承 {@link PageQuery}。
 * {@link #withItems} 为 {@code true} 时返回快照明细，供“历史菜单一键复用”生成新草稿。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MenuQuery extends PageQuery
{
	/**
	 * 生效日期区间下限（含），为空表示不限
	 */
	private LocalDate dateFrom;

	/**
	 * 生效日期区间上限（含），为空表示不限
	 */
	private LocalDate dateTo;

	/**
	 * 状态编码：DRAFT / PUBLISHED / OFFLINE，为空表示不限
	 */
	private String status;

	/**
	 * 菜单ID，用于按ID精确查询单份菜单，为空表示不限
	 */
	private Long menuId;

	/**
	 * 菜单名称关键字，模糊匹配，为空表示不限
	 */
	private String keyword;

	/**
	 * 是否携带明细：{@code true} 时已发布菜单返回冻结快照、草稿菜单返回当前菜品，
	 * 供历史菜单复用；默认 {@code false} 只返回菜单头信息
	 */
	private Boolean withItems = Boolean.FALSE;
}
