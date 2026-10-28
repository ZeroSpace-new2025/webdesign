package com.university.webdesign.config;

import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.enums.PermissionEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 课程演示用临时管理员账号。
 * <p>
 * 该账号不落库，默认工号 {@code admin}、密码 {@code admin123}，拥有
 * {@link PermissionEnum} 中全部细粒度权限点，因此空库或 {@code app.storage=none}
 * 时仍可进入系统。正式上线前应删除本类及以下引用：
 * <ul>
 *     <li>{@code impl.user.AuthServiceImpl} 中的临时账号登录与权限分支；</li>
 *     <li>{@code web.PageController} 中的临时管理员提示；</li>
 *     <li>登录页与总门户中的临时账号说明和快捷登录按钮。</li>
 * </ul>
 */
@Component
public class TemporaryAdminAccount
{
	/**
	 * 临时管理员固定用户ID，使用负数避免与数据库自增ID冲突
	 */
	public static final Long USER_ID = -1L;

	/**
	 * 临时管理员展示名称
	 */
	public static final String NAME = "临时管理员";

	private final boolean enabled;
	private final String employeeNo;
	private final String password;
	private final List<String> roles;
	private final List<String> permCodes;

	/**
	 * 构造器注入配置。
	 *
	 * @param enabled    是否启用临时管理员
	 * @param employeeNo 临时管理员工号
	 * @param password   临时管理员明文密码
	 */
	public TemporaryAdminAccount(
			@Value("${app.temporary-admin.enabled:true}") boolean enabled,
			@Value("${app.temporary-admin.employee-no:admin}") String employeeNo,
			@Value("${app.temporary-admin.password:admin123}") String password) {
		this.enabled = enabled;
		this.employeeNo = employeeNo == null ? "admin" : employeeNo.trim();
		this.password = password == null ? "admin123" : password;
		this.roles = List.of(RoleCodes.MANAGER);
		this.permCodes = PermissionEnum.dictionary().stream()
				.map(PermissionEnum::getPermCode)
				.toList();
	}

	/**
	 * 判断工号与密码是否匹配临时管理员。
	 *
	 * @param candidateEmployeeNo 登录工号
	 * @param candidatePassword   登录密码
	 * @return 匹配且启用时返回 true
	 */
	public boolean matches(String candidateEmployeeNo, String candidatePassword) {
		if (!enabled || candidateEmployeeNo == null || candidatePassword == null) {
			return false;
		}
		return employeeNo.equalsIgnoreCase(candidateEmployeeNo.trim())
				&& secureEquals(password, candidatePassword);
	}

	/**
	 * 判断用户ID是否代表临时管理员。
	 *
	 * @param userId 用户ID
	 * @return 是临时管理员且功能启用时返回 true
	 */
	public boolean isAdmin(Long userId) {
		return enabled && USER_ID.equals(userId);
	}

	/**
	 * @return 临时管理员工号
	 */
	public String employeeNo() {
		return employeeNo;
	}

	/**
	 * @return 临时管理员角色
	 */
	public List<String> roles() {
		return roles;
	}

	/**
	 * @return 现有权限字典中的全部细粒度权限点
	 */
	public List<String> permCodes() {
		return permCodes;
	}

	/**
	 * 常量时间字符串比较，避免密码比较出现明显的时间差。
	 *
	 * @param expected 配置密码
	 * @param actual   用户输入
	 * @return 内容一致时返回 true
	 */
	private boolean secureEquals(String expected, String actual) {
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
				actual.getBytes(StandardCharsets.UTF_8));
	}
}
