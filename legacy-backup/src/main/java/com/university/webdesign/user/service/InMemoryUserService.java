package com.university.webdesign.user.service;

import com.university.webdesign.user.api.LoginData;
import com.university.webdesign.user.api.PasswordUpdateData;
import com.university.webdesign.user.api.RoleDTO;
import com.university.webdesign.user.api.UserDTO;
import com.university.webdesign.user.api.UserImportData;
import com.university.webdesign.user.api.UserImportErrorDTO;
import com.university.webdesign.user.api.UserImportResultDTO;
import com.university.webdesign.user.api.UserQueryData;
import com.university.webdesign.user.api.UserRegisterData;
import com.university.webdesign.user.api.UserUpdateData;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存数据的用户、认证和授权服务。
 */
@Service
public class InMemoryUserService implements UserService
{
	private final RoleService roleService;
	private final Map<Long, UserAccount> users = new LinkedHashMap<>();
	private final AtomicLong userIdGenerator = new AtomicLong(20);

	/**
	 * 注入角色服务，用于把角色ID展开为角色编码和权限编码。
	 */
	public InMemoryUserService(RoleService roleService) {
		this.roleService = roleService;
		seedUsers();
	}

	@Override
	public synchronized UserDTO login(LoginData loginData) {
		if (loginData == null || isBlank(loginData.getUsername()) || isBlank(loginData.getPassword())) {
			throw new IllegalArgumentException("请输入用户名和密码");
		}
		UserAccount account = findByUsername(loginData.getUsername());
		if (account == null || !Objects.equals(account.password, loginData.getPassword())) {
			throw new IllegalArgumentException("用户名或密码错误");
		}
		if (!Boolean.TRUE.equals(account.user.getEnabled())) {
			throw new IllegalArgumentException("账号已停用，请联系管理员");
		}
		return copyUser(account.user);
	}

	@Override
	public void logout() {
		// 会话由控制层负责清理。
	}

	@Override
	public synchronized UserDTO createUser(UserRegisterData registerData) {
		if (registerData == null || isBlank(registerData.getUsername()) || isBlank(registerData.getName())) {
			throw new IllegalArgumentException("用户名和姓名不能为空");
		}
		if (findByUsername(registerData.getUsername()) != null) {
			throw new IllegalArgumentException("用户名已存在");
		}
		Long userId = userIdGenerator.incrementAndGet();
		UserDTO user = new UserDTO();
		user.setUserId(userId);
		user.setUsername(registerData.getUsername().trim());
		user.setName(registerData.getName().trim());
		user.setDepartment(normalize(registerData.getDepartment()));
		user.setWorkstation(normalize(registerData.getWorkstation()));
		user.setPhone(normalize(registerData.getPhone()));
		user.setEnabled(true);
		user.setRoleIds(normalizeRoleIds(registerData.getRoleIds()));
		String password = isBlank(registerData.getPassword()) ? "123456" : registerData.getPassword();
		users.put(userId, new UserAccount(user, password));
		return enrich(copyUser(user));
	}

	@Override
	public synchronized List<UserDTO> query(UserQueryData queryData) {
		UserQueryData query = queryData == null ? new UserQueryData() : queryData;
		String keyword = normalize(query.getKeyword()).toLowerCase(Locale.ROOT);
		return users.values().stream()
			.map(account -> account.user)
			.filter(user -> keyword.isBlank()
				|| contains(user.getUsername(), keyword)
				|| contains(user.getName(), keyword)
				|| contains(user.getWorkstation(), keyword)
				|| contains(user.getPhone(), keyword))
			.filter(user -> isBlank(query.getDepartment())
				|| Objects.equals(user.getDepartment(), query.getDepartment()))
			.filter(user -> query.getRoleId() == null || user.getRoleIds().contains(query.getRoleId()))
			.filter(user -> query.getEnabled() == null || Objects.equals(user.getEnabled(), query.getEnabled()))
			.sorted(Comparator.comparing(UserDTO::getUserId))
			.map(this::copyUser)
			.map(this::enrich)
			.toList();
	}

