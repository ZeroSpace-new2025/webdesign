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
 */
@Service
public class UserServiceImpl implements UserService {

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
		return List.of();
	}

	@Override
	public boolean hasPermission(Long userId, String permissionCode) {
		return false;
	}
}
