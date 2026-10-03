package com.university.webdesign.user.api;

import lombok.Data;

/**
 * 权限目录项。
 */
@Data
public class PermissionDTO
{
	/**
	 * 权限ID
	 */
	private Long permissionId;

	/**
	 * 权限编码
	 */
	private String code;

	/**
	 * 权限名称
	 */
	private String name;

	/**
	 * 权限说明
	 */
	private String description;
}