	@Override
	public synchronized UserDTO getUserInfo(Long userId) {
		return enrich(copyUser(requireAccount(userId).user));
	}

	@Override
	public synchronized UserDTO updateUser(UserUpdateData updateData) {
		if (updateData == null || updateData.getUserId() == null) {
			throw new IllegalArgumentException("用户ID不能为空");
		}
		UserAccount account = requireAccount(updateData.getUserId());
		if (!isBlank(updateData.getName())) {
			account.user.setName(updateData.getName().trim());
		}
		account.user.setDepartment(normalize(updateData.getDepartment()));
		account.user.setWorkstation(normalize(updateData.getWorkstation()));
		account.user.setPhone(normalize(updateData.getPhone()));
		if (updateData.getRoleIds() != null) {
			account.user.setRoleIds(normalizeRoleIds(updateData.getRoleIds()));
		}
		return enrich(copyUser(account.user));
	}

	@Override
	public synchronized void changePassword(PasswordUpdateData passwordUpdateData) {
		if (passwordUpdateData == null || passwordUpdateData.getUserId() == null) {
			throw new IllegalArgumentException("用户ID不能为空");
		}
		if (isBlank(passwordUpdateData.getNewPassword()) || passwordUpdateData.getNewPassword().length() < 6) {
			throw new IllegalArgumentException("新密码长度不能少于6位");
		}
		UserAccount account = requireAccount(passwordUpdateData.getUserId());
		if (!Objects.equals(account.password, passwordUpdateData.getCurrentPassword())) {
			throw new IllegalArgumentException("当前密码错误");
		}
		account.password = passwordUpdateData.getNewPassword();
	}

	@Override
	public synchronized void resetPassword(Long userId, String newPassword) {
		if (isBlank(newPassword) || newPassword.length() < 6) {
			throw new IllegalArgumentException("新密码长度不能少于6位");
		}
		requireAccount(userId).password = newPassword;
	}

	@Override
	public synchronized void setEnabled(Long userId, boolean enabled) {
		if (Objects.equals(userId, 1L) && !enabled) {
			throw new IllegalArgumentException("系统管理员不能停用");
		}
		requireAccount(userId).user.setEnabled(enabled);
	}

	@Override
	public synchronized void assignRoles(Long userId, List<Long> roleIds) {
		requireAccount(userId).user.setRoleIds(normalizeRoleIds(roleIds));
	}

	@Override
	public synchronized void deleteUser(Long userId) {
		if (Objects.equals(userId, 1L)) {
			throw new IllegalArgumentException("系统管理员不能删除");
		}
		if (users.remove(userId) == null) {
			throw new IllegalArgumentException("用户不存在");
		}
	}

	@Override
	public synchronized UserImportResultDTO importUsers(UserImportData importData) {
		// 少量错误不终止整个文件：逐行导入，并把失败原因写回结果。
		List<List<String>> rows;
		try {
			rows = UserImportParser.readRows(importData);
		} catch (IOException exception) {
			throw new IllegalArgumentException(exception.getMessage());
		}
		if (rows.isEmpty()) {
			throw new IllegalArgumentException("导入文件中没有数据");
		}

		// 有表头时按列名定位，无表头时按约定的固定顺序读取。
		boolean hasHeader = isHeader(rows.get(0));
		Map<String, Integer> columns = hasHeader ? headerColumns(rows.get(0)) : defaultColumns();
		int firstDataRow = hasHeader ? 1 : 0;
		int successCount = 0;
		List<UserImportErrorDTO> errors = new ArrayList<>();
		for (int index = firstDataRow; index < rows.size(); index++) {
			List<String> row = rows.get(index);
			if (row.stream().allMatch(String::isBlank)) {
				continue;
			}
			try {
				UserRegisterData registerData = toRegisterData(row, columns);
				createUser(registerData);
				successCount++;
			} catch (IllegalArgumentException exception) {
				UserImportErrorDTO error = new UserImportErrorDTO();
				error.setRowNumber(index + 1);
				error.setMessage(exception.getMessage());
				errors.add(error);
			}
		}
		int totalCount = rows.size() - firstDataRow;
		UserImportResultDTO result = new UserImportResultDTO();
		result.setTotalCount(totalCount);
		result.setSuccessCount(successCount);
		result.setFailureCount(totalCount - successCount);
		result.setErrors(errors);
		return result;
	}

