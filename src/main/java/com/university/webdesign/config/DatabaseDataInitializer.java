package com.university.webdesign.config;

import com.university.webdesign.reporting.data.ReportOrderEntity;
import com.university.webdesign.reporting.data.ReportOrderItemEntity;
import com.university.webdesign.reporting.repository.ReportOrderRepository;
import com.university.webdesign.reporting.service.ReportService;
import com.university.webdesign.user.data.PermissionEntity;
import com.university.webdesign.user.data.RoleEntity;
import com.university.webdesign.user.data.UserEntity;
import com.university.webdesign.user.repository.PermissionRepository;
import com.university.webdesign.user.repository.RoleRepository;
import com.university.webdesign.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首次启动时初始化权限、角色、用户及演示订单数据。
 */
@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "database", matchIfMissing = true)
public class DatabaseDataInitializer implements CommandLineRunner
{
	private final PermissionRepository permissionRepository;
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final ReportOrderRepository reportOrderRepository;
	private final PasswordEncoder passwordEncoder;
	private final ReportService reportService;

	public DatabaseDataInitializer(
		PermissionRepository permissionRepository,
		RoleRepository roleRepository,
		UserRepository userRepository,
		ReportOrderRepository reportOrderRepository,
		PasswordEncoder passwordEncoder,
		ReportService reportService) {
		this.permissionRepository = permissionRepository;
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.reportOrderRepository = reportOrderRepository;
		this.passwordEncoder = passwordEncoder;
		this.reportService = reportService;
	}

	@Override
	@Transactional
	public void run(String... args) {
		seedPermissionsAndRoles();
		seedUsers();
		seedReportOrders();
		YearMonth current = YearMonth.now();
		reportService.refreshMonthlyReport(current);
		reportService.refreshMonthlyReport(current.minusMonths(1));
	}

	private void seedPermissionsAndRoles() {
		if (permissionRepository.count() > 0 || roleRepository.count() > 0) {
			return;
		}
		Map<String, PermissionEntity> permissions = new LinkedHashMap<>();
		permissions.put("user:view", permission("user:view", "查看用户", "查看员工账号与基础信息"));
		permissions.put("user:manage", permission("user:manage", "维护用户", "新增、编辑、启停和删除员工账号"));
		permissions.put("role:manage", permission("role:manage", "管理角色", "维护角色及其权限"));
		permissions.put("report:view", permission("report:view", "查看报表", "查看餐厅月度销售总报表"));
		permissions.put("report:export", permission("report:export", "导出报表", "打印或导出财务报表"));
		permissions.put("audit:view", permission("audit:view", "消费审计", "查询员工月度消费明细"));
		permissionRepository.saveAll(permissions.values());

		roleRepository.save(role("MANAGER", "餐厅经理", "系统管理员，拥有全部权限",
			permissions.values().stream().toList()));
		roleRepository.save(role("KITCHEN_SUPERVISOR", "厨房主管", "查看订单与履约数据",
			List.of(permissions.get("report:view"))));
		roleRepository.save(role("DELIVERY_STAFF", "配餐员", "查看当日配餐任务", List.of()));
		roleRepository.save(role("FINANCE", "财务", "查看、导出财务报表并审计员工消费",
			List.of(permissions.get("user:view"), permissions.get("report:view"),
				permissions.get("report:export"), permissions.get("audit:view"))));
		roleRepository.save(role("EMPLOYEE", "员工", "企业员工基础权限", List.of()));
	}

	private void seedUsers() {
		if (userRepository.count() > 0) {
			return;
		}
		addUser("manager", "admin123", "林经理", "行政部", "A-01", "13800000001", "MANAGER");
		addUser("finance", "finance123", "周财务", "财务部", "B-03", "13800000002", "FINANCE");
		addUser("kitchen", "kitchen123", "陈主管", "后厨", "K-01", "13800000003", "KITCHEN_SUPERVISOR");
		addUser("delivery", "delivery123", "赵配餐", "配送组", "D-02", "13800000004", "DELIVERY_STAFF");
		addUser("zhangsan", "123456", "张三", "研发部", "R-12", "13800000005", "EMPLOYEE");
		addUser("lisi", "123456", "李四", "产品部", "P-08", "13800000006", "EMPLOYEE");
		addUser("wangwu", "123456", "王五", "运营部", "O-06", "13800000007", "EMPLOYEE");
		addUser("sunliu", "123456", "孙六", "研发部", "R-18", "13800000008", "EMPLOYEE");
	}

