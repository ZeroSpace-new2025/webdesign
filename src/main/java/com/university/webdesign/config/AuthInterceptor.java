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
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String token = resolveToken(request);

		// 先写入登录上下文：白名单接口（登录）也会经过这里，token 为空则跳过
		if (token != null && !token.isBlank()) {
			UserContext context = authService.verifyToken(token);
			UserContextHolder.set(context);
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
			throw BusinessException.forbidden("无权限访问该接口");
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
