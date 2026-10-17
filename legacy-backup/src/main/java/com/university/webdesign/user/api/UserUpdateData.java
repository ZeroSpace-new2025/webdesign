package com.university.webdesign.user.api;

import lombok.Data;

import java.util.List;

/**
 * 更新员工基础信息的数据。
 */
@Data
public class UserUpdateData
{
	/**
	 * 用户ID
	 */
	private Long userId;

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
