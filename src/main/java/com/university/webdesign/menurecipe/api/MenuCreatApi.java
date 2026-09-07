package com.university.webdesign.menurecipe.api;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 菜单创建 API 接口
 * @remark 所有涉及删除的操作都是逻辑删除，数据库中仍然保留数据
 */
@SuppressWarnings("UnusedDeclaration")
public interface MenuCreatApi
{
	/**
	 * 添加菜品到菜单
	 *
	 * @param recipeId 菜品 ID
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi addRecipe(long recipeId);
	
	/**
	 * 添加菜品到菜单
	 *
	 * @param recipeData 菜品 ID 列表
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi addRecipes(@NonNull List<Long> recipeData);
	
	/**
	 * 移除菜单中的菜品
	 *
	 * @param recipeId 菜品 ID
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi removeRecipe(long recipeId);
	
	/**
	 * 移除菜单中的菜品
	 *
	 * @param recipeData 菜品 ID 列表
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi removeRecipes(@NonNull List<Long> recipeData);
	
	/**
	 * 更新菜单中的菜品
	 *
	 * @param recipeId 菜品 ID
	 * @param newRecipeId 新的菜品 ID
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi updateRecipe(long recipeId, long newRecipeId);
	
	/**
	 * 更新菜单中的菜品
	 *
	 * @param recipeData 菜品 ID 列表
	 * @param newRecipeData 新的菜品 ID 列表
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi updateRecipes(@NonNull List<Long> recipeData, @NonNull List<Long> newRecipeData);
	
	/**
	 * 清空菜单中的菜品
	 *
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi deleteRecipe(long recipeId);
	
	/**
	 * 清空菜单中的菜品
	 *
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi deleteRecipes(@NonNull List<Long> recipeData);
	
	/**
	 * 清空菜单中的菜品
	 *
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi clearRecipes();
	
	/**
	 * 设置菜单描述
	 *
	 * @param description 菜单描述
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi setDescription(@NonNull String description);
	
	/**
	 * 设置菜单图片 URL
	 *
	 * @param imageUrl 菜单图片 URL
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi setImageUrl(@NonNull String imageUrl);
	
	/**
	 * 设置菜单名称
	 *
	 * @param name 菜单名称
	 * @return MenuCreatApi 实例
	 */
	MenuCreatApi setName(@NonNull String name);
	
	/**
	 * 上传/保存菜单
	 *
	 * @return 菜单 ID
	 * @remark 落库操作
	 */
	long uploadMenu();
}