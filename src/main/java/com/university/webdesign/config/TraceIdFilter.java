package com.university.webdesign.config;

import com.university.webdesign.common.TraceId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求追踪号过滤器。
 * <p>
 * 对应《对外方法表》1.4 约定：响应体 {@code Result.traceId} 用于串联日志。
 * 优先复用调用方传入的 {@code X-Trace-Id} 请求头，否则新生成一个，并写回响应头。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter
{
	/**
	 * 追踪号请求/响应头名称
	 */
	public static final String HEADER = "X-Trace-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = request.getHeader(HEADER);
		if (traceId == null || traceId.isBlank()) {
			traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		}
		request.setAttribute(TraceId.REQUEST_ATTRIBUTE, traceId);
		response.setHeader(HEADER, traceId);
		filterChain.doFilter(request, response);
	}
}
