package com.university.webdesign.config;

import com.university.webdesign.common.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 登录上下文的兜底清理过滤器。
 * <p>
 * 身份放在 {@link UserContextHolder} 的 ThreadLocal 里，必须在请求结束后一定清掉，
 * 否则 Tomcat 线程复用时会“串号”——下一个请求可能带着上一个用户的身份，
 * 未登录的请求甚至会被当成已登录用户通过页面校验。
 * <p>
 * 为什么不能只靠 {@code AuthInterceptor.afterCompletion}：拦截器只有在
 * {@code preHandle} 返回 true（或正常执行完）时才会回调 {@code afterCompletion}；
 * 一旦某个拦截器在 {@code preHandle} 里抛异常（例如 {@code @RequiresPerm} 越权 40300），
 * 认证拦截器自己的 {@code afterCompletion} 不会被调用，身份就留在 ThreadLocal 里了。
 * 过滤器包住整个请求（含异常与错误页渲染），在最外层用 finally 清理，才是可靠的兜底。
 */
@Component
public class UserContextCleanupFilter extends OncePerRequestFilter
{
	/**
	 * 执行过滤器链，请求结束时清空登录上下文
	 *
	 * @param request  请求
	 * @param response 响应
	 * @param chain    过滤器链
	 * @throws ServletException Servlet 异常
	 * @throws IOException      IO 异常
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		try {
			chain.doFilter(request, response);
		} finally {
			UserContextHolder.clear();
		}
	}
}
