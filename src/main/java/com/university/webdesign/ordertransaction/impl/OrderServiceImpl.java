package com.university.webdesign.ordertransaction.impl;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuItemData;
import com.university.webdesign.menurecipe.api.MenuService;
import com.university.webdesign.ordertransaction.api.OrderCreateData;
import com.university.webdesign.ordertransaction.api.OrderDTO;
import com.university.webdesign.ordertransaction.api.OrderItemDTO;
import com.university.webdesign.ordertransaction.api.OrderQueryData;
import com.university.webdesign.ordertransaction.api.OrderUpdateData;
import com.university.webdesign.ordertransaction.api.PersonalConsumptionDTO;
import com.university.webdesign.ordertransaction.data.Order;
import com.university.webdesign.ordertransaction.data.OrderItem;
import com.university.webdesign.ordertransaction.data.OrderStatus;
import com.university.webdesign.ordertransaction.repository.OrderRepository;
import com.university.webdesign.ordertransaction.service.OrderService;
import com.university.webdesign.user.api.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 订单与交易核心服务实现
 * <p>
 * 已实现的业务规则：
 * <ol>
 *     <li><b>时间窗口</b>：下单/改单/取消都必须早于“订餐截止时间”（默认 09:00）；</li>
 *     <li><b>一人一天一单</b>：同一员工同一天只允许一张有效（非取消）订单；</li>
 *     <li><b>下单只允许当日已发布菜单内的菜品</b>，数量必须为正整数；</li>
 *     <li><b>数据快照</b>：明细保存下单时刻的菜名、分类、单价，菜品库后续变动不影响历史订单；</li>
 *     <li><b>删单留痕</b>：经理“删除”违规订单时置为已取消而非物理删除，保证财务审计可追溯。</li>
 * </ol>
 * <p>
 * 注意：本类统一使用 {@code @Transactional}（不加 {@code readOnly}）。
 * Hibernate 在只读事务下会把 flush 模式切到 MANUAL，同一事务内“先改后查”会读到旧数据，
 * 因此这里只用普通事务，正确性由方法语义与查询条件保证。
 */
@Slf4j
@Component
@Transactional
public class OrderServiceImpl implements OrderService
{
	/**
	 * 单条明细允许的最大数量
	 */
	private static final int MAX_QUANTITY = 999;
	
	/**
	 * 订单号每日流水位数
	 */
	private static final long DAILY_SEQUENCE_MODULUS = 100_000L;
	
	/**
	 * 餐厅经理角色编码
	 * <p>
	 * //todo 确认：需求只列了角色名称（餐厅经理、厨房主管、配餐员、财务、员工），没有给编码。
	 * 这里按用户与报表中心（王家豪）的 IAM 约定暂用 MANAGER / FINANCE，
	 * 需确认实际 roleCode 取值后再定稿。
	 */
	private static final String ROLE_MANAGER = "MANAGER";
	
	/**
	 * 财务角色编码，见 {@link #ROLE_MANAGER} 的说明
	 */
	private static final String ROLE_FINANCE = "FINANCE";
	
	/**
	 * 订餐截止时间，默认 09:00
	 * <p>
	 * //todo 确认：截止时间属于可配置项，需求文档给的是“默认 9:00”。
	 * 现在先用配置项 {@code order.cutoff-time} 落地（默认值 09:00），
	 * 待确认是否由用户与报表中心提供系统配置表统一管理（特别是需要按天/按部门覆盖时）。
	 */
	private final LocalTime cutoffTime;
	
	/**
	 * 业务时区，默认取系统时区
	 * <p>
	 * //todo 确认：时区目前取 {@code ZoneId.systemDefault()}，需与部署环境协商确定；
	 * 若数据库或服务器时区与业务时区不一致，跨日边界（00:00、09:00）的判断会出错。
	 */
	private final ZoneId zoneId;
	
	private final OrderRepository orderRepository;
	
