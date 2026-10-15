package com.university.webdesign.service.menu;

/**
 * 菜单发布快照服务契约（M1 内部能力，无 HTTP 端点，供 {@code MenuService.publish} 直调）。
 * <p>
 * **数据保护核心**：把 {@code menu_item} 的菜名/分类/单位/单价/菜单价一次性写入
 * {@code menu_snapshot}，发布后菜品改名改价不再影响该菜单的取价结果，
 * 从而保证“改菜品只影响未来菜单，不影响历史订单与历史菜单”。
 * <p>
 * 快照表**只插不改**：本接口只提供冻结能力，不提供修改/删除入口。
 */
public interface MenuSnapshotService
{
	/**
	 * 冻结菜单快照（只插不改）
	 * <p>
	 * 读取 {@code menu_item} 当前内容并逐条写入 {@code menu_snapshot}（含冻结时间）。
	 * 幂等：若该菜单已存在快照，直接返回既有条数，不重复写入。
	 *
	 * @param menuId 菜单ID
	 * @return 快照条数
	 */
	int freeze(Long menuId);
}
