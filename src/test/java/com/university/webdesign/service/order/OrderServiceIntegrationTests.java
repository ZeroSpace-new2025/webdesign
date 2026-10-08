package com.university.webdesign.service.order;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.PermCodes;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.domain.menu.RecipeStatus;
import com.university.webdesign.domain.order.OrderStatus;
import com.university.webdesign.domain.user.Permission;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.domain.user.User;
import com.university.webdesign.domain.user.UserStatus;
import com.university.webdesign.repository.order.OrderFormRepository;
import com.university.webdesign.repository.user.PermissionRepository;
import com.university.webdesign.repository.user.RoleRepository;
import com.university.webdesign.repository.user.UserRepository;
import com.university.webdesign.service.menu.MenuService;
import com.university.webdesign.service.menu.RecipeService;
import com.university.webdesign.service.menu.dto.MenuCreateCmd;
import com.university.webdesign.service.menu.dto.MenuCreateItem;
import com.university.webdesign.service.menu.dto.RecipeCreateCmd;
import com.university.webdesign.service.menu.dto.RecipeVO;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.operation.dto.ServiceWindowCmd;
import com.university.webdesign.service.order.dto.OrderHistoryQuery;
import com.university.webdesign.service.order.dto.OrderModifyCmd;
import com.university.webdesign.service.order.dto.OrderSubmitCmd;
import com.university.webdesign.service.order.dto.OrderVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 订单与交易核心的集成测试。
 * <p>
 * 走真实的 Spring 上下文与 H2 数据库，覆盖《对外方法表》M2 与 `demand.md` 的硬性业务不变量：
 * <ol>
 *     <li>下单写入明细快照（菜名/分类/单位/单价）并正确计算总价；</li>
 *     <li>幂等键命中时返回既有订单，不重复下单；</li>
 *     <li>一人一天一单（42202）；</li>
 *     <li>改单整体替换明细并重算总价，版本冲突抛 40902；</li>
 *     <li>员工只能取消自己的订单，取消后状态为 CANCELLED；</li>
 *     <li>经理作废走 INVALID + 审计留痕，且普通员工无权作废（40300）；</li>
 *     <li>越权查询他人订单详情抛 40300；</li>
 *     <li>个人历史分页只返回本人订单。</li>
 * </ol>
 * 重构前这里依赖 `StubServicesTestConfiguration` 的角色开关，现在改为造真实的角色/权限数据，
 * 因此测试覆盖的是真实鉴权链路。
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "app.storage=none")
class OrderServiceIntegrationTests
{
	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderQueryService orderQueryService;

	@Autowired
	private MenuService menuService;

	@Autowired
	private RecipeService recipeService;

	@Autowired
	private ServiceWindowService serviceWindowService;

	@Autowired
	private OrderFormRepository orderFormRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * 当前登录员工ID
	 */
	private Long employeeId;

	/**
	 * 当前登录经理ID
	 */
	private Long managerId;

	/**
	 * 当日菜单中的菜品（食谱）ID
	 */
	private Long recipeId;

	/**
	 * 当日菜单ID
	 */
	private Long menuId;