	/**
	 * 菜品与菜单中心提供的服务
	 * <p>
	 * //todo 确认：按需求约定“假设对方 service 层已存在”。当前假定菜单模块对外提供：
	 * <ul>
	 *     <li>{@code MenuDTO getActiveMenu(LocalDate date)}：返回指定日期已发布的菜单（无则返回 null）；</li>
	 *     <li>{@code MenuDTO.menuItems}：菜单项列表，元素为 {@link MenuItemData}。</li>
	 * </ul>
	 * 需与菜品与菜单中心（方家乐）确认上述方法签名与菜单项字段后再定稿。
	 */
	private final MenuService menuService;
	
	/**
	 * 用户与报表中心提供的服务，用于角色与越权校验
	 * <p>
	 * //todo 确认：依赖 {@code UserService.hasRole(userId, roleCode)} 判断经理/财务角色。
	 * 该接口目前只有骨架、没有实现类，测试环境使用 StubServicesTestConfiguration 中的桩
	 * （桩的 hasRole 一律返回 false）。
	 * 若用户中心最终不提供角色查询能力，需要改为由 Spring Security 的认证上下文提供权限。
	 */
	private final UserService userService;
	
	public OrderServiceImpl(OrderRepository orderRepository,
			MenuService menuService,
			UserService userService,
			@Value("${order.cutoff-time:09:00}") String cutoffTime,
			@Value("${order.zone:}") String zone) {
		this.orderRepository = orderRepository;
		this.menuService = menuService;
		this.userService = userService;
		this.cutoffTime = LocalTime.parse(cutoffTime);
		this.zoneId = zone == null || zone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(zone);
	}
	
	// ------------------------------------------------------------------ 查询
	
	@Override
	public List<OrderDTO> getTodayOrders() {
		LocalDate today = LocalDate.now(zoneId);
		// 只取有效订单：已取消订单不占用当日产能，也不参与后厨汇总与配送
		List<Order> orders = orderRepository
				.findByOrderDateGreaterThanEqualAndOrderDateLessThanOrderByOrderDateAsc(
						startOfDay(today), startOfDay(today.plusDays(1)))
				.stream()
				.filter(order -> order.getStatus() != null && order.getStatus().isActive())
				.toList();
		return OrderDTO.fromAll(orders);
	}
	
	@Override
	public List<OrderDTO> query(OrderQueryData queryData) {
		if (queryData == null) {
			return getTodayOrders();
		}
		List<Order> orders = orderRepository.findAll(buildSpecification(queryData));
		return OrderDTO.fromAll(orders);
	}
	
	@Override
	public OrderDTO getOrder(Long orderId) {
		if (orderId == null) {
			return null;
		}
		return orderRepository.findById(orderId).map(OrderDTO::from).orElse(null);
	}
	
	@Override
	public List<OrderDTO> getHistoryOrders(Long userId, Long operatorId, LocalDate start, LocalDate end) {
		if (userId == null) {
			throw new IllegalArgumentException("员工ID不能为空");
		}
		// 只能查自己的历史；经理与财务可查他人（消费审计）
		assertSelfOrRole(operatorId, userId, "查看他人订单历史", ROLE_MANAGER, ROLE_FINANCE);
		return queryHistory(userId, start, end);
	}
	
	@Override
	public List<OrderDTO> getHistoryOrders(Long userId, LocalDate start, LocalDate end) {
		return getHistoryOrders(userId, null, start, end);
	}
	
	/**
	 * 个人历史查询的实际实现
	 *
	 * @param userId 员工ID
	 * @param start  起始日期（含），可为 null
	 * @param end    结束日期（含），可为 null
	 * @return 历史订单列表
	 */
	private List<OrderDTO> queryHistory(Long userId, LocalDate start, LocalDate end) {
		OrderQueryData queryData = new OrderQueryData();
		queryData.setUserId(userId);
		// 个人历史需要能看到自己取消过的订单
		queryData.setIncludeCancelled(Boolean.TRUE);
		if (start != null) {
			queryData.setStartTime(toEpochMilli(startOfDay(start)));
		}
		if (end != null) {
			queryData.setEndTime(toEpochMilli(startOfDay(end.plusDays(1))) - 1L);
		}
		return query(queryData);
	}
	
