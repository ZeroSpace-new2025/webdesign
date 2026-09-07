package com.university.webdesign.menurecipe.api;

import org.jspecify.annotations.NonNull;

/**
 * 菜品 API 接口
 * @remark 所有涉及删除的操作都是逻辑删除，数据库中仍然保留数据
 */
@SuppressWarnings("UnusedDeclaration")
public interface RecipeApi
{
	/**
	 * 创建菜品
	 *
	 * @param recipeData 菜品数据类
	 * @return 菜品 ID
	 * @remark 落库操作
	 */
	long createRecipe(@NonNull RecipeData recipeData);
	
	/**
	 * 更新菜品
	 *
	 * @param recipeId 菜品 ID
	 * @param recipeData 菜品数据类
	 * @return 菜品 ID
	 * @remark 落库操作
	 */
	long updateRecipe(long recipeId, @NonNull RecipeData recipeData);
	
	/**
	 * 删除菜品
	 *
	 * @param recipeId 菜品 ID
	 * @return 菜品 ID
	 * @remark 落库操作
	 */
	long deleteRecipe(long recipeId);
	
	/**
	 * 获取菜品
	 *
	 * @param recipeId 菜品 ID
	 * @return 菜品 ID
	 * @remark 落库操作
	 */
	long getRecipeById(long recipeId);
	
	/**
	 * 设置菜品库存
	 *
	 * @param recipeId 菜品 ID
	 * @param inventory 库存数量
	 * @remark 落库操作
	 */
	void setRecipeInventory(long recipeId, int inventory);
	
	/**
	 * 获取菜品库存
	 *
	 * @param recipeId 菜品 ID
	 * @return 库存数量
	 * @remark 落库操作
	 */
	int getRecipeInventory(long recipeId);
}
