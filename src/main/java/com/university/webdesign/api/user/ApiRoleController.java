package com.university.webdesign.api.user;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.PermCodes;
import com.university.webdesign.common.Result;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.user.RoleService;
import com.university.webdesign.service.user.dto.PermVO;
import com.university.webdesign.service.user.dto.RoleCmd;
import com.university.webdesign.service.user.dto.RoleQuery;
import com.university.webdesign.service.user.dto.RoleVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 角色与权限 REST 控制器（基础路径 {@code /api/v1/user}）。
 * <p>
 * 对应《对外方法表》5.1.2 的角色权限部分共 7 个接口：
 * 新增角色、分页角色、更新角色、删除角色、权限点字典、配置角色权限、查询角色权限。
 * 本类只做协议转换，业务规则全部在 {@link RoleService} 中。
 * <p>
 * 鉴权：由 {@code config.AuthInterceptor} 按 {@link RequiresPerm}（{@link PermCodes#ROLE_MANAGE}）统一校验。
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiresPerm(PermCodes.ROLE_MANAGE)
public class ApiRoleController extends ApiUserSupport
{
	private final RoleService roleService;

	/**
	 * 构造器注入
	 *
	 * @param roleService 角色服务
	 */
	public ApiRoleController(RoleService roleService) {
		this.roleService = roleService;
	}

	/**
	 * M4-13 新增角色
	 *
	 * @param request 角色编码、名称、描述
	 * @return 新角色ID
	 */
	@PostMapping("/roles")
	public Result<Long> create(@Valid @RequestBody RoleRequest request) {
		RoleCmd cmd = new RoleCmd();
		cmd.setRoleCode(request.getRoleCode());
		cmd.setRoleName(request.getRoleName());
		cmd.setDescription(request.getDescription());
		return Result.success(roleService.create(cmd));
	}

	/**
	 * M4-14 分页查询角色
	 *
	 * @param query 查询条件
	 * @return 分页角色列表
	 */
	@GetMapping("/roles")
	public Result<PageResult<RoleVO>> page(@ModelAttribute RoleQuery query) {
		return Result.success(roleService.page(query));
	}

	/**
	 * M4-15 更新角色
	 *
	 * @param roleId  角色ID
	 * @param request 名称、描述
	 * @return 空结果
	 */
	@PutMapping("/roles/{roleId}")
	public Result<Void> update(@PathVariable("roleId") Long roleId,
			@Valid @RequestBody RoleRequest request) {
		RoleCmd cmd = new RoleCmd();
		cmd.setRoleCode(request.getRoleCode());
		cmd.setRoleName(request.getRoleName());
		cmd.setDescription(request.getDescription());
		roleService.update(roleId, cmd);
		return Result.ok();
	}

	/**
	 * M4-16 删除角色（存在关联用户时拒绝）
	 *
	 * @param roleId 角色ID
	 * @return 空结果
	 */
	@DeleteMapping("/roles/{roleId}")
	public Result<Void> delete(@PathVariable("roleId") Long roleId) {
		roleService.delete(roleId);
		return Result.ok();
	}

	/**
	 * M4-17 查询权限点列表（按模块分组）
	 *
	 * @param module 模块名，可选
	 * @return 权限点列表
	 */
	@GetMapping("/permissions")
	public Result<List<PermVO>> permissions(
			@RequestParam(value = "module", required = false) String module) {
		return Result.success(roleService.listPermissions(module));
	}

	/**
	 * M4-18 配置角色权限（全量覆盖）
	 *
	 * @param roleId  角色ID
	 * @param request 权限点编码集合
	 * @return 更新后的权限集合
	 */
	@PutMapping("/roles/{roleId}/permissions")
	public Result<Set<String>> updatePermissions(@PathVariable("roleId") Long roleId,
			@RequestBody PermissionAssignRequest request) {
		roleService.updatePermissions(roleId, request.getPermCodes());
		return Result.success(roleService.getPermissions(roleId));
	}

	/**
	 * M4-19 查询角色权限
	 *
	 * @param roleId 角色ID
	 * @return 权限点编码集合
	 */
	@GetMapping("/roles/{roleId}/permissions")
	public Result<Set<String>> getPermissions(@PathVariable("roleId") Long roleId) {
		return Result.success(roleService.getPermissions(roleId));
	}
}
