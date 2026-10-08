package com.university.webdesign.service.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.domain.user.Permission;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.event.RolePermissionChangedEvent;
import com.university.webdesign.repository.user.PermissionRepository;
import com.university.webdesign.repository.user.RoleRepository;
import com.university.webdesign.service.user.RoleService;
import com.university.webdesign.service.user.dto.PermVO;
import com.university.webdesign.service.user.dto.RoleCmd;
import com.university.webdesign.service.user.dto.RoleQuery;
import com.university.webdesign.service.user.dto.RoleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色与权限维护服务实现。
 * <p>
 * 对应《对外方法表》5.2 的 {@code RoleService} 全部方法。
 * 角色权限或用户角色发生变更时发布
 * {@link RolePermissionChangedEvent}，各模块据此失效鉴权缓存。
 * <p>
 * 事务统一用普通 {@code @Transactional}（含查询），不加 {@code readOnly = true}。
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService
{
	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 构造器注入
	 *
	 * @param roleRepository       角色仓库
	 * @param permissionRepository 权限点仓库
	 * @param eventPublisher       领域事件发布器
	 */
	public RoleServiceImpl(
			RoleRepository roleRepository,
			PermissionRepository permissionRepository,
			ApplicationEventPublisher eventPublisher) {
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public Long create(RoleCmd cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "角色入参不能为空");
		}
		String roleCode = required(cmd.getRoleCode(), "角色编码不能为空");
		if (roleRepository.existsByRoleCodeIgnoreCase(roleCode)) {
			throw new BusinessException(ErrorCode.DUPLICATED, "角色编码已存在：" + roleCode);
		}
		Role role = new Role();
		role.setRoleCode(roleCode);
		role.setRoleName(required(cmd.getRoleName(), "角色名称不能为空"));
		role.setDescription(trimToNull(cmd.getDescription()));
		Role saved = roleRepository.save(role);
		log.info("新增角色：编码={}，角色ID={}", saved.getRoleCode(), saved.getId());
		return saved.getId();
	}

	@Override
	@Transactional
	public PageResult<RoleVO> page(RoleQuery q) {
		RoleQuery query = q == null ? new RoleQuery() : q;
		Page<Role> page = roleRepository.search(trimToNull(query.getKeyword()),
				PageRequest.of(query.toSpringPageNumber(), query.normalizedPageSize()));
		return PageResult.from(page, this::toVO);
	}

	@Override
	@Transactional
	public void update(Long roleId, RoleCmd cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "角色入参不能为空");
		}
		Role role = load(roleId);
		if (cmd.getRoleName() != null) {
			role.setRoleName(required(cmd.getRoleName(), "角色名称不能为空"));
		}
		if (cmd.getDescription() != null) {
			role.setDescription(trimToNull(cmd.getDescription()));
		}
		Role saved = roleRepository.save(role);
		log.info("更新角色：角色ID={}，名称={}", saved.getId(), saved.getRoleName());
		publishChanged(saved);
	}

	@Override
	@Transactional
	public void delete(Long roleId) {
		Role role = load(roleId);
		long users = roleRepository.countUsersByRoleId(role.getId());
		if (users > 0) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT,
					"角色「" + role.getRoleName() + "」仍有 " + users + " 个用户关联，无法删除");
		}
		roleRepository.delete(role);
		log.info("删除角色：角色ID={}，编码={}", role.getId(), role.getRoleCode());
		publishChanged(role);
	}

	@Override
	@Transactional
	public List<PermVO> listPermissions(String module) {
		String normalized = trimToNull(module);
		List<Permission> permissions = normalized == null
				? permissionRepository.findAllByOrderByModuleAscPermCodeAsc()
				: permissionRepository.findAllByModuleOrderByPermCodeAsc(normalized);
		List<PermVO> result = new ArrayList<>(permissions.size());
		for (Permission permission : permissions) {
			result.add(new PermVO(permission.getPermCode(), permission.getPermName(),
					permission.getModule(), permission.getDescription()));
		}
		return result;
	}

	@Override
	@Transactional
	public void updatePermissions(Long roleId, List<String> permCodes) {
		Role role = load(roleId);
		Set<String> codes = normalizeCodes(permCodes);
		Set<Permission> permissions = new LinkedHashSet<>();
		if (!codes.isEmpty()) {
			List<Permission> found = permissionRepository.findAllByPermCodeIn(codes);
			if (found.size() != codes.size()) {
				Set<String> known = new LinkedHashSet<>();
				found.forEach(permission -> known.add(permission.getPermCode()));
				codes.removeAll(known);
				throw new BusinessException(ErrorCode.NOT_FOUND, "权限点不存在：" + codes);
			}
			permissions.addAll(found);
		}
		role.setPermissions(permissions);
		Role saved = roleRepository.save(role);
		log.info("配置角色权限：角色ID={}，权限点数={}", saved.getId(), permissions.size());
		publishChanged(saved);
	}

	@Override
	@Transactional
	public Set<String> getPermissions(Long roleId) {
		return new LinkedHashSet<>(load(roleId).permCodes());
	}

	/**
	 * 发布角色权限变更事件（全模块鉴权缓存失效）
	 *
	 * @param role 角色实体
	 */
	private void publishChanged(Role role) {
		eventPublisher.publishEvent(new RolePermissionChangedEvent(role.getId(), null,
				List.copyOf(role.permCodes())));
	}

	/**
	 * 按ID读取角色（含权限点），不存在抛 40400
	 *
	 * @param roleId 角色ID
	 * @return 角色实体
	 */
	private Role load(Long roleId) {
		if (roleId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "角色ID不能为空");
		}
		return roleRepository.findWithPermissionsById(roleId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在：" + roleId));
	}

	/**
	 * 角色实体 → 角色视图
	 *
	 * @param role 角色实体
	 * @return 角色视图
	 */
	private RoleVO toVO(Role role) {
		RoleVO vo = new RoleVO();
		vo.setRoleId(role.getId());
		vo.setRoleCode(role.getRoleCode());
		vo.setRoleName(role.getRoleName());
		vo.setDescription(role.getDescription());
		vo.setPermCodes(new LinkedHashSet<>(role.permCodes()));
		return vo;
	}

	/**
	 * 权限点编码归一化：去空白、去重、忽略大小写差异
	 *
	 * @param permCodes 原始编码集合
	 * @return 归一化后的编码集合
	 */
	private Set<String> normalizeCodes(List<String> permCodes) {
		Set<String> codes = new LinkedHashSet<>();
		if (permCodes == null) {
			return codes;
		}
		for (String code : permCodes) {
			String normalized = trimToNull(code);
			if (normalized == null) {
				continue;
			}
			boolean duplicated = codes.stream().anyMatch(existing -> existing.equalsIgnoreCase(normalized));
			if (!duplicated) {
				codes.add(normalized);
			}
		}
		return codes;
	}

	/**
	 * 必填校验
	 *
	 * @param value   原值
	 * @param message 为空时的提示
	 * @return 去空白后的值
	 */
	private String required(String value, String message) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, message);
		}
		return normalized;
	}

	/**
	 * 去空白，空白返回 null
	 *
	 * @param value 原值
	 * @return 归一化后的值
	 */
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
