package com.university.webdesign.service.user.dto;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 角色视图对象。
 * <p>
 * 对应《对外方法表》M4-14 / M4-15 的 {@code RoleVO}：角色基础信息 + 已授权限点编码集合。
 * 角色只有名称一个业务字段（`role.name`），权限位图展开为权限点编码（{@code PermissionEnum.permCode}）集合。
 */
@Data
public class RoleVO
{
	/**
	 * 角色ID
	 */
	private Long roleId;

	/**
	 * 角色名称，全局唯一
	 */
	private String name;

	/**
	 * 已授权的权限点编码集合
	 */
	private Set<String> permCodes = new LinkedHashSet<>();
}
