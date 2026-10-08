package com.university.webdesign.domain.user;

/**
 * 用户账号状态。
 * <p>
 * 对应《对外方法表》M4-12 `PUT /users/{userId}/status` 的取值：启用、停用、锁定。
 * 停用只做逻辑停用，不做物理删除，保证历史订单与报表仍能追溯到员工归属。
 */
public enum UserStatus
{
	/**
	 * 启用：可正常登录与点餐
	 */
	ACTIVE("启用"),

	/**
	 * 停用：管理员逻辑停用，保留历史数据归属
	 */
	DISABLED("停用"),

	/**
	 * 锁定：连续登录失败或被管理员锁定，禁止登录
	 */
	LOCKED("锁定");

	/**
	 * 中文文案，供页面与报表展示
	 */
	private final String text;

	UserStatus(String text) {
		this.text = text;
	}

	/**
	 * @return 中文文案
	 */
	public String getText() {
		return text;
	}

	/**
	 * 解析状态编码，忽略大小写
	 *
	 * @param code 状态编码，可为 null
	 * @return 状态；无法识别时返回 null
	 */
	public static UserStatus parse(String code) {
		if (code == null || code.isBlank()) {
			return null;
		}
		String normalized = code.trim();
		for (UserStatus value : values()) {
			if (value.name().equalsIgnoreCase(normalized) || value.text.equals(normalized)) {
				return value;
			}
		}
		return null;
	}
}
