package com.university.webdesign.ordertransaction.api;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuItemData;
import com.university.webdesign.menurecipe.service.MenuService;
import com.university.webdesign.ordertransaction.data.OrderStatus;
import com.university.webdesign.ordertransaction.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*本类只包含订单相关的页面入口，不包含其他模块的页面。修改后要立即提交。*/
/*页面只做参数收集与结果展示，所有业务规则（时间窗口、一人一天一单、快照、权限）都在 OrderService 中。*/

/**
 * 订单页面入口（Thymeleaf 视图层）
 * <p>
 * 与 {@link OrderApi} 的分工：{@code OrderApi} 是对外的 REST 接口（{@code /api/order/**}，返回 JSON），
 * 本类只负责把订单模块的页面渲染出来（{@code /order/**}，返回模板视图名）。
 * 两者都只调用 {@link OrderService} 与菜单模块的 {@link MenuService} 契约接口，不碰任何实现类。
 * <p>
 * 页面清单（对应 AGENTS.md 与 demand.md 中“订单与交易核心”的功能）：
 * <ul>
 *     <li>{@code GET /order/create}：今日点餐页，展示今日已发布菜单，支持下单与截止前改单；</li>
 *     <li>{@code POST /order/submit}：提交下单/改单（今日已有有效订单时自动走改单）；</li>
 *     <li>{@code GET /order/mine}：我的订单页，个人历史查询 + 今日订单的支付/取消。</li>
 * </ul>
 * <p>
 * //todo 确认：登录态由用户与报表中心负责，目前尚未接入。
 * 当前把“当前员工”放在会话属性 {@value #SESSION_OPERATOR_ID} 中，由页面顶部的身份栏写入，
 * 第一次访问也可以通过 {@code ?operatorId=} 设置。接入 Spring Security 后应改为从认证上下文获取，
 * 并删除身份栏与 {@code /order/identity} 入口。
 */
@Controller
@RequestMapping("/order")
public class OrderPageApi
{
	/**
	 * 会话中保存“当前员工ID”的属性名
	 */
	private static final String SESSION_OPERATOR_ID = "order.operatorId";
	
	/**
	 * 菜单项没有分类时归入的分组名
	 */
	private static final String UNCATEGORIZED = "其他";
	
	/**
	 * 页面表单中“菜品数量”输入框的前缀，完整名字为 {@code quantity_<菜品ID>}
	 */
	private static final String QUANTITY_PARAM_PREFIX = "quantity_";
	
	/**
	 * 页面展示下单时间用的格式
	 */
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	
	/**
	 * 订单服务（跨模块契约）
	 */
	private final OrderService orderService;
	
	/**
	 * 菜单服务（跨模块契约）：点餐页需要展示“今日可点什么菜、什么价”
	 */
	private final MenuService menuService;
	
	/**
	 * 订餐截止时间文本，仅用于页面提示
	 * <p>
	 * //todo 确认：与 {@code OrderServiceImpl} 使用同一个配置项 {@code order.cutoff-time}。
	 * 这里只做展示，真正的判定一律调用 {@code OrderService.isBeforeCutoff(LocalDate)}，
	 * 避免页面与服务对“是否已截止”给出不同结论。
	 */
	private final String cutoffTime;
	
	/**
	 * 业务时区，用于计算页面上的“今天”
	 * <p>
	 * //todo 确认：与 {@code OrderServiceImpl} 使用同一个配置项 {@code order.zone}，
	 * 若后续把这两项抽到公共配置类，请一并迁移。
	 */
	private final ZoneId zoneId;
	
	/**
	 * 构造器注入订单服务、菜单服务与可配置的截止时间、时区
	 *
	 * @param orderService 订单服务
	 * @param menuService  菜单服务
	 * @param cutoffTime   订餐截止时间文本，默认 09:00
	 * @param zone         业务时区，留空时取系统时区
	 */
	public OrderPageApi(OrderService orderService,
			MenuService menuService,
			@Value("${order.cutoff-time:09:00}") String cutoffTime,
			@Value("${order.zone:}") String zone) {
		this.orderService = orderService;
		this.menuService = menuService;
		this.cutoffTime = cutoffTime;
		this.zoneId = zone == null || zone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(zone);
	}
	
