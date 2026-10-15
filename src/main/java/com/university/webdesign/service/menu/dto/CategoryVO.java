package com.university.webdesign.service.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜品分类视图对象（M1-07）。
 * <p>
 * 对应《对外方法表》M1-07：供菜单编排、生产单按分类统计使用。
 * <p>
 * **当前实现说明**：分类字典由 {@code recipe.category} 去重聚合得到（忽略空值），
 * 不是独立的主数据；{@link #categoryId} 取分类名的稳定哈希
 * {@code (long) Math.abs(categoryName.hashCode())}。
 * 未来可独立建分类表（{@code category}）并改由表主键充当 {@code categoryId}，
 * 届时调用方无需改动（字段语义不变）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO
{
	/**
	 * 分类ID：当前为分类名的稳定哈希，未来可替换为分类表主键
	 */
	private Long categoryId;

	/**
	 * 分类名称，如“荤菜”
	 */
	private String categoryName;

	/**
	 * 该分类下的菜品数
	 */
	private long recipeCount;
}
