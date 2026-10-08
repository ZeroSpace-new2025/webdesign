package com.university.webdesign.config;

import com.university.webdesign.common.PermCodes;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.domain.menu.RecipeStatus;
import com.university.webdesign.domain.user.Permission;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.domain.user.User;
import com.university.webdesign.domain.user.UserStatus;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首次启动时的演示数据初始化。
 * <p>
 * 按《重构实施规范》第 8 节与 `AGENTS.md` 的约定，本类负责把“系统能跑起来”所需的最小数据落库：
 * <ol>
 *     <li><b>权限点</b>：以 {@link PermCodes} 为权威列表写入 {@code permissions}；</li>
 *     <li><b>预置角色</b>：{@link RoleCodes} 的五类角色并授予对应权限；</li>
 *     <li><b>演示账号</b>：经理 / 财务 / 厨房主管 / 配餐员 / 员工，密码统一 BCrypt 加密；</li>
 *     <li><b>时间窗口</b>：默认 09:00 / 11:30；</li>
 *     <li><b>演示菜品与当日已发布菜单</b>：让点餐页与下单链路开箱可用。</li>
 * </ol>
 * 幂等：每段数据都有存在性判断，重复启动不会写重复数据。
 * 只在 {@code app.storage=database}（默认）时生效，测试环境可用该属性关闭。
 * <p>
 * 重构前本类还写入了 `reporting` 包的演示订单，依赖的是已删除的旧包与新表中并不存在的
 * `ReportOrderEntity`；报表改为经 {@code OrderQueryService} 实时取数后，这里不再造订单数据。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "database", matchIfMissing = true)
public class DatabaseDataInitializer implements CommandLineRunner
{
	private final PermissionRepository permissionRepository;
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RecipeService recipeService;
	private final MenuService menuService;
	private final ServiceWindowService serviceWindowService;

	public DatabaseDataInitializer(
			PermissionRepository permissionRepository,
			RoleRepository roleRepository,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			RecipeService recipeService,
			MenuService menuService,
			ServiceWindowService serviceWindowService) {
		this.permissionRepository = permissionRepository;
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.recipeService = recipeService;
		this.menuService = menuService;
		this.serviceWindowService = serviceWindowService;
	}

	@Override
	@Transactional
	public void run(String... args) {
		seedPermissions();
		seedRoles();
		seedUsers();
		seedServiceWindow();
		seedMenu();
	}

	/**
	 * 写入权限点字典（对应 `permissions` 表）
	 */
	private void seedPermissions() {
		if (permissionRepository.count() > 0) {
			return;
		}
		List<Permission> permissions = new ArrayList<>();
		for (String code : PermCodes.ALL) {
			Permission permission = new Permission();
			permission.setPermCode(code);
			permission.setPermName(permissionName(code));
			permission.setModule(code.split(":")[0]);
			permission.setDescription(permissionName(code));
			permissions.add(permission);
		}
		permissionRepository.saveAll(permissions);
		log.info("已初始化 {} 个权限点", permissions.size());
	}

	/**
	 * 写入五类预置角色并授权
	 */
	private void seedRoles() {
		if (roleRepository.count() > 0) {
			return;
		}
		roleRepository.save(role(RoleCodes.MANAGER, "餐厅经理", "系统管理员，拥有全部权限", PermCodes.ALL));
		roleRepository.save(role(RoleCodes.KITCHEN_SUPERVISOR, "厨房主管", "查看订单、聚合总括订单与打印生产单",
				new String[] {PermCodes.ORDER_VIEW_ALL, PermCodes.OPERATION_AGGREGATE,
						PermCodes.OPERATION_DELIVERY_PRINT, PermCodes.REPORT_VIEW}));
		roleRepository.save(role(RoleCodes.DELIVERY_STAFF, "配餐员", "生成与打印配送单、更新配送状态",
				new String[] {PermCodes.OPERATION_DELIVERY_PRINT}));
		roleRepository.save(role(RoleCodes.FINANCE, "财务管理", "查看与导出报表、审计员工消费",
				new String[] {PermCodes.ORDER_VIEW_ALL, PermCodes.REPORT_VIEW,
						PermCodes.REPORT_EXPORT, PermCodes.AUDIT_VIEW}));
		roleRepository.save(role(RoleCodes.EMPLOYEE, "企业员工", "企业员工基础权限：点餐与查看自己的订单",
				new String[] {PermCodes.ORDER_SUBMIT}));
		log.info("已初始化 {} 个预置角色", roleRepository.count());
	}

