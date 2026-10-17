package com.university.webdesign.ordertransaction.api;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuItemData;
import lombok.Data;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 点餐页（今日点餐）视图数据
 * <p>
 * 供 Thymeleaf 模板渲染使用，只承载页面展示需要的数据：
 * 时间窗口状态、今日菜单（按分类分组）以及当前员工今日已有订单（用于回填改单表单）。
 * 所有业务规则仍由 {@link com.university.webdesign.ordertransaction.service.OrderService} 判定，
 * 这里的字段只是判定结果的呈现。
 */
@Data
public class OrderPageData
{
	/**
	 * 当前操作员工ID，尚未设置身份时为 null
	 */
	private Long userId;
	
	/**
	 * 就餐日期（业务时区下的“今天”）
	 */
	private LocalDate today;
	
	/**
	 * 订餐截止时间文本，例如 09:00，用于页面提示
	 */
	private String cutoffTime;
	
	/**
	 * 当前是否仍在可下单窗口内（截止前）
	 */
	private boolean canOrder;
	
	/**
	 * 今日已发布菜单，尚未发布时为 null
	 */
	private MenuDTO menu;
	
	/**
	 * 按菜品分类分组的菜单项，保持菜单给出的顺序，供页面分组展示
	 */
	private Map<String, List<MenuItemData>> menuGroups = new LinkedHashMap<>();
	
	/**
	 * 当前员工今日已有的有效订单，没有时为 null（为 null 表示走下单，否则走改单）
	 */
	private OrderDTO currentOrder;
	
	/**
	 * 已有订单的菜品数量（菜品ID → 数量），用于回填改单表单
	 */
	private Map<Long, Integer> currentQuantities = new LinkedHashMap<>();
}
