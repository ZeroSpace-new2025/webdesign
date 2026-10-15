package com.university.webdesign.service.user.dto;

import lombok.Data;

/**
 * 更新员工信息入参。
 * <p>
 * 对应《对外方法表》M4-08 `PUT /users/{userId}`：只允许改姓名、部门、工位、电话；
 * 工号与密码不在本入参内（改密走 {@code AuthService.changePassword}）。
 * 工位变更会影响后续配送单，因此实现类需发 {@code UserUpdatedEvent}。
 */
@Data
public class UserUpdateCmd
{
	/**
	 * 员工姓名
	 */
	private String name;

	/**
	 * 部门ID
	 */
	private Long deptId;

	/**
	 * 部门名称快照
	 */
	private String deptName;

	/**
	 * 工位（供配送单使用）
	 */
	private String workstation;

	/**
	 * 联系电话
	 */
	private String phone;
}