	/**
	 * 写入演示账号（工号 / 密码 / 姓名 / 部门 / 工位 / 电话 / 角色）
	 */
	private void seedUsers() {
		if (userRepository.count() > 0) {
			return;
		}
		addUser("manager", "admin123", "林经理", 1L, "行政部", "A-01", "13800000001", RoleCodes.MANAGER);
		addUser("finance", "finance123", "周财务", 2L, "财务部", "B-03", "13800000002", RoleCodes.FINANCE);
		addUser("kitchen", "kitchen123", "陈主管", 3L, "后厨", "K-01", "13800000003", RoleCodes.KITCHEN_SUPERVISOR);
		addUser("delivery", "delivery123", "赵配餐", 4L, "配送组", "D-02", "13800000004", RoleCodes.DELIVERY_STAFF);
		addUser("zhangsan", "123456", "张三", 5L, "研发部", "R-12", "13800000005", RoleCodes.EMPLOYEE);
		addUser("lisi", "123456", "李四", 5L, "研发部", "R-08", "13800000006", RoleCodes.EMPLOYEE);
		addUser("wangwu", "123456", "王五", 6L, "运营部", "O-06", "13800000007", RoleCodes.EMPLOYEE);
		addUser("sunliu", "123456", "孙六", 6L, "运营部", "O-18", "13800000008", RoleCodes.EMPLOYEE);
		log.info("已初始化 {} 个演示账号：manager / finance / kitchen / delivery / zhangsan / lisi / wangwu / sunliu",
				userRepository.count());
	}

	/**
	 * 写入默认时间窗口（09:00 截止 / 11:30 开始配餐）
	 */
	private void seedServiceWindow() {
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(LocalTime.of(9, 0));
		cmd.setDeliveryStartTime(LocalTime.of(11, 30));
		cmd.setEffectiveFrom(LocalDate.now().minusYears(1));
		serviceWindowService.create(cmd);
	}

	/**
	 * 写入演示菜品并发布当日菜单，让点餐页与下单链路开箱可用
	 */
	private void seedMenu() {
		if (menuService.getCurrent(LocalDate.now()) != null) {
			return;
		}
		Map<String, String[]> dishes = new LinkedHashMap<>();
		dishes.put("黑椒牛柳", new String[] {"热菜", "份", "32.00"});
		dishes.put("宫保鸡丁", new String[] {"热菜", "份", "26.00"});
		dishes.put("香煎鳕鱼", new String[] {"热菜", "份", "36.00"});
		dishes.put("青椒肉丝", new String[] {"热菜", "份", "22.00"});
		dishes.put("米饭", new String[] {"主食", "份", "2.00"});
		dishes.put("杂粮饭", new String[] {"主食", "份", "3.00"});
		dishes.put("例汤", new String[] {"汤品", "份", "4.00"});
		dishes.put("酸奶", new String[] {"饮品", "份", "6.00"});

		MenuCreateCmd menu = new MenuCreateCmd();
		menu.setName("今日菜单 " + LocalDate.now());
		menu.setDescription("系统初始化的演示菜单，可直接用于点餐与下单联调");
		menu.setEffectiveDate(LocalDate.now());
		List<MenuCreateItem> items = new ArrayList<>();
		for (Map.Entry<String, String[]> entry : dishes.entrySet()) {
			String[] meta = entry.getValue();
			RecipeCreateCmd recipe = new RecipeCreateCmd();
			recipe.setName(entry.getKey());
			recipe.setCategory(meta[0]);
			recipe.setUnit(meta[1]);
			recipe.setUnitPrice(new BigDecimal(meta[2]));
			recipe.setDescription("演示菜品：" + entry.getKey());
			Long recipeId = recipeService.create(recipe);
			RecipeVO saved = recipeService.getById(recipeId);
			if (!RecipeStatus.ACTIVE.name().equals(saved.getStatus())) {
				continue;
			}
			MenuCreateItem item = new MenuCreateItem();
			item.setRecipeId(recipeId);
			items.add(item);
		}
		menu.setItems(items);
		Long menuId = menuService.createDraft(menu);
		menuService.publish(menuId);
		log.info("已初始化演示菜单（菜单ID={}，菜品 {} 道）", menuId, items.size());
	}

