package com.university.webdesign.common;

/**
 * 业务异常。
 * <p>
 * 对应《对外方法表》1.4 约定：service 层业务校验失败时抛出本异常，由
 * {@link GlobalExceptionHandler} 统一映射为带 {@link ErrorCode} 的 {@link Result}。
 */
public class BusinessException extends RuntimeException
{
	/**
	 * 统一错误码
	 */
	private final ErrorCode errorCode;

	/**
	 * @param errorCode 统一错误码
	 */
	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage());
	}

	/**
	 * @param errorCode 统一错误码
	 * @param message   面向用户的提示信息
	 */
	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * @param errorCode 统一错误码
	 * @param message   面向用户的提示信息
	 * @param cause     根因
	 */
	public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	/**
	 * @return 统一错误码
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}

	/**
	 * 参数非法
	 *
	 * @param message 提示信息
	 * @return 业务异常
	 */
	public static BusinessException paramInvalid(String message) {
		return new BusinessException(ErrorCode.PARAM_INVALID, message);
	}

	/**
	 * 资源不存在
	 *
	 * @param message 提示信息
	 * @return 业务异常
	 */
	public static BusinessException notFound(String message) {
		return new BusinessException(ErrorCode.NOT_FOUND, message);
	}

	/**
	 * 越权访问
	 *
	 * @param message 提示信息
	 * @return 业务异常
	 */
	public static BusinessException forbidden(String message) {
		return new BusinessException(ErrorCode.FORBIDDEN, message);
	}

	/**
	 * 业务状态冲突
	 *
	 * @param message 提示信息
	 * @return 业务异常
	 */
	public static BusinessException stateConflict(String message) {
		return new BusinessException(ErrorCode.STATE_CONFLICT, message);
	}

	/**
	 * 超出时间窗口
	 *
	 * @param message 提示信息
	 * @return 业务异常
	 */
	public static BusinessException outOfWindow(String message) {
		return new BusinessException(ErrorCode.OUT_OF_TIME_WINDOW, message);
	}

	/**
	 * 违反业务规则
	 *
	 * @param message 提示信息
	 * @return 业务异常
	 */
	public static BusinessException ruleViolation(String message) {
		return new BusinessException(ErrorCode.RULE_VIOLATION, message);
	}
}
