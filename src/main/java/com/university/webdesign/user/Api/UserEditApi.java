package com.university.webdesign.user.Api;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 用户编辑接口
 */
@SuppressWarnings("UnusedDeclaration")
public interface UserEditApi
{
	/**
	 * 创建用户
	 * @param username 用户名
	 * @param password 密码
	 * @param Number 工号
	 * @param roleName 角色名
	 * @return 新创建的用户ID
	 */
	long createUser(@NonNull String username, @NonNull String password, @NonNull String Number, @NonNull String roleName);
	
	/**
	 * 批量创建用户
	 * @param users 用户数据列表
	 * @remark 用户数据列表中的每个用户数据必须包含用户名、密码、工号和角色名
	 * @remark 返回的用户id直接修改users列表中的userId字段
	 */
	void createUsers(@NonNull List<UserData> users);
	
	/**
	 * 更新用户信息
	 * @param userId 用户ID
	 * @param username 用户名
	 */
	void updateUserName(long userId, @NonNull String username);
	
	/**
	 * 更新用户密码
	 * @param userId 用户ID
	 * @param password 密码
	 */
	void updatePassword(long userId, @NonNull String password);
	
	/**
	 * 更新用户工号
	 * @param userId 用户ID
	 * @param Number 工号
	 */
	void updateNumber(long userId, @NonNull String Number);
	
	/**
	 * 更新用户角色
	 * @param userId 用户ID
	 * @param roleId 角色ID
	 */
	void updateRole(long userId, int roleId);
	
	/**
	 * 删除用户
	 * @param userId 用户ID
	 */
	void deleteUser(long userId);
	
	/**
	 * 根据用户名获取用户ID
	 * @param username 用户名
	 * @return 用户ID
	 */
	long getUserIdByUserName(@NonNull String username);
	
	/**
	 * 根据工号获取用户ID
	 * @param Number 工号
	 * @return 用户ID
	 */
	long getUserIdByNumber(@NonNull String Number);
}
