package com.university.webdesign.service.user.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户视图对象。
 * <p>
 * 对应《对外方法表》M4-06 / M4-07 / M4-08 的 {@code UserVO}：基础信息 + 已分配角色，
 * 不包含密码等敏感字段。
 */
@Data
public class UserVO
{
	/**
	 * 用户ID
	 */
	private Long userId;

	/**
	 * 工号（登录账号）
	 */
	private String employeeNo;

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

	/**
	 * 账号状态编码 ACTIVE / DISABLED / LOCKED
	 */
	private String status;

	/**
	 * 账号状态中文文案
	 */
	private String statusText;

	/**
	 * 已分配角色
	 */
	private List<RoleVO> roles = new ArrayList<>();

	/**
	 * 已分配角色ID集合
	 */
	private List<Long> roleIds = new ArrayList<>();

	/**
	 * 角色编码集合
	 */
	private List<String> roleCodes = new ArrayList<>();
}
