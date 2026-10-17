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
import com.university.webdesign.user.data.PermissionEntity;
import com.university.webdesign.user.data.RoleEntity;
import com.university.webdesign.user.data.UserEntity;
import com.university.webdesign.user.repository.RoleRepository;
import com.university.webdesign.user.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 使用JPA持久化用户、账号状态和用户角色关系。
 */
@Service
@ConditionalOnProperty(name = "app.storage", havingValue = "database", matchIfMissing = true)
public class JpaUserService implements UserService
{
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public JpaUserService(
		UserRepository userRepository,
		RoleRepository roleRepository,
		PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDTO login(LoginData loginData) {
		if (loginData == null || isBlank(loginData.getUsername()) || isBlank(loginData.getPassword())) {
			throw new IllegalArgumentException("请输入用户名和密码");
		}
		UserEntity user = userRepository.findWithRolesByUsernameIgnoreCase(loginData.getUsername().trim())
			.orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
		if (!passwordEncoder.matches(loginData.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("用户名或密码错误");
		}
		if (!Boolean.TRUE.equals(user.getEnabled())) {
			throw new IllegalArgumentException("账号已停用，请联系管理员");
		}
		return toDto(user);
	}

	@Override
	public void logout() {
		// 会话由控制层负责清理。
	}

	@Override
	@Transactional
	public UserDTO createUser(UserRegisterData registerData) {
		if (registerData == null || isBlank(registerData.getUsername()) || isBlank(registerData.getName())) {
			throw new IllegalArgumentException("用户名和姓名不能为空");
		}
		String username = registerData.getUsername().trim();
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw new IllegalArgumentException("用户名已存在");
		}
		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(isBlank(registerData.getPassword())
			? "123456"
			: registerData.getPassword()));
		applyProfile(user, registerData.getName(), registerData.getDepartment(),
			registerData.getWorkstation(), registerData.getPhone());
		user.setEnabled(true);
		user.setRoles(resolveRoles(registerData.getRoleIds()));
		return toDto(userRepository.save(user));
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserDTO> query(UserQueryData queryData) {
		UserQueryData query = queryData == null ? new UserQueryData() : queryData;
		String keyword = blankToNull(query.getKeyword());
		String department = blankToNull(query.getDepartment());
		return userRepository.search(keyword, department, query.getRoleId(), query.getEnabled()).stream()
			.map(this::toDto)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public UserDTO getUserInfo(Long userId) {
		return toDto(requireUser(userId));
	}

	@Override
	@Transactional
	public UserDTO updateUser(UserUpdateData updateData) {
		if (updateData == null || updateData.getUserId() == null) {
			throw new IllegalArgumentException("用户ID不能为空");
		}
		UserEntity user = requireUser(updateData.getUserId());
		String name = isBlank(updateData.getName()) ? user.getName() : updateData.getName().trim();
		applyProfile(user, name, updateData.getDepartment(), updateData.getWorkstation(), updateData.getPhone());
		if (updateData.getRoleIds() != null) {
			user.setRoles(resolveRoles(updateData.getRoleIds()));
		}
		return toDto(userRepository.save(user));
	}

	@Override
	@Transactional
	public void changePassword(PasswordUpdateData passwordUpdateData) {
		if (passwordUpdateData == null || passwordUpdateData.getUserId() == null) {
			throw new IllegalArgumentException("用户ID不能为空");
		}
		validateNewPassword(passwordUpdateData.getNewPassword());
		UserEntity user = requireUser(passwordUpdateData.getUserId());
		if (!passwordEncoder.matches(passwordUpdateData.getCurrentPassword(), user.getPassword())) {
			throw new IllegalArgumentException("当前密码错误");
		}
		user.setPassword(passwordEncoder.encode(passwordUpdateData.getNewPassword()));
		userRepository.save(user);
	}

	@Override
	@Transactional
	public void resetPassword(Long userId, String newPassword) {
		validateNewPassword(newPassword);
		UserEntity user = requireUser(userId);
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

	@Override
	@Transactional
	public void setEnabled(Long userId, boolean enabled) {
		UserEntity user = requireUser(userId);
		if ("manager".equalsIgnoreCase(user.getUsername()) && !enabled) {
			throw new IllegalArgumentException("系统管理员不能停用");
		}
		user.setEnabled(enabled);
		userRepository.save(user);
	}

	@Override
	@Transactional
	public void assignRoles(Long userId, List<Long> roleIds) {
		UserEntity user = requireUser(userId);
		user.setRoles(resolveRoles(roleIds));
		userRepository.save(user);
	}

	@Override
	@Transactional
	public void deleteUser(Long userId) {
		UserEntity user = requireUser(userId);
		if ("manager".equalsIgnoreCase(user.getUsername())) {
			throw new IllegalArgumentException("系统管理员不能删除");
		}
		userRepository.delete(user);
	}

	@Override
	@Transactional
	public UserImportResultDTO importUsers(UserImportData importData) {
		List<List<String>> rows;
		try {
			rows = UserImportParser.readRows(importData);
		} catch (IOException exception) {
			throw new IllegalArgumentException(exception.getMessage());
		}
		if (rows.isEmpty()) {
			throw new IllegalArgumentException("导入文件中没有数据");
		}
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
				createUser(toRegisterData(row, columns));
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
	@Transactional(readOnly = true)
	public List<String> getPermissionCodes(Long userId) {
		return toDto(requireUser(userId)).getPermissionCodes();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasPermission(Long userId, String permissionCode) {
		return !isBlank(permissionCode) && getPermissionCodes(userId).contains(permissionCode);
	}

	private UserEntity requireUser(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("用户不存在");
		}
		return userRepository.findWithRolesById(userId)
			.orElseThrow(() -> new IllegalArgumentException("用户不存在"));
	}

	private Set<RoleEntity> resolveRoles(List<Long> roleIds) {
		Set<Long> ids = new LinkedHashSet<>();
		if (roleIds != null) {
			roleIds.stream().filter(Objects::nonNull).forEach(ids::add);
		}
		if (ids.isEmpty()) {
			roleRepository.findByCodeIgnoreCase("EMPLOYEE").ifPresent(role -> ids.add(role.getId()));
		}
		return new LinkedHashSet<>(roleRepository.findAllById(ids));
	}

	private void applyProfile(UserEntity user, String name, String department, String workstation, String phone) {
		user.setName(name == null ? "" : name.trim());
		user.setDepartment(normalize(department));
		user.setWorkstation(normalize(workstation));
		user.setPhone(normalize(phone));
	}

	private void validateNewPassword(String password) {
		if (isBlank(password) || password.length() < 6) {
			throw new IllegalArgumentException("新密码长度不能少于6位");
		}
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
		List<Long> roleIds = new ArrayList<>();
		for (String code : roleCodes.split("[,，;；|]")) {
			String normalized = code.trim();
			if (normalized.isBlank()) {
				continue;
			}
			roleRepository.findByCodeIgnoreCase(normalized).ifPresent(role -> roleIds.add(role.getId()));
		}
		return roleIds;
	}

	private UserDTO toDto(UserEntity user) {
		UserDTO dto = new UserDTO();
		dto.setUserId(user.getId());
		dto.setUsername(user.getUsername());
		dto.setName(user.getName());
		dto.setDepartment(user.getDepartment());
		dto.setWorkstation(user.getWorkstation());
		dto.setPhone(user.getPhone());
		dto.setEnabled(user.getEnabled());
		dto.setRoleIds(user.getRoles().stream().map(RoleEntity::getId).toList());
		dto.setRoleCodes(user.getRoles().stream().map(RoleEntity::getCode).toList());
		dto.setPermissionCodes(user.getRoles().stream()
			.flatMap(role -> role.getPermissions().stream())
			.map(PermissionEntity::getCode)
			.distinct()
			.toList());
		return dto;
	}

	private String blankToNull(String value) {
		String normalized = normalize(value);
		return normalized.isBlank() ? null : normalized;
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
