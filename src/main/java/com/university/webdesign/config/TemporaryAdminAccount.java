package com.university.webdesign.config;

import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.enums.PermissionEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 临时硬编码管理员账号（课程演示用，**不落库**）。
 * <p>
 * 用途：在空库、{@code app.storage=none} 或数据库不可用时也能登录系统，方便演示与联调。
 * 账号默认 {@code admin / admin123}，可用 {@code app.temporary-admin.employee-no} /
 * {@code app.temporary-admin.password} 覆盖；它固定拥有 {@link PermissionEnum#dictionary()}
 * 里的全部细粒度权限点，因此不受数据库角色配置影响。
 * <p>
 * 与数据库用户完全隔离：登录时 {@code AuthServiceImpl} 先比对本账号，命中后直接签发 JWT，
 * 不查 {@code users} 表；{@link #USER_ID} 使用负数，绝不与数据库自增主键冲突。
 * <p>
 * <b>上线前必须删除</b>，删除步骤：
 * <ol>
 *     <li>删除本类；</li>
 *     <li>删除 {@code AuthServiceImpl} 中对 {@code TemporaryAdminAccount} 的字段、构造参数、
 *         登录分支与 {@code temporaryAdminInfo()} 方法；</li>
 *     <li>删除 {@code web.PageController} 中对它的注入与 {@code temporaryAdmin} 模型属性，
 *         以及登录页/总门户里的“演示账号”入口；</li>
 *     <li>删除 {@code app.temporary-admin.*} 配置项。</li>
 * </ol>
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