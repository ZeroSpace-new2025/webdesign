package com.university.webdesign.user.api;

import com.university.webdesign.common.Result;
import com.university.webdesign.user.service.RoleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色与权限API。
 */
@RestController
@RequestMapping("/api/role")
public class RoleApi
{
	private final RoleService roleService;

	public RoleApi(RoleService roleService) {
		this.roleService = roleService;
	}

	/**
	 * 查询角色及其权限编码。
	 */
	@GetMapping
	public Result<List<RoleDTO>> query() {
		return Result.success(roleService.query());
	}

	/**
	 * 权限目录用于角色编辑界面的多选列表。
	 */
	@GetMapping("/permission")
	public Result<List<PermissionDTO>> queryPermissions() {
		return Result.success(roleService.queryPermissions());
	}

	/**
	 * 新角色可以同时携带初始权限。
	 */
	@PostMapping
	public Result<RoleDTO> create(@RequestBody RoleDTO roleDTO) {
		return Result.success(roleService.createRole(roleDTO));
	}

	/**
	 * 更新角色基础信息。
	 */
	@PutMapping
	public Result<RoleDTO> update(@RequestBody RoleDTO roleDTO) {
		return Result.success(roleService.updateRole(roleDTO));
	}

	/**
	 * 删除非系统预置角色。
	 */
	@DeleteMapping("/{id}")
	public Result<Void> delete(@PathVariable("id") Long id) {
		roleService.deleteRole(id);
		return Result.success(null);
	}

	/**
	 * 覆盖式重置角色的权限集合。
	 */
	@PutMapping("/{id}/permission")
	public Result<RoleDTO> updatePermissions(@PathVariable("id") Long id, @RequestBody List<String> permissionCodes) {
		return Result.success(roleService.updatePermissions(id, permissionCodes));
	}
}
