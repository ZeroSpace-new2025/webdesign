package com.university.webdesign.common;

/**
 * 全项目统一的权限点编码常量。
 * <p>
 * 对应《重构实施规范》2.1：权限点是全项目共享的极小常量，因此放在 {@code common} 包，
 * 由用户中心（M4）持久化到 {@code permissions} 表并授予角色，其他模块用编码判定。
 */
public final class PermCodes
{
	/**
	 * 菜谱维护
	 */
	public static final String MENU_RECIPE_MANAGE = "menu:recipe:manage";

	/**
	 * 菜单维护
	 */
	public static final String MENU_MENU_MANAGE = "menu:menu:manage";

	/**
	 * 提交订单
	 */
	public static final String ORDER_SUBMIT = "order:submit";

	/**
	 * 作废违规订单
	 */
	public static final String ORDER_INVALIDATE = "order:invalidate";

	/**
	 * 查询全部订单
	 */
	public static final String ORDER_VIEW_ALL = "order:view:all";

	/**
	 * 触发总括订单聚合
	 */
	public static final String OPERATION_AGGREGATE = "operation:aggregate";

	/**
	 * 配送单打印
	 */
	public static final String OPERATION_DELIVERY_PRINT = "operation:delivery:print";

	/**
	 * 服务时间窗口维护
	 */
	public static final String OPERATION_WINDOW_MANAGE = "operation:window:manage";

	/**
	 * 员工账号维护
	 */
	public static final String USER_MANAGE = "user:manage";

	/**
	 * 角色权限维护
	 */
	public static final String ROLE_MANAGE = "role:manage";

	/**
	 * 查看报表
	 */
	public static final String REPORT_VIEW = "report:view";

	/**
	 * 导出报表
	 */
	public static final String REPORT_EXPORT = "report:export";

	/**
	 * 查看消费审计
	 */
	public static final String AUDIT_VIEW = "audit:view";

	/**
	 * 全部权限点编码（权限字典的权威列表，`RoleService.listPermissions` 按此输出）
	 */
	public static final String[] ALL = {
			MENU_RECIPE_MANAGE,
			MENU_MENU_MANAGE,
			ORDER_SUBMIT,
			ORDER_INVALIDATE,
			ORDER_VIEW_ALL,
			OPERATION_AGGREGATE,
			OPERATION_DELIVERY_PRINT,
			OPERATION_WINDOW_MANAGE,
			USER_MANAGE,
			ROLE_MANAGE,
			REPORT_VIEW,
			REPORT_EXPORT,
			AUDIT_VIEW
	};

	private PermCodes() {
	}
}
