package com.university.webdesign.menurecipe.api;

import java.math.BigDecimal;

/**
 * 菜单项数据（跨模块读模型）
 * <p>
 * 菜单模块 `api` 包中的 {@link MenuDTO#getMenuItems()} 目前是无泛型的 {@code java.util.List}，
 * 无法在编译期取到菜单项字段，这里约定菜单模块对外暴露的菜单项结构：
 * <ul>
 *     <li>{@code itemId}：菜品（菜谱）ID；</li>
 *     <li>{@code itemName}：菜名；</li>
 *     <li>{@code category}：菜品分类，用于后厨生产单按分类汇总；</li>
 *     <li>{@code price}：菜单层级定价，订单按此价格生成快照。</li>
 * </ul>
 * <p>
 * //todo 确认：该记录需与菜品与菜单中心（方家乐）对齐——确认菜单项对外字段名与定价语义，
 * 并将 {@code MenuDTO.menuItems} 改为 {@code List<MenuItemData>} 以获得编译期类型安全。
 *
 * @param itemId   菜品（菜谱）ID
 * @param itemName 菜名
 * @param category 菜品分类
 * @param price    菜单售价（单价）
 */
public record MenuItemData(Long itemId, String itemName, String category, BigDecimal price)
{
}
