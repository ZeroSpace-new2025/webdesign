package com.university.webdesign.common;

/**
 * {@link UserContext} 的 ThreadLocal 持有者。
 * <p>
 * 由认证拦截器在请求进入时写入、请求结束时清理；service 层通过
 * {@link #require()} 取当前登录用户，通过 {@link #get()} 取可空身份。
 * 定时任务等无请求上下文的场景下两个方法都可能返回空。
 */
public final class UserContextHolder
{
	private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

	private UserContextHolder() {
	}

	/**
	 * 写入当前登录用户
	 *
	 * @param context 登录用户上下文
	 */
	public static void set(UserContext context) {
		HOLDER.set(context);
	}

	/**
	 * 取当前登录用户
	 *
	 * @return 登录用户上下文；未登录时返回 null
	 */
	public static UserContext get() {
		return HOLDER.get();
	}

	/**
	 * 取当前登录用户，未登录直接抛 40100
	 *
	 * @return 登录用户上下文
	 */
	public static UserContext require() {
		UserContext context = HOLDER.get();
		if (context == null || context.userId() == null) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "请先登录");
		}
		return context;
	}

	/**
	 * 取当前登录用户ID
	 *
	 * @return 用户ID；未登录时返回 null
	 */
	public static Long currentUserId() {
		UserContext context = HOLDER.get();
		return context == null ? null : context.userId();
	}

	/**
	 * 清理，避免线程复用导致身份串号
	 */
	public static void clear() {
		HOLDER.remove();
	}
}