	@BeforeEach
	void setUp() {
		Permission submit = permission(PermCodes.ORDER_SUBMIT);
		Permission invalidate = permission(PermCodes.ORDER_INVALIDATE);
		permissionRepository.saveAll(List.of(submit, invalidate));

		Role employeeRole = role(RoleCodes.EMPLOYEE, Set.of(submit));
		Role managerRole = role(RoleCodes.MANAGER, Set.of(submit, invalidate));
		roleRepository.saveAll(List.of(employeeRole, managerRole));

		employeeId = user("employee001", "张三", RoleCodes.EMPLOYEE).getId();
		managerId = user("manager001", "林经理", RoleCodes.MANAGER).getId();

		// 时间窗口：截止时间设成当天 23:59（晚于任何运行时刻），保证测试随时都能下单；
		// 配餐开始时间必须晚于截止时间，否则服务层会按参数非法拒绝。
		// 生效日期固定用一年前，避免同一作用域+生效日期的唯一约束在用例间冲突。
		ServiceWindowCmd window = new ServiceWindowCmd();
		window.setCutoffTime(LocalTime.of(23, 58));
		window.setDeliveryStartTime(LocalTime.of(23, 59));
		window.setEffectiveFrom(LocalDate.now().minusYears(1));
		serviceWindowService.create(window);

		// 菜品 + 当日已发布菜单（下单的唯一取价来源）
		RecipeCreateCmd recipe = new RecipeCreateCmd();
		recipe.setName("黑椒牛柳-" + UUID.randomUUID());
		recipe.setCategory("热菜");
		recipe.setUnit("份");
		recipe.setUnitPrice(new BigDecimal("32.00"));
		RecipeVO created = recipeService.getById(recipeService.create(recipe));
		assertThat(created.getStatus()).isEqualTo(RecipeStatus.ACTIVE.name());
		recipeId = created.getRecipeId();

		MenuCreateCmd menu = new MenuCreateCmd();
		menu.setName("今日菜单");
		menu.setEffectiveDate(LocalDate.now());
		MenuCreateItem item = new MenuCreateItem();
		item.setRecipeId(recipeId);
		item.setMenuPrice(new BigDecimal("30.00"));
		menu.setItems(List.of(item));
		menuId = menuService.createDraft(menu);
		menuService.publish(menuId);
		assertThat(menuService.getCurrent(LocalDate.now())).isNotNull();
	}

	@AfterEach
	void tearDown() {
		UserContextHolder.clear();
	}

