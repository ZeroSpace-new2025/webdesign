package com.university.webdesign.common;

/**
 * 通用返回结果类
 */
public record Result<T>(int code, boolean success, String message, T data)
{
	/**
	 * 成功返回结果
	 *
	 * @param data 返回数据
	 * @param <T>  数据类型
	 * @return 返回结果
	 */
	public static <T> Result<T> success(T data) {
		return new Result<>(200, true, "操作成功", data);
	}
	
	/**
	 * 失败返回结果
	 *
	 * @param code    错误代码
	 * @param message 错误信息
	 * @param <T>     数据类型
	 * @return 返回结果
	 */
	public static <T> Result<T> failure(int code, String message) {
		return new Result<>(code, false, message, null);
	}
}