	@Override
	public PersonalConsumptionDTO getMonthlyConsumption(Long userId, Long operatorId, int year, int month) {
		if (userId == null) {
			throw new IllegalArgumentException("员工ID不能为空");
		}
		// 只能查自己的消费；经理与财务可查他人（财务审计）
		assertSelfOrRole(operatorId, userId, "查看他人消费明细", ROLE_MANAGER, ROLE_FINANCE);
		return buildMonthlyConsumption(userId, year, month);
	}
	
	@Override
	public PersonalConsumptionDTO getMonthlyConsumption(Long userId, int year, int month) {
		return getMonthlyConsumption(userId, null, year, month);
	}
	
	/**
	 * 月度消费统计的实际实现
	 *
	 * @param userId 员工ID
	 * @param year   年份
	 * @param month  月份（1-12）
	 * @return 月度消费统计
	 */
	private PersonalConsumptionDTO buildMonthlyConsumption(Long userId, int year, int month) {
		YearMonth yearMonth;
		try {
			yearMonth = YearMonth.of(year, month);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("月份不合法：" + year + "-" + month);
		}
		
		LocalDateTime start = startOfDay(yearMonth.atDay(1));
		LocalDateTime end = startOfDay(yearMonth.plusMonths(1).atDay(1));
		
		// 只统计非取消订单；按下单时间升序，便于打印月度账单
		List<Order> orders = orderRepository
				.findByUserIdAndOrderDateGreaterThanEqualAndOrderDateLessThanOrderByOrderDateAsc(userId, start, end)
				.stream()
				.filter(order -> order.getStatus() != null && order.getStatus().isActive())
				.toList();
		
		PersonalConsumptionDTO result = new PersonalConsumptionDTO();
		result.setUserId(userId);
		result.setMonth(yearMonth.toString());
		result.setOrderCount(orders.size());
		result.setOrders(OrderDTO.fromAll(orders));
		
		BigDecimal totalAmount = BigDecimal.ZERO;
		Map<Long, PersonalConsumptionDTO.ItemConsumptionDTO> summaries = new LinkedHashMap<>();
		for (Order order : orders) {
			if (order.getTotal() != null) {
				totalAmount = totalAmount.add(order.getTotal());
			}
			for (OrderItem item : order.getItems()) {
				PersonalConsumptionDTO.ItemConsumptionDTO summary = summaries.computeIfAbsent(item.getItemId(), key -> {
					PersonalConsumptionDTO.ItemConsumptionDTO created =
							new PersonalConsumptionDTO.ItemConsumptionDTO();
					created.setItemId(item.getItemId());
					created.setItemName(item.getItemName());
					return created;
				});
				summary.setQuantity(summary.getQuantity() + (item.getQuantity() == null ? 0 : item.getQuantity()));
				summary.setAmount(summary.getAmount().add(item.subtotal()));
			}
		}
		result.setTotalAmount(totalAmount);
		result.setItemSummaries(new ArrayList<>(summaries.values()));
		return result;
	}
	
	@Override
	public OrderDTO findActiveOrder(Long userId, LocalDate workDate) {
		if (userId == null || workDate == null) {
			return null;
		}
		return orderRepository
				.findByUserIdAndOrderDateGreaterThanEqualAndOrderDateLessThanOrderByOrderDateAsc(
						userId, startOfDay(workDate), startOfDay(workDate.plusDays(1)))
				.stream()
				.filter(order -> order.getStatus() != null && order.getStatus().isActive())
				.findFirst()
				.map(OrderDTO::from)
				.orElse(null);
	}
	
	@Override
	public boolean isBeforeCutoff(LocalDate workDate) {
		return checkTimeWindow(workDate) == null;
	}
	
	// ------------------------------------------------------------------ 下单 / 改单
	
