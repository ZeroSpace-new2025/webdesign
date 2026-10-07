package com.university.webdesign.user.api;

import lombok.Data;

import java.util.List;

/**
 * 用户信息DTO，不包含密码等敏感字段。
 */
@Data
public class UserDTO
{
	/**
	 * 用户ID
	 */
	private Long userId;

	/**
	 * 登录用户名
	 */
	private String username;

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
	 * 账号是否可用
	 */
	private Boolean enabled;

	/**
	 * 用户拥有的角色ID
	 */
	private List<Long> roleIds;

	/**
	 * 用户拥有的角色编码
	 */
	private List<String> roleCodes;

	/**
	 * 用户拥有的权限编码
	 */
	private List<String> permissionCodes;
}
