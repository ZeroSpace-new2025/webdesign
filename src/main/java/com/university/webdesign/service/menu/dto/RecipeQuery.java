package com.university.webdesign.service.menu.dto;

import com.university.webdesign.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 菜品分页查询条件（M1-05）。
 * <p>
 * 对应《对外方法表》M1-05：名称模糊（{@link #keyword}）+ 分类（{@link #categoryId} 或
 * {@link #category}）+ 状态（{@link #status}）+ 价格区间（{@link #minPrice} ~ {@link #maxPrice}），
 * 分页参数继承 {@link PageQuery}（页码从 1 开始，每页上限 100）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecipeQuery extends PageQuery
{
	/**
	 * 菜品名称关键字，模糊匹配，为空表示不限
	 */
	private String keyword;

	/**
	 * 分类ID（派生分类哈希），为空表示不限
	 */
	private Long categoryId;

	/**
	 * 分类名，优先于 {@link #categoryId} 使用，为空表示不限
	 */
	private String category;

	/**
	 * 状态编码：ACTIVE / DISABLED，为空表示不限
	 */
	private String status;

	/**
	 * 价格区间下限（含），为空表示不限
	 */
	private BigDecimal minPrice;

	/**
	 * 价格区间上限（含），为空表示不限
	 */
	private BigDecimal maxPrice;
}
