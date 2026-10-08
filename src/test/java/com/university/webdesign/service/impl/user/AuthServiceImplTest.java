package com.university.webdesign.service.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.domain.user.Permission;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.domain.user.User;
import com.university.webdesign.domain.user.UserStatus;
import com.university.webdesign.repository.user.UserRepository;
import com.university.webdesign.service.impl.user.support.JwtCodec;
import com.university.webdesign.service.impl.user.support.JwtPayload;
import com.university.webdesign.service.user.dto.CurrentUserVO;
import com.university.webdesign.service.user.dto.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务单元测试（Mockito，不起 Spring 上下文）。
 * <p>
 * 覆盖 M4 的认证约定：工号+BCrypt 密码登录、停用/锁定账号拒绝登录、
 * JWT 校验返回 {@link UserContext}、登出黑名单、改密后旧令牌失效、权限点判定。
 */
class AuthServiceImplTest
{
	private static final String SECRET = "unit-test-secret-key-for-auth-service-0123456789";

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private JwtCodec jwtCodec;
	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		jwtCodec = new JwtCodec(SECRET, 60);
		authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtCodec);
		lenient().when(passwordEncoder.encode(anyString())).thenReturn("ENCODED-NEW");
	}

	@Test
	@DisplayName("登录成功：签发 JWT 并返回角色与权限点")
	void shouldLoginAndIssueToken() {
		User user = user(UserStatus.ACTIVE);
		when(userRepository.findWithRolesByEmployeeNoIgnoreCase("manager")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("admin123", "ENCODED")).thenReturn(true);

		LoginVO login = authService.login("manager", "admin123");

		assertThat(login.getToken()).isNotBlank();
		assertThat(login.getExpiresIn()).isEqualTo(60 * 60L);
		assertThat(login.getUserInfo().getUserId()).isEqualTo(1L);
		assertThat(login.getUserInfo().getRoles()).containsExactly("MANAGER");
		assertThat(login.getUserInfo().getPermCodes()).containsExactly("report:view");
		// 令牌可被 verifyToken 解析回同一身份
		UserContext context = authService.verifyToken(login.getToken());
		assertThat(context.userId()).isEqualTo(1L);
		assertThat(context.employeeNo()).isEqualTo("manager");
		assertThat(context.hasAnyRole("MANAGER")).isTrue();
	}

	@Test
	@DisplayName("登录失败：工号不存在或密码错误一律 40100")
	void shouldRejectBadCredentials() {
		when(userRepository.findWithRolesByEmployeeNoIgnoreCase("nobody")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> authService.login("nobody", "x"))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.UNAUTHENTICATED);

		when(userRepository.findWithRolesByEmployeeNoIgnoreCase("manager"))
				.thenReturn(Optional.of(user(UserStatus.ACTIVE)));
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
		assertThatThrownBy(() -> authService.login("manager", "wrong"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("工号或密码错误");
	}

	@Test
	@DisplayName("登录失败：工号或密码为空抛 40001")
	void shouldRejectBlankCredentials() {
		assertThatThrownBy(() -> authService.login("  ", "x"))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.PARAM_INVALID);
	}

	@Test
	@DisplayName("登录失败：账号停用抛 40300")
	void shouldRejectDisabledAccount() {
		when(userRepository.findWithRolesByEmployeeNoIgnoreCase("manager"))
				.thenReturn(Optional.of(user(UserStatus.DISABLED)));

		assertThatThrownBy(() -> authService.login("manager", "admin123"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("已停用");
	}

	@Test
	@DisplayName("登录失败：账号锁定抛 40300")
	void shouldRejectLockedAccount() {
		when(userRepository.findWithRolesByEmployeeNoIgnoreCase("manager"))
				.thenReturn(Optional.of(user(UserStatus.LOCKED)));

		assertThatThrownBy(() -> authService.login("manager", "admin123"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("已锁定");
	}

	@Test
	@DisplayName("登出：令牌进入黑名单，再次校验抛 40100")
	void shouldBlacklistTokenOnLogout() {
		when(userRepository.findWithRolesByEmployeeNoIgnoreCase("manager"))
				.thenReturn(Optional.of(user(UserStatus.ACTIVE)));
		when(passwordEncoder.matches("admin123", "ENCODED")).thenReturn(true);
		String token = authService.login("manager", "admin123").getToken();

		authService.logout(token);

		assertThatThrownBy(() -> authService.verifyToken(token))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("登录已失效");
	}

	@Test
	@DisplayName("登出：非法令牌静默忽略，不抛异常")
	void shouldIgnoreInvalidTokenOnLogout() {
		authService.logout("not-a-jwt");
		authService.logout(null);
		authService.logout("  ");
	}

	@Test
	@DisplayName("获取当前用户：本人可查，他人需经理，用户不存在抛 40400")
	void shouldLoadCurrentUser() {
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));

		CurrentUserVO vo = authService.currentUser(1L);

		assertThat(vo.getEmployeeNo()).isEqualTo("manager");
		assertThat(vo.getWorkstation()).isEqualTo("A-01");
		assertThat(vo.getPermCodes()).containsExactly("report:view");

		when(userRepository.findWithRolesById(99L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> authService.currentUser(99L))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("修改密码：校验旧密码、BCrypt 存新密码")
	void shouldChangePasswordWithEncodedValue() {
		User user = user(UserStatus.ACTIVE);
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("admin123", "ENCODED")).thenReturn(true);

		authService.changePassword(1L, "admin123", "newPass123");

		assertThat(user.getPassword()).isEqualTo("ENCODED-NEW");
		verify(passwordEncoder).encode("newPass123");
		verify(userRepository).save(user);
	}

	@Test
	@DisplayName("修改密码：改密后此前签发的令牌不再有效")
	void shouldInvalidateTokenIssuedBeforePasswordChange() {
		Instant now = Instant.now();
		JwtCodec mockCodec = mock(JwtCodec.class);
		// 令牌在一小时前签发（iat 明确早于改密时刻），一小时后到期
		when(mockCodec.parse("OLD-TOKEN")).thenReturn(new JwtPayload(1L, "manager", "林经理", 1L,
				List.of("MANAGER"), List.of("report:view"),
				now.minusSeconds(3600).getEpochSecond(), now.plusSeconds(3600).getEpochSecond()));
		AuthServiceImpl service = new AuthServiceImpl(userRepository, passwordEncoder, mockCodec);
		when(userRepository.findWithRolesById(1L))
				.thenReturn(Optional.of(user(UserStatus.ACTIVE)));
		when(passwordEncoder.matches("admin123", "ENCODED")).thenReturn(true);

		// 改密发生在本秒内，令牌在改密之前签发（iat 更早一秒）
		service.changePassword(1L, "admin123", "newPass123");

		assertThatThrownBy(() -> service.verifyToken("OLD-TOKEN"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("密码已修改");
	}

	@Test
	@DisplayName("修改密码：旧密码错误抛 40001，新密码过短抛 40001")
	void shouldValidatePasswordChange() {
		User user = user(UserStatus.ACTIVE);
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("bad", "ENCODED")).thenReturn(false);

		assertThatThrownBy(() -> authService.changePassword(1L, "bad", "newPass123"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("旧密码不正确");
		assertThatThrownBy(() -> authService.changePassword(1L, "admin123", "123"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("新密码长度不能少于");
		assertThatThrownBy(() -> authService.changePassword(1L, "same", "same"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("新密码不能与旧密码相同");
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	@DisplayName("权限点判定：命中返回 true，停用账号或不存在的用户返回 false")
	void shouldCheckPermission() {
		User user = user(UserStatus.ACTIVE);
		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
		when(userRepository.findWithRolesById(2L)).thenReturn(Optional.of(user(UserStatus.DISABLED)));
		when(userRepository.findWithRolesById(3L)).thenReturn(Optional.empty());

		assertThat(authService.checkPermission(1L, "report:view")).isTrue();
		assertThat(authService.checkPermission(1L, "REPORT:VIEW")).isTrue();
		assertThat(authService.checkPermission(1L, "order:invalidate")).isFalse();
		assertThat(authService.checkPermission(2L, "report:view")).isFalse();
		assertThat(authService.checkPermission(3L, "report:view")).isFalse();
		assertThat(authService.checkPermission(null, "report:view")).isFalse();
		assertThat(authService.checkPermission(1L, " ")).isFalse();
	}

	/**
	 * 构造用户实体（含 MANAGER 角色与 report:view 权限点）
	 *
	 * @param status 账号状态
	 * @return 用户实体
	 */
	private User user(UserStatus status) {
		Permission permission = new Permission();
		permission.setId(100L);
		permission.setPermCode("report:view");
		permission.setPermName("查看报表");
		permission.setModule("report");
		Role role = new Role();
		role.setId(9L);
		role.setRoleCode("MANAGER");
		role.setRoleName("餐厅经理");
		role.setPermissions(new LinkedHashSet<>(List.of(permission)));
		User user = new User();
		user.setId(1L);
		user.setEmployeeNo("manager");
		user.setName("林经理");
		user.setPassword("ENCODED");
		user.setDeptId(1L);
		user.setDeptName("行政部");
		user.setWorkstation("A-01");
		user.setPhone("13800000001");
		user.setStatus(status);
		user.setRoles(new LinkedHashSet<>(List.of(role)));
		return user;
	}
}
