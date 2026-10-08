package com.university.webdesign.service.user;

import com.university.webdesign.common.UserContext;
import com.university.webdesign.service.user.dto.CurrentUserVO;
import com.university.webdesign.service.user.dto.LoginVO;

/**
 * 认证与授权服务。
 * <p>
 * 对应《对外方法表》5.2 的 {@code AuthService}：JWT 签发与校验、当前用户上下文、
 * 改密、权限点判定。
 * <p>
 * 实现约束（见《重构实施规范》第 6 节的环形依赖说明）：
 * {@link #checkPermission} 与 {@link #verifyToken} **只读取本模块的用户/角色/权限数据**，
 * 不得依赖 M2/M3 的任何类型。
 */
public interface AuthService
{
	/**
	 * 登录（M4-01，唯一免鉴权接口）
	 * <p>
	 * 校验工号存在、账号为启用状态；BCrypt 比对密码；签发 HS256 JWT。
	 *
	 * @param employeeNo  工号
	 * @param rawPassword 明文密码
	 * @return 登录结果（令牌 + 有效期 + 用户信息）
	 */
	LoginVO login(String employeeNo, String rawPassword);

	/**
	 * 登出（M4-02）
	 * <p>
	 * 把当前 token 加入内存黑名单，TTL 取该 token 的剩余有效期。
	 *
	 * @param token 待失效的访问令牌
	 */
	void logout(String token);

	/**
	 * 获取当前登录用户（M4-03）
	 * <p>
	 * 查用户 + 角色 + 展开权限点集合，供前端恢复上下文与渲染菜单。
	 *
	 * @param userId 用户ID
	 * @return 当前用户视图
	 */
	CurrentUserVO currentUser(Long userId);

	/**
	 * 修改密码（M4-04）
	 * <p>
	 * 校验旧密码与密码强度，更新后使该用户此前签发的 token 全部失效。
	 *
	 * @param userId  用户ID
	 * @param oldPwd  旧密码
	 * @param newPwd  新密码
	 */
	void changePassword(Long userId, String oldPwd, String newPwd);

	/**
	 * 校验并解析令牌（供 api 层 {@code AuthInterceptor} 调用）
	 * <p>
	 * 解析 JWT 的 header.payload.signature、校验签名与有效期、检查登出黑名单，
	 * 返回可直接写入
	 * {@link com.university.webdesign.common.UserContextHolder} 的上下文。
	 *
	 * @param token 访问令牌
	 * @return 登录用户上下文
	 */
	UserContext verifyToken(String token);

	/**
	 * 权限点判定（供各模块 Service 调用）
	 *
	 * @param userId   用户ID
	 * @param permCode 权限点编码，如 {@code order:invalidate}
	 * @return 拥有该权限点时返回 true；用户不存在或入参为空时返回 false
	 */
	boolean checkPermission(Long userId, String permCode);
}
