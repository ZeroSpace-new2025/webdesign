package com.university.webdesign.api.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增/更新角色请求（M4-13、M4-15）。
 * <p>
 * 新增时 {@code roleCode} 与 {@code roleName} 必填；更新时只使用 {@code roleName} 与
 * {@code description}，角色编码不可修改。
 */
@Data
public class RoleRequest
{
	/**
	 * 角色编码（如 {@code MANAGER}），新增时必填
	 */
	private String roleCode;

	/**
	 * 角色名称
	 */
	@NotBlank(message = "角色名称不能为空")
	private String roleName;

	/**
	 * 角色描述
	 */
	private String description;
}
