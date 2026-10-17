package com.university.webdesign.user.api;

import lombok.Data;

/**
 * 用户查询条件。
 */
@Data
public class UserQueryData
{
	/**
	 * 用户名、姓名、工位或电话关键字
	 */
	private String keyword;

	/**
	 * 所属部门
	 */
	private String department;

	/**
	 * 角色ID
	 */
	private Long roleId;

	/**
	 * 账号是否可用
	 */
	private Boolean enabled;
}
