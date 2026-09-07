package com.university.webdesign.ordertransaction.impl;

import com.university.webdesign.menurecipe.api.RecipeData;
import com.university.webdesign.ordertransaction.api.*;
import com.university.webdesign.ordertransaction.data.Order;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class OrderImpl implements OrderApi
{
	private final Order order;
	
	public OrderImpl(long shopId, long userId,long time)
	{
		order = new Order();
		order.setUserId(userId);
		order.setTime(time);
		order.setRecipeIds(new ArrayList<>());
	}
	
	@Override
	public OrderApi addRecipe(RecipeData recipeData) {
		order.getRecipeIds().add(recipeData.getId());
		return this;
	}
	
	@Override
	public OrderApi addRecipes(List<RecipeData> recipeData)
	{
		order.getRecipeIds().addAll(recipeData.stream().map(RecipeData::getId).toList());
		return this;
	}
	
	@Override
	public OrderApi cleanRecipes()
	{
		order.getRecipeIds().clear();
		return this;
	}
	
	@Override
	public OrderApi removeRecipe(RecipeData recipeData)
	{
		order.getRecipeIds().remove(recipeData.getId());
		return this;
	}
	
	@Override
	public OrderApi removeRecipes(List<RecipeData> recipeData)
	{
		order.getRecipeIds().removeAll(recipeData.stream().map(RecipeData::getId).toList());
		return this;
	}
	
	@Override
	public long uploadOrder()
	{
		return 0;//todo 数据库操作
	}
	
	@Override
	public void deleteOrder()
	{
		order.getRecipeIds().clear();
	}
	
	@Override
	public OrderData getOrder()
	{
		return new OrderData(order);
	}
	
}
