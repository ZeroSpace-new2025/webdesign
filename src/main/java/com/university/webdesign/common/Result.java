package com.university.webdesign.common;

/**
 * 统一响应体。
 * <p>
 * 对应《对外方法表》1.4 通用约定：{@code Result<T>} 形如
 * {@code {"code": 0, "message": "ok", "data": {...}, "traceId": "..."}}，
 * {@code code = 0} 表示成功，具体错误码见 {@link ErrorCode}。
 * <p>
 * 兼容性说明：为了不破坏已上线的前端脚本（{@code static/js/app.js} 与页面内联脚本
 * 依赖 {@code result.success} 判定成败），在 {@code code} 之外额外保留了 {@code success}
 * 布尔字段；它与 {@code code == 0} 恒等，不参与业务判定。
 *
 * @param code    业务错误码，0 表示成功
 * @param success 是否成功，等价于 {@code code == 0}（兼容旧前端）
 * @param message 提示信息
 * @param data    业务数据，失败时为 null
 * @param traceId 请求追踪号，便于按日志排查
 * @param <T>     业务数据类型
 */
public record Result<T>(int code, boolean success, String message, T data, String traceId)
{
	/**
	 * 成功返回结果
	 *
	 * @param data 业务数据
	 * @param <T>  数据类型
	 * @return 成功响应
	 */
	public static <T> Result<T> success(T data) {
		return new Result<>(ErrorCode.SUCCESS.getCode(), true, "ok", data, TraceId.current());
	}

	/**
	 * 成功返回结果（无数据体）
	 *
	 * @param <T> 数据类型
	 * @return 成功响应
	 */
	public static <T> Result<T> ok() {
		return success(null);
	}

	/**
	 * 失败返回结果
	 *
	 * @param code    业务错误码
	 * @param message 错误信息
	 * @param <T>     数据类型
	 * @return 失败响应
	 */
	public static <T> Result<T> failure(int code, String message) {
		return new Result<>(code, false, message, null, TraceId.current());
	}

	/**
	 * 失败返回结果（按统一错误码表）
	 *
	 * @param errorCode 统一错误码
	 * @param message   错误信息，为空时取错误码默认描述
	 * @param <T>       数据类型
	 * @return 失败响应
	 */
	public static <T> Result<T> failure(ErrorCode errorCode, String message) {
		return failure(errorCode.getCode(),
				message == null || message.isBlank() ? errorCode.getMessage() : message);
	}
}
