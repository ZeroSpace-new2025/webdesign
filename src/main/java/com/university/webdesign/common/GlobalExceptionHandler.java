package com.university.webdesign.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一转换业务异常。
 */
@RestControllerAdvice(basePackages = {
	"com.university.webdesign.user.api",
	"com.university.webdesign.reporting.api"
})
public class GlobalExceptionHandler
{
	/**
	 * 将可预期的业务错误转换为统一失败结果。
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public Result<Void> handleIllegalArgument(IllegalArgumentException exception) {
		return Result.failure(400, exception.getMessage());
	}

	/**
	 * 避免未处理异常直接返回Tomcat错误页。
	 */
	@ExceptionHandler(Exception.class)
	public Result<Void> handleException(Exception exception) {
		return Result.failure(500, "服务器处理失败：" + exception.getMessage());
	}
}
