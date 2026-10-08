package com.university.webdesign.service.user.dto;

import lombok.Data;

/**
 * 角色视图对象。
 * <p>
 * 对应《对外方法表》M4-14 / M4-15 的 {@code RoleVO}：角色基础信息 + 已授权限点编码集合。
 */
@Data
public class RoleVO
{
	/**
	 * 角色ID
	 */
	private Long roleId;

	/**
	 * 角色编码
	 */
	private String roleCode;

	/**
	 * 角色名称
	 */
	private String roleName;

	/**
	 * 角色描述
	 */
	private String description;

	/**
	 * 已授权的权限点编码集合
	 */
	private java.util.Set<String> permCodes = new java.util.LinkedHashSet<>();
}
