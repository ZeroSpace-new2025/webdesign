package com.university.webdesign.service.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建员工账号入参。
 * <p>
 * 对应《对外方法表》M4-05 `POST /users`：管理员维护员工账号（姓名、部门、工位、电话），
 * 员工无需自行注册。工号（{@code employeeNo}）是唯一键，也是登录账号。
 */
@Data
public class UserCreateCmd
{
	/**
	 * 工号（登录账号），全局唯一
	 */
	@NotBlank(message = "工号不能为空")
	private String employeeNo;

	/**
	 * 员工姓名
	 */
	@NotBlank(message = "姓名不能为空")
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

	/**
	 * 初始密码，为空时使用默认初始密码 {@code 123456}
	 */
	private String initialPassword;

	/**
	 * 初始分配的角色ID集合
	 */
	private List<Long> roleIds;
}
