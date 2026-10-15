package com.university.webdesign.api.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（M4-01）。
 * <p>
 * 唯一免鉴权接口的入参：工号 + 明文密码。登录成功后返回 JWT，
 * 前端放入请求头 {@code Authorization: Bearer <token>}。
 */
@Data
public class LoginRequest
{
	/**
	 * 工号（登录账号）
	 */
	@NotBlank(message = "工号不能为空")
	private String employeeNo;

	/**
	 * 明文密码
	 */
	@NotBlank(message = "密码不能为空")
	private String password;
}
