package com.university.webdesign.web.order;

import com.university.webdesign.api.order.view.OrderHistoryPageView;
import com.university.webdesign.api.order.view.OrderPageView;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.domain.order.OrderStatus;
import com.university.webdesign.service.menu.MenuService;
import com.university.webdesign.service.menu.dto.MenuItemVO;
import com.university.webdesign.service.menu.dto.MenuVO;
import com.university.webdesign.service.order.OrderService;
import com.university.webdesign.service.order.dto.OrderHistoryQuery;
import com.university.webdesign.service.order.dto.OrderModifyCmd;
import com.university.webdesign.service.order.dto.OrderSubmitCmd;
import com.university.webdesign.service.order.dto.OrderVO;
import com.university.webdesign.service.order.dto.ServiceWindowVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 订单模块页面控制器（服务端渲染）。
 * <p>
 * 对应 `AGENTS.md` 的页面约定：页面控制器与 REST 控制器分开写，走 `/order/**` 返回视图名。
 * 本类只做“取数 + 组装视图”，**不写任何业务规则**：时间窗口、可否下单、可否取消
 * 全部由 {@link OrderService} 判定后展示。
 * <p>
 * 与重构前的差异：登录身份来自认证拦截器写入的 {@link UserContextHolder}，
 * 页面不再传 `operatorId`，也不再提供“切换身份”入口（那会绕过角色校验）。
 */
@Controller
@RequestMapping("/order")
public class OrderPageController
{
	private final OrderService orderService;
	private final MenuService menuService;

	public OrderPageController(OrderService orderService, MenuService menuService) {
		this.orderService = orderService;
		this.menuService = menuService;
	}

	/**
	 * 今日点餐页：展示时间窗口、今日菜单与当前员工今日已有订单
	 *
	 * @param model 视图模型
	 * @return 视图名
	 */
	@GetMapping("/create")
	public String createPage(Model model) {
		UserContext context = UserContextHolder.get();
		OrderPageView view = new OrderPageView();
		LocalDate today = LocalDate.now();
		view.setToday(today);
		if (context != null) {
			view.setUserId(context.userId());
			view.setUserName(context.name());
		}
		ServiceWindowVO window = orderService.getWindow(today);
		view.setCutoffTime(window.getCutoffTime() == null ? "09:00" : window.getCutoffTime().toString());
		view.setCanOrder(window.isCanOrder());

		MenuVO menu = menuService.getCurrent(today);
		if (menu != null) {
			OrderPageView.MenuVOView menuView = new OrderPageView.MenuVOView();
			menuView.setMenuId(menu.getMenuId());
			menuView.setName(menu.getName());
			List<MenuItemVO> items = menu.getItems() == null ? List.of() : menu.getItems();
			menuView.setItemCount(items.size());
			Map<String, List<MenuItemVO>> groups = new LinkedHashMap<>();
			for (MenuItemVO item : items) {
				String category = item.getCategory() == null || item.getCategory().isBlank()
						? "未分类" : item.getCategory();
				groups.computeIfAbsent(category, key -> new ArrayList<>()).add(item);
				menuView.getRecipeNames().add(item.getRecipeName());
			}
			view.setMenu(menuView);
			view.setMenuGroups(groups);
		}

		if (context != null && context.userId() != null) {
			OrderVO current = findTodayOrder(context.userId(), today);
			view.setCurrentOrder(current);
			if (current != null) {
				Map<Long, Integer> quantities = new LinkedHashMap<>();
				for (var item : current.getItems()) {
					quantities.merge(item.getRecipeId(), item.getQuantity() == null ? 0 : item.getQuantity(),
							Integer::sum);
				}
				view.setCurrentQuantities(quantities);
			}
		}
		model.addAttribute("data", view);
		return "ordertransaction/order-create";
	}

