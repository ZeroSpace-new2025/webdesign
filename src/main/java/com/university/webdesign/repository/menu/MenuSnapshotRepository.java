package com.university.webdesign.repository.menu;

import com.university.webdesign.domain.menu.MenuSnapshot;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 菜单发布快照数据访问接口，对应表 {@code menu_snapshot}。
 * <p>
 * **数据保护核心**：快照表只允许插入，不允许更新或删除，因此本接口刻意只继承标记接口
 * {@link Repository} 并暴露“保存 + 查询”方法，不暴露任何 delete/update 入口，
 * 从代码层面保证已发布菜单与历史订单的取价数据不可被改写。
 */
public interface MenuSnapshotRepository extends Repository<MenuSnapshot, Long>
{
	/**
	 * 保存一条快照（仅新增场景调用）
	 *
	 * @param snapshot 快照实体
	 * @param <S>      快照类型
	 * @return 保存后的实体
	 */
	<S extends MenuSnapshot> S save(S snapshot);

	/**
	 * 查询某菜单的冻结快照，按快照ID升序（即冻结时的菜品顺序）
	 *
	 * @param menuId 菜单ID
	 * @return 快照列表
	 */
	List<MenuSnapshot> findByMenuIdOrderBySnapshotIdAsc(Long menuId);

	/**
	 * 批量查询多个菜单的快照，供历史菜单分页时一次性取数（避免 N+1）
	 *
	 * @param menuIds 菜单ID集合
	 * @return 快照列表
	 */
	List<MenuSnapshot> findByMenuIdInOrderByMenuIdAscSnapshotIdAsc(Collection<Long> menuIds);

	/**
	 * 统计某菜单已冻结的快照数
	 *
	 * @param menuId 菜单ID
	 * @return 快照数
	 */
	long countByMenuId(Long menuId);

	/**
	 * 判断某菜单是否已冻结快照
	 *
	 * @param menuId 菜单ID
	 * @return 是否已冻结
	 */
	boolean existsByMenuId(Long menuId);
}
