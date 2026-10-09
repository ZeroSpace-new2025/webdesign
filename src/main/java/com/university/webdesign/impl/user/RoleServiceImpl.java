package com.university.webdesign.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.common.model.PermissionList;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.event.RolePermissionChangedEvent;
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
 * <p>
 * 权限模型：角色名称唯一（预置角色取 {@code common.RoleCodes} 的取值），
 * 角色权限是 {@link PermissionList} 位图，权限字典由 {@link PermissionEnum} 表达，
 * 因此本类不再依赖权限点表；对外仍然以权限点编码（{@code PermissionEnum.permCode}）字符串收发权限，
 * 保证 {@code @RequiresPerm}、JWT 载荷与前端契约不变。
 * <p>
 * 角色名称或角色权限变更时发布 {@link RolePermissionChangedEvent}，各模块据此失效鉴权缓存。
 * <p>
 * 事务统一用普通 {@code @Transactional}（含查询），不加 {@code readOnly = true}。
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService
{
	private final RoleRepository roleRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 构造器注入
	 *
	 * @param roleRepository 角色仓库
	 * @param eventPublisher 领域事件发布器
	 */
	public RoleServiceImpl(RoleRepository roleRepository, ApplicationEventPublisher eventPublisher) {
		this.roleRepository = roleRepository;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public Long create(RoleCmd cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "角色入参不能为空");
		}
		String name = required(cmd.getName(), "角色名称不能为空");
		if (roleRepository.existsByNameIgnoreCase(name)) {
			throw new BusinessException(ErrorCode.DUPLICATED, "角色名称已存在：" + name);
		}
		Role role = new Role();
		role.setName(name);
		role.setPermissionList(new PermissionList());
		Role saved = roleRepository.save(role);
		log.info("新增角色：名称={}，角色ID={}", saved.getName(), saved.getId());
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
		if (cmd.getName() == null) {
			return;
		}
		String name = required(cmd.getName(), "角色名称不能为空");
		if (name.equalsIgnoreCase(role.getName())) {
			return;
		}
		// 角色名称即角色身份：预置角色被 @RequiresPerm(roles=…) 与 RoleCodes 引用，改名会让鉴权失配
		if (RoleCodes.isPreset(role.getName())) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT,
					"预置角色「" + role.getName() + "」的名称不可修改");
		}
		if (roleRepository.existsByNameIgnoreCase(name)) {
			throw new BusinessException(ErrorCode.DUPLICATED, "角色名称已存在：" + name);
		}
		role.setName(name);
		Role saved = roleRepository.save(role);
		log.info("更新角色：角色ID={}，名称={}", saved.getId(), saved.getName());
		publishChanged(saved);
	}

	@Override
	@Transactional
	public void delete(Long roleId) {
		Role role = load(roleId);
		if (RoleCodes.isPreset(role.getName())) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT,
					"预置角色「" + role.getName() + "」不可删除");
		}
		long users = roleRepository.countUsersByRoleId(role.getId());
		if (users > 0) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT,
					"角色「" + role.getName() + "」仍有 " + users + " 个用户关联，无法删除");
		}
		roleRepository.delete(role);
		log.info("删除角色：角色ID={}，名称={}", role.getId(), role.getName());
		publishChanged(role);
	}

	@Override
	@Transactional
	public List<PermVO> listPermissions(String module) {
		List<PermissionEnum> permissions = PermissionEnum.dictionary(module);
		List<PermVO> result = new ArrayList<>(permissions.size());
		for (PermissionEnum permission : permissions) {
			result.add(new PermVO(permission.getPermCode(), permission.getPermName(),
					permission.getModule(), permission.getPermName()));
		}
		return result;
	}

	@Override
	@Transactional
	public void updatePermissions(Long roleId, List<String> permCodes) {
		Role role = load(roleId);
		PermissionList permissions = new PermissionList();
		Set<String> unknown = new LinkedHashSet<>();
		for (String code : normalizeCodes(permCodes)) {
			PermissionEnum.parse(code).ifPresentOrElse(permissions::setRoles, () -> unknown.add(code));
		}
		if (!unknown.isEmpty()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "权限点不存在：" + unknown);
		}
		role.setPermissionList(permissions);
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
	 * 按ID读取角色，不存在抛 40400
	 *
	 * @param roleId 角色ID
	 * @return 角色实体
	 */
	private Role load(Long roleId) {
		if (roleId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "角色ID不能为空");
		}
		return roleRepository.findById(roleId)
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
		vo.setName(role.getName());
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
