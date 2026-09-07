package com.university.webdesign.ordertransaction.api;

import com.university.webdesign.menurecipe.api.RecipeData;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public interface OrderApi
{
	/**
	 * 添加菜品到订单
	 *
	 * @param recipeData 订单数据类
	 * @return OrderApi 实例
	 */
	OrderApi addRecipe(RecipeData recipeData);
	
	/**
	 * 添加菜品到订单
	 *
	 * @param recipeData 订单数据类列表
	 * @return OrderApi 实例
	 */
	OrderApi addRecipes(List<RecipeData> recipeData);
	
	/**
	 * 清空订单中的菜品
	 *
	 * @return OrderApi 实例
	 */
	OrderApi cleanRecipes();
	
	/**
	 * 移除订单中的菜品
	 *
	 * @param recipeData 订单数据类
	 * @return OrderApi 实例
	 */
	OrderApi removeRecipe(RecipeData recipeData);
	
	/**
	 * 移除订单中的菜品
	 *
	 * @param recipeData 订单数据类列表
	 * @return OrderApi 实例
	 */
	OrderApi removeRecipes(List<RecipeData> recipeData);
	
	/**
	 * 上传/保存订单
	 *
	 * @return 订单 ID
	 * @remark 落库操作
	 */
	long uploadOrder();
	
	/**
	 * 删除订单
	 *
	 * @remark 只能删除未上传的订单，已支付的订单不能删除
	 */
	void deleteOrder();
	
	/**
	 * 获取订单数据
	 *
	 * @return 订单数据类
	 */
	OrderData getOrder();
}
