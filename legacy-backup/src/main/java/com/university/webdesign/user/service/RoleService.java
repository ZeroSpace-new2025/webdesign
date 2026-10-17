package com.university.webdesign.user.service;

import com.university.webdesign.user.api.PermissionDTO;
import com.university.webdesign.user.api.RoleDTO;

import java.util.List;

/**
 * 角色与权限服务。
 */
public interface RoleService
{
	/**
	 * 查询全部角色。
	 */
	List<RoleDTO> query();

	/**
	 * 获取角色详情。
	 */
	RoleDTO getRole(Long roleId);

	/**
	 * 创建角色并设置权限。
	 */
	RoleDTO createRole(RoleDTO roleDTO);

	/**
	 * 更新角色基础信息。
	 */
	RoleDTO updateRole(RoleDTO roleDTO);

	/**
	 * 删除角色。
	 */
	void deleteRole(Long roleId);

	/**
	 * 查询系统权限目录。
	 */
	List<PermissionDTO> queryPermissions();

	/**
	 * 重置角色拥有的权限。
	 */
	RoleDTO updatePermissions(Long roleId, List<String> permissionCodes);
}
