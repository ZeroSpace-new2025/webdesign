package com.university.webdesign.common;

import com.university.webdesign.common.enums.PermissionEnum;

import java.util.List;

/**
 * 当前登录用户上下文。
 * <p>
 * 对应《对外方法表》1.4 身份传递约定：由认证拦截器解析 token 后写入
 * {@link UserContextHolder}（ThreadLocal），service 层据此做数据归属与权限校验，
 * 不再依赖前端传入的 {@code operatorId}。
 *
 * @param userId        用户ID
 * @param employeeNo    工号
 * @param name          姓名
 * @param deptId        部门ID
 * @param workstation   工位
 * @param phone         联系电话
 * @param roles         角色名称集合（`role.name`，预置角色取 {@code RoleCodes} 的取值）
 * @param permCodes     权限点编码集合（角色权限的展开结果）
 */
public record UserContext(
		Long userId,
		String employeeNo,
		String name,
		Long deptId,
		String workstation,
		String phone,
		List<String> roles,
		List<String> permCodes)
{
	/**
	 * 判断是否拥有指定角色中的任意一个
	 *
	 * @param roleCodes 角色名称（`role.name`）
	 * @return 命中任一角色时返回 true
	 */
	public boolean hasAnyRole(String... roleCodes) {
		if (roles == null || roles.isEmpty() || roleCodes == null) {
			return false;
		}
		for (String expected : roleCodes) {
			if (expected == null) {
				continue;
			}
			for (String actual : roles) {
				if (expected.equalsIgnoreCase(actual)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 判断是否拥有指定权限点
	 *
	 * @param permission 权限点枚举
	 * @return 拥有时返回 true
	 */
	public boolean hasPermission(PermissionEnum permission) {
		return permission != null && hasPermission(permission.displayCode());
	}

	/**
	 * 判断是否拥有指定权限点
	 * <p>
	 * 入参是 JWT 载荷里的权限点编码（如 {@code report:view}），
	 * 与前端 {@code permCodes} 同一口径。
	 *
	 * @param permCode 权限点编码
	 * @return 拥有时返回 true
	 */
	public boolean hasPermission(String permCode) {
		return permCode != null && permCodes != null
				&& permCodes.stream().anyMatch(code -> permCode.equalsIgnoreCase(code));
	}
}
