package com.university.webdesign.user.Api;

/**
 * 角色接口
 */
@SuppressWarnings("UnusedDeclaration")
public interface RoleApi
{
	/**
	 * 创建角色
	 * @param roleName 角色名称
	 * @return 角色ID
	 */
	int createRole(String roleName);
	
	/**
	 * 更新角色名称
	 * @param roleId 角色ID
	 * @param roleName 新的角色名称
	 */
	void updateRoleName(long roleId, String roleName);
	
	/**
	 * 删除角色
	 * @param roleId 角色ID
	 */
	void deleteRole(long roleId);
	
	/**
	 * 获取角色ID
	 * @param roleName 角色名称
	 * @return 角色ID
	 */
	int getRoleId(String roleName);
	
	/**
	 * 添加角色权限
	 * @param roleId 角色ID
	 * @param permissionId 权限ID
	 */
	void addPermission(int roleId, int permissionId);
	
	/**
	 * 删除角色权限
	 * @param roleId 角色ID
	 * @param permissionId 权限ID
	 */
	void removePermission(int roleId, int permissionId);
}
