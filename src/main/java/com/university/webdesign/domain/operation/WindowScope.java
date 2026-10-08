package com.university.webdesign.domain.operation;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;

/**
 * 时间窗口配置的作用域。
 * <p>
 * 对应《重构实施规范》第 3 节的 `service_window.scope` 字段：
 * {@code DEPT}（按部门配置）优先级高于 {@code GLOBAL}（全局配置），
 * {@link com.university.webdesign.service.operation.ServiceWindowService#get} 按此优先级取值。
 */
public enum WindowScope
{
	/**
	 * 全局配置（默认）
	 */
	GLOBAL("全局"),

	/**
	 * 部门级配置（优先级更高）
	 */
	DEPT("部门");

	/**
	 * 中文文案
	 */
	private final String text;

	WindowScope(String text) {
		this.text = text;
	}

	/**
	 * @return 中文文案
	 */
	public String getText() {
		return text;
	}

	/**
	 * 解析作用域编码，忽略大小写；为空时取全局
	 *
	 * @param scope 作用域编码，可为空
	 * @return 作用域
	 */
	public static WindowScope parse(String scope) {
		if (scope == null || scope.isBlank()) {
			return GLOBAL;
		}
		for (WindowScope value : values()) {
			if (value.name().equalsIgnoreCase(scope.trim())) {
				return value;
			}
		}
		throw new BusinessException(ErrorCode.PARAM_INVALID, "未知的时间窗口作用域：" + scope + "（可选 GLOBAL / DEPT）");
	}
}
