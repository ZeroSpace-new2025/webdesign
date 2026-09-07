package com.university.webdesign.user.Api;

import org.jspecify.annotations.NonNull;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

@SuppressWarnings("UnusedDeclaration")
public interface UserLoginApi extends PermissionEvaluator
{
	/**
	 * 用户是否登录
	 *
	 * @return true表示已登录，false表示未登录
	 */
	boolean isLogin();
	
	/**
	 * 用户是否有指定权限
	 *
	 * @param permissionId 权限ID
	 * @return true表示有权限，false表示无权限
	 * @remark 不要在其他系统调用
	 */
	boolean hasPermission(int permissionId);
	
	@Override
	boolean hasPermission(@NonNull Authentication authentication,@NonNull Object targetDomainObject,
	                       @NonNull Object permission);
	
	/**
	 * 用户是否有指定权限
	 *
	 * @param permissionName 权限名称
	 * @return true表示有权限，false表示无权限
	 * @remark 不要在其他系统调用
	 */
	boolean hasPermission( @NonNull String permissionName);
}