	@Override
	public OrderDTO createOrder(OrderCreateData createData) {
		if (createData == null) {
			throw new IllegalArgumentException("下单请求不能为空");
		}
		Long userId = createData.getOperatorId();
		if (userId == null) {
			//todo 确认：登录态由用户与报表中心提供，当前由前端传 operatorId，需改为从认证上下文取
			throw new IllegalArgumentException("员工ID不能为空（operatorId 需由登录态提供）");
		}
		
		LocalDate today = LocalDate.now(zoneId);
		assertWithinTimeWindow(today);
		assertNoActiveOrderToday(userId, today);
		
		List<ItemRequest> requests = parseItemRequests(createData.getItemIds(), createData.getQuantities());
		if (requests.isEmpty()) {
			throw new IllegalArgumentException("请至少选择一道菜品");
		}
		
		Map<Long, MenuItemData> menuItemMap = resolveMenuItems(getTodayMenu(today));
		if (menuItemMap.isEmpty()) {
			throw new IllegalStateException("今日菜单为空或未发布，暂时无法下单");
		}
		
		Order order = new Order();
		order.setUserId(userId);
		order.setStatus(OrderStatus.UNPAID);
		order.setOrderDate(LocalDateTime.now(zoneId));
		order.setLastModifiedTime(order.getOrderDate());
		order.setOrderNumber(generateOrderNumber(today));
		
		appendItems(order, requests, menuItemMap);
		order.recalculateTotal();
		
		Order saved = orderRepository.save(order);
		log.info("员工 {} 完成下单，订单号 {}，金额 {}", userId, saved.getOrderNumber(), saved.getTotal());
		return OrderDTO.from(saved);
	}
	
	@Override
	public OrderDTO createOrder(Long userId) {
		// 兼容旧签名：菜品清单暂缺，交由上层后续调用改单接口补齐
		OrderCreateData createData = new OrderCreateData();
		createData.setOperatorId(userId);
		createData.setItemIds(List.of());
		createData.setQuantities(List.of());
		return createOrder(createData);
	}
	
	@Override
	public OrderDTO updateOrder(OrderUpdateData updateData) {
		if (updateData == null || updateData.getOrderId() == null) {
			throw new IllegalArgumentException("订单ID不能为空");
		}
		Order order = loadOrder(updateData.getOrderId());
		assertOperator(order, updateData.getOperatorId());
		
		LocalDate workDate = order.getOrderDate().toLocalDate();
		assertWithinTimeWindow(workDate);
		assertEditable(order);
		
		List<ItemRequest> requests = parseItemRequests(updateData.getItemIds(), updateData.getQuantities());
		if (requests.isEmpty()) {
			throw new IllegalArgumentException("请至少选择一道菜品");
		}
		
		Map<Long, MenuItemData> menuItemMap = resolveMenuItems(getTodayMenu(workDate));
		if (menuItemMap.isEmpty()) {
			throw new IllegalStateException("当日菜单为空或未发布，无法修改订单");
		}
		
		order.clearItems();
		appendItems(order, requests, menuItemMap);
		order.recalculateTotal();
		order.setLastModifiedTime(LocalDateTime.now(zoneId));
		
		Order saved = orderRepository.save(order);
		log.info("订单 {} 已修改，新金额 {}", saved.getOrderNumber(), saved.getTotal());
		return OrderDTO.from(saved);
	}
	
	@Override
	public OrderDTO updateOrder(OrderDTO orderDTO) {
		if (orderDTO == null || orderDTO.getOrderId() == null) {
			throw new IllegalArgumentException("订单ID不能为空");
		}
		OrderUpdateData updateData = new OrderUpdateData();
		updateData.setOrderId(orderDTO.getOrderId());
		updateData.setOperatorId(orderDTO.getUserId());
		List<Long> itemIds = new ArrayList<>();
		List<Integer> quantities = new ArrayList<>();
		if (orderDTO.getItems() != null && !orderDTO.getItems().isEmpty()) {
			for (OrderItemDTO item : orderDTO.getItems()) {
				itemIds.add(item.getItemId());
				quantities.add(item.getQuantity());
			}
		} else if (orderDTO.getItemIds() != null) {
			// 只传了菜品ID时按每样一份处理
			for (Long itemId : orderDTO.getItemIds()) {
				itemIds.add(itemId);
				quantities.add(1);
			}
		}
		updateData.setItemIds(itemIds);
		updateData.setQuantities(quantities);
		return updateOrder(updateData);
	}
	
	// ------------------------------------------------------------------ 支付 / 取消 / 删除
	
