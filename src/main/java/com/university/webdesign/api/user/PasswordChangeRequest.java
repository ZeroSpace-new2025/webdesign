package com.university.webdesign.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求（M4-04）。
 * <p>
 * 本人改密：必须提供旧密码，新密码长度不少于 6 位。
 */
@Data
public class PasswordChangeRequest
{
	/**
	 * 旧密码
	 */
	@NotBlank(message = "旧密码不能为空")
	private String oldPassword;

	/**
	 * 新密码
	 */
	@NotBlank(message = "新密码不能为空")
	@Size(min = 6, message = "新密码长度不能少于 6 位")
	private String newPassword;
}
