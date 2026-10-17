package com.university.webdesign.user.api;
/*其他的API接口可以根据需要添加。修改后要立即提交。*/
/*应该只包含与用户相关的API接口，而不应该包含其他相关的接口。*/
/*请勿把内部实现暴露给外部。*/

import com.university.webdesign.common.AuthSession;
import com.university.webdesign.common.Result;
import com.university.webdesign.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 用户API接口
 */
@RestController
@RequestMapping("/api/user")
public class UserApi
{
	private final UserService userService;

	public UserApi(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 校验账号密码，并把用户ID写入HttpSession。
	 */
	@PostMapping("/login")
	public Result<UserDTO> login(@RequestBody LoginData loginData, HttpSession session) {
		UserDTO user = userService.login(loginData);
		AuthSession.setUserId(session, user.getUserId());
		return Result.success(user);
	}

	/**
	 * 同时清理业务会话和Servlet Session。
	 */
	@PostMapping("/logout")
	public Result<Void> logout(HttpSession session) {
		userService.logout();
		AuthSession.clear(session);
		return Result.success(null);
	}

	/**
	 * 管理员创建员工账号。
	 */
	@PostMapping("/register")
	public Result<UserDTO> register(@RequestBody UserRegisterData userRegisterData) {
		return Result.success(userService.createUser(userRegisterData));
	}

	/**
	 * 查询条件为空时返回全部用户。
	 */
	@PostMapping("/query")
	public Result<List<UserDTO>> query(@RequestBody(required = false) UserQueryData queryData) {
		return Result.success(userService.query(queryData));
	}

	/**
	 * 更新员工基础资料和角色关系。
	 */
	@PutMapping
	public Result<UserDTO> update(@RequestBody UserUpdateData updateData) {
		return Result.success(userService.updateUser(updateData));
	}

	/**
	 * 删除员工账号，历史报表数据不会被删除。
	 */
	@DeleteMapping("/{id}")
	public Result<Void> delete(@PathVariable("id") Long id) {
		userService.deleteUser(id);
		return Result.success(null);
	}

	/**
	 * 启用或停用账号。
	 */
	@PatchMapping("/{id}/status")
	public Result<Void> updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Boolean> body) {
		userService.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
		return Result.success(null);
	}

	/**
	 * 覆盖式设置用户角色。
	 */
	@PutMapping("/{id}/roles")
	public Result<Void> assignRoles(@PathVariable("id") Long id, @RequestBody List<Long> roleIds) {
		userService.assignRoles(id, roleIds);
		return Result.success(null);
	}

	/**
	 * 用户修改自己的密码，需要提供当前密码。
	 */
	@PostMapping("/change-password")
	public Result<Void> changePassword(@RequestBody PasswordUpdateData passwordUpdateData) {
		userService.changePassword(passwordUpdateData);
		return Result.success(null);
	}

	/**
	 * 管理员重置指定用户密码。
	 */
	@PostMapping("/reset-password/{id}")
	public Result<Void> resetPassword(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
		userService.resetPassword(id, body.get("newPassword"));
		return Result.success(null);
	}

	/**
	 * 获取当前登录用户。
	 */
	@GetMapping("/me")
	public Result<UserDTO> currentUser(HttpSession session) {
		Long userId = AuthSession.getUserId(session);
		if (userId == null) {
			throw new IllegalArgumentException("登录状态已失效");
		}
		return Result.success(userService.getUserInfo(userId));
	}

	/**
	 * 获取指定用户详情。
	 */
	@GetMapping("/{id}")
	public Result<UserDTO> getUserInfo(@PathVariable("id") Long id) {
		return Result.success(userService.getUserInfo(id));
	}

	/**
	 * Multipart文件在控制层转换为导入请求，避免把Web类型传入Service。
	 */
	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result<UserImportResultDTO> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
		UserImportData importData = new UserImportData();
		importData.setFileName(file.getOriginalFilename());
		importData.setInputStream(file.getInputStream());
		return Result.success(userService.importUsers(importData));
	}

	/**
	 * 下载可直接由Excel打开的CSV模板。
	 */
	@GetMapping("/import/template")
	public ResponseEntity<byte[]> importTemplate() {
		String content = "\uFEFFusername,password,name,department,workstation,phone,roles\n"
			+ "employee001,123456,示例员工,研发部,R-01,13800000000,EMPLOYEE\n";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user-import-template.csv");
		return ResponseEntity.ok().headers(headers).body(content.getBytes(StandardCharsets.UTF_8));
	}
}
