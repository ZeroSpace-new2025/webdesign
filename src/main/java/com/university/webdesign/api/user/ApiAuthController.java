package com.university.webdesign.api.user;

import com.university.webdesign.common.Result;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.service.user.AuthService;
import com.university.webdesign.service.user.dto.CurrentUserVO;
import com.university.webdesign.service.user.dto.LoginVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 REST 控制器（基础路径 {@code /api/v1/user/auth}）。
 * <p>
 * 对应《对外方法表》5.1.1：登录（唯一免鉴权接口）、登出、获取当前登录用户、修改密码。
 * 本类只做协议转换，业务规则全部在 {@link AuthService} 中。
 * <p>
 * 身份来源：token 由 {@code config.AuthInterceptor} 解析后写入
 * {@link com.university.webdesign.common.UserContextHolder}，
 * 本类通过 {@link #currentUser()} 读取，**不接受**前端自报的 {@code userId}。
 * 登录接口在白名单内，无需 {@code @RequiresPerm}；其余接口由拦截器统一要求登录态。
 */
@RestController
@RequestMapping("/api/v1/user/auth")
public class ApiAuthController extends ApiUserSupport
{
	private final AuthService authService;

	/**
	 * 构造器注入
	 *
	 * @param authService 认证服务
	 */
	public ApiAuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * M4-01 登录（唯一免鉴权接口）
	 *
	 * @param request 工号 + 密码
	 * @return 令牌、有效期与用户信息
	 */
	@PostMapping("/login")
	public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
		return Result.success(authService.login(request.getEmployeeNo(), request.getPassword()));
	}

	/**
	 * M4-02 登出：把当前 token 加入黑名单
	 *
	 * @param authorization 请求头 {@code Authorization: Bearer <token>}，可选
	 * @return 空结果
	 */
	@PostMapping("/logout")
	public Result<Void> logout(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		authService.logout(authorization);
		return Result.ok();
	}

	/**
	 * M4-03 获取当前登录用户：前端刷新页面后恢复上下文
	 *
	 * @return 当前用户信息（含角色与权限点）
	 */
	@GetMapping("/me")
	public Result<CurrentUserVO> me() {
		UserContext context = currentUser();
		return Result.success(authService.currentUser(context.userId()));
	}

	/**
	 * M4-04 修改密码（本人）
	 *
	 * @param request 旧密码 + 新密码
	 * @return 空结果
	 */
	@PutMapping("/password")
	public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
		UserContext context = currentUser();
		authService.changePassword(context.userId(), request.getOldPassword(), request.getNewPassword());
		return Result.ok();
	}
}
