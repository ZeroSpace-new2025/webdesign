package com.university.webdesign.user.Api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

@Data
@NoArgsConstructor
public class UserData
{
	private long userId = 1L;
	private String username = "null";
	private String password = "null";
	private String number = "null";
	private String roleName = "null";
	
	/**
	 * 用户数据类
	 * @param userId 用户ID
	 * @param username 用户名
	 * @param password 密码
	 * @param number 工号
	 * @param roleName 角色名
	 */
	public UserData(long userId, @NonNull String username,@NonNull String password,@NonNull String number,
					@NonNull String roleName)
	{
		this.userId = userId;
		this.username = username;
		this.password = password;
		this.number = number;
		this.roleName = roleName;
	}
	
	/**
	 * 用户数据类
	 * @param username 用户名
	 * @param password 密码
	 * @param number 工号
	 * @param roleName 角色名
	 */
	public UserData(@NonNull String username, @NonNull String password, @NonNull String number, @NonNull String roleName)
	{
		this.username = username;
		this.password = password;
		this.number = number;
		this.roleName = roleName;
	}
}