	@Override
	public synchronized List<String> getPermissionCodes(Long userId) {
		UserDTO user = enrich(copyUser(requireAccount(userId).user));
		return user.getPermissionCodes();
	}

	@Override
	public synchronized boolean hasPermission(Long userId, String permissionCode) {
		return !isBlank(permissionCode) && getPermissionCodes(userId).contains(permissionCode);
	}

	private UserRegisterData toRegisterData(List<String> row, Map<String, Integer> columns) {
		UserRegisterData data = new UserRegisterData();
		data.setUsername(cell(row, columns, "username"));
		data.setPassword(cell(row, columns, "password"));
		data.setName(cell(row, columns, "name"));
		data.setDepartment(cell(row, columns, "department"));
		data.setWorkstation(cell(row, columns, "workstation"));
		data.setPhone(cell(row, columns, "phone"));
		String roleCodes = cell(row, columns, "roles");
		if (!roleCodes.isBlank()) {
			data.setRoleIds(roleIdsByCodes(roleCodes));
		}
		return data;
	}

	private boolean isHeader(List<String> row) {
		return row.stream().map(this::normalize).anyMatch(value -> {
			String lower = value.toLowerCase(Locale.ROOT);
			return lower.equals("username") || value.equals("用户名") || value.equals("登录名");
		});
	}

	private Map<String, Integer> headerColumns(List<String> header) {
		// 同时兼容英文列名和常见中文列名。
		Map<String, Integer> columns = new LinkedHashMap<>();
		for (int index = 0; index < header.size(); index++) {
			String value = normalize(header.get(index)).toLowerCase(Locale.ROOT);
			switch (value) {
				case "username", "用户名", "登录名" -> columns.put("username", index);
				case "password", "密码", "初始密码" -> columns.put("password", index);
				case "name", "姓名" -> columns.put("name", index);
				case "department", "部门" -> columns.put("department", index);
				case "workstation", "工位" -> columns.put("workstation", index);
				case "phone", "电话", "手机号" -> columns.put("phone", index);
				case "roles", "role", "角色", "角色编码" -> columns.put("roles", index);
				default -> {
				}
			}
		}
		if (!columns.containsKey("username") || !columns.containsKey("name")) {
			throw new IllegalArgumentException("表头必须包含用户名和姓名");
		}
		return columns;
	}

	private Map<String, Integer> defaultColumns() {
		Map<String, Integer> columns = new LinkedHashMap<>();
		columns.put("username", 0);
		columns.put("password", 1);
		columns.put("name", 2);
		columns.put("department", 3);
		columns.put("workstation", 4);
		columns.put("phone", 5);
		columns.put("roles", 6);
		return columns;
	}

	private String cell(List<String> row, Map<String, Integer> columns, String key) {
		Integer index = columns.get(key);
		return index == null || index >= row.size() ? "" : normalize(row.get(index));
	}

	private List<Long> roleIdsByCodes(String roleCodes) {
		// 多个角色编码允许使用中英文逗号、分号或竖线分隔。
		Set<Long> roleIds = new LinkedHashSet<>();
		for (String code : roleCodes.split("[,，;；|]")) {
			String normalized = code.trim().toUpperCase(Locale.ROOT);
			if (normalized.isBlank()) {
				continue;
			}
			roleService.query().stream()
				.filter(role -> role.getCode().equalsIgnoreCase(normalized))
				.map(RoleDTO::getRoleId)
				.findFirst()
				.ifPresent(roleIds::add);
		}
		if (roleIds.isEmpty()) {
			roleIds.add(5L);
		}
		return new ArrayList<>(roleIds);
	}

	private UserAccount requireAccount(Long userId) {
		if (userId == null || !users.containsKey(userId)) {
			throw new IllegalArgumentException("用户不存在");
		}
		return users.get(userId);
	}

