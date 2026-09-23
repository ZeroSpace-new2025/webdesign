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
	public Result<String> login(@RequestBody LoginData loginData) {
		return Result.success("Login successful");
	}
	
	@PostMapping("/logout")
	public Result<String> logout() {
		return Result.success("Logout successful");
	}
	
	@PostMapping("/register")
	public Result<String> register(@RequestBody UserRegisterData userRegisterData) {
		return Result.success("Register successful");
	}
	
	@PostMapping("/update/password")
	public Result<String> updatePassword(@RequestBody String newPassword) {
		return Result.success("Password update successful");
	}
	
	@PostMapping("/info")
	public Result<UserDTO> getUserInfo(@RequestBody long id) {
		return Result.success(new UserDTO());
	}
}