	@Override
	public OrderDTO payOrder(Long orderId, Long operatorId) {
		requireOperatorId(operatorId);
		Order order = loadOrder(orderId);
		assertOperator(order, operatorId);
		if (order.getStatus() != OrderStatus.UNPAID) {
			throw new IllegalStateException("当前状态（" + order.getStatus() + "）不可支付");
		}
		order.setStatus(OrderStatus.PAID);
		order.setLastModifiedTime(LocalDateTime.now(zoneId));
		Order saved = orderRepository.saveAndFlush(order);
		log.info("订单 {} 已支付", saved.getOrderNumber());
		return OrderDTO.from(saved);
	}
	
	@Override
	public OrderDTO cancelOrder(Long orderId, Long operatorId) {
		requireOperatorId(operatorId);
		Order order = loadOrder(orderId);
		assertOperator(order, operatorId);
		// 取消同样受截止时间限制：截止后厨房已开始备料，员工不能自行撤销
		assertWithinTimeWindow(order.getOrderDate().toLocalDate());
		assertEditable(order);
		return markCancelled(order, "员工取消");
	}
	
	@Override
	public OrderDTO deleteOrder(Long orderId, Long operatorId) {
		Order order = loadOrder(orderId);
		// 经理删单：不受截止时间限制，但以“已取消”留痕，保证历史与财务数据可追溯
		//todo 确认：删除违规订单是否要求同时记录删除人/删除原因（审计留痕字段），以及是否需要“硬删除”语义
		assertCanManage(operatorId, "删除订单");
		if (order.getStatus() == OrderStatus.CANCELLED) {
			throw new IllegalStateException("该订单已经是已取消状态");
		}
		log.info("操作人 {} 删除订单 {}（当前状态 {}）", operatorId, order.getOrderNumber(), order.getStatus());
		return markCancelled(order, "经理删除");
	}
	
	@Override
	public OrderDTO deleteOrder(Long orderId) {
		return deleteOrder(orderId, null);
	}
	
	// ------------------------------------------------------------------ 内部方法：时间窗口与一人一天一单
	
	/**
	 * 校验时间窗口
	 *
	 * @param workDate 就餐日期
	 * @return 违规原因；允许操作时返回 null
	 */
	private String checkTimeWindow(LocalDate workDate) {
		if (workDate == null) {
			return "就餐日期不能为空";
		}
		LocalDate today = LocalDate.now(zoneId);
		if (workDate.isBefore(today)) {
			return "不能操作已过去日期的订单";
		}
		if (workDate.isAfter(today)) {
			//todo 确认：需求只明确了“当日点餐”，未定义是否支持提前预订未来日期的餐次
			return "暂不支持提前预订未来日期的餐次";
		}
		if (!LocalTime.now(zoneId).isBefore(cutoffTime)) {
			return "已过订餐截止时间（" + cutoffTime + "），无法下单或修改订单";
		}
		return null;
	}
	
	/**
	 * 断言当前处于可操作的时间窗口内
	 *
	 * @param workDate 就餐日期
	 */
	private void assertWithinTimeWindow(LocalDate workDate) {
		String reason = checkTimeWindow(workDate);
		if (reason != null) {
			throw new IllegalStateException(reason);
		}
	}
	
	/**
	 * 断言该员工当日还没有有效订单（“一人一天一单”）
	 * <p>
	 * //todo 确认：并发下两个请求可能同时通过该检查而各插一张订单。
	 * 稳妥做法是在 {@code my_order} 上对 (user_id, 就餐日期) 加唯一约束（取消订单不占用名额，
	 * 需要配合部分索引或另建占用表），此项需与数据库设计确认后再落地。
	 *
	 * @param userId   员工ID
	 * @param workDate 就餐日期
	 */
	private void assertNoActiveOrderToday(Long userId, LocalDate workDate) {
		for (OrderStatus status : OrderStatus.values()) {
			if (!status.isActive()) {
				continue;
			}
			List<Order> exists = orderRepository.findByUserIdAndStatusAndOrderDateGreaterThanEqualAndOrderDateLessThan(
					userId, status, startOfDay(workDate), startOfDay(workDate.plusDays(1)));
			if (!exists.isEmpty()) {
				throw new IllegalStateException("您今天已经下过单了，每人每天只能点一份餐（可在截止前修改原订单）");
			}
		}
	}
	
