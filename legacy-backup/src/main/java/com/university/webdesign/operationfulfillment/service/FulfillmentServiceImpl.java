package com.university.webdesign.operationfulfillment.service;

import com.university.webdesign.operationfulfillment.api.BlanketOrderDTO;
import com.university.webdesign.operationfulfillment.api.DeliveryItemDTO;
import com.university.webdesign.operationfulfillment.api.DeliveryTaskDTO;
import com.university.webdesign.operationfulfillment.api.DeliveryTaskQueryData;
import com.university.webdesign.operationfulfillment.api.ProductionItemDTO;
import com.university.webdesign.ordertransaction.api.OrderDTO;
import com.university.webdesign.ordertransaction.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 运营与履约服务实现。
 * <p>
 * 当前阶段使用内存存储（Daily_Statistics / Delivery_Task 的快照），
 * 待 JPA 仓储接入后，将 ConcurrentHashMap 替换为 Repository 调用即可。
 * <p>
 * 数据依赖说明：
 * <ul>
 *     <li>订单数据：通过 {@link OrderService#getTodayOrders()} 获取当日有效订单。</li>
 *     <li>菜品/分类/单位：当前 {@link OrderDTO} 仅暴露订单项 ID（itemIds），
 *         故聚合按订单项 ID 作为代理键；待订单详情（OrderDetail，含 dishId/quantity/unit/name）
 *         与菜单中心数据暴露后，将聚合维度切换为 dishId，并补全名称/分类/单位。</li>
 *     <li>员工姓名/工位/电话：当前以占位符生成，待用户中心 UserService 暴露后补全。</li>
 * </ul>
 */
@Service
public class FulfillmentServiceImpl implements FulfillmentService
{
	private final OrderService orderService;

	@Value("${fulfillment.order-deadline:09:00}")
	private String orderDeadline;

	@Value("${fulfillment.delivery-start:11:30}")
	private String deliveryStart;

	/** 当日总括订单快照：statisticsDate(毫秒) -> BlanketOrderDTO */
	private final Map<Long, BlanketOrderDTO> blanketOrderStore = new ConcurrentHashMap<>();

	/** 配送任务存储：id -> DeliveryTaskDTO */
	private final Map<Long, DeliveryTaskDTO> deliveryTaskStore = new ConcurrentHashMap<>();

	private final AtomicLong deliveryTaskIdSeq = new AtomicLong(1);

	private final AtomicLong statisticsIdSeq = new AtomicLong(1);

	private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

	public FulfillmentServiceImpl(OrderService orderService) {
		this.orderService = orderService;
	}

	/* ==================== 总括订单 (Blanket Order) ==================== */

	@Override
	public BlanketOrderDTO generateBlanketOrder() {
		if (!isAfterOrderDeadline()) {
			throw new IllegalStateException("未到订餐截止时间（" + orderDeadline + "），尚不能聚合总括订单");
		}
		long today = startOfDayEpoch(System.currentTimeMillis());
		// 幂等：当日已聚合则直接返回
		BlanketOrderDTO existed = blanketOrderStore.get(today);
		if (existed != null) {
			return existed;
		}

		List<OrderDTO> orders = orderService.getTodayOrders();
		// 聚合算法：汇总所有有效订单的订单项，按 itemId 聚合（代理键）。
		// TODO: 待 OrderDetail 暴露后，改为按 dishId 聚合并 sum(quantity)，
		//       同时从菜单中心补全 dishName / category / unit。
		Map<Long, Double> quantityByItem = new HashMap<>();
		for (OrderDTO order : orders) {
			if (order.getItemIds() == null) {
				continue;
			}
			for (Long itemId : order.getItemIds()) {
				quantityByItem.merge(itemId, 1.0, Double::sum);
			}
		}

		List<ProductionItemDTO> items = new ArrayList<>();
		for (var entry : quantityByItem.entrySet()) {
			ProductionItemDTO item = new ProductionItemDTO();
			item.setDishId(entry.getKey());
			item.setDishName("菜品#" + entry.getKey()); // TODO: 从菜单中心补全
			item.setCategory("未分类"); // TODO: 从菜单中心补全
			item.setTotalQuantity(entry.getValue());
			item.setUnit("份"); // TODO: 从订单详情补全
			items.add(item);
		}

		BlanketOrderDTO dto = new BlanketOrderDTO();
		dto.setStatisticsId(statisticsIdSeq.getAndIncrement()); // 占位主键，JPA 接入后由数据库生成
		dto.setStatisticsDate(today);
		dto.setTotalOrders(orders.size());
		dto.setTotalAmount(0.0); // TODO: 需订单总价，待 OrderDTO 暴露总价或由 OrderDetail 累加
		dto.setItems(items);
		dto.setStatus("AGGREGATED");
		dto.setCreatedTime(System.currentTimeMillis());
		blanketOrderStore.put(today, dto);
		return dto;
	}

	@Override
	public BlanketOrderDTO getBlanketOrder(Long date) {
		if (date == null) {
			return null;
		}
		return blanketOrderStore.get(startOfDayEpoch(date));
	}

	@Override
	public BlanketOrderDTO getTodayBlanketOrder() {
		return blanketOrderStore.get(startOfDayEpoch(System.currentTimeMillis()));
	}

	/* ==================== 配送管理 (Delivery) ==================== */

	@Override
	public boolean canPrintDelivery() {
		LocalTime now = LocalTime.now();
		return !now.isBefore(parse(deliveryStart));
	}

	@Override
	public List<DeliveryTaskDTO> generateDeliveryTasks() {
		if (!canPrintDelivery()) {
			throw new IllegalStateException("未到配餐开始时间（" + deliveryStart + "），配送单打印权限未开放");
		}
		long today = startOfDayEpoch(System.currentTimeMillis());

		// 幂等：当日已生成则直接返回
		List<DeliveryTaskDTO> existed = deliveryTaskStore.values().stream()
				.filter(t -> Long.valueOf(today).equals(t.getDeliveryDate()))
				.toList();
		if (!existed.isEmpty()) {
			return existed;
		}

		List<OrderDTO> orders = orderService.getTodayOrders();
		// 按员工维度拆分（配送单的天然分组键）
		Map<Long, List<OrderDTO>> ordersByUser = orders.stream()
				.filter(o -> o.getUserId() != null)
				.collect(Collectors.groupingBy(OrderDTO::getUserId));

		List<DeliveryTaskDTO> tasks = new ArrayList<>();
		for (var entry : ordersByUser.entrySet()) {
			Long userId = entry.getKey();
			List<OrderDTO> userOrders = entry.getValue();

			List<DeliveryItemDTO> items = new ArrayList<>();
			for (OrderDTO order : userOrders) {
				if (order.getItemIds() == null) {
					continue;
				}
				for (Long itemId : order.getItemIds()) {
					DeliveryItemDTO item = new DeliveryItemDTO();
					item.setDishId(itemId);
					item.setDishName("菜品#" + itemId); // TODO: 从菜单中心补全
					item.setQuantity(1.0); // TODO: 真实分量需 OrderDetail.quantity
					item.setUnit("份"); // TODO
					items.add(item);
				}
			}

			DeliveryTaskDTO task = new DeliveryTaskDTO();
			long id = deliveryTaskIdSeq.getAndIncrement();
			task.setId(id);
			task.setTaskId("DT-" + today + "-" + id);
			task.setUserId(userId);
			task.setEmployeeName("员工#" + userId); // TODO: 从用户中心补全
			task.setWorkstation(""); // TODO: 从用户中心补全
			task.setPhone(""); // TODO: 从用户中心补全
			task.setDeliveryDate(today);
			task.setItems(items);
			task.setStatus("PENDING");
			task.setCreatedTime(System.currentTimeMillis());
			deliveryTaskStore.put(id, task);
			tasks.add(task);
		}
		return tasks;
	}

	@Override
	public List<DeliveryTaskDTO> queryDeliveryTasks(DeliveryTaskQueryData queryData) {
		return deliveryTaskStore.values().stream()
				.filter(t -> matchQuery(t, queryData))
				.toList();
	}

	@Override
	public DeliveryTaskDTO getDeliveryTask(Long taskId) {
		if (taskId == null) {
			return null;
		}
		return deliveryTaskStore.get(taskId);
	}

	/* ==================== 内部辅助 ==================== */

	private boolean isAfterOrderDeadline() {
		return !LocalTime.now().isBefore(parse(orderDeadline));
	}

	private LocalTime parse(String hhmm) {
		return LocalTime.parse(hhmm, HH_MM);
	}

	/** 将任意毫秒时间戳归一化为当日 00:00 的毫秒时间戳（系统时区）。 */
	private long startOfDayEpoch(long epochMillis) {
		ZoneId zone = ZoneId.systemDefault();
		LocalDate date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate();
		return date.atStartOfDay(zone).toInstant().toEpochMilli();
	}

	private boolean matchQuery(DeliveryTaskDTO task, DeliveryTaskQueryData q) {
		if (q == null) {
			return true;
		}
		if (q.getDeliveryDate() != null
				&& !Long.valueOf(startOfDayEpoch(q.getDeliveryDate())).equals(task.getDeliveryDate())) {
			return false;
		}
		if (q.getUserId() != null && !q.getUserId().equals(task.getUserId())) {
			return false;
		}
		if (q.getWorkstation() != null && !q.getWorkstation().equals(task.getWorkstation())) {
			return false;
		}
		if (q.getStatus() != null && !q.getStatus().equals(task.getStatus())) {
			return false;
		}
		return true;
	}
}
