package com.university.webdesign.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 请求追踪号（traceId）。
 * <p>
 * 优先复用 {@code TraceIdFilter} 写入请求属性的追踪号；在非 Web 上下文（如定时任务、
 * 单元测试）中回退为新生成的短 ID，保证 {@link Result#traceId()} 始终有值。
 */
public final class TraceId
{
	/**
	 * 请求属性名，与 {@code TraceIdFilter} 保持一致
	 */
	public static final String REQUEST_ATTRIBUTE = "dsh.traceId";

	private TraceId() {
	}

	/**
	 * 取得当前请求的追踪号
	 *
	 * @return 追踪号；无 Web 上下文时返回临时生成的追踪号
	 */
	public static String current() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes instanceof ServletRequestAttributes servletAttributes) {
			HttpServletRequest request = servletAttributes.getRequest();
			Object existing = request.getAttribute(REQUEST_ATTRIBUTE);
			if (existing instanceof String traceId && !traceId.isBlank()) {
				return traceId;
			}
		}
		return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}
}
