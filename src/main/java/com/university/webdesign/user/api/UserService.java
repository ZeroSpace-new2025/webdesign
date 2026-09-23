package com.university.webdesign.user.api;

import org.springframework.stereotype.Component;

/**
 * 用户与权限服务（跨模块能力契约）
 * <p>
 * //todo 确认：本接口为跨模块依赖的接口骨架，请用户与报表中心（王家豪）确认后补齐。
 * 订单模块目前最需要其中的两项能力：
 * <ul>
 *     <li>登录认证：拿到“当前登录用户”，替代前端自报的 operatorId；</li>
 *     <li>角色判断：区分餐厅经理、厨房主管、配餐员、财务、员工，用于经理删单、报表查询等越权控制。</li>
 * </ul>
 */
@Component
public interface UserService
{
	/**
	 * 登录认证
	 *
	 * @param loginData 登录信息
	 * @return 认证令牌或会话标识；失败时返回 null
	 */
	String login(LoginData loginData);
	
	/**
	 * 退出登录
	 *
	 * @return 是否成功
	 */
	boolean logout();
	
	/**
	 * 注册用户
	 *
	 * @param registerData 注册信息
	 * @return 新用户ID
	 */
	Long register(UserRegisterData registerData);
	
	/**
	 * 修改密码
	 *
	 * @param userId      用户ID
	 * @param newPassword 新密码
	 * @return 是否成功
	 */
	boolean updatePassword(Long userId, String newPassword);
	
	/**
	 * 查询用户信息
	 *
	 * @param userId 用户ID
	 * @return 用户信息；不存在时返回 null
	 */
	UserDTO getUserInfo(Long userId);
	
	/**
	 * 判断用户是否拥有指定角色
	 *
	 * @param userId   用户ID
	 * @param roleCode 角色编码（如 MANAGER、KITCHEN_SUPERVISOR、SERVER、FINANCE、EMPLOYEE）
	 * @return 拥有该角色返回 true
	 */
	boolean hasRole(Long userId, String roleCode);
}