	private UserAccount findByUsername(String username) {
		if (isBlank(username)) {
			return null;
		}
		return users.values().stream()
			.filter(account -> account.user.getUsername().equalsIgnoreCase(username.trim()))
			.findFirst()
			.orElse(null);
	}

	private UserDTO enrich(UserDTO user) {
		// 对外DTO只暴露角色和权限编码，不直接暴露角色实体。
		List<Long> roleIds = normalizeRoleIds(user.getRoleIds());
		user.setRoleIds(roleIds);
		List<String> roleCodes = new ArrayList<>();
		Set<String> permissionCodes = new LinkedHashSet<>();
		for (Long roleId : roleIds) {
			try {
				RoleDTO role = roleService.getRole(roleId);
				roleCodes.add(role.getCode());
				if (role.getPermissionCodes() != null) {
					permissionCodes.addAll(role.getPermissionCodes());
				}
			} catch (IllegalArgumentException ignored) {
				// 已删除角色的历史用户仍可展示，但不再授予对应权限。
			}
		}
		user.setRoleCodes(roleCodes);
		user.setPermissionCodes(new ArrayList<>(permissionCodes));
		return user;
	}

	private List<Long> normalizeRoleIds(List<Long> roleIds) {
		List<Long> normalized = roleIds == null
			? new ArrayList<>()
			: roleIds.stream().filter(Objects::nonNull).distinct().toList();
		return normalized.isEmpty() ? new ArrayList<>(List.of(5L)) : new ArrayList<>(normalized);
	}

	private UserDTO copyUser(UserDTO source) {
		UserDTO copy = new UserDTO();
		copy.setUserId(source.getUserId());
		copy.setUsername(source.getUsername());
		copy.setName(source.getName());
		copy.setDepartment(source.getDepartment());
		copy.setWorkstation(source.getWorkstation());
		copy.setPhone(source.getPhone());
		copy.setEnabled(source.getEnabled());
		copy.setRoleIds(source.getRoleIds() == null ? new ArrayList<>() : new ArrayList<>(source.getRoleIds()));
		copy.setRoleCodes(source.getRoleCodes() == null ? new ArrayList<>() : new ArrayList<>(source.getRoleCodes()));
		copy.setPermissionCodes(source.getPermissionCodes() == null
			? new ArrayList<>()
			: new ArrayList<>(source.getPermissionCodes()));
		return copy;
	}

	private void seedUsers() {
		// 演示账号覆盖经理、财务、后厨、配送和普通员工。
		addUser(1L, "manager", "admin123", "林经理", "行政部", "A-01", "13800000001", List.of(1L));
		addUser(2L, "finance", "finance123", "周财务", "财务部", "B-03", "13800000002", List.of(4L));
		addUser(3L, "kitchen", "kitchen123", "陈主管", "后厨", "K-01", "13800000003", List.of(2L));
		addUser(4L, "delivery", "delivery123", "赵配餐", "配送组", "D-02", "13800000004", List.of(3L));
		addUser(5L, "zhangsan", "123456", "张三", "研发部", "R-12", "13800000005", List.of(5L));
		addUser(6L, "lisi", "123456", "李四", "产品部", "P-08", "13800000006", List.of(5L));
		addUser(7L, "wangwu", "123456", "王五", "运营部", "O-06", "13800000007", List.of(5L));
		addUser(8L, "sunliu", "123456", "孙六", "研发部", "R-18", "13800000008", List.of(5L));
	}

	private void addUser(Long id, String username, String password, String name, String department,
		String workstation, String phone, List<Long> roleIds) {
		UserDTO user = new UserDTO();
		user.setUserId(id);
		user.setUsername(username);
		user.setName(name);
		user.setDepartment(department);
		user.setWorkstation(workstation);
		user.setPhone(phone);
		user.setEnabled(true);
		user.setRoleIds(roleIds);
		users.put(id, new UserAccount(user, password));
	}

	private boolean contains(String value, String keyword) {
		return normalize(value).toLowerCase(Locale.ROOT).contains(keyword);
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static final class UserAccount
	{
		private final UserDTO user;
		private String password;

		private UserAccount(UserDTO user, String password) {
			this.user = user;
			this.password = password;
		}
	}
}
