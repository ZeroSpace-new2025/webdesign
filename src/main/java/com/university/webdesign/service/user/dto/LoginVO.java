package com.university.webdesign.service.user.dto;

import lombok.Data;

/**
 * 登录结果视图。
 * <p>
 * 对应《对外方法表》M4-01 `POST /auth/login` 的返回：{@code token}、{@code expiresIn}、{@code userInfo}。
 */
@Data
public class LoginVO
{
	/**
	 * JWT 访问令牌，前端放入请求头 {@code Authorization: Bearer <token>}
	 */
	private String token;

	/**
	 * 令牌有效期（秒），供前端提前刷新
	 */
	private Long expiresIn;

	/**
	 * 令牌到期时间（Unix 毫秒）
	 */
	private Long expiresAt;

	/**
	 * 登录用户信息（含角色与权限点）
	 */
	private CurrentUserVO userInfo;

	/**
	 * 构造登录结果
	 *
	 * @param token     访问令牌
	 * @param expiresIn 有效秒数
	 * @param expiresAt 到期毫秒时间戳
	 * @param userInfo  登录用户信息
	 * @return 登录结果
	 */
	public static LoginVO of(String token, long expiresIn, long expiresAt, CurrentUserVO userInfo) {
		LoginVO vo = new LoginVO();
		vo.setToken(token);
		vo.setExpiresIn(expiresIn);
		vo.setExpiresAt(expiresAt);
		vo.setUserInfo(userInfo);
		return vo;
	}
}
