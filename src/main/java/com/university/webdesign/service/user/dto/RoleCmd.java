package com.university.webdesign.service.user.dto;

import lombok.Data;

/**
 * 角色维护入参。
 * <p>
 * 对应《对外方法表》M4-13 `POST /roles` 与 M4-15 `PUT /roles/{roleId}`：
 * 角色只有名称一个业务字段，新增时必填且全局唯一；更新时传名称即改名（预置角色不可改名）。
 */
@Data
public class RoleCmd
{
	/**
	 * 角色名称，全局唯一（预置角色取 {@code RoleCodes} 的取值，如 {@code MANAGER}）
	 */
	private String name;
}
