package com.university.webdesign.user.api;

import lombok.Data;

import java.util.List;

/**
 * 创建员工账号的数据。
 */
@Data
public class UserRegisterData
{
	/**
	 * 登录用户名
	 */
	private String username;

	/**
	 * 初始密码
	 */
	private String password;

	/**
	 * 员工姓名
	 */
	private String name;

	/**
	 * 所属部门
	 */
	private String department;

	/**
	 * 工位
	 */
	private String workstation;

	/**
	 * 联系电话
	 */
	private String phone;

	/**
	 * 用户角色ID
	 */
	private List<Long> roleIds;
}