	/**
	 * 提交下单或改单
	 * <p>
	 * 已有今日订单走改单，否则走下单；幂等键在服务端生成，防止刷新导致重复提交。
	 *
	 * @param menuId   菜单ID
	 * @param params   全部表单参数（数量字段形如 {@code quantity_<菜品ID>}）
	 * @param redirect 重定向属性
	 * @return 重定向到点餐页
	 */
	@PostMapping("/submit")
	public String submitOrder(
			@RequestParam(value = "menuId", required = false) Long menuId,
			@RequestParam Map<String, String> params,
			RedirectAttributes redirect) {
		UserContext context = UserContextHolder.require();
		try {
			List<OrderSubmitCmd.SubmitItem> items = parseItems(params);
			if (items.isEmpty()) {
				throw new IllegalArgumentException("请至少选择一道菜品");
			}
			LocalDate today = LocalDate.now();
			OrderVO existing = findTodayOrder(context.userId(), today);
			if (existing != null) {
				OrderModifyCmd cmd = new OrderModifyCmd();
				cmd.setItems(items);
				cmd.setVersion(existing.getVersion());
				orderService.modify(existing.getOrderId(), cmd);
				redirect.addFlashAttribute("success", "改单成功，明细已按下单时刻快照更新");
			} else {
				OrderSubmitCmd cmd = new OrderSubmitCmd();
				cmd.setMenuId(menuId);
				cmd.setItems(items);
				cmd.setIdempotencyKey(UUID.randomUUID().toString());
				orderService.submit(cmd);
				redirect.addFlashAttribute("success", "下单成功");
			}
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/order/create";
	}

	/**
	 * 我的订单页：今日订单 + 历史订单区间查询
	 *
	 * @param start 起始日期（含），可选
	 * @param end   结束日期（含），可选
	 * @param all   是否查询全部历史
	 * @param model 视图模型
	 * @return 视图名
	 */
	@GetMapping("/mine")
	public String myOrdersPage(
			@RequestParam(value = "start", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(value = "end", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@RequestParam(value = "all", required = false, defaultValue = "false") boolean all,
			Model model) {
		UserContext context = UserContextHolder.get();
		OrderHistoryPageView view = new OrderHistoryPageView();
		ServiceWindowVO window = orderService.getWindow(LocalDate.now());
		view.setCutoffTime(window.getCutoffTime() == null ? "09:00" : window.getCutoffTime().toString());
		view.setShowAll(all);
		if (context == null || context.userId() == null) {
			model.addAttribute("data", view);
			return "ordertransaction/my-orders";
		}
		view.setUserId(context.userId());
		view.setUserName(context.name());
		view.setCanModify(window.isCanOrder());
		view.setTodayOrder(findTodayOrder(context.userId(), LocalDate.now()));

		LocalDate from = start;
		LocalDate to = end;
		if (!all && from == null && to == null) {
			YearMonth month = YearMonth.now();
			from = month.atDay(1);
			to = month.atEndOfMonth();
		}
		view.setStart(from);
		view.setEnd(to);

		OrderHistoryQuery query = new OrderHistoryQuery();
		query.setDateFrom(from);
		query.setDateTo(to);
		query.setPageNum(1);
		query.setPageSize(100);
		PageResult<OrderVO> history = orderService.pageHistory(query);

		List<OrderHistoryPageView.OrderRow> rows = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		for (OrderVO order : history.getList()) {
			OrderHistoryPageView.OrderRow row = new OrderHistoryPageView.OrderRow();
			row.setOrder(order);
			row.setCreatedAtText(order.getCreatedAt());
			boolean cancelled = OrderStatus.CANCELLED.name().equals(order.getStatus())
					|| OrderStatus.INVALID.name().equals(order.getStatus());
			row.setCancelled(cancelled);
			row.setCancellable(!cancelled && window.isCanOrder()
					&& OrderStatus.PENDING.name().equals(order.getStatus()));
			if (!cancelled && order.getTotalAmount() != null) {
				total = total.add(order.getTotalAmount());
			}
			rows.add(row);
		}
		view.setRows(rows);
		view.setOrderCount(rows.size());
		view.setTotalAmount(total);
		model.addAttribute("data", view);
		return "ordertransaction/my-orders";
	}

	/**
	 * 取消订单
	 *
	 * @param id         订单ID
	 * @param reason     取消原因
	 * @param redirectTo 取消后回到的页面
	 * @param redirect   重定向属性
	 * @return 重定向
	 */
	@PostMapping("/{id}/cancel")
	public String cancelOrder(
			@PathVariable("id") Long id,
			@RequestParam(value = "reason", required = false) String reason,
			@RequestParam(value = "redirectTo", required = false, defaultValue = "/order/mine") String redirectTo,
			RedirectAttributes redirect) {
		try {
			orderService.cancel(id, reason);
			redirect.addFlashAttribute("success", "订单已取消");
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:" + (redirectTo.startsWith("/order") ? redirectTo : "/order/mine");
	}

	// ------------------------------------------------------------------ 内部方法

	/**
	 * 取某员工当日的有效订单
	 *
	 * @param userId 员工ID
	 * @param date   就餐日期
	 * @return 有效订单；不存在时返回 null
	 */
	private OrderVO findTodayOrder(Long userId, LocalDate date) {
		OrderHistoryQuery query = new OrderHistoryQuery();
		query.setDateFrom(date);
		query.setDateTo(date);
		query.setPageNum(1);
		query.setPageSize(10);
		return orderService.pageHistory(query).getList().stream()
				.filter(order -> OrderStatus.PENDING.name().equals(order.getStatus())
						|| OrderStatus.VALID.name().equals(order.getStatus()))
				.findFirst()
				.orElse(null);
	}

	/**
	 * 解析表单中的数量字段（{@code quantity_<菜品ID>}），数量大于 0 的进入明细
	 *
	 * @param params 全部表单参数
	 * @return 下单明细列表
	 */
	private List<OrderSubmitCmd.SubmitItem> parseItems(Map<String, String> params) {
		List<OrderSubmitCmd.SubmitItem> items = new ArrayList<>();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (!entry.getKey().startsWith("quantity_")) {
				continue;
			}
			Long recipeId;
			try {
				recipeId = Long.valueOf(entry.getKey().substring("quantity_".length()));
			} catch (NumberFormatException exception) {
				continue;
			}
			int quantity = parseQuantity(entry.getValue());
			if (quantity <= 0) {
				continue;
			}
			OrderSubmitCmd.SubmitItem item = new OrderSubmitCmd.SubmitItem();
			item.setRecipeId(recipeId);
			item.setQuantity(quantity);
			items.add(item);
		}
		return items;
	}

	private int parseQuantity(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("数量必须是整数");
		}
	}
}
