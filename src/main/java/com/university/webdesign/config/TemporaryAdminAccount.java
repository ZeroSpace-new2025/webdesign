package com.university.webdesign.config;

import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.enums.PermissionEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 临时硬编码管理员账号。
 * <p>
 * 用于解决开发初期数据库没初始化、或者 M4 模块还没写完时，系统能有一个兜底的管理员账号登录。
 * 只要工号和密码匹配，就能直接签发最高权限的 Token。
 */
@Component
public class TemporaryAdminAccount
{
	/**
	 * 临时管理员的固定用户ID：负数，避免与数据库自增主键冲突
	 */
	public static final Long USER_ID = -1L;

	/**
	 * 临时管理员展示名
	 */
	public static final String NAME = "临时管理员";

	/**
	 * 登录工号（可配置）
	 */
	private final String employeeNo;

	/**
	 * 登录密码（明文比对，仅课程演示使用）
	 */
	private final String password;

	/**
	 * 固定权限点：全部细粒度权限点
	 */
	private final List<String> permCodes;

	/**
	 * 构造器注入配置
	 *
	 * @param employeeNo 工号，默认 {@code admin}
	 * @param password   密码，默认 {@code admin123}
	 */
	public TemporaryAdminAccount(
			@Value("${app.temporary-admin.employee-no:admin}") String employeeNo,
			@Value("${app.temporary-admin.password:admin123}") String password) {
		this.employeeNo = employeeNo;
		this.password = password;
		this.permCodes = PermissionEnum.dictionary().stream()
				.map(PermissionEnum::displayCode)
				.toList();
	}

	/**
	 * 工号 + 密码是否命中临时管理员
	 *
	 * @param employeeNo  登录工号（已去空白）
	 * @param rawPassword 明文密码
	 * @return 命中时返回 true
	 */
	public boolean matches(String employeeNo, String rawPassword) {
		return employeeNo != null && rawPassword != null
				&& this.employeeNo.equalsIgnoreCase(employeeNo.trim())
				&& this.password.equals(rawPassword);
	}

	/**
	 * 指定用户ID是否就是临时管理员
	 *
	 * @param userId 用户ID
	 * @return 是临时管理员时返回 true
	 */
	public boolean isAdmin(Long userId) {
		return USER_ID.equals(userId);
	}

	/**
	 * @return 登录工号
	 */
	public String employeeNo() {
		return employeeNo;
	}

	/**
	 * 临时管理员角色：仅用于页面展示，权限不依赖该角色
	 *
	 * @return 角色名称列表
	 */
	public List<String> roles() {
		return List.of(RoleCodes.MANAGER);
	}

	/**
	 * @return 全部细粒度权限点编码
	 */
	public List<String> permCodes() {
		return permCodes;
	}
}