	/**
	 * 构造角色实体并挂权限
	 *
	 * @param code        角色编码
	 * @param name        角色名称
	 * @param description 角色描述
	 * @param permCodes   权限点编码
	 * @return 角色实体
	 */
	private Role role(String code, String name, String description, String[] permCodes) {
		Role role = new Role();
		role.setRoleCode(code);
		role.setRoleName(name);
		role.setDescription(description);
		for (String permCode : permCodes) {
			permissionRepository.findByPermCode(permCode).ifPresent(role.getPermissions()::add);
		}
		return role;
	}

	/**
	 * 写入单个演示账号
	 *
	 * @param employeeNo  工号
	 * @param rawPassword 初始密码（明文，落库前 BCrypt）
	 * @param name        姓名
	 * @param deptId      部门ID
	 * @param deptName    部门名称
	 * @param workstation 工位
	 * @param phone       联系电话
	 * @param roleCode    角色编码
	 */
	private void addUser(String employeeNo, String rawPassword, String name, Long deptId, String deptName,
			String workstation, String phone, String roleCode) {
		if (userRepository.existsByEmployeeNoIgnoreCase(employeeNo)) {
			return;
		}
		User user = new User();
		user.setEmployeeNo(employeeNo);
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setName(name);
		user.setDeptId(deptId);
		user.setDeptName(deptName);
		user.setWorkstation(workstation);
		user.setPhone(phone);
		user.setStatus(UserStatus.ACTIVE);
		roleRepository.findByRoleCodeIgnoreCase(roleCode).ifPresent(role -> user.getRoles().add(role));
		userRepository.save(user);
	}

	/**
	 * 权限点编码 → 中文名称（权限树展示用）
	 *
	 * @param code 权限点编码
	 * @return 中文名称；未登记的编码回退为编码本身
	 */
	private String permissionName(String code) {
		return switch (code) {
			case PermCodes.MENU_RECIPE_MANAGE -> "菜品维护";
			case PermCodes.MENU_MENU_MANAGE -> "菜单维护";
			case PermCodes.ORDER_SUBMIT -> "提交订单";
			case PermCodes.ORDER_INVALIDATE -> "作废违规订单";
			case PermCodes.ORDER_VIEW_ALL -> "查看全部订单";
			case PermCodes.OPERATION_AGGREGATE -> "聚合总括订单";
			case PermCodes.OPERATION_DELIVERY_PRINT -> "配送单打印";
			case PermCodes.OPERATION_WINDOW_MANAGE -> "维护时间窗口";
			case PermCodes.USER_MANAGE -> "员工账号维护";
			case PermCodes.ROLE_MANAGE -> "角色权限维护";
			case PermCodes.REPORT_VIEW -> "查看报表";
			case PermCodes.REPORT_EXPORT -> "导出报表";
			case PermCodes.AUDIT_VIEW -> "消费审计";
			default -> code;
		};
	}
}
