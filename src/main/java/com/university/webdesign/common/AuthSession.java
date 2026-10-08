package com.university.webdesign.common;

import jakarta.servlet.http.HttpSession;

/**
 * 当前登录会话信息。
 */
public final class AuthSession
{
	public static final String USER_ID = "LOGIN_USER_ID";

	private AuthSession() {
	}

	/**
	 * 从Session中读取登录用户ID。
	 */
	public static Long getUserId(HttpSession session) {
		if (session == null) {
			return null;
		}
		Object value = session.getAttribute(USER_ID);
		return value instanceof Long userId ? userId : null;
	}

	/**
	 * 登录成功后写入当前用户ID。
	 */
	public static void setUserId(HttpSession session, Long userId) {
		session.setAttribute(USER_ID, userId);
	}

	/**
	 * 注销时销毁整个Session。
	 */
	public static void clear(HttpSession session) {
		if (session != null) {
			session.invalidate();
		}
	}
}