	/**
	 * 断言订单处于可修改/可取消状态
	 *
	 * @param order 订单
	 */
	private void assertEditable(Order order) {
		OrderStatus status = order.getStatus();
		if (status == null || !status.isEditable()) {
			throw new IllegalStateException("订单当前状态（" + status + "）不可修改或取消");
		}
	}
	
	/**
	 * 断言订单操作人身份（归属校验）
	 * <p>
	 * 员工对自有订单的改单/支付/取消走这里：只能操作自己的订单。
	 * 需要“经理可以操作他人订单”的场景请走 {@link #assertRole}。
	 *
	 * @param order      订单
	 * @param operatorId 操作用户ID；为 null 时跳过校验（兼容未接入登录态的调用）
	 */
	private void assertOperator(Order order, Long operatorId) {
		if (operatorId == null) {
			return;
		}
		if (!Objects.equals(order.getUserId(), operatorId)) {
			throw new IllegalStateException("只能操作自己的订单");
		}
	}
	
	/**
	 * 要求必须提供操作人身份
	 * <p>
	 * 支付、取消是员工对自有订单的操作，缺少操作人时不能放行（否则等于任何人都能操作任意订单）。
	 *
	 * @param operatorId 操作用户ID
	 */
	private void requireOperatorId(Long operatorId) {
		if (operatorId == null) {
			//todo 确认：接入登录态后这里的身份应直接来自认证上下文，不再作为请求参数接收
			throw new IllegalArgumentException("缺少操作用户ID（operatorId 需由登录态提供）");
		}
	}
	
	/**
	 * 断言操作人拥有“管理类”角色（经理删单、财务审计等场景）
	 * <p>
	 * 与 {@link #assertOperator(Order, Long)} 不同，这里不放过缺失操作人的情况：
	 * 管理动作一旦没有可信身份，就不能执行。
	 *
	 * @param operatorId 操作用户ID
	 * @param action     动作名称，用于报错信息
	 * @param roleCodes  允许的角色编码，满足其一即可
	 */
	private void assertRole(Long operatorId, String action, String... roleCodes) {
		requireOperatorId(operatorId);
		if (userService.hasAnyRole(operatorId, roleCodes)) {
			return;
		}
		log.warn("用户 {} 尝试执行“{}”，但缺少所需角色 {}", operatorId, action, String.join("/", roleCodes));
		throw new IllegalStateException("无权执行该操作：" + action);
	}
	
	/**
	 * 断言操作人可以执行管理类操作，缺失操作人时只告警不拦截
	 * <p>
	 * 用于“兼容旧签名”的入口：老调用方不传操作人，此时退化为不校验并留告警，
	 * 以免破坏现有流程；新代码请使用带 operatorId 的重载。
	 *
	 * @param operatorId 操作用户ID，可为 null
	 * @param action     动作名称
	 */
	private void assertCanManage(Long operatorId, String action) {
		if (operatorId == null) {
			//todo 确认：这些兼容重载在接入登录态后应当删除，改为强制校验操作人
			log.warn("执行“{}”时未提供操作人，已跳过角色校验：请改用带 operatorId 的接口", action);
			return;
		}
		assertRole(operatorId, action, ROLE_MANAGER);
	}
	
	/**
	 * 断言操作人要么是数据本人，要么拥有指定角色
	 * <p>
	 * 用于个人历史、个人消费这类“本人可查、经理/财务也可查”的场景。
	 * 操作人为 null 时视为可信的服务端内部调用（如履约模块聚合当日订单）。
	 *
	 * @param operatorId 操作用户ID，可为 null
	 * @param targetId   数据归属的用户ID
	 * @param action     动作名称，用于报错信息
	 * @param roleCodes  允许“查他人”的角色编码
	 */
	private void assertSelfOrRole(Long operatorId, Long targetId, String action, String... roleCodes) {
		if (operatorId == null) {
			return;
		}
		if (Objects.equals(operatorId, targetId)) {
			return;
		}
		assertRole(operatorId, action, roleCodes);
	}
	
	// ------------------------------------------------------------------ 内部方法：菜品与快照
	