	private void seedReportOrders() {
		if (reportOrderRepository.count() > 0) {
			return;
		}
		YearMonth current = YearMonth.now();
		YearMonth previous = current.minusMonths(1);
		List<ReportOrderEntity> orders = new ArrayList<>();
		orders.add(order(1001L, userId("zhangsan"), current.atDay(2).atTime(11, 32), List.of(
			line(101L, "黑椒牛柳", "热菜", "32.00", 1),
			line(201L, "米饭", "主食", "2.00", 1),
			line(301L, "例汤", "汤品", "4.00", 1))));
		orders.add(order(1002L, userId("lisi"), current.atDay(2).atTime(11, 35), List.of(
			line(102L, "宫保鸡丁", "热菜", "26.00", 1),
			line(201L, "米饭", "主食", "2.00", 1))));
		orders.add(order(1003L, userId("wangwu"), current.atDay(3).atTime(11, 45), List.of(
			line(103L, "香煎鳕鱼", "热菜", "36.00", 1),
			line(202L, "杂粮饭", "主食", "3.00", 1),
			line(302L, "酸奶", "饮品", "6.00", 1))));
		orders.add(order(1004L, userId("sunliu"), current.atDay(3).atTime(11, 48), List.of(
			line(104L, "青椒肉丝", "热菜", "22.00", 1),
			line(201L, "米饭", "主食", "2.00", 1))));
		orders.add(order(1005L, userId("zhangsan"), current.atDay(4).atTime(11, 30), List.of(
			line(103L, "香煎鳕鱼", "热菜", "36.00", 1),
			line(202L, "杂粮饭", "主食", "3.00", 1))));
		orders.add(order(1006L, userId("finance"), current.atDay(4).atTime(11, 52), List.of(
			line(101L, "黑椒牛柳", "热菜", "32.00", 1),
			line(301L, "例汤", "汤品", "4.00", 1))));
		orders.add(order(1007L, userId("kitchen"), current.atDay(5).atTime(11, 33), List.of(
			line(102L, "宫保鸡丁", "热菜", "26.00", 1),
			line(201L, "米饭", "主食", "2.00", 1))));
		orders.add(order(1101L, userId("zhangsan"), previous.atDay(8).atTime(11, 38), List.of(
			line(101L, "黑椒牛柳", "热菜", "32.00", 1),
			line(201L, "米饭", "主食", "2.00", 1))));
		orders.add(order(1102L, userId("lisi"), previous.atDay(9).atTime(11, 42), List.of(
			line(103L, "香煎鳕鱼", "热菜", "36.00", 1),
			line(202L, "杂粮饭", "主食", "3.00", 1),
			line(302L, "酸奶", "饮品", "6.00", 1))));
		orders.add(order(1103L, userId("wangwu"), previous.atDay(10).atTime(11, 31), List.of(
			line(104L, "青椒肉丝", "热菜", "22.00", 1),
			line(201L, "米饭", "主食", "2.00", 1),
			line(301L, "例汤", "汤品", "4.00", 1))));
		reportOrderRepository.saveAll(orders);
	}

	private PermissionEntity permission(String code, String name, String description) {
		PermissionEntity permission = new PermissionEntity();
		permission.setCode(code);
		permission.setName(name);
		permission.setDescription(description);
		return permission;
	}

	private RoleEntity role(String code, String name, String description, List<PermissionEntity> permissions) {
		RoleEntity role = new RoleEntity();
		role.setCode(code);
		role.setName(name);
		role.setDescription(description);
		role.getPermissions().addAll(permissions);
		return role;
	}

	private void addUser(
		String username,
		String password,
		String name,
		String department,
		String workstation,
		String phone,
		String roleCode) {
		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(password));
		user.setName(name);
		user.setDepartment(department);
		user.setWorkstation(workstation);
		user.setPhone(phone);
		user.setEnabled(true);
		roleRepository.findByCodeIgnoreCase(roleCode).ifPresent(role -> user.getRoles().add(role));
		userRepository.save(user);
	}

	private Long userId(String username) {
		return userRepository.findWithRolesByUsernameIgnoreCase(username)
			.orElseThrow()
			.getId();
	}

	private ReportOrderEntity order(Long id, Long userId, LocalDateTime time, List<ReportOrderItemEntity> lines) {
		ReportOrderEntity order = new ReportOrderEntity();
		order.setId(id);
		order.setUserId(userId);
		order.setCreatedAt(time);
		order.setTotalAmount(lines.stream()
			.map(ReportOrderItemEntity::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add));
		for (ReportOrderItemEntity line : lines) {
			line.setOrder(order);
			order.getItems().add(line);
		}
		return order;
	}

	private ReportOrderItemEntity line(
		Long recipeId,
		String recipeName,
		String category,
		String unitPrice,
		int quantity) {
		ReportOrderItemEntity line = new ReportOrderItemEntity();
		line.setRecipeId(recipeId);
		line.setRecipeName(recipeName);
		line.setCategory(category);
		line.setUnitPrice(new BigDecimal(unitPrice));
		line.setQuantity(quantity);
		line.setAmount(line.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
		return line;
	}
}
