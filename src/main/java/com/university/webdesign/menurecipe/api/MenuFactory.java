package com.university.webdesign.menurecipe.api;

import org.jspecify.annotations.NonNull;

@SuppressWarnings("UnusedDeclaration")
public class MenuFactory
{
	/**
	 * 创建一个新的 MenuCreatApi 实例
	 *
	 * @param name 菜单名称
	 * @param description 菜单描述
	 * @param imageUrl 菜单图片 URL
	 * @return MenuCreatApi 实例
	 */
	@NonNull
	public static MenuCreatApi createMenu(String name, String description, String imageUrl) {
		MenuData menuData = new MenuData();
		menuData.setName(name);
		menuData.setDescription(description);
		menuData.setImageUrl(imageUrl);
		menuData.setState(MenuState.DRAFT);
		//todo 替换为实际的 MenuCreatApi 实现类
		return null;//todo new MenuCreatApi(menuData);
	}
	
	/**
	 * 创建一个新的 MenuCreatApi 实例
	 *
	 * @return MenuCreatApi 实例
	 */
	@NonNull
	public static MenuCreatApi createMenu()
	{
		MenuData menuData = new MenuData();
		menuData.setName("name");
		menuData.setDescription("description");
		menuData.setImageUrl("imageUrl");
		menuData.setState(MenuState.DRAFT);
		//todo 替换为实际的 MenuCreatApi 实现类
		return null;//todo new MenuCreatApi(menuData);
	}
	
	/**
	 * 创建一个新的 MenuSearchApi 实例
	 *
	 * @return MenuSearchApi 实例
	 */
	@NonNull
	public static MenuSearchApi searchMenu()
	{
		//todo 替换为实际的 MenuSearchApi 实现类
		return null;//todo new MenuSearchApiImpl();
	}
}