	@Test
	@DisplayName("下单：写入明细快照并按菜单价计算总价")
	void submit_writesSnapshotAndTotal() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);

		OrderVO order = orderService.submit(submitCmd(2, UUID.randomUUID().toString()));

		assertThat(order.getOrderId()).isNotNull();
		assertThat(order.getOrderNo()).startsWith(LocalDate.now().toString().replace("-", ""));
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING.name());
		assertThat(order.getEmployeeId()).isEqualTo(employeeId);
		assertThat(order.getItems()).hasSize(1);
		// 快照价取菜单级价格（30.00）而不是菜品标准价（32.00）
		assertThat(order.getItems().get(0).getRecipeName()).startsWith("黑椒牛柳-");
		assertThat(order.getItems().get(0).getCategory()).isEqualTo("热菜");
		assertThat(order.getItems().get(0).getUnit()).isEqualTo("份");
		assertThat(order.getItems().get(0).getUnitPrice()).isEqualByComparingTo("30.00");
		assertThat(order.getTotalAmount()).isEqualByComparingTo("60.00");
	}

	@Test
	@DisplayName("幂等：同一 Idempotency-Key 不重复下单")
	void submit_isIdempotent() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		String key = UUID.randomUUID().toString();

		OrderVO first = orderService.submit(submitCmd(1, key));
		OrderVO second = orderService.submit(submitCmd(1, key));

		assertThat(second.getOrderId()).isEqualTo(first.getOrderId());
		assertThat(orderFormRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("一人一天一单：第二单抛违反业务规则(42202)")
	void submit_rejectsSecondOrderOfTheDay() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		orderService.submit(submitCmd(1, UUID.randomUUID().toString()));

		assertThatThrownBy(() -> orderService.submit(submitCmd(1, UUID.randomUUID().toString())))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.RULE_VIOLATION);
	}

	@Test
	@DisplayName("缺少幂等键：抛参数校验失败(40001)")
	void submit_requiresIdempotencyKey() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);

		assertThatThrownBy(() -> orderService.submit(submitCmd(1, null)))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARAM_INVALID);
	}

	@Test
	@DisplayName("菜品不在当日菜单内：抛参数校验失败(40001)")
	void submit_rejectsRecipeOutsideMenu() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		OrderSubmitCmd cmd = new OrderSubmitCmd();
		cmd.setMenuId(menuId);
		cmd.setIdempotencyKey(UUID.randomUUID().toString());
		OrderSubmitCmd.SubmitItem item = new OrderSubmitCmd.SubmitItem();
		item.setRecipeId(999_999L);
		item.setQuantity(1);
		cmd.setItems(List.of(item));

		assertThatThrownBy(() -> orderService.submit(cmd))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARAM_INVALID);
	}

	@Test
	@DisplayName("改单：整体替换明细并重算总价")
	void modify_replacesDetailsAndRecalculates() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		OrderVO created = orderService.submit(submitCmd(1, UUID.randomUUID().toString()));

		OrderModifyCmd modify = new OrderModifyCmd();
		OrderSubmitCmd.SubmitItem item = new OrderSubmitCmd.SubmitItem();
		item.setRecipeId(recipeId);
		item.setQuantity(3);
		modify.setItems(List.of(item));
		modify.setVersion(created.getVersion());
		orderService.modify(created.getOrderId(), modify);

		OrderVO updated = orderQueryService.getDetail(created.getOrderId());
		assertThat(updated.getItems()).hasSize(1);
		assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(3);
		assertThat(updated.getTotalAmount()).isEqualByComparingTo("90.00");
	}

	@Test
	@DisplayName("改单：版本号过期抛状态冲突(40902)")
	void modify_rejectsStaleVersion() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		OrderVO created = orderService.submit(submitCmd(1, UUID.randomUUID().toString()));

		OrderModifyCmd modify = new OrderModifyCmd();
		OrderSubmitCmd.SubmitItem item = new OrderSubmitCmd.SubmitItem();
		item.setRecipeId(recipeId);
		item.setQuantity(2);
		modify.setItems(List.of(item));
		modify.setVersion(created.getVersion() + 99);

		assertThatThrownBy(() -> orderService.modify(created.getOrderId(), modify))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.STATE_CONFLICT);
	}

	@Test
	@DisplayName("取消：本人可取消，状态变为 CANCELLED")
	void cancel_marksOrderCancelled() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		OrderVO created = orderService.submit(submitCmd(1, UUID.randomUUID().toString()));

		orderService.cancel(created.getOrderId(), "临时有事");

		OrderVO cancelled = orderQueryService.getDetail(created.getOrderId());
		assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED.name());
		// 取消后不再占用当日名额
		assertThat(orderService.countDailyOrders(employeeId, LocalDate.now())).isZero();
	}

	@Test
	@DisplayName("越权：员工不能查看他人订单详情(40300)")
	void getDetail_forbidsOtherEmployee() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		OrderVO created = orderService.submit(submitCmd(1, UUID.randomUUID().toString()));

		Long otherEmployeeId = user("employee002", "李四", RoleCodes.EMPLOYEE).getId();
		loginAs(otherEmployeeId, RoleCodes.EMPLOYEE);

		assertThatThrownBy(() -> orderQueryService.getDetail(created.getOrderId()))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("作废：员工无权作废(40300)，经理作废后状态 INVALID 且留痕")
	void invalidate_requiresManagerAndLeavesAuditTrail() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		OrderVO created = orderService.submit(submitCmd(1, UUID.randomUUID().toString()));

		assertThatThrownBy(() -> orderService.invalidate(created.getOrderId(), "重复下单"))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

		loginAs(managerId, RoleCodes.MANAGER);
		String auditId = orderService.invalidate(created.getOrderId(), "重复下单");
		assertThat(auditId).isNotBlank();

		OrderVO invalid = orderQueryService.getDetail(created.getOrderId());
		assertThat(invalid.getStatus()).isEqualTo(OrderStatus.INVALID.name());
		assertThat(invalid.getInvalidatedBy()).isEqualTo(managerId);
		assertThat(invalid.getInvalidateReason()).isEqualTo("重复下单");
		// 作废后不再占用当日名额
		assertThat(orderService.countDailyOrders(employeeId, LocalDate.now())).isZero();
	}

	@Test
	@DisplayName("个人历史：只返回本人订单，且默认不带他人数据")
	void pageHistory_returnsOnlyOwnOrders() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		orderService.submit(submitCmd(1, UUID.randomUUID().toString()));

		Long otherEmployeeId = user("employee003", "王五", RoleCodes.EMPLOYEE).getId();
		loginAs(otherEmployeeId, RoleCodes.EMPLOYEE);
		PageResult<OrderVO> emptyHistory = orderService.pageHistory(new OrderHistoryQuery());

		assertThat(emptyHistory.getList()).isEmpty();
		assertThat(emptyHistory.getTotal()).isZero();

		loginAs(employeeId, RoleCodes.EMPLOYEE);
		PageResult<OrderVO> history = orderService.pageHistory(new OrderHistoryQuery());
		assertThat(history.getTotal()).isEqualTo(1);
		assertThat(history.getList().get(0).getEmployeeId()).isEqualTo(employeeId);
	}

	@Test
	@DisplayName("时间窗口：截止后不能下单(42201)")
	void submit_rejectsAfterCutoff() {
		loginAs(employeeId, RoleCodes.EMPLOYEE);
		// 直接把生效中的时间窗口改成“今天 00:01 截止”，模拟已过截止时间的场景。
		// 时间窗口的唯一数据来源是 ServiceWindowService，因此必须从它读取当前生效配置。
		ServiceWindowCmd cutoff = new ServiceWindowCmd();
		cutoff.setCutoffTime(LocalTime.of(0, 1));
		cutoff.setDeliveryStartTime(LocalTime.of(0, 2));
		cutoff.setEffectiveFrom(LocalDate.now());
		Long configId = serviceWindowService.get(LocalDate.now(), null, null).getConfigId();
		assertThat(configId).isNotNull();
		serviceWindowService.update(configId, cutoff);
		assertThat(serviceWindowService.get(LocalDate.now(), null, null).isCanOrder()).isFalse();

		assertThatThrownBy(() -> orderService.submit(submitCmd(1, UUID.randomUUID().toString())))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_TIME_WINDOW);
	}

	// ------------------------------------------------------------------ 测试辅助

	private OrderSubmitCmd submitCmd(int quantity, String idempotencyKey) {
		OrderSubmitCmd cmd = new OrderSubmitCmd();
		cmd.setMenuId(menuId);
		cmd.setIdempotencyKey(idempotencyKey);
		OrderSubmitCmd.SubmitItem item = new OrderSubmitCmd.SubmitItem();
		item.setRecipeId(recipeId);
		item.setQuantity(quantity);
		cmd.setItems(List.of(item));
		return cmd;
	}

	private void loginAs(Long userId, String roleCode) {
		User user = userRepository.findWithRolesById(userId).orElseThrow();
		Set<String> permCodes = new java.util.LinkedHashSet<>();
		user.getRoles().forEach(role -> permCodes.addAll(role.permCodes()));
		UserContextHolder.set(new UserContext(user.getId(), user.getEmployeeNo(), user.getName(),
				user.getDeptId(), user.getWorkstation(), user.getPhone(),
				List.of(roleCode), List.copyOf(permCodes)));
	}

	private Permission permission(String code) {
		Permission permission = new Permission();
		permission.setPermCode(code);
		permission.setPermName(code);
		permission.setModule(code.split(":")[0]);
		return permission;
	}

	private Role role(String code, Set<Permission> permissions) {
		Role role = new Role();
		role.setRoleCode(code);
		role.setRoleName(code);
		role.setPermissions(new java.util.LinkedHashSet<>(permissions));
		return role;
	}

	private User user(String employeeNo, String name, String roleCode) {
		User user = new User();
		user.setEmployeeNo(employeeNo);
		user.setPassword(passwordEncoder.encode("123456"));
		user.setName(name);
		user.setDeptId(1L);
		user.setDeptName("研发部");
		user.setWorkstation("R-01");
		user.setPhone("13800000000");
		user.setStatus(UserStatus.ACTIVE);
		roleRepository.findByRoleCodeIgnoreCase(roleCode).ifPresent(role -> user.getRoles().add(role));
		return userRepository.save(user);
	}
}
