package com.university.webdesign.ordertransaction;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuItemData;
import com.university.webdesign.menurecipe.service.MenuService;
import com.university.webdesign.ordertransaction.api.OrderCreateData;
import com.university.webdesign.ordertransaction.api.OrderDTO;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 订单模块页面测试
 * <p>
 * 用 MockMvc 真实渲染 Thymeleaf 模板（模板语法错误、变量名写错都会在这里暴露），
 * 并覆盖页面上的两条主流程：提交下单/改单、取消订单。
 * <p>
 * 截止时间推迟到 23:59:59，保证测试在任意时刻运行都处于可下单窗口；
 * 使用独立的 H2 库名，避免与其他测试类共用内存库互相影响。
 */
@SpringBootTest(properties = {
		"order.cutoff-time=23:59:59",
		"order.zone=Asia/Shanghai",
		"spring.datasource.url=jdbc:h2:mem:webdesign_order_page;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
})
@AutoConfigureMockMvc
@Import(StubServicesTestConfiguration.class)
class OrderPageTests
{
	/**
	 * 业务时区，与 {@code order.zone} 配置保持一致
	 */
	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
	
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private OrderRepository orderRepository;
	
	@MockitoBean
	private MenuService menuService;
	
	@BeforeEach
	void setUp() {
		orderRepository.deleteAll();
		Mockito.when(menuService.getActiveMenu(Mockito.any(LocalDate.class))).thenReturn(todayMenu());
	}
	
	@AfterEach
	void tearDown() {
		// 角色开关是跨测试共享的静态状态，必须复位
		StubServicesTestConfiguration.grantAllRoles(false);
	}
	
	/**
	 * 构造今日菜单
	 *
	 * @return 已发布的今日菜单
	 */
	private MenuDTO todayMenu() {
		MenuDTO menu = new MenuDTO();
		menu.setId(1L);
		menu.setName("今日菜单");
		menu.setStatus("已发布");
		menu.setMenuItems(List.of(
				new MenuItemData(101L, "小炒肉", "热菜", new BigDecimal("18.00")),
				new MenuItemData(102L, "米饭", "主食", new BigDecimal("2.00"))));
		return menu;
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
	 * 建一个已经设置了员工身份的会话（走页面上的身份切换入口）
	 *
	 * @param userId 员工ID
	 * @return 带身份的会话
	 * @throws Exception 请求异常
	 */
	private MockHttpSession identitySession(Long userId) throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(post("/order/identity").session(session).param("operatorId", String.valueOf(userId)))
				.andExpect(status().is3xxRedirection());
		return session;
	}
	
	/**
	 * 发起 GET 请求并返回渲染后的 HTML
	 *
	 * @param path   路径
	 * @param params 交替出现的参数名与参数值
	 * @return 渲染后的 HTML
	 * @throws Exception 请求异常
	 */
	private String getHtml(String path, String... params) throws Exception {
		MockHttpServletRequestBuilder builder = get(path);
		for (int i = 0; i + 1 < params.length; i += 2) {
			builder = builder.param(params[i], params[i + 1]);
		}
		return mockMvc.perform(builder)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}
	
