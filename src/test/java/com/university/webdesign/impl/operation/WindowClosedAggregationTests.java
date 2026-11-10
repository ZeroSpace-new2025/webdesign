package com.university.webdesign.impl.operation;

import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.config.OperationScheduleConfig;
import com.university.webdesign.domain.operation.DailyStatistics;
import com.university.webdesign.repository.operation.DailyStatisticsRepository;
import com.university.webdesign.service.menu.MenuService;
import com.university.webdesign.service.menu.dto.MenuVO;
import com.university.webdesign.service.operation.BlanketOrderService;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.operation.dto.CategoryStatVO;
import com.university.webdesign.service.operation.dto.DailyStatVO;
import com.university.webdesign.service.operation.dto.ServiceWindowCmd;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import com.university.webdesign.service.order.OrderService;
import com.university.webdesign.service.order.dto.OrderSubmitCmd;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：订餐窗口关闭时的定时聚合必须真的执行，并且覆盖重算。
 * <p>
 * 缺陷背景（两个叠加，导致 09:00 的自动聚合从来没生效）：
 * <ol>
 *     <li>{@code OrderWindowClosedListener} 用 {@code @TransactionalEventListener} 但没开
 *         {@code fallbackExecution}——定时任务发布事件时没有事务，监听器被**静默跳过**；</li>
 *     <li>即使被调用，在 {@code AFTER_COMMIT} 里以 {@code REQUIRED} 传播进入也会“加入”
 *         已完成的事务，写库时抛 {@code TransactionRequiredException}，异常被事务同步器吞掉。</li>
 * </ol>
 * 本测试直接调用定时任务入口（没有事务）验证这两点都已被修好：先人为把当日快照写脏，
 * 再触发任务，快照必须被覆盖重算回正确值。
 */
@SpringBootTest
class WindowClosedAggregationTests
{
	/**
	 * 种子账号 sunliu（EMPLOYEE）的固定 ID
	 */
	private static final Long EMPLOYEE_ID = 8L;

	@Autowired
	private OperationScheduleConfig scheduleConfig;

	@Autowired
	private OrderService orderService;

	@Autowired
	private MenuService menuService;

	@Autowired
	private BlanketOrderService blanketOrderService;

	@Autowired
	private ServiceWindowService serviceWindowService;

	@Autowired
	private DailyStatisticsRepository dailyStatisticsRepository;

	private Long windowConfigId;

	private LocalTime originalCutoff;

	private LocalTime originalDeliveryStart;

	private LocalDate originalEffectiveFrom;

	@BeforeEach
	void prepare() {
		ServiceWindowVO window = serviceWindowService.get(LocalDate.now(), null, null);
		assertNotNull(window, "种子数据里应有全局时间窗口");
		windowConfigId = window.getConfigId();
		originalCutoff = window.getCutoffTime();
		originalDeliveryStart = window.getDeliveryStartTime();
		originalEffectiveFrom = window.getEffectiveFrom() == null ? LocalDate.now() : window.getEffectiveFrom();

		// 1) 先把窗口放开，下一单（用例与真实时钟无关）
		applyWindow(LocalTime.of(23, 58), LocalTime.of(23, 59));
		MenuVO menu = menuService.getCurrent(LocalDate.now());
		assertNotNull(menu, "测试依赖启动时初始化的“当日已发布菜单”");
		Long recipeId = menu.getItems().get(0).getRecipeId();

		UserContextHolder.set(new UserContext(EMPLOYEE_ID, "sunliu", "孙六", 6L,
				null, null, List.of("EMPLOYEE"), List.of("order:submit")));
		OrderSubmitCmd cmd = new OrderSubmitCmd();
		cmd.setMenuId(menu.getMenuId());
		cmd.setIdempotencyKey("test-window-closed-" + UUID.randomUUID());
		OrderSubmitCmd.SubmitItem item = new OrderSubmitCmd.SubmitItem();
		item.setRecipeId(recipeId);
		item.setQuantity(2);
		cmd.setItems(List.of(item));
		orderService.submit(cmd);
	}

	@AfterEach
	void restore() {
		UserContextHolder.clear();
		if (windowConfigId != null) {
			ServiceWindowCmd cmd = new ServiceWindowCmd();
			cmd.setCutoffTime(originalCutoff);
			cmd.setDeliveryStartTime(originalDeliveryStart);
			cmd.setEffectiveFrom(originalEffectiveFrom);
			cmd.setScope("GLOBAL");
			serviceWindowService.update(windowConfigId, cmd);
		}
	}

	@Test
	void scheduledAggregationRunsWithoutTransactionAndOverwritesStaleSnapshot() {
		LocalDate today = LocalDate.now();

		// 2) 关窗：截止时间 00:00 / 配餐 00:01，聚合的“必须已过截止时间”校验才能通过
		applyWindow(LocalTime.of(0, 0), LocalTime.of(0, 1));

		// 3) 人为把当日快照写脏（模拟截止前写下的旧快照，且明细为空）
		DailyStatistics stale = dailyStatisticsRepository.findByStatisticsDate(today).orElse(null);
		assertNotNull(stale, "下单后应当已经由 OrderCreatedListener 生成当日快照");
		stale.getItems().clear();
		stale.setTotalOrders(999);
		stale.setTotalAmount(new BigDecimal("999999.00"));
		dailyStatisticsRepository.saveAndFlush(stale);

		// 4) 触发定时任务入口：该方法本身不是 @Transactional，事件在无事务上下文中发布，
		//    正好覆盖 fallbackExecution + REQUIRES_NEW 两个修复点
		scheduleConfig.orderWindowCloseJob();

		// 5) 断言快照已被覆盖重算
		List<CategoryStatVO> items = blanketOrderService.listAggregated(today, null);
		assertFalse(items.isEmpty(), "定时聚合必须写入当日总括订单明细");
		DailyStatVO snapshot = blanketOrderService.pageDailyStat(null, null).getList().stream()
				.filter(vo -> today.equals(vo.getStatisticsDate()))
				.findFirst()
				.orElse(null);
		assertNotNull(snapshot, "当日快照必须存在");
		assertTrue(snapshot.getTotalOrders() < 999, "定时聚合必须覆盖掉脏数据（force = true）");
		assertTrue(snapshot.getTotalAmount().compareTo(new BigDecimal("999999.00")) != 0,
				"定时聚合必须覆盖掉脏金额（force = true）");
	}

	/**
	 * 改写当日全局时间窗口
	 *
	 * @param cutoff       订餐截止时间
	 * @param deliveryStart 配餐开始时间
	 */
	private void applyWindow(LocalTime cutoff, LocalTime deliveryStart) {
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(cutoff);
		cmd.setDeliveryStartTime(deliveryStart);
		cmd.setEffectiveFrom(originalEffectiveFrom);
		cmd.setScope("GLOBAL");
		serviceWindowService.update(windowConfigId, cmd);
	}
}