	// ------------------------------------------------------------------ 今日点餐
	
	/**
	 * 今日点餐页
	 *
	 * @param operatorId 可选：首次进入时用 {@code ?operatorId=} 指定当前员工，之后记入会话
	 * @param session    会话
	 * @param model      视图数据
	 * @return 模板视图名
	 */
	@GetMapping("/create")
	public String createPage(@RequestParam(value = "operatorId", required = false) Long operatorId,
			HttpSession session, Model model) {
		Long userId = resolveOperatorId(operatorId, session);
		model.addAttribute("data", buildCreatePageData(userId));
		model.addAttribute("activePage", "create");
		return "ordertransaction/order-create";
	}
	
	/**
	 * 提交下单或改单
	 * <p>
	 * 今日已有有效订单时按改单处理（“一人一天一单”），否则新下单。
	 * 时间窗口、菜品是否在菜单内、数量合法性都由服务层判定，这里只把失败信息回显到页面。
	 *
	 * @param operatorId         可选：首次提交时用 {@code ?operatorId=} 指定当前员工，之后记入会话
	 * @param request            原始请求，用于读取 {@code quantity_<菜品ID>} 形式的数量
	 * @param session            会话
	 * @param redirectAttributes 重定向后的提示消息
	 * @return 重定向到今日点餐页
	 */
	@PostMapping("/submit")
	public String submitOrder(@RequestParam(value = "operatorId", required = false) Long operatorId,
			HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
		Long userId = resolveOperatorId(operatorId, session);
		if (userId == null) {
			return redirectWithMessage(redirectAttributes, "error", "/order/create",
					"请先设置员工身份（员工ID）后再点餐");
		}
		try {
			List<Long> itemIds = new ArrayList<>();
			List<Integer> quantities = new ArrayList<>();
			collectSelectedItems(request, itemIds, quantities);
			if (itemIds.isEmpty()) {
				throw new IllegalArgumentException("请至少选择一道菜品");
			}
			
			OrderDTO current = orderService.findActiveOrder(userId, today());
			OrderDTO saved;
			if (current == null) {
				OrderCreateData createData = new OrderCreateData();
				createData.setOperatorId(userId);
				createData.setItemIds(itemIds);
				createData.setQuantities(quantities);
				saved = orderService.createOrder(createData);
				redirectAttributes.addFlashAttribute("success",
						"下单成功，订单号 " + saved.getOrderNumber() + "，共计 " + saved.getTotal() + " 元");
			} else {
				OrderUpdateData updateData = new OrderUpdateData();
				updateData.setOrderId(current.getOrderId());
				updateData.setOperatorId(userId);
				updateData.setItemIds(itemIds);
				updateData.setQuantities(quantities);
				saved = orderService.updateOrder(updateData);
				redirectAttributes.addFlashAttribute("success",
						"订单已修改，订单号 " + saved.getOrderNumber() + "，共计 " + saved.getTotal() + " 元");
			}
		} catch (IllegalArgumentException | IllegalStateException e) {
			// 业务规则不满足：把原因回显给员工，不把异常抛成 500
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/order/create";
	}
	
	// ------------------------------------------------------------------ 我的订单
	
	/**
	 * 我的订单页：个人历史查询 + 今日订单操作
	 *
	 * @param start      起始日期（含），与 end 同时为空且未指定 all 时默认查本月
	 * @param end        结束日期（含）
	 * @param all        是否查询全部历史，为 true 时忽略日期范围
	 * @param operatorId 可选：首次进入时用 {@code ?operatorId=} 指定当前员工，之后记入会话
	 * @param session    会话
	 * @param model      视图数据
	 * @return 模板视图名
	 */
	@GetMapping("/mine")
	public String myOrdersPage(
			@RequestParam(value = "start", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(value = "end", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@RequestParam(value = "all", required = false) Boolean all,
			@RequestParam(value = "operatorId", required = false) Long operatorId,
			HttpSession session, Model model) {
		Long userId = resolveOperatorId(operatorId, session);
		LocalDate today = today();
		boolean canModify = orderService.isBeforeCutoff(today);
		
		OrderHistoryPageData data = new OrderHistoryPageData();
		data.setUserId(userId);
		data.setToday(today);
		data.setCutoffTime(cutoffTime);
		data.setCanModify(canModify);
		data.setShowAll(Boolean.TRUE.equals(all));
		if (!data.isShowAll()) {
			// 默认只看本月，避免历史订单把页面撑爆；需要全量时点“查询全部”
			if (start == null && end == null) {
				start = YearMonth.from(today).atDay(1);
				end = today;
			}
		}
		data.setStart(data.isShowAll() ? null : start);
		data.setEnd(data.isShowAll() ? null : end);
		
		if (userId != null) {
			// 只查自己的历史，操作人与被查人是同一个员工
			List<OrderDTO> orders = orderService.getHistoryOrders(userId, userId, data.getStart(), data.getEnd());
			data.setRows(toRows(orders, canModify, today));
			data.setOrderCount(data.getRows().size());
			data.setTotalAmount(sumActiveAmount(orders));
			OrderDTO todayOrder = orderService.findActiveOrder(userId, today);
			data.setTodayRow(todayOrder == null ? null : toRow(todayOrder, canModify, today));
		}
		
		model.addAttribute("data", data);
		model.addAttribute("activePage", "mine");
		return "ordertransaction/my-orders";
	}
	
	// ------------------------------------------------------------------ 今日订单操作
	
	/**
	 * 支付订单
	 *
	 * @param id                 订单ID
	 * @param redirectTo         操作完成后回到的模块内页面，留空时回到“我的订单”
	 * @param session            会话
	 * @param redirectAttributes 重定向后的提示消息
	 * @return 重定向
	 */
	@PostMapping("/{id}/pay")
	public String payOrder(@PathVariable("id") Long id,
			@RequestParam(value = "redirectTo", required = false) String redirectTo,
			HttpSession session, RedirectAttributes redirectAttributes) {
		Long userId = resolveOperatorId(null, session);
		String target = safeRedirect(redirectTo);
		if (userId == null) {
			return redirectWithMessage(redirectAttributes, "error", target, "请先设置员工身份（员工ID）");
		}
		try {
			OrderDTO paid = orderService.payOrder(id, userId);
			redirectAttributes.addFlashAttribute("success", "订单 " + paid.getOrderNumber() + " 已支付");
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:" + target;
	}
	
	/**
	 * 取消订单（员工在截止时间前取消自己的订单）
	 *
	 * @param id                 订单ID
	 * @param redirectTo         操作完成后回到的模块内页面，留空时回到“我的订单”
	 * @param session            会话
	 * @param redirectAttributes 重定向后的提示消息
	 * @return 重定向
	 */
	@PostMapping("/{id}/cancel")
	public String cancelOrder(@PathVariable("id") Long id,
			@RequestParam(value = "redirectTo", required = false) String redirectTo,
			HttpSession session, RedirectAttributes redirectAttributes) {
		Long userId = resolveOperatorId(null, session);
		String target = safeRedirect(redirectTo);
		if (userId == null) {
			return redirectWithMessage(redirectAttributes, "error", target, "请先设置员工身份（员工ID）");
		}
		try {
			OrderDTO cancelled = orderService.cancelOrder(id, userId);
			redirectAttributes.addFlashAttribute("success", "订单 " + cancelled.getOrderNumber() + " 已取消");
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:" + target;
	}
	
	// ------------------------------------------------------------------ 身份（登录态接入前的临时方案）
	
	/**
	 * 切换当前员工身份，写入会话后回到原页面
	 *
	 * @param operatorId 员工ID
	 * @param redirectTo 切换后回到的模块内页面，留空时回到今日点餐页
	 * @param session    会话
	 * @return 重定向
	 */
	@PostMapping("/identity")
	public String switchIdentity(@RequestParam("operatorId") Long operatorId,
			@RequestParam(value = "redirectTo", required = false) String redirectTo,
			HttpSession session) {
		if (operatorId != null && operatorId > 0) {
			session.setAttribute(SESSION_OPERATOR_ID, operatorId);
		}
		return "redirect:" + safeRedirect(redirectTo);
	}
	
	// ------------------------------------------------------------------ 内部方法
	
	/**
	 * 解析当前员工ID
	 * <p>
	 * 优先使用请求参数（首次进入时带上），其次读会话；两者都没有时返回 null，页面会提示先设置身份。
	 *
	 * @param operatorId 请求参数中的员工ID，可为 null
	 * @param session    会话
	 * @return 当前员工ID，未设置时返回 null
	 */
	private Long resolveOperatorId(Long operatorId, HttpSession session) {
		if (operatorId != null && operatorId > 0) {
			session.setAttribute(SESSION_OPERATOR_ID, operatorId);
			return operatorId;
		}
		Object stored = session.getAttribute(SESSION_OPERATOR_ID);
		return stored instanceof Long value ? value : null;
	}
	
	/**
	 * 只允许在订单模块自己的页面之间跳转，避免被构造成站外跳转
	 *
	 * @param redirectTo 目标路径
	 * @return 合法路径原样返回；非法或为空时返回“我的订单”页
	 */
	private String safeRedirect(String redirectTo) {
		if (redirectTo == null || !redirectTo.startsWith("/order/") || redirectTo.contains("//")) {
			return "/order/mine";
		}
		return redirectTo;
	}
	
	/**
	 * 组装重定向提示
	 *
	 * @param redirectAttributes 重定向属性
	 * @param level              提示级别：{@code success} 或 {@code error}
	 * @param path               目标页面
	 * @param message            提示内容
	 * @return 重定向视图名
	 */
	private String redirectWithMessage(RedirectAttributes redirectAttributes, String level, String path,
			String message) {
		redirectAttributes.addFlashAttribute(level, message);
		return "redirect:" + path;
	}
	
	/**
	 * 组装点餐页视图数据
	 *
	 * @param userId 当前员工ID，可为 null
	 * @return 点餐页视图数据
	 */
	private OrderPageData buildCreatePageData(Long userId) {
		LocalDate today = today();
		OrderPageData data = new OrderPageData();
		data.setUserId(userId);
		data.setToday(today);
		data.setCutoffTime(cutoffTime);
		// 是否可下单以服务层判定为准，页面上的截止时间只用来说明原因
		data.setCanOrder(orderService.isBeforeCutoff(today));
		data.setMenu(menuService.getActiveMenu(today));
		data.setMenuGroups(groupByCategory(data.getMenu()));
		if (userId != null) {
			OrderDTO current = orderService.findActiveOrder(userId, today);
			data.setCurrentOrder(current);
			data.setCurrentQuantities(quantitiesOf(current));
		}
		return data;
	}
	
	/**
	 * 把菜单项按分类分组，保持菜单给出的顺序
	 *
	 * @param menu 菜单，可为 null
	 * @return 分类 → 菜单项列表
	 */
	private Map<String, List<MenuItemData>> groupByCategory(MenuDTO menu) {
		Map<String, List<MenuItemData>> groups = new LinkedHashMap<>();
		if (menu == null || menu.getMenuItems() == null) {
			return groups;
		}
		for (MenuItemData item : menu.getMenuItems()) {
			if (item == null || item.itemId() == null) {
				continue;
			}
			String category = item.category() == null || item.category().isBlank()
					? UNCATEGORIZED : item.category();
			groups.computeIfAbsent(category, key -> new ArrayList<>()).add(item);
		}
		return groups;
	}
	
	/**
	 * 取已有订单的菜品数量，用于回填改单表单
	 *
	 * @param order 订单，可为 null
	 * @return 菜品ID → 数量
	 */
	private Map<Long, Integer> quantitiesOf(OrderDTO order) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		if (order == null || order.getItems() == null) {
			return quantities;
		}
		for (OrderItemDTO item : order.getItems()) {
			if (item.getItemId() != null && item.getQuantity() != null) {
				quantities.merge(item.getItemId(), item.getQuantity(), Integer::sum);
			}
		}
		return quantities;
	}
	
	/**
	 * 从表单读取已选菜品与数量
	 * <p>
	 * 数量输入框命名为 {@code quantity_<菜品ID>}，这里按今日菜单逐项读取：
	 * 只接受菜单内的菜品，数量为 0 或空表示不点这道菜。
	 * 这样既不会读进菜单外的菜品，也不依赖表单字段的提交顺序。
	 *
	 * @param request    原始请求
	 * @param itemIds    出参：菜品ID列表
	 * @param quantities 出参：与菜品ID一一对应的数量列表
	 */
	private void collectSelectedItems(HttpServletRequest request, List<Long> itemIds, List<Integer> quantities) {
		MenuDTO menu = menuService.getActiveMenu(today());
		if (menu == null || menu.getMenuItems() == null) {
			throw new IllegalStateException("今日菜单为空或未发布，暂时无法下单");
		}
		for (MenuItemData item : menu.getMenuItems()) {
			if (item == null || item.itemId() == null) {
				continue;
			}
			int quantity = parseQuantity(request.getParameter(QUANTITY_PARAM_PREFIX + item.itemId()));
			if (quantity > 0) {
				itemIds.add(item.itemId());
				quantities.add(quantity);
			}
		}
	}
	
	/**
	 * 解析数量输入
	 *
	 * @param raw 表单原始值
	 * @return 数量；空值或非法值按 0 处理
	 */
	private int parseQuantity(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0;
		}
		try {
			return Math.max(0, Integer.parseInt(raw.trim()));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	/**
	 * 历史订单 → 页面行
	 *
	 * @param orders    历史订单
	 * @param canModify 当前是否仍在可修改窗口内
	 * @param today     就餐日期（今天）
	 * @return 页面行列表
	 */
	private List<OrderHistoryPageData.OrderRow> toRows(List<OrderDTO> orders, boolean canModify, LocalDate today) {
		List<OrderHistoryPageData.OrderRow> rows = new ArrayList<>();
		for (OrderDTO order : orders) {
			rows.add(toRow(order, canModify, today));
		}
		return rows;
	}
	
	/**
	 * 历史订单 → 页面行
	 *
	 * @param order     订单
	 * @param canModify 当前是否仍在可修改窗口内
	 * @param today     就餐日期（今天）
	 * @return 页面行
	 */
	private OrderHistoryPageData.OrderRow toRow(OrderDTO order, boolean canModify, LocalDate today) {
		OrderStatus status = OrderStatus.fromString(order.getStatus());
		OrderHistoryPageData.OrderRow row = new OrderHistoryPageData.OrderRow();
		row.setOrder(order);
		row.setCreatedAtText(formatTime(order.getCreatedTime()));
		// 与服务层保持一致：未支付可支付；未支付/已支付且未过截止时间、且订单是今天的才可取消
		row.setPayable(status == OrderStatus.UNPAID);
		row.setCancellable(status != null && status.isEditable() && canModify
				&& today.equals(toLocalDate(order.getCreatedTime())));
		row.setCancelled(status == OrderStatus.CANCELLED);
		return row;
	}
	
	/**
	 * 消费合计：已取消订单不计入
	 *
	 * @param orders 订单列表
	 * @return 合计金额
	 */
	private BigDecimal sumActiveAmount(List<OrderDTO> orders) {
		BigDecimal total = BigDecimal.ZERO;
		for (OrderDTO order : orders) {
			if (OrderStatus.CANCELLED.toString().equals(order.getStatus()) || order.getTotal() == null) {
				continue;
			}
			total = total.add(order.getTotal());
		}
		return total;
	}
	
	/**
	 * 毫秒时间戳 → 业务时区日期
	 *
	 * @param epochMilli 毫秒时间戳
	 * @return 日期
	 */
	private LocalDate toLocalDate(long epochMilli) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), zoneId).toLocalDate();
	}
	
	/**
	 * 毫秒时间戳 → 页面展示时间文本
	 *
	 * @param epochMilli 毫秒时间戳，0 表示无
	 * @return 时间文本，无值时返回空串
	 */
	private String formatTime(long epochMilli) {
		if (epochMilli <= 0L) {
			return "";
		}
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), zoneId).format(TIME_FORMATTER);
	}
	
	/**
	 * 业务时区下的“今天”
	 *
	 * @return 今天
	 */
	private LocalDate today() {
		return LocalDate.now(zoneId);
	}
}
