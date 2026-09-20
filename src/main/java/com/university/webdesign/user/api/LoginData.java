package com.university.webdesign.user.api;

import lombok.Data;

/**
 * 登录请求数据。
 */
@Data
public class LoginData
{
	/**
	 * 登录用户名
	 */
	private String username;

	/**
	 * 登录密码
	 */
	private String password;
}