	/**
	 * 取指定日期的已发布菜单
	 *
	 * @param workDate 就餐日期
	 * @return 菜单；不存在时返回 null
	 */
	private MenuDTO getTodayMenu(LocalDate workDate) {
		//todo 确认：方法名与返回值语义需与菜品与菜单中心对齐（见 menuService 字段注释）
		return menuService.getActiveMenu(workDate);
	}
	
	/**
	 * 校验下单参数并整理为“菜品 + 数量”的结构
	 *
	 * @param itemIds    菜品ID列表
	 * @param quantities 数量列表，与菜品ID一一对应
	 * @return 请求项列表
	 */
	private List<ItemRequest> parseItemRequests(List<Long> itemIds, List<Integer> quantities) {
		List<ItemRequest> requests = new ArrayList<>();
		if (itemIds == null || itemIds.isEmpty()) {
			return requests;
		}
		if (quantities == null || quantities.size() != itemIds.size()) {
			throw new IllegalArgumentException("菜品数量与菜品ID不匹配，请重新选择菜品");
		}
		for (int i = 0; i < itemIds.size(); i++) {
			Long itemId = itemIds.get(i);
			Integer quantity = quantities.get(i);
			if (itemId == null) {
				throw new IllegalArgumentException("菜品ID不能为空");
			}
			if (quantity == null || quantity <= 0) {
				throw new IllegalArgumentException("菜品数量必须为正整数");
			}
			if (quantity > MAX_QUANTITY) {
				throw new IllegalArgumentException("单个菜品数量不能超过 " + MAX_QUANTITY);
			}
			requests.add(new ItemRequest(itemId, quantity));
		}
		return requests;
	}
	
	/**
	 * 把菜单项整理成“菜品ID → 菜单项”的映射
	 *
	 * @param menu 菜单
	 * @return 菜品ID到菜单项的映射
	 */
	private Map<Long, MenuItemData> resolveMenuItems(MenuDTO menu) {
		Map<Long, MenuItemData> map = new LinkedHashMap<>();
		if (menu == null || menu.getMenuItems() == null) {
			return map;
		}
		for (MenuItemData item : menu.getMenuItems()) {
			if (item != null && item.itemId() != null) {
				map.put(item.itemId(), item);
			}
		}
		return map;
	}
	
	/**
	 * 按请求写入订单明细（同一菜品合并数量）
	 * <p>
	 * 先整体校验再写入：改单时若中途发现某个菜品不在菜单内，已清空的明细不会被写坏。
	 *
	 * @param order       订单
	 * @param requests    请求项
	 * @param menuItemMap 当日菜单项映射
	 */
	private void appendItems(Order order, List<ItemRequest> requests, Map<Long, MenuItemData> menuItemMap) {
		Map<Long, Integer> merged = new LinkedHashMap<>();
		for (ItemRequest request : requests) {
			merged.merge(request.itemId(), request.quantity(), Integer::sum);
		}
		List<OrderItem> pending = new ArrayList<>();
		for (Map.Entry<Long, Integer> entry : merged.entrySet()) {
			Long itemId = entry.getKey();
			MenuItemData menuItem = menuItemMap.get(itemId);
			if (menuItem == null) {
				throw new IllegalArgumentException("菜品（ID=" + itemId + "）不在今日菜单中，无法下单");
			}
			if (menuItem.price() == null || menuItem.price().signum() < 0) {
				throw new IllegalStateException("菜品（ID=" + itemId + "）未正确配置价格，无法下单");
			}
			OrderItem item = new OrderItem();
			item.setItemId(itemId);
			// 快照：保存下单时刻的菜名、分类与单价
			item.setItemName(menuItem.itemName() == null ? "菜品" + itemId : menuItem.itemName());
			item.setCategory(menuItem.category());
			item.setUnitPrice(menuItem.price());
			item.setQuantity(entry.getValue());
			pending.add(item);
		}
		for (OrderItem item : pending) {
			order.addItem(item);
		}
	}
	
