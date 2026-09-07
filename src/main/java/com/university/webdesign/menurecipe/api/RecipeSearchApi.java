package com.university.webdesign.menurecipe.api;

import org.jspecify.annotations.NonNull;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public interface RecipeSearchApi
{
	/**
	 * 根据菜品名称模糊搜索菜品
	 *
	 * @param name 菜品名称
	 * @return RecipeSearchApi 实例
	 * @remark 新条件会覆盖旧条件
	 */
	RecipeSearchApi withNameLike(@NonNull String name);
	
	/**
	 * 根据菜品描述模糊搜索菜品
	 *
	 * @param description 菜品描述
	 * @return RecipeSearchApi 实例
	 * @remark 新条件会覆盖旧条件
	 */
	RecipeSearchApi withDescriptionLike(@NonNull String description);
	
	/**
	 * 根据菜品价格区间搜索菜品
	 *
	 * @param minPrice 最低价格
	 * @param maxPrice 最高价格
	 * @return RecipeSearchApi 实例
	 * @remark 新条件会覆盖旧条件
	 */
	RecipeSearchApi withPriceBetween(int minPrice, int maxPrice);
	
	/**
	 * 根据菜品 ID 搜索菜品
	 *
	 * @param recipeId 菜品 ID
	 * @return RecipeSearchApi 实例
	 */
	RecipeSearchApi byId(long recipeId);
	
	/**
	 * 根据菜品 ID 列表搜索菜品
	 *
	 * @param recipeIds 菜品 ID 列表
	 * @return RecipeSearchApi 实例
	 */
	RecipeSearchApi byIds(@NonNull List<Long> recipeIds);
	
	/**
	 * 移除菜品 ID 搜索条件
	 *
	 * @param recipeId 菜品 ID
	 * @return RecipeSearchApi 实例
	 */
	RecipeSearchApi removeId(long recipeId);
	
	/**
	 * 移除菜品 ID 列表搜索条件
	 *
	 * @param recipeIds 菜品 ID 列表
	 * @return RecipeSearchApi 实例
	 */
	RecipeSearchApi removeIds(@NonNull List<Long> recipeIds);
	
	/**
	 * 根据店铺 ID 搜索菜品
	 *
	 * @param shopId 店铺 ID
	 * @return RecipeSearchApi 实例
	 * @remark 新条件会覆盖旧条件
	 */
	RecipeSearchApi byShopId(long shopId);
	
	/**
	 * 执行搜索操作
	 *
	 * @return 菜品数据类列表
	 * @remark 落库操作
	 */
	@NonNull
	List<RecipeData> execute();
}
