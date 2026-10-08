package com.university.webdesign.service.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.domain.user.Permission;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.domain.user.User;
import com.university.webdesign.domain.user.UserStatus;
import com.university.webdesign.event.RolePermissionChangedEvent;
import com.university.webdesign.event.UserUpdatedEvent;
import com.university.webdesign.repository.user.RoleRepository;
import com.university.webdesign.repository.user.UserRepository;
import com.university.webdesign.service.user.dto.ImportResultVO;
import com.university.webdesign.service.user.dto.UserBriefVO;
import com.university.webdesign.service.user.dto.UserCreateCmd;
import com.university.webdesign.service.user.dto.UserQuery;
import com.university.webdesign.service.user.dto.UserUpdateCmd;
import com.university.webdesign.service.user.dto.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户维护服务单元测试（Mockito，不起 Spring 上下文）。
 * <p>
 * 覆盖 M4 的员工维护约定：工号唯一（40901）、BCrypt 初始密码、默认初始密码 123456、
 * 停用为逻辑停用、批量导入支持部分成功、以及跨模块契约 {@code listByIds} / {@code hasAnyRole}。
 */
class UserServiceImplTest
{
	private UserRepository userRepository;
	private RoleRepository roleRepository;
	private PasswordEncoder passwordEncoder;
	private ApplicationEventPublisher eventPublisher;
	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		roleRepository = mock(RoleRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		eventPublisher = mock(ApplicationEventPublisher.class);
		userService = new UserServiceImpl(userRepository, roleRepository, passwordEncoder, eventPublisher);
		lenient().when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
		lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			if (user.getId() == null) {
				user.setId(1L);
			}
			return user;
		});
	}

	@Test
	@DisplayName("创建员工：用 BCrypt 存密码、绑定角色，并发布 UserUpdatedEvent")
	void shouldCreateUserWithEncodedPasswordAndRoles() {
		Role role = role(5L, "EMPLOYEE", "order:submit");
		when(userRepository.existsByEmployeeNoIgnoreCase("E1001")).thenReturn(false);
		when(roleRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(role));

		UserCreateCmd cmd = new UserCreateCmd();
		cmd.setEmployeeNo("E1001");
		cmd.setName("张三");
		cmd.setDeptId(10L);
		cmd.setDeptName("研发部");
		cmd.setWorkstation("R-12");
		cmd.setPhone("13800000001");
		cmd.setRoleIds(List.of(5L));

		Long userId = userService.create(cmd);

		assertThat(userId).isEqualTo(1L);
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();
		assertThat(saved.getPassword()).isEqualTo("ENCODED");
		assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(saved.getRoles()).extracting(Role::getRoleCode).containsExactly("EMPLOYEE");
		verify(passwordEncoder).encode("123456");
		verify(eventPublisher).publishEvent(any(UserUpdatedEvent.class));
	}

	@Test
	@DisplayName("创建员工：工号重复抛 40901")
	void shouldRejectDuplicatedEmployeeNo() {
		when(userRepository.existsByEmployeeNoIgnoreCase("E1001")).thenReturn(true);
		UserCreateCmd cmd = new UserCreateCmd();
		cmd.setEmployeeNo("E1001");
		cmd.setName("张三");

		assertThatThrownBy(() -> userService.create(cmd))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.DUPLICATED);
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	@DisplayName("创建员工：手机号格式非法抛 40001")
	void shouldRejectInvalidPhone() {
		when(userRepository.existsByEmployeeNoIgnoreCase("E1002")).thenReturn(false);
		UserCreateCmd cmd = new UserCreateCmd();
		cmd.setEmployeeNo("E1002");
		cmd.setName("李四");
		cmd.setPhone("abc");

		assertThatThrownBy(() -> userService.create(cmd))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("联系电话格式不正确");
	}

	@Test
	@DisplayName("更新员工：改工位后发布事件，工位供配送单使用")
	void shouldUpdateWorkstationAndPublishEvent() {
		User user = user(1L, UserStatus.ACTIVE);
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));

		UserUpdateCmd cmd = new UserUpdateCmd();
		cmd.setWorkstation("R-20");
		cmd.setPhone("13800000009");
		userService.update(1L, cmd);

		assertThat(user.getWorkstation()).isEqualTo("R-20");
		assertThat(user.getPhone()).isEqualTo("13800000009");
		verify(eventPublisher).publishEvent(any(UserUpdatedEvent.class));
	}

	@Test
	@DisplayName("停用员工：只置状态，不物理删除")
	void shouldDisableInsteadOfDelete() {
		User user = user(1L, UserStatus.ACTIVE);
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));

		userService.disable(1L);

		assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
		verify(userRepository, never()).delete(any(User.class));
		verify(userRepository, never()).deleteById(any());
	}

	@Test
	@DisplayName("变更状态：状态为空抛 40001")
	void shouldRejectNullStatus() {
		assertThatThrownBy(() -> userService.changeStatus(1L, null))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("账号状态不能为空");
	}

	@Test
	@DisplayName("详情：返回角色编码、角色ID与状态中文文案")
	void shouldReturnDetailWithRoles() {
		User user = user(1L, UserStatus.LOCKED);
		user.setRoles(new LinkedHashSet<>(List.of(role(5L, "FINANCE", "report:view"))));
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));

		UserVO vo = userService.getById(1L);

		assertThat(vo.getUserId()).isEqualTo(1L);
		assertThat(vo.getStatus()).isEqualTo("LOCKED");
		assertThat(vo.getStatusText()).isEqualTo("锁定");
		assertThat(vo.getRoleCodes()).containsExactly("FINANCE");
		assertThat(vo.getRoleIds()).containsExactly(5L);
		assertThat(vo.getRoles()).hasSize(1);
	}

	@Test
	@DisplayName("详情：用户不存在抛 40400")
	void shouldFailWhenUserMissing() {
		when(userRepository.findWithRolesById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getById(99L))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("分页：状态编码非法抛 40001")
	void shouldRejectUnknownStatusFilter() {
		UserQuery query = new UserQuery();
		query.setStatus("UNKNOWN");

		assertThatThrownBy(() -> userService.page(query))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("账号状态不合法");
	}

	@Test
	@DisplayName("分页：条件透传到仓库并返回 PageResult")
	void shouldPageWithFilters() {
		User user = user(1L, UserStatus.ACTIVE);
		Page<User> page = new PageImpl<>(List.of(user));
		when(userRepository.search(eq("张"), eq(10L), eq("R"), eq(UserStatus.ACTIVE), any(Pageable.class)))
				.thenReturn(page);
		UserQuery query = new UserQuery();
		query.setKeyword("张");
		query.setDeptId(10L);
		query.setWorkstation("R");
		query.setStatus("ACTIVE");

		PageResult<UserVO> result = userService.page(query);

		assertThat(result.getTotal()).isEqualTo(1L);
		assertThat(result.getList()).hasSize(1);
		assertThat(result.getList().get(0).getEmployeeNo()).isEqualTo("E1001");
	}

	@Test
	@DisplayName("跨模块契约 listByIds：批量返回精简信息，空入参返回空列表")
	void shouldListBriefByIds() {
		when(userRepository.findAllByIdIn(anyCollection()))
				.thenReturn(List.of(user(1L, UserStatus.ACTIVE)));
		List<UserBriefVO> briefs = userService.listByIds(List.of(1L, 2L));

		assertThat(briefs).hasSize(1);
		assertThat(briefs.get(0).getUserId()).isEqualTo(1L);
		assertThat(briefs.get(0).getName()).isEqualTo("张三");
		assertThat(briefs.get(0).getWorkstation()).isEqualTo("R-12");
		assertThat(userService.listByIds(List.of())).isEmpty();
		assertThat(userService.listByIds(null)).isEmpty();
	}

	@Test
	@DisplayName("跨模块契约 hasAnyRole：命中任一角色即为真，用户不存在为假")
	void shouldCheckAnyRole() {
		User user = user(1L, UserStatus.ACTIVE);
		user.setRoles(new LinkedHashSet<>(List.of(role(5L, "FINANCE", "report:view"))));
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
		when(userRepository.findWithRolesById(2L)).thenReturn(Optional.empty());

		assertThat(userService.hasAnyRole(1L, "MANAGER", "finance")).isTrue();
		assertThat(userService.hasAnyRole(1L, "MANAGER")).isFalse();
		assertThat(userService.hasAnyRole(2L, "MANAGER")).isFalse();
		assertThat(userService.hasAnyRole(null, "MANAGER")).isFalse();
		assertThat(userService.hasAnyRole(1L)).isFalse();
	}

	@Test
	@DisplayName("分配角色：全量覆盖并发布 RolePermissionChangedEvent")
	void shouldAssignRolesAndPublishPermissionChanged() {
		User user = user(1L, UserStatus.ACTIVE);
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
		when(roleRepository.findAllByIdIn(anyCollection()))
				.thenReturn(List.of(role(5L, "MANAGER", "report:view")));

		userService.assignRoles(1L, List.of(5L));

		assertThat(user.getRoles()).extracting(Role::getRoleCode).containsExactly("MANAGER");
		verify(eventPublisher).publishEvent(any(RolePermissionChangedEvent.class));
	}

	@Test
	@DisplayName("分配角色：角色不存在抛 40400")
	void shouldRejectUnknownRole() {
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user(1L, UserStatus.ACTIVE)));
		when(roleRepository.findAllByIdIn(anyCollection())).thenReturn(List.of());

		assertThatThrownBy(() -> userService.assignRoles(1L, List.of(7L)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("角色不存在");
	}

	@Test
	@DisplayName("批量导入：逐行校验，支持部分成功并返回行号")
	void shouldImportPartially() {
		when(userRepository.existsByEmployeeNoIgnoreCase("E1001")).thenReturn(false);
		when(userRepository.existsByEmployeeNoIgnoreCase("E1002")).thenReturn(true);
		String csv = "工号,姓名,部门ID,部门名称,工位,电话\n"
				+ "E1001,张三,10,研发部,R-12,13800000001\n"
				+ "E1002,李四,20,产品部,P-08,13800000002\n"
				+ "E1003,,20,产品部,P-09,13800000003\n";
		MockMultipartFile file = new MockMultipartFile("file", "employees.csv", "text/csv",
				csv.getBytes(StandardCharsets.UTF_8));

		ImportResultVO result = userService.importFromExcel(file, 10L, List.of());

		assertThat(result.getTotal()).isEqualTo(3);
		assertThat(result.getSuccessCount()).isEqualTo(1);
		assertThat(result.getFailCount()).isEqualTo(2);
		assertThat(result.getErrors()).hasSize(2);
		assertThat(result.getErrors().get(0).row()).isEqualTo(3);
		assertThat(result.getErrors().get(0).reason()).contains("工号已存在");
		assertThat(result.getErrors().get(1).row()).isEqualTo(4);
		assertThat(result.getErrors().get(1).reason()).contains("姓名不能为空");
	}

	@Test
	@DisplayName("批量导入：表头缺少工号或姓名抛 40001")
	void shouldRejectWrongHeader() {
		MockMultipartFile file = new MockMultipartFile("file", "employees.csv", "text/csv",
				"部门,工位\n研发部,R-12\n".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> userService.importFromExcel(file, null, null))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("模板表头不正确");
	}

	@Test
	@DisplayName("导入模板：返回带 UTF-8 BOM 的 CSV，含中英文表头")
	void shouldExportTemplateWithBom() throws Exception {
		String content = new String(userService.exportImportTemplate().getContentAsByteArray(),
				StandardCharsets.UTF_8);

		assertThat(content).startsWith("\uFEFF");
		assertThat(content).contains("工号(employeeNo)");
		assertThat(content).contains("姓名(name)");
		assertThat(content).contains("E1001");
	}

	/**
	 * 构造用户实体
	 *
	 * @param id     用户ID
	 * @param status 状态
	 * @return 用户实体
	 */
	private User user(Long id, UserStatus status) {
		User user = new User();
		user.setId(id);
		user.setEmployeeNo("E1001");
		user.setName("张三");
		user.setPassword("ENCODED");
		user.setDeptId(10L);
		user.setDeptName("研发部");
		user.setWorkstation("R-12");
		user.setPhone("13800000001");
		user.setStatus(status);
		return user;
	}

	/**
	 * 构造角色实体
	 *
	 * @param id        角色ID
	 * @param roleCode  角色编码
	 * @param permCode  权限点编码
	 * @return 角色实体
	 */
	private Role role(Long id, String roleCode, String permCode) {
		Permission permission = new Permission();
		permission.setId(100L);
		permission.setPermCode(permCode);
		permission.setPermName(permCode);
		permission.setModule("report");
		Role role = new Role();
		role.setId(id);
		role.setRoleCode(roleCode);
		role.setRoleName(roleCode);
		role.setPermissions(new LinkedHashSet<>(List.of(permission)));
		return role;
	}
}
