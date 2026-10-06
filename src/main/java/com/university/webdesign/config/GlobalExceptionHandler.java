package com.university.webdesign.config;

import com.university.webdesign.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 * 将业务校验类异常转换为规范的 HTTP 状态码与统一返回结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 参数非法（如必填项为空、价格为负、数据重复）
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Result.failure(400, ex.getMessage()));
	}

	/**
	 * 状态冲突（如对已发布菜单做修改、停用菜品加入菜单）
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Result<Void>> handleIllegalState(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Result.failure(409, ex.getMessage()));
	}
}
