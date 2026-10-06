package com.university.webdesign.menurecipe.service;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuQueryData;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜单服务。
 * 负责从菜品库组装菜单、菜单版本控制、发布下架以及菜单层级定价调整。
 */
public interface MenuService {

	/**
	 * 根据ID获取菜单
	 */
	MenuDTO getById(Long id);

	/**
	 * 按条件查询菜单
	 */
	List<MenuDTO> query(MenuQueryData queryData);

	/**
	 * 获取全部菜单
	 */
	List<MenuDTO> getAll();

	/**
	 * 新建菜单（可携带菜单项）
	 */
	MenuDTO create(MenuDTO menuDTO);

	/**
	 * 修改菜单基本信息或其菜单项
	 */
	MenuDTO update(MenuDTO menuDTO);

	/**
	 * 删除菜单
	 */
	void delete(Long id);

	/**
	 * 发布菜单，发布后可供点餐
	 */
	MenuDTO publish(Long id);

	/**
	 * 下架菜单
	 */
	MenuDTO offline(Long id);

	/**
	 * 复制一份历史菜单，便于复用（生成草稿副本）
	 */
	MenuDTO copyMenu(Long sourceId);

	/**
	 * 在菜单层级调整指定菜品的售价
	 */
	MenuDTO adjustItemPrice(Long menuId, Long itemId, BigDecimal price);
}
