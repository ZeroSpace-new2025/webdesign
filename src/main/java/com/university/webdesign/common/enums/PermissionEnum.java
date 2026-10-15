package com.university.webdesign.common.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 权限枚举类，也是全项目**唯一的权限点编码来源**。
 * <p>
 * 枚举值即 {@code role.permission_list} 位图里的位号（{@link #getCode()}），因此
 * <b>不要随意修改已有枚举值</b>，否则数据库中已保存的位图会失效；新增权限只能往后追加。
 * <p>
 * 本枚举包含两层权限：
 * <ol>
 *     <li><b>模块级管理/阅读权限</b>（{@code 1~12}）：{@code X_MANAGER} / {@code X_READER}，
 *         面向“按模块粗粒度授权”的场景，保留新角色模型最初定义的枚举值，不写 {@code permCode}；</li>
 *     <li><b>细粒度权限点</b>（{@code 13~25}）：{@link #getPermCode()} 非空，是全项目实际使用、
 *         可授予、可展示的权限集合，即 {@code GET /permissions} 返回的权限字典，
 *         也是 {@code @RequiresPerm}、JWT 载荷 {@code permCodes} 与前端判定所用的编码。</li>
 * </ol>
 * 模块级枚举值不写 {@code permCode}，其展示编码回退为枚举常量名（{@link #displayCode()}）。
 */
@Getter
public enum PermissionEnum
{
	None(0, null, "无权限", null),
	//====管理权限枚举====
	/**
	 * 菜单管理权限
	 */
	MENU_MANAGER(1, null, "菜单管理", "menu"),

	/**
	 * 订单管理权限
	 */
	ORDER_MANAGER(2, null, "订单管理", "order"),

	/**
	 * 运营管理权限
	 */
	OPERATION_MANAGER(3, null, "运营管理", "operation"),

	/**
	 * 财务管理权限
	 */
	FINANCE_MANAGER(4, null, "财务管理", "report"),

	/**
	 * 用户管理权限
	 */
	USER_MANAGER(5, null, "用户管理", "user"),

	/**
	 * 角色管理权限
	 */
	ROLE_MANAGER(6, null, "角色管理", "role"),

	//===阅读权限枚举====
	/**
	 * 菜单阅读权限
	 */
	MENU_READER(7, null, "菜单阅读", "menu"),

	/**
	 * 订单阅读权限
	 */
	ORDER_READER(8, null, "订单阅读", "order"),

	/**
	 * 运营阅读权限
	 */
	OPERATION_READER(9, null, "运营阅读", "operation"),

	/**
	 * 财务阅读权限
	 */
	FINANCE_READER(10, null, "财务阅读", "report"),

	/**
	 * 用户阅读权限
	 */
	USER_READER(11, null, "用户阅读", "user"),

	/**
	 * 角色阅读权限
	 */
	ROLE_READER(12, null, "角色阅读", "role"),

	//===细粒度权限点：permCode 是全项目唯一的权限点编码（新增只能往后追加）===
	/**
	 * 菜品维护（{@code menu:recipe:manage}）
	 */
	MENU_RECIPE_MANAGE(13, "menu:recipe:manage", "菜品维护", "menu"),

	/**
	 * 菜单维护（{@code menu:menu:manage}）
	 */
	MENU_MENU_MANAGE(14, "menu:menu:manage", "菜单维护", "menu"),

	/**
	 * 提交订单（{@code order:submit}）
	 */
	ORDER_SUBMIT(15, "order:submit", "提交订单", "order"),

	/**
	 * 作废违规订单（{@code order:invalidate}）
	 */
	ORDER_INVALIDATE(16, "order:invalidate", "作废违规订单", "order"),

	/**
	 * 查询全部订单（{@code order:view:all}）
	 */
	ORDER_VIEW_ALL(17, "order:view:all", "查看全部订单", "order"),

	/**
	 * 触发总括订单聚合（{@code operation:aggregate}）
	 */
	OPERATION_AGGREGATE(18, "operation:aggregate", "聚合总括订单", "operation"),

	/**
	 * 配送单打印（{@code operation:delivery:print}）
	 */
	OPERATION_DELIVERY_PRINT(19, "operation:delivery:print", "配送单打印", "operation"),

	/**
	 * 服务时间窗口维护（{@code operation:window:manage}）
	 */
	OPERATION_WINDOW_MANAGE(20, "operation:window:manage", "维护时间窗口", "operation"),

	/**
	 * 员工账号维护（{@code user:manage}）
	 */
	USER_MANAGE(21, "user:manage", "员工账号维护", "user"),

	/**
	 * 角色权限维护（{@code role:manage}）
	 */
	ROLE_MANAGE(22, "role:manage", "角色权限维护", "role"),

	/**
	 * 查看报表（{@code report:view}）
	 */
	REPORT_VIEW(23, "report:view", "查看报表", "report"),

	/**
	 * 导出报表（{@code report:export}）
	 */
	REPORT_EXPORT(24, "report:export", "导出报表", "report"),

	/**
	 * 查看消费审计（{@code audit:view}）
	 */
	AUDIT_VIEW(25, "audit:view", "消费审计", "report")
	;

	/**
	 * 位图位号，落库到 {@code role.permission_list}，一旦确定不可更改
	 */
	private final int code;

	/**
	 * 权限点编码，全项目唯一来源；模块级枚举值为 null
	 */
	private final String permCode;

	/**
	 * 权限点中文名称（权限字典与前端展示用）
	 */
	private final String permName;

	/**
	 * 所属模块（menu / order / operation / user / role / report）
	 */
	private final String module;

	PermissionEnum(int code, String permCode, String permName, String module) {
		this.code = code;
		this.permCode = permCode;
		this.permName = permName;
		this.module = module;
	}

	/**
	 * 按位号查枚举，未知位号抛异常
	 *
	 * @param code 位号
	 * @return 权限枚举
	 */
	public static PermissionEnum valueOf(int code) {
		for (PermissionEnum permission : PermissionEnum.values()) {
			if (permission.getCode() == code) {
				return permission;
			}
		}
		throw new IllegalArgumentException("Invalid PermissionEnum code: " + code);
	}

	/**
	 * 按位号查枚举，未知位号返回 {@link #None}
	 *
	 * @param code 位号
	 * @return 权限枚举；未知时返回 {@link #None}
	 */
	public static PermissionEnum fromCode(int code) {
		for (PermissionEnum permission : PermissionEnum.values()) {
			if (permission.getCode() == code) {
				return permission;
			}
		}
		return None; // Return None if the code is not found
	}

	/**
	 * 按权限点编码（或枚举常量名）查枚举，忽略大小写
	 * <p>
	 * 同时接受细粒度权限点编码（如 {@code report:view}）与枚举常量名（如 {@code REPORT_VIEW}），
	 * 便于接口层容错。
	 *
	 * @param text 权限点编码或枚举常量名
	 * @return 权限枚举；无法识别时返回空
	 */
	public static Optional<PermissionEnum> parse(String text) {
		if (text == null || text.isBlank()) {
			return Optional.empty();
		}
		String expected = text.trim();
		for (PermissionEnum permission : PermissionEnum.values()) {
			if (permission == None) {
				continue;
			}
			if (expected.equalsIgnoreCase(permission.displayCode())
					|| expected.equalsIgnoreCase(permission.name())) {
				return Optional.of(permission);
			}
		}
		return Optional.empty();
	}

	/**
	 * 权限字典：全部可授予的细粒度权限点，按模块、位号排序
	 *
	 * @return 权限点列表
	 */
	public static List<PermissionEnum> dictionary() {
		List<PermissionEnum> permissions = new ArrayList<>();
		for (PermissionEnum permission : PermissionEnum.values()) {
			if (permission.permCode != null) {
				permissions.add(permission);
			}
		}
		permissions.sort((left, right) -> {
			String leftModule = left.module == null ? "" : left.module;
			String rightModule = right.module == null ? "" : right.module;
			int byModule = leftModule.compareTo(rightModule);
			return byModule != 0 ? byModule : Integer.compare(left.code, right.code);
		});
		return permissions;
	}

	/**
	 * 权限字典筛选：按模块过滤
	 *
	 * @param module 模块名，为空表示不限
	 * @return 权限点列表
	 */
	public static List<PermissionEnum> dictionary(String module) {
		if (module == null || module.isBlank()) {
			return dictionary();
		}
		String expected = module.trim();
		return dictionary().stream()
				.filter(permission -> expected.equalsIgnoreCase(permission.module))
				.toList();
	}

	/**
	 * 展示用编码：细粒度权限点用其 {@code permCode}，模块级枚举值回退为常量名
	 *
	 * @return 展示编码
	 */
	public String displayCode() {
		return permCode != null ? permCode : name();
	}
}
