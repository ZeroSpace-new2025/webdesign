package com.university.webdesign.user.service;

import com.university.webdesign.user.api.PermissionDTO;
import com.university.webdesign.user.api.RoleDTO;
import com.university.webdesign.user.data.PermissionEntity;
import com.university.webdesign.user.data.RoleEntity;
import com.university.webdesign.user.repository.PermissionRepository;
import com.university.webdesign.user.repository.RoleRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 使用JPA持久化角色和权限映射。
 */
@Service
@ConditionalOnProperty(name = "app.storage", havingValue = "database", matchIfMissing = true)
public class JpaRoleService implements RoleService
{
	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;

	public JpaRoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoleDTO> query() {
		return roleRepository.findAllByOrderByIdAsc().stream()
			.map(this::toDto)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public RoleDTO getRole(Long roleId) {
		return toDto(requireRole(roleId));
	}

	@Override
	@Transactional
	public RoleDTO createRole(RoleDTO roleDTO) {
		validateRole(roleDTO, null);
		RoleEntity role = new RoleEntity();
		apply(role, roleDTO);
		role.setPermissions(resolvePermissions(roleDTO.getPermissionCodes()));
		return toDto(roleRepository.save(role));
	}

	@Override
	@Transactional
	public RoleDTO updateRole(RoleDTO roleDTO) {
		if (roleDTO == null || roleDTO.getRoleId() == null) {
			throw new IllegalArgumentException("角色ID不能为空");
		}
		validateRole(roleDTO, roleDTO.getRoleId());
		RoleEntity role = requireRole(roleDTO.getRoleId());
		apply(role, roleDTO);
		return toDto(roleRepository.save(role));
	}

	@Override
	@Transactional
	public void deleteRole(Long roleId) {
		RoleEntity role = requireRole(roleId);
		if (Set.of("MANAGER", "KITCHEN_SUPERVISOR", "DELIVERY_STAFF", "FINANCE", "EMPLOYEE")
			.contains(role.getCode().toUpperCase(Locale.ROOT))) {
			throw new IllegalArgumentException("系统预置角色不能删除");
		}
		roleRepository.delete(role);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PermissionDTO> queryPermissions() {
		return permissionRepository.findAllByOrderByIdAsc().stream()
			.map(this::toDto)
			.toList();
	}

	@Override
	@Transactional
	public RoleDTO updatePermissions(Long roleId, List<String> permissionCodes) {
		RoleEntity role = requireRole(roleId);
		role.setPermissions(resolvePermissions(permissionCodes));
		return toDto(roleRepository.save(role));
	}

	private RoleEntity requireRole(Long roleId) {
		if (roleId == null) {
			throw new IllegalArgumentException("角色不存在");
		}
		return roleRepository.findWithPermissionsById(roleId)
			.orElseThrow(() -> new IllegalArgumentException("角色不存在"));
	}

	private void validateRole(RoleDTO roleDTO, Long ignoredRoleId) {
		if (roleDTO == null || isBlank(roleDTO.getCode()) || isBlank(roleDTO.getName())) {
			throw new IllegalArgumentException("角色编码和名称不能为空");
		}
		roleRepository.findByCodeIgnoreCase(roleDTO.getCode().trim()).ifPresent(existing -> {
			if (!existing.getId().equals(ignoredRoleId)) {
				throw new IllegalArgumentException("角色编码已存在");
			}
		});
	}

	private void apply(RoleEntity role, RoleDTO roleDTO) {
		role.setCode(roleDTO.getCode().trim().toUpperCase(Locale.ROOT));
		role.setName(roleDTO.getName().trim());
		role.setDescription(roleDTO.getDescription());
	}

	private Set<PermissionEntity> resolvePermissions(List<String> permissionCodes) {
		Set<PermissionEntity> permissions = new LinkedHashSet<>();
		if (permissionCodes == null) {
			return permissions;
		}
		for (String code : permissionCodes) {
			if (isBlank(code)) {
				continue;
			}
			PermissionEntity permission = permissionRepository.findByCodeIgnoreCase(code.trim())
				.orElseThrow(() -> new IllegalArgumentException("权限不存在：" + code));
			permissions.add(permission);
		}
		return permissions;
	}

	private RoleDTO toDto(RoleEntity role) {
		RoleDTO dto = new RoleDTO();
		dto.setRoleId(role.getId());
		dto.setCode(role.getCode());
		dto.setName(role.getName());
		dto.setDescription(role.getDescription());
		dto.setPermissionCodes(role.getPermissions().stream().map(PermissionEntity::getCode).toList());
		return dto;
	}

	private PermissionDTO toDto(PermissionEntity permission) {
		PermissionDTO dto = new PermissionDTO();
		dto.setPermissionId(permission.getId());
		dto.setCode(permission.getCode());
		dto.setName(permission.getName());
		dto.setDescription(permission.getDescription());
		return dto;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
