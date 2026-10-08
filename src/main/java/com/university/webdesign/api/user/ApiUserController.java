package com.university.webdesign.api.user;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.PermCodes;
import com.university.webdesign.common.Result;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.domain.user.UserStatus;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.ImportResultVO;
import com.university.webdesign.service.user.dto.UserCreateCmd;
import com.university.webdesign.service.user.dto.UserQuery;
import com.university.webdesign.service.user.dto.UserUpdateCmd;
import com.university.webdesign.service.user.dto.UserVO;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户（员工）维护 REST 控制器（基础路径 {@code /api/v1/user}）。
 * <p>
 * 对应《对外方法表》5.1.2 的员工维护部分共 9 个接口：
 * 创建、分页、详情、更新、停用、批量导入、导入模板、更新状态、分配角色。
 * 本类只做协议转换，业务规则全部在 {@link UserService} 中。
 * <p>
 * 鉴权：由 {@code config.AuthInterceptor} 按 {@link RequiresPerm} 声明统一校验
 * （员工维护需要 {@link PermCodes#USER_MANAGE}，员工导入同时接受 {@link PermCodes#ROLE_MANAGE}）。
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiresPerm(PermCodes.USER_MANAGE)
public class ApiUserController extends ApiUserSupport
{
	private final UserService userService;

	/**
	 * 构造器注入
	 *
	 * @param userService 用户服务
	 */
	public ApiUserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * M4-05 创建用户
	 *
	 * @param cmd 创建入参
	 * @return 新用户ID
	 */
	@PostMapping("/users")
	public Result<Long> create(@Valid @RequestBody UserCreateCmd cmd) {
		return Result.success(userService.create(cmd));
	}

	/**
	 * M4-06 分页查询用户
	 *
	 * @param query 查询条件（关键字、部门、工位、状态、分页）
	 * @return 分页用户列表
	 */
	@GetMapping("/users")
	public Result<PageResult<UserVO>> page(@ModelAttribute UserQuery query) {
		return Result.success(userService.page(query));
	}

	/**
	 * M4-07 查询用户详情
	 *
	 * @param userId 用户ID
	 * @return 用户详情（含角色）
	 */
	@GetMapping("/users/{userId}")
	public Result<UserVO> detail(@PathVariable("userId") Long userId) {
		return Result.success(userService.getById(userId));
	}

	/**
	 * M4-08 更新用户
	 *
	 * @param userId 用户ID
	 * @param cmd    更新入参
	 * @return 更新后的用户
	 */
	@PutMapping("/users/{userId}")
	public Result<UserVO> update(@PathVariable("userId") Long userId,
			@Valid @RequestBody UserUpdateCmd cmd) {
		userService.update(userId, cmd);
		return Result.success(userService.getById(userId));
	}

	/**
	 * M4-09 停用用户（逻辑停用，保留历史订单归属）
	 *
	 * @param userId 用户ID
	 * @return 空结果
	 */
	@DeleteMapping("/users/{userId}")
	public Result<Void> disable(@PathVariable("userId") Long userId) {
		userService.disable(userId);
		return Result.ok();
	}

	/**
	 * M4-10 批量导入员工
	 *
	 * @param file    导入文件（CSV / XLSX）
	 * @param deptId  统一指定的部门ID，可选
	 * @param roleIds 统一分配的角色ID，可选
	 * @return 逐行校验结果（支持部分成功）
	 */
	@PostMapping("/users/import")
	@RequiresPerm({PermCodes.USER_MANAGE, PermCodes.ROLE_MANAGE})
	public Result<ImportResultVO> importEmployees(
			@RequestPart("file") MultipartFile file,
			@RequestParam(value = "deptId", required = false) Long deptId,
			@RequestParam(value = "roleIds", required = false) List<Long> roleIds) {
		return Result.success(userService.importFromExcel(file, deptId, roleIds));
	}

	/**
	 * M4-11 下载导入模板（返回 CSV，UTF-8 BOM）
	 *
	 * @return 模板文件
	 */
	@GetMapping("/users/import/template")
	@RequiresPerm({PermCodes.USER_MANAGE, PermCodes.ROLE_MANAGE})
	public ResponseEntity<Resource> importTemplate() {
		Resource resource = userService.exportImportTemplate();
		String fileName = URLEncoder.encode("员工导入模板.csv", StandardCharsets.UTF_8).replace("+", "%20");
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"employee-import-template.csv\"; filename*=UTF-8''" + fileName)
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.body(resource);
	}

	/**
	 * M4-12 更新账号状态
	 *
	 * @param userId  用户ID
	 * @param request 目标状态 ACTIVE / DISABLED / LOCKED
	 * @return 更新后的用户
	 */
	@PutMapping("/users/{userId}/status")
	public Result<UserVO> changeStatus(@PathVariable("userId") Long userId,
			@Valid @RequestBody UserStatusRequest request) {
		userService.changeStatus(userId, UserStatus.parse(request.getStatus()));
		return Result.success(userService.getById(userId));
	}

	/**
	 * M4-20 分配用户角色（全量覆盖）
	 *
	 * @param userId  用户ID
	 * @param request 角色ID集合
	 * @return 更新后的用户
	 */
	@PutMapping("/users/{userId}/roles")
	public Result<UserVO> assignRoles(@PathVariable("userId") Long userId,
			@RequestBody RoleAssignRequest request) {
		userService.assignRoles(userId, request.getRoleIds());
		return Result.success(userService.getById(userId));
	}
}
