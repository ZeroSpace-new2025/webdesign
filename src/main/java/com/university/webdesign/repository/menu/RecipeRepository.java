package com.university.webdesign.repository.menu;

import com.university.webdesign.domain.menu.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 菜品（食谱）数据访问接口，对应表 {@code recipe}。
 * <p>
 * 只属于 M1 菜品与菜单中心；其他模块不得直接依赖本接口
 * （跨模块取菜名/分类请走 {@code RecipeService} 契约）。
 */
public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe>
{
	/**
	 * 判断菜品名是否已存在（新增时校验重名，冲突抛 40901）
	 *
	 * @param recipeName 菜品名称
	 * @return 是否存在
	 */
	boolean existsByRecipeName(String recipeName);

	/**
	 * 判断菜品名是否被其他菜品占用（更新时排除自身）
	 *
	 * @param recipeName 菜品名称
	 * @param recipeId   需要排除的菜品ID
	 * @return 是否存在同名菜品
	 */
	boolean existsByRecipeNameAndRecipeIdNot(String recipeName, Long recipeId);

	/**
	 * 去重查询全部分类名（分类字典当前由 {@code recipe.category} 派生，忽略空值）
	 *
	 * @return 分类名列表，按名称升序
	 */
	@Query("select distinct r.category from Recipe r where r.category is not null and r.category <> '' order by r.category")
	List<String> findDistinctCategories();

	/**
	 * 统计某分类下的菜品数
	 *
	 * @param category 分类名
	 * @return 菜品数
	 */
	long countByCategory(String category);
}