	@Test
	@DisplayName("点餐页：渲染今日菜单，并按分类分组给出数量输入框")
	void createPageShouldRenderTodayMenu() throws Exception {
		mockMvc.perform(get("/order/create").param("operatorId", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("ordertransaction/order-create"));
		
		String html = getHtml("/order/create", "operatorId", "1");
		assertThat(html).contains("今日点餐");
		assertThat(html).contains("小炒肉");
		assertThat(html).contains("热菜");
		assertThat(html).contains("主食");
		// 数量输入框按 quantity_<菜品ID> 命名，服务端据此读取已选菜品
		assertThat(html).contains("quantity_101");
		assertThat(html).contains("quantity_102");
	}
	
	@Test
	@DisplayName("点餐页：今日菜单未发布时给出空状态提示，不报错")
	void createPageShouldHandleEmptyMenu() throws Exception {
		Mockito.when(menuService.getActiveMenu(Mockito.any(LocalDate.class))).thenReturn(null);
		
		String html = getHtml("/order/create", "operatorId", "1");
		assertThat(html).contains("今日菜单尚未发布");
	}
	
	@Test
	@DisplayName("点餐流程：提交生成订单，第二次提交走改单而不是新订单")
	void submitShouldCreateThenUpdateOrder() throws Exception {
		MockHttpSession session = identitySession(1L);
		
		mockMvc.perform(post("/order/submit").session(session)
						.param("quantity_101", "2")
						.param("quantity_102", "1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/order/create"));
		
		OrderDTO created = orderService.findActiveOrder(1L, LocalDate.now(ZONE));
		assertThat(created).isNotNull();
		assertThat(created.getTotal()).isEqualByComparingTo("38.00");
		
		// 已经有今日订单，再提交同样走“一人一天一单”的改单分支
		mockMvc.perform(post("/order/submit").session(session)
						.param("quantity_101", "1")
						.param("quantity_102", "0"))
				.andExpect(status().is3xxRedirection());
		
		OrderDTO updated = orderService.findActiveOrder(1L, LocalDate.now(ZONE));
		assertThat(updated.getOrderId()).isEqualTo(created.getOrderId());
		assertThat(updated.getTotal()).isEqualByComparingTo("18.00");
		assertThat(updated.getItems()).hasSize(1);
	}
	
	@Test
	@DisplayName("点餐流程：一道菜都没选时把原因回显到页面，不产生订单")
	void submitWithoutSelectionShouldShowError() throws Exception {
		MockHttpSession session = identitySession(2L);
		
		mockMvc.perform(post("/order/submit").session(session)
						.param("quantity_101", "0")
						.param("quantity_102", ""))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/order/create"));
		
		String html = mockMvc.perform(get("/order/create").session(session))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(html).contains("请至少选择一道菜品");
		assertThat(orderService.findActiveOrder(2L, LocalDate.now(ZONE))).isNull();
	}
	
	@Test
	@DisplayName("我的订单：未设置身份时给出提示，页面仍可正常打开")
	void myOrdersPageWithoutIdentityShouldHint() throws Exception {
		mockMvc.perform(get("/order/mine"))
				.andExpect(status().isOk())
				.andExpect(view().name("ordertransaction/my-orders"));
		
		String html = getHtml("/order/mine");
		assertThat(html).contains("尚未设置员工身份");
	}
	
	@Test
	@DisplayName("我的订单：展示历史订单与快照明细，顶部展示今日订单")
	void myOrdersPageShouldRenderHistory() throws Exception {
		orderService.createOrder(createData(3L, List.of(101L, 102L), List.of(1, 2)));
		
		String html = getHtml("/order/mine", "operatorId", "3");
		assertThat(html).contains("我的今日订单");
		assertThat(html).contains("小炒肉");
		assertThat(html).contains("下单时间");
		assertThat(html).contains("消费合计");
	}
	
	@Test
	@DisplayName("我的订单：取消今日订单后状态为已取消，且不再占用当日名额")
	void cancelShouldMarkOrderCancelled() throws Exception {
		OrderDTO created = orderService.createOrder(createData(4L, List.of(101L), List.of(1)));
		MockHttpSession session = identitySession(4L);
		
		mockMvc.perform(post("/order/{id}/cancel", created.getOrderId()).session(session)
						.param("redirectTo", "/order/mine"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/order/mine"));
		
		assertThat(orderService.getOrder(created.getOrderId()).getStatus())
				.isEqualTo(OrderStatus.CANCELLED.toString());
		// 取消是置状态留痕，不做物理删除
		assertThat(orderRepository.findById(created.getOrderId())).isPresent();
		assertThat(orderService.findActiveOrder(4L, LocalDate.now(ZONE))).isNull();
	}
	
	@Test
	@DisplayName("我的订单：不能操作他人订单，错误信息回显到页面")
	void cancelOtherUserOrderShouldShowError() throws Exception {
		OrderDTO created = orderService.createOrder(createData(6L, List.of(101L), List.of(1)));
		MockHttpSession session = identitySession(5L);
		
		mockMvc.perform(post("/order/{id}/cancel", created.getOrderId()).session(session))
				.andExpect(status().is3xxRedirection());
		
		assertThat(orderService.getOrder(created.getOrderId()).getStatus())
				.isEqualTo(OrderStatus.UNPAID.toString());
		
		String html = mockMvc.perform(get("/order/mine").session(session))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(html).contains("只能操作自己的订单");
	}
	
	@Test
	@DisplayName("我的订单：支付后状态变为已支付")
	void payShouldChangeStatus() throws Exception {
		OrderDTO created = orderService.createOrder(createData(7L, List.of(102L), List.of(3)));
		MockHttpSession session = identitySession(7L);
		
		mockMvc.perform(post("/order/{id}/pay", created.getOrderId()).session(session))
				.andExpect(status().is3xxRedirection());
		
		assertThat(orderService.getOrder(created.getOrderId()).getStatus())
				.isEqualTo(OrderStatus.PAID.toString());
	}
}
