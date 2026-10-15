package com.university.webdesign.impl.order;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.domain.order.OrderDetail;
import com.university.webdesign.domain.order.OrderForm;
import com.university.webdesign.domain.order.OrderStatus;
import com.university.webdesign.event.OrderCreatedEvent;
import com.university.webdesign.event.OrderUpdatedEvent;
import com.university.webdesign.repository.order.OrderFormRepository;
import com.university.webdesign.service.menu.MenuService;
import com.university.webdesign.service.menu.dto.MenuItemVO;
import com.university.webdesign.service.menu.dto.MenuVO;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.OrderService;
import com.university.webdesign.service.order.dto.OrderModifyCmd;
import com.university.webdesign.service.order.dto.OrderQuery;
import com.university.webdesign.service.order.dto.OrderHistoryQuery;
import com.university.webdesign.service.order.dto.OrderSubmitCmd;
import com.university.webdesign.service.order.dto.OrderVO;
import com.university.webdesign.service.order.dto.ServiceWindowVO;
import com.university.webdesign.service.user.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 订单与交易核心服务实现。
 * <p>
 * 对应《对外方法表》3.2 的 `OrderService`，落地以下业务规则：
 * <ol>
 *     <li><b>时间窗口</b>：截止时间唯一来源是 {@link ServiceWindowService#get}，超时抛 42201；</li>
 *     <li><b>一人一天一单</b>：同一员工同一天只允许一张有效（{@code PENDING}/{@code VALID}）订单，冲突抛 42202；</li>
 *     <li><b>下单只允许当日已发布菜单内的菜品</b>，价格取菜单快照（菜单级调价优先）；</li>
 *     <li><b>数据快照</b>：明细保存下单时刻的菜名、分类、单位、单价，菜谱后续变动不影响历史订单；</li>
 *     <li><b>幂等</b>：{@code Idempotency-Key} 命中既有订单时直接返回该订单，不重复下单；</li>
 *     <li><b>删单留痕</b>：经理作废违规订单置为 {@code INVALID} 并写审计字段，不物理删除。</li>
 * </ol>
 * <p>
 * 事务说明：本类统一使用普通 {@code @Transactional}，**不加 {@code readOnly = true}**。
 * Hibernate 在只读事务下会把 flush 模式切到 MANUAL，同一事务内“先改后查”会读到旧数据。
 */
@Slf4j
@Service
@Transactional
public class OrderServiceImpl implements OrderService
{
	/**
	 * 订单号日期前缀格式
	 */
	private static final DateTimeFormatter ORDER_NO_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd");

	/**
	 * 时间文案格式
	 */
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final OrderFormRepository orderFormRepository;
	private final OrderQueryService orderQueryService;
	private final MenuService menuService;
	private final ServiceWindowService serviceWindowService;
	private final AuthService authService;
	private final OrderViewAssembler assembler;
	private final ApplicationEventPublisher eventPublisher;

	public OrderServiceImpl(
			OrderFormRepository orderFormRepository,
			OrderQueryService orderQueryService,
			MenuService menuService,
			ServiceWindowService serviceWindowService,
			AuthService authService,
			OrderViewAssembler assembler,
			ApplicationEventPublisher eventPublisher) {
		this.orderFormRepository = orderFormRepository;
		this.orderQueryService = orderQueryService;
		this.menuService = menuService;
		this.serviceWindowService = serviceWindowService;
		this.authService = authService;
		this.assembler = assembler;
		this.eventPublisher = eventPublisher;
	}

	// ------------------------------------------------------------------ 时间窗口

	@Override
	public ServiceWindowVO getWindow(LocalDate date) {
		LocalDate target = date == null ? LocalDate.now() : date;
		Long deptId = UserContextHolder.get() == null ? null : UserContextHolder.get().deptId();
		// 时间窗口的唯一数据来源是 M3 的 ServiceWindowService；这里映射成订单模块对外的读模型，
		// 避免把 operation 模块的 DTO 直接暴露给前端（api 层只依赖自己模块的 service 契约）。
		com.university.webdesign.service.operation.dto.ServiceWindowVO source =
				serviceWindowService.get(target, null, deptId);
		ServiceWindowVO window = new ServiceWindowVO();
		window.setDate(target);
		window.setCutoffTime(source.getCutoffTime());
		window.setDeliveryStartTime(source.getDeliveryStartTime());
		window.setCanOrder(source.isCanOrder());
		window.setCanDeliver(!LocalTime.now().isBefore(source.getDeliveryStartTime() == null
				? LocalTime.of(11, 30) : source.getDeliveryStartTime()));
		window.setServerTime(DATE_TIME.format(LocalDateTime.now()));
		return window;
	}

	// ------------------------------------------------------------------ 下单

	@Override
	public OrderVO submit(OrderSubmitCmd cmd) {
		if (cmd == null) {
			throw BusinessException.paramInvalid("下单请求不能为空");
		}
		if (cmd.getIdempotencyKey() == null || cmd.getIdempotencyKey().isBlank()) {
			throw BusinessException.paramInvalid("缺少幂等键：请通过 Idempotency-Key 请求头提交");
		}
		if (cmd.getItems() == null || cmd.getItems().isEmpty()) {
			throw BusinessException.paramInvalid("请至少选择一道菜品");
		}
		UserContext context = UserContextHolder.require();
		if (!authService.checkPermission(context.userId(), PermissionEnum.ORDER_SUBMIT)) {
			throw BusinessException.forbidden("当前账号没有点餐权限");
		}

		// 幂等：命中既有订单直接返回，不重复下单
		OrderForm existed = orderFormRepository.findByIdempotencyKey(cmd.getIdempotencyKey().trim()).orElse(null);
		if (existed != null) {
			log.info("幂等键 {} 命中既有订单 {}", cmd.getIdempotencyKey(), existed.getOrderNo());
			return assembler.toVO(existed);
		}

		LocalDate today = LocalDate.now();
		assertWithinWindow(today, context.deptId());
		assertNoActiveOrderToday(context.userId(), today);

		MenuVO menu = requirePublishedMenu(today);
		if (cmd.getMenuId() != null && !Objects.equals(cmd.getMenuId(), menu.getMenuId())) {
			throw BusinessException.stateConflict("菜单已更新，请刷新页面后重新选择菜品");
		}
		Map<Long, MenuItemVO> menuItems = indexMenuItems(menu);

		OrderForm order = new OrderForm();
		order.setOrderNo(nextOrderNo(today));
		order.setEmployeeId(context.userId());
		order.setDeptId(context.deptId());
		order.setOrderDate(today);
		order.setStatus(OrderStatus.PENDING);
		order.setRemark(cmd.getRemark());
		order.setIdempotencyKey(cmd.getIdempotencyKey().trim());
		appendDetails(order, cmd.getItems(), menuItems);
		order.recalculateTotal();

		OrderForm saved = orderFormRepository.save(order);
		publishCreated(saved);
		log.info("员工 {} 下单成功，订单号 {}，金额 {}", context.userId(), saved.getOrderNo(), saved.getTotalAmount());
		return assembler.toVO(saved);
	}

	// ------------------------------------------------------------------ 改单 / 取消 / 作废

	@Override
	public void modify(Long orderId, OrderModifyCmd cmd) {
		UserContext context = UserContextHolder.require();
		OrderForm order = requireOrder(orderId);
		assertOwner(order, context);
		assertWithinWindow(order.getOrderDate(), context.deptId());
		assertEditable(order, "修改");
		if (cmd == null || cmd.getItems() == null || cmd.getItems().isEmpty()) {
			throw BusinessException.paramInvalid("请至少选择一道菜品");
		}
		if (cmd.getVersion() != null && !Objects.equals(cmd.getVersion(), order.getVersion())) {
			throw BusinessException.stateConflict("订单已被其他操作修改，请刷新后重试");
		}

		MenuVO menu = requirePublishedMenu(order.getOrderDate());
		Map<Long, MenuItemVO> menuItems = indexMenuItems(menu);
		List<OrderSubmitCmd.SubmitItem> items = new ArrayList<>(cmd.getItems());
		if (items.stream().anyMatch(item -> item.getRecipeId() == null)) {
			throw BusinessException.paramInvalid("菜品ID不能为空");
		}

		String previousStatus = order.getStatus().name();
		order.clearDetails();
		appendDetails(order, items, menuItems);
		order.recalculateTotal();
		if (cmd.getRemark() != null) {
			order.setRemark(cmd.getRemark());
		}
		OrderForm saved = orderFormRepository.saveAndFlush(order);
		eventPublisher.publishEvent(new OrderUpdatedEvent(saved.getOrderId(), saved.getEmployeeId(),
				saved.getOrderDate(), OrderUpdatedEvent.MODIFIED, previousStatus, saved.getStatus().name()));
		log.info("订单 {} 已修改，新金额 {}", saved.getOrderNo(), saved.getTotalAmount());
	}

	@Override
	public void cancel(Long orderId, String reason) {
		UserContext context = UserContextHolder.require();
		OrderForm order = requireOrder(orderId);
		assertOwner(order, context);
		// 取消同样受截止时间限制：截止后厨房已开始备料，员工不能自行撤销
		assertWithinWindow(order.getOrderDate(), context.deptId());
		assertEditable(order, "取消");
		String previousStatus = order.getStatus().name();
		order.markCancelled(reason == null || reason.isBlank() ? "员工取消" : reason);
		OrderForm saved = orderFormRepository.saveAndFlush(order);
		eventPublisher.publishEvent(new OrderUpdatedEvent(saved.getOrderId(), saved.getEmployeeId(),
				saved.getOrderDate(), OrderUpdatedEvent.CANCELLED, previousStatus, saved.getStatus().name()));
		log.info("订单 {} 已取消", saved.getOrderNo());
	}

	@Override
	public String invalidate(Long orderId, String reason) {
		if (reason == null || reason.isBlank()) {
			throw BusinessException.paramInvalid("作废原因不能为空");
		}
		UserContext context = UserContextHolder.require();
		if (!authService.checkPermission(context.userId(), PermissionEnum.ORDER_INVALIDATE)) {
			throw BusinessException.forbidden("只有餐厅经理可以作废违规订单");
		}
		OrderForm order = requireOrder(orderId);
		if (order.getStatus() == OrderStatus.INVALID) {
			throw BusinessException.stateConflict("该订单已经是已作废状态");
		}
		String previousStatus = order.getStatus().name();
		// 经理作废不受截止时间限制，但必须留痕，保证财务审计可追溯
		order.markInvalid(context.userId(), reason);
		OrderForm saved = orderFormRepository.saveAndFlush(order);
		eventPublisher.publishEvent(new OrderUpdatedEvent(saved.getOrderId(), saved.getEmployeeId(),
				saved.getOrderDate(), OrderUpdatedEvent.INVALIDATED, previousStatus, saved.getStatus().name()));
		String auditId = "AUDIT-" + saved.getOrderId() + "-" + context.userId();
		log.info("经理 {} 作废订单 {}（原因：{}，审计号 {}）",
				context.userId(), saved.getOrderNo(), reason, auditId);
		return auditId;
	}

	@Override
	public int countDailyOrders(Long employeeId, LocalDate date) {
		if (employeeId == null || date == null) {
			return 0;
		}
		return (int) orderFormRepository.countByEmployeeIdAndOrderDateAndStatusIn(
				employeeId, date, List.of(OrderStatus.PENDING, OrderStatus.VALID));
	}

	// ------------------------------------------------------------------ 查询

	@Override
	public PageResult<OrderVO> page(OrderQuery query) {
		return orderQueryService.page(query);
	}

	@Override
	public PageResult<OrderVO> pageHistory(OrderHistoryQuery query) {
		UserContext context = UserContextHolder.require();
		OrderHistoryQuery effective = query == null ? new OrderHistoryQuery() : query;
		return orderQueryService.pageHistory(context.userId(),
				new com.university.webdesign.common.DateRange(effective.getDateFrom(), effective.getDateTo()),
				effective);
	}

	// ------------------------------------------------------------------ 内部方法

	/**
	 * 校验订单操作人是否为订单本人
	 *
	 * @param order   订单
	 * @param context 当前登录用户
	 */
	private void assertOwner(OrderForm order, UserContext context) {
		if (!Objects.equals(order.getEmployeeId(), context.userId())) {
			throw BusinessException.forbidden("只能操作自己的订单");
		}
	}

	/**
	 * 校验订单处于可改可取消状态
	 *
	 * @param order  订单
	 * @param action 动作名称
	 */
	private void assertEditable(OrderForm order, String action) {
		if (order.getStatus() == null || !order.getStatus().isEditable()) {
			throw BusinessException.stateConflict(
					"订单当前状态（" + (order.getStatus() == null ? "未知" : order.getStatus().getText())
							+ "）不可" + action);
		}
	}

	/**
	 * 校验当前时间仍在订餐截止时间之前
	 *
	 * @param date   就餐日期
	 * @param deptId 部门ID
	 */
	private void assertWithinWindow(LocalDate date, Long deptId) {
		var window = serviceWindowService.get(date, null, deptId);
		if (!window.isCanOrder()) {
			throw BusinessException.outOfWindow("已过订餐截止时间（"
					+ (window.getCutoffTime() == null ? LocalTime.of(9, 0) : window.getCutoffTime())
					+ "），无法下单或修改订单");
		}
	}

	/**
	 * 断言该员工当日还没有有效订单（一人一天一单）
	 *
	 * @param employeeId 员工ID
	 * @param date       就餐日期
	 */
	private void assertNoActiveOrderToday(Long employeeId, LocalDate date) {
		long count = orderFormRepository.countByEmployeeIdAndOrderDateAndStatusIn(
				employeeId, date, List.of(OrderStatus.PENDING, OrderStatus.VALID));
		if (count > 0) {
			throw BusinessException.ruleViolation("您今天已经下过单了，每人每天只能点一份餐（可在截止前修改原订单）");
		}
	}

	/**
	 * 取当日已发布菜单，未发布时抛 40902
	 *
	 * @param date 就餐日期
	 * @return 菜单视图（含快照明细）
	 */
	private MenuVO requirePublishedMenu(LocalDate date) {
		MenuVO menu = menuService.getCurrent(date);
		if (menu == null) {
			throw BusinessException.stateConflict("今日菜单尚未发布，暂不能下单");
		}
		return menu;
	}

	/**
	 * 建立“菜品ID → 菜单项”索引
	 *
	 * @param menu 菜单视图
	 * @return 索引
	 */
	private Map<Long, MenuItemVO> indexMenuItems(MenuVO menu) {
		Map<Long, MenuItemVO> map = new LinkedHashMap<>();
		if (menu.getItems() == null) {
			return map;
		}
		for (MenuItemVO item : menu.getItems()) {
			Long key = item.getRecipeId() != null ? item.getRecipeId() : item.getItemId();
			if (key != null) {
				map.put(key, item);
			}
		}
		return map;
	}

	/**
	 * 按请求写入明细快照（同一菜品合并数量，先整体校验再写入）
	 *
	 * @param order     订单
	 * @param items     请求明细
	 * @param menuItems 当日菜单项索引
	 */
	private void appendDetails(OrderForm order, List<OrderSubmitCmd.SubmitItem> items, Map<Long, MenuItemVO> menuItems) {
		Map<Long, Integer> merged = new LinkedHashMap<>();
		for (OrderSubmitCmd.SubmitItem item : items) {
			if (item.getRecipeId() == null) {
				throw BusinessException.paramInvalid("菜品ID不能为空");
			}
			Integer quantity = item.getQuantity();
			if (quantity == null || quantity <= 0) {
				throw BusinessException.ruleViolation("菜品数量必须为正整数");
			}
			if (quantity > 999) {
				throw BusinessException.ruleViolation("单个菜品数量不能超过 999");
			}
			merged.merge(item.getRecipeId(), quantity, Integer::sum);
		}
		for (Map.Entry<Long, Integer> entry : merged.entrySet()) {
			Long recipeId = entry.getKey();
			MenuItemVO menuItem = menuItems.get(recipeId);
			if (menuItem == null) {
				throw BusinessException.paramInvalid("菜品（ID=" + recipeId + "）不在今日菜单中，无法下单");
			}
			BigDecimal price = menuItem.getMenuPrice() != null ? menuItem.getMenuPrice() : menuItem.getUnitPrice();
			if (price == null || price.signum() < 0) {
				throw BusinessException.stateConflict("菜品（ID=" + recipeId + "）未正确配置价格，无法下单");
			}
			OrderDetail detail = new OrderDetail();
			detail.setRecipeId(recipeId);
			// 快照：保存下单时刻的菜名、分类、单位与单价
			detail.setRecipeName(menuItem.getRecipeName() == null ? "菜品" + recipeId : menuItem.getRecipeName());
			detail.setCategory(menuItem.getCategory());
			detail.setUnit(menuItem.getUnit());
			detail.setUnitPrice(price);
			detail.setQuantity(entry.getValue());
			detail.setAmount(price.multiply(BigDecimal.valueOf(entry.getValue())));
			order.addDetail(detail);
		}
	}

	/**
	 * 生成订单号：{@code yyyyMMdd} + 4 位当日流水
	 *
	 * @param date 就餐日期
	 * @return 订单号
	 */
	private String nextOrderNo(LocalDate date) {
		String prefix = ORDER_NO_PREFIX.format(date);
		String max = orderFormRepository.findMaxOrderNoByPrefix(prefix);
		long sequence = 1L;
		if (max != null && max.length() > prefix.length()) {
			try {
				sequence = Long.parseLong(max.substring(prefix.length())) + 1L;
			} catch (NumberFormatException ignored) {
				sequence = orderFormRepository.countByOrderDate(date) + 1L;
			}
		}
		return prefix + String.format("%04d", sequence % 10_000L);
	}

	/**
	 * 查询订单，不存在抛 40400
	 *
	 * @param orderId 订单ID
	 * @return 订单实体
	 */
	private OrderForm requireOrder(Long orderId) {
		if (orderId == null) {
			throw BusinessException.paramInvalid("订单ID不能为空");
		}
		return orderFormRepository.findById(orderId)
				.orElseThrow(() -> BusinessException.notFound("订单不存在：" + orderId));
	}

	/**
	 * 发布订单创建事件
	 *
	 * @param order 订单实体
	 */
	private void publishCreated(OrderForm order) {
		List<OrderCreatedEvent.Item> items = new ArrayList<>();
		for (OrderDetail detail : order.getDetails()) {
			items.add(new OrderCreatedEvent.Item(detail.getRecipeId(), detail.getRecipeName(),
					detail.getCategory(), detail.getQuantity(), detail.subtotal()));
		}
		eventPublisher.publishEvent(new OrderCreatedEvent(order.getOrderId(), order.getEmployeeId(),
				order.getOrderDate(), items, order.getTotalAmount()));
	}
}
