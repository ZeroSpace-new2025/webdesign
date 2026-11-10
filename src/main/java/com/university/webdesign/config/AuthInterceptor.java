package com.university.webdesign.config;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.service.user.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 认证与鉴权拦截器。
 * <p>
 * 对应《对外方法表》1.4 / 7：
 * <ol>
 *     <li>从 {@code Authorization: Bearer <token>} 解析登录态（也兼容页面侧的 `token` 请求头）；</li>
 *     <li>调用 {@link AuthService#verifyToken} 校验有效期与黑名单，把 {@link UserContext}
 *         写入 {@link UserContextHolder}，供 service 层做数据归属校验；</li>
 *     <li>按 {@link RequiresPerm} 声明校验权限点/角色，不足则 40300；</li>
 *     <li>请求结束务必清理 ThreadLocal，避免线程复用导致身份串号。</li>
 * </ol>
 * 白名单（登录、静态资源、H2 控制台等）在 {@code SecurityConfig} 中配置。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor
{
	/**
	 * 标准认证头
	 */
	private static final String AUTH_HEADER = "Authorization";

	/**
	 * Bearer 前缀
	 */
	private static final String BEARER_PREFIX = "Bearer ";

	/**
	 * 兼容页面跳到页面的场景：HTML 链接无法自定义请求头，因此 token 同时存 Cookie
	 */
	private static final String AUTH_COOKIE = "dsh_token";

	private final AuthService authService;

	public AuthInterceptor(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws java.io.IOException {
		String token = resolveToken(request);

		// 先写入登录上下文：白名单接口（登录）也会经过这里，token 为空则跳过
		if (token != null && !token.isBlank()) {
			UserContext context = authService.verifyToken(token);
			UserContextHolder.set(context);
		}

		// 页面一律要求登录：未登录直接回登录页，避免匿名访问业务页面（看到空数据或不该看的内容）。
		// /api/** 保持原来的语义：由接口自己的 @RequiresPerm 与 service 归属校验返回 40100 / 40300 JSON。
		if (!isApiRequest(request) && UserContextHolder.get() == null) {
			response.sendRedirect(request.getContextPath() + "/login");
			return false;
		}

		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}
		RequiresPerm requiresPerm = resolveAnnotation(handlerMethod);
		if (requiresPerm == null) {
			return true;
		}
		UserContext context = UserContextHolder.require();
		boolean roleMatched = requiresPerm.roles().length > 0 && context.hasAnyRole(requiresPerm.roles());
		boolean permMatched = Arrays.stream(requiresPerm.value()).anyMatch(context::hasPermission);
		if (!roleMatched && !permMatched) {
			// 页面与接口的提示分开写，用户看错误页时能一眼看出是“页面无权限”还是“接口无权限”
			throw BusinessException.forbidden(isApiRequest(request) ? "无权限访问该接口" : "无权限访问该页面");
		}
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		UserContextHolder.clear();
	}

	/**
	 * 解析请求中的 token，优先级：标准 Bearer 头 → 页面侧 token 头 → 认证 Cookie
	 * <p>
	 * Cookie 只为“页面跳页面”这种无法自定义请求头的场景兜底（页面控制器需要登录态渲染视图）；
	 * 纯 JSON 接口请使用 {@code Authorization: Bearer <token>}。
	 *
	 * @param request 请求
	 * @return token；不存在时返回 null
	 */
	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(AUTH_HEADER);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length()).trim();
		}
		String fallback = request.getHeader("token");
		if (fallback != null && !fallback.isBlank()) {
			return fallback.trim();
		}
		if (request.getCookies() != null) {
			for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
				if (AUTH_COOKIE.equals(cookie.getName()) && cookie.getValue() != null
						&& !cookie.getValue().isBlank()) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	/**
	 * 是否 JSON 接口请求
	 * <p>
	 * 本项目的对外接口统一挂在 `/api/v1/**` 下；其余路径都是页面、静态资源或 H2 控制台，
	 * 因此用路径前缀区分“页面”和“接口”即可，不需要额外的请求头约定。
	 *
	 * @param request 请求
	 * @return 接口请求返回 true
	 */
	private boolean isApiRequest(HttpServletRequest request) {
		return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
	}

	/**
	 * 取方法或类上的权限声明（方法优先）
	 *
	 * @param handlerMethod 处理方法
	 * @return 权限注解；未声明时返回 null
	 */
	private RequiresPerm resolveAnnotation(HandlerMethod handlerMethod) {
		RequiresPerm onMethod = handlerMethod.getMethodAnnotation(RequiresPerm.class);
		if (onMethod != null) {
			return onMethod;
		}
		return handlerMethod.getBeanType().getAnnotation(RequiresPerm.class);
	}
}
