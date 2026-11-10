package com.university.webdesign.impl.user.support;

import java.util.List;

/**
 * JWT 载荷（payload）。
 * <p>
 * 对应《重构实施规范》第 5 节 M4 的约定：payload 携带 {@code userId / employeeNo / name / deptId /
 * roles / permCodes / exp}，另有随机唯一标识 {@code jti}（不解析进本记录，只用于保证令牌字符串唯一）。
 * <p>
 * <b>载荷里的 {@code roles} / {@code permCodes} 只是签发那一刻的快照，不是授权依据</b>：
 * {@code AuthService.verifyToken} 会按 {@code userId} 回查库里的账号状态与角色权限，
 * 否则“停用账号”“撤销权限点”都必须等令牌自然过期（默认 12 小时）才生效。
 * 载荷仍然携带这些字段，是为了排查问题时能直接看出令牌签发时的权限快照。
 *
 * @param userId     用户ID
 * @param employeeNo 工号
 * @param name       姓名
 * @param deptId     部门ID
 * @param roles      角色编码集合（签发时快照）
 * @param permCodes  权限点编码集合（签发时快照）
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