	/**
	 * 生成订单号：yyMMdd + 当日 5 位流水
	 * <p>
	 * //todo 确认：该规则为自行拟定（需求只要求“订单号”），且并发下流水号可能重复，
	 * 需要确认是否改用数据库序列、Redis 发号器或 UUID 方案。
	 *
	 * @param workDate 就餐日期
	 * @return 订单号
	 */
	private Long generateOrderNumber(LocalDate workDate) {
		long dailyCount = orderRepository.countByOrderDateGreaterThanEqualAndOrderDateLessThan(
				startOfDay(workDate), startOfDay(workDate.plusDays(1)));
		LocalDate today = LocalDate.now(zoneId);
		long datePrefix = (today.getYear() % 100) * 10_000L + today.getMonthValue() * 100L + today.getDayOfMonth();
		long sequence = (dailyCount + 1) % DAILY_SEQUENCE_MODULUS;
		return datePrefix * DAILY_SEQUENCE_MODULUS + sequence;
	}
	
	/**
	 * 置订单为已取消
	 *
	 * @param order  订单
	 * @param reason 操作来源，用于日志
	 * @return 更新后的订单
	 */
	private OrderDTO markCancelled(Order order, String reason) {
		order.setStatus(OrderStatus.CANCELLED);
		order.setLastModifiedTime(LocalDateTime.now(zoneId));
		// 立即写库：取消会影响当日产能、后厨汇总与个人统计，不能让状态悬在事务里等下次查询
		Order saved = orderRepository.saveAndFlush(order);
		log.info("订单 {} 已置为已取消（{}）", saved.getOrderNumber(), reason);
		return OrderDTO.from(saved);
	}
	
	// ------------------------------------------------------------------ 内部方法：查询与时间
	
	/**
	 * 组装动态查询条件
	 *
	 * @param queryData 查询条件
	 * @return JPA Specification
	 */
	private Specification<Order> buildSpecification(OrderQueryData queryData) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (queryData.getOrderId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("id"), queryData.getOrderId()));
			}
			if (queryData.getOrderNumber() != null) {
				predicates.add(criteriaBuilder.equal(root.get("orderNumber"), queryData.getOrderNumber()));
			}
			if (queryData.getUserId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("userId"), queryData.getUserId()));
			}
			if (queryData.getOrderStatus() != null && !queryData.getOrderStatus().isBlank()) {
				OrderStatus status = OrderStatus.fromString(queryData.getOrderStatus());
				if (status == null) {
					throw new IllegalArgumentException("未知的订单状态：" + queryData.getOrderStatus());
				}
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			} else if (!Boolean.TRUE.equals(queryData.getIncludeCancelled())) {
				// 默认只看有效订单：已取消订单不计入当日产能与统计
				predicates.add(criteriaBuilder.notEqual(root.get("status"), OrderStatus.CANCELLED));
			}
			if (queryData.getStartTime() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("orderDate"),
						fromEpochMilli(queryData.getStartTime())));
			}
			if (queryData.getEndTime() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("orderDate"),
						fromEpochMilli(queryData.getEndTime())));
			}
			criteriaQuery.orderBy(criteriaBuilder.asc(root.get("orderDate")));
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
	
	/**
	 * 查询订单，不存在时抛异常
	 *
	 * @param orderId 订单ID
	 * @return 订单实体
	 */
	private Order loadOrder(Long orderId) {
		if (orderId == null) {
			throw new IllegalArgumentException("订单ID不能为空");
		}
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("订单不存在：" + orderId));
	}
	
	/**
	 * 当天 00:00
	 *
	 * @param date 日期
	 * @return 当天起点时间
	 */
	private LocalDateTime startOfDay(LocalDate date) {
		return date.atStartOfDay();
	}
	
	/**
	 * 毫秒时间戳 → 业务时区时间
	 *
	 * @param epochMilli 毫秒时间戳
	 * @return 本地时间
	 */
	private LocalDateTime fromEpochMilli(long epochMilli) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), zoneId);
	}
	
	/**
	 * 业务时区时间 → 毫秒时间戳
	 *
	 * @param time 本地时间
	 * @return 毫秒时间戳
	 */
	private long toEpochMilli(LocalDateTime time) {
		return time.atZone(zoneId).toInstant().toEpochMilli();
	}
	
	/**
	 * 下单请求项
	 *
	 * @param itemId   菜品ID
	 * @param quantity 数量
	 */
	private record ItemRequest(Long itemId, Integer quantity)
	{
	}
}
