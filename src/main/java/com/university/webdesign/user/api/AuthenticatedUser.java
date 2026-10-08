package com.university.webdesign.user.api;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;

/**
 * Spring Security 认证成功后的用户主体。
 * <p>
 * 只保存身份与授权编码，不包含密码；控制层可从认证上下文直接读取当前用户ID。
 *
 * @param userId          用户ID
 * @param username        登录用户名
 * @param name            员工姓名
 * @param roleCodes       角色编码，例如 MANAGER / FINANCE
 * @param permissionCodes 权限编码，例如 user:view / report:export
 */
public record AuthenticatedUser(
	Long userId,
	String username,
	String name,
	List<String> roleCodes,
	List<String> permissionCodes) implements Principal, Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 复制授权集合，避免认证主体被外部修改。
	 */
	public AuthenticatedUser {
		roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
		permissionCodes = permissionCodes == null ? List.of() : List.copyOf(permissionCodes);
	}

	@Override
	public String getName() {
		return username;
	}
}
