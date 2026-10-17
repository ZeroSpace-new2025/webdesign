package com.university.webdesign.user.api;

import lombok.Data;

/**
 * 修改密码的数据。
 */
@Data
public class PasswordUpdateData
{
	/**
	 * 用户ID
	 */
	private Long userId;

	/**
	 * 当前密码
	 */
	private String currentPassword;

	/**
	 * 新密码
	 */
	private String newPassword;
}
