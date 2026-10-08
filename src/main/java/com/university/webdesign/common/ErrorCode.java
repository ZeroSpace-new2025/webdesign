package com.university.webdesign.common;

/**
 * 统一错误码。
 * <p>
 * 对应《对外方法表》1.5 统一错误码表，全模块共用；service 层业务校验失败时抛
 * {@link BusinessException}（携带本枚举），由 {@link GlobalExceptionHandler} 统一映射成
 * {@link Result} 响应。
 */
public enum ErrorCode
{
	/**
	 * 成功
	 */
	SUCCESS(0, "ok"),

	/**
	 * 参数校验失败
	 */
	PARAM_INVALID(40001, "参数校验失败"),

	/**
	 * 未认证：token 缺失或过期
	 */
	UNAUTHENTICATED(40100, "未认证或登录已过期"),

	/**
	 * 无权限：角色不匹配、越权访问他人数据
	 */
	FORBIDDEN(40300, "无权限执行该操作"),

	/**
	 * 资源不存在
	 */
	NOT_FOUND(40400, "资源不存在"),

	/**
	 * 唯一性冲突：菜品名重复、工号重复、手机号重复
	 */
	DUPLICATED(40901, "唯一性冲突"),

	/**
	 * 业务状态冲突：菜单未发布即下单、乐观锁版本冲突、删除有用户的角色
	 */
	STATE_CONFLICT(40902, "业务状态冲突"),

	/**
	 * 超出时间窗口：已过订餐截止时间仍下单、未到配餐开始时间即打配送单
	 */
	OUT_OF_TIME_WINDOW(42201, "超出时间窗口"),

	/**
	 * 违反业务规则：每人每天仅限一张订单、数量 ≤ 0
	 */
	RULE_VIOLATION(42202, "违反业务规则"),

	/**
	 * 数据保护限制：菜单已发布，不允许物理删除菜品
	 */
	DATA_PROTECTED(42203, "数据保护限制"),

	/**
	 * 服务内部错误
	 */
	INTERNAL_ERROR(50000, "服务内部错误");

	/**
	 * 错误码
	 */
	private final int code;

	/**
	 * 默认描述
	 */
	private final String message;

	ErrorCode(int code, String message) {
		this.code = code;
		this.message = message;
	}

	/**
	 * @return 错误码
	 */
	public int getCode() {
		return code;
	}

	/**
	 * @return 默认描述
	 */
	public String getMessage() {
		return message;
	}
}
