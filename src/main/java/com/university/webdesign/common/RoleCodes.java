package com.university.webdesign.common;

/**
 * 全项目统一的角色名称常量。
 * <p>
 * 对应《重构实施规范》2.1：角色名称是全项目共享的极小常量，因此放在 {@code common} 包，
 * 各模块（M1~M4）只依赖本类，不再各自声明字符串字面量。
 * <p>
 * 角色名称同时是 `role.name`（全局唯一）与 JWT 里的角色标识，因此取值保持大写下划线形式，
 * {@code @RequiresPerm(roles = …)}、前端 {@code currentUser.roleCodes} 均按此匹配。
 * <p>
 * 角色取值与 {@code demand.md} 的业务角色一一对应：
 * 餐厅经理、厨房主管、配餐员、财务管理、企业员工。
 */
public final class RoleCodes
{
	/**
	 * 餐厅经理（系统管理员，拥有全部权限）
	 */
	public static final String MANAGER = "MANAGER";

	/**
	 * 厨房主管
	 */
	public static final String KITCHEN_SUPERVISOR = "KITCHEN_SUPERVISOR";

	/**
	 * 配餐员
	 */
	public static final String DELIVERY_STAFF = "DELIVERY_STAFF";

	/**
	 * 财务管理（可查他人历史与消费明细、导出报表）
	 */
	public static final String FINANCE = "FINANCE";

	/**
	 * 企业员工
	 */
	public static final String EMPLOYEE = "EMPLOYEE";

	/**
	 * 全部预置角色名称（用户中心做数据初始化、以及“预置角色不可改名/不可删除”校验时使用）
	 */
	public static final String[] ALL = {
			MANAGER, KITCHEN_SUPERVISOR, DELIVERY_STAFF, FINANCE, EMPLOYEE
	};

	/**
	 * 判断角色名称是否属于预置角色
	 *
	 * @param name 角色名称（`role.name`）
	 * @return 是预置角色时返回 true
	 */
	public static boolean isPreset(String name) {
		if (name == null) {
			return false;
		}
		for (String code : ALL) {
			if (code.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	private RoleCodes() {
	}
}
