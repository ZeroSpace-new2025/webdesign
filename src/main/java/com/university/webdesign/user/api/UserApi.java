package com.university.webdesign.user.api;
/*其他的API接口可以根据需要添加。修改后要立即提交。*/
/*应该只包含与用户相关的API接口，而不应该包含其他相关的接口。*/
/*请勿把内部实现暴露给外部。*/

import com.university.webdesign.common.Result;
import com.university.webdesign.user.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户API接口
 */
@RestController
@RequestMapping("/api/user")
public class UserApi
{
	private final UserService userService;

	public UserApi(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/login")
	public Result<UserDTO> login(@RequestBody LoginData loginData) {
		return Result.success(userService.login(loginData));
	}

	@PostMapping("/logout")
	public Result<Void> logout() {
		userService.logout();
		return Result.success(null);
	}

	@PostMapping("/register")
	public Result<UserDTO> register(@RequestBody UserRegisterData userRegisterData) {
		return Result.success(userService.createUser(userRegisterData));
	}

	@PostMapping("/update/password")
	public Result<Void> updatePassword(@RequestBody PasswordUpdateData passwordUpdateData) {
		userService.changePassword(passwordUpdateData);
		return Result.success(null);
	}

	@PostMapping("/info")
	public Result<UserDTO> getUserInfo(@RequestBody long id) {
		return Result.success(userService.getUserInfo(id));
	}
}
