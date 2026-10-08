package com.university.webdesign.user.service;

import com.university.webdesign.user.api.AuthenticatedUser;
import com.university.webdesign.user.api.LoginData;
import com.university.webdesign.user.api.UserDTO;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 把用户中心的 {@link UserService} 登录能力接入 Spring Security。
 * <p>
 * 用户中心可以按 {@code app.storage} 选择 JPA 或内存实现，这里只依赖
 * {@link UserService} 契约，不直接访问用户仓储。
 */
@Component
public class UserAuthenticationProvider implements AuthenticationProvider
{
	private final UserService userService;

	public UserAuthenticationProvider(UserService userService) {
		this.userService = userService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName() == null ? "" : authentication.getName().trim();
		String password = authentication.getCredentials() == null
			? ""
			: authentication.getCredentials().toString();

		LoginData loginData = new LoginData();
		loginData.setUsername(username);
		loginData.setPassword(password);

		UserDTO user;
		try {
			user = userService.login(loginData);
		} catch (IllegalArgumentException exception) {
			String message = exception.getMessage();
			if (message != null && message.contains("停用")) {
				throw new DisabledException(message);
			}
			throw new BadCredentialsException("用户名或密码错误");
		}

		AuthenticatedUser principal = new AuthenticatedUser(
			user.getUserId(),
			user.getUsername(),
			user.getName(),
			user.getRoleCodes(),
			user.getPermissionCodes());
		return UsernamePasswordAuthenticationToken.authenticated(
			principal, null, buildAuthorities(user));
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}

	/**
	 * 角色转换为 Spring Security 的 ROLE_* 授权码，权限编码保持原样，
	 * 方便后续按方法级权限做细粒度控制。
	 */
	private List<GrantedAuthority> buildAuthorities(UserDTO user) {
		Set<String> authorities = new LinkedHashSet<>();
		if (user.getRoleCodes() != null) {
			user.getRoleCodes().stream()
				.filter(code -> code != null && !code.isBlank())
				.map(code -> "ROLE_" + code.trim().toUpperCase(Locale.ROOT))
				.forEach(authorities::add);
		}
		if (user.getPermissionCodes() != null) {
			user.getPermissionCodes().stream()
				.filter(code -> code != null && !code.isBlank())
				.map(String::trim)
				.forEach(authorities::add);
		}
		List<GrantedAuthority> result = new ArrayList<>();
		authorities.forEach(code -> result.add(new SimpleGrantedAuthority(code)));
		return result;
	}
}
