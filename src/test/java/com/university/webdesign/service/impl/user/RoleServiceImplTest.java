package com.university.webdesign.service.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.domain.user.Permission;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.event.RolePermissionChangedEvent;
import com.university.webdesign.repository.user.PermissionRepository;
import com.university.webdesign.repository.user.RoleRepository;
import com.university.webdesign.service.user.dto.PermVO;
import com.university.webdesign.service.user.dto.RoleCmd;
import com.university.webdesign.service.user.dto.RoleQuery;
import com.university.webdesign.service.user.dto.RoleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色与权限服务单元测试（Mockito，不起 Spring 上下文）。
 * <p>
 * 覆盖 M4 的角色维护约定：角色编码唯一（40901）、有关联用户时禁止删除（40902）、
 * 权限点全量覆盖、权限点不存在（40400），以及权限字典按模块过滤。
 */
class RoleServiceImplTest
{
	private RoleRepository roleRepository;
	private PermissionRepository permissionRepository;
	private ApplicationEventPublisher eventPublisher;
	private RoleServiceImpl roleService;

	@BeforeEach
	void setUp() {
		roleRepository = mock(RoleRepository.class);
		permissionRepository = mock(PermissionRepository.class);
		eventPublisher = mock(ApplicationEventPublisher.class);
		roleService = new RoleServiceImpl(roleRepository, permissionRepository, eventPublisher);
		lenient().when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
			Role role = invocation.getArgument(0);
			if (role.getId() == null) {
				role.setId(1L);
			}
			return role;
		});
	}

	@Test
	@DisplayName("新增角色：编码唯一，成功返回角色ID")
	void shouldCreateRole() {
		when(roleRepository.existsByRoleCodeIgnoreCase("MANAGER")).thenReturn(false);
		RoleCmd cmd = new RoleCmd();
		cmd.setRoleCode("MANAGER");
		cmd.setRoleName("餐厅经理");
		cmd.setDescription("系统管理员");

		Long roleId = roleService.create(cmd);

		assertThat(roleId).isEqualTo(1L);
		verify(roleRepository).save(any(Role.class));
	}

	@Test
	@DisplayName("新增角色：编码重复抛 40901，名称为空抛 40001")
	void shouldRejectInvalidCreate() {
		when(roleRepository.existsByRoleCodeIgnoreCase("MANAGER")).thenReturn(true);
		RoleCmd duplicated = new RoleCmd();
		duplicated.setRoleCode("MANAGER");
		duplicated.setRoleName("餐厅经理");

		assertThatThrownBy(() -> roleService.create(duplicated))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.DUPLICATED);

		when(roleRepository.existsByRoleCodeIgnoreCase("FINANCE")).thenReturn(false);
		RoleCmd blankName = new RoleCmd();
		blankName.setRoleCode("FINANCE");
		assertThatThrownBy(() -> roleService.create(blankName))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("角色名称不能为空");
	}

	@Test
	@DisplayName("删除角色：存在关联用户抛 40902")
	void shouldRejectDeleteWhenUsersExist() {
		Role role = role(9L, "MANAGER");
		when(roleRepository.findWithPermissionsById(9L)).thenReturn(Optional.of(role));
		when(roleRepository.countUsersByRoleId(9L)).thenReturn(3L);

		assertThatThrownBy(() -> roleService.delete(9L))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.STATE_CONFLICT);
		verify(roleRepository, never()).delete(any(Role.class));
	}

	@Test
	@DisplayName("删除角色：无关联用户时删除并发布变更事件")
	void shouldDeleteRoleWithoutUsers() {
		Role role = role(9L, "MANAGER");
		when(roleRepository.findWithPermissionsById(9L)).thenReturn(Optional.of(role));
		when(roleRepository.countUsersByRoleId(9L)).thenReturn(0L);

		roleService.delete(9L);

		verify(roleRepository).delete(role);
		verify(eventPublisher).publishEvent(any(RolePermissionChangedEvent.class));
	}

	@Test
	@DisplayName("分页查询：关键字透传并返回角色视图（含权限集合）")
	void shouldPageRoles() {
		Role role = role(9L, "MANAGER");
		role.getPermissions().add(permission("report:view"));
		Page<Role> page = new PageImpl<>(List.of(role));
		when(roleRepository.search(eq("经理"), any(Pageable.class))).thenReturn(page);
		RoleQuery query = new RoleQuery();
		query.setKeyword("经理");

		PageResult<RoleVO> result = roleService.page(query);

		assertThat(result.getTotal()).isEqualTo(1L);
		assertThat(result.getList().get(0).getRoleCode()).isEqualTo("MANAGER");
		assertThat(result.getList().get(0).getPermCodes()).containsExactly("report:view");
	}

	@Test
	@DisplayName("配置角色权限：全量覆盖并发布变更事件")
	void shouldUpdatePermissions() {
		Role role = role(9L, "FINANCE");
		when(roleRepository.findWithPermissionsById(9L)).thenReturn(Optional.of(role));
		when(permissionRepository.findAllByPermCodeIn(anyCollection()))
				.thenReturn(List.of(permission("report:view"), permission("audit:view")));

		roleService.updatePermissions(9L, List.of("report:view", "audit:view"));

		assertThat(role.getPermissions()).extracting(Permission::getPermCode)
				.containsExactlyInAnyOrder("report:view", "audit:view");
		verify(eventPublisher).publishEvent(any(RolePermissionChangedEvent.class));
	}

	@Test
	@DisplayName("配置角色权限：传空集合即清空权限")
	void shouldClearPermissionsWithEmptyList() {
		Role role = role(9L, "DELIVERY_STAFF");
		role.getPermissions().add(permission("report:view"));
		when(roleRepository.findWithPermissionsById(9L)).thenReturn(Optional.of(role));

		roleService.updatePermissions(9L, List.of());

		assertThat(role.getPermissions()).isEmpty();
		verify(permissionRepository, never()).findAllByPermCodeIn(anyCollection());
	}

	@Test
	@DisplayName("配置角色权限：权限点不存在抛 40400 并列出缺失编码")
	void shouldRejectUnknownPermissionCode() {
		Role role = role(9L, "FINANCE");
		when(roleRepository.findWithPermissionsById(9L)).thenReturn(Optional.of(role));
		when(permissionRepository.findAllByPermCodeIn(anyCollection()))
				.thenReturn(List.of(permission("report:view")));

		assertThatThrownBy(() -> roleService.updatePermissions(9L, List.of("report:view", "no:such")))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("no:such")
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("查询角色权限：返回权限点编码集合，角色不存在抛 40400")
	void shouldGetPermissions() {
		Role role = role(9L, "FINANCE");
		role.getPermissions().add(permission("report:view"));
		when(roleRepository.findWithPermissionsById(9L)).thenReturn(Optional.of(role));

		Set<String> codes = roleService.getPermissions(9L);

		assertThat(codes).containsExactly("report:view");
		assertThatThrownBy(() -> roleService.getPermissions(99L))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("权限点字典：不传模块返回全部，传模块只返回该模块")
	void shouldListPermissionsByModule() {
		when(permissionRepository.findAllByOrderByModuleAscPermCodeAsc())
				.thenReturn(List.of(permission("report:view"), permission("audit:view")));
		when(permissionRepository.findAllByModuleOrderByPermCodeAsc("report"))
				.thenReturn(List.of(permission("report:view")));

		List<PermVO> all = roleService.listPermissions(null);
		List<PermVO> reportOnly = roleService.listPermissions("report");

		assertThat(all).hasSize(2);
		assertThat(reportOnly).hasSize(1);
		assertThat(reportOnly.get(0).permCode()).isEqualTo("report:view");
		assertThat(reportOnly.get(0).module()).isEqualTo("report");
	}

	@Test
	@DisplayName("更新角色：只改名称与描述，并发布变更事件")
	void shouldUpdateRoleNameAndDescription() {
		Role role = role(9L, "MANAGER");
		when(roleRepository.findWithPermissionsById(9L)).thenReturn(Optional.of(role));
		RoleCmd cmd = new RoleCmd();
		cmd.setRoleCode("IGNORED");
		cmd.setRoleName("餐厅经理（新）");
		cmd.setDescription("拥有全部权限");

		roleService.update(9L, cmd);

		ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
		verify(roleRepository).save(captor.capture());
		assertThat(captor.getValue().getRoleName()).isEqualTo("餐厅经理（新）");
		assertThat(captor.getValue().getRoleCode()).isEqualTo("MANAGER");
		verify(eventPublisher).publishEvent(any(RolePermissionChangedEvent.class));
	}

	/**
	 * 构造角色实体
	 *
	 * @param id       角色ID
	 * @param roleCode 角色编码
	 * @return 角色实体
	 */
	private Role role(Long id, String roleCode) {
		Role role = new Role();
		role.setId(id);
		role.setRoleCode(roleCode);
		role.setRoleName(roleCode);
		role.setPermissions(new LinkedHashSet<>());
		return role;
	}

	/**
	 * 构造权限点实体
	 *
	 * @param permCode 权限点编码
	 * @return 权限点实体
	 */
	private Permission permission(String permCode) {
		Permission permission = new Permission();
		permission.setId((long) permCode.hashCode());
		permission.setPermCode(permCode);
		permission.setPermName(permCode);
		permission.setModule("report");
		permission.setDescription("测试权限点");
		return permission;
	}
}
