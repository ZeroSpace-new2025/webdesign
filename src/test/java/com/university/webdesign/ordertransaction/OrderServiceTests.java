package com.university.webdesign.ordertransaction;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuItemData;
import com.university.webdesign.menurecipe.service.MenuService;
import com.university.webdesign.ordertransaction.api.OrderCreateData;
import com.university.webdesign.ordertransaction.api.OrderDTO;
import com.university.webdesign.ordertransaction.api.OrderQueryData;
import com.university.webdesign.ordertransaction.api.OrderUpdateData;
import com.university.webdesign.ordertransaction.api.PersonalConsumptionDTO;
import com.university.webdesign.ordertransaction.data.OrderStatus;
import com.university.webdesign.ordertransaction.repository.OrderRepository;
import com.university.webdesign.ordertransaction.service.OrderService;
import com.university.webdesign.support.StubServicesTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 订单模块集成测试
 * <p>
 * 使用 H2 内存库跑真实 JPA 流程，菜单模块用桩替换（其实现尚未提供），
 * 覆盖时间窗口、一人一天一单、价格快照、改单、删单、检索与月度统计等核心规则。
 */
@SpringBootTest
@Import(StubServicesTestConfiguration.class)
@ContextConfiguration(initializers = OrderServiceTests.CutoffInitializer.class)
class OrderServiceTests
{
	/**
	 * 把“订餐截止时间”推迟到 23:59:59，并固定业务时区。
	 * <p>
	 * 这样测试在任意时刻运行都处于“未截止”状态，避免依赖运行时间导致偶发失败。
	 */
	static class CutoffInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
	{
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertyValues.of("order.cutoff-time=23:59:59", "order.zone=Asia/Shanghai")
					.applyTo(applicationContext.getEnvironment());
		}
	}
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private OrderRepository orderRepository;
	
	@MockitoBean
	private MenuService menuService;
	
	@BeforeEach
	void setUp() {
		orderRepository.deleteAll();
		
		MenuDTO menu = new MenuDTO();
		menu.setId(1L);
		menu.setName("今日菜单");
		menu.setStatus("已发布");
		menu.setMenuItems(List.of(
				new MenuItemData(101L, "小炒肉", "热菜", new BigDecimal("18.00")),
				new MenuItemData(102L, "米饭", "主食", new BigDecimal("2.00"))));
		Mockito.when(menuService.getActiveMenu(Mockito.any(LocalDate.class))).thenReturn(menu);
	}
	
	@AfterEach
	void tearDown() {
		// 角色开关是跨测试共享的静态状态，必须复位
		StubServicesTestConfiguration.grantAllRoles(false);
	}
	
	/**
	 * 构造下单请求
	 *
	 * @param userId     员工ID
	 * @param itemIds    菜品ID
	 * @param quantities 数量
	 * @return 下单请求
	 */
	private OrderCreateData createData(Long userId, List<Long> itemIds, List<Integer> quantities) {
		OrderCreateData data = new OrderCreateData();
		data.setOperatorId(userId);
		data.setItemIds(itemIds);
		data.setQuantities(quantities);
		return data;
	}
	
	/**
	 * 构造改单请求
	 *
	 * @param orderId    订单ID
	 * @param operatorId 操作人
	 * @param itemIds    菜品ID
	 * @param quantities 数量
	 * @return 改单请求
	 */
	private OrderUpdateData updateData(Long orderId, Long operatorId, List<Long> itemIds, List<Integer> quantities) {
		OrderUpdateData data = new OrderUpdateData();
		data.setOrderId(orderId);
		data.setOperatorId(operatorId);
		data.setItemIds(itemIds);
		data.setQuantities(quantities);
		return data;
	}
	
	@Test
	@DisplayName("下单成功：按菜单价计算总价，并保存菜名/分类/单价快照")
	void createOrderShouldSnapshotPriceAndTotal() {
		OrderDTO order = orderService.createOrder(createData(1L, List.of(101L, 102L), List.of(2, 3)));
		
		assertThat(order.getOrderId()).isNotNull();
		assertThat(order.getOrderNumber()).isNotNull();
		assertThat(order.getTotal()).isEqualByComparingTo("42.00");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.UNPAID.toString());
		assertThat(order.getItems()).hasSize(2);
		assertThat(order.getItems()).anySatisfy(item -> {
			assertThat(item.getItemName()).isEqualTo("小炒肉");
			assertThat(item.getCategory()).isEqualTo("热菜");
			assertThat(item.getUnitPrice()).isEqualByComparingTo("18.00");
			assertThat(item.getSubtotal()).isEqualByComparingTo("36.00");
		});
		assertThat(order.getItemIds()).containsExactly(101L, 102L);
	}
	
	@Test
	@DisplayName("一人一天一单：同一天再次下单被拒绝")
	void createOrderTwiceShouldFail() {
		orderService.createOrder(createData(1L, List.of(101L), List.of(1)));
		
		assertThatThrownBy(() -> orderService.createOrder(createData(1L, List.of(102L), List.of(1))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("每人每天只能点一份餐");
	}
	
	@Test
	@DisplayName("下单校验：菜品不在当日菜单、数量非法、数量与菜品不匹配都要拦住")
	void createOrderShouldValidateRequest() {
		assertThatThrownBy(() -> orderService.createOrder(createData(2L, List.of(999L), List.of(1))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不在今日菜单中");
		
		assertThatThrownBy(() -> orderService.createOrder(createData(2L, List.of(101L), List.of(0))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("数量必须为正整数");
		
		assertThatThrownBy(() -> orderService.createOrder(createData(2L, List.of(101L, 102L), List.of(1))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不匹配");
	}
	
	@Test
	@DisplayName("时间窗口：当日处于可下单窗口，历史日期不允许操作")
	void timeWindowShouldRejectOtherDates() {
		// 测试上下文的截止时间被初始化为 23:59:59，故当日应处于可下单窗口
		assertThat(orderService.isBeforeCutoff(LocalDate.now())).isTrue();
		// 历史日期不允许操作
		assertThat(orderService.isBeforeCutoff(LocalDate.now().minusDays(1))).isFalse();
	}
	
	@Test
	@DisplayName("改单：整体替换明细并重算总价")
	void updateOrderShouldReplaceItems() {
		OrderDTO created = orderService.createOrder(createData(3L, List.of(101L), List.of(1)));
		
		OrderDTO updated = orderService.updateOrder(
				updateData(created.getOrderId(), 3L, List.of(102L), List.of(5)));
		
		assertThat(updated.getTotal()).isEqualByComparingTo("10.00");
		assertThat(updated.getItems()).hasSize(1);
		assertThat(updated.getItems().get(0).getItemName()).isEqualTo("米饭");
	}
	
	@Test
	@DisplayName("改单：不能修改他人订单")
	void updateOrderByOtherUserShouldFail() {
		OrderDTO created = orderService.createOrder(createData(4L, List.of(101L), List.of(1)));
		
		assertThatThrownBy(() -> orderService.updateOrder(
				updateData(created.getOrderId(), 5L, List.of(101L), List.of(1))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("只能操作自己的订单");
	}
	
	@Test
	@DisplayName("删除订单：经理删单以已取消留痕，且取消后不再占用当日名额")
	void deleteOrderShouldCancelAndFreeQuota() {
		StubServicesTestConfiguration.grantAllRoles(true);
		OrderDTO created = orderService.createOrder(createData(6L, List.of(101L), List.of(1)));
		
		OrderDTO deleted = orderService.deleteOrder(created.getOrderId(), 1L);
		assertThat(deleted.getStatus()).isEqualTo(OrderStatus.CANCELLED.toString());
		// 留痕：记录仍在库中，不做物理删除
		assertThat(orderRepository.findById(created.getOrderId())).isPresent();
		
		// 取消后当日名额释放，可以重新下单
		OrderDTO again = orderService.createOrder(createData(6L, List.of(102L), List.of(1)));
		assertThat(again.getOrderId()).isNotEqualTo(created.getOrderId());
	}
	
	@Test
	@DisplayName("越权校验：没有经理角色不能删除订单")
	void deleteOrderRequiresManagerRole() {
		// 默认桩不授予任何角色
		OrderDTO created = orderService.createOrder(createData(13L, List.of(101L), List.of(1)));
		
		assertThatThrownBy(() -> orderService.deleteOrder(created.getOrderId(), 99L))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("无权执行该操作");
		
		// 订单未被取消
		assertThat(orderService.getOrder(created.getOrderId()).getStatus())
				.isEqualTo(OrderStatus.UNPAID.toString());
	}
	
	@Test
	@DisplayName("越权校验：员工只能查自己的历史与消费，经理/财务可查他人")
	void personalQueriesRequireSelfOrRole() {
		OrderDTO order = orderService.createOrder(createData(14L, List.of(101L), List.of(1)));
		LocalDate today = LocalDate.now();
		
		// 无角色的人查他人：拒绝
		assertThatThrownBy(() -> orderService.getHistoryOrders(14L, 99L, null, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("无权执行该操作");
		assertThatThrownBy(() -> orderService.getMonthlyConsumption(14L, 99L, today.getYear(), today.getMonthValue()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("无权执行该操作");
		
		// 查自己：允许
		assertThat(orderService.getHistoryOrders(14L, 14L, null, null)).hasSize(1);
		assertThat(orderService.getMonthlyConsumption(14L, 14L, today.getYear(), today.getMonthValue())
				.getOrderCount()).isEqualTo(1);
		
		// 财务角色查他人：允许
		StubServicesTestConfiguration.grantAllRoles(true);
		assertThat(orderService.getHistoryOrders(14L, 99L, null, null))
				.extracting(OrderDTO::getOrderId)
				.containsExactly(order.getOrderId());
	}
	
	@Test
	@DisplayName("支付：未支付订单可支付，已支付订单不可重复支付")
	void payOrderShouldChangeStatus() {
		OrderDTO created = orderService.createOrder(createData(7L, List.of(101L), List.of(1)));
		
		OrderDTO paid = orderService.payOrder(created.getOrderId(), 7L);
		assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID.toString());
		
		assertThatThrownBy(() -> orderService.payOrder(created.getOrderId(), 7L))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("不可支付");
	}
	
	@Test
	@DisplayName("支付/取消必须带操作人身份")
	void operatorIsRequiredForPayAndCancel() {
		OrderDTO created = orderService.createOrder(createData(12L, List.of(101L), List.of(1)));
		
		assertThatThrownBy(() -> orderService.payOrder(created.getOrderId(), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("缺少操作用户ID");
		
		assertThatThrownBy(() -> orderService.cancelOrder(created.getOrderId(), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("缺少操作用户ID");
	}
	
	@Test
	@DisplayName("查询：默认过滤已取消订单，按条件可查指定员工的订单")
	void queryShouldFilterCancelledByDefault() {
		StubServicesTestConfiguration.grantAllRoles(true);
		OrderDTO kept = orderService.createOrder(createData(8L, List.of(101L), List.of(1)));
		OrderDTO removed = orderService.createOrder(createData(9L, List.of(102L), List.of(1)));
		orderService.deleteOrder(removed.getOrderId(), 1L);
		
		List<OrderDTO> active = orderService.getTodayOrders();
		assertThat(active).extracting(OrderDTO::getOrderId)
				.contains(kept.getOrderId())
				.doesNotContain(removed.getOrderId());
		
		OrderQueryData queryData = new OrderQueryData();
		queryData.setUserId(9L);
		queryData.setIncludeCancelled(Boolean.TRUE);
		assertThat(orderService.query(queryData)).hasSize(1);
		
		// 个人历史查询默认包含已取消订单
		assertThat(orderService.getHistoryOrders(9L, null, null)).hasSize(1);
	}
	
	@Test
	@DisplayName("月度消费统计：只统计有效订单，并按菜品汇总")
	void monthlyConsumptionShouldAggregate() {
		orderService.createOrder(createData(10L, List.of(101L, 102L), List.of(2, 2)));
		
		LocalDate today = LocalDate.now();
		PersonalConsumptionDTO consumption =
				orderService.getMonthlyConsumption(10L, today.getYear(), today.getMonthValue());
		
		assertThat(consumption.getMonth())
				.isEqualTo(String.format("%04d-%02d", today.getYear(), today.getMonthValue()));
		assertThat(consumption.getOrderCount()).isEqualTo(1);
		assertThat(consumption.getTotalAmount()).isEqualByComparingTo("40.00");
		assertThat(consumption.getItemSummaries()).hasSize(2);
		assertThat(consumption.getItemSummaries()).anySatisfy(summary -> {
			assertThat(summary.getItemName()).isEqualTo("小炒肉");
			assertThat(summary.getQuantity()).isEqualTo(2L);
			assertThat(summary.getAmount()).isEqualByComparingTo("36.00");
		});
	}
	
	@Test
	@DisplayName("查询指定日期有效订单：供履约模块判断员工当日是否已点餐")
	void findActiveOrderShouldWork() {
		StubServicesTestConfiguration.grantAllRoles(true);
		assertThat(orderService.findActiveOrder(11L, LocalDate.now())).isNull();
		
		OrderDTO created = orderService.createOrder(createData(11L, List.of(101L), List.of(1)));
		OrderDTO active = orderService.findActiveOrder(11L, LocalDate.now());
		assertThat(active).isNotNull();
		assertThat(active.getOrderId()).isEqualTo(created.getOrderId());
		
		orderService.deleteOrder(created.getOrderId(), 1L);
		assertThat(orderService.findActiveOrder(11L, LocalDate.now())).isNull();
	}
}
