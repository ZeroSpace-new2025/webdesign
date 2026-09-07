package com.university.webdesign.menurecipe.api;

/**
 * 菜单接口
 */
@SuppressWarnings("UnusedDeclaration")
public interface MenuApi
{
	/**
	 * 获取菜单
	 *
	 * @param id 菜单id
	 * @return 菜单数据
	 */
	MenuData getMenu(long id);
	
	/**
	 * 发布菜单
	 *
	 * @param id 菜单id
	 * @return 是否发布成功
	 */
	boolean PublishMenu(long id);
	
	/**
	 * 下架菜单
	 *
	 * @param id 菜单id
	 * @return 是否下架成功
	 */
	boolean TakedownMenu(long id);
}
