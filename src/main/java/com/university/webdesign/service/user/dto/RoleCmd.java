package com.university.webdesign.service.user.dto;

import lombok.Data;

/**
 * 角色维护入参。
 * <p>
 * 对应《对外方法表》M4-13 `POST /roles` 与 M4-15 `PUT /roles/{roleId}`：
 * 新增时 {@code roleCode} 必填且全局唯一；更新时只改名称与描述，角色编码不可变。
 */
@Data
public class RoleCmd
{
	/**
	 * 角色编码，全局唯一（如 {@code MANAGER}）
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
}
