package com.university.webdesign.menurecipe.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 菜品数据访问层
 */
public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

	/**
	 * 判断是否已存在同名菜品（排除自身）
	 */
	boolean existsByRecipeNameAndRecipeIdNot(String recipeName, Long recipeId);

	/**
	 * 判断是否已存在同名菜品
	 */
	boolean existsByRecipeName(String recipeName);
}
