package com.university.webdesign.api.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增/更新角色请求（M4-13、M4-15）。
 * <p>
 * 角色只有名称一个业务字段：新增时必填且全局唯一；更新时传名称即改名，
 * 预置角色（{@code RoleCodes.ALL}）不允许改名，权限另走
 * {@code PUT /roles/{roleId}/permissions}。
 */
@Data
public class RoleRequest
{
	/**
	 * 角色名称（如 {@code MANAGER}），全局唯一
	 */
	@NotBlank(message = "角色名称不能为空")
	private String name;
}
