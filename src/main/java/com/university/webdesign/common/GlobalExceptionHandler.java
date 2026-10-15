package com.university.webdesign.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（全模块唯一）。
 * <p>
 * 对应《对外方法表》1.4 / 1.5 约定：api 层的参数校验失败统一转 40001，
 * service 层抛出的 {@link BusinessException} 按其错误码映射，其余异常归入 50000。
 * <p>
 * 历史上 {@code common} 与 {@code config} 包各有一份处理器，边界互相交叉；
 * 重构后只保留本类，删除 {@code config.GlobalExceptionHandler}。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{
	/**
	 * 业务异常：按错误码原样返回
	 *
	 * @param exception 业务异常
	 * @return 失败响应或错误页面
	 */
	@ExceptionHandler(BusinessException.class)
	public Object handleBusiness(BusinessException exception) {
		return failureOrErrorPage(exception.getErrorCode().getCode(), exception.getMessage());
	}

	/**
	 * 请求体校验失败（{@code @Valid} + {@code @RequestBody}）
	 *
	 * @param exception 校验异常
	 * @return 失败响应（40001）
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Result<Void> handleInvalidBody(MethodArgumentNotValidException exception) {
		return invalidParam(exception.getBindingResult().getFieldErrors().stream()
				.map(this::describe)
				.collect(Collectors.joining("；")));
	}

	/**
	 * 表单/查询参数绑定校验失败
	 *
	 * @param exception 绑定异常
	 * @return 失败响应（40001）
	 */
	@ExceptionHandler(BindException.class)
	public Result<Void> handleBind(BindException exception) {
		return invalidParam(exception.getFieldErrors().stream()
				.map(this::describe)
				.collect(Collectors.joining("；")));
	}

	/**
	 * 缺少必填请求参数
	 *
	 * @param exception 缺参异常
	 * @return 失败响应（40001）
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public Result<Void> handleMissingParam(MissingServletRequestParameterException exception) {
		return invalidParam("缺少必填参数：" + exception.getParameterName());
	}

	/**
	 * 参数类型不匹配（如日期格式错误）
	 *
	 * @param exception 类型不匹配异常
	 * @return 失败响应（40001）
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		return invalidParam("参数格式不正确：" + exception.getName());
	}

	/**
	 * 请求体不可解析（JSON 格式错误）
	 *
	 * @param exception 解析异常
	 * @return 失败响应（40001）
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public Result<Void> handleUnreadable(HttpMessageNotReadableException exception) {
		return invalidParam("请求体格式不正确");
	}

	/**
	 * 请求 Content-Type 不被支持（例如对 JSON 接口发了表单）
	 *
	 * @param exception 媒体类型异常
	 * @return 失败响应（40001）
	 */
	@ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
	public Result<Void> handleUnsupportedMediaType(
			org.springframework.web.HttpMediaTypeNotSupportedException exception) {
		return invalidParam("不支持的请求格式：" + exception.getContentType()
				+ "，请使用 application/json");
	}

	/**
	 * 请求方法不支持（例如对 GET 接口发了 POST）
	 *
	 * @param exception 方法异常
	 * @return 失败响应（40001）
	 */
	@ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
	public Result<Void> handleMethodNotSupported(
			org.springframework.web.HttpRequestMethodNotSupportedException exception) {
		return invalidParam("请求方法不支持：" + exception.getMethod());
	}

	/**
	 * 访问了不存在的接口或静态资源
	 *
	 * @param exception 资源不存在异常
	 * @return 失败响应（40400）
	 */
	@ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
	public Result<Void> handleNoResource(
			org.springframework.web.servlet.resource.NoResourceFoundException exception) {
		return Result.failure(ErrorCode.NOT_FOUND.getCode(), "接口或资源不存在：" + exception.getResourcePath());
	}

	/**
	 * 参数非法（页面控制器与 service 层沿用的 JDK 异常）
	 *
	 * @param exception 异常
	 * @return 失败响应（40001）
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public Object handleIllegalArgument(IllegalArgumentException exception) {
		return failureOrErrorPage(ErrorCode.PARAM_INVALID.getCode(), exception.getMessage());
	}

	/**
	 * 状态非法（页面控制器与 service 层沿用的 JDK 异常）
	 *
	 * @param exception 异常
	 * @return 失败响应（40902）
	 */
	@ExceptionHandler(IllegalStateException.class)
	public Object handleIllegalState(IllegalStateException exception) {
		return failureOrErrorPage(ErrorCode.STATE_CONFLICT.getCode(), exception.getMessage());
	}

	/**
	 * 兜底：未知异常记日志并返回 50000，避免把堆栈暴露给前端
	 *
	 * @param exception 异常
	 * @return 失败响应（50000）
	 */
	@ExceptionHandler(Exception.class)
	public Object handleUnexpected(Exception exception) {
		log.error("未预期的异常", exception);
		return failureOrErrorPage(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
	}

	// ------------------------------------------------------------------ 内部方法

	/**
	 * 按请求类型选择错误输出形态
	 * <p>
	 * {@code /api/**} 一律返回 {@link Result} JSON；页面请求返回 {@code error} 视图。
	 * 之前所有异常都强行返回 JSON，导致页面请求出错时 Spring 抛出
	 * {@code HttpMessageNotWritableException: No converter for [LinkedHashMap] with preset Content-Type 'text/html'}
	 * ——用户看到的不是错误原因，而是一层二次异常。
	 *
	 * @param code    错误码
	 * @param message 错误信息
	 * @return JSON 失败响应或错误页面
	 */
	private Object failureOrErrorPage(int code, String message) {
		String path = currentRequestPath();
		if (path != null && path.startsWith("/api/")) {
			return Result.failure(code, message);
		}
		ModelAndView view = new ModelAndView("error");
		view.addObject("status", code);
		view.addObject("message", message == null || message.isBlank() ? "处理请求时发生错误" : message);
		return view;
	}

	/**
	 * 取当前请求路径
	 *
	 * @return 请求路径；不在 Web 上下文时返回 null
	 */
	private String currentRequestPath() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes instanceof ServletRequestAttributes servletAttributes) {
			return servletAttributes.getRequest().getRequestURI();
		}
		return null;
	}

	private Result<Void> invalidParam(String message) {
		return Result.failure(ErrorCode.PARAM_INVALID.getCode(),
				message == null || message.isBlank() ? ErrorCode.PARAM_INVALID.getMessage() : message);
	}

	private String describe(FieldError error) {
		return error.getField() + " " + error.getDefaultMessage();
	}
}
