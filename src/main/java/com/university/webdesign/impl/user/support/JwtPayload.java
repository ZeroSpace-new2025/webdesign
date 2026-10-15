package com.university.webdesign.impl.user.support;

import java.util.List;

/**
 * JWT 载荷（payload）。
 * <p>
 * 对应《重构实施规范》第 5 节 M4 的约定：payload 携带 {@code userId / employeeNo / name / deptId /
 * roles / permCodes / exp}。用户信息随令牌携带，{@code verifyToken} 无需再查库即可构造
 * {@link com.university.webdesign.common.UserContext}，避免每个请求都打数据库。
 *
 * @param userId     用户ID
 * @param employeeNo 工号
 * @param name       姓名
 * @param deptId     部门ID
 * @param roles      角色编码集合
 * @param permCodes  权限点编码集合
 * @param issuedAt   签发时间（Unix 秒）
 * @param expiresAt  到期时间（Unix 秒）
 */
public record JwtPayload(
		Long userId,
		String employeeNo,
		String name,
		Long deptId,
		List<String> roles,
		List<String> permCodes,
		long issuedAt,
		long expiresAt)
{
}
