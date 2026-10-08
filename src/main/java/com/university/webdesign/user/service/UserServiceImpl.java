package com.university.webdesign.user.service;

import com.university.webdesign.user.api.LoginData;
import com.university.webdesign.user.api.PasswordUpdateData;
import com.university.webdesign.user.api.UserDTO;
import com.university.webdesign.user.api.UserImportData;
import com.university.webdesign.user.api.UserImportResultDTO;
import com.university.webdesign.user.api.UserQueryData;
import com.university.webdesign.user.api.UserRegisterData;
import com.university.webdesign.user.api.UserUpdateData;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 【临时占位实现】为了让应用在其他模块完成前能够启动而添加。
 * 用户与权限服务（User & Reporting Service）应由对应负责人实现，
 * 完成后请用真正的实现类替换本类。
 * <p>
 * 本类目前没有角色/授权表可查，所有权限判定一律按“最小权限”返回 false。
 * 为了让订单模块的经理/财务越权测试仍能覆盖到“有权限”分支，
 * 这里保留一个仅供测试使用的全局开关 {@link #grantAllRolesForTest(boolean)}。
 */
@Service
public class UserServiceImpl implements UserService {

	/**
	 * 【仅测试使用】是否把所有角色都视为已授予。
	 * <p>
	 * 用户中心尚未落地角色数据，测试无法通过真实数据造出经理/财务角色，
	 * 因此由 {@code StubServicesTestConfiguration.grantAllRoles} 在测试中切换本开关。
	 * 生产环境不会被赋值，固定为 false。
	 */
	private static volatile boolean ALL_ROLES_FOR_TEST = false;

	/**
	 * 【仅测试使用】开关“授予所有角色”，用完请在测试结束（{@code @AfterEach}）复位。
	 *
	 * @param granted true 表示所有用户都拥有任意角色
	 */
	public static void grantAllRolesForTest(boolean granted) {
		ALL_ROLES_FOR_TEST = granted;
	}

	@Override
	public UserDTO login(LoginData loginData) {
		throw new UnsupportedOperationException("用户服务尚未实现");
	}

	@Override
	public void logout() {
		// 暂不处理
	}

	@Override
	public UserDTO createUser(UserRegisterData registerData) {
		throw new UnsupportedOperationException("用户服务尚未实现");
	}

	@Override
	public List<UserDTO> query(UserQueryData queryData) {
		return List.of();
	}

	@Override
	public UserDTO getUserInfo(Long userId) {
		return null;
	}

	@Override
	public UserDTO updateUser(UserUpdateData updateData) {
		throw new UnsupportedOperationException("用户服务尚未实现");
	}

	@Override
	public void changePassword(PasswordUpdateData passwordUpdateData) {
		// 暂不处理
	}

	@Override
	public void resetPassword(Long userId, String newPassword) {
		// 暂不处理
	}

	@Override
	public void setEnabled(Long userId, boolean enabled) {
		// 暂不处理
	}

	@Override
	public void assignRoles(Long userId, List<Long> roleIds) {
		// 暂不处理
	}

	@Override
	public void deleteUser(Long userId) {
		// 暂不处理
	}

	@Override
	public UserImportResultDTO importUsers(UserImportData importData) {
		throw new UnsupportedOperationException("用户服务尚未实现");
	}

	@Override
	public List<String> getPermissionCodes(Long userId) {
		return userId != null && ALL_ROLES_FOR_TEST ? List.of("*") : List.of();
	}

	@Override
	public boolean hasPermission(Long userId, String permissionCode) {
		return userId != null && ALL_ROLES_FOR_TEST;
	}

	@Override
	public boolean hasAnyRole(Long userId, String... roleCodes) {
		//todo: 用户中心尚未落地角色与授权表，这里按“最小权限”处理。
		// 生产环境下 ALL_ROLES_FOR_TEST 恒为 false，即任何经理/财务操作都会被判定为无权限（安全侧默认拒绝）；
		// 接入真实角色数据后，本方法应改为查询用户的角色编码是否命中 roleCodes。
		return userId != null && ALL_ROLES_FOR_TEST;
	}
}
