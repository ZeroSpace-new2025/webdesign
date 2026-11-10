package com.university.webdesign.impl.order;

import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.service.menu.MenuService;
import com.university.webdesign.service.menu.dto.MenuItemVO;
import com.university.webdesign.service.menu.dto.MenuVO;
import com.university.webdesign.service.operation.BlanketOrderService;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.operation.dto.CategoryStatVO;
import com.university.webdesign.service.operation.dto.ServiceWindowCmd;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import com.university.webdesign.service.order.OrderService;
import com.university.webdesign.service.order.dto.OrderModifyCmd;
import com.university.webdesign.service.order.dto.OrderSubmitCmd;
import com.university.webdesign.service.order.dto.OrderVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 回归测试：新增/修改订单后必须刷新当日 {@code daily_statistics} 快照。
 * <p>
 * 缺陷背景：下单只发布 {@code OrderCreatedEvent}，但重构后只有“订单变更”监听器会重算快照，
 * 新建订单不会；而 {@code BlanketOrderService.aggregate(date, force = false)} 是
 * “已有快照直接复用”的幂等语义，09:00 的定时聚合拿到的仍是旧快照，
 * 结果厨房生产单少算截止前新提交的订单。
 * <p>
 * 本测试不加 {@code @Transactional}：监听器是 {@code AFTER_COMMIT}，必须在真实提交后才会执行。
 */
@SpringBootTest
class OrderAggregationRefreshTests
{
	/**
	 * 种子账号 zhangsan（EMPLOYEE，带 order:submit）的固定 ID
	 */
	private static final Long EMPLOYEE_ID = 5L;

	@Autowired
	private OrderService orderService;

	@Autowired
	private MenuService menuService;

	@Autowired
	private BlanketOrderService blanketOrderService;

	@Autowired
	private ServiceWindowService serviceWindowService;

	/**
	 * 被测试临时改写的全局时间窗口（结束时要还原，避免污染同一上下文里的其它用例）
	 */
	private Long windowConfigId;

	private LocalTime originalCutoff;

	private LocalTime originalDeliveryStart;

	private LocalDate originalEffectiveFrom;

	@BeforeEach
	void openOrderingWindow() {
		ServiceWindowVO window = serviceWindowService.get(LocalDate.now(), null, null);
		assertNotNull(window, "种子数据里应有全局时间窗口（reploy/sql/seed.sql 第 4 段）");
		windowConfigId = window.getConfigId();
		originalCutoff = window.getCutoffTime();
		originalDeliveryStart = window.getDeliveryStartTime();
		originalEffectiveFrom = window.getEffectiveFrom() == null ? LocalDate.now() : window.getEffectiveFrom();

		// 下单受“当日截止时间”限制，用例若跑在 09:00 之后就下不了单；
		// 这里把当日窗口推到 23:58/23:59，让用例与真实时钟无关（结束后还原）。
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(LocalTime.of(23, 58));
		cmd.setDeliveryStartTime(LocalTime.of(23, 59));
		cmd.setEffectiveFrom(window.getEffectiveFrom() == null ? LocalDate.now() : window.getEffectiveFrom());
		cmd.setScope(window.getScope());
		serviceWindowService.update(windowConfigId, cmd);
	}

	@AfterEach
	void restoreWindowAndContext() {
		UserContextHolder.clear();
		if (windowConfigId == null) {
			return;
		}
		ServiceWindowCmd restore = new ServiceWindowCmd();
		restore.setCutoffTime(originalCutoff);
		restore.setDeliveryStartTime(originalDeliveryStart);
		restore.setEffectiveFrom(originalEffectiveFrom);
		restore.setScope("GLOBAL");
		serviceWindowService.update(windowConfigId, restore);
	}

	@Test
	void creatingAndModifyingOrderRefreshesDailyStatistics() {
		LocalDate today = LocalDate.now();
		MenuVO menu = menuService.getCurrent(today);
		assertNotNull(menu, "测试依赖启动时初始化的“当日已发布菜单”（DatabaseDataInitializer）");
		assertNotNull(menu.getItems(), "当日菜单必须有菜品");
		assertFalse(menu.getItems().isEmpty(), "当日菜单必须有菜品");
		MenuItemVO menuItem = menu.getItems().get(0);
		Long recipeId = menuItem.getRecipeId();
		assertNotNull(recipeId, "菜单项必须关联菜品ID");

		UserContextHolder.set(new UserContext(EMPLOYEE_ID, "zhangsan", "张三", 5L,
				null, null, List.of("EMPLOYEE"), List.of("order:submit")));

		int beforeQuantity = quantityOf(blanketOrderService.listAggregated(today, null), recipeId);

		// 1) 下单：快照必须把这张新订单算进去
		OrderSubmitCmd submit = new OrderSubmitCmd();
		submit.setMenuId(menu.getMenuId());
		submit.setIdempotencyKey("test-aggregation-" + UUID.randomUUID());
		submit.setItems(List.of(item(recipeId, 3)));
		OrderVO created = orderService.submit(submit);
		assertNotNull(created.getOrderId(), "下单应返回订单ID");
		assertEquals(beforeQuantity + 3,
				quantityOf(blanketOrderService.listAggregated(today, null), recipeId),
				"下单后总括订单必须包含这张新订单");

		// 2) 改单：快照必须跟着改
		OrderModifyCmd modify = new OrderModifyCmd();
		modify.setVersion(created.getVersion());
		modify.setItems(List.of(item(recipeId, 5)));
		orderService.modify(created.getOrderId(), modify);
		assertEquals(beforeQuantity + 5,
				quantityOf(blanketOrderService.listAggregated(today, null), recipeId),
				"改单后总括订单必须按新数量重算");
	}

	/**
	 * 构造一条下单明细
	 *
	 * @param recipeId 菜品ID
	 * @param quantity 数量
	 * @return 明细
	 */
	private OrderSubmitCmd.SubmitItem item(Long recipeId, int quantity) {
		OrderSubmitCmd.SubmitItem item = new OrderSubmitCmd.SubmitItem();
		item.setRecipeId(recipeId);
		item.setQuantity(quantity);
		return item;
	}

	/**
	 * 取快照里某个菜品的需求总量
	 *
	 * @param stats    快照明细
	 * @param recipeId 菜品ID
	 * @return 需求总量；不存在时返回 0
	 */
	private int quantityOf(List<CategoryStatVO> stats, Long recipeId) {
		for (CategoryStatVO stat : stats) {
			if (recipeId != null && recipeId.equals(stat.getRecipeId())) {
				return stat.getTotalQuantity();
			}
		}
		return 0;
	}
}
