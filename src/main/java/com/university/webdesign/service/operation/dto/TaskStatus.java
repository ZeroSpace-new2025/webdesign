package com.university.webdesign.service.operation.dto;

/**
 * 配送任务状态。
 * <p>
 * 对应《对外方法表》M3-11 的状态机：`PENDING → DELIVERING → DELIVERED / EXCEPTION`。
 * 禁止跳跃（如 `PENDING` 直接到 `DELIVERED`）与回退（已完结状态不可再改），非法流转抛 40902。
 */
public enum TaskStatus
{
	/**
	 * 待配送
	 */
	PENDING("待配送"),

	/**
	 * 配送中
	 */
	DELIVERING("配送中"),

	/**
	 * 已送达
	 */
	DELIVERED("已送达"),

	/**
	 * 配送异常
	 */
	EXCEPTION("配送异常");

	/**
	 * 中文文案
	 */
	private final String text;

	TaskStatus(String text) {
		this.text = text;
	}

	/**
	 * @return 中文文案
	 */
	public String getText() {
		return text;
	}

	/**
	 * 是否已完结（不允许再流转）
	 *
	 * @return 已送达或异常时返回 true
	 */
	public boolean isFinished() {
		return this == DELIVERED || this == EXCEPTION;
	}

	/**
	 * 判断能否从当前状态流转到目标状态
	 *
	 * @param target 目标状态
	 * @return 允许流转时返回 true
	 */
	public boolean canTransitionTo(TaskStatus target) {
		if (target == null || target == this || isFinished()) {
			return false;
		}
		return switch (this) {
			case PENDING -> target == DELIVERING || target == EXCEPTION;
			case DELIVERING -> target == DELIVERED || target == EXCEPTION;
			case DELIVERED, EXCEPTION -> false;
		};
	}

	/**
	 * 解析状态字符串，忽略大小写
	 *
	 * @param status 状态字符串
	 * @return 配送状态
	 */
	public static TaskStatus parse(String status) {
		if (status == null || status.isBlank()) {
			throw new com.university.webdesign.common.BusinessException(
					com.university.webdesign.common.ErrorCode.PARAM_INVALID, "配送状态不能为空");
		}
		for (TaskStatus value : values()) {
			if (value.name().equalsIgnoreCase(status.trim())) {
				return value;
			}
		}
		throw new com.university.webdesign.common.BusinessException(
				com.university.webdesign.common.ErrorCode.PARAM_INVALID, "未知的配送状态：" + status);
	}
}
