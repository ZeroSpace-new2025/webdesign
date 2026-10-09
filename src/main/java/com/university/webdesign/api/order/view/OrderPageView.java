package com.university.webdesign.api.order.view;

import com.university.webdesign.service.menu.dto.MenuItemVO;
import com.university.webdesign.service.order.dto.OrderVO;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 今日点餐页视图数据。
 * <p>
 * 只承载页面展示需要的内容：时间窗口状态、今日菜单（按分类分组）、当前员工今日已有订单。
 * 所有业务判定（是否可下单、是否可改单）都由 {@code OrderService} / {@code ServiceWindowService}
 * 完成，这里只是判定结果的呈现，页面里不再硬编码任何业务规则。
 */
@Data
public class OrderPageView
{
	/**
	 * 当前登录员工ID
	 */
	private Long userId;

	/**
	 * 当前登录员工姓名
	 */
	private String userName;

	/**
	 * 就餐日期
	 */
	private LocalDate today;

	/**
	 * 订餐截止时间文本，如 09:00
	 */
	private String cutoffTime;

	/**
	 * 当前是否仍可下单/改单
	 */
	private boolean canOrder;

	/**
	 * 今日已发布菜单，未发布时为 null
	 */
	private MenuVOView menu;

	/**
	 * 按分类分组的菜单项，保持菜单给出的顺序
	 */
	private Map<String, List<MenuItemVO>> menuGroups = new LinkedHashMap<>();

	/**
	 * 当前员工今日已有的有效订单，为 null 表示走下单流程
	 */
	private OrderVO currentOrder;

	/**
	 * 已有订单的数量（菜品ID → 数量），用于回填改单表单
	 */
	private Map<Long, Integer> currentQuantities = new LinkedHashMap<>();

	/**
	 * 页面用的菜单头信息
	 */
	@Data
	public static class MenuVOView
	{
		/**
		 * 菜单ID
		 */
		private Long menuId;

		/**
		 * 菜单名称
		 */
		private String name;

		/**
		 * 菜单项数量
		 */
		private int itemCount;

		/**
		 * 菜品种类清单（供页面提示）
		 */
		private List<String> recipeNames = new ArrayList<>();
	}
}
