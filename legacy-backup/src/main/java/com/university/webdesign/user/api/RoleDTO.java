package com.university.webdesign.user.api;

import lombok.Data;

import java.util.List;

/**
 * 角色与权限映射DTO。
 */
@Data
public class RoleDTO
{
	/**
	 * 角色ID
	 */
	private Long roleId;

	/**
	 * 角色编码，例如 MANAGER、FINANCE
	 */
	private String code;

	/**
	 * 角色名称
	 */
	private String name;

	/**
	 * 角色说明
	 */
	private String description;

	/**
	 * 角色拥有的权限编码
	 */
	private List<String> permissionCodes;
}
