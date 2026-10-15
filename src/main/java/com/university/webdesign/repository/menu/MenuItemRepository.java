package com.university.webdesign.repository.menu;

import com.university.webdesign.domain.menu.MenuItem;
import com.university.webdesign.domain.menu.MenuStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 菜单项数据访问接口，对应表 {@code menu_item}。
 * <p>
 * 除常规查询外提供两项 M1 专用能力：
 * <ul>
 *     <li>{@link #deleteAllByMenuId(Long)}：草稿菜单整体重排菜品时先清空旧项；</li>
 *     <li>{@link #countMenusReferencing(Long, MenuStatus)}：菜品下架前判断是否被
 *         “已发布菜单”引用，被引用时抛 42203 数据保护。</li>
 * </ul>
 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Long>
{
	/**
	 * 查询某菜单的全部菜单项，按菜单项ID升序（即编排顺序）
	 *
	 * @param menuId 菜单ID
	 * @return 菜单项列表
	 */
	List<MenuItem> findByMenuIdOrderByItemIdAsc(Long menuId);

	/**
	 * 批量查询多个菜单的菜单项，供历史菜单分页时一次性取数（避免 N+1）
	 *
	 * @param menuIds 菜单ID集合
	 * @return 菜单项列表
	 */
	List<MenuItem> findByMenuIdInOrderByMenuIdAscItemIdAsc(Collection<Long> menuIds);

	/**
	 * 统计某菜单的菜单项数
	 *
	 * @param menuId 菜单ID
	 * @return 菜单项数
	 */
	long countByMenuId(Long menuId);

	/**
	 * 清空某菜单的全部菜单项（仅草稿菜单可调用）
	 *
	 * @param menuId 菜单ID
	 * @return 删除行数
	 */
	@Modifying
	@Query("delete from MenuItem i where i.menuId = :menuId")
	int deleteAllByMenuId(@Param("menuId") Long menuId);

	/**
	 * 统计引用某菜品的指定状态菜单数
	 *
	 * @param recipeId 菜品ID
	 * @param status   菜单状态，{@code PUBLISHED} 用于数据保护判定
	 * @return 菜单数（去重）
	 */
	@Query("select count(distinct i.menuId) from MenuItem i "
			+ "where i.recipeId = :recipeId "
			+ "and i.menuId in (select m.menuId from Menu m where m.status = :status)")
	long countMenusReferencing(@Param("recipeId") Long recipeId, @Param("status") MenuStatus status);
}
