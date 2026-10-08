package com.university.webdesign.user;

import com.university.webdesign.user.api.AuthenticatedUser;
import com.university.webdesign.user.api.LoginData;
import com.university.webdesign.user.api.UserDTO;
import com.university.webdesign.user.service.UserAuthenticationProvider;
import com.university.webdesign.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 用户中心接入 Spring Security 的认证适配器测试。
 */
class UserAuthenticationProviderTests
{
	private final UserService userService = Mockito.mock(UserService.class);

	private final UserAuthenticationProvider provider = new UserAuthenticationProvider(userService);

	@Test
	@DisplayName("认证成功：UserId写入主体，角色和权限转换为授权码")
	void authenticateShouldExposeUserIdRolesAndPermissions() {
		UserDTO user = user(1L, "manager", "林经理",
			List.of("MANAGER"), List.of("user:view", "report:export"));
		when(userService.login(any(LoginData.class))).thenReturn(user);

		Authentication authentication = provider.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated("manager", "admin123"));

		assertThat(authentication.isAuthenticated()).isTrue();
		assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		assertThat(principal.userId()).isEqualTo(1L);
		assertThat(principal.name()).isEqualTo("林经理");
		assertThat(authentication.getAuthorities())
			.extracting(authority -> authority.getAuthority())
			.contains("ROLE_MANAGER", "user:view", "report:export");
	}

	@Test
	@DisplayName("认证失败：用户名或密码错误")
	void authenticateShouldRejectBadCredentials() {
		when(userService.login(any(LoginData.class)))
			.thenThrow(new IllegalArgumentException("用户名或密码错误"));

		assertThatThrownBy(() -> provider.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated("manager", "wrong")))
			.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	@DisplayName("认证失败：停用账号返回Spring Security可识别的停用异常")
	void authenticateShouldRejectDisabledAccount() {
		when(userService.login(any(LoginData.class)))
			.thenThrow(new IllegalArgumentException("账号已停用，请联系管理员"));

		assertThatThrownBy(() -> provider.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated("manager", "admin123")))
			.isInstanceOf(DisabledException.class)
			.hasMessageContaining("停用");
	}

	private UserDTO user(Long userId, String username, String name, List<String> roleCodes,
		List<String> permissionCodes) {
		UserDTO user = new UserDTO();
		user.setUserId(userId);
		user.setUsername(username);
		user.setName(name);
		user.setRoleCodes(roleCodes);
		user.setPermissionCodes(permissionCodes);
		return user;
	}
}
