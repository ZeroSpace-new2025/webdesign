package com.university.webdesign.menurecipe.api;

import org.jspecify.annotations.NonNull;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public interface MenuSearchApi
{
	/**
	 * 根据时间区间搜索菜单
	 *
	 * @param startTime 开始时间，格式为 yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间，格式为 yyyy-MM-dd HH:mm:ss
	 * @return MenuSearchApi 实例
	 * @remark 新条件会覆盖旧条件
	 */
	MenuSearchApi withTimeBetween(long startTime,long endTime);
	
	/**
	 * 根据菜单状态搜索菜单
	 *
	 * @param state 菜单状态
	 * @return MenuSearchApi 实例
	 * @remark 新条件会覆盖旧条件
	 */
	MenuSearchApi byState(@NonNull MenuState state);
	
	/**
	 * 根据菜单名称模糊搜索菜单
	 *
	 * @param name 菜单名称
	 * @return MenuSearchApi 实例
	 * @remark 新条件会覆盖旧条件
	 */
	MenuSearchApi byNameLike(@NonNull String name);
	
	/**
	 * 执行搜索
	 *
	 * @return 菜单数据列表
	 */
	List<MenuData> execute();
}
