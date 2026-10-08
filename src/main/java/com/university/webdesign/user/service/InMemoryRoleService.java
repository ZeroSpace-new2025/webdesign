package com.university.webdesign.user.service;

import com.university.webdesign.user.api.PermissionDTO;
import com.university.webdesign.user.api.RoleDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存数据的角色与权限服务。
 */
@Service
public class InMemoryRoleService implements RoleService
{
	private final Map<Long, RoleDTO> roles = new LinkedHashMap<>();
	private final Map<String, PermissionDTO> permissions = new LinkedHashMap<>();
	private final AtomicLong roleIdGenerator = new AtomicLong(10);

	/**
	 * 服务创建时写入角色和权限的默认数据。
	 */
	public InMemoryRoleService() {
		seedPermissions();
		seedRoles();
	}

	@Override
	public synchronized List<RoleDTO> query() {
		return roles.values().stream()
			.sorted(Comparator.comparing(RoleDTO::getRoleId))
			.map(this::copyRole)
			.toList();
	}

	@Override
	public synchronized RoleDTO getRole(Long roleId) {
		RoleDTO role = roles.get(roleId);
		if (role == null) {
			throw new IllegalArgumentException("角色不存在");
		}
		return copyRole(role);
	}

	@Override
	public synchronized RoleDTO createRole(RoleDTO roleDTO) {
		validateRole(roleDTO, null);
		Long roleId = roleIdGenerator.incrementAndGet();
		RoleDTO role = copyRole(roleDTO);
		role.setRoleId(roleId);
		roles.put(roleId, role);
		return copyRole(role);
	}

	@Override
	public synchronized RoleDTO updateRole(RoleDTO roleDTO) {
		if (roleDTO.getRoleId() == null || !roles.containsKey(roleDTO.getRoleId())) {
			throw new IllegalArgumentException("角色不存在");
		}
		validateRole(roleDTO, roleDTO.getRoleId());
		RoleDTO current = roles.get(roleDTO.getRoleId());
		current.setCode(roleDTO.getCode());
		current.setName(roleDTO.getName());
		current.setDescription(roleDTO.getDescription());
		return copyRole(current);
	}

	@Override
	public synchronized void deleteRole(Long roleId) {
		if (roleId != null && roleId <= 5) {
			throw new IllegalArgumentException("系统预置角色不能删除");
		}
		roles.remove(roleId);
	}

	@Override
	public synchronized List<PermissionDTO> queryPermissions() {
		return permissions.values().stream()
			.map(this::copyPermission)
			.toList();
	}

	@Override
	public synchronized RoleDTO updatePermissions(Long roleId, List<String> permissionCodes) {
		RoleDTO role = roles.get(roleId);
		if (role == null) {
			throw new IllegalArgumentException("角色不存在");
		}
		List<String> codes = permissionCodes == null ? List.of() : permissionCodes;
		for (String code : codes) {
			if (!permissions.containsKey(code)) {
				throw new IllegalArgumentException("权限不存在：" + code);
			}
		}
		role.setPermissionCodes(new ArrayList<>(new LinkedHashSet<>(codes)));
		return copyRole(role);
	}

	private void validateRole(RoleDTO roleDTO, Long ignoredRoleId) {
		if (roleDTO == null || isBlank(roleDTO.getCode()) || isBlank(roleDTO.getName())) {
			throw new IllegalArgumentException("角色编码和名称不能为空");
		}
		boolean duplicated = roles.values().stream()
			.anyMatch(role -> !Objects.equals(role.getRoleId(), ignoredRoleId)
				&& role.getCode().equalsIgnoreCase(roleDTO.getCode()));
		if (duplicated) {
			throw new IllegalArgumentException("角色编码已存在");
		}
	}

	private void seedPermissions() {
		// 权限编码前后端共用，修改时需要同步前端按钮显隐逻辑。
		addPermission(1L, "user:view", "查看用户", "查看员工账号与基础信息");
		addPermission(2L, "user:manage", "维护用户", "新增、编辑、启停和删除员工账号");
		addPermission(3L, "role:manage", "管理角色", "维护角色及其权限");
		addPermission(4L, "report:view", "查看报表", "查看餐厅月度销售总报表");
		addPermission(5L, "report:export", "导出报表", "打印或导出财务报表");
		addPermission(6L, "audit:view", "消费审计", "查询员工月度消费明细");
	}

	private void seedRoles() {
		// 前5个角色视为系统预置角色，允许编辑权限但不允许删除。
		addRole(1L, "MANAGER", "餐厅经理", "系统管理员，拥有全部权限", List.of(
			"user:view", "user:manage", "role:manage", "report:view", "report:export", "audit:view"));
		addRole(2L, "KITCHEN_SUPERVISOR", "厨房主管", "查看订单与履约数据", List.of("report:view"));
		addRole(3L, "DELIVERY_STAFF", "配餐员", "查看当日配餐任务", List.of());
		addRole(4L, "FINANCE", "财务", "查看、导出财务报表并审计员工消费", List.of(
			"user:view", "report:view", "report:export", "audit:view"));
		addRole(5L, "EMPLOYEE", "员工", "企业员工基础权限", List.of());
	}

	private void addPermission(Long id, String code, String name, String description) {
		PermissionDTO permission = new PermissionDTO();
		permission.setPermissionId(id);
		permission.setCode(code);
		permission.setName(name);
		permission.setDescription(description);
		permissions.put(code, permission);
	}

	private void addRole(Long id, String code, String name, String description, List<String> permissionCodes) {
		RoleDTO role = new RoleDTO();
		role.setRoleId(id);
		role.setCode(code);
		role.setName(name);
		role.setDescription(description);
		role.setPermissionCodes(new ArrayList<>(permissionCodes));
		roles.put(id, role);
	}

	private RoleDTO copyRole(RoleDTO source) {
		RoleDTO copy = new RoleDTO();
		copy.setRoleId(source.getRoleId());
		copy.setCode(source.getCode());
		copy.setName(source.getName());
		copy.setDescription(source.getDescription());
		copy.setPermissionCodes(source.getPermissionCodes() == null
			? new ArrayList<>()
			: new ArrayList<>(new LinkedHashSet<>(source.getPermissionCodes())));
		return copy;
	}

	private PermissionDTO copyPermission(PermissionDTO source) {
		PermissionDTO copy = new PermissionDTO();
		copy.setPermissionId(source.getPermissionId());
		copy.setCode(source.getCode());
		copy.setName(source.getName());
		copy.setDescription(source.getDescription());
		return copy;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
