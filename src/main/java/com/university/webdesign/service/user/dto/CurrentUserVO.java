package com.university.webdesign.service.user.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 当前登录用户视图。
 * <p>
 * 对应《对外方法表》M4-03 `GET /auth/me`：前端刷新页面后据此恢复上下文，
 * 同时提供 {@code permCodes} 供前端按权限渲染菜单与按钮。
 */
@Data
public class CurrentUserVO
{
	/**
	 * 用户ID
	 */
	private Long userId;

	/**
	 * 工号
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
	 * 工位
	 */
	private String workstation;

	/**
	 * 联系电话
	 */
	private String phone;

	/**
	 * 账号状态编码
	 */
	private String status;

	/**
	 * 账号状态中文文案
	 */
	private String statusText;

	/**
	 * 角色编码集合
	 */
	private List<String> roles = new ArrayList<>();

	/**
	 * 展开后的权限点编码集合
	 */
	private List<String> permCodes = new ArrayList<>();
}
