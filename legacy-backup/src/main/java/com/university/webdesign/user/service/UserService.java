package com.university.webdesign.user.service;

import com.university.webdesign.user.api.LoginData;
import com.university.webdesign.user.api.PasswordUpdateData;
import com.university.webdesign.user.api.UserDTO;
import com.university.webdesign.user.api.UserImportData;
import com.university.webdesign.user.api.UserImportResultDTO;
import com.university.webdesign.user.api.UserQueryData;
import com.university.webdesign.user.api.UserRegisterData;
import com.university.webdesign.user.api.UserUpdateData;

import java.util.List;

/**
 * 用户认证、员工维护与授权服务。
 */
public interface UserService
{
	/**
	 * 校验用户凭据并返回用户、角色和权限信息。
	 */
	UserDTO login(LoginData loginData);

	/**
	 * 注销当前登录用户。
	 */
	void logout();

	/**
	 * 创建员工账号。
	 */
	UserDTO createUser(UserRegisterData registerData);

	/**
	 * 查询用户列表。
	 */
	List<UserDTO> query(UserQueryData queryData);

	/**
	 * 获取用户详情。
	 */
	UserDTO getUserInfo(Long userId);

	/**
	 * 更新员工基础信息和角色。
	 */
	UserDTO updateUser(UserUpdateData updateData);

	/**
	 * 修改密码，必须校验当前密码。
	 */
	void changePassword(PasswordUpdateData passwordUpdateData);

	/**
	 * 由管理员重置密码。
	 */
	void resetPassword(Long userId, String newPassword);

	/**
	 * 启用或停用员工账号。
	 */
	void setEnabled(Long userId, boolean enabled);

	/**
	 * 为用户分配角色。
	 */
	void assignRoles(Long userId, List<Long> roleIds);

	/**
	 * 删除用户。历史订单应保留，不随用户删除而删除。
	 */
	void deleteUser(Long userId);

	/**
	 * 从Excel批量导入员工信息。
	 */
	UserImportResultDTO importUsers(UserImportData importData);

	/**
	 * 获取用户拥有的全部权限编码。
	 */
	List<String> getPermissionCodes(Long userId);

	/**
	 * 判断用户是否拥有指定权限。
	 */
	boolean hasPermission(Long userId, String permissionCode);

	/**
	 * 判断用户是否拥有指定角色中的任意一个。
	 * <p>
	 * 本方法是订单与交易核心依赖的跨模块契约：订单模块用它校验“经理删单”“经理/财务查他人历史与消费”，
	 * 角色编码由订单模块约定为 {@code MANAGER}（餐厅经理）与 {@code FINANCE}（财务管理），
	 * 详见 AGENTS.md 第 2 节与第 4 节的权限边界约定。
	 *
	 * @param userId    用户ID
	 * @param roleCodes 允许的角色编码，满足其一即返回 true
	 * @return 拥有其中任一角色时返回 true
	 */
	boolean hasAnyRole(Long userId, String... roleCodes);
